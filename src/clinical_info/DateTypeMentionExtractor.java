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

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotator;
import edu.stanford.nlp.time.TimeExpression;
import edu.stanford.nlp.util.CoreMap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateTypeMentionExtractor {
    private static final StanfordCoreNLP stanfordCoreNLP = getStanfordCoreNLP();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

    private static StanfordCoreNLP getStanfordCoreNLP() {
        Properties properties = new Properties();
        properties.setProperty("annotators", "tokenize , ssplit , pos");
        StanfordCoreNLP stanfordCoreNLP = new StanfordCoreNLP(properties);
        stanfordCoreNLP.addAnnotator(new TimeAnnotator("sutime", new Properties()));

        return stanfordCoreNLP;
    }

    public static Set<String> getDates(String text)
    {
        Set<String> dates = new HashSet<>();
        Annotation annotation = stanfordCoreNLP.process(text);
        annotation.set(CoreAnnotations.DocDateAnnotation.class, "23-05-2017");

        for (CoreMap cm : annotation.get(TimeAnnotations.TimexAnnotations.class)) {
            String dateStr = cm.get(TimeExpression.Annotation.class).getTemporal().toString();
            try {
                Date d = sdf.parse(dateStr);
                dates.add(dateStr);
            }catch (ParseException p)
            {
//                System.out.println(dateStr);
            }
        }

        return dates;
    }

    public static boolean hasDate(String text)
    {
        Annotation annotation = stanfordCoreNLP.process(text);
        annotation.set(CoreAnnotations.DocDateAnnotation.class, "23-05-2017");

        for (CoreMap cm : annotation.get(TimeAnnotations.TimexAnnotations.class)) {
            String dateStr = cm.get(TimeExpression.Annotation.class).getTemporal().toString();
               // System.out.println(dateStr);
//                Date d = sdf.parse(dateStr);
                return true;

        }

        return false;
    }

    public static void main(String[] args){
        List<String> sample  = new ArrayList<>();
        sample.add("blah balah balh baah 35 fdfd 23 4");
        sample.add("had surgery 3 months ago");
        sample.add("had surgery three months ago");
        sample.add("following Ornish/Michael Milken-style diet RARP March 2015");
        sample.add("8/2011 RRP@Henry Ford PostOp");
        sample.add("Da Vinci prostatectomy 2/2/2011 Taking 20 mg levitra daily June 2011");
        sample.add("he had his surgery on 2/3/11");
        sample.add("Robotic surgery July 23, 2008");
        sample.add("DaVinci: 06/2011");
        sample.add("Davinci Feb. 5th 2010/");
        sample.add("01/06 open RP");

        for(String s: sample){
            Boolean b = hasDate(s);
          //  System.out.println(b + ": " + s);
        }

    }


}
