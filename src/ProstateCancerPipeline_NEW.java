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

import clinical_info.*;
import demographic.*;
import emotion.EmotionExtractor;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import util.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class ProstateCancerPipeline_NEW {
    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    long ageExtractionTime = 0;
    long genderExtractionTime = 0;
    int totalPosts = 0;
    int totalWords = 0;
    int totalProfiles = 0;
    private final AgeClassifier ageClassifier = new AgeClassifier();


    public static void main(String... args) throws Exception {
        ProstateCancerPipeline_NEW pipeline = new ProstateCancerPipeline_NEW();

        long start = System.currentTimeMillis();
        final Properties pipelineProperties = new Properties();
        FileInputStream input = new FileInputStream("./pipelineProperties");
        pipelineProperties.load(input);
        input.close();
        String folderPath = pipelineProperties.getProperty("folderPath").trim();
        String[] typeList = pipelineProperties.getProperty("types").trim().split(",");
        Set<String> options = new HashSet<>();
        for (String s : pipelineProperties.getProperty("options").trim().split(",")) options.add(s.trim());

        for (String type : typeList) {
            type = type.trim();
            pipeline.processProfiles(folderPath + type, options);
        }

        long end = System.currentTimeMillis();
        double time = (end - start)/(double)1000;

        BufferedWriter bw = new BufferedWriter(new FileWriter("processing_times_" + end));
        System.out.println("Total time taken: " + time);
        bw.write("Age classifier: " +pipeline.ageExtractionTime); bw.newLine();
        bw.write("Gender classifier: " +pipeline.genderExtractionTime); bw.newLine();
        bw.write("Total profiles processed: " +pipeline.totalProfiles); bw.newLine();
        bw.write("Total posts processed: " +pipeline.totalPosts); bw.newLine();
        bw.write("Total words processed: " +pipeline.totalWords); bw.newLine();
        bw.write("Total time taken: " + time);
        bw.close();

    }

    private void processProfiles(String folderPath, Set<String> options) {
        File folder = new File(folderPath);
        File[] fileList = folder.listFiles();

        for (File file : fileList) {
            if (file.isFile()) {

                try {
                    JSONObject profile = Utils.getJSONFromFile(file);

                    //extract demographics
                    if (options.contains("all") || options.contains("demographics")) {
                        extractDemographics(profile);
                        if (profile.has("ProfileDemographics")) profile.remove("ProfileDemographics");
                        JSONObject profileDemographics = AggregateDemographics_ProstateCancer.getProfileDemographics(profile);
                        profile.put("ProfileDemographics", profileDemographics);
                    }

                    List<String> experiencedPosts = Utils.getExperiencePostContent(profile);
                    List<String> allPosts = Utils.getAllPostContent(profile);
                    String about = "";
                    if (profile.has("About")) about = profile.getString("About");

                    //extract gleason score
                    if (options.contains("all") || options.contains("gleason")) {
                        GleasonScoreExtractor.getGleasonScoreProfileJSON(profile, allPosts, about);
                    }

                    //extract treatment type
                    if (options.contains("all") || options.contains("treatmentType")) {
                        TreatmentTypeExtractor.getTreatmentTypeProfileJSON(profile, experiencedPosts, about);
                    }

                    //extract side effects
                    if (options.contains("all") || options.contains("sideEffects")) {
                        SideEffectExtractor.getSideEffectsProfileJSON(profile, experiencedPosts, about);
                    }

                  //extract emotions
                    if (options.contains("all") || options.contains("emotions")) {
                        EmotionExtractor.getEmotionsProfileJSON(profile, experiencedPosts);
                    }


                    if (options.contains("all") || options.contains("postTypeInfo")) {
                        PostTypeCountExtractor.extractPostTypeInfoTreatment(profile);
                    }

                    if (options.contains("all") || options.contains("treatmentConfirmation")) {
                        TreatmentTimelineExtractor.getTreatmentTypeExtracted(profile);
                    }

                    if(options.contains("all") || options.contains("treatmentDecision")){
                        TreatmentDecisionExtractor.extractTreatmentDecision(profile);
                    }

//                 extract PSA
                    if (options.contains("all") || options.contains("psa")) {
                        PSAExtractor.getPSAProfileJSON(profile, allPosts, about);
                    }

                    //add counts
                    int wordCount = getWordCount(profile);
                    int postCount = profile.getJSONArray("Posts").length();
                    profile.put("NPosts", postCount);
                    profile.put("NExperiencePosts", experiencedPosts.size());
                    profile.put("WordCount", wordCount);
                    totalWords += wordCount;
                    totalPosts += postCount;
                    ++totalProfiles;

                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write(profile.toString(2));
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        }
    }

    private void extractDemographics(JSONObject profile) throws JSONException {
        JSONArray posts = profile.getJSONArray("Posts");
        System.out.println("demographic extracting: " + profile.getString("AuthorID"));

        if(profile.has("About")) {
            if(profile.has("DemographicsAboutMe")) profile.remove("DemographicsAboutMe");
            profile.put("DemographicsAboutMe", getDemographics(profile.getString("About")));
        }

        for (int i = 0; i < posts.length(); i++) {
            JSONObject post = posts.getJSONObject(i);
            if(post.has("Demographics")) post.remove("Demographics");

            String content = post.getString("Content");

            if(post.getInt("MessageIndex") == 0){
                content = post.getString("Title") + " " + content;
            }

            post.put("Demographics", getDemographics(content));
        }
    }

    private JSONObject getDemographics(String text) throws JSONException {
//        System.out.println(text);
        JSONObject demographics = new JSONObject();

        long t1= System.nanoTime();
        demographics.put("AgeInfo", ageClassifier.getHighConfidenceAgeMentions(text));
        long t2= System.nanoTime();
        demographics.put("GenderInfo", GenderClassifier.getGenderInformation(text));
        long t3= System.nanoTime();
        ageExtractionTime = ageExtractionTime +  (t2 - t1);
        genderExtractionTime = genderExtractionTime + (t3 - t2);
        return demographics;
    }


    private int getWordCount(JSONObject profile) throws JSONException {
        int wordCount = 0;
        JSONArray posts = profile.getJSONArray("Posts");

        for (int i = 0; i < posts.length(); i++) {
            JSONObject post = posts.getJSONObject(i);
            String content = post.getString("Content");
            if (post.getInt("MessageIndex") == 0) {
                content = post.getString("Title") + " " + content;
            }
            wordCount += Utils.countWords(content);
        }
        return wordCount;
    }

}
