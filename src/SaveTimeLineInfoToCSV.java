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

import clinical_info.SideeffectToCSVHelper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SaveTimeLineInfoToCSV {
    private static final Map<String, String> timelineInfo = Utils.getTimelineInfo();

    public static void main(String... args) throws IOException, JSONException {
        String timelineType;
        if(args.length>0){
            timelineType = args[0].trim();
        }else {
            timelineType = "surgery";
        }
       toCSVFromFolder(timelineType);
    }

    private static void toCSVFromFolder(String timelineType) throws IOException, JSONException {
        final Properties pipelineProperties = new Properties();
        FileInputStream input = new FileInputStream("./pipelineProperties");
        pipelineProperties.load(input);
        input.close();
        String[] typeList = pipelineProperties.getProperty("types").trim().split(",");
        String folderPath = pipelineProperties.getProperty("folderPath").trim();
        String[] title = getTitle();
        CSVPrinter csvPrinter = new CSVPrinter(new FileWriter("author timeline data " + timelineType + ".csv"), CSVFormat.RFC4180.withDelimiter(',').withHeader(title));
        int nDocsParsed = 0;


        for (String type : typeList) {
            type = type.trim();
            File folder = new File(folderPath + type);
            File[] fileList = folder.listFiles();

            for (File file : fileList) {
                if (file.isFile()) {
                    JSONObject profile = Utils.getJSONFromFile(file);
                    formatProfile(profile);
                    for (String key : timelineInfo.keySet()) {
                        List<Object> values = getCSVRecord(profile, key,timelineType, type);
                        csvPrinter.printRecord(values);
                    }

                    nDocsParsed++;
                    if (nDocsParsed % 100 == 0) {
                        System.out.println("Timeline - Authors parsed: " + nDocsParsed);
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

        boolean hasProfileAge = (profileDemographics.getJSONObject("Age").getDouble("Value") > 0) ? true : false;
        boolean hasProfileGender = (!profileDemographics.getJSONObject("Gender").getString("Gender").equalsIgnoreCase("unknown")) ? true : false;

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

    private static List<Object> getCSVRecord(JSONObject doc, String timelineKey, String timelineType, String forumName) {
        List<Object> values = new ArrayList<>();

        try {
            values.add(timelineKey);
            values.add(forumName + "_" + doc.getString("AuthorID"));
            values.add(doc.getString("AuthorID"));

            JSONObject info = new JSONObject();


            if (timelineType.equalsIgnoreCase("surgery") && doc.has("SurgeryTypeInfo")) {
                info = doc.getJSONObject("SurgeryTypeInfo");
            }else if(timelineType.equalsIgnoreCase("treatment") && doc.has("TreatmentTimelineInfo")){
                info = doc.getJSONObject("TreatmentTimelineInfo");
            }

            if(info.has("Verified")){
                values.add(info.getInt("Verified"));
            } else {
                values.add(0);
            }

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
            }else {
                values.add("Unknown");
                values.add(new Integer(-1));
            }

            if (doc.has("GleasonInfo")) {
                JSONObject gleasonInfo = doc.getJSONObject("GleasonInfo");

                if (gleasonInfo.has("min")) {
                    values.add(gleasonInfo.getInt("min"));
                } else {
                    values.add(-1);
                }

            } else {
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

            if (doc.has("SurgeryTypeInfo")) {
                JSONArray treatmentType = doc.getJSONObject("SurgeryTypeInfo").getJSONArray("SurgeryMentions");
                String treatmentString = "";

                for (int i = 0; i < treatmentType.length(); i++) {
                    treatmentString = treatmentString + treatmentType.getString(i) + ",";
                }

                if (treatmentType.length() > 0)
                    treatmentString = treatmentString.substring(0, treatmentString.length() - 1);

                values.add(treatmentString.trim());
            } else {
              //  values.add("");
                values.add("");
            }

            if (timelineType.equalsIgnoreCase("surgery") && info.has("SurgeryType")) {
                values.add(info.getString("SurgeryType"));
            }else if (timelineType.equalsIgnoreCase("treatment") && info.has("TreatmentType")) {
                values.add(info.getString("TreatmentType"));
            }else {
                values.add("");
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
            }
            if (info.has("Verified") && info.getInt("Verified") == 1) {
                String emotionsName = timelineKey + " Emotions";
                // surgery
                if(timelineType.equalsIgnoreCase("surgery") && doc.has("SurgeryTimeLineInfo")){
                    JSONObject surgeryTimeline = doc.getJSONObject("SurgeryTimeLineInfo");
                    if (surgeryTimeline.has(emotionsName)) {
                        JSONObject emotions = surgeryTimeline.getJSONObject(emotionsName).getJSONObject("Emotions");
                        EmotionToCSVHelper.addEmotionInfo(emotions, values);
                    } else {
                        EmotionToCSVHelper.addNAEmotionInfo(values);
                    }
                }
                // treatment
                if (timelineType.equalsIgnoreCase("treatment")) {
                    if (info.has(emotionsName)) {
                        JSONObject emotions = info.getJSONObject(emotionsName).getJSONObject("Emotions");
                        EmotionToCSVHelper.addEmotionInfo(emotions, values);
                    } else {
                        EmotionToCSVHelper.addNAEmotionInfo(values);
                    }
                }
            }else {
                EmotionToCSVHelper.addNAEmotionInfo(values);
            }


            String sideEffectsName = "";
            if (!timelineKey.contains("-")) {
                sideEffectsName = timelineKey + " SideEffects";

                if (info.has("Verified") && info.getInt("Verified") == 1) {
                    // surgery
                    if(timelineType.equalsIgnoreCase("surgery") && doc.has("SurgeryTimeLineInfo")){
                        JSONObject surgeryTimeline = doc.getJSONObject("SurgeryTimeLineInfo");
                        if (surgeryTimeline.has(sideEffectsName)) {
                            JSONArray sideEffectArray = surgeryTimeline.getJSONObject(sideEffectsName).getJSONArray("SideEffectType");
                            SideeffectToCSVHelper.addSideEffectInfo(sideEffectArray, values);
                        } else {
                            SideeffectToCSVHelper.addZeroSideEffectInfo(values);
                        }
                    }

                    // treatment
                    if(timelineType.equalsIgnoreCase("treatment")) {
                        if (info.has(sideEffectsName)) {
                            JSONArray sideEffectArray = info.getJSONObject(sideEffectsName).getJSONArray("SideEffectType");
                            SideeffectToCSVHelper.addSideEffectInfo(sideEffectArray, values);
                        } else {
                            SideeffectToCSVHelper.addZeroSideEffectInfo(values);
                        }
                    }
                } else {
                    SideeffectToCSVHelper.addNASideEffectInfo(values);
                }
            } else {
                SideeffectToCSVHelper.addNASideEffectInfo(values);
            }


            JSONObject postTypeInfo = doc.getJSONObject("PostTypeInfo");
            String postTypeName = timelineKey + " PostTypeInfo";

            JSONObject postTypeInfoBin = postTypeInfo.getJSONObject(postTypeName);
            if (postTypeInfoBin.getDouble("NTotal") == 0.0) {
                values.add("NA");
                values.add("NA");
            } else {
                values.add(postTypeInfoBin.get("PExperience"));
                values.add(postTypeInfoBin.get("PAdvice"));
            }
            values.add(postTypeInfoBin.get("NTotal"));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return values;
    }

    private static String[] getTitle() {
        List<String> titles = new ArrayList<>();

        String[] set1 = new String[]{"Timeline", "AuthorKey", "AuthorID", "Verified", "Gender", "Age", "AgeGroup", "Gleason:Min", "TreatmentType", "TreatmentMentions", "SurgeryMentions", "SurgeryType", "Treatment Decision"};
        for (String s : set1) titles.add(s);
        for (String s : EmotionToCSVHelper.getEmotionHeader()) titles.add(s);
        for (String s : SideeffectToCSVHelper.getSideEffectHeader()) titles.add(s);

        titles.add( "PExperience");
        titles.add("PAdvice");
        titles.add("NTotal");

        String[] titleArray = new String[titles.size()];
        titles.toArray(titleArray);
        return titleArray;
    }
}
