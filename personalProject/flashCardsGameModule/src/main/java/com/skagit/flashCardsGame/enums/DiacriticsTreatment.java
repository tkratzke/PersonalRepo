package com.skagit.flashCardsGame.enums;

public enum DiacriticsTreatment {
    STRICT("Diacritics Must be Correct."), //
    LENIENT("Inaccurate Diacritics are Pointed Out."), //
    RELAXED("Inaccurate Diacritics are Ignored.");

    final public static DiacriticsTreatment[] _Values = values();

    final String _explanation;

    DiacriticsTreatment(final String explanation) {
	_explanation = String.format("%s: %s", name(), explanation);
    }
}
