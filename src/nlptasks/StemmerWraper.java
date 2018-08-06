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
package nlptasks;

import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.stemmer.Stemmer;

public class StemmerWraper {
    private final Stemmer stemmer;
    private static StemmerWraper STEMMER_WRAPER;

    private StemmerWraper() {
        stemmer = new PorterStemmer();
    }

    public String stem(String text){
        return (String) stemmer.stem(text);
    }

    public static StemmerWraper getStemmer(){
        if(STEMMER_WRAPER == null){
            STEMMER_WRAPER = new StemmerWraper();
        }

        return STEMMER_WRAPER;
    }
}
