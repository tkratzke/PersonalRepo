package com.skagit.flashCardsGame.enums;

import java.util.EnumSet;

public enum PropertyPlus {
	CARDS_FILE(/* propertyName= */"Cards.File", //
			/* shortName= */null, //
			/* defaultValue= */null, //
			/* comment= */"""
					Strings that indicate where to find the Cards File,\

					the directory of sound files, \

					and whether or not to play the sounds."""), //
	SOUND_FILES_DIR(/* propetyName= */"Sound.Files.Dir", //
			/* shortName= */null, //
			/* defaultValue= */"", //
			/* comment= */""), //
	BE_SILENT(/* propertyName= */"Be.Silent", //
			/* shortName= */null, //
			/* defaultValue= */Boolean.FALSE.toString(), //
			/* comment= */""), //

	TOP_CARD_INDEX(/* propertyName= */"Top.Card.Index", //
			/* shortName= */"TCI", //
			/* defaultValue= */"0", //
			/* comment= */"Highest card index in first quiz," + //
					"\nplus 5 lines that specify normal quiz generation."), //

	NUMBER_OF_NEW_WORDS(/* propertyName= */"Number.Of.New.Words", //
			/* shortName= */null, //
			/* defaultValue= */"1", //
			/* comment= */""), //
	NUMBER_OF_RECENT_WORDS(/* propertyName= */"Number.Of.Recent.Words", //
			/* shortName= */null, //
			/* defaultValue= */"3", //
			/* comment= */""), //
	NUMBER_OF_TIMES_FOR_NEW_WORDS(/* propertyName= */"Number.Of.Times.For.New.Words", //
			/* shortName= */null, //
			/* defaultValue= */"1", //
			/* comment= */""), //
	PERCENTAGE_FOR_RECENT_WORDS(/* propertyName= */"Percentage.For.Recent.Words", //
			/* shortName= */null, //
			/* defaultValue= */"75%", //
			/* comment= */""), //
	DECAY_TYPE(/* propertyName= */"Decay.Type", //
			/* shortName= */null, //
			/* defaultValue= */"LINEAR", //
			/* comment= */""), //

	DIACRITICS_TREATMENT(/* propertyName= */"Diacritics.Treatment", //
			/* shortName= */null, //
			/* defaultValue= */DiacriticsTreatment.STRICT.name(), //
			/* comment= */DiacriticsTreatment.STRICT._explanation + //
					"\n" + DiacriticsTreatment.LENIENT._explanation + //
					"\n" + DiacriticsTreatment.RELAXED._explanation//
	),

	MODE(/* propertyName= */"Mode", //
			/* shortName= */null, //
			/* defaultValue= */Mode.NORMAL.name(), //
			/* comment= */Mode.NORMAL._explanation + //
					"\n" + Mode.STEP._explanation + //
					"\n" + Mode.SWITCH_AND_DUMP._explanation + //
					"\n" + Mode.REGULAR_SORT_AND_DUMP._explanation + //
					"\n" + Mode.STRANGE_SORT_AND_DUMP._explanation + //
					"\n" + Mode.SUPPRESS_ANSWERS_AND_DUMP._explanation//
	),

	BLOCK_SIZE(/* propertyName= */"Block.Size", //
			/* shortName= */null, //
			/* defaultValue= */"10", //
			/* comment= */"Size of block when dumping Cards File"),

	RANDOM_SEED(/* propertyName= */"Random.Size", //
			/* shortName= */null, //
			/* defaultValue= */"0", //
			/* comment= */"""
					0: Cards Retain their Order.
					Positive #: Use that for the Random Seed.
					Any Negative #: Use SuperRandom."""//
	),

	ALLOWABLE_MISS_PERCENTAGE(/* propertyName= */"Allowable.Miss.Percentage", //
			/* shortName= */null, //
			/* defaultValue= */"10%", //
			/* comment= */"Determines if each quiz is \"passed\" or not."//
	),

	CLUMPING(/* propertyName= */"Clumping", //
			/* shortName= */null, //
			/* defaultValue= */Clumping.NO_CLUMPING.name(), //
			/* comment= */Clumping.BY_CLUE._explanation + //
					"\n" + Clumping.BY_ANSWER._explanation + //
					"\n" + Clumping.NO_CLUMPING._explanation//
	),;

	final public static PropertyPlus[] _Values = values();

	final public static EnumSet<PropertyPlus> _ChangeableCoreProperties = EnumSet.noneOf(PropertyPlus.class);

	final public static EnumSet<PropertyPlus> _ChangeableQuizGeneratorProperties = EnumSet.of(//
			TOP_CARD_INDEX //
	);

	final public String _propertyName;
	final public String _shortName;
	final public String _defaultValue;
	final public String _comment;

	PropertyPlus(final String propertyName, final String shortName, final String defaultValue, final String comment) {
		_propertyName = propertyName;
		_shortName = shortName;
		_defaultValue = defaultValue;
		_comment = comment;
	}

}
