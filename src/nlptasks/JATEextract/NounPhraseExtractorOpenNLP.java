package nlptasks.JATEextract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nounphrase extractor implemented with OpenNLP tools. It applies certain heuristics to clean a candidate noun phrase and
 * return it to the normalised root. These heuristics include:
 * <br>-Stopwords will be trimmed from the head and tails of a phrase. E.g, "the cat on the mat" becomes "cat on the mat".
 * <br>-phrases containing "or" "and" will be split, e.g., "Tom and Jerry" becomes "tom" "jerry"
 * <br>-must have letters
 * <br>-must have at least two characters
 * <br>-characters that do not match the pattern [a-zA-Z\-] are replaced with whitespaces.
 * <br>-may or may not have digits, this is set by the property file
 * <br>-must contain no more than N words, this is set by the property file
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class NounPhraseExtractorOpenNLP extends CandidateTermExtractor {


    private static final String TERM_CLEAN_PATTERN = "[^a-zA-Z0-9\\-]";
    public static final int TERM_MAX_WORDS = 4;

    private static NounPhraseExtractorOpenNLP nounPhraseExtractorOpenNLP;

    public static NounPhraseExtractorOpenNLP getNounPhraseExtractorOpenNLP() {

        if(nounPhraseExtractorOpenNLP == null)
        {
            nounPhraseExtractorOpenNLP = new NounPhraseExtractorOpenNLP();
        }
        return nounPhraseExtractorOpenNLP;
    }

    /**
     * Creates an instance with specified stopwords list and norm
     */
    private NounPhraseExtractorOpenNLP() {
        _stoplist = new StopList(true);
        _normaliser = new Lemmatizer();
    }

    public Map<String, Integer> extract(List<String> c) {
        Map<String, Integer> res = new HashMap<>();
        for (String d : c) {
//            _logger.info("Extracting candidate NP... From Document " + d);
            for (Map.Entry<String, Integer> e : extract_doc(d).entrySet()) {
                Integer count = res.get(e.getKey());
                count = count == null ? e.getValue() : (count + e.getValue());
                res.put(e.getKey(), count);
            }
        }
        return res;
    }

    public List<String> getPhrases(String sentence){
        List<String> phrases = new ArrayList<>();
        try {
            String[] tokens = NLPToolsControllerOpenNLP.getInstance().getTokeniser().tokenize(sentence);
            String[] pos = NLPToolsControllerOpenNLP.getInstance().getPosTagger().tag(tokens);
            String[] candidates = chunkNPs(tokens, pos);

            for(String c: candidates){
                c = applyCharacterReplacement(c, TERM_CLEAN_PATTERN);
                String[] e = applySplitList(c);

                for (String str : e) {
                    String stopremoved = applyTrimStopwords(str, _stoplist, _normaliser);
                    if (stopremoved == null) continue;
                    String original = stopremoved;
                    str = _normaliser.normalize(stopremoved.toLowerCase()).trim();

                    String[] nelements = str.split("\\s+");
                    if (nelements.length < 1 || nelements.length > 3)
                        continue;
//                    if (JATEProperties.getInstance().isIgnoringDigits() && containsDigit(str))continue;
//                    if (!containsLetter(str)) continue;
//                    if (!hasReasonableNumChars(str)) continue;

                    if (c.toLowerCase().indexOf(str) != -1) {
                        phrases.add(str);
                    }
                }
            }
        } catch (IOException wte) {
            wte.printStackTrace();
        }
        return phrases;
    }

    public List<String> getPhrases(List<String> sentences) {
        List<String> phrases = new ArrayList<>();
        if(sentences != null) {
            for (String sentence : sentences) {
                phrases.addAll(getPhrases(sentence));
            }
        }
        return phrases;
    }

    protected Map<String, Integer> extract_doc(String d){
        Map<String, Integer> res = new HashMap<>();
        try {
            for (String s : NLPToolsControllerOpenNLP.getInstance().getSentenceSplitter().sentDetect(d)) {
                for (Map.Entry<String, Integer> e : extract_sentence(s).entrySet()) {
                    Integer count = res.get(e.getKey());
                    count = count == null ? e.getValue() : (count + e.getValue());
                    res.put(e.getKey(), count);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }


    protected Map<String, Integer> extract_sentence(String content) {
        Map<String, Integer> nouns = new HashMap<>();
        try {
            String[] tokens = NLPToolsControllerOpenNLP.getInstance().getTokeniser().tokenize(content);
            String[] pos = NLPToolsControllerOpenNLP.getInstance().getPosTagger().tag(tokens);
            String[] candidates = chunkNPs(tokens, pos);
            for (String c : candidates) {
                c = applyCharacterReplacement(c, TERM_CLEAN_PATTERN);
                String[] e = applySplitList(c);

                for (String str : e) {
                    String stopremoved = applyTrimStopwords(str, _stoplist, _normaliser);
                    if (stopremoved == null) continue;
                    String original = stopremoved;
                    str = _normaliser.normalize(stopremoved.toLowerCase()).trim();

                    String[] nelements = str.split("\\s+");
                    if (nelements.length < 1 || nelements.length > TERM_MAX_WORDS)
                        continue;
//                    if (JATEProperties.getInstance().isIgnoringDigits() &&
//                            containsDigit(str))
//                        continue;
                    if (!containsLetter(str)) continue;
                    if (!hasReasonableNumChars(str)) continue;

                    if (c.toLowerCase().indexOf(str) != -1) {
                        Integer count = nouns.get(str);
                        count = count == null ? 1 : (count + 1);
                        nouns.put(str, count);
                    }
                }
            }
        } catch (IOException wte) {
            wte.printStackTrace();
        }
        return nouns;
    }

    private String[] chunkNPs(String[] tokens, String[] pos) throws IOException {
        String[] phrases = NLPToolsControllerOpenNLP.getInstance().getPhraseChunker().chunk(tokens, pos);
        List<String> candidates = new ArrayList<String>();
        String phrase = "";
        for (int n = 0; n < tokens.length; n++) {
            if (phrases[n].equals("B-NP")) {
                phrase = tokens[n];
                for (int m = n + 1; m < tokens.length; m++) {
                    if (phrases[m].equals("I-NP")) {
                        phrase = phrase + " " + tokens[m];
                    } else {
                        n = m;
                        break;
                    }
                }
                phrase = phrase.replaceAll("\\s+", " ").trim();
                if (phrase.length() > 0)
                    candidates.add(phrase);

            }
        }
        return candidates.toArray(new String[0]);
    }


}