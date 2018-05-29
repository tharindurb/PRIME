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
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import util.Utils;

import java.io.*;
import java.util.*;

public class SurgeryTimelineExtractor {

    public static void main(String... args) throws Exception {
//        String folderPath = "D:\\La Trobe Projects\\Health Forums\\TextFiles\\TestProfile\\New folder\\";
//        String[] typeList = new String[]{"A", "B"};
        final Properties pipelineProperties = new Properties();
        FileInputStream input = new FileInputStream("./pipelineProperties");
        pipelineProperties.load(input);
        input.close();
        String[] typeList = pipelineProperties.getProperty("types").trim().split(",");
        String folderPath = "./ProstateProfiles/";
       // String[] typeList = new String[]{"prostatecanceruk", "cancerforums", "healingwell", "healthboards", "macmillanuk", "cancercompass", "csn"};
        SurgeryTimelineExtractor pipeline = new SurgeryTimelineExtractor();
        for (String type : typeList) {
            type = type.trim();
            pipeline.processProfiles(folderPath + type);
            System.out.println("Processing ... " + folderPath + type);
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
                    extractSurgeryInfo(profile);
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write(profile.toString(2));
                    writer.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Processed ... ");
    }


    private static void extractSurgeryInfo(JSONObject profile) throws Exception {

        JSONObject surgeryTimelineInfo = new JSONObject();

        // remove previous timeline info
        if (profile.has("SurgeryTypeInfo")) {
            JSONObject info = profile.getJSONObject("SurgeryTypeInfo");
            List<TimeBin> timelineMap = TimelineInfo.getTimelineInfo();
            for (TimeBin bin : timelineMap) {
                if (info.has(bin.label + " Emotions")) {
                    info.remove(bin.label + " Emotions");
                }

                if (info.has(bin.label + " SideEffects")) {
                    info.remove(bin.label + " SideEffects");
                }
            }
        }

        JSONArray posts = profile.getJSONArray("Posts");
        System.out.println("surgery timeline extracting: " + profile.getString("AuthorID"));

        // get timeline emotions and side effects
        if (profile.has("SurgeryTypeInfo") && profile.getJSONObject("SurgeryTypeInfo").has("Verified")) {
            if (profile.getJSONObject("SurgeryTypeInfo").getInt("Verified") == 1) {

                String dateOfSurgery = profile.getJSONObject("SurgeryTypeInfo").getString("DateOfSurgery");
                Map<String, List<JSONObject>> timelineBinnedPosts = TimelineInfo.getTimeLineBinnedPosts(posts, dateOfSurgery);
                for (Map.Entry<String, List<JSONObject>> entry : timelineBinnedPosts.entrySet()) {
                    addTimelineSurgeryEmotionInfo(entry.getValue(), entry.getKey(), surgeryTimelineInfo);
                    addTimelineSurgerySideEffectsInfo(profile, entry.getValue(), entry.getKey(), surgeryTimelineInfo);
                }
                profile.put("SurgeryTimeLineInfo", surgeryTimelineInfo);
            }
        }
    }

    private static void addTimelineSurgerySideEffectsInfo(JSONObject profile, List<JSONObject> posts, String duration, JSONObject surgeryTypeInfo) {
        List<String> extractedPosts = new ArrayList<String>();
        for (JSONObject extractedPost : posts) {
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

    private static void addTimelineSurgeryEmotionInfo(List<JSONObject> posts, String duration, JSONObject surgeryTypeInfo) {
        List<String> extractedPosts = new ArrayList<String>();
        for (JSONObject extractedPost : posts) {
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
}

