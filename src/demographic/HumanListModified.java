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
package demographic;

import nlptasks.SentenceTokenizer;
import org.apache.commons.lang.StringUtils;
import util.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HumanListModified {
    private final static String[] malePronouns = new String("he him himself his").split(" ");
    private final static String[] femalePronouns = new String("she her herself").split(" ");
    private final static String[] reflexivePronouns = new String("himself herself").split(" ");
    private final static String[] possessivePronouns = new String("him her").split(" ");
    private final static String[] femaleGenderNouns = new String("fiancee girl mother mom mum grl sister daughter wife aunt niece granddaughter girlfriend grandmother daughter-in-law").split(" ");
    private final static String[] maleGenderNouns = new String("fiance father dad brother son boy husband uncle nephew grandson hubby boyfriend grandpa grandfather son-in-law").split(" ");
    private final static String[] unknownGenderNouns = new String("partner friend baby spouse colleague").split(" ");
    private final static Hashtable healthOccupationTb = getNameTb("./models/genderClassifier/medical_professions.txt");

    private final static List<Pattern> directGenderCluesFemalePatterns2 = getNounPatterns(
            new String("female").split(","));
    private final static List<Pattern> directGenderCluesMalePatterns2 = getNounPatterns(
            new String("male").split(","));
    private final static List<Pattern> directGenderCluesFemalePatterns1 = getFirstPersonPatterns(
            new String("female, woman, mother, mom, mum, girl, grl, grandma, grandmother, lady").split(","));
    private final static List<Pattern> directGenderCluesMalePatterns1 = getFirstPersonPatterns(
            new String("male, man, guy, father, dad, boy, grandpa, grandfather, widower").split(","));
    private final static List<Pattern> indirectGenderCluesFemalePatterns = getIndirectGenderCluePattern(
            new String("my husband, my fiance, my boyfriend, my boyfriend, my hubby, my bf").split(","));
    private final static List<Pattern> indirectGenderCluesMalePatterns = getIndirectGenderCluePattern(
            new String("my wife, my fiancee, my girlfriend, my gf").split(","));
    private final static List<Pattern> indirectGenderCluesNeutralPatterns = getIndirectGenderCluePattern(
            new String("my spouse, my partner").split(","));
    private final static List<Pattern> impliedOppositeGender = getIndirectGenderCluePattern(
            new String("my wife, my spouse, my partner").split(","));
    private final static List<Pattern> secondPersonPronouns = getNounPatterns(
            new String("you, your, yours, u, ur, yourself").split(","));

    private final static List<Pattern> a = getNounPatterns(
            new String("a").split(","));

    private final static List<Pattern> have = getNounPatterns(
            new String("have, had, has").split(","));

    private final static List<Pattern> firstPersonPronouns = getNounPatterns(
            new String("i , im").split(","));




    private static List<Pattern> getIndirectGenderCluePattern(String[] terms) {
        List<Pattern> patterns = new ArrayList<>();
        for (String term : terms) {
            term = term.trim();
            patterns.add(Pattern.compile("\\b" + StringUtils.join(term.split("\\s+"), "\\s+([^\\s]+\\s+){0,2}") + "\\b", Pattern.CASE_INSENSITIVE));
        }
        return patterns;
    }

    private static List<Pattern> getNounPatterns(String[] terms) {
        List<Pattern> patterns = new ArrayList<>();
        for (String term : terms) {
            term = term.trim();
            patterns.add(Pattern.compile("\\b" + StringUtils.join(term.split("\\s+"), "[\\s\\xA0]+") + "\\b", Pattern.CASE_INSENSITIVE));
        }
        return patterns;
    }


    private static List<Pattern> getFirstPersonPatterns(String[] terms) {
        List<Pattern> patterns = new ArrayList<>();
        for (String term : terms) {
            term = term.trim();
            patterns.add(Pattern.compile("\\b(i|i'm|im|am)\\s+([^\\s]+\\s+){0,5}\\b" + StringUtils.join(term.split("\\s+"), "[\\s\\xA0]+") + "\\b", Pattern.CASE_INSENSITIVE));
        }
        return patterns;
    }

    public static boolean isImpliedOppositeGender(String text){
        for (Pattern pattern : impliedOppositeGender) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    public static String resolveDirectGenderMentions(String text) {
        for (Pattern pattern : directGenderCluesFemalePatterns1) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String phrase = matcher.group(0);
                if (hasA(phrase) && !(hasYou(phrase) || hasHave(phrase))) {
                    return "female";
                }
            }
        }

        for (Pattern pattern : directGenderCluesMalePatterns1) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String phrase = matcher.group(0);
                if (hasA(phrase) && !(hasYou(phrase) || hasHave(phrase))) {
                    return "male";
                }
            }

        }

        for (Pattern pattern : directGenderCluesFemalePatterns2) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String phrase = matcher.group(0);
                if (!hasYou(phrase)) {
                    return "female";
                }
            }
        }

        for (Pattern pattern : directGenderCluesMalePatterns2) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String phrase = matcher.group(0);
                if (!hasYou(phrase)) {
                    return "male";
                }
            }

        }

        return null;
    }

    public static String resolveIndirectGenderMentions(String text) {
        for (Pattern pattern : indirectGenderCluesFemalePatterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return "female";
            }
        }

        for (Pattern pattern : indirectGenderCluesMalePatterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return "male";

            }

        }

        return null;
    }

    public static boolean hasYou(String text) {
        for (Pattern pattern : secondPersonPronouns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasA(String text) {
        for (Pattern pattern : a) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasHave(String text) {
        for (Pattern pattern :  have) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasI(String text) {
        for (Pattern pattern : firstPersonPronouns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }


    public static boolean isMale(String wd) {
        //People's name should start with a capital letter
        return contains(maleGenderNouns, wd) || contains(malePronouns, wd);
    }

    public static boolean isFemale(String wd) {
        //People's name should start with a capital letter
        return contains(femaleGenderNouns, wd) || contains(femalePronouns, wd);
    }

    public static boolean isRelevantNoun(String wd) {
        return contains(femaleGenderNouns, wd)
                || contains(maleGenderNouns, wd)
                || contains(unknownGenderNouns, wd)
                || contains((healthOccupationTb), wd);
    }

    public static boolean isHealthOccupationNoun(String wd) {
        if(wd.length() < 2) return false;
        wd = wd.toLowerCase();
        if(wd.charAt(wd.length()-1)=='s') wd = wd.substring(0,wd.length()-1);
        return contains((healthOccupationTb), wd);
    }

    public static boolean hasHealthOccupationMentioned(String text){
        for(String tok: SentenceTokenizer.getSentenceTokenizer().getTokens(text)){
            if(isHealthOccupationNoun(tok)) return true;
        }
        return false;
    }

    public static String getHealthOccupationMentioned(String text){
        for(String tok: SentenceTokenizer.getSentenceTokenizer().getTokens(text)){
            if(isHealthOccupationNoun(tok)) return tok;
        }
        return null;
    }

    public static boolean isRelationMentionNoun(String wd) {
        return contains(femaleGenderNouns, wd)
                || contains(maleGenderNouns, wd)
                || contains(unknownGenderNouns, wd);
    }

    public static boolean isPronoun(String wd) {
        //People's name should start with a capital letter
        return contains(malePronouns, wd) || contains(femalePronouns, wd);
    }

    public static boolean isPossessivePronoun(String wd) {
        //People's name should start with a capital letter
        return contains(possessivePronouns, wd);
    }


    //public static boolean isHumanTitle(String wd){
    //return contains(humanTitleTb,wd.toLowerCase());
    //}

    private static boolean contains(String[] list, String str) {
        return contains(list, str, false);
    }

    private static boolean contains(String[] list, String str, boolean caseSensitive) {
        boolean contain = false;

        if (caseSensitive) { //make this a outer check for efficiency's sake
            for (int i = 0; i < list.length; i++) {
                if (list[i].equals(str)) {
                    contain = true;
                    break;
                }
            }
        } else {
            for (int i = 0; i < list.length; i++) {
                if (list[i].equalsIgnoreCase(str)) {
                    contain = true;
                    break;
                }
            }
        }

        return contain;
    }

    private static boolean contains(Hashtable tb, String wd) {
        return tb.containsKey(wd);
    }

    private static String[] retrieveList(String listFile) {
        return read(listFile).toString().split("\\s+");
    }

    private static Hashtable getNameTb(String listFile) {
        return getNameTb(listFile, -1);
    }

    private static Hashtable getNameTb(String listFile, int range) {
        String[] nameArray = retrieveList(listFile);
        Hashtable tb = new Hashtable();

        if (nameArray.length <= 0) {
            System.err.println(listFile + " not found. Please download the latest data files. \n System quit.");
            System.exit(0);
        }

        if (nameArray != null) {
            int stopAt;
            if (range == -1) {
                stopAt = nameArray.length;
            } else {
                stopAt = Math.min(range, nameArray.length);
            }
            for (int i = 0; i < stopAt; i++) {
                String name = nameArray[i].substring(0, 1);
                if (nameArray[i].length() > 1) {
                    name += nameArray[i].substring(1).toLowerCase();
                }
                name = name.toLowerCase();
                tb.put(name, name);
            }
        }
        return tb;
    }

    private static StringBuffer read(String fileName) {
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader in =
                    new BufferedReader(new FileReader(fileName));
            String s;

            while ((s = in.readLine()) != null) {
                sb.append(s);
                sb.append(System.getProperty("line.separator"));
            }
            in.close();
        } catch (IOException ex) {
            System.err.println(fileName + " not found. Please check it." +
                    System.getProperty("line.separator") + "Skipping...");
        }
        return sb;
    }


    public static String getDirectGender(String subject) {
        if (HumanListModified.isMale(subject)) {
            return "male";
        }else if(isFemale(subject)){
            return "female";
        }

        return null;
    }

    public static String getOppositGenderNeutralMentions(String text) {
        for (Pattern pattern : indirectGenderCluesNeutralPatterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
               String match = matcher.group(0);
                match = match.replaceAll("my", " ").trim();
                return match;
            }
        }

        return null;

    }
}




