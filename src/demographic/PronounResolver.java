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
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PronounResolver {

    class NounCandidate {
        final int sentID;
        final int wordID;
        int tag;
        private final String word;
        private String resolution = null;
        private final int candidateID;

        public NounCandidate(int candidateID, int sentID, int wordID, String word, int tag) {
            this.sentID = sentID;
            this.wordID = wordID;
            this.word = word;
            this.tag = tag;
            this.candidateID = candidateID;
        }

        public NounCandidate(int candidateID, int sentID, int wordID, String word, int tag, String resolution) {
            this.sentID = sentID;
            this.wordID = wordID;
            this.word = word;
            this.tag = tag;
            this.resolution = resolution;
            this.candidateID = candidateID;
        }

        public int getCandidateID() {
            return candidateID;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public String getWord() {
            if (resolution != null) {
                return resolution;
            }

            return word;
        }

        public boolean equals(Object o) {
            return ((o instanceof NounCandidate) && ((this.getCandidateID() == ((NounCandidate) o).getCandidateID())
                    || (this.word.equalsIgnoreCase(((NounCandidate) o).word))));
        }

        public int hashCode() {
            return this.getCandidateID();
        }
    }

    List<AnaphoraSentence> tagged_sentences;
    final String textOriginal;
    private String textResolved;
    private List<NounCandidate> nounCandidates = new ArrayList<>();
//    Map<String, Integer> subjectHistogram;



    public PronounResolver(String textOriginal) {
        this.textOriginal = textOriginal;
        tagSentences();
        resolvePronouns();
        buildResolvedText();
    }

    public String getTextResolved() {
        return textResolved;
    }

    public List<String> getResolvedSentences() {
        List<String> sentences = new ArrayList<>();
        for (AnaphoraSentence sentence : tagged_sentences) {
            sentences.add(sentence.getPronounResolvedSentence());
        }
        return sentences;
    }

    public List<String> getOriginalSentences() {
        List<String> sentences = new ArrayList<>();
        for (AnaphoraSentence sentence : tagged_sentences) {
            sentences.add(sentence.originalSentence);
        }
        return sentences;
    }

//    public Map<String, Integer> getResolvedSubjectHistogram() {
//        if (subjectHistogram == null) {
//            subjectHistogram = new HashMap<>();
//
//            for (AnaphoraSentence sentence : tagged_sentences) {
//                for (String subject : sentence.getResolvedSubjects()) {
//                    subject = subject.toLowerCase();
//                    if (subjectHistogram.containsKey(subject)) {
//                        subjectHistogram.put(subject, subjectHistogram.get(subject) + 1);
//                    } else {
//                        subjectHistogram.put(subject, 1);
//                    }
//                }
//            }
//        }
//        return subjectHistogram;
//    }

    public Map<String, Integer> getResolvedNounHistogram() {
        Map<String, Integer> nounMap  = new HashMap<>();
        for (AnaphoraSentence sentence : tagged_sentences) {
            for(int i=0; i < sentence.wordArray.length; i++){
                String noun = null;
                if(AnaphoraSentence.isNoun(sentence.tagArray[i])){
                    noun = (sentence.resolutions.containsKey(i)) ? sentence.resolutions.get(i) : sentence.wordArray[i];
                }else if(HumanListModified.hasI(sentence.wordArray[i])){
                    noun = "I";
                }else if(HumanListModified.hasYou(sentence.wordArray[i])){
                    noun = "You";
                }

                if(noun != null) {
                    if (nounMap.containsKey(noun)) {
                        nounMap.put(noun, nounMap.get(noun) + 1);
                    } else {
                        nounMap.put(noun, 1);
                    }
                }
            }
        }

        return nounMap;
    }

    public JSONObject getGenderInfo() {
        JSONObject genderInfo = new JSONObject();
//        Map<String, Integer> subjectHistogram = getResolvedSubjectHistogram();
        Map<String, Integer> nounHistogram = getResolvedNounHistogram();

        String mostMentionedSubject = null;

        for (Map.Entry<String, Integer> entry : nounHistogram.entrySet()) {
            if (mostMentionedSubject == null) {
                mostMentionedSubject = entry.getKey();
            } else if (nounHistogram.get(mostMentionedSubject) < entry.getValue()) {
                mostMentionedSubject = entry.getKey();
            }
        }

        try {
            genderInfo.put("MainSubject",mostMentionedSubject);
            resolvedSubjectInformation(mostMentionedSubject, genderInfo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return genderInfo;
    }

        public void resolvedSubjectInformation(String noun, JSONObject genderInfo) throws JSONException {
            if(noun == null){
                genderInfo.put("NarrationType", "unknown");
                genderInfo.put("PatientGender", "unknown");
                genderInfo.put("NarratorGender", "unknown");
                return;
            }

        if (noun.equalsIgnoreCase("i")) {
            String gender = resolveGenderFirstPerson();

            genderInfo.put("NarrationType", "first-person");
            genderInfo.put("PatientGender", gender);
            genderInfo.put("NarratorGender", gender);
        } else if (HumanListModified.hasYou(noun)) {
            genderInfo.put("NarrationType", "advice");
        } else if (HumanListModified.isRelationMentionNoun(noun)) {
            genderInfo.put("NarrationType", "second-person");
            genderInfo.put("Relation", noun);

            String gender = HumanListModified.getDirectGender(noun);
            String narratorGender = HumanListModified.resolveIndirectGenderMentions("my " + noun);
            if (gender != null){
                genderInfo.put("PatientGender", gender);

                if(narratorGender != null){
                    genderInfo.put("NarratorGender", narratorGender);
                }
            } else {
                for (NounCandidate nounCandidate : nounCandidates) {
                    if (nounCandidate.getWord().equalsIgnoreCase(noun)) {
                        if (AnaphoraSentence.isMaleNoun(nounCandidate.tag)) {
                            genderInfo.put("PatientGender", "male");
                            if(HumanListModified.isImpliedOppositeGender(noun)){
                                genderInfo.put("NarratorGender", "female");
                            }
                            break;
                        } else if (AnaphoraSentence.isFemaleNoun(nounCandidate.tag)) {
                            genderInfo.put("PatientGender", "female");
                            if(HumanListModified.isImpliedOppositeGender(noun)){
                                genderInfo.put("NarratorGender", "male");
                            }
                            break;
                        }
                    }
                }

                if(!genderInfo.has("PatientGender")){
                    genderInfo.put("PatientGender", "unknown");
                }

            }

        }else if(HumanListModified.isHealthOccupationNoun(noun)){
            genderInfo.put("HealthProfessional", noun);
            genderInfo.put("NarrationType", "unknown");
            genderInfo.put("PatientGender", "unknown");
            genderInfo.put("NarratorGender", "unknown");
//            for (NounCandidate nounCandidate : nounCandidates) {
//                if (nounCandidate.getWord().equalsIgnoreCase(subject)) {
//                    if (AnaphoraSentence.isMaleNoun(nounCandidate.tag)) {
//                        genderInfo.put("health-professional", subject);
//                    } else if (AnaphoraSentence.isFemaleNoun(nounCandidate.tag)) {
//                        return "health-professional, female";
//                    }
//                }
//            }
        }else {
            genderInfo.put("NarrationType", "unknown");
            genderInfo.put("PatientGender", "unknown");
            genderInfo.put("NarratorGender", "unknown");
        }
    }


    private String resolveGenderFirstPerson() {
//        StringBuilder sb = new StringBuilder();

//        for(AnaphoraSentence sentence: tagged_sentences){
//            for(Integer wordID: sentence.subjectWordIds){
//                if(sentence.wordArray[wordID].equalsIgnoreCase("i")){
//                    sb.append(sentence.getPronounResolvedSentence());
//                }
//            }
//        }

        String text = removePunctuations(textOriginal);
        String gender = HumanListModified.resolveDirectGenderMentions(text);

        if(gender == null) {
            gender = HumanListModified.resolveIndirectGenderMentions(text);
        }

        if(gender == null) {
            String noun = HumanListModified.getOppositGenderNeutralMentions(text);
            if(noun != null) {
                for (NounCandidate nounCandidate : nounCandidates) {
                    if (nounCandidate.getWord().equalsIgnoreCase(noun)) {
                        if (AnaphoraSentence.isMaleNoun(nounCandidate.tag)) {
                            return "female";
                        } else if (AnaphoraSentence.isFemaleNoun(nounCandidate.tag)) {
                            return "male";
                        }
                    }
                }
            }
        }

        if(gender != null) {
            return gender;
        }else {
            return "unknown";
        }
    }

    private void buildResolvedText() {
        StringBuilder sb = new StringBuilder();
        for (AnaphoraSentence sentence : tagged_sentences) {
            sb.append(sentence.getPronounResolvedSentence());
        }

        textResolved = sb.toString();
    }


    private static String removePunctuations(String text) {
        return text.replaceAll("[:?\\.;<>(),!+-/]", " ");
    }

    private void resolvePronouns() {
        int nNewCandidates = 0;
        for (int sentID = 0; sentID < tagged_sentences.size(); sentID++) {
            AnaphoraSentence sentence = tagged_sentences.get(sentID);

            if (sentence.hasPronouns || sentence.hasNouns) {

                for (int wordID = 0; wordID < sentence.getNWords(); wordID++) {

                    if (AnaphoraSentence.isNoun(sentence.tagArray[wordID])) {
                        nounCandidates.add(new NounCandidate(nNewCandidates++,sentID, wordID, sentence.wordArray[wordID], sentence.tagArray[wordID]));
                    } else if (AnaphoraSentence.isPronoun(sentence.tagArray[wordID])) {
                        NounCandidate resolution = null;
                        String word = sentence.wordArray[wordID].trim();
                        int tag = sentence.tagArray[wordID];

                        List<NounCandidate> matchingNounCandidates = new ArrayList<>();
                        for (NounCandidate nounCandidate : nounCandidates) {
                            if (AnaphoraSentence.matchGender(tag, nounCandidate.tag)) {
                                matchingNounCandidates.remove(nounCandidate);
                                matchingNounCandidates.add(nounCandidate);
                            }
                        }

                        int nMatchingNounCandidates = matchingNounCandidates.size();

                        if (nMatchingNounCandidates > 0) {
                            //rule 1 only candidate so far
                            if (nMatchingNounCandidates == 1) {
                                resolution = matchingNounCandidates.get(nMatchingNounCandidates - 1);
                            }

                            //rule2 reflexive pronoun
                            else if (word.endsWith("self") && matchingNounCandidates.get(nMatchingNounCandidates - 1).sentID == sentID) {
                                resolution = matchingNounCandidates.get(nMatchingNounCandidates - 1);
                            }

                            //rule3 unique in current + prior
                            else if ((matchingNounCandidates.get(nMatchingNounCandidates - 1).sentID >= (sentID - 1)) &&
                                    (matchingNounCandidates.get(nMatchingNounCandidates - 2).sentID < (sentID - 1))) {
                                resolution = matchingNounCandidates.get(nMatchingNounCandidates - 1);


                            }

                            //rule4 possessive pronoun
                            else if (HumanListModified.isPossessivePronoun(word)) {
                                for (int i = nMatchingNounCandidates - 1; i >= 0; i--) {
                                    if ((matchingNounCandidates.get(i).sentID == (sentID - 1)) && matchingNounCandidates.get(i).word.equalsIgnoreCase(word)) {
                                        resolution = matchingNounCandidates.get(i);
                                        break;

                                    }
                                }
                            }

                            //rule5 matching pronoun in same sentence
                            else if ((matchingNounCandidates.get(nMatchingNounCandidates - 1).sentID >= sentID) &&
                                    AnaphoraSentence.isPronoun(matchingNounCandidates.get(nMatchingNounCandidates - 1).tag)) {
                                resolution = matchingNounCandidates.get(nMatchingNounCandidates - 1);
                            }

                            //rule6
                            else if (wordID < 3) //probably the subject
                            {
                                for (int i = nMatchingNounCandidates - 1; i >= 0; i--) {
                                    if ((matchingNounCandidates.get(i).sentID == (sentID - 1)) && matchingNounCandidates.get(i).wordID < 3) {
                                        resolution = matchingNounCandidates.get(i);

                                    }
                                }
                            }
                        }

//                        if (sentence.hasNouns || sentence.hasResolvedPronouns) {
//                            resolution = resolveFromLeftSideOfThisSentence(sentID, wordID);
//                        }
//
//                        if (resolution == null) {
//                            resolution = resolveFromPreviousSentences(sentID, sentence.tagArray[wordID]);
//                        }
//
//                        if (resolution == null && (sentence.hasNouns || sentence.hasResolvedPronouns)) {
//                            resolution = resolveFromRightSideOfThisSentence(sentID, wordID);
//                        }

                        if (resolution != null) {
                            sentence.hasResolvedPronouns = true;
                            sentence.resolutions.put(wordID, resolution.getWord());

                            nounCandidates.add(new NounCandidate(resolution.getCandidateID(),sentID, wordID, word, tag, resolution.getWord()));

                            if (AnaphoraSentence.isMalePronoun(tag)) {
                                sentence.tagArray[wordID] = AnaphoraSentence.MALE_RESOLVED_PRONOUN;

                                if (AnaphoraSentence.isNeutralNoun(resolution.tag)) {
                                    resolution.tag = AnaphoraSentence.MALE_RESOLVED_NOUN;
                                }
                            } else if (AnaphoraSentence.isFemalePronoun(tag)) {
                                sentence.tagArray[wordID] = AnaphoraSentence.FEMALE_RESOLVED_PRONOUN;

                                if (AnaphoraSentence.isNeutralNoun(resolution.tag)) {
                                    resolution.tag = AnaphoraSentence.FEMALE_RESOLVED_NOUN;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String resolveFromPreviousSentences(int sentID, int tag) {
        String resolution = null;
        for (int id = sentID - 1; id >= 0; id--) {
            AnaphoraSentence sentence = tagged_sentences.get(id);
            if (sentence.hasNouns || sentence.hasResolvedPronouns) {
                resolution = resolveFromAnotherSentence(id, tag);
                if (resolution != null) {
                    break;
                }
            }
        }
        return resolution;
    }

    private String resolveFromAnotherSentence(int sentID, int tag) {
        AnaphoraSentence sentence = tagged_sentences.get(sentID);
        String resolution = null;
        for (int id = sentence.getNWords() - 1; id >= 0; id--) {
            if (AnaphoraSentence.isNoun(sentence.tagArray[id])) {
                if (AnaphoraSentence.isMalePronoun(tag) && (AnaphoraSentence.isMaleNoun(sentence.tagArray[id]) || AnaphoraSentence.isNeutralNoun(sentence.tagArray[id]))) {
                    resolution = sentence.getResolution(id);
                } else if (AnaphoraSentence.isFemalePronoun(tag) && (AnaphoraSentence.isFemaleNoun(sentence.tagArray[id]) || AnaphoraSentence.isNeutralNoun(sentence.tagArray[id]))) {
                    resolution = sentence.getResolution(id);
                }
            }
        }

        return resolution;
    }

    private String resolveFromLeftSideOfThisSentence(int sentID, int wordID) {
        AnaphoraSentence sentence = tagged_sentences.get(sentID);
        String resolution = null;
        int tag = sentence.tagArray[wordID];
        for (int id = wordID - 1; id >= 0; id--) {
            if (AnaphoraSentence.isNoun(sentence.tagArray[id])) {
                if (AnaphoraSentence.isMalePronoun(tag) && (AnaphoraSentence.isMaleNoun(sentence.tagArray[id]) || AnaphoraSentence.isNeutralNoun(sentence.tagArray[id]))) {
                    resolution = sentence.getResolution(id);
                } else if (AnaphoraSentence.isFemalePronoun(tag) && (AnaphoraSentence.isFemaleNoun(sentence.tagArray[id]) || AnaphoraSentence.isNeutralNoun(sentence.tagArray[id]))) {
                    resolution = sentence.getResolution(id);
                }
            }
        }

        return resolution;
    }

    private String resolveFromRightSideOfThisSentence(int sentID, int wordID) {
        AnaphoraSentence sentence = tagged_sentences.get(sentID);
        String resolution = null;
        int tag = sentence.tagArray[wordID];
        for (int id = wordID + 1; id < sentence.getNWords(); id++) {
            if (AnaphoraSentence.isNoun(sentence.tagArray[id])) {
                if (AnaphoraSentence.isMalePronoun(tag) && (AnaphoraSentence.isMaleNoun(sentence.tagArray[id]) || AnaphoraSentence.isNeutralNoun(sentence.tagArray[id]))) {
                    resolution = sentence.getResolution(id);
                } else if (AnaphoraSentence.isFemalePronoun(tag) && (AnaphoraSentence.isFemaleNoun(sentence.tagArray[id]) || AnaphoraSentence.isNeutralNoun(sentence.tagArray[id]))) {
                    resolution = sentence.getResolution(id);
                }
            }
        }

        return resolution;
    }

    private void tagSentences() {
        String[] sentences = SentenceSplitter.getSentenceSplitter().sentDetect(textOriginal);
        tagged_sentences = new ArrayList<>(sentences.length);

        for (String sentence : sentences) {
            tagged_sentences.add(new AnaphoraSentence(sentence));
        }
    }

}


