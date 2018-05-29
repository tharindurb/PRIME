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

import emotion.EmotionExtractor;
import javafx.util.Pair;
import nlptasks.SentenceSplitter;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import util.Utils;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class TreatmentTimelineExtractor {

      public static void main(String... args) throws Exception {

        String folderPath = "D:\\La Trobe Projects\\Health Forums\\TextFiles\\TestProfile\\New folder";
        //   String folderPath = "./ProstateProfiles/";
        String[] typeList = new String[]{"prostatecanceruk", "cancerforums", "healingwell", "healthboards", "macmillanuk",  "cancercompass", "csn"};

        TreatmentTimelineExtractor pipeline = new TreatmentTimelineExtractor();
        pipeline.processProfiles(folderPath);
        System.out.println("Processed ... " );
        //  }
    }

    public static void getTreatmentTypeExtracted(JSONObject profile){

        try {
            if(profile.has("TreatmentTimelineInfo")) profile.remove("TreatmentTimelineInfo");
            extractDecisionInfo(profile);
        } catch (Exception e) {
            e.printStackTrace();
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
                Date o1Date = Utils.dateFormat.parse(o1.getString("PostDate"));
                Date o2Date = Utils.dateFormat.parse(o2.getString("PostDate"));
                return o1Date.compareTo(o2Date);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });

        return sortedPosts;
    }


    private void processProfiles(String folderPath) {

        File folder = new File(folderPath);
        File[] fileList = folder.listFiles();
        // **** to write to JSON file ***//
        BufferedWriter bw = null;
        File fileNew = new File("D:\\La Trobe Projects\\Health Forums\\TextFiles\\test4.json");

        FileWriter fw = null;
        try {
            fw = new FileWriter(fileNew);
        } catch (IOException e) {
            e.printStackTrace();
        }
        bw = new BufferedWriter(fw);


        if (fileList != null) {
            System.out.println("No of authors: " + fileList.length);
            for (File file : fileList) {
                if (file != null && file.isFile()) try {
                    String sLine;
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new FileReader(file));

                    while ((sLine = reader.readLine()) != null) sb.append(sLine);
                    reader.close();
                    JSONObject profile = new JSONObject(sb.toString());
                    extractDecisionInfo(profile);
                    bw.write(profile.toString(2) + ",");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Output File written Successfully");
    }


    private static void extractDecisionInfo(JSONObject profile) throws Exception {
        JSONObject treatmentTimelineInfo = new JSONObject();

        JSONArray posts = profile.getJSONArray("Posts");

        System.out.println("treatment timeline extracting: " + profile.getString("AuthorID"));
        List<JSONObject> sortedPosts = sortPostsByDate(posts);

        JSONObject post = null;
        int postIndex = 0;
        String treatmentKeyWord = "";

        for (int i = 0; i < sortedPosts.size(); i++) {
            JSONObject treatmentTypeProfile = new JSONObject();

            String matchKeyWord = "";
            TreatmentTypeExtractor.getTreatmentTypeProfileSinglePost(treatmentTypeProfile, sortedPosts.get(i).getString("Content"));
            JSONArray treatmentKeywords = null;

            if(treatmentTypeProfile.has("TreatmentTypeInfo") && treatmentTypeProfile.getJSONObject("TreatmentTypeInfo").has("TreatmentTypeKeywords")) {

                treatmentKeywords = treatmentTypeProfile.getJSONObject("TreatmentTypeInfo").getJSONArray("TreatmentTypeKeywords");
            }

            if (treatmentKeywords != null && treatmentKeywords.length()>0) {
                for(int key=0; key< treatmentKeywords.length(); key++) {
                    String keyWord  = treatmentKeywords.getString(key);
                    for(String sentence: SentenceSplitter.getSentenceSplitter().sentDetect(sortedPosts.get(i).getString("Content"))) {
                        matchKeyWord = DecisionConfirmationExtractor.processPostToExtractSurgeryDecision(keyWord, sentence);
                        if (matchKeyWord != "") break;
                    }
                    if (matchKeyWord != "") break;
                }

                if(matchKeyWord != ""){
                    post = sortedPosts.get(i);
                    treatmentKeyWord = matchKeyWord;
                    postIndex = i;
                    break;
                }
            }
        }

        if(post != null){
            treatmentTimelineInfo.put("Verified", 1);
            treatmentTimelineInfo.put("DateOfTreatment", post.getString("PostDate"));
        }
        else {
            treatmentTimelineInfo.put("Verified", 0);
        }


        // Add verified and post date
        if(post != null) {

            Map<String, List<Pair<String, Pattern>>> featureMap = TreatmentTypeExtractor.getTreatmentTypes();

            for (String keyWord : featureMap.keySet()) {
                boolean m = false;
                for (Pair<String, Pattern> key : featureMap.get(keyWord)) {

                    if (key.getKey().equalsIgnoreCase(treatmentKeyWord)) {

                        treatmentTimelineInfo.put("TreatmentType", keyWord);

                        m = true;
                    }
                    if (m) break;
                }
                if (m) break;
            }
        }

        // get timeline emotions and side effects
        if(post != null) {
            String dateOfTreatment = treatmentTimelineInfo.getString("DateOfTreatment");
            Map<String,List<JSONObject>> timelineBinnedPosts = TimelineInfo.getTimeLineBinnedPosts(posts,dateOfTreatment);
            for(Map.Entry<String,List<JSONObject>> entry: timelineBinnedPosts.entrySet()){
                    addTimelineSurgeryEmotionInfo(entry.getValue(),entry.getKey(), treatmentTimelineInfo);
                    addTimelineSurgerySideEffectsInfo(profile,  entry.getValue(),entry.getKey(), treatmentTimelineInfo);
                          }
            profile.put("TreatmentTimelineInfo", treatmentTimelineInfo);
        }
    }

    private static void addTimelineSurgerySideEffectsInfo(JSONObject profile, List<JSONObject> posts, String duration, JSONObject surgeryTypeInfo) {
        List<String> extractedPosts = new ArrayList<String>();
        for(JSONObject extractedPost: posts) {
            String cleanPost = "";
            try {
                cleanPost = Utils.removePunctuations(extractedPost.getString("Content"));
                cleanPost = Utils.removeUnicode(cleanPost);
                extractedPosts.add(cleanPost);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            JSONObject sideEffectsAggregated = SideEffectExtractor.getSideEffectsProfileJSONForTimeline(extractedPosts);
            if (sideEffectsAggregated.has("SideEffectType")) {
                surgeryTypeInfo.put(duration + " SideEffects", sideEffectsAggregated);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static void addTimelineSurgeryEmotionInfo(List<JSONObject> posts, String duration, JSONObject surgeryTypeInfo){
        List<String> extractedPosts = new ArrayList<String>();
        for(JSONObject extractedPost: posts) {
            String cleanPost = "";
            try {
                cleanPost = Utils.removeUnicode(Utils.removePunctuations(extractedPost.getString("Content")));
                extractedPosts.add(cleanPost);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            surgeryTypeInfo.put(duration + " Emotions", EmotionExtractor.getEmotionsJSON(extractedPosts));
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private static String getMostlyUsedSurgeryKeyWord(String surgeryKeywords){
        Map<String, Integer> keywordMap = new HashMap<>();

        if (surgeryKeywords != "") {
            String[] keywords = surgeryKeywords.split(",");
            for (String key : keywords) {
                if (keywordMap.containsKey(key)) {
                    int value = keywordMap.get(key);
                    keywordMap.replace(key, value + 1);
                } else {
                    keywordMap.put(key, 1);
                }
            }
        }

        int maxValue = 0;

        if (!keywordMap.isEmpty()) {
            maxValue = Collections.max(keywordMap.values());
        }

        String surgeryKeyWord = "";
        boolean check = false;
        for (String o : keywordMap.keySet()) {
            if (keywordMap.get(o).equals(maxValue)) {
                surgeryKeyWord = o;
                check = true;
            }
            if (check) break;
        }

        return surgeryKeyWord;
    }
}

