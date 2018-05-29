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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class AggregateDemographics_ProstateCancer {

    public static void main(String... args) throws Exception {
        AggregateDemographics_ProstateCancer aggregateDemographics = new AggregateDemographics_ProstateCancer();
        aggregateDemographics.testSample();
    }

    private void testSample() throws Exception {
        File folder = new File("./ProstateHealwell/Test");

        for (File file : folder.listFiles())

            if (file.isFile()) {
                String sLine;
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new FileReader(file));
                while ((sLine = reader.readLine()) != null) sb.append(sLine);
                reader.close();

                JSONObject profile = new JSONObject(sb.toString());

//        printToCSV(ageInfo);

                JSONObject profileDemographics = getProfileDemographics(profile);

                System.out.println(file.getName());
                System.out.println(profileDemographics);
                System.out.println("#####################################################");
            }
    }

    public static JSONObject getProfileDemographics(JSONObject profile) throws JSONException {
        int ageThreshold= 25;
        String defaultGender = "male";
        JSONObject resolvedAge = resolveAge(profile,ageThreshold);
        JSONObject resolvedGender = resolveGender(profile, defaultGender);
        JSONObject profileDemographics = new JSONObject();
        profileDemographics.put("Age", resolvedAge);
        profileDemographics.put("Gender", resolvedGender);

        return profileDemographics;
    }

    private static JSONObject resolveGender(JSONObject profile, String defaultGender) throws JSONException {

        JSONArray posts = profile.getJSONArray("Posts");
        Map<String, Integer> genderCount = new HashMap<>();


        genderCount.put("male", 0);
        genderCount.put("female", 0);
        genderCount.put("unknown", 0);
        for (int i = 0; i < posts.length(); i++) {
            JSONObject genderInfo = posts.getJSONObject(i).getJSONObject("Demographics").getJSONObject("GenderInfo");
//            System.out.println(genderInfo);
            if (genderInfo.getJSONObject("NounBasedGender").has("NarrationType") && genderInfo.getJSONObject("NounBasedGender").getString("NarrationType").equalsIgnoreCase("first-person")) {
                if (genderInfo.getJSONObject("NounBasedGender").has("PatientGender")) {
                    String gender = genderInfo.getJSONObject("NounBasedGender").getString("PatientGender");
                    genderCount.put(gender, genderCount.get(gender) + 2);
                }
                genderCount.put("male", genderCount.get("male") + genderInfo.getJSONObject("MedicalTermBasedGender").getJSONArray("MaleMedicalTerms").length());
                genderCount.put("female", genderCount.get("female") + genderInfo.getJSONObject("MedicalTermBasedGender").getJSONArray("FemaleMedicalTerms").length());
            }else if (genderInfo.getJSONObject("NounBasedGender").has("NarrationType") && genderInfo.getJSONObject("NounBasedGender").getString("NarrationType").equalsIgnoreCase("second-person")) {
                if (genderInfo.getJSONObject("NounBasedGender").has("NarratorGender")) {
                    String gender = genderInfo.getJSONObject("NounBasedGender").getString("NarratorGender");
                    genderCount.put(gender, genderCount.get(gender) + 2);
                }
            }
        }

        if (profile.has("DemographicsAboutMe")) {
            JSONObject genderInfoAboutMe = profile.getJSONObject("DemographicsAboutMe").getJSONObject("GenderInfo");
            if (!genderInfoAboutMe.getJSONObject("NounBasedGender").has("NarrationType") && genderInfoAboutMe.getJSONObject("NounBasedGender").getString("NarrationType").equalsIgnoreCase("second-person")) {
                if (genderInfoAboutMe.getJSONObject("NounBasedGender").has("PatientGender")) {
                    String gender = genderInfoAboutMe.getJSONObject("NounBasedGender").getString("PatientGender");
                    genderCount.put(gender, genderCount.get(gender) + 10);
                }

            }else if (genderInfoAboutMe.getJSONObject("NounBasedGender").has("NarrationType") && genderInfoAboutMe.getJSONObject("NounBasedGender").getString("NarrationType").equalsIgnoreCase("second-person")) {
                if (genderInfoAboutMe.getJSONObject("NounBasedGender").has("NarratorGender")) {
                    String gender = genderInfoAboutMe.getJSONObject("NounBasedGender").getString("NarratorGender");
                    genderCount.put(gender, genderCount.get(gender) + 2);
                }
            }

            genderCount.put("male", genderCount.get("male") + (genderInfoAboutMe.getJSONObject("MedicalTermBasedGender").getJSONArray("MaleMedicalTerms").length() * 2));
            genderCount.put("female", genderCount.get("female") + (genderInfoAboutMe.getJSONObject("MedicalTermBasedGender").getJSONArray("FemaleMedicalTerms").length() * 2));

        }

        String derivedGender = null;
        float confidence = 0.0F;
        if (genderCount.get("male") > Math.max(0, genderCount.get("female"))) {
            derivedGender = "male";
            confidence = genderCount.get("male") / (float) (genderCount.get("male") + genderCount.get("female"));
        } else if (genderCount.get("female") > Math.max(0, genderCount.get("male"))) {
            derivedGender = "female";
            confidence = genderCount.get("female") / (float) (genderCount.get("male") + genderCount.get("female"));
        }


        if (derivedGender != null) {
            JSONObject genderInfo = new JSONObject();
            genderInfo.put("Gender", derivedGender);
            genderInfo.put("NarrationType", "first-person");
            genderInfo.put("Confidence", (Math.round(confidence * 1000) / 1000.0F));
            return genderInfo;
        }else {
            JSONObject nullGender = new JSONObject();
            nullGender.put("Gender", defaultGender);
            nullGender.put("NarrationType", "unknown");
            return nullGender;
        }
    }

    private static void printToCSV(JSONArray ageInfoArray) throws Exception {
        CSVFormat format = CSVFormat.RFC4180.withHeader("Relation", "Tense", "Confidence", "Phrase").withDelimiter(',');
        CSVPrinter csvPrinter = new CSVPrinter(new FileWriter("ageInfo.csv"), format);
        for (int i = 0; i < ageInfoArray.length(); i++) {
            JSONObject ageInfo = ageInfoArray.getJSONObject(i);
            csvPrinter.printRecord(ageInfo.getString("Relation"), ageInfo.getString("Tense"), ageInfo.getString("Confidence"), ageInfo.getString("Phrase"));
        }

        csvPrinter.close();
    }

    private static JSONObject resolveAge(JSONObject profile, int threshold) throws JSONException {

        JSONArray ageInfoArray = getAgeMentionsInThreads(profile);

        Map<String, Double> ageResolution = new HashMap<>();
        Map<String, List<JSONObject>> ageResolutionJSONObjects = new HashMap<>();
        Map<String, Boolean> hasFirstPerson = new HashMap<>();

        for (int i = 0; i < ageInfoArray.length(); i++) {

            JSONObject ageInfo = ageInfoArray.getJSONObject(i);
            String value = ageInfo.getString("Value");
            if(Double.parseDouble(value) < threshold) continue;

            String relation = ageInfo.getString("Relation");
            double confidence = ageInfo.getDouble("Confidence");
            String tense = ageInfo.getString("Tense");
            if (tense.equalsIgnoreCase("present")) confidence += 0.25;
            else if (tense.equalsIgnoreCase("past")) confidence += 0.0;
            else if (tense.equalsIgnoreCase("unknown")) confidence += 0.0;

            if (relation.equalsIgnoreCase("first-person")) confidence += 0.25;
            else if (relation.equalsIgnoreCase("second-person")) confidence += 0.0;
            else if (relation.equalsIgnoreCase("advice")) confidence += 0.0;

            if (ageResolution.containsKey(value)) {
                ageResolution.put(value, ageResolution.get(value) + confidence);
                ageResolutionJSONObjects.get(value).add(ageInfo);
            } else {
                ageResolution.put(value, confidence);
                ageResolutionJSONObjects.put(value, new ArrayList<>());
                ageResolutionJSONObjects.get(value).add(ageInfo);
            }

            if(!hasFirstPerson.containsKey(value)){
                hasFirstPerson.put(value,false);
            }

            if(relation.equalsIgnoreCase("first-person") || relation.equalsIgnoreCase("unknown")){
                hasFirstPerson.put(value,true);
            }
        }

        if (profile.has("DemographicsAboutMe")) {
            JSONArray ageInfoAboutMeArray = profile.getJSONObject("DemographicsAboutMe").getJSONArray("AgeInfo");
            for (int i = 0; i < ageInfoAboutMeArray.length(); i++) {
                JSONObject ageInfo = ageInfoAboutMeArray.getJSONObject(i);
                String value = ageInfo.getString("Value");
                if(Double.parseDouble(value) >= threshold) {
                    String relation = ageInfo.getString("Relation");
                    if (ageResolution.containsKey(value)) {
                        ageResolution.put(value, (ageResolution.get(value) + ageInfo.getDouble("Confidence") + 3.0));
                        ageResolutionJSONObjects.get(value).add(ageInfo);
                    } else {
                        ageResolution.put(value, (ageInfo.getDouble("Confidence") + 3.0));
                        ageResolutionJSONObjects.put(value, new ArrayList<>());
                        ageResolutionJSONObjects.get(value).add(ageInfo);
                    }

                    if (!hasFirstPerson.containsKey(value)) {
                        hasFirstPerson.put(value, false);
                    }

                    if (relation.equalsIgnoreCase("first-person") || relation.equalsIgnoreCase("unknown")) {
                        hasFirstPerson.put(value, true);
                    }
                }
            }
        }

        if (ageResolution.isEmpty()) {
            JSONObject unknownAgeInfo = new JSONObject();
            unknownAgeInfo.put("Confidence", 0);
            unknownAgeInfo.put("Tense", "unknown");
            unknownAgeInfo.put("Relation", "unknown");
            unknownAgeInfo.put("Value", -1);
            return unknownAgeInfo;
        }

        for (int i = 0; i < ageInfoArray.length(); i++) {
            JSONObject ageInfo = ageInfoArray.getJSONObject(i);
            String relation = ageInfo.getString("Relation");
            double confidence = ageInfo.getDouble("Confidence");
            String tense = ageInfo.getString("Tense");
            if (true) {
                String valueStr = ageInfo.getString("Value");
                int value = (int) Double.parseDouble(valueStr);

                if (value > 5) {
                    for (int sValue = value - 2; sValue <= (value + 2); sValue++) {
                        if (sValue == value) continue;

                        String sValueStr = String.valueOf((double)sValue);
                        if (ageResolution.containsKey(sValueStr)) {
                            ageResolution.put(sValueStr, (ageResolution.get(sValueStr) + (0.75 * (confidence + 0.5) / (double) Math.abs(value - sValue))));
                        }
                    }
                }
            }
        }

//        for (Map.Entry<String, Double> entry : ageResolution.entrySet()) {
//            int value = (int)Double.parseDouble(entry.getKey());
//
//            if(value > 5){
//                for(int sValue= value -2; sValue <= (value+2); sValue++)
//                {
//                    if(sValue == value) continue;
//
//                    String sValueStr = String.valueOf(sValue);
//                    if(ageResolution.containsKey(sValueStr)){
//
//                        ageResolution.put(sValueStr, (ageResolution.get(sValueStr) + (0.5 * entry.getValue()/(double)Math.abs(value - sValue))));
//                    }
//                }
//            }
//        }

//        System.out.println(hasFirstPerson);
//        System.out.println(ageResolution);
        Map.Entry<String, Double> highestConfidenceEntry = null;
        for (Map.Entry<String, Double> entry : ageResolution.entrySet()) {
            if (hasFirstPerson.get(entry.getKey())) {
                if(highestConfidenceEntry == null) {
                    highestConfidenceEntry = entry;
                }else if(entry.getValue() > highestConfidenceEntry.getValue()){
                    highestConfidenceEntry = entry;
                }
            }
        }

        if (highestConfidenceEntry == null) {
            JSONObject unknownAgeInfo = new JSONObject();
            unknownAgeInfo.put("Confidence", 0);
            unknownAgeInfo.put("Tense", "unknown");
            unknownAgeInfo.put("Relation", "unknown");
            unknownAgeInfo.put("Value", -1);
            return unknownAgeInfo;
        }

        JSONObject highestConfidenceAgeInfo = ageResolutionJSONObjects.get(highestConfidenceEntry.getKey()).get(0);

//        System.out.println(highestConfidenceAgeInfo);

        for (JSONObject ageInfo : ageResolutionJSONObjects.get(highestConfidenceEntry.getKey())) {
            if(!(ageInfo.getString("Relation").equalsIgnoreCase("second-person"))) {
                if (highestConfidenceAgeInfo.getString("Relation").equalsIgnoreCase("second-person")) {
                    highestConfidenceAgeInfo = ageInfo;
                }

                if (ageInfo.getDouble("Confidence") > highestConfidenceAgeInfo.getDouble("Confidence")) {
                    highestConfidenceAgeInfo = ageInfo;
                }
            }
        }

        //highestConfidenceAgeInfo.remove("Phrase");
        return highestConfidenceAgeInfo;
    }

    private static JSONArray getAgeMentionsInThreads(JSONObject profile) throws JSONException {
        JSONArray posts = profile.getJSONArray("Posts");
        JSONArray ageMentions = new JSONArray();

        for (int i = 0; i < posts.length(); i++) {
            JSONArray ageMentionsThread = posts.getJSONObject(i).getJSONObject("Demographics").getJSONArray("AgeInfo");
            for (int j = 0; j < ageMentionsThread.length(); j++) {
                ageMentions.put(ageMentionsThread.getJSONObject(j));
            }
        }

        return ageMentions;
    }
}
