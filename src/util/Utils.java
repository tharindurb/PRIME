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
package util;

import nlptasks.SentenceSplitter;
import nlptasks.SentenceTokenizer;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
public class Utils {
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public static String compilePostsToOne(JSONArray posts) throws JSONException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < posts.length(); i++) {
            String post = posts.getString(i).trim();
            sb.append(post);
            if (post.length() > 1 && post.charAt(post.length() - 1) != '.') {
                sb.append(". ");
            }
        }
        String text = removeUnicode(sb.toString());

        return text;
    }

    public static String removeUnicode(String text) {
        String result = Normalizer.normalize(text, Normalizer.Form.NFD);
        return result.replaceAll("[^\\x00-\\x7F]", "");
    }

    public static String removePunctuations(String text) {
        return text.replaceAll("[:?\\.;<>(),!+-/\"]", " ");
    }

    public static int countWords(String text) {
        int nWords = 0;
        text = removeUnicode(text);
        for(String sentence : SentenceSplitter.getSentenceSplitter().sentDetect(text)){
            sentence = removePunctuations(sentence);
            String[] tokens  = SentenceTokenizer.getSentenceTokenizer().getTokens(sentence);
            nWords += tokens.length;
        }

        return nWords;
    }

    public static JSONObject getJSONFromFile(File file) throws IOException, JSONException {
        String sLine;
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));

        while ((sLine = reader.readLine()) != null) sb.append(sLine);
        reader.close();
        return new JSONObject(sb.toString());
    }

    public static List<String> getExperiencePostContent(JSONObject profile) throws JSONException {
        List<String> postContent = new ArrayList<>();
        JSONArray posts = profile.getJSONArray("Posts");

        for (int i = 0; i < posts.length(); i++) {
            JSONObject post = posts.getJSONObject(i);

            if (!post.getJSONObject("Demographics").getJSONObject("GenderInfo").getJSONObject("NounBasedGender").getString("NarrationType").equalsIgnoreCase("advice")) {
                String content = Utils.removeUnicode(post.getString("Content"));

                if (post.getInt("MessageIndex") == 0) {
                    content = Utils.removeUnicode(post.getString("Title")) + " " + content;
                }
                postContent.add(content);
            }
        }
        return postContent;
    }

    public static List<String> getAllPostContent(JSONObject profile) throws JSONException {
        List<String> postContent = new ArrayList<>();
        JSONArray posts = profile.getJSONArray("Posts");

        for (int i = 0; i < posts.length(); i++) {
            JSONObject post = posts.getJSONObject(i);
            String content = Utils.removeUnicode(post.getString("Content"));
            if (post.getInt("MessageIndex") == 0) {
                content = Utils.removeUnicode(post.getString("Title")) + " " + content;
            }
            postContent.add(content);
        }
        return postContent;
    }

    public static Map<String, String> getTimelineInfo() {
        Map<String, String> timelineMap = new LinkedHashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./models/surgeryKeywords/surgeryTimeline.txt"));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] firstSplit = line.split(":");
                    String timeline = firstSplit[0];
                    String dateRange = firstSplit[1];
                    timelineMap.put(timeline, dateRange);
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return timelineMap;
    }

    public static int getDateDifferece(String initialDate, String postDate) {
        Date date1 = null;
        try {
            date1 = dateFormat.parse(initialDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date date2 = null;
        try {
            date2 = dateFormat.parse(postDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long diff = date2.getTime() - date1.getTime();
        int dateDiff = (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        return dateDiff;
    }
}
