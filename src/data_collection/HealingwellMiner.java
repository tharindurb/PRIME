/*
 *   This is part of the source code of Patient Reported Information Multidimensional Exploration (PRIME)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package data_collection;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HealingwellMiner {
    private final String index = "general";
    private final String type = "healingwell";
    private final String baseURL = "http://www.healingwell.com";
    private final String authorFileName = "healingwell_forum_authors_all.txt";
    private final String threadFileName = "healingwell_forum_threads_all.csv";
    private final ESBulkFeeder esBulkFeeder = new ESBulkFeeder(index, type, 100);
    private static final Semaphore s = new Semaphore(2);
    private static SimpleDateFormat fromDate = new SimpleDateFormat("MM/dd/yyyy");
    private static SimpleDateFormat toDate = new SimpleDateFormat("dd/MM/yyyy");

    private static final Pattern forumDatePattern = Pattern.compile("\\b(0?[1-9]|1[012])[\\/\\-](0?[1-9]|[12][0-9]|3[01])[\\/\\-]\\d{4}\\b");
    private static final Pattern nPostsPattern = Pattern.compile("\\b(\\d+)\\s+posts\\s+in\\s+this\\s+thread\\b");

    public static void main(String... args) throws Exception {
        HealingwellMiner miner = new HealingwellMiner();
        miner.collectThreadURLS();
        Thread.sleep(300000);
        miner.readThreads();
        Thread.sleep(300000);
        miner.collectAuthorProfiles();
    }

    private void collectAuthorProfiles() throws Exception {
        BufferedReader authorFileReader = new BufferedReader(new FileReader(authorFileName));

        String folderName = "./ProstateProfiles/" + type;
        File temp = new File(folderName);
        if(!temp.exists()) temp.mkdir();

        String line;
        while ((line = authorFileReader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                String authorID = line;

                JSONArray posts = DataCollectionUtils.getAuthoredPosts(authorID,esBulkFeeder.getClient(),index,type);

                if(posts.length()==0) continue;
                String AuthorURL = posts.getJSONObject(0).getString("AuthorURL");

                JSONObject profile = new JSONObject();
                profile.put("Posts", posts);
                profile.put("AuthorID", authorID);
                profile.put("AuthorURL", AuthorURL);
                BufferedWriter writer = new BufferedWriter(new FileWriter(folderName + "/" + util.Utils.removePunctuations(authorID) + ".json"));
                writer.write(profile.toString(2));
                writer.close();
                System.out.println("Author: " + authorID + " processed");
            }
        }
    }

    private void readThreads() {
        try {
            new Resty().json("http://localhost:9200/" + index + "/" + type + "/_mapping", Resty.put(Resty.content(DataCollectionUtils.getIndexMapping()))).object();
            final Set<String> authors = new HashSet<>();
            BufferedWriter authorFileWriter = new BufferedWriter(new FileWriter(authorFileName));
            CSVParser csvParser = new CSVParser(new FileReader(threadFileName), CSVFormat.RFC4180.withHeader().withDelimiter(','));

            for (final CSVRecord record : csvParser.getRecords()) {

                s.acquire();
                new Thread() {
                    public void run() {
                        try {
                            ForumThread forumThread = readThread(record.get(0), record.get(1));
                            feedToES(forumThread);

                            synchronized (this) {
                                for (Message m : forumThread.messages) {
                                    authors.add(m.getAuthorID());
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            s.release();
                        }
                    }
                }.start();

            }

            synchronized (this) {
                esBulkFeeder.flushBulk();

                for (String author : authors) {
                    authorFileWriter.write(author + "\n");
                }

                authorFileWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private ForumThread readThread(String title, String threadURL) {
        ForumThread forumThread = new ForumThread(title, threadURL);
        forumThread.messages = new ArrayList<>();
        int pageNo = 1;


        try {

            String pageURL = threadURL + "&p=" + pageNo++;
            Document doc = Jsoup.connect(pageURL).timeout(10000).ignoreContentType(true).get();


            int nPages = getNumberOfPages(doc);
            int i = 0;
            while (true) {


                Elements messageElements = doc.select("table[class=PostBox]");


                for (Element messageElement : messageElements) {
                    Element userElement = messageElement.select("td[class=msgUser]").first().select("a[href]").first();
                    String authorID = userElement.text();
                    String authorURL = userElement.attr("href");
                    String date = getDate(messageElement.select("td[class=msgThreadInfo PostThreadInfo").first().text());

                    String text = getMessageText(messageElement.select("div[class=PostBoxWrapper]").first());
                    Message message = new Message(text, date, i++, authorURL, authorID);
                    forumThread.messages.add(message);
                }

                System.out.println("Processed: " + pageURL);

                if (pageNo > nPages) break;

                pageURL = threadURL + "&p=" + pageNo++;
                doc = Jsoup.connect(pageURL).timeout(10000).ignoreContentType(true).get();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        forumThread.nMessages = forumThread.messages.size();
        return forumThread;
    }

    private int getNumberOfPages(Document doc) {
        for (Element element : doc.select("td[class=msgSm]")) {
            String text = element.text();
            Matcher m = nPostsPattern.matcher(text);

            if (m.find()) {
                int nPosts = Integer.parseInt(m.group(1));
                return (int) Math.ceil(nPosts / 25.00);
            }
        }

        return 1;
    }

    private String getMessageText(Element messageBody) {
        StringBuilder text = new StringBuilder();
        for (Element e : messageBody.children()) {
            if (!e.hasClass("PostToTopLink")) {
                text.append(e.text()).append("\n");
            }
        }

        if (text.length() > 1) text.setLength((text.length() - 1));

        return text.toString();
    }

    private String getDate(String postDate) {
        if (postDate.contains("Today") || postDate.contains("minutes")) {
            return "11/06/2017";
        } else if (postDate.contains("Yesterday")) {
            return "10/06/2017";
        } else {
            Matcher m = forumDatePattern.matcher(postDate);
            if (m.find()) {
                return m.group(0);
            } else {
                System.out.println("!!!!!! Date Reading Issue: " + postDate);
                return "";
            }
        }
    }

    private void collectThreadURLS() {
        try {
            CSVPrinter printer = new CSVPrinter(new FileWriter(threadFileName), CSVFormat.RFC4180.withHeader(new String[]{"Title", "Link"}).withDelimiter(','));
            Map<String, Integer> forumMap = getForumMap();
            for(Map.Entry<String,Integer> f: forumMap.entrySet()) {
                for (int i = 1; i <= f.getValue(); i++) {
                    String pageURL = baseURL + "/community/default.aspx?f="+f.getKey()+"&p=" + i;
                    Document doc = Jsoup.connect(pageURL).timeout(10000).ignoreContentType(true).get();

                    Elements threads = doc.select("td[class=msgTopic TopicTitle");
                    for (Element thread : threads) {
                        Element linkElement = thread.select("a").first();
                        String link = baseURL + linkElement.attr("href");
                        String title = linkElement.text();
                        printer.printRecord(new String[]{title, link});
                    }

                    System.out.println("Processed Page: " + pageURL + " with threads: " + threads.size());
                    printer.flush();
                }
            }
            printer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String,Integer> getForumMap() {
        Map<String, Integer> forumMap = new HashMap<>();
        forumMap.put("35",33064);

        for(String key: forumMap.keySet()) {
            forumMap.put(key,(int)Math.ceil(forumMap.get(key)/25.0));
        }

        return forumMap;
    }

    private void feedToES(ForumThread forumThread) throws JSONException, ParseException {
        synchronized (this) {
            String baseID = forumThread.url.replaceFirst("http://", "").trim();
            for (Message message : forumThread.messages) {
                JSONObject messageJSON = new JSONObject();
                messageJSON.put("Title", forumThread.title);
                messageJSON.put("Content", message.getText());
                messageJSON.put("MessageIndex", message.getIndex());
                messageJSON.put("ThreadURL", forumThread.url);
                messageJSON.put("PostDate", toDate.format(fromDate.parse(message.getDate())));
                messageJSON.put("AuthorID", message.getAuthorID());
                messageJSON.put("AuthorURL", baseURL + message.getAuthorURL());

                esBulkFeeder.feedToES(baseID + "_" + message.getIndex(), messageJSON);
            }
        }

    }
}
