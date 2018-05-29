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

import org.apache.commons.lang.StringUtils;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgeFeature {
    final String sentence;
    final String chunk;
    final String L;
    final String R;
    String MStr;
    final int sentenceNo;
    String classValue;
    String tense;
    String relation;
    double confidence;
    double value;
    private Map<String, Integer> featureMap;
    private static final Map<String, List<Pattern>> featurePatternMap = getPatternMap();
    private static final List<Pattern> featurePatternMonth = getPatterns(new String("month, months").split(","));
    private static final List<Pattern> featurePatternWeek = getPatterns(new String("week, weeks, wks").split(","));
    private static final List<Pattern> featurePatternDay = getPatterns(new String("day, days").split(","));
    private static final List<Pattern> featurePatternPresent = getPatterns(new String("is, am, m, Im, will, now, turning, almost").split(","));
    private static final List<Pattern> featurePatternPast = getPatterns(new String("was, at, aged, since").split(","));

    public String getChunk() {
        return chunk;
    }

    public AgeFeature(String sentence, String chunk, String L, String M, String R, int sentenceNo) {
        this.sentence = sentence;
        this.chunk = chunk;
        this.sentenceNo = sentenceNo;
        this.L = L;
        this.R = R;
//        extractFeatures(L, M, R);
        extractFeatures_NEW(L, M, R);
        extractAgeValue(M);
        extractOtherIno();
    }


    private void extractAgeValue(String text) {
        StringBuilder sbAge = new StringBuilder();
        StringBuilder sbStr = new StringBuilder();

        for (int i = 0, len = text.length(); i < len; i++) {
            if (Character.isDigit(text.charAt(i)) || (text.charAt(i) == '.')) {
                sbAge.append(text.charAt(i));
            }else {
                sbStr.append(text.charAt(i));
            }
        }

        value = Double.parseDouble(sbAge.toString());
        MStr = sbStr.toString();
        featureMap.put("Value", ((int)value/20));
    }

    private void extractFeatures(String L, String M, String R) {
        featureMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<Pattern>> entry : featurePatternMap.entrySet()) {
            switch (entry.getKey().charAt(0)) {
                case 'L':
                    featureMap.put(entry.getKey(), findFeature(L, entry.getValue()));
                    break;
                case 'M':
                    featureMap.put(entry.getKey(), findFeature(M, entry.getValue()));
                    break;
                case 'R':
                    featureMap.put(entry.getKey(), findFeature(R, entry.getValue()));
                    break;
            }
        }

    }

    private void extractFeatures_NEW(String L, String M, String R) {
        featureMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<Pattern>> entry : featurePatternMap.entrySet()) {
            switch (entry.getKey().charAt(0)) {
                case 'L':
                    featureMap.put(entry.getKey(), findFeature_NEW_L(L, entry.getValue()));
                    break;
                case 'M':
                    featureMap.put(entry.getKey(), findFeature(M, entry.getValue()));
                    break;
                case 'R':
                    featureMap.put(entry.getKey(), findFeature_NEW_R(R, entry.getValue()));
                    break;
            }
        }

    }

    private Integer findFeature(String text, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return 1;
            }
        }

        return 0;
    }

    private Integer findFeature_NEW_L(String text, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                int distance = findNumberOfWords(text.substring(matcher.end(),text.length())) + 1;
                return distance;
            }
        }
        return 0;
    }

    private Integer findFeature_NEW_R(String text, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                int distance = findNumberOfWords(text.substring(0,matcher.start())) + 1;
                return distance;
            }
        }
        return 0;
    }

    private int findNumberOfWords(String subText) {
        subText  = subText.trim();

        if(subText.length()>0) {
            String[] words = subText.split("\\s+");
            return words.length;
        }else {
            return 0;
        }

    }

    public final String[] getFeatureRecord() {
        String[] record = new String[featureMap.size()];
        int i = 0;
        for (Integer value : featureMap.values()) {
            record[i++] = String.valueOf(value);
        }

        return record;
    }

    private void extractOtherIno() {
        this.relation = getRelationship();
        this.tense = getTense();
    }

    public JSONObject getAgeFeatureJSON() {
        JSONObject ageFeatuer = new JSONObject();
        try {
            ageFeatuer.put("Value", resolveAge());
            ageFeatuer.put("Confidence", confidence);
            ageFeatuer.put("Phrase", chunk);
            ageFeatuer.put("Relation", relation);
            ageFeatuer.put("Tense", tense);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ageFeatuer;
    }

    private String getRelationship() {
//        if(resolveAge() < 10){
//            return "second-person";
//        } else
        if (featureMap.get("LHasI") == 1 | featureMap.get("LHasGenderDirect") == 1 | featureMap.get("RHasGenderDirect") == 1) {
            return "first-person";
        } else if (featureMap.get("LHasGenderIndirect") == 1 || featureMap.get("RHasGenderIndirect") == 1) {
            return "second-person";
        } else {
            return "unknown";
        }
    }

    public String getTense() {
        if(hasPattern(L,featurePatternPast)){
            return "past";
        }else if(hasPattern(L,featurePatternPresent)){
            return "present";
        }
        else if (hasPattern(chunk, featurePatternPresent)) {
            return "present";
        } else if (hasPattern(chunk, featurePatternPast)) {
            return "past";
        }
        return "unknown";
    }

    private double resolveAge() {
        if (featureMap.get("MYear") == 1 || featureMap.get("RYear") == 1) {
            return value;
        } else if (hasPattern(R, featurePatternMonth) || hasPattern(MStr, featurePatternMonth)) {
            return value / 12;
        } else if (hasPattern(R, featurePatternWeek) || hasPattern(MStr, featurePatternWeek)) {
            return value / 52;
        }
//        else if (hasPattern(R, featurePatternDay) || hasPattern(MStr, featurePatternDay)) {
//            return value / 365;
//        }
        else {
            return value;
        }
    }

    private boolean hasPattern(String chunk, List<Pattern> featurePattern) {
        for (Pattern pattern : featurePattern) {
            Matcher m = pattern.matcher(chunk);
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

//    public final String[] getFeatureRecordWithPhraseAndClass() {
//        String[] record = new String[featureMap.size() + 3];
//        int i = 0;
//        for(Integer value: featureMap.values()){
//            record[i++] = String.valueOf(value);
//        }
//
//        record[i++] = chunk;
//        record[i++] = classValue;
//        record[i] = String.valueOf(confidence);
//        return record;
//    }

    public final String[] getFeatureRecordWithClass() {
        String[] record = new String[featureMap.size() + 1];
        int i = 0;
        for (Integer value : featureMap.values()) {
            record[i++] = String.valueOf(value);
        }

//        record[i++] = String.valueOf(value);
        record[i++] = classValue;
        return record;
    }

    private static List<Pattern> getPatterns(String[] terms) {
        List<Pattern> patterns = new ArrayList<>();
        for (String term : terms) {
            term = term.trim();
            patterns.add(Pattern.compile("\\b" + StringUtils.join(term.split("\\s+"), "\\s+") + "\\b", Pattern.CASE_INSENSITIVE));
        }
        return patterns;
    }

    private static Map<String, List<Pattern>> getPatternMap() {
        Map<String, List<Pattern>> featureMap = new LinkedHashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./models/ageClassifier/age_feature_list_new.txt"));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] firstSplit = line.split(":");

                    if (firstSplit.length == 2) {
                        String category = firstSplit[0].trim();
                        if (category.charAt(0) == '%') continue;

                        List<Pattern> keywords = new ArrayList<>();

                        for (String keyword : firstSplit[1].trim().split(",")) {
                            keyword = keyword.trim();
                            if (!keyword.isEmpty()) {
                                if (category.charAt(0) == 'M') {
                                    keywords.add(Pattern.compile("\\b\\d+" + StringUtils.join(keyword.split("\\s+"), "\\s+") + "\\b", Pattern.CASE_INSENSITIVE));
                                } else {
                                    keywords.add(Pattern.compile("\\b" + StringUtils.join(keyword.split("\\s+"), "\\s+") + "\\b", Pattern.CASE_INSENSITIVE));
                                }
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

    @Override
    public String toString() {
        return "AgeFeature{" + "chunk='" + chunk +
                "', featureMap=" + featureMap +
                '}';
    }

    public Map<String, Integer> getFeatureMap() {
        return featureMap;
    }


}
