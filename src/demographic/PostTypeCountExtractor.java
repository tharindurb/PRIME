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
package demographic;

import clinical_info.TimelineInfo;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import util.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PostTypeCountExtractor {
    public final static Map<String, String> timeline = Utils.getTimelineInfo();
//    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public static void main(String... args) throws Exception {

        String folderPath = "C:\\TextClustering\\HealthForums\\Prostate Cancer\\Surgery Type work\\ProstateProfiles\\";
        //   String folderPath = "./ProstateProfiles/";
        String[] typeList = new String[]{"test"};

        PostTypeCountExtractor postTypeCountExtractor = new PostTypeCountExtractor();

        for (String type : typeList) postTypeCountExtractor.processProfiles(folderPath + type);
        System.out.println("Processed ... ");
        //  }
    }

    private void processProfiles(String folderPath) {
        File folder = new File(folderPath);
        File[] fileList = folder.listFiles();
        for (File file : fileList) {
            if (file.isFile()) {

                try {
                    JSONObject profile = Utils.getJSONFromFile(file);
                    extractPostTypeInfoTreatment(profile);
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write(profile.toString(2));
                    writer.close();

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void extractPostTypeInfoStartDate(JSONObject profile) throws Exception {

        System.out.println("PostType extracting: " + profile.getString("AuthorID"));
        if (profile.has("PostSpanInfo")) {
            JSONArray posts = profile.getJSONArray("Posts");
            String dateOfTreatment = profile.getJSONObject("PostSpanInfo").getString("firstPostDate");
            JSONObject postTypeInfo = new JSONObject();

            Map<String, List<JSONObject>> timelineBinnedPosts = TimelineInfo.getTimeLineBinnedPosts(posts, dateOfTreatment);
            for (Map.Entry<String, List<JSONObject>> entry : timelineBinnedPosts.entrySet()) {
                int eCount = 0;
                int aCount = 0;
                for (JSONObject post : entry.getValue()) {
                    if (post.getJSONObject("Demographics").getJSONObject("GenderInfo").
                            getJSONObject("NounBasedGender").getString("NarrationType").equalsIgnoreCase("advice")) {
                        ++aCount;
                    } else {
                        ++eCount;
                    }
                }

                JSONObject postTypeInfoBin = new JSONObject();
                postTypeInfoBin.put("NExperience", eCount);
                postTypeInfoBin.put("NAdvice", aCount);
                int total = entry.getValue().size();
                postTypeInfoBin.put("NTotal", total);
                if (total > 0) {
                    postTypeInfoBin.put("PExperience", eCount / (double) total);
                    postTypeInfoBin.put("PAdvice", aCount / (double) total);
                } else {
                    postTypeInfoBin.put("PExperience", 0.0);
                    postTypeInfoBin.put("PAdvice", 0.0);
                }
                postTypeInfo.put(entry.getKey() + " PostTypeInfo", postTypeInfoBin);

            }

            profile.put("PostTypeInfo", postTypeInfo);
        }
    }

    public static void extractPostTypeInfoTreatment(JSONObject profile) throws Exception {

        System.out.println("PostType extracting: " + profile.getString("AuthorID"));
        if (profile.has("TreatmentTimelineInfo") && profile.getJSONObject("TreatmentTimelineInfo").has("Verified") && profile.getJSONObject("TreatmentTimelineInfo").getInt("Verified") == 1) {
            JSONArray posts = profile.getJSONArray("Posts");
            String dateOfTreatment = profile.getJSONObject("TreatmentTimelineInfo").getString("DateOfTreatment");
            JSONObject postTypeInfo = new JSONObject();

            Map<String, List<JSONObject>> timelineBinnedPosts = TimelineInfo.getTimeLineBinnedPosts(posts, dateOfTreatment);
            for (Map.Entry<String, List<JSONObject>> entry : timelineBinnedPosts.entrySet()) {
                int eCount = 0;
                int aCount = 0;
                for (JSONObject post : entry.getValue()) {
                    if (post.getJSONObject("Demographics").getJSONObject("GenderInfo").
                            getJSONObject("NounBasedGender").getString("NarrationType").equalsIgnoreCase("advice")) {
                        ++aCount;
                    } else {
                        ++eCount;
                    }
                }

                JSONObject postTypeInfoBin = new JSONObject();
                postTypeInfoBin.put("NExperience", eCount);
                postTypeInfoBin.put("NAdvice", aCount);
                int total = entry.getValue().size();
                postTypeInfoBin.put("NTotal", total);
                if (total > 0) {
                    postTypeInfoBin.put("PExperience", eCount / (double) total);
                    postTypeInfoBin.put("PAdvice", aCount / (double) total);
                } else {
                    postTypeInfoBin.put("PExperience", 0.0);
                    postTypeInfoBin.put("PAdvice", 0.0);
                }
                postTypeInfo.put(entry.getKey() + " PostTypeInfo", postTypeInfoBin);

            }

            profile.put("PostTypeInfo", postTypeInfo);
        }
    }
}
