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

import nlptasks.SentenceTokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnaphoraSentence {
    static final int MALE_NOUN = 11;
    static final int FEMALE_NOUN = 12;
    static final int NEUTRAL_NOUN = 13;
    static final int MALE_PRONOUN = 21;
    static final int FEMALE_PRONOUN = 22;
    static final int MALE_RESOLVED_PRONOUN = 31;
    static final int FEMALE_RESOLVED_PRONOUN = 32;
    static final int MALE_RESOLVED_NOUN = 41;
    static final int FEMALE_RESOLVED_NOUN = 42;
    static final int NONE = 0;

    static final int MALE = 1;
    static final int FEMALE = 2;
    static final int NEUTRAL = 3;
    static final int PRONOUN = 2;


    final String originalSentence;
    private String pronounResolvedSentence = null;
    String[] wordArray;
    int[] tagArray;
    final Map<Integer,String> resolutions = new HashMap<>();;
//    List<Integer> subjectWordIds;
    boolean hasNouns = false;
    boolean hasPronouns = false;
    boolean hasResolvedPronouns = false;

    public AnaphoraSentence(String originalSentence) {
        this.originalSentence = originalSentence;
        tokenizeSentence();
        tagTokens();
    }

//    public List<String> getOriginalSubjects(){
//        List<String> subjects = new ArrayList<>(subjectWordIds.size());
//        for(Integer id: subjectWordIds){
//            subjects.add(wordArray[id]);
//        }
//
//        return subjects;
//    }
//
//    public List<String> getResolvedSubjects(){
//        List<String> subjects = new ArrayList<>(subjectWordIds.size());
//        for(Integer id: subjectWordIds){
//            if(resolutions.containsKey(id)){
//                subjects.add(resolutions.get(id));
//            }else {
//                subjects.add(wordArray[id]);
//            }
//        }
//
//        return subjects;
//    }

    public List<String> getResolvedTokens(){
        List<String> tokens = new ArrayList<>(wordArray.length);
        for(int id = 0; id < wordArray.length; id++){
            if(resolutions.containsKey(id)){
                tokens.add(resolutions.get(id));
            }else {
                tokens.add(wordArray[id]);
            }
        }

        return tokens;
    }

    private void tokenizeSentence() {
//        this.wordArray = SubjectExtractor.getSubjectExtractor().getTokens(originalSentence);
        this.wordArray = SentenceTokenizer.getSentenceTokenizer().getTokens(originalSentence);
//        this.subjectWordIds = SubjectExtractor.getSubjectExtractor().getSubjectWordIds(wordArray);
        this.tagArray = new int[wordArray.length];
    }

    public String getPronounResolvedSentence() {
        if(pronounResolvedSentence == null){
            StringBuilder sb =  new StringBuilder();
            for(int wordID= 0; wordID < wordArray.length; wordID++){
                if(!resolutions.containsKey(wordID)){
                    sb.append(wordArray[wordID]).append(" ");
                }else {
                    sb.append(resolutions.get(wordID)).append(" ");
                }
            }
            pronounResolvedSentence = sb.toString();
        }
        return pronounResolvedSentence;
    }

    private void tagTokens() {
        for(int i=0; i< wordArray.length; i++){
            String word = wordArray[i];
            int tag = NONE;

            if(HumanListModified.isRelevantNoun(word)){
                hasNouns = true;
                if (HumanListModified.isMale(word)){
                    tag = MALE_NOUN;
                }else if(HumanListModified.isFemale(word)){
                    tag = FEMALE_NOUN;
                }else {
                    tag = NEUTRAL_NOUN;
                }
            }else if(HumanListModified.isPronoun(word)){
                hasPronouns = true;
                if (HumanListModified.isMale(word)){
                    tag = MALE_PRONOUN;
                }else if(HumanListModified.isFemale(word)){
                    tag = FEMALE_PRONOUN;
                }
            }

            tagArray[i] = tag;
        }
    }

    public static boolean isPronoun(int i) {
        return (i/10) == PRONOUN;
    }

    public static boolean isNoun(int i) {
        return (i != NONE) && !isPronoun(i);    }

    public static boolean isMaleNoun(int i) {
        return (i%10) == MALE;
    }

    public static boolean isFemaleNoun(int i) {
        return (i%10) == FEMALE;
    }

    public static boolean isNeutralNoun(int i) {
        return (i%10) == NEUTRAL;
    }

    public static boolean isMalePronoun(int i) {
        return (i== MALE_PRONOUN);
    }


    public static boolean isFemalePronoun(int i) {
        return (i== FEMALE_PRONOUN);
    }

    public String getResolution(int id) {
        if(!resolutions.containsKey(id)){
            return wordArray[id];
        } else{
            return resolutions.get(id);
        }
    }

    public int getNWords() {
        return wordArray.length;
    }

    public static boolean matchGender(int tag1, int tag2) {
        if(isNeutralNoun(tag1) || isNeutralNoun(tag2)){
            return  true;
        }else if((tag1 + tag2) % 2 ==0){
            return true;
        }else {
            return false;
        }
    }
}
