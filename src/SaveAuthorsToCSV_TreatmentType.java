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

import clinical_info.SideeffectToCSVHelper;
import clinical_info.SurgeryDecisionCategory;
import emotion.EmotionToCSVHelper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SaveAuthorsToCSV_TreatmentType {

    public static void main(String... args) throws IOException, JSONException {
        toCSVFromFolder();
    }

    private static void toCSVFromFolder() throws IOException, JSONException {
        final Properties pipelineProperties = new Properties();
        FileInputStream input = new FileInputStream("./pipelineProperties");
        pipelineProperties.load(input);
        input.close();
        String[] typeList = pipelineProperties.getProperty("types").trim().split(",");
        String folderPath = pipelineProperties.getProperty("folderPath").trim();
//        for (String sideEffect : sideEffectList) sideEffectMap.put(sideEffect.trim(), 0);
        String[] title = getTitle();
        CSVPrinter csvPrinter = new CSVPrinter(new FileWriter("author data [treatment type].csv"), CSVFormat.RFC4180.withDelimiter(',').withHeader(title));
        int nDocsParsed = 0;


        for (String type : typeList) {
            type = type.trim();
            File folder = new File(folderPath + type);
            File[] fileList = folder.listFiles();

            for (File file : fileList) {
                if (file.isFile()) {
                    JSONObject profile = Utils.getJSONFromFile(file);
                    formatProfile(profile);
                    List<Object> values = getCSVRecord(profile, type);
                    csvPrinter.printRecord(values);
                    nDocsParsed++;
                    if (nDocsParsed % 100 == 0) {
                        System.out.println("Authors parsed: " + nDocsParsed);
                        csvPrinter.flush();
                    }
                }
            }
            csvPrinter.flush();
        }
        csvPrinter.close();
    }

    private static void formatProfile(JSONObject profile) throws JSONException {
        JSONObject profileDemographics = profile.getJSONObject("ProfileDemographics");
//        System.out.println("formatting: " + profile.getString("AuthorID"));

        boolean hasProfileAge = (profileDemographics.getJSONObject("Age").getDouble("Value") > 0) ? true : false;
        boolean hasProfileGender = (!profileDemographics.getJSONObject("Gender").getString("Gender").equalsIgnoreCase("unknown")) ? true : false;
//        String profileNarrationType = profileDemographics.getJSONObject("Gender").getString("NarrationType");

        JSONObject narrator = new JSONObject();

        if (hasProfileAge && (!profileDemographics.getJSONObject("Age").getString("Relation").equalsIgnoreCase("second-person"))) {
            narrator.put("Age", profileDemographics.getJSONObject("Age").getDouble("Value"));
            narrator.put("HasAge", 1);
        } else {
            narrator.put("HasAge", 0);
        }

        if (hasProfileGender && (!profileDemographics.getJSONObject("Gender").getString("NarrationType").equalsIgnoreCase("second-person"))) {
            narrator.put("Gender", profileDemographics.getJSONObject("Gender").getString("Gender"));
            narrator.put("HasGender", 1);
        } else {
            narrator.put("HasGender", 0);
        }

        profile.remove("ProfileDemographics");
        profile.put("ProfileDemographics", narrator);
    }

    private static List<Object> getCSVRecord(JSONObject doc, String forumName) {
        List<Object> values = new ArrayList<>();

        try {
            values.add(doc.getString("AuthorID"));

            values.add(forumName);

            if (doc.has("ProfileDemographics")) {
                JSONObject profileDemo = doc.getJSONObject("ProfileDemographics");
                if (profileDemo.getInt("HasGender") == 1) {
                    values.add(profileDemo.getString("Gender"));
                } else {
                    values.add("Unknown");
                }

                if (profileDemo.getInt("HasAge") == 1) {
                    values.add(profileDemo.getInt("Age"));
                } else {
                    values.add(new Integer(-1));
                }

                if (profileDemo.getInt("HasAge") == 1) {
                    int age = profileDemo.getInt("Age");
                    if (20 < age && age <= 40) {
                        values.add("le 40");
                    } else if (40 < age && age <= 50) {
                        values.add("41-50");
                    } else if (50 < age && age <= 60) {
                        values.add("51-60");
                    } else if (60 < age && age <= 70) {
                        values.add("61-70");
                    } else if (age > 70) {
                        values.add("ge 70");
                    } else {
                        values.add("NA");
                    }
                } else {
                    values.add(new Integer(-1));
                }

            } else {
                values.add("Unknown");
                values.add(new Integer(-1));
            }

            values.add(doc.getInt("NPosts"));
            values.add(doc.getInt("NExperiencePosts"));
            values.add(doc.getInt("WordCount"));

                      if (doc.has("GleasonInfo")) {
                JSONObject gleasonInfo = doc.getJSONObject("GleasonInfo");

                if (gleasonInfo.has("min")) {
                    values.add(gleasonInfo.getInt("min"));
                } else {
                    values.add(-1);
                }

                if (gleasonInfo.has("max")) {
                    values.add(gleasonInfo.getInt("max"));
                } else {
                    values.add(-1);
                }

            } else {
                values.add(-1);
                values.add(-1);
            }

            if (doc.has("PSAInfo")) {
                JSONObject psaInfo = doc.getJSONObject("PSAInfo");

                if (psaInfo.has("min")) {
                    values.add(psaInfo.getDouble("min"));
                } else {
                    values.add(-1);
                }

                if (psaInfo.has("max")) {
                    values.add(psaInfo.getDouble("max"));
                } else {
                    values.add(-1);
                }

            } else {
                values.add(-1);
                values.add(-1);
            }

            if (doc.has("TreatmentTypeInfo")) {
                values.add(doc.getJSONObject("TreatmentTypeInfo").getString("TreatmentType"));
                JSONArray treatmentType = doc.getJSONObject("TreatmentTypeInfo").getJSONArray("TreatmentMentions");
                String treatmentString = "";

                for (int i = 0; i < treatmentType.length(); i++) {
                    treatmentString = treatmentString + treatmentType.getString(i) + ",";
                }

                if (treatmentType.length() > 0)
                    treatmentString = treatmentString.substring(0, treatmentString.length() - 1);

                values.add(treatmentString.trim());
            } else {
                values.add("");
                values.add("");
            }

            if (doc.has("SurgeryTypeInfo") && doc.getJSONObject("SurgeryTypeInfo").has("SurgeryType")) {
                values.add(doc.getJSONObject("SurgeryTypeInfo").getString("SurgeryType"));
                //  values.add(doc.getJSONObject("SurgeryTypeInfo").getString("Verified"));

                JSONArray surgeryMentions = doc.getJSONObject("SurgeryTypeInfo").getJSONArray("SurgeryMentions");
                String surgeryString = "";

                for (int i = 0; i < surgeryMentions.length(); i++) {
                    surgeryString = surgeryString + surgeryMentions.getString(i) + ",";
                }

                if (surgeryMentions.length() > 0)
                    surgeryString = surgeryString.substring(0, surgeryString.length() - 1);

                values.add(surgeryString.trim());

            } else {
                values.add("");
                values.add("");
            }

            if (doc.has("SurgeryTypeInfo")) {
                values.add(doc.getJSONObject("SurgeryTypeInfo").getInt("Verified"));
            } else {
                values.add(0);
            }


            {
//                for (Map.Entry<String, Integer> entry : sideEffectMap.entrySet()) entry.setValue(0);
                if (doc.has("SideEffectTypeInfo")) {
                    JSONArray sideEffectArray = doc.getJSONObject("SideEffectTypeInfo").getJSONArray("SideEffectType");
                    SideeffectToCSVHelper.addSideEffectInfo(sideEffectArray,values);
                }else {
                    SideeffectToCSVHelper.addZeroSideEffectInfo(values);
                }

//                for (Map.Entry<String, Integer> entry : sideEffectMap.entrySet()) values.add(entry.getValue());
            }

            {
                if (doc.has("EmotionInfo")) {
                    JSONObject emotionInfo = doc.getJSONObject("EmotionInfo").getJSONObject("Emotions");
                    EmotionToCSVHelper.addEmotionInfo(emotionInfo,values);

                } else {
                    EmotionToCSVHelper.addZeroEmotionInfo(values);
                }
            }

            if(doc.has("TreatmentTimelineInfo")
                    && doc.getJSONObject("TreatmentTimelineInfo").has("Verified")
                    && doc.getJSONObject("TreatmentTimelineInfo").getInt("Verified") ==1){
                values.add(doc.getJSONObject("TreatmentTimelineInfo").getString("TreatmentType"));
            }else {
                values.add("NA");
            }

            {
                JSONObject dR = doc.getJSONObject("TreatmentDecision").getJSONObject("DoctorRecommended");
                JSONObject pD = doc.getJSONObject("TreatmentDecision").getJSONObject("PatientDecided");
                if(dR.getInt("Value")==1 && pD.getInt("Value")==1){
                    values.add("Doctor Recommended and Patient Decided");
                }
                else if(dR.getInt("Value")==1 && pD.getInt("Value")==0){
                    values.add("Doctor Recommended");
                }else if(dR.getInt("Value")==0 && pD.getInt("Value")==1){
                    values.add("Patient Decided");
                }else {
                    values.add("NA");
                }

                for(String category: SurgeryDecisionCategory.getHeader()){

                    int dRv = dR.getInt(category);
                    int pDv = pD.getInt(category);

                    if((dRv + pDv) >  0) {
                        values.add(1);
                    }else {
                        values.add(0);
                    }
                }
            }

//            JSONObject dR = doc.getJSONObject("TreatmentDecision").getJSONObject("DoctorRecommended");
//            values.add(dR.getInt("Value"));
//            for(String category: SurgeryDecisionCategory.getHeader()){
//                values.add(dR.getInt(category));
//            }
//
//            if(dR.has("docMentions")) {
//                values.add(dR.getString("docMentions"));
//            }else {
//                values.add(" ");
//            }
//
//            JSONObject pD = doc.getJSONObject("TreatmentDecision").getJSONObject("PatientDecided");
//            values.add(pD.getInt("Value"));
//            for(String category: SurgeryDecisionCategory.getHeader()){
//                values.add(pD.getInt(category));
//            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return values;
    }

    private static String[] getTitle() {
        List<String> titles = new ArrayList<>();

        String[] domographics = new String[]{"AuthorID", "ForumName", "Gender", "Age", "AgeGroup", "NPosts", "NExperiencePosts", "WordCount",
                "G:min", "G:max", "PSA:min", "PSA:max", "TreatmentType", "TreatmentMentions", "SurgeryType", "SurgeryMentions", "Surgery: Verified"};

        for (String s : domographics) titles.add(s);
        for (String s : SideeffectToCSVHelper.getSideEffectHeader()) titles.add(s);
        for (String s : EmotionToCSVHelper.getEmotionHeader()) titles.add(s);
        titles.add("Verified Treatment");
        titles.add("Treatment Decision");
        for(String category: SurgeryDecisionCategory.getHeader()) titles.add("R: " + category);
        String[] titleArray = new String[titles.size()];
        titles.toArray(titleArray);
        return titleArray;
    }
}
