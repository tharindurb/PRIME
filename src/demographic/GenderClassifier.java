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

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GenderClassifier {

    public static void main(String[] args) {

        List<String> samples = new ArrayList<>();


        for(String text: samples) {
            System.out.println(text);
            System.out.println(getGenderInformation(text));
            System.out.println();
        }
    }

    public static JSONObject getGenderInformation(String text){
        JSONObject genderInfo = new JSONObject();
        PronounResolver pronounResolver = new PronounResolver(text);

        try {
            genderInfo.put("NounBasedGender", pronounResolver.getGenderInfo());
            genderInfo.put("MedicalTermBasedGender", MedicalTermGenderClassifier.getGenderInfo(text));
        }catch (JSONException e){
            e.printStackTrace();
        }
        return genderInfo;
    }




}
