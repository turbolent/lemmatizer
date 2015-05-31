package com.turbolent.lemmatizer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommandLineInterface {
    private static void printUsage() {
        String usage =
            "Usage:\n"
            + "  generate <wordnet-directory> <model-file>\n"
            + "  morphy <model-file> <word> <penn-tag>\n"
            + "\n"
            + "Commands:\n"
            + "  generate  Creates a model from the given directory of WordNet database files.\n"
            + "  morphy    Find lemmas for the given word and Penn Treebank part of speech tag\n"
            + "            using the given model. Writes one lemma per line to standard output.\n";
        System.err.println(usage);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            printUsage();
            return;
        }

        String command = args[0];

        switch (command) {
            case "generate": {
                if (args.length < 3) {
                    printUsage();
                    return;
                }

                Path wordNetPath = Paths.get(args[1]);
                Path targetPath = Paths.get(args[2]);

                Lemmatizer.loadFromWordNet(wordNetPath)
                          .saveTo(targetPath);

                break;
            }
            case "morphy": {
                if (args.length < 4) {
                    printUsage();
                    return;
                }

                Path path = Paths.get(args[1]);
                String word = args[2];
                String pennTag = args[3];

                Lemmatizer lemmatizer = Lemmatizer.loadFrom(path);
                PartOfSpeech pos = PartOfSpeech.fromPennTreebankTag(pennTag);
                String[] lemmas = lemmatizer.morphy(word, pos);

                for (String lemma : lemmas)
                    System.out.println(lemma);

                break;
            }
            default:
                printUsage();
        }
    }
}
