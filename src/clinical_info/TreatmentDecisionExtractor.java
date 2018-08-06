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
package clinical_info;

import demographic.HumanListModified;
import nlptasks.SentenceSplitter;
import nlptasks.SentenceTokenizer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONObject;
import util.Utils;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TreatmentDecisionExtractor {
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
        for (String s : new String[]{"AuthorID", "Sentence", "RP", "EBRT", "AS", "Decision", "Recommend", "Suggested", "Has I", "Has You", "Has Doc"})
            header.add(s);

        final String folderPath = "C:\\TextClustering\\HealthForums\\Prostate Cancer\\Surgery Type work\\ProstateProfiles\\";
        CSVPrinter csvPrinter = new CSVPrinter(new FileWriter("treatment type decision.csv"), CSVFormat.RFC4180.withDelimiter(',').withHeader(header.toArray(new String[header.size()])));

        TreatmentDecisionExtractor extractor = new TreatmentDecisionExtractor();
        String[] typeList = new String[]{"ustoo", "cancerforums", "healthboards", "macmillanuk", "cancercompass", "csn", "prostatecanceruk", "patientinfo", "healingwell"};
//        String[] typeList = new String[]{"cancerforums", "healthboards", "macmillanuk"};

        for (String type : typeList) {
            extractor.processProfiles(folderPath + type, csvPrinter);
            csvPrinter.flush();
            System.out.println(type + " done.");
        }
        csvPrinter.close();
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

    public static void extractTreatmentDecision(JSONObject profile) {
        try {
            System.out.println("treatment type decision extracting: " + profile.getString("AuthorID"));

            if(profile.has("TreatmentDecision")) profile.remove("TreatmentDecision");

            JSONObject patientDecided = new JSONObject();
            JSONObject doctorRecommended = new JSONObject();
            Set<String> docSentences = new HashSet<>();
            Set<String> patientSentences = new HashSet<>();
            Set<String> docMentions = new HashSet<>();

            doctorRecommended.put("Value", 0);
            patientDecided.put("Value", 0);
            for (String decisionCategory : SurgeryDecisionCategory.getHeader()) {
                doctorRecommended.put(decisionCategory, 0);
                patientDecided.put(decisionCategory, 0);
            }

            int dr = 0;
            int pd = 0;
            boolean isFirst = true;



            if (profile.has("TreatmentTimelineInfo")) {
                String treatment = profile.getJSONObject("TreatmentTimelineInfo").getString("TreatmentType");

                List<JSONObject> posts = sortPostsByDate(profile.getJSONArray("Posts"));
                for (JSONObject post : posts) {
                    String[] sentences = SentenceSplitter.getSentenceSplitter().sentDetect(Utils.removeUnicode(post.getString("Content")));
                    for (int i = 0; i < sentences.length; i++) {
                        String sentence = sentences[i];
                        String sentence1 = sentence;
                        if (i - 1 >= 0) sentence = sentences[i - 1] + " " + sentence;

                        boolean hasTreatment = TreatmentTypeExtractor.hasTreatmentTypeMentions(treatment, sentence);
                        if (hasTreatment) {
                            boolean hasDecision = DecisionTermExtractor.hasDecisionPhrases("Decision", sentence);
                            boolean hasRecommended = DecisionTermExtractor.hasDecisionPhrases("Recommend", sentence);
                            boolean hasSuggested = DecisionTermExtractor.hasDecisionPhrases("Suggest", sentence);
                            String docName = HumanListModified.getHealthOccupationMentioned(sentence);
                            boolean hasDocMention = (docName != null);

                            if ((hasRecommended || hasSuggested) && hasDocMention) {
                                ++dr;

                                doctorRecommended.put("Value", 1);
                                docSentences.add(sentence);
                                docMentions.add(docName.trim().toLowerCase());
                                String cleanedSentence = cleanSentence(sentence);
                                for (String decisionCategory : SurgeryDecisionCategory.getHeader()) {
                                    if (decisionCategory.equalsIgnoreCase("Surgeon")) {
                                        String docWindow = getDocWindow(cleanedSentence, 10);
                                        if (SurgeryDecisionCategory.hasDecisionCategory(decisionCategory, docWindow))
                                            doctorRecommended.put(decisionCategory, 1);
                                    } else {
                                        if (SurgeryDecisionCategory.hasDecisionCategory(decisionCategory, cleanedSentence))
                                            doctorRecommended.put(decisionCategory, 1);
                                    }
                                }

                            }

                            if(hasDecision && !HumanListModified.hasYou(sentence1)){
                                ++pd;

                                patientDecided.put("Value", 1);
                                patientSentences.add(sentence);
                                String cleanedSentence = cleanSentence(sentence);
                                for (String decisionCategory : SurgeryDecisionCategory.getHeader()) {
                                    if (decisionCategory.equalsIgnoreCase("Surgeon")) {
                                        String docWindow = getDocWindow(cleanedSentence, 10);
                                        if (SurgeryDecisionCategory.hasDecisionCategory(decisionCategory, docWindow))
                                            patientDecided.put(decisionCategory, 1);
                                    } else {
                                        if (SurgeryDecisionCategory.hasDecisionCategory(decisionCategory, cleanedSentence))
                                            patientDecided.put(decisionCategory, 1);
                                    }
                                }
                            }
                        }
                    }
                }
            }

                if(dr > 0){
//                    patientDecided.put("Value", 0);
//                    for (String decisionCategory : SurgeryDecisionCategory.getHeader()) {
//                        patientDecided.put(decisionCategory, 0);
//                    }
                    StringBuilder docMentionsString = new StringBuilder();
                    for(String s: docMentions) docMentionsString.append(s).append(",");
                    if(docMentionsString.length() > 0) docMentionsString.setLength(docMentionsString.length()-1);
                    doctorRecommended.put("docMentions", docMentionsString.toString());

                    StringBuilder docSample = new StringBuilder();
                    for(String s: docSentences) docSample.append(s).append("...");
                    doctorRecommended.put("Sample",docSample.toString());
                }else if(pd > 0) {
//                    doctorRecommended.put("Value", 0);
//                    for (String decisionCategory : SurgeryDecisionCategory.getHeader()) {
//                        doctorRecommended.put(decisionCategory, 0);
//                    }

                    StringBuilder patientSample = new StringBuilder();
                    for(String s: patientSentences) patientSample.append(s).append("...");
                    patientDecided.put("Sample",patientSample.toString());
                }


            JSONObject treatmentDecision = new JSONObject();
            treatmentDecision.put("PatientDecided",patientDecided);
            treatmentDecision.put("DoctorRecommended",doctorRecommended);
            profile.put("TreatmentDecision",treatmentDecision);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                        if (i - 1 >= 0) sentence = sentences[i - 1] + " " + sentence;
//
                        boolean hasRP = TreatmentTypeExtractor.hasTreatmentTypeMentions("Surgery", sentence);
                        boolean hasEBRT = TreatmentTypeExtractor.hasTreatmentTypeMentions("Radiation", sentence);
                        boolean hasAS = TreatmentTypeExtractor.hasTreatmentTypeMentions("Surveillance", sentence);
                        if ((hasRP || hasEBRT || hasAS)) {
                            boolean hasDecision = DecisionTermExtractor.hasDecisionPhrases("Decision", sentence);
                            boolean hasRecommend = DecisionTermExtractor.hasDecisionPhrases("Recommend", sentence);
                            boolean hasSuggested = DecisionTermExtractor.hasDecisionPhrases("Suggest", sentence);

                            if (hasDecision || hasRecommend || hasSuggested) {
                                List<Object> values = new ArrayList<>();
                                String cleanedSentence = cleanSentence(sentence);
                                values.add(profile.getString("AuthorID"));
                                values.add(sentence);
                                values.add(hasRP);
                                values.add(hasEBRT);
                                values.add(hasAS);
                                values.add(hasDecision);
                                values.add(hasRecommend);
                                values.add(hasSuggested);
                                values.add(HumanListModified.hasI(cleanedSentence));
                                values.add(HumanListModified.hasYou(cleanedSentence));
                                values.add(HumanListModified.hasHealthOccupationMentioned(cleanedSentence));
                                csvPrinter.printRecord(values);
                            }
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
