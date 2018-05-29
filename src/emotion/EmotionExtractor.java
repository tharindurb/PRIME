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
package emotion;

import javafx.util.Pair;
import nlptasks.SentenceSplitter;
import nlptasks.SentenceTokenizer;
import nlptasks.StemmerWraper;
import org.apache.commons.lang.StringUtils;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import util.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Chammi on 19/09/2016.
 */
public class EmotionExtractor {

    private static final Map<String, List<Pair<String, Pattern>>> emotionPatterns = getPatternMap();
    private static final SentenceTokenizer tokenizer = SentenceTokenizer.getSentenceTokenizer();
    private static final StemmerWraper stemmer = StemmerWraper.getStemmer();
    private static final DecimalFormat df = new DecimalFormat("#.###");

    public static void main(String... args) throws JSONException {
    }

    public static void getEmotionsMostRecentPostProfileJSON(JSONObject profile, String post) throws JSONException {
        System.out.println("emotion extracting recent post: " + profile.getString("AuthorID"));

        Map<String, Integer> emotionMap = new HashMap<>();
        Set<String> emotionKeywords = new HashSet<>();

        for (String emotion : emotionPatterns.keySet()) {
            emotionMap.put(emotion, 0);
        }

        extractEmotions(post, emotionMap, emotionKeywords);

        JSONObject emotionJSON = new JSONObject();
        // JSONArray emotionKeywordsJSON = new JSONArray();

        for (Map.Entry<String, Integer> entry : emotionMap.entrySet()) {
            emotionJSON.put(entry.getKey(), df.format(entry.getValue()));
        }

        JSONObject emotionInfo;
        if(profile.has("EmotionInfo")){
            emotionInfo = profile.getJSONObject("EmotionInfo");
        }else {
            emotionInfo = new JSONObject();
            profile.put("EmotionInfo",emotionInfo);
        }

        emotionInfo.put("EmotionsRecentPost", emotionJSON);
    }


    public static void getEmotionsProfileJSON(JSONObject profile, List<String> posts) throws JSONException {
        System.out.println("emotion extracting: " + profile.getString("AuthorID"));
        JSONObject emotionInfo = getEmotionsJSON(posts);
        profile.put("EmotionInfo", emotionInfo);
    }

    public static JSONObject getEmotionsJSON(List<String> posts) throws JSONException {
        Map<String, Integer> emotionMap = new HashMap<>();
        Collection<String> emotionKeywords = new ArrayList<>();

        for (String emotion : emotionPatterns.keySet()) {
            emotionMap.put(emotion, 0);
        }

        for (String post : posts) {
            extractEmotions(post, emotionMap, emotionKeywords);

        }

        JSONObject emotionJSON = new JSONObject();
        JSONArray emotionKeywordsJSON = new JSONArray();

        if(posts.size() > 0) {
            for (Map.Entry<String, Integer> entry : emotionMap.entrySet()) {
                emotionJSON.put(entry.getKey(), df.format(entry.getValue() / (double) posts.size()));
            }
        }else {
            for (Map.Entry<String, Integer> entry : emotionMap.entrySet()) {
                emotionJSON.put(entry.getKey(), 0);
            }
        }

        for (String emotionKeyword : emotionKeywords) {
            emotionKeywordsJSON.put(emotionKeyword);
        }

        JSONObject emotionInfo = new JSONObject();
        emotionInfo.put("Emotions", emotionJSON);
        emotionInfo.put("EmotionKeywords", emotionKeywords);

        return emotionInfo;
    }

    // Extract Emotions Per Post
    public static void getEmotionsProfileJSONPerPost(String post, JSONObject postProfile) throws JSONException {
//        System.out.println("emotion extracting: " + profile.getString("AuthorID"));

        Map<String, Double> emotionMap = new HashMap<>();
        Collection<String> emotionKeywords = new ArrayList<>();

        for (String emotion : emotionPatterns.keySet()) {
            emotionMap.put(emotion, 0.0);
        }

        extractEmotionsPerPost(post, emotionMap, emotionKeywords);

        if (!post.isEmpty()) {
            JSONObject emotionJSON = new JSONObject();
            JSONArray emotionKeywordsJSON = new JSONArray();
            int wordCount = Utils.countWords(post);

            if(wordCount > 0) {
                for (Map.Entry<String, Double> entry : emotionMap.entrySet()) {
                    emotionJSON.put(entry.getKey(), df.format(entry.getValue() / wordCount));
                }
            }

            for (String emotionKeyword : emotionKeywords) {
                emotionKeywordsJSON.put(emotionKeyword);
            }

            JSONObject emotionInfo;
            emotionInfo = new JSONObject();
            postProfile.put("EmotionInfo",emotionInfo);

            emotionInfo.put("Emotions", emotionJSON);
            String emptionKeywords = "";

            for(int i=0; i < emotionKeywordsJSON.length(); i++){
                emptionKeywords = emptionKeywords + emotionKeywordsJSON.getString(i) + ",";
            }

            emotionInfo.put("EmotionsMentions", emptionKeywords.trim());
            postProfile.put("EmotionInfo", emotionInfo);
        }

    }

    private static void extractEmotions(String text, Map<String, Integer> emotionMap, Collection<String> emotionKeywords) {
        String stemmedText = tokeniseAndStem(text);

        for (Map.Entry<String, List<Pair<String, Pattern>>> entry : emotionPatterns.entrySet()) {
            List<String> emotionList = getEmotionList(entry.getValue(), stemmedText);
            if (!emotionList.isEmpty()) {
                emotionMap.put(entry.getKey(), emotionMap.get(entry.getKey()) + emotionList.size());
                emotionKeywords.addAll(emotionList);
            }
        }
    }

    private static void extractEmotionsPerPost(String text, Map<String, Double> emotionMap, Collection<String> emotionKeywords) {
        String stemmedText = tokeniseAndStem(text);

        for (Map.Entry<String, List<Pair<String, Pattern>>> entry : emotionPatterns.entrySet()) {
            List<String> emotionList = getEmotionList(entry.getValue(), stemmedText);
            if (!emotionList.isEmpty()) {
                emotionMap.put(entry.getKey(), emotionMap.get(entry.getKey()) + emotionList.size());
                emotionKeywords.addAll(emotionList);
            }
        }
    }

    public static JSONObject getEmotionJSON(String text) {
        String stemmedText = tokeniseAndStem(text);

        JSONArray emotions = new JSONArray();
        JSONArray emotionText = new JSONArray();
        for (Map.Entry<String, List<Pair<String, Pattern>>> entry : emotionPatterns.entrySet()) {
            List<String> emotionList = getEmotionList(entry.getValue(), stemmedText);
            if (!emotionList.isEmpty()) {
                emotions.put(entry.getKey());
                for (String emotion : emotionList) {
                    emotionText.put(emotion);
                }
            }
        }

        JSONObject emotionJSON = new JSONObject();
        try {
            emotionJSON.put("Emotions", emotions);
            emotionJSON.put("EmotionKeywords", emotionText);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return emotionJSON;
    }

    private static List<String> getEmotionList(List<Pair<String, Pattern>> value, String text) {
        List<String> emotionList = new ArrayList<>();
        for (Pair<String, Pattern> pair : value) {
            Matcher m = pair.getValue().matcher(text);
            if (m.find()) {
                emotionList.add(pair.getKey());
            }
        }
        return emotionList;
    }

    private static String tokeniseAndStem(String text) {
        StringBuilder sentenceBuilder = new StringBuilder();

        for (String token : tokenizer.getTokens(text)) {
            sentenceBuilder.append(stemmer.stem(token)).append(" ");
        }

        return sentenceBuilder.toString().trim();
    }


    private static Map<String, List<Pair<String, Pattern>>> getPatternMap() {
        Map<String, List<Pair<String, Pattern>>> featureMap = new LinkedHashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./models/emotions/emotions_list.txt"));
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
                            String keywordStemmed = StemmerWraper.getStemmer().stem(keyword);
                            if (!keyword.isEmpty()) {
                                keywords.add(new Pair<>(keyword, Pattern.compile("\\b" + StringUtils.join(keywordStemmed.split("\\s+"), "\\s+") + "\\b", Pattern.CASE_INSENSITIVE)));
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


}
