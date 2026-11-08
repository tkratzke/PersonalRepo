package com.skagit.flashCardsGame.enums;

public enum MatchType {
    MATCHED_INCLUDING_DIACRITICS, //
    MATCHED_EXCEPT_DIACRITICS_ARE_WRONG, //
    NOT_MATCHED_EVEN_WHEN_IGNORING_DIACRITICS;

    final public static MatchType[] _Values = values();
}
