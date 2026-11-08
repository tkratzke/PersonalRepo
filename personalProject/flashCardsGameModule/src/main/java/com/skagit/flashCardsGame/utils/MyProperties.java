package com.skagit.flashCardsGame.utils;

import java.util.Properties;

import com.skagit.flashCardsGame.enums.Clumping;
import com.skagit.flashCardsGame.enums.DecayType;
import com.skagit.flashCardsGame.enums.DiacriticsTreatment;
import com.skagit.flashCardsGame.enums.Mode;
import com.skagit.flashCardsGame.enums.PropertyPlus;

public class MyProperties extends Properties {
	private static final long serialVersionUID = 1L;

	/**
	 * Validates a value, and returns the defaultValue for the PropertyPlus
	 * otherwise.
	 */
	public String getValidString(final PropertyPlus propertyPlus, final String overrideString) {
		if (propertyPlus == null) {
			return null;
		}
		final String propertiesString = (String) get(propertyPlus._propertyName);
		final String defaultString = propertyPlus._defaultValue;
		final String stringToTry;
		if (overrideString != null) {
			stringToTry = overrideString;
		} else if (propertiesString != null) {
			stringToTry = propertiesString;
		} else {
			stringToTry = defaultString;
		}
		if (stringToTry == null) {
			return null;
		}
		String stringToParseForAnInt = stringToTry;
		switch (propertyPlus) {
		case CARDS_FILE:
		case SOUND_FILES_DIR:
			return stringToTry;
		case ALLOWABLE_MISS_PERCENTAGE:
		case PERCENTAGE_FOR_RECENT_WORDS:
			final int len = stringToTry == null ? 0 : stringToTry.length();
			if (len < 2 || stringToTry.charAt(len - 1) != '%') {
				return defaultString;
			}
			stringToParseForAnInt = stringToTry.substring(0, len - 1);
		case NUMBER_OF_NEW_WORDS:
		case NUMBER_OF_RECENT_WORDS:
		case NUMBER_OF_TIMES_FOR_NEW_WORDS:
		case TOP_CARD_INDEX:
		case RANDOM_SEED:
		case BLOCK_SIZE:
			try {
				final int i = Integer.parseInt(stringToParseForAnInt);
				switch (propertyPlus) {
				case ALLOWABLE_MISS_PERCENTAGE:
				case PERCENTAGE_FOR_RECENT_WORDS:
					return 0 <= i && i <= 100 ? stringToTry : defaultString;
				case NUMBER_OF_NEW_WORDS:
					return i >= 1 ? stringToTry : defaultString;
				case NUMBER_OF_RECENT_WORDS:
				case NUMBER_OF_TIMES_FOR_NEW_WORDS:
				case TOP_CARD_INDEX:
					return i >= 0 ? stringToTry : defaultString;
				case BLOCK_SIZE:
					return i >= 0 ? stringToTry : defaultString;
				case RANDOM_SEED:
					return stringToTry;
				default:
					/** Cannot get to the following: */
					return null;
				}
			} catch (final NumberFormatException e) {
				return defaultString;
			}
		case DECAY_TYPE:
		case DIACRITICS_TREATMENT:
		case CLUMPING:
		case MODE:
			try {
				if (propertyPlus == PropertyPlus.DECAY_TYPE) {
					DecayType.valueOf(stringToTry);
				} else if (propertyPlus == PropertyPlus.DIACRITICS_TREATMENT) {
					DiacriticsTreatment.valueOf(stringToTry);
				} else if (propertyPlus == PropertyPlus.CLUMPING) {
					Clumping.valueOf(stringToTry);
				} else if (propertyPlus == PropertyPlus.MODE) {
					Mode.valueOf(stringToTry);
				}
			} catch (final IllegalArgumentException e) {
				return defaultString;
			}
			return stringToTry;
		case BE_SILENT:
			return String.valueOf(Boolean.valueOf(stringToTry));
		}
		/** To keep the compiler happy: */
		return null;
	}

}
