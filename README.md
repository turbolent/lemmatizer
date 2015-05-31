lemmatizer
==========

[![Build Status](https://travis-ci.org/turbolent/lemmatizer.svg?branch=master)](https://travis-ci.org/turbolent/lemmatizer)

A lemmatizer based on [WordNet's morphological processing](https://wordnet.princeton.edu/man/morphy.7WN.html).

## Usage

### API

```java
Path modelPath = Paths.get("<path-to-model>");
Lemmatizer lemmatizer = Lemmatizer.loadFrom(modelPath);
PartOfSpeech pos = PartOfSpeech.fromPennTreebankTag(pennTag);
String[] lemmas = lemmatizer.morphy(word, pos);
```

### Command Line

    $ mvn compile assembly:single
    $ java -jar target/lemmatizer.jar
    Usage:
      generate <wordnet-directory> <model-file>
      morphy <model-file> <word> <penn-tag>

    Commands:
      generate  Creates a model from the given directory of WordNet database files.
      morphy    Find lemmas for the given word and Penn Treebank part of speech tag
                using the given model. Writes one lemma per line to standard output.

    $ wget http://wordnetcode.princeton.edu/wn3.1.dict.tar.gz
    $ tar xzvf wn3.1.dict.tar.gz
    $ java -jar target/lemmatizer.jar generate dict model
    $ java -jar target/lemmatizer.jar morphy model were VBD
    be
