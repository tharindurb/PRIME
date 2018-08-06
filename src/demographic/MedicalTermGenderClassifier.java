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
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MedicalTermGenderClassifier {
    private static final List<Pattern> femaleMedicalTerms = getPatternsFromFile("./models/genderClassifier/female_medical_terms.txt");
    private static final List<Pattern> maleMedicalTerms = getPatternsFromFile("./models/genderClassifier/male_medical_terms.txt");

    public static JSONObject getGenderInfo(String text){
        JSONObject genderInfo = new JSONObject();
        JSONArray maleMedicalTermsArray = new JSONArray();
        JSONArray femaleMedicalTermsArray = new JSONArray();

        try {

            for (Pattern pattern : maleMedicalTerms) {
                Matcher m = pattern.matcher(text);
                if (m.find()) {
                    maleMedicalTermsArray.put(m.group(0));
                }
            }

            for (Pattern pattern : femaleMedicalTerms) {
                Matcher m = pattern.matcher(text);
                if (m.find()) {
                    femaleMedicalTermsArray.put(m.group(0));
                }
            }

            genderInfo.put("MaleMedicalTerms",maleMedicalTermsArray);
            genderInfo.put("FemaleMedicalTerms",femaleMedicalTermsArray);
        }catch (JSONException e){
            e.printStackTrace();
        }

        return genderInfo;
    }

    private static List<Pattern> getPatternsFromFile(String fileName) {
        List<Pattern> patterns = new ArrayList<>();
        try{
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String sLine;

            while ((sLine = reader.readLine()) != null){
                sLine = sLine.trim();
                if(Character.isUpperCase(sLine.charAt(0))) {
                    patterns.add(Pattern.compile("\\b" + StringUtils.join(sLine.trim().split("\\s+"), "\\s+") + "\\b"));
                }else {
                    patterns.add(Pattern.compile("\\b" + StringUtils.join(sLine.trim().split("\\s+"), "\\s+") + "\\b", Pattern.CASE_INSENSITIVE));
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        return patterns;
    }
}
