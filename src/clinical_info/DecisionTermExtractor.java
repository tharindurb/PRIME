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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecisionTermExtractor {
    private static Map<String,List<Pair<String,Pattern>>> patterns = loadPatterns();

    private static Map<String,List<Pair<String,Pattern>>> loadPatterns() {
        Map<String,List<Pair<String,Pattern>>> _patterns = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("models/surgeryKeywords/decisionWords_TakingDecision.txt"));
            String line;
//            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(":");
                List<Pair<String,Pattern>> keywords = new ArrayList<>();
                for (String keyword : split[1].trim().split(",")) {
                    keyword = keyword.trim();
                    if (!keyword.isEmpty()) {
                        keywords.add(new Pair<>(keyword, Pattern.compile("\\b" + StringUtils.join(keyword.split("\\s+"), "\\s+") + "\\b", Pattern.CASE_INSENSITIVE)));
                    }
                }
                _patterns.put(split[0].trim(),keywords);
            }
            reader.close();
        }  catch (IOException e) {
            e.printStackTrace();
        }

        return _patterns;
    }

    public static boolean hasDecisionPhrases(String key, String text){
        for (Pair<String, Pattern> pair : patterns.get(key)) {
            Matcher m = pair.getValue().matcher(text);
            if (m.find()) {
               return true;
            }
        }
        return false;
    }

    public static List<String> getDecisionPhrases(String key,String text){
        List<String> decisionPhrases = new ArrayList<>();
        for (Pair<String, Pattern> pair : patterns.get(key)) {
            Matcher m = pair.getValue().matcher(text);
            if (m.find()) {
                decisionPhrases.add(pair.getKey());
            }
        }
        return decisionPhrases;
    }

    public static void main(String... args) {
        DecisionTermExtractor decisionTermExtractor = new DecisionTermExtractor();
        System.out.println(decisionTermExtractor.patterns);

    }

}
