package com.turbolent.lemmatizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;


/**
 * From https://wordnet.princeton.edu/wordnet/man/wndb.5WN.html:
 *
 * index.noun, data.noun, index.verb, data.verb, index.adj, data.adj, index.adv, data.adv
 *   - WordNet database files
 * noun.exc, verb.exc. adj.exc adv.exc
 *   - morphology exception lists
 *
 * [...]
 *
 * Index File Format
 *
 * Each index file begins with several lines containing a copyright notice, version number
 * and license agreement. These lines all begin with two spaces and the line number [...].
 *
 * All other lines are in the following format. [...]
 *
 *  lemma  [...]
 *
 * lemma
 *   lower case ASCII text of word or collocation. Collocations are formed by joining individual
 *   words with an underscore (_ ) character.
 *
 *
 * Exception List File Format
 *
 * Exception lists are alphabetized lists of inflected forms of words and their base forms.
 * The first field of each line is an inflected form, followed by a space separated list of one
 * or more base forms of the word. There is one exception list file for each syntactic category.
 *
 */

public class WordNetParser {

    private static Map<PartOfSpeech, String> FILENAME_PARTS =
        new HashMap<PartOfSpeech, String>() {{
            put(PartOfSpeech.ADJECTIVE, "adj");
            put(PartOfSpeech.ADVERB, "adv");
            put(PartOfSpeech.NOUN, "noun");
            put(PartOfSpeech.VERB, "verb");
        }};

    public static Map<String, Set<PartOfSpeech>> loadLemmas(Path wordNetPath)
        throws IOException
    {
        Map<String, Set<PartOfSpeech>> allLemmas = new HashMap<>();

        for (PartOfSpeech pos : PartOfSpeech.values()) {
            String fileNamePart = FILENAME_PARTS.get(pos);
            String fileName = String.format("index.%s", fileNamePart);
            Path path = wordNetPath.resolve(fileName);
            Files.lines(path)
                 .filter(line -> !line.startsWith(" "))
                 .forEach(line -> {
                     String lemma = line.substring(0, line.indexOf(" "));
                     Set<PartOfSpeech> parts =
                         allLemmas.computeIfAbsent(lemma, missingLemma ->
                             new HashSet<>());
                     parts.add(pos);
                 });
        }

        return allLemmas;
    }

    private static Pattern SPACE = Pattern.compile(" ");

    public static Map<PartOfSpeech, Map<String, List<String>>> loadExceptions(Path basePath)
        throws IOException
    {
        Map<PartOfSpeech, Map<String, List<String>>> allExceptions = new HashMap<>();

        for (PartOfSpeech pos : PartOfSpeech.values()) {
            Map<String, List<String>> exceptions = new HashMap<>();
            allExceptions.put(pos, exceptions);

            String fileNamePart = FILENAME_PARTS.get(pos);
            String fileName = String.format("%s.exc", fileNamePart);
            Path path = basePath.resolve(fileName);
            Files.lines(path)
                 .forEach(line -> {
                     List<String> terms = Arrays.asList(SPACE.split(line));
                     exceptions.put(terms.get(0),
                                    terms.subList(1, terms.size()));
                 });
        }

        return allExceptions;
    }
}
