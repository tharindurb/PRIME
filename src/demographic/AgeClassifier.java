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
package demographic;

import nlptasks.SentenceSplitter;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import util.Utils;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgeClassifier {
    private static final Pattern agePattern = Pattern.compile("(([^\\s]+\\s+)[^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+[^\\s]+)\\s+(\\d{1,2}\\s*s{0,1})\\s+([^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+[^\\s]+)");
    private static final Pattern agePatternNew = Pattern.compile("(([^\\s]+\\s+)[^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+[^\\s]+)\\s+(\\d{1,2}[a-zA-Z]{0,5})\\s+([^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+[^\\s]+)");
    private static final Pattern agePatternNewNew = Pattern.compile("(([^\\s]+\\s+)[^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+[^\\s]+)\\s+(\\d{1,2}[a-zA-Z]{0,5})[.!?\\\\-]*\\s+([^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+[^\\s]+\\s+[^\\s]+)");
    private static Classifier classifier;
    private static Instances ageTrainingData;
    private static AgeClassifier ageClassifier;
    private static final double ageConfidenceThreshold = 0.65D;

    public AgeClassifier() {

        try {
//                    String method = "NaiveBayes";
            String method = "RandomForest";
            classifier = (Classifier) SerializationHelper.read("./models/ageClassifier/AgeClassifier_Prostate_"+method+".model");
//            classifier = (Classifier) SerializationHelper.read("./models/ageClassifier/AgeClassifier_"+method+".model");
            BufferedReader reader = new BufferedReader(new FileReader("./models/ageClassifier/age_phrase_features.arff"));
            ageTrainingData = new Instances(reader);
            reader.close();
            ageTrainingData.setClassIndex(ageTrainingData.numAttributes() - 1);
        }catch (IOException e){
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        AgeClassifier ageClassifier = new AgeClassifier();
    }

    public JSONArray getHighConfidenceAgeMentions(String text){

        JSONArray ageFeaturesJSON = new JSONArray();
        try {
            for(AgeFeature ageFeature: getAgeFeatures(text)){
                if (ageFeature.classValue.equalsIgnoreCase("age") && ageFeature.confidence > ageConfidenceThreshold) {
                    ageFeaturesJSON.put(ageFeature.getAgeFeatureJSON());
//                    System.out.println(ageFeature.getAgeFeatureJSON());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ageFeaturesJSON;
    }

    private List<AgeFeature> getAgeFeatures(String text) throws Exception {
        int i = 0;
        List<AgeFeature> ageFeatures = new ArrayList<>();
        for (String sentence : SentenceSplitter.getSentenceSplitter().sentDetect(text)) {
//
            for (AgeFeature ageFeature : getAgeFeatures(sentence, ++i)) {
                Instance instance = new Instance(ageTrainingData.numAttributes());
                instance.setDataset(ageTrainingData);

                Map<String, Integer> features = ageFeature.getFeatureMap();
                for (int attributeNo = 0; attributeNo < ageTrainingData.numAttributes() - 1; attributeNo++) {
                    Attribute attribute = ageTrainingData.attribute(attributeNo);
                    instance.setValue(attribute.index(), features.get(attribute.name()));
                }

                int result = (int) classifier.classifyInstance(instance);
                ageFeature.classValue = ageTrainingData.classAttribute().value(result);
                ageFeature.confidence = classifier.distributionForInstance(instance)[result];
                ageFeatures.add(ageFeature);
            }
        }
        return ageFeatures;
    }

    private static List<AgeFeature> getAgeFeatures(String text, int sentenceNo) {
        text  = Utils.removePunctuations(text);
        text = text.trim();
//        if (text.length() > 1 && text.charAt(text.length() - 1) == '.') {
//            text = text.substring(0,text.length()-1);
//        }

        Matcher matcher = agePatternNewNew.matcher("<Z> <Z> <Z> <Z> <Z> " + text + " <Z> <Z> <Z> <Z> <Z>");
        List<AgeFeature> list = new ArrayList<>();

        if (matcher.find()) {
            do {
                list.add(new AgeFeature(text, matcher.group(0), matcher.group(1), matcher.group(3), matcher.group(4), sentenceNo));
            } while (matcher.find(matcher.end(2)));
        }
        return list;
    }



    public static AgeFeature getAgeFeaturesFromLabelledData(String text, int sentenceNo) {
        Matcher matcher = agePatternNewNew.matcher(text);

        if(matcher.find()) {
            return new AgeFeature(text, matcher.group(0), matcher.group(1), matcher.group(3), matcher.group(4), sentenceNo);
        }else {
            return null;
        }
    }

    public static AgeClassifier getAgeClassifier(){
        if(ageClassifier != null){
            ageClassifier = new AgeClassifier();
        }

        return ageClassifier;
    }
}
