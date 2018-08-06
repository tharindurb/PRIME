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

public class SurgeryTypeExtarctor {
    public static Map<String, List<Pair<String, Pattern>>> getSurgeryTypes() {
        return surgeryTypes;
    }

    private static final Map<String, List<Pair<String, Pattern>>> surgeryTypes = getPatternMap();


    public static void main(String... args) {
        SurgeryTypeExtarctor surgeryTypeExtarctor = new SurgeryTypeExtarctor();
        System.out.println(surgeryTypeExtarctor.surgeryTypes);
    }

    public static Map<String,Integer> getSurgeryMentionCounts(String post){
        Map<String, Integer> surgeryTypeMap = new HashMap<>();
        Set<String> surgeryTypeKeywords = new HashSet<>();

        for (String surgeryType : surgeryTypes.keySet()) {
            surgeryTypeMap.put(surgeryType, 0);
        }

        extractSurgeryType(post, surgeryTypeMap, surgeryTypeKeywords);


        return surgeryTypeMap;
    }

    public static void getSurgeryTypeProfileJSON(JSONObject profile, List<String> posts, String about) throws JSONException {

        System.out.println("surgery type extracting: " + profile.getString("AuthorID"));
        Map<String, Integer> surgeryTypeMap = new HashMap<>();
        Set<String> surgeryTypeKeywords = new HashSet<>();

        for (String surgeryType : surgeryTypes.keySet()) {
            surgeryTypeMap.put(surgeryType, 0);
        }

        if(!about.isEmpty()){
            List<String> postsNew = posts.stream().map(post -> post + ". " + about).collect(Collectors.toList());
            posts = postsNew;
        }


        for (String post : posts) {
            extractSurgeryType(post, surgeryTypeMap, surgeryTypeKeywords);

        }


        if (!posts.isEmpty()) {
            JSONArray surgeryTypeJSON = new JSONArray();
            JSONArray surgeryTypeKeywordsJSON = new JSONArray();

            for (Map.Entry<String, Integer> entry : surgeryTypeMap.entrySet()) {
                if ((10 * entry.getValue()) >= posts.size()) {
                    surgeryTypeJSON.put(entry.getKey());
                }
            }

            for (String surgeryTypeKeyword : surgeryTypeKeywords) {
                surgeryTypeKeywordsJSON.put(surgeryTypeKeyword);
            }

            String maxModality = "";
            int max = 0;
            for(Map.Entry<String,Integer> entry: surgeryTypeMap.entrySet()){
                if(entry.getValue()> max){
                    max = entry.getValue();
                    maxModality = entry.getKey();
                }
            }

            JSONObject surgeryTypeInfo;
            if(profile.has("SurgeryTypeInfo")){
                surgeryTypeInfo = profile.getJSONObject("SurgeryTypeInfo");
            }else {
                surgeryTypeInfo = new JSONObject();
            }

            try {
                surgeryTypeInfo.put("SurgeryType", maxModality);
                surgeryTypeInfo.put("SurgeryMentions", surgeryTypeJSON);
                surgeryTypeInfo.put("SurgeryTypeKeywords", surgeryTypeKeywordsJSON);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            profile.put("SurgeryTypeInfo", surgeryTypeInfo);
        }
    }

    public static boolean hasSurgeryMentions(String post) {
        for (Map.Entry<String, List<Pair<String, Pattern>>> entry : surgeryTypes.entrySet()) {
            List<String> surgeryTypeKeywordList = getSurgeryTypeKeywords(entry.getValue(), post);
            if (!surgeryTypeKeywordList.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void extractSurgeryType(String text, Map<String, Integer> surgeryTypeMap, Set<String> surgeryKeywords) {
        for (Map.Entry<String, List<Pair<String, Pattern>>> entry : surgeryTypes.entrySet()) {

            List<String> surgeryTypeKeywordList = getSurgeryTypeKeywords(entry.getValue(), text);

            if (!surgeryTypeKeywordList.isEmpty()) {
                surgeryTypeMap.put(entry.getKey(), surgeryTypeMap.get(entry.getKey()) + surgeryTypeKeywordList.size());
                surgeryKeywords.addAll(surgeryTypeKeywordList);
            }
        }
    }


    private static List<String> getSurgeryTypeKeywords(List<Pair<String, Pattern>> value, String text) {
        List<String> surgeryTypeKeywordList = new ArrayList<>();
        for (Pair<String, Pattern> pair : value) {
            Matcher m = pair.getValue().matcher(text);
            if (m.find()) {
                surgeryTypeKeywordList.add(pair.getKey());
            }
        }
        return surgeryTypeKeywordList;
    }


    private static Map<String, List<Pair<String, Pattern>>> getPatternMap() {
        Map<String, List<Pair<String, Pattern>>> featureMap = new LinkedHashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./models/treatment_type/surgery_type_list.txt"));
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
                                keywords.add(new Pair<>(keyword, Pattern.compile("\\b" + StringUtils.join(keyword.split("\\s+"), "\\s+") + "\\b", Pattern.CASE_INSENSITIVE)));
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

//    public static Map<String,String> getTreatmentMentions(String text) {
//        text = Utils.removeUnicode(text);
//        Map<String,String> treatmentMentions = new HashMap<>();
//        for(String sentence: SentenceSplitter.getSentenceSplitter().sentDetect(text)){
//            for (Map.Entry<String, List<Pair<String, Pattern>>> entry : surgeryTypes.entrySet()) {
//                List<String> treatmentTypeKeywordList = getTreatmentTypeKeywords(entry.getValue(), sentence);
//                if (!treatmentTypeKeywordList.isEmpty()) {
//                    treatmentMentions.put(sentence,entry.getKey());
//                }
//            }
//        }
//        return treatmentMentions;
//    }
public static void getSurgeryTypeProfilePerPost(String post, JSONObject surgeryTypeProfile) throws JSONException {

    // System.out.println("surgery type extracting: " + profile.getString("AuthorID"));
    Map<String, Integer> surgeryTypeMap = new HashMap<>();
    Set<String> surgeryTypeKeywords = new HashSet<>();

    for (String surgeryType : surgeryTypes.keySet()) {
        surgeryTypeMap.put(surgeryType, 0);
    }

    extractSurgeryType(post, surgeryTypeMap, surgeryTypeKeywords);

    if (!post.isEmpty()) {
        JSONArray surgeryTypeJSON = new JSONArray();
        JSONArray surgeryTypeKeywordsJSON = new JSONArray();

        for (Map.Entry<String, Integer> entry : surgeryTypeMap.entrySet()) {
            surgeryTypeJSON.put(entry.getKey());
        }

        for (String surgeryTypeKeyword : surgeryTypeKeywords) {
            surgeryTypeKeywordsJSON.put(surgeryTypeKeyword);
        }

        String maxModality = "";
        int max = 0;
        for (Map.Entry<String, Integer> entry : surgeryTypeMap.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                maxModality = entry.getKey();
            }
        }

        JSONObject surgeryTypeInfo = new JSONObject();
        try {
            surgeryTypeInfo.put("SurgeryType", maxModality);

            String surgeryMentions = "";

            for (int i = 0; i < surgeryTypeJSON.length(); i++) {
                surgeryMentions = surgeryMentions + surgeryTypeJSON.getString(i) + ",";
            }

            surgeryTypeInfo.put("SurgeryMentions", surgeryMentions.trim());

            String surgeryKeyWordsString = "";

            for (int i = 0; i < surgeryTypeKeywordsJSON.length(); i++) {
                surgeryKeyWordsString = surgeryKeyWordsString + surgeryTypeKeywordsJSON.getString(i) + ",";
            }

            surgeryTypeInfo.put("SurgeryTypeKeywords", surgeryKeyWordsString.trim());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        surgeryTypeProfile.put("SurgeryTypeInfo", surgeryTypeInfo);
    }
}
}
