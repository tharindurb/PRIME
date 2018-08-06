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

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Chammi on 12/01/2017.
 */
public class PSAExtractor {
    private static final Pattern PSAPattern = Pattern.compile("\\b(PSA)(\\s+[^\\s]+){5,5}\\b",Pattern.CASE_INSENSITIVE);
    private static final Pattern NumberPattern1 = Pattern.compile("\\d+(\\.\\d+){0,1}",Pattern.CASE_INSENSITIVE);
//    private static final Pattern NumberPattern2 = Pattern.compile("(\\d{1,2})\\/(\\d{1,2})",Pattern.CASE_INSENSITIVE);


    public static void main(String... args){
        List<String> samples = getPSASamples();
        for(String sample: samples){
            List<Double> psaValues = getPSAList(sample);
            System.out.println(psaValues + " P: " + sample);
        }
    }

    public static void getPSAProfileJSON(JSONObject profile, List<String> posts, String about) throws JSONException {
        System.out.println("PSA extracting: " + profile.getString("AuthorID"));

        if(!posts.isEmpty()) {
            if(!about.isEmpty()){
                List<String> postsNew = posts.stream().map(post -> post + ". " + about).collect(Collectors.toList());
                posts = postsNew;
            }

            JSONObject psaJSON = getPSAJSON(posts);
            profile.put("PSAInfo", psaJSON);
            System.out.println(psaJSON);
        }
    }

    private static JSONObject getPSAJSON(List<String> posts) {
        JSONObject psaJSON = new JSONObject();
        Map<Double, Integer> psaScoreMap = getPSAMap(posts);

        if (!psaScoreMap.isEmpty()) {
            double max = 0D;
            double min = 1000D;
            JSONArray scores = new JSONArray();
            for (Map.Entry<Double, Integer> entry : psaScoreMap.entrySet()) {
                scores.put(entry.getKey());
                if ((entry.getValue() * 20) >= posts.size()) {
                        max = Math.max(max, entry.getKey());
                        min = Math.min(min, entry.getKey());
                }
            }

            try {
                if (max > 0) {
                    psaJSON.put("max", max);

                }

                if(min <100){
                    psaJSON.put("min", min);
                }


                psaJSON.put("scores", scores);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return psaJSON;
    }

    private static Map<Double, Integer> getPSAMap(List<String> posts) {
        Map<Double, Integer> gleasonScoreMap = new HashMap<>();

        for (String post : posts) {
            for (Double score : getPSAList(post)) {
                if (!gleasonScoreMap.containsKey(score)) {
                    gleasonScoreMap.put(score, 1);
                } else {
                    gleasonScoreMap.put(score, gleasonScoreMap.get(score) + 1);
                }
            }
        }
        return gleasonScoreMap;
    }

    private static List<Double> getPSAList(String text){
        List<String> phrases = getPSAPhrases(text);
        List<Double> psaValues = new ArrayList<>();
        for(String phrase: phrases){
            double value = getPSAValue(cleanText(phrase));
            if(value > 0D) psaValues.add(value);
        }
        return psaValues;
    }

    private static double getPSAValue(String text) {
        Matcher m1 = NumberPattern1.matcher(text);
        if(m1.find()){
            return Double.parseDouble(m1.group(0));
        }else {
            return -1.0D;
        }
//        else {
//            Matcher m2 = NumberPattern2.matcher(text);
//            if(m2.find()){
//                return Integer.parseInt(m2.group(1)) / (double)Integer.parseInt(m2.group(2));
//            }
//            else{
//                return -1.0D;
//            }
//        }
    }

    private static String cleanText(String text){
        text = text.replaceAll("\\d{4,4}","");
        text = text.replaceAll("\\d{1,2}\\/\\d{1,2}","");
        return text;
    }

    public static List<String> getPSAPhrases(String text){
        text = Utils.removeUnicode(text);
        text = text.replaceAll("[\":?;<>(),!=]", " ");

        Matcher matcher = PSAPattern.matcher(text + " <Z> <Z> <Z> <Z> <Z> ");
        List<String> psaPhrases = new ArrayList<>();

        if (matcher.find()) {
            do {
                psaPhrases.add(matcher.group(0));
            } while (matcher.find(matcher.end(1)));
        }
        return psaPhrases;
    }


    private static List<String> getPSASamples() {
        List<String> samples = new ArrayList<>();
        samples.add("all good 5 year post radical prostatectomy 4 year post radiation good news PSA < 0.05");
        samples.add("Diagnosed 11/07 at age 57 with PSA 23, Gleason 4+5=9 in all six cores.");
        samples.add("2012 PSA April 2012--3.6 PSA May 2012--2.5 PSA Aug 2012--2.2 PSA Nov 2012--2.9 PSA Feb 2013--2.8 PSA May 2013--2.1 PSA Aug 2013--2.3 PSA Nov 2013--2.5 PSA May 2014--1.1 PSA Dec 2014--0.8 PSA Jun 2015--0.5 PSA Jan. 2016--0.4 PSA Aug. 2016--0.4");
        samples.add("Steve n Dallas - The numbers are mine.\\n  56 yrs old, excellent health - Feb '11 - DRE - enlarged PSA 6.0 Feb '11 - DRE - enlarged PSA 8.8 Mar '11 - Biopsy 12 of 12 Negative Sept '11 - DRE - enlarged PSA 11.5 Dec '11 - DRE - enlarged PSA 5.9 Free PSA 13% Aug '12 - PSA 7.1 Free PSA 13% May '13 - DRE - enlarged PSA 7.9 Free PSA 13%");
        samples.add("A free PSA test was done because of the Slightly elevated PSA. The Free PSA value was 49.");
        samples.add("Surgery. Had a 1mm positive margin a little over a year ago 40yo PSA 12.05 5/12 PSA 14.5 9/12 PSA 14.6 10/12 PSA 16.5 11/12 PSA 13.9");

        return samples;
    }
}
