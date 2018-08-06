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
import org.apache.commons.lang.StringUtils;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SideEffectExtractor {
    private static final Map<String, List<Pair<String, Pattern>>> sideEffectTypes = getPatternMap();

    public static void main(String... args) {
        SideEffectExtractor sideEffectExtractor = new SideEffectExtractor();
        System.out.println(sideEffectExtractor.sideEffectTypes);

    }

    public static void getSideEffectsProfileJSON(JSONObject profile, List<String> posts, String about) throws JSONException {
        System.out.println("side effect extracting: " + profile.getString("AuthorID"));

        Set<String> sideEffectType = new HashSet<>();
        Set<String> sideEffectTypeKeywords = new HashSet<>();


        if(!about.isEmpty()) extractSideEffectType(about, sideEffectType, sideEffectTypeKeywords);

        for (String post: posts) {

                extractSideEffectType(post, sideEffectType, sideEffectTypeKeywords);

        }

        JSONArray sideEffectTypeJSON = new JSONArray();
        JSONArray sideEffectTypeKeywordsJSON = new JSONArray();

        for (String text : sideEffectType) sideEffectTypeJSON.put(text);
        for (String sideEffectKeyword : sideEffectTypeKeywords) sideEffectTypeKeywordsJSON.put(sideEffectKeyword);


        JSONObject sideEffectTypeInfo = new JSONObject();
        try {
            sideEffectTypeInfo.put("SideEffectType", sideEffectTypeJSON);
            sideEffectTypeInfo.put("SideEffectTypeKeywords", sideEffectTypeKeywordsJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profile.put("SideEffectTypeInfo", sideEffectTypeInfo);
    }


    private static void extractSideEffectType(String text, Set<String> sideEffectType, Set<String> sideEffectTypeKeywords) {
        for (Map.Entry<String, List<Pair<String, Pattern>>> entry : sideEffectTypes.entrySet()) {
                List<String> sideeffectTypeKeywordList = getSideEffectTypeKeywords(entry.getValue(), text);

                if (!sideeffectTypeKeywordList.isEmpty()) {
                    sideEffectType.add(entry.getKey());
                    sideEffectTypeKeywords.addAll(sideeffectTypeKeywordList);
                }
            }
    }

    private static List<String> getSideEffectTypeKeywords(List<Pair<String, Pattern>> value, String text) {
        List<String> sideeffectKeywordList = new ArrayList<>();
        for (Pair<String, Pattern> pair : value) {
            Matcher m = pair.getValue().matcher(text);
            if (m.find()) {
                sideeffectKeywordList.add(pair.getKey());
            }
        }
        return sideeffectKeywordList;
    }

    private static Map<String, List<Pair<String, Pattern>>> getPatternMap() {
        Map<String, List<Pair<String, Pattern>>> featureMap = new LinkedHashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./models/treatment_type/treatment_type_side_effects.txt"));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] firstSplit = line.split(":");

                    if (firstSplit.length == 2) {
                        String type = firstSplit[0].trim();
                        if (type.charAt(0) == '%') continue;

                        List<Pair<String, Pattern>> keywords = new ArrayList<>();

                        for (String keyword : firstSplit[1].trim().split(",")) {
                            keyword = keyword.trim();

                            if (!keyword.isEmpty()) {
                                keywords.add(new Pair<>(keyword, Pattern.compile("\\b" + StringUtils.join(keyword.split("\\s+"), "\\s+") + "\\b", Pattern.CASE_INSENSITIVE)));
                            }
                        }

                        if (keywords.size() > 0) {
                           featureMap.put(type, keywords);
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

    public static JSONObject getSideEffectJSON(String text) {
        JSONArray sideeffectType = new JSONArray();
        JSONArray sideeffectTypeKeywords = new JSONArray();

        for (Map.Entry<String, List<Pair<String, Pattern>>> entry : sideEffectTypes.entrySet()) {
                List<String> sideeffectTypeKeywordList = getSideEffectTypeKeywords(entry.getValue(), text);

                if (!sideeffectTypeKeywordList.isEmpty()) {
                    sideeffectType.put(entry.getKey());
                    for (String sideeffectTypeKeyword : sideeffectTypeKeywordList) {
                        sideeffectTypeKeywords.put(sideeffectTypeKeyword);
                    }
                }
            }

        JSONObject sideeffectJSON = new JSONObject();
        try {
            sideeffectJSON.put("SideEffectType", sideeffectType);
            sideeffectJSON.put("SideEffectTypeKeywords", sideeffectTypeKeywords);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return sideeffectJSON;
    }


    public static void getSideEffectsProfileJSONPerPost(JSONObject profile, String post) throws JSONException {
        System.out.println("side effect extracting: " + profile.getString("AuthorID"));

        Set<String> sideEffectType = new HashSet<>();
        Set<String> sideEffectTypeKeywords = new HashSet<>();


        extractSideEffectType(post, sideEffectType, sideEffectTypeKeywords);

        JSONArray sideEffectTypeJSON = new JSONArray();
        JSONArray sideEffectTypeKeywordsJSON = new JSONArray();

        for (String text : sideEffectType) sideEffectTypeJSON.put(text);
        for (String sideEffectKeyword : sideEffectTypeKeywords) sideEffectTypeKeywordsJSON.put(sideEffectKeyword);


        JSONObject sideEffectTypeInfo = new JSONObject();
        try {
            sideEffectTypeInfo.put("SideEffectType", sideEffectTypeJSON);
            sideEffectTypeInfo.put("SideEffectTypeKeywords", sideEffectTypeKeywordsJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        profile.put("SideEffectTypeInfo", sideEffectTypeInfo);
    }

    public static JSONObject getSideEffectsProfileJSONForTimeline(List<String> posts) throws JSONException {
        Set<String> sideEffectType = new HashSet<>();
        Set<String> sideEffectTypeKeywords = new HashSet<>();

        for (String post: posts) {

            extractSideEffectType(post, sideEffectType, sideEffectTypeKeywords);

        }

        JSONArray sideEffectTypeJSON = new JSONArray();
        JSONArray sideEffectTypeKeywordsJSON = new JSONArray();

        for (String text : sideEffectType) sideEffectTypeJSON.put(text);
        for (String sideEffectKeyword : sideEffectTypeKeywords) sideEffectTypeKeywordsJSON.put(sideEffectKeyword);


        JSONObject sideEffectTypeInfo = new JSONObject();
        try {
            sideEffectTypeInfo.put("SideEffectType", sideEffectTypeJSON);
            sideEffectTypeInfo.put("SideEffectTypeKeywords", sideEffectTypeKeywordsJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sideEffectTypeInfo;
    }
}
