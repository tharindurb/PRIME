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
package clinical_info;

import javafx.util.Pair;
import nlptasks.SentenceSplitter;
import org.apache.commons.lang.StringUtils;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import util.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TreatmentTypeExtractor {
    public static Map<String, List<Pair<String, Pattern>>> getTreatmentTypes() {
        return treatmentTypes;
    }

    private static final Map<String, List<Pair<String, Pattern>>> treatmentTypes = getPatternMap();


    public static void main(String... args) {
        TreatmentTypeExtractor treatmentTypeExtractor = new TreatmentTypeExtractor();
        System.out.println(treatmentTypeExtractor.treatmentTypes);
    }

    public static boolean hasTreatmentTypeMentions(String key, String post) {
            List<String> surgeryTypeKeywordList = getTreatmentTypeKeywords(treatmentTypes.get(key), post);
            if (!surgeryTypeKeywordList.isEmpty()) {
                return true;
            }
        return false;
    }

    public static Map<String,Integer> getTreatmentMentionCounts(List<String> posts){
        Map<String, Integer> treatmentTypeMap = new HashMap<>();
        Set<String> treatmentTypeKeywords = new HashSet<>();

        for (String treatmentType : treatmentTypes.keySet()) {
            treatmentTypeMap.put(treatmentType, 0);
        }

        for (String post : posts) {
            extractTreatmentType(post, treatmentTypeMap, treatmentTypeKeywords);
        }

        return treatmentTypeMap;
    }

    public static void getTreatmentTypeProfileSinglePost(JSONObject profile, String post) throws JSONException {

        Map<String, Integer> treatmentTypeMap = new HashMap<>();
        Set<String> treatmentTypeKeywords = new HashSet<>();

        for (String treatmentType : treatmentTypes.keySet()) {
            treatmentTypeMap.put(treatmentType, 0);
        }


        extractTreatmentType(post, treatmentTypeMap, treatmentTypeKeywords);



        if (!post.isEmpty()) {
            JSONArray treatmentTypeJSON = new JSONArray();
            JSONArray treatmentTypeKeywordsJSON = new JSONArray();

            for (Map.Entry<String, Integer> entry : treatmentTypeMap.entrySet()) {
                if (entry.getValue() >0) {
                    treatmentTypeJSON.put(entry.getKey());
                }
            }

            for (String treatmentTypeKeyword : treatmentTypeKeywords) {
                treatmentTypeKeywordsJSON.put(treatmentTypeKeyword);
            }

            String maxModality = "";
            int max = 0;
            for(Map.Entry<String,Integer> entry: treatmentTypeMap.entrySet()){
                if(entry.getValue()> max){
                    max = entry.getValue();
                    maxModality = entry.getKey();
                }
            }

            JSONObject treatmentTypeInfo = new JSONObject();
            try {
                treatmentTypeInfo.put("TreatmentType", maxModality);
                treatmentTypeInfo.put("TreatmentMentions", treatmentTypeJSON);
                treatmentTypeInfo.put("TreatmentTypeKeywords", treatmentTypeKeywordsJSON);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            profile.put("TreatmentTypeInfo", treatmentTypeInfo);
        }
    }


    public static void getTreatmentTypeProfileJSON(JSONObject profile, List<String> posts, String about) throws JSONException {

        System.out.println("treatment type extracting: " + profile.getString("AuthorID"));
        Map<String, Integer> treatmentTypeMap = new HashMap<>();
        Set<String> treatmentTypeKeywords = new HashSet<>();

        for (String treatmentType : treatmentTypes.keySet()) {
            treatmentTypeMap.put(treatmentType, 0);
        }

        if(!about.isEmpty()){
            List<String> postsNew = posts.stream().map(post -> post + ". " + about).collect(Collectors.toList());
            posts = postsNew;
        }


        for (String post : posts) {
            extractTreatmentType(post, treatmentTypeMap, treatmentTypeKeywords);

        }


        if (!posts.isEmpty()) {
            JSONArray treatmentTypeJSON = new JSONArray();
            JSONArray treatmentTypeKeywordsJSON = new JSONArray();

            for (Map.Entry<String, Integer> entry : treatmentTypeMap.entrySet()) {
                if ((10 * entry.getValue()) >= posts.size()) {
                    treatmentTypeJSON.put(entry.getKey());
                }
            }

            for (String treatmentTypeKeyword : treatmentTypeKeywords) {
                treatmentTypeKeywordsJSON.put(treatmentTypeKeyword);
            }

            String maxModality = "";
            int max = 0;
            for(Map.Entry<String,Integer> entry: treatmentTypeMap.entrySet()){
                if(entry.getValue()> max){
                    max = entry.getValue();
                    maxModality = entry.getKey();
                }
            }

            JSONObject treatmentTypeInfo = new JSONObject();
            try {
                treatmentTypeInfo.put("TreatmentType", maxModality);
                treatmentTypeInfo.put("TreatmentMentions", treatmentTypeJSON);
                treatmentTypeInfo.put("TreatmentTypeKeywords", treatmentTypeKeywordsJSON);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            profile.put("TreatmentTypeInfo", treatmentTypeInfo);
        }
    }

    private static void extractTreatmentType(String text, Map<String, Integer> treatmentTypeMap, Set<String> treatmentKeywords) {
        for (Map.Entry<String, List<Pair<String, Pattern>>> entry : treatmentTypes.entrySet()) {

            List<String> treatmentTypeKeywordList = getTreatmentTypeKeywords(entry.getValue(), text);

            if (!treatmentTypeKeywordList.isEmpty()) {
                treatmentTypeMap.put(entry.getKey(), treatmentTypeMap.get(entry.getKey()) + treatmentTypeKeywordList.size());
                treatmentKeywords.addAll(treatmentTypeKeywordList);
            }
        }
    }

    public static JSONObject getTreatmentTypeJSON(String text) {

        JSONArray treatmentType = new JSONArray();
        JSONArray treatmentTypeKeywords = new JSONArray();
        for (Map.Entry<String, List<Pair<String, Pattern>>> entry : treatmentTypes.entrySet()) {
            List<String> treatmentTypeKeywordList = getTreatmentTypeKeywords(entry.getValue(), text);
            if (!treatmentTypeKeywordList.isEmpty()) {
                treatmentType.put(entry.getKey());
                for (String treatmentTypeKeyword : treatmentTypeKeywordList) {
                    treatmentTypeKeywords.put(treatmentTypeKeyword);
                }
            }
        }

        JSONObject treatmentTypeJSON = new JSONObject();
        try {
            treatmentTypeJSON.put("TreatmentType", treatmentType);
            treatmentTypeJSON.put("TreatmentTypeKeywords", treatmentTypeKeywords);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return treatmentTypeJSON;
    }

    private static List<String> getTreatmentTypeKeywords(List<Pair<String, Pattern>> value, String text) {
        List<String> treatmentTypeKeywordList = new ArrayList<>();
        for (Pair<String, Pattern> pair : value) {
            Matcher m = pair.getValue().matcher(text);
            if (m.find()) {
                treatmentTypeKeywordList.add(pair.getKey());
            }
        }
        return treatmentTypeKeywordList;
    }


    private static Map<String, List<Pair<String, Pattern>>> getPatternMap() {
        Map<String, List<Pair<String, Pattern>>> featureMap = new LinkedHashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./models/treatment_type/treatment_type_list.txt"));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] firstSplit = line.split(":");

                    if (firstSplit.length == 2) {
                        String category = firstSplit[0].trim();
                        if (category.charAt(0) == '%') continue;

                        List<Pair<String, Pattern>> keywords = new ArrayList<>();

                        for (String keyword : firstSplit[1].trim().split(",")) {
                            keyword = keyword.trim();

                            if (!keyword.isEmpty()) {
                                if (Character.isUpperCase(keyword.charAt(0))) {
                                    keywords.add(new Pair<>(keyword, Pattern.compile("\\b" + StringUtils.join(keyword.split("\\s+"), "\\s+") + "\\b")));
                                } else{
                                    keywords.add(new Pair<>(keyword, Pattern.compile("\\b" + StringUtils.join(keyword.split("\\s+"), "\\s+") + "\\b", Pattern.CASE_INSENSITIVE)));
                               }
                            }
                        }

                        if (keywords.size() > 0) {
                            featureMap.put(category, keywords);
                        }
                    }
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return featureMap;
    }

    public static Map<String,String> getTreatmentMentions(String text) {
        text = Utils.removeUnicode(text);
        Map<String,String> treatmentMentions = new HashMap<>();
        for(String sentence: SentenceSplitter.getSentenceSplitter().sentDetect(text)){
            for (Map.Entry<String, List<Pair<String, Pattern>>> entry : treatmentTypes.entrySet()) {
                List<String> treatmentTypeKeywordList = getTreatmentTypeKeywords(entry.getValue(), sentence);
                if (!treatmentTypeKeywordList.isEmpty()) {
                    treatmentMentions.put(sentence,entry.getKey());
                }
            }
        }
        return treatmentMentions;
    }
}
