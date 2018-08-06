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

import java.util.*;

public class SideeffectToCSVHelper {

    private static final String[] sideEffectHeader = new String[]{"Tiredness", "Bladder Irritation", "Rectal irritation",
            "Urinary incontinence", "Urethral strictures", "Rectal irritation/bleeding",
            "Erectile dysfunction", "Bleeding", "Infection", "Clot", "Stroke", "Bladder neck contracture", "Hernia"};
    private static final Map<String,List<String>> sideEffectCategoryHeader = generateSideEffectCategoryMap();

    private static Map<String,List<String>> generateSideEffectCategoryMap() {
        Map<String,List<String>> _sideEffectCategoryMap = new TreeMap<>();
        _sideEffectCategoryMap.put("Urinary Symptoms", Arrays.asList(new String[]{"Bladder Irritation", "Urinary incontinence", "Urethral strictures", "Bladder neck contracture"}));
        _sideEffectCategoryMap.put("Bowel Symptoms", Arrays.asList(new String[]{"Rectal irritation", "Rectal irritation/bleeding"}));
        _sideEffectCategoryMap.put("Sexual Symptoms", Arrays.asList(new String[]{"Erectile dysfunction"}));
        _sideEffectCategoryMap.put("Other", Arrays.asList(new String[]{"Tiredness", "Bleeding", "Infection", "Clot", "Stroke", "Hernia"}));
        return _sideEffectCategoryMap;
    }

    private static final List<String> header = generateHeader();

    private static List<String> generateHeader() {
        List<String> _header = new ArrayList<>();
        for (String sideEffect: sideEffectHeader) {
            _header.add("SI:" + sideEffect);
        }
        for (Map.Entry<String, List<String>> entry : sideEffectCategoryHeader.entrySet()) {
            _header.add("SC:"+ entry.getKey());
        }
        return _header;
    }

    public static List<String> getSideEffectHeader() {
        return header;
    }

    public static void addZeroSideEffectInfo(List<Object> values){
        for(String entry: header) values.add(0);
    }

    public static void addNASideEffectInfo(List<Object> values){
        for(String entry: header) values.add("NA");
    }

    public static void addSideEffectInfo(JSONArray sideeffectJSON, List<Object> values){
        Set<String> sideeffectsMentioned = new HashSet<>();

        try {
            for (int i = 0; i < sideeffectJSON.length(); i++) {
                sideeffectsMentioned.add(sideeffectJSON.getString(i).trim());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (String sideEffect: sideEffectHeader) {
            if(sideeffectsMentioned.contains(sideEffect)) {
                values.add(1);
            }else {
                values.add(0);
            }
        }

        for (Map.Entry<String, List<String>> entry : sideEffectCategoryHeader.entrySet()) {
            boolean hasCategory = false;
            for(String sideEffect: entry.getValue()){
                if(sideeffectsMentioned.contains(sideEffect)) {
                    hasCategory = true;
                    break;
                }
            }
            if(hasCategory) {
                values.add(1);
            }else {
                values.add(0);
            }
        }
    }
}
