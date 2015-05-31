package com.turbolent.lemmatizer;

public enum PartOfSpeech {
    ADJECTIVE,
    ADVERB,
    NOUN,
    VERB;

    public static PartOfSpeech fromPennTreebankTag(String tag) {
        if (tag.startsWith("J"))
            return ADJECTIVE;
        if (tag.startsWith("V"))
            return VERB;
        if (tag.startsWith("R"))
            return ADVERB;
        if (tag.startsWith("N"))
            return NOUN;
        return null;
    }
}
