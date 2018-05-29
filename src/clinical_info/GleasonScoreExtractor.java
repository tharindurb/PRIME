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

public class GleasonScoreExtractor {
    private static final Pattern gleasonSumPattern = Pattern.compile("\\b(gleson|gleeson|gleason|g|gs)(\\s+[^\\s\\d]+){0,2}\\s*(\\d\\s*\\+\\s*\\d)", Pattern.CASE_INSENSITIVE);
    private static final Pattern gleasonFullPattern = Pattern.compile("\\b(gleson|gleeson|gleason|g|gs)(\\s+[^\\s\\d]+){0,2}\\s*(\\d{1,2})(\\s*[^\\s\\+])", Pattern.CASE_INSENSITIVE);
    private static final Pattern gleasonSumPatternGeneral  = Pattern.compile("\\b(\\d\\s*\\+\\s*\\d)\\b", Pattern.CASE_INSENSITIVE);


    public static void main(String... args) {
        List<String> samples = new ArrayList<>();
        samples.addAll(getGleasonSumSamples());
        samples.addAll(getGleasonFullSamples());

        for (String text : samples) {
            List<Integer> gleasonScores = getGleasonScoreValues(text);
            System.out.println(gleasonScores);
        }

    }

    public static void getGleasonScoreProfileJSON(JSONObject profile, List<String> posts, String about) throws JSONException {
        System.out.println("gleason extracting: " + profile.getString("AuthorID"));


        if(!posts.isEmpty()) {
            if(!about.isEmpty()){
                List<String> postsNew = posts.stream().map(post -> post + ". " + about).collect(Collectors.toList());
                posts = postsNew;
            }

            JSONObject gleasonJSON = getGleasonScoreJSON(posts);
            profile.put("GleasonInfo", gleasonJSON);
        }
    }

    public static JSONObject getGleasonScoreJSON(List<String> posts) {
        JSONObject gleasonJSON = new JSONObject();
        Map<Integer, Integer> gleasonScoreMap = getGleasonMap(posts);

        if (!gleasonScoreMap.isEmpty()) {
            int max = 0;
            int min = 100;
            JSONArray scores = new JSONArray();
            for (Map.Entry<Integer, Integer> entry : gleasonScoreMap.entrySet()) {
                scores.put(entry.getKey());
                if ((entry.getValue() * 10) >= posts.size()) {
                    if(entry.getKey() <= 10 && entry.getKey() >= 2) {
                        max = Math.max(max, entry.getKey());
                        min = Math.min(min, entry.getKey());
                    }
                }
            }

            try {
                if (max > 0) {
                    gleasonJSON.put("max", max);

                }

                if(min <100){
                    gleasonJSON.put("min", min);
                }


                gleasonJSON.put("scores", scores);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return gleasonJSON;
    }

    private static Map<Integer, Integer> getGleasonMap(List<String> posts) {
        Map<Integer, Integer> gleasonScoreMap = new HashMap<>();

        for (String post : posts) {
            for (Integer score : getGleasonScoreValues(post)) {
                if (!gleasonScoreMap.containsKey(score)) {
                    gleasonScoreMap.put(score, 1);
                } else {
                    gleasonScoreMap.put(score, gleasonScoreMap.get(score) + 1);
                }
            }
        }
        return gleasonScoreMap;
    }

    private static List<Integer> getGleasonScoreValues(String text) {

        text = Utils.removeUnicode(text);
        text = text.replaceAll("[\":?\\.;<>()\\[\\],!/-]", " ");

        List<Integer> gleasonScoreValues = new ArrayList<>();

        if (!text.isEmpty()) {
            getGleasonScoreValues_SumPattern(text, gleasonScoreValues);
            getGleasonScoreValues_FullPattern(text, gleasonScoreValues);
        }
        return gleasonScoreValues;
    }

    private static void getGleasonScoreValues_FullPattern(String text, List<Integer> gleasonScoreValues) {

        Matcher matcher = gleasonFullPattern.matcher("<S> " + text + " <S>");

        if (matcher.find()) {
            do {
                gleasonScoreValues.add(Integer.parseInt(matcher.group(3).trim()));
            } while (matcher.find());
        }
    }

    private static void getGleasonScoreValues_SumPattern(String text, List<Integer> gleasonScoreValues) {

        Matcher matcher = gleasonSumPattern.matcher("<S> " + text + " <S>");

        if (matcher.find()) {
            do {
                int sum = 0;
                for (String val : matcher.group(3).trim().split("\\+")) {
                    sum += Integer.parseInt(val.trim());
                }
                gleasonScoreValues.add(sum);
            } while (matcher.find());
        }else {
            matcher = gleasonSumPatternGeneral.matcher("<S> " + text + " <S>");
            if (matcher.find()) {
                do {
                    int sum = 0;
                    for (String val : matcher.group(1).trim().split("\\+")) {
                        sum += Integer.parseInt(val.trim());
                    }
                    gleasonScoreValues.add(sum);
                } while (matcher.find());
            }
        }
    }


    private static List<String> getGleasonSumSamples() {
        List<String> samples = new ArrayList<>();

//        samples.add("Age 58, PSA 4.47 Biopsy - 2/12 cores , Gleason 4 + 5 = 9 Da Vinci, Cleveland Clinic  4/14/09   Nerves spared, but carved up a little.");
//        samples.add("Father's Age 62 (now 63) Original Gleason 3+4=7, Post-Op Gleason- 4+3=7, DaVinci Surgery Aug 31, 2007 Focally Positive Right Margin, One positive node. T3a N1 M0. Bone Scan/CT Negative (Sept. 10, 2007) Oct. 17 PSA 0.07 Nov. 13 PSA 0.05 Casodex adm. Nov 07, Lupron beg. Dec 03, 2007 2 yrs Radiation March 03-April 22, 2008- 8 weeks 5x a week July 2, 08 PSA <.02 Praying for a cured dad. Co-Moderator Prostate Cancer Forum");
//        samples.add("Prostate Biopsy-3 of 12 cores Positive Volume 97 Gleason 4+4=8 T2c n0m0 Bone&CT=neg MRI-1 nerve bundle involved open RRP 8/5/11,home 8/6/11,cath out 8/16/11 Post Op Path 71g,5.5x5.0x5.0 cm Gleason upgraded 5+4=9 13 lymph nodes,SVI neg Margin,PNI-pos pT3aN0Mx");
//        samples.add("Nellie Age 59 Gleason 9 1/10 PSA 14.7 5/10 Bx: Gleason 3+4 8/4/2010 RRP: Gleason 4+5; Positive Margins, PNI Incontinence: N/A; ED: 70% Until 4/11, PSA <.01; 4/11: .01; 6/11: .03 ADT3 started 7/20; WPRT started 8/22");
//        samples.add("Age: 58, 56 dx, PSA: 7/07 5.8, 10/08 16.3 3rd Biopsy: 9/08 7 of 7 Positive, 40-90%, Gleason 4+3 open RP: 11/08, on catheters for 101 days Path Rpt: Gleason 3+4, pT2c, 42g, 20% cancer, 1 pos margin Incont & ED: None Post Surgery PSA: 2/09 .05,5/09 .1, 6/09 .11. 8/09 .16 Post SRT PSA: 1/10 .12, 4/8 .04, 8/6 .06, 2/11 1.24, 4/11 3.81, 6/11 5.8 Latest: 6 Corr Surgeries to Bladder Neck, SP Catheter since 10/1/9, SRT 39 Sess/72 gy ended 11/09, 21 Catheters, Ileal Conduit Surgery 9/10");
//        samples.add("I guess we are in the same boat. I'll go back for my 2nd PSA in November and the Dr. has already said I'll probably go see a oncologist no matter the results because of the positive path report. Age 44\\nPSA 4.8\\nGleason 3+3=6\\nT2\\nBiopsy - 3 of 12 (2@20% / 1@80%) \\nDa Vinci 7/31/07 @ Duke\\n2.5 hr. surgery, released from Duke with in 14 hrs.\\nSaved both nerve bundles. \\nFoley out 8/10/08\\nPositive Pathology Report \\nNear left side nerve bundle\\nNew Gleason 7\\nOncology visit in the near future for radiation\\nBack to work 3 weeks following surgery - Sales\\n \\n9/13/07 - 1 Post PSA Undetectable less than 0.1 \\n \\n9/17/07 - Pad Free!");
//        samples.add("but his PSA was always high, below I listed it out by date since 2006. The Dr. gave him a score of 4+3=7, Grade 3 after his prostate biopsy. From MRI and bone Scan, doesn't seem to have spread in the bone or chest.");
//        samples.add("If you'll notice in my sig it went from 4+4=8 (in 4 of 8 cores), to 3+4=7, a huge difference in my favor");
//         samples.add("samples on the right side, 1% of the gland envolved, Gleason score 3+3 2nd expert opinionon");
        return samples;
    }

    private static List<String> getGleasonFullSamples() {

        List<String> samples = new ArrayList<>();

//        samples.add("LupronJim 65 - DX 64 Feb 2013 PSA 3.68 (6 mo doubling) Gleason 9 (4+5) T1CN0M1B stage IV w. 7 of 12 cores worst ones 70% right perineural");
//        samples.add("Age 56 > DRE 12/22/08 > PSA 3.4 on Jan 09 > PSA 3.8 on Feb 09 > Biopsy 3/9/09 3 out of 12 positive > Gleason 7 > cat scan neg > divinci robotic surgery 4/23/09 at Mount Sinai NY >");
//        samples.add("ard \"You have cancer, about 80% involved, all Gleason 9.\" Dave (grandpaof4), that's when suddenly I couldn't catch my breath either. Uro said it's serious but very likel");
//        samples.add("gleason score of 9");
//        samples.add("And do I see that you were a Gleason 5 back 22 years ago");
//        samples.add("ok thanks guys i am scared to death of everything going on with my dad i feel like every time we go back there is a little more bad news to get i was surprised to hear that he had bone mets but lymph nodes are clear and also surprised that last year his psa was 5.0 and now is .3 and all scans were clear last year but not this year i do know his gleason is a 10");
//        samples.add("High Volume Advance Prostate Cancer (G-9, last Oct. 1st. Cancer has spread from my right skull down two my toes");
        samples.add("infiltration. Gleason score of 6 made me a candidate for Brachytherapy");
        return samples;
    }


}
