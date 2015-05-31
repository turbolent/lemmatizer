package com.turbolent.lemmatizer;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.template.SetTemplate;
import org.msgpack.template.Template;
import org.msgpack.unpacker.Unpacker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.turbolent.lemmatizer.PartOfSpeech.*;
import static org.msgpack.template.Templates.*;


// See https://wordnet.princeton.edu/man/morphy.7WN.html

public class Lemmatizer {
    private static final Template<Map<String, Set<PartOfSpeech>>>
        LEMMAS_TEMPLATE = tMap(TString,
                               new SetTemplate<>(tOrdinalEnum(PartOfSpeech.class)));
    private static final Template<Map<PartOfSpeech, Map<String, List<String>>>>
        EXCEPTIONS_TEMPLATE = tMap(tOrdinalEnum(PartOfSpeech.class),
                                   tMap(TString, tList(TString)));

    private final Map<String, Set<PartOfSpeech>> lemmas;
    private final Map<PartOfSpeech, Map<String, List<String>>> exceptions;

    public Lemmatizer(Map<String, Set<PartOfSpeech>> lemmas,
                      Map<PartOfSpeech, Map<String, List<String>>> exceptions)
    {
        this.lemmas = lemmas;
        this.exceptions = exceptions;
    }

    public static Lemmatizer loadFrom(Path inputPath) throws IOException {
        File inputFile = inputPath.toFile();

        try (FileInputStream fileStream = new FileInputStream(inputFile);
             Unpacker unpacker = new MessagePack().createUnpacker(fileStream))
        {
            Map<String, Set<PartOfSpeech>> lemmas =
                unpacker.read(LEMMAS_TEMPLATE);
            Map<PartOfSpeech, Map<String, List<String>>> exceptions =
                unpacker.read(EXCEPTIONS_TEMPLATE);

            return new Lemmatizer(lemmas, exceptions);
        }
    }

    public static Lemmatizer loadFromWordNet(Path wordNetPath) throws IOException {
        Map<String, Set<PartOfSpeech>> lemmas =
            WordNetParser.loadLemmas(wordNetPath);
        Map<PartOfSpeech, Map<String, List<String>>> exceptions =
            WordNetParser.loadExceptions(wordNetPath);
        return new Lemmatizer(lemmas, exceptions);
    }

    public void saveTo(Path outputPath) throws IOException {
        File outputFile = outputPath.toFile();

        MessagePack messagePack = new MessagePack();
        messagePack.register(PartOfSpeech.class);

        try (FileOutputStream fileStream = new FileOutputStream(outputFile);
             Packer packer = messagePack.createPacker(fileStream))
        {
            packer.write(this.lemmas);
            packer.write(this.exceptions);
        }
    }

    private static String stripSuffix(String form, String suffix) {
        return form.substring(0, form.length() - suffix.length());
    }

    private static Stream<String> collectSubstitutions
        (Map<String, List<String>> substitutions, String form)
    {
        return substitutions
            .entrySet().stream()
            .filter(entry -> {
                String oldSuffix = entry.getKey();
                return form.endsWith(oldSuffix);
            })
            .flatMap(entry -> {
                String oldSuffix = entry.getKey();
                List<String> newSuffixes = entry.getValue();

                return newSuffixes.stream()
                                  .map(newSuffix ->
                                           stripSuffix(form, oldSuffix) + newSuffix);
            });
    }

    private static List<String> collectSubstitutions
        (Map<String, List<String>> substitutions, List<String> forms)
    {
        return forms.stream()
                    .flatMap(form -> collectSubstitutions(substitutions, form))
                    .collect(Collectors.toList());
    }

    private boolean formHasPos(String form, PartOfSpeech pos) {
        Set<PartOfSpeech> parts = this.lemmas.get(form);
        return (parts != null
                && parts.contains(pos));
    }

    private Stream<String> filterForms(Stream<String> forms, PartOfSpeech pos) {
        return forms.filter(form -> formHasPos(form, pos))
                    .distinct();
    }

    private static final Map<PartOfSpeech, Map<String, List<String>>> SUBSTITUTIONS =
        new HashMap<PartOfSpeech, Map<String, List<String>>>() {

            private void addSubstitution(PartOfSpeech pos, String oldSuffix, String newSuffix) {
                Map<String, List<String>> substitutions =
                    computeIfAbsent(pos, missingPos ->
                        new HashMap<>());

                List<String> newSuffixes =
                    substitutions.computeIfAbsent(oldSuffix, missingSuffix ->
                        new ArrayList<>());

                newSuffixes.add(newSuffix);
            }

            {
                // nouns
                addSubstitution(NOUN, "s", "");
                addSubstitution(NOUN, "ses", "s");
                addSubstitution(NOUN, "ves", "f");
                addSubstitution(NOUN, "xes", "x");
                addSubstitution(NOUN, "zes", "z");
                addSubstitution(NOUN, "ches", "ch");
                addSubstitution(NOUN, "shes", "sh");
                addSubstitution(NOUN, "men", "man");
                addSubstitution(NOUN, "ies", "y");

                // verbs
                addSubstitution(VERB, "s", "");
                addSubstitution(VERB, "ies", "y");
                addSubstitution(VERB, "es", "e");
                addSubstitution(VERB, "es", "");
                addSubstitution(VERB, "ed", "e");
                addSubstitution(VERB, "ed", "");
                addSubstitution(VERB, "ing", "e");
                addSubstitution(VERB, "ing", "");

                // adjectives
                addSubstitution(ADJECTIVE, "er", "");
                addSubstitution(ADJECTIVE, "est", "");
                addSubstitution(ADJECTIVE, "er", "e");
                addSubstitution(ADJECTIVE, "est", "e");

                // adverbs
                put(ADVERB, new HashMap<>());
            }
        };

    private static final String[] EMPTY_RESULT = {};

    public String[] morphy(String form, PartOfSpeech pos) {
        Map<String, List<String>> exceptions = this.exceptions.get(pos);
        if (exceptions.containsKey(form)) {
            Stream<String> forms = Stream.concat(Stream.of(form),
                                                 exceptions.get(form).stream());
            return filterForms(forms, pos)
                .toArray(String[]::new);
        }

        Map<String, List<String>> substitutions = SUBSTITUTIONS.get(pos);
        List<String> forms = collectSubstitutions(substitutions, form)
            .collect(Collectors.toList());
        Stream<String> combinedForms = Stream.concat(Stream.of(form),
                                                     forms.stream());
        String[] results = filterForms(combinedForms, pos)
            .toArray(String[]::new);

        if (results.length > 0)
            return results;

        while (!forms.isEmpty()) {
            forms = collectSubstitutions(substitutions, forms);
            results = filterForms(forms.stream(), pos)
                .toArray(String[]::new);
            if (results.length > 0)
                return results;
        }

        return EMPTY_RESULT;
    }
}
