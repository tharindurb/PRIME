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
import util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecisionConfirmationExtractor {
    private final static List<Pair<String,Pattern>> decisionKeyWords = getDecisionKeyWords();

    private static List<Pair<String,Pattern>> getDecisionKeyWords() {

        List<Pair<String,Pattern>> _decisionKeyWords = new ArrayList<>();

        Scanner scan = null;
        File file = new File("./models/surgeryKeywords/decisionWords_Confirmation.txt");

        try {
            scan = new Scanner(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while(scan.hasNextLine()){
            String keyword = scan.nextLine().trim();
            if(!keyword.isEmpty()) {
                _decisionKeyWords.add(new Pair<>(keyword,Pattern.compile("\\b" + StringUtils.join(keyword.split("\\s+"), "\\s+") + "\\b", Pattern.CASE_INSENSITIVE)));
            }

        }

        return _decisionKeyWords;
    }

    public static String processPostToExtractSurgeryDecision(String keyWord, String postContent){
        postContent = Utils.removePunctuations(postContent);
        postContent = Utils.removeUnicode(postContent);
        postContent = "<Z> <Z> <Z> <Z> <Z> "  + postContent + " <Z> <Z> <Z> <Z> <Z>";
        String matchSurgeryKeyWord = "";

        Pattern pattern = Pattern.compile("([^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+)" + keyWord, Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(postContent);
        String supportingWords = "";
        boolean matchFound = false;

        if (matcher.find()) {
            do {
                supportingWords = matcher.group(1).trim();
                for (Pair<String,Pattern> decisionPattern : decisionKeyWords) {
                    if (!supportingWords.isEmpty()) {
                        Matcher matcherDecision = decisionPattern.getValue().matcher(supportingWords);
                        if(matcherDecision.find()) {
                            return keyWord;
                        }
                    }
                }
            } while (matcher.find(matcher.end(1)));
        }



        if(!matchFound){
            Pattern datePattern = Pattern.compile("([^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+)" + keyWord + "\\s+([^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+)", Pattern.CASE_INSENSITIVE);
            Matcher dateMatcher = datePattern.matcher(postContent);
            if (dateMatcher.find()){
                String supportingWords1 = dateMatcher.group(1);
                String supportingWords2 = dateMatcher.group(2);

                boolean mentions1 = DateTypeMentionExtractor.hasDate(supportingWords1);
                if(mentions1){
                    return keyWord;
                }
                boolean mentions2 = DateTypeMentionExtractor.hasDate(supportingWords2);

                if(mentions2){
                    return keyWord;
                }
            }
        }
        return matchSurgeryKeyWord;
    }
}
