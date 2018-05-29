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

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Chammi on 01/07/2017.
 */
public class EmotionToCSVHelper {
    private static final String[] emotionsList = new String[]{
            "AFRAID", "ANGRY", "CONFUSED", "DEPRESSED", "HELPLESS", "HURT", "INDIFFERENT", "SAD",
            "ALIVE", "GOOD", "HAPPY", "INTERESTED", "LOVE", "OPEN", "POSITIVE", "STRONG"
    };
    private static final Set<String> positive_emotionsList = getPositiveEmotions();
    private static final Set<String> negative_emotionsList = getNegativeEmotions();
    private static final List<String> header = generateHeader();

    private static List<String> generateHeader() {
        List<String> _header = new ArrayList<>();
        for (String sideEffect : emotionsList) {
            _header.add("E:" + sideEffect);
        }
        _header.add("E:PositiveAgg");
        _header.add("E:NegativeAgg");
        return _header;
    }

    public static List<String> getEmotionHeader() {
        return header;
    }

    public static void addEmotionInfo(JSONObject emotionJASON, List<Object> values) throws JSONException {
        double positiveAgg = 0.0, negativeAgg = 0.0;
        for (String emotion : emotionsList) {
            double eVal = emotionJASON.getDouble(emotion);
            values.add(eVal);
            if (positive_emotionsList.contains(emotion)) {
                positiveAgg += eVal;
            } else if (negative_emotionsList.contains(emotion)) {
                negativeAgg += eVal;
            }
        }

        values.add(positiveAgg);
        values.add(negativeAgg);
    }

    public static void addZeroEmotionInfo(List<Object> values) {
        for (String emotion : header) {
            values.add(new Double(0.0));
        }
    }

    public static void addNAEmotionInfo(List<Object> values) {
        for (String emotion : header) {
            values.add("NA");
        }
    }


    private static Set<String> getPositiveEmotions() {
        Set<String> _positive_emotionsList = new HashSet<>();
        for (String s : new String[]{"ALIVE", "GOOD", "HAPPY", "INTERESTED", "LOVE", "OPEN", "POSITIVE", "STRONG"}) {
            _positive_emotionsList.add(s);
        }
        return _positive_emotionsList;
    }

    private static Set<String> getNegativeEmotions() {
        Set<String> _negative_emotionsList = new HashSet<>();
        for (String s : new String[]{"AFRAID", "ANGRY", "CONFUSED", "DEPRESSED", "HELPLESS", "HURT", "INDIFFERENT", "SAD"}) {
            _negative_emotionsList.add(s);
        }
        return _negative_emotionsList;
    }
}
