package com.skagit.flashCardsGame;

import java.util.ArrayList;
import java.util.Scanner;

import com.skagit.flashCardsGame.enums.DiacriticsTreatment;
import com.skagit.flashCardsGame.enums.MatchType;
import com.skagit.flashCardsGame.utils.FlashCardStatics;
import com.skagit.utils.StringUtilities;

public class ResponseEvaluator {
	final boolean _gotItRight;
	final String[] _diffStrings;

	ResponseEvaluator(final Scanner sc, final DiacriticsTreatment diacriticsTreatment, final String answerStringPart,
			final String responseStringPart) {

		final MatchType matchType = getMatchType(answerStringPart, responseStringPart);
		final boolean almostRight = matchType == MatchType.MATCHED_EXCEPT_DIACRITICS_ARE_WRONG;
		if (matchType == MatchType.MATCHED_INCLUDING_DIACRITICS
				|| (diacriticsTreatment == DiacriticsTreatment.RELAXED && almostRight)) {
			_gotItRight = true;
			_diffStrings = null;
			return;
		}

		/**
		 * We didn't get it exactly right. See if we got it "right enough" and then fill
		 * _diffStrings with the answer plus up to two differences.
		 */
		_gotItRight = almostRight && diacriticsTreatment == DiacriticsTreatment.LENIENT;

		final String[] answerFields = answerStringPart.split(StringUtilities._WhiteSpace);
		final String[] responseFields = responseStringPart.split(StringUtilities._WhiteSpace);
		final int nAnswerFields = answerFields.length;
		final int nResponseFields = responseFields.length;

		final int nSmaller = Math.min(nAnswerFields, nResponseFields);
		final ArrayList<String> diffStrings = new ArrayList<>();
		for (int k = 0; k < nSmaller; ++k) {
			final String answerField = answerFields[k];
			final String responseField = responseFields[k];
			final boolean thisIsADiff;
			final MatchType typeOfMatch1 = getMatchType(answerField, responseField);
			if (_gotItRight) {
				/** Got it right; ANY diff is interesting. */
				thisIsADiff = typeOfMatch1 != MatchType.MATCHED_INCLUDING_DIACRITICS;
			} else {
				switch (typeOfMatch1) {
				case MATCHED_INCLUDING_DIACRITICS:
					thisIsADiff = false;
					break;
				case MATCHED_EXCEPT_DIACRITICS_ARE_WRONG:
					/**
					 * Since we got it wrong, a LENIENT requirement will show something as
					 * NOT_MATCHED_EVEN_WHEN_IGNORING_DIACRITICS. Hence, we only count this as a
					 * diff if we are STRICT.
					 */
					thisIsADiff = diacriticsTreatment == DiacriticsTreatment.STRICT;
					break;
				case NOT_MATCHED_EVEN_WHEN_IGNORING_DIACRITICS:
				default:
					thisIsADiff = true;
					break;
				}
			}
			if (thisIsADiff) {
				diffStrings.add(String.format("(%s/%s)", answerField, responseField));
				if (diffStrings.size() == 2) {
					_diffStrings = diffStrings.toArray(new String[diffStrings.size()]);
					return;
				}
			}
		}
		for (int k = nAnswerFields; k < nResponseFields; ++k) {
			diffStrings.add(String.format("(null/%s)", responseFields[k]));
			if (diffStrings.size() == 2) {
				_diffStrings = diffStrings.toArray(new String[diffStrings.size()]);
				return;
			}
		}
		for (int k = nResponseFields; k < nAnswerFields; ++k) {
			diffStrings.add(String.format("(%s/null)", answerFields[k]));
			if (diffStrings.size() == 2) {
				_diffStrings = diffStrings.toArray(new String[diffStrings.size()]);
				return;
			}
		}
		_diffStrings = diffStrings.toArray(new String[diffStrings.size()]);
	}

	private static MatchType getMatchType(final String s0, final String s1) {
		final String s0a = StringUtilities.CleanWhiteSpace(FlashCardStatics.KillPunct(s0));
		final String s1a = StringUtilities.CleanWhiteSpace(FlashCardStatics.KillPunct(s1));
		if (s0a.equalsIgnoreCase(s1a)) {
			return MatchType.MATCHED_INCLUDING_DIACRITICS;
		}
		final String s0b = FlashCardStatics.StripVNDiacritics(s0a);
		final String s1b = FlashCardStatics.StripVNDiacritics(s1a);
		if (s0b.equalsIgnoreCase(s1b)) {
			return MatchType.MATCHED_EXCEPT_DIACRITICS_ARE_WRONG;
		}
		return MatchType.NOT_MATCHED_EVEN_WHEN_IGNORING_DIACRITICS;
	}
}