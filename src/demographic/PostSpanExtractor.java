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
package demographic;

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

import static clinical_info.TimelineInfo.sortPostsByDate;
public class PostSpanExtractor {
    public final static Map<String, String> timeline = Utils.getTimelineInfo();
//    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public static void main(String... args) throws Exception {

        String folderPath = "C:\\TextClustering\\HealthForums\\Prostate Cancer\\ProstateProfiles\\";
        //   String folderPath = "./ProstateProfiles/";
        String[] typeList = new String[]{"test"};

        PostSpanExtractor postSpanExtractor = new PostSpanExtractor();

        for(String type: typeList) postSpanExtractor.processProfiles(folderPath + type);
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
                    extractPostSpanInfoTreatment(profile);
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

    public static void extractPostSpanInfoTreatment(JSONObject profile) throws Exception {

        System.out.println("PostSpan extracting: " + profile.getString("AuthorID"));

        JSONArray posts = profile.getJSONArray("Posts");
        List<JSONObject> sortedPosts = sortPostsByDate(posts);
        JSONObject postSpanInfo = new JSONObject();
        if(!sortedPosts.isEmpty()){
            String firstPost = sortedPosts.get(0).getString("PostDate");
            String lastPost  = sortedPosts.get(sortedPosts.size()-1).getString("PostDate");

            postSpanInfo.put("firstPostDate",firstPost);
            postSpanInfo.put("lastPostDate",lastPost);
            int dateDiff = Utils.getDateDifferece(firstPost, lastPost) + 1;
            postSpanInfo.put("postSpan",dateDiff);
        }
        profile.put("PostSpanInfo", postSpanInfo);
    }
}
