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

import org.apache.commons.lang.StringUtils;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenderFeature {
    String gender;
    final String sentence;
    final int sentenceNo;
    String classValue;
    double confidence;
    private Map<String, Integer> featureMap;
    private static final Map<String, List<Pattern>> featurePatternMap = getPatternMap();
    private static List<Pattern> genderPatterns;


    public GenderFeature(String sentence, int sentenceNo) {
        this.sentence = sentence;
        this.sentenceNo = sentenceNo;
        extractFeatures(sentence);
        parseGender();
    }

    private void parseGender() {
        if((featureMap.get("MaleGenderDirect") +  featureMap.get("MaleGenderIndirect") + featureMap.get("ThirdPersonMale")) > 0)
        {
            gender = "male";
        }else if ((featureMap.get("FemaleGenderDirect") +  featureMap.get("FemaleGenderIndirect") + featureMap.get("ThirdPersonFemale")) > 0){
            gender = "female";
        }else {
            gender = "na";
        }
    }

    private void extractFeatures(String sentence) {
        featureMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<Pattern>> entry : featurePatternMap.entrySet()) {
            featureMap.put(entry.getKey(), findFeature(sentence, entry.getValue()));
        }
        featureMap.put("SentenceNo", sentenceNo);
    }

    private static Integer findFeature(String text, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return 1;
            }
        }

        return 0;
    }

    public static String[] getFeatureHeaderWithPhrase(){
        String[] header = new String[featurePatternMap.size()+2];
        int i = 0;
        for(String value: featurePatternMap.keySet()){
            header[i++] = value;
        }
        header[i++] = "SentenceNo";
        header[i] = "Phrase";

        return header;
    }

    public String getGender() {
        return gender;
    }

    public JSONObject getGenderFeatureJSON() {
        JSONObject genderFeature = new JSONObject();
        try {
            genderFeature.put("Value", gender);
            genderFeature.put("Phrase", sentence);
            genderFeature.put("Confidence", confidence);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return genderFeature;
    }

    private static Map<String, List<Pattern>> getPatternMap() {
        Map<String, List<Pattern>> featureMap = new LinkedHashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("gender_feature_list.txt"));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] firstSplit = line.split(":");

                    if (firstSplit.length == 2) {
                        String category = firstSplit[0].trim();
                        List<Pattern> keywords = new ArrayList<>();

                        for (String keyword : firstSplit[1].trim().split(",")) {
                            keyword = keyword.trim();
                            if (!keyword.isEmpty()) {
                                keywords.add(Pattern.compile("\\b" + StringUtils.join(keyword.split("\\s+"), "\\s+") + "\\b", Pattern.CASE_INSENSITIVE));
                            }
                        }

                        if (keywords.size() > 0) {
                            featureMap.put(category, keywords);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return featureMap;
    }

    public final String[] getFeatureRecordWithPhrase(String content)
    {
        String[] record = new String[featureMap.size() + 2];
        int i = 0;
        for(Integer value: featureMap.values()){
            record[i++] = String.valueOf(value);
        }

        record[i++] = sentence;
        record[i] = content;
        return record;
    }

    public static boolean hasGender(String sentence) {
        if(genderPatterns == null){
            genderPatterns = new ArrayList<>();
            genderPatterns.addAll(featurePatternMap.get("MaleGenderDirect"));
            genderPatterns.addAll(featurePatternMap.get("MaleGenderIndirect"));
            genderPatterns.addAll(featurePatternMap.get("ThirdPersonMale"));
            genderPatterns.addAll(featurePatternMap.get("FemaleGenderDirect"));
            genderPatterns.addAll(featurePatternMap.get("FemaleGenderIndirect"));
            genderPatterns.addAll(featurePatternMap.get("ThirdPersonFemale"));
        }

        if(findFeature(sentence,genderPatterns)> 0){
            return true;
        }else {
            return false;
        }
    }
}
