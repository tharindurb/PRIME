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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SurgeryDecisionCategory {
    private static Map<String, List<Pair<String,Pattern>>> patternMap = loadPatterns();

    public static List<String> getHeader() {
        return header;
    }

    private static List<String> header = loadHeader();

    private static List<String> loadHeader() {
        List<String> _header = new ArrayList<>(patternMap.size());
        for(String s: patternMap.keySet()){
            _header.add(s);
        }
        return _header;
    }

    public static Set<String> getTokens(){
        Set<String> tokens = new HashSet<>();
        for(List<Pair<String,Pattern>> patternList: patternMap.values()){
            for(Pair<String,Pattern> p: patternList){
                for(String tok: p.getKey().split("\\s+")){
                    tokens.add(tok.trim().toLowerCase());
                }
            }
        }
        return tokens;
    }

    private static Map<String, List<Pair<String,Pattern>>> loadPatterns() {
        Map<String, List<Pair<String,Pattern>>> _patternMap = new LinkedHashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("models/surgeryKeywords/surgeryDecisionCatergories.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                line  = line.trim();
                String[] split = line.split(":");
                String key = split[0].trim();
                List<Pair<String,Pattern>> _patterns = new ArrayList<>();

                for (String keyword : split[1].trim().split(",")) {
                    keyword = keyword.trim();
                    if (!keyword.isEmpty()) {
                        _patterns.add(new Pair<>(keyword, Pattern.compile("\\b" + StringUtils.join(keyword.split("\\s+"), "\\s+") + "\\b", Pattern.CASE_INSENSITIVE)));
                    }
                }
                if(!_patterns.isEmpty()) _patternMap.put(key, _patterns);
            }
            reader.close();
        }  catch (IOException e) {
            e.printStackTrace();
        }

        return _patternMap;
    }

    public static boolean hasDecisionCategory(String category, String text){
        for (Pair<String, Pattern> pair : patternMap.get(category)) {
            Matcher m = pair.getValue().matcher(text);
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

//    public static List<String> getDecisionPhrases(String text){
//        List<String> decisionPhrases = new ArrayList<>();
//        for (Pair<String, Pattern> pair : patterns) {
//            Matcher m = pair.getValue().matcher(text);
//            if (m.find()) {
//                decisionPhrases.add(pair.getKey());
//            }
//        }
//        return decisionPhrases;
//    }

    public static void main(String... args) {
        SurgeryDecisionCategory decisionTermExtractor = new SurgeryDecisionCategory();
        System.out.println(decisionTermExtractor.patternMap);

    }
}
