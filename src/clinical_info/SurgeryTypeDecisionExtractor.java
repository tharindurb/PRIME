/*
 *   This is part of the source code of Patient Empowered Analytics Platform (PEAP)
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
package clinical_info;

import demographic.HumanListModified;
import emotion.EmotionExtractor;
import nlptasks.SentenceSplitter;
import nlptasks.SentenceTokenizer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import util.Utils;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SurgeryTypeDecisionExtractor {
    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static final Set<String> stoplist = getStoplist();
    private static final String stopListFile = "./Resources/JATEResources/stoplist.txt";

    private static Set<String> getStoplist() {
        Set<String> _stopSet = new HashSet<>();
        try {

            final BufferedReader reader = new BufferedReader(new FileReader(stopListFile));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equals("") || line.startsWith("//")) continue;
                _stopSet.add(line.toLowerCase());
            }
            reader.close();

            Set<String> patternTokens = SurgeryDecisionCategory.getTokens();
            for (String token : patternTokens) _stopSet.remove(token);
        } catch (IOException e) {
            e.printStackTrace();
        }


        return _stopSet;
    }

    public static void main(String... args) throws Exception {
        List<String> header = new ArrayList<>();
        for (String s : new String[]{"AuthorID", "Sentence", "Has I", "Has You", "Has Doc"}) header.add(s);
        for (String s : SurgeryDecisionCategory.getHeader()) header.add(s);

        final String folderPath = "C:\\TextClustering\\HealthForums\\Prostate Cancer\\Surgery Type work\\ProstateProfiles\\";
        CSVPrinter csvPrinter = new CSVPrinter(new FileWriter("surgery decison.csv"), CSVFormat.RFC4180.withDelimiter(',').withHeader(header.toArray(new String[header.size()])));

        SurgeryTypeDecisionExtractor extractor = new SurgeryTypeDecisionExtractor();
        String[] typeList = new String[]{"ustoo", "cancerforums", "healthboards", "macmillanuk", "cancercompass", "csn", "prostatecanceruk", "patientinfo", "healingwell"};
        //        String[] typeList = new String[]{"cancerforums", "healthboards", "macmillanuk"};

        for (String type : typeList) {
            extractor.processProfiles(folderPath + type, csvPrinter);
            csvPrinter.flush();
            System.out.println(type + " done.");
        }
        csvPrinter.close();

        //        evaluateSamples();
    }

    private static void evaluateSamples() throws JSONException {
        List<String> samples = new ArrayList<>();
        samples.add("In my non professional opinion, robotic allows for more precision because the mechanical arms have more degrees of freedom, smaller steadier movements, and color 3D, 10X magnification view which more than compensate for lack of feel of the surgeon's hands.");
        for (String sample : samples) {
            JSONObject surgeryDecisionInfo = new JSONObject();
            getDecisionInfo(sample, surgeryDecisionInfo);
            surgeryDecisionInfo.remove("Sentence");
            System.out.println(surgeryDecisionInfo);
        }


    }

    private static void getDecisionInfo(String sentence, JSONObject surgeryDecisionInfo) throws JSONException {
        String cleanedSentence = cleanSentence(sentence);
        Boolean hasDocMention = HumanListModified.hasHealthOccupationMentioned(cleanedSentence);
        surgeryDecisionInfo.put("Sentence", sentence);
        surgeryDecisionInfo.put("HasI", HumanListModified.hasI(cleanedSentence));
        surgeryDecisionInfo.put("HasYou", HumanListModified.hasYou(cleanedSentence));
        surgeryDecisionInfo.put("HasDoc", hasDocMention);

        for (String decisionCategory : SurgeryDecisionCategory.getHeader()) {
            if (decisionCategory.equalsIgnoreCase("Surgeon")) {
                if (hasDocMention) {
                    String docWindow = getDocWindow(cleanedSentence, 10);
                    surgeryDecisionInfo.put(decisionCategory, SurgeryDecisionCategory.hasDecisionCategory(decisionCategory, docWindow));
                } else {
                    surgeryDecisionInfo.put(decisionCategory, false);
                }
            } else {
                surgeryDecisionInfo.put(decisionCategory, SurgeryDecisionCategory.hasDecisionCategory(decisionCategory, cleanedSentence));
            }
        }
    }

    public static void extractDecisionTypeInfo(JSONObject profile) {
        try {
            System.out.println("surgery type decision extracting: " + profile.getString("AuthorID"));
            if (profile.has("SurgeryTypeInfo") && profile.getJSONObject("SurgeryTypeInfo").has("SurgeryTypeKeywords")) {
                JSONObject surgeryTypeInfo = profile.getJSONObject("SurgeryTypeInfo");
                List<JSONObject> posts = sortPostsByDate(profile.getJSONArray("Posts"));
                boolean found = false;
                String decisionDate = null;
                JSONObject surgeryDecisionInfo = new JSONObject();

                for (JSONObject post : posts) {
                    String[] sentences = SentenceSplitter.getSentenceSplitter().sentDetect(post.getString("Content"));
                    for (int i = 0; i < sentences.length; i++) {
                        String sentence = sentences[i];
                        if (SurgeryTypeExtarctor.hasSurgeryMentions(sentence) && DecisionTermExtractor.hasDecisionPhrases("Decision", sentence)) {
                            found = true;
                            if (i - 1 >= 0) sentence = sentences[i - 1] + " " + sentence;
                            if (i + 1 < sentences.length) sentence = sentence + " " + sentences[i + 1];
                            decisionDate = post.getString("PostDate");
                            surgeryDecisionInfo.put("DecisionDate", decisionDate);
                            getDecisionInfo(sentence, surgeryDecisionInfo);
                        }
                    }

                    if (found) break;
                }

                if (found && surgeryTypeInfo.has("DateOfSurgery")) {
                    String dateOfSurgery = surgeryTypeInfo.getString("DateOfSurgery").trim();
                    if (!dateOfSurgery.isEmpty()) {
                        int diff = getDateDifference(decisionDate, dateOfSurgery);
                        surgeryDecisionInfo.put("DecisionToProcedureDays", diff);

                        if (diff > 0) {
                            StringBuilder beforeDecision = new StringBuilder(" ");
                            StringBuilder onDecision = new StringBuilder(" ");
                            StringBuilder afterDecisionBeforeSurgery = new StringBuilder(" ");

                            for (int i = 0; i < posts.size(); i++) {
                                JSONObject post = posts.get(i);
                                String postDate = post.getString("PostDate");
                                int d1 = getDateDifference(decisionDate, postDate);
                                int d2 = getDateDifference(dateOfSurgery, postDate);
                                if (d1 < 0) {
                                    if (!post.getJSONObject("Demographics").getJSONObject("GenderInfo").getJSONObject("NounBasedGender").getString("NarrationType").equalsIgnoreCase("advice")) {
                                        beforeDecision.append(post.getString("Content")).append(" ");
                                    }
                                } else if (d1 == 0) {
                                    onDecision.append(post.getString("Content")).append(" ");
                                } else if (d1 > 0 && d2 < 0) {
                                    if (!post.getJSONObject("Demographics").getJSONObject("GenderInfo").getJSONObject("NounBasedGender").getString("NarrationType").equalsIgnoreCase("advice")) {
                                        afterDecisionBeforeSurgery.append(post.getString("Content")).append(" ");
                                    }
                                }
                            }

                            JSONObject emotionBeforeDecision = new JSONObject();
                            EmotionExtractor.getEmotionsProfileJSONPerPost( beforeDecision.toString(), emotionBeforeDecision);
                            JSONObject emotionOnDecision = new JSONObject();
                            EmotionExtractor.getEmotionsProfileJSONPerPost( onDecision.toString(), emotionOnDecision);
                            JSONObject emotionAfterDecisionBeforeSurgery = new JSONObject();
                            EmotionExtractor.getEmotionsProfileJSONPerPost(afterDecisionBeforeSurgery.toString(), emotionAfterDecisionBeforeSurgery);
                            surgeryDecisionInfo.put("EmotionBeforeDecision", emotionBeforeDecision.getJSONObject("EmotionInfo").getJSONObject("Emotions"));
                            surgeryDecisionInfo.put("EmotionOnDecision", emotionOnDecision.getJSONObject("EmotionInfo").getJSONObject("Emotions"));
                            surgeryDecisionInfo.put("EmotionAfterDecisionBeforeSurgery", emotionAfterDecisionBeforeSurgery.getJSONObject("EmotionInfo").getJSONObject("Emotions"));
                        }
                    } else {
                        surgeryDecisionInfo.put("DecisionToProcedureDays", "");
                    }
                }

                if (found) surgeryTypeInfo.put("SurgeryDecisionInfo", surgeryDecisionInfo);


            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int getDateDifference(String firstDate, String secondDate) {
        Date date1 = null;
        try {
            date1 = dateFormat.parse(firstDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date date2 = null;
        try {
            date2 = dateFormat.parse(secondDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long diff = date2.getTime() - date1.getTime();
        //  System.out.println("Days: " + TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS));
        int dateDiff = (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        return dateDiff;
    }

    private void processProfiles(String folderPath, CSVPrinter csvPrinter) throws Exception {

        //        Map<String, List<Pattern>> wordFeatureMap = getWordFeatureMap();
        File folder = new File(folderPath);
        File[] fileList = folder.listFiles();

        for (File file : fileList) {
            if (file.isFile()) {
                JSONObject profile = Utils.getJSONFromFile(file);
                List<JSONObject> posts = sortPostsByDate(profile.getJSONArray("Posts"));
                for (JSONObject post : posts) {
                    String[] sentences = SentenceSplitter.getSentenceSplitter().sentDetect(post.getString("Content"));
                    for (int i = 0; i < sentences.length; i++) {
                        String sentence = sentences[i];
                        //                        if (i - 1 >= 0) sentence = sentences[i - 1] + " " +  sentence;
                        //                        if (i + 1 < sentences.length) sentence = sentence + " " +  sentences[i + 1];
                        if (SurgeryTypeExtarctor.hasSurgeryMentions(sentence) && DecisionTermExtractor.hasDecisionPhrases("Decision", sentence)) {
                            JSONObject surgeryDecisionInfo = new JSONObject();
                            getDecisionInfo(sentence, surgeryDecisionInfo);
                            List<Object> values = new ArrayList<>();
                            values.add(profile.getString("AuthorID"));
                            values.add(sentence);
                            values.add(surgeryDecisionInfo.getString("HasI"));
                            values.add(surgeryDecisionInfo.getString("HasYou"));
                            values.add(surgeryDecisionInfo.getString("HasDoc"));
                            for (String decisionCategory : SurgeryDecisionCategory.getHeader()) {
                                values.add(surgeryDecisionInfo.getString(decisionCategory));
                            }
                            csvPrinter.printRecord(values);
                        }
                    }
                }

            }
        }
    }

    private static List<JSONObject> sortPostsByDate(JSONArray posts) throws Exception {
        List<JSONObject> sortedPosts = new ArrayList<>();

        for (int i = 0; i < posts.length(); i++) {
            JSONObject post = posts.getJSONObject(i);
            sortedPosts.add(post);
        }

        sortedPosts.sort((o1, o2) -> {
            try {
                Date o1Date = dateFormat.parse(o1.getString("PostDate"));
                Date o2Date = dateFormat.parse(o2.getString("PostDate"));
                return o1Date.compareTo(o2Date);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });

        return sortedPosts;
    }

    private static String getDocWindow(String text, int w) {
        String[] toks = SentenceTokenizer.getSentenceTokenizer().getTokens(text);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < toks.length; i++) {
            if (HumanListModified.isHealthOccupationNoun(toks[i])) {
                for (int j = Math.max(0, i - w); j <= Math.min(toks.length - 1, i + w); j++) {
                    sb.append(toks[j]).append(" ");
                }
            }
        }
        return sb.toString();
    }

    private static String cleanSentence(String text) {
        text = Utils.removePunctuations(text).toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (String tok : SentenceTokenizer.getSentenceTokenizer().getTokens(text)) {
            if (!stoplist.contains(tok)) sb.append(tok).append(" ");
        }
        return sb.toString().trim();
    }

}
