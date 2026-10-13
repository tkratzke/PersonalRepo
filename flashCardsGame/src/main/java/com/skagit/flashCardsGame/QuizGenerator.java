package com.skagit.flashCardsGame;

import java.util.Arrays;
import java.util.Random;

import com.skagit.flashCardsGame.enums.ChangeType;
import com.skagit.flashCardsGame.enums.DecayType;
import com.skagit.flashCardsGame.enums.Mode;
import com.skagit.flashCardsGame.enums.PropertyPlus;
import com.skagit.flashCardsGame.utils.FlashCardStatics;
import com.skagit.flashCardsGame.utils.MyProperties;

public class QuizGenerator {
	int _topCardIndex;
	final private int _maxNNewWords;
	final private int _maxNRecentWords;

	final private int _nRepeatsOfNew;
	final private int _percentageForRecentsI;
	final private DecayType _decayType;

	final private int _allowablePerCent;

	final private Mode _mode;

	final private Random _r;

	private boolean _changedQuizGeneratorParameters;

	QuizGenerator(final Mode mode, final MyProperties myProperties, final int nCards, final long seed) {
		_mode = mode;
		_topCardIndex = Integer
				.parseInt(myProperties.getValidString(PropertyPlus.TOP_CARD_INDEX, /* overrideString= */null));
		_maxNNewWords = Integer
				.parseInt(myProperties.getValidString(PropertyPlus.NUMBER_OF_NEW_WORDS, /* overrideString= */null));
		_nRepeatsOfNew = Integer.parseInt(
				myProperties.getValidString(PropertyPlus.NUMBER_OF_TIMES_FOR_NEW_WORDS, /* overrideString= */null));
		_maxNRecentWords = Integer
				.parseInt(myProperties.getValidString(PropertyPlus.NUMBER_OF_RECENT_WORDS, /* overrideString= */null));
		if (seed < 0) {
			_topCardIndex = Math.min(_topCardIndex, _maxNNewWords + _maxNRecentWords - 1);
		}
		_decayType = DecayType.valueOf(myProperties.getValidString(PropertyPlus.DECAY_TYPE, /* overrideString= */null));
		String s = myProperties.getValidString(PropertyPlus.ALLOWABLE_MISS_PERCENTAGE, /* overrideString= */null);
		_allowablePerCent = Integer.parseInt(s.substring(0, s.length() - 1));
		s = myProperties.getValidString(PropertyPlus.PERCENTAGE_FOR_RECENT_WORDS, /* overrideString= */null);
		_percentageForRecentsI = Integer.parseInt(s.substring(0, s.length() - 1));
		_r = new Random();
		if (seed >= 0) {
			_r.setSeed(Math.max(5, seed));
		}
		while (_r.nextLong() < Long.MAX_VALUE / 2) {
		}
		_changedQuizGeneratorParameters = false;
		correctQuizGeneratorProperties(nCards);
	}

	void updateChangeableProperties(final MyProperties myProperties) {
		myProperties.put(PropertyPlus.TOP_CARD_INDEX._propertyName, Integer.toString(_topCardIndex));
	}

	long[] getChangeableLongPropertyValues() {
		return new long[] { _topCardIndex };
	}

	void correctQuizGeneratorProperties(final int nCards) {
		_topCardIndex = Math.max(0, Math.min(_topCardIndex, nCards - 1));
	}

	void reactToReloadOfCards(final int nCards, final int tci) {
		if (tci >= 0) {
			_topCardIndex = tci;
		}
		correctQuizGeneratorProperties(nCards);
	}

	QuizPlus createNewQuizPlus(final int nCards) {
		correctQuizGeneratorProperties(nCards);
		if (_mode == Mode.STEP) {
			final int nInQuiz = nCards - _topCardIndex;
			final int[] quiz = new int[nInQuiz];
			for (int k = _topCardIndex; k < nCards; ++k) {
				quiz[k - _topCardIndex] = k;
			}
			final int[] criticalCardIndices = {};
			return new QuizPlus(quiz, criticalCardIndices);
		}
		final int nAvail = _topCardIndex + 1;
		final int nNewWordsInQuiz = Math.min(nAvail, _maxNNewWords);
		final int nRecentWordsInQuiz = Math.min(_maxNRecentWords, nAvail - nNewWordsInQuiz);
		final int nDifferentWordsInQuiz = nNewWordsInQuiz + nRecentWordsInQuiz;
		final int array1Len = nNewWordsInQuiz * _nRepeatsOfNew;
		final int[] array1 = new int[array1Len];
		final int[] temp = new int[_nRepeatsOfNew];
		for (int k0 = 0; k0 < nNewWordsInQuiz; ++k0) {
			Arrays.fill(temp, _topCardIndex - k0);
			System.arraycopy(temp, 0, array1, k0 * _nRepeatsOfNew, _nRepeatsOfNew);
		}
		FlashCardStatics.shuffleArray(array1, _r, /* lastValue= */-1);
		final int[] array2 = createArray2(nNewWordsInQuiz, nRecentWordsInQuiz);
		final int lastValue = array1Len > 0 ? array1[array1Len - 1] : -1;
		FlashCardStatics.shuffleArray(array2, _r, lastValue);
		final int fullQuizLen = array1Len + nDifferentWordsInQuiz;
		final int[] quiz = new int[fullQuizLen];
		System.arraycopy(array1, 0, quiz, 0, array1Len);
		System.arraycopy(array2, 0, quiz, array1Len, nDifferentWordsInQuiz);
		final int[] criticalQuizIndices = new int[nDifferentWordsInQuiz];
		for (int k = 0; k < nDifferentWordsInQuiz; ++k) {
			criticalQuizIndices[k] = array1Len + k;
		}
		return new QuizPlus(quiz, criticalQuizIndices);
	}

	private int[] createArray2(final int nNewWords, final int nRecentWords) {
		final int arrayLen = nNewWords + nRecentWords;
		final int[] array = new int[arrayLen];
		/** Fill in one copy of each of the new words. */
		for (int k = 0; k < nNewWords; ++k) {
			array[k] = _topCardIndex - k;
		}
		/**
		 * If there are more to fill in than we have number of choices for, or we are
		 * not using exponential or linear draws, then cycle through the choices.
		 */
		final int topChoiceForSlots = _topCardIndex - nNewWords;
		final int nChoicesForSlots = topChoiceForSlots + 1;
		if (nRecentWords >= nChoicesForSlots || _decayType == DecayType.NO_DECAY) {
			for (int k = 0; k < nRecentWords; ++k) {
				array[nNewWords + k] = topChoiceForSlots - (k % nChoicesForSlots);
			}
		} else {
			final double[] cums = computeCums(nRecentWords, nChoicesForSlots);
			for (int k = 0; k < nRecentWords; ++k) {
				final double cum = _r.nextDouble();
				final int cardIdx = selectCardIdx(cums, cum);
				array[nNewWords + k] = cardIdx;
				annihilateCardIdxForArray2(cums, cardIdx);
			}
		}
		return array;
	}

	private static int selectCardIdx(final double[] cums, final double cum) {
		if (cum <= cums[0])
			return 0;
		else if (cum >= cums[cums.length - 1])
			return cums.length - 1;
		final int cardIdx = Arrays.binarySearch(cums, cum);
		if (cardIdx >= 0)
			return cardIdx;
		return -cardIdx - 1;
	}

	private static void annihilateCardIdxForArray2(final double[] cums, final int cardIdx) {
		final int nCums = cums.length;
		final double probToDistribute = cums[cardIdx] - (cardIdx == 0 ? 0d : cums[cardIdx - 1]);
		final double scale = 1d / (1d - probToDistribute);
		/** Convert cums to probs. */
		for (int k = nCums - 1; k > 0; --k) {
			if (k == cardIdx) {
				cums[k] = 0d;
			} else {
				cums[k] = scale * (cums[k] - cums[k - 1]);
			}
		}
		/** Convert cums from probs back to cums. */
		for (int k = 1; k < nCums; ++k) {
			cums[k] += cums[k - 1];
		}
	}

	/** Creates cums, using exponential or linear decay. */
	private double[] computeCums(final int nRecentWords, final int nChoicesForSlots) {
		final double[] vector = new double[nChoicesForSlots];
		final double proportionForDregs = 1d - _percentageForRecentsI / 100d;
		final int nDregs = nChoicesForSlots - nRecentWords;
		final double pForDregs;
		if (_decayType == DecayType.EXPONENTIAL) {
			pForDregs = computePForExponential(nDregs, proportionForDregs);
		} else if (_decayType == DecayType.LINEAR) {
			pForDregs = computePForLinear(nDregs, proportionForDregs);
		} else
			return null;

		for (int k = 0; k < nDregs; ++k) {
			final double p;
			if (_decayType == DecayType.EXPONENTIAL) {
				p = Math.pow(pForDregs, nDregs - k);
			} else if (_decayType == DecayType.LINEAR) {
				p = (k + 1d) * pForDregs;
			} else
				return null;
			vector[k] = p;
		}
		final double proportionForRecentWords = 1d - proportionForDregs;
		final double pForRecentWords;
		if (_decayType == DecayType.EXPONENTIAL) {
			pForRecentWords = computePForExponential(nRecentWords, proportionForRecentWords);
		} else if (_decayType == DecayType.LINEAR) {
			pForRecentWords = computePForLinear(nRecentWords, proportionForRecentWords);
		} else
			return null;
		for (int k = 0; k < nRecentWords; ++k) {
			final double p;
			if (_decayType == DecayType.EXPONENTIAL) {
				p = Math.pow(pForRecentWords, nRecentWords - k);
			} else if (_decayType == DecayType.LINEAR) {
				p = (k + 1d) * pForRecentWords;
			} else
				return null;
			vector[nDregs + k] = p;
		}
		/** Convert vector from probs to cums. */
		for (int k = 1; k < nChoicesForSlots; ++k) {
			vector[k] += vector[k - 1];
		}
		/** Normalize; should be unnecessary. */
		final double f = vector[nChoicesForSlots - 1];
		for (int k = 0; k < nChoicesForSlots; ++k) {
			vector[k] /= f;
		}
		vector[nChoicesForSlots - 1] = 1d;
		return vector;
	}

	private static double computePForExponential(final int n, final double targetSum) {
		if (targetSum < 0d)
			return Double.NaN;
		if (n <= 0)
			return targetSum == 0d ? 0d : Double.NaN;
		if (n == 1)
			return targetSum;
		if (targetSum == 0d)
			return 0d;
		double tooLow = 0d;
		/** Since n >= 2, target is certainly too high. */
		double tooHigh = targetSum;
		while (tooHigh > tooLow * (1d + 1.e-10)) {
			final double thisP = (tooLow + tooHigh) / 2d;
			double v = (Math.pow(thisP, n + 1) - thisP) / (thisP - 1d);
			if (!Double.isFinite(v)) {
				/** thisP is too close to 1. So the computation of v is simple. */
				v = n;
			}
			if (v > targetSum) {
				tooHigh = thisP;
			} else {
				tooLow = thisP;
			}
		}
		return (tooLow + tooHigh) / 2d;
	}

	private static double computePForLinear(final int n, final double targetProportion) {
		if (n <= 0)
			return targetProportion == 0d ? 0d : Double.NaN;
		/**
		 * <pre>
		 * n(n + 1)/2 * p = target
		 * p = target * 2/(n(n + 1))
		 * </pre>
		 */
		return targetProportion * 2d / (n * (n + 1d));
	}

	String getTypeIIPrompt() {
		return String.format("\n\t%s(Top Card Index<%d>)", //
				PropertyPlus.TOP_CARD_INDEX._shortName, _topCardIndex);
	}

	void modifyProperty(final PropertyPlus propertyPlus, final String propertyPlusValue) {
		/** Only one property that we might modify right now; TOP_CARD_INDEX. */
		if (propertyPlus == PropertyPlus.TOP_CARD_INDEX) {
			try {
				_topCardIndex = Integer.parseInt(propertyPlusValue);
			} catch (final NumberFormatException e) {
				/** We get here if field1 is null or not a number. */
			}
		}
	}

	QuizPlusTransition getStatusChange(final Mode mode, final int nCards, final boolean restarted,
			final QuizPlus quizPlus) {
		if (restarted) {
			final QuizPlus newQuizPlus = quizPlus;
			newQuizPlus.resetForFullMode();
			return new QuizPlusTransition(quizPlus, newQuizPlus, ChangeType.RESTART);
		}
		if (quizPlus == null) {
			final QuizPlus newQuizPlus = createNewQuizPlus(nCards);
			return new QuizPlusTransition(quizPlus, newQuizPlus, ChangeType.NOTHING_TO_SOMETHING);
		}
		if (_changedQuizGeneratorParameters) {
			final QuizPlus newQuizPlus = createNewQuizPlus(nCards);
			_changedQuizGeneratorParameters = false;
			return new QuizPlusTransition(quizPlus, newQuizPlus, ChangeType.PARAMETERS_CHANGED);
		}
		if (mode == Mode.STEP)
			return new QuizPlusTransition(quizPlus, quizPlus, ChangeType.NO_CHANGE);
		if (quizPlus.haveWon(_allowablePerCent)) {
			final QuizPlus newQuizPlus;
			final ChangeType changeType;
			if (!quizPlus._criticalQuizIndicesOnly) {
				_topCardIndex += _maxNNewWords;
				correctQuizGeneratorProperties(nCards);
				newQuizPlus = createNewQuizPlus(nCards);
				changeType = ChangeType.MOVE_ON_WIN;
			} else {
				newQuizPlus = new QuizPlus(quizPlus);
				newQuizPlus.resetForFullMode();
				changeType = ChangeType.CRITICAL_ONLY_WIN;
			}
			return new QuizPlusTransition(quizPlus, newQuizPlus, changeType);
		}
		if (quizPlus.haveLost(_allowablePerCent)) {
			final QuizPlus newQuizPlus = new QuizPlus(quizPlus);
			newQuizPlus.adjustQuizForLoss(_r);
			return new QuizPlusTransition(quizPlus, newQuizPlus, ChangeType.LOSS);
		}
		return new QuizPlusTransition(quizPlus, quizPlus, ChangeType.NO_CHANGE);
	}

	String getString() {
		final String s = String.format(//
				"TopCardIdx[%d], #New/Recent Words[%d/%d], FailureRate[%d%%] Decay[%s]", //
				_topCardIndex, _maxNNewWords, _maxNRecentWords, _allowablePerCent, _decayType.name());
		return s;
	}

	@Override
	public String toString() {
		return getString();
	}
}
