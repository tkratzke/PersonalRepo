package com.skagit.flashCardsGame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.TreeMap;

import com.skagit.flashCardsGame.utils.FlashCardStatics;
import com.skagit.utils.Constants0;
import com.skagit.utils.StaticUtilities;

class Card {
	int _cardNumber;
	final FullSide _clueSide;
	final FullSide _answerSide;
	String[] _commentLines;
	final String _sheetName;
	final int _rowNumber;

	Card(final boolean switchSides, final TreeMap<String, String> partToStem, final int cardNumber,
			final String clueSideString, final String answerSideString, final String[] commentLines) {
		_cardNumber = cardNumber;
		_clueSide = new FullSide(partToStem, switchSides ? answerSideString : clueSideString);
		_answerSide = new FullSide(partToStem, switchSides ? clueSideString : answerSideString);
		_commentLines = commentLines;
		_sheetName = null;
		_rowNumber = -1;
	}

	Card(final boolean switchSides, final TreeMap<String, String> partToStem, final int cardNumber,
			final String clueSideString, final String answerSideString, final String sheetName, final int rowNumber) {
		_cardNumber = cardNumber;
		_clueSide = new FullSide(partToStem, switchSides ? answerSideString : clueSideString);
		_answerSide = new FullSide(partToStem, switchSides ? clueSideString : answerSideString);
		_commentLines = null;
		_sheetName = sheetName;
		_rowNumber = rowNumber;
	}

	final static Comparator<Card> _ByCardNumberOnly = (card0, card1) -> {
		final int compareValue = StaticUtilities.NullCompare(card0, card1);
		if (-1 <= compareValue && compareValue <= 1) {
			return compareValue;
		}
		final int idx0 = card0._cardNumber, idx1 = card1._cardNumber;
		return idx0 < idx1 ? -1 : (idx0 > idx1 ? 1 : 0);
	};

	/**
	 * As written, this puts the ones that have Answers that have Sound Files on the
	 * top, and then by increasing number of spaces in the answers, and then by
	 * increasing length of the answers.
	 */
	final static Comparator<Card> _StrangeSort = new Comparator<>() {

		@Override
		public int compare(final Card card0, final Card card1) {
			int compareValue = StaticUtilities.NullCompare(card0, card1);
			if (-1 <= compareValue && compareValue <= 1) {
				return compareValue;
			}
			final FullSide answerSide0 = card0._answerSide;
			final FullSide answerSide1 = card1._answerSide;
			final String soundFileString0 = card0.getSoundFileString(/* forClue= */false);
			final String soundFileString1 = card1.getSoundFileString(/* forClue= */false);
			if ((soundFileString0 != null) != (soundFileString1 != null)) {
				return soundFileString0 != null ? -1 : 1;
			}
			if (soundFileString0 != null) {
				compareValue = soundFileString0.compareTo(soundFileString1);
				if (compareValue != 0) {
					return compareValue;
				}
			}
			/** Retain the input order now. */
			if (!Constants0.Functions.returnTrue()) {
				return _ByCardNumberOnly.compare(card0, card1);
			}
			final String s0 = answerSide0._stringPart;
			final String s1 = answerSide1._stringPart;
			final int nSpaces0 = countSpaces(s0);
			final int nSpaces1 = countSpaces(s1);
			if (nSpaces0 != nSpaces1) {
				return nSpaces0 < nSpaces1 ? -1 : 1;
			}
			final int len0 = s0.length();
			final int len1 = s1.length();
			if (len0 != len1) {
				return len0 < len1 ? -1 : 1;
			}
			if (!Constants0.Functions.returnTrue()) {
				final String s00 = FlashCardStatics.StripVNDiacritics(s0);
				final String s10 = FlashCardStatics.StripVNDiacritics(s1);
				compareValue = s00.compareTo(s10);
				if (compareValue != 0) {
					return compareValue;
				}
				compareValue = s0.compareTo(s1);
				if (compareValue != 0) {
					return compareValue;
				}
			}
			return _ByCardNumberOnly.compare(card0, card1);
		}
	};

	private static int countSpaces(final String s) {
		if (s == null) {
			return -1;
		}
		return s.length() - s.replace(" ", "").length();
	}

	static final Comparator<Card> _ByClueSideOnly = new Comparator<>() {

		@Override
		public int compare(final Card card0, final Card card1) {
			final int compareValue = StaticUtilities.NullCompare(card0, card1);
			if (-1 <= compareValue && compareValue <= 1) {
				return compareValue;
			}
			return card0._clueSide.compareTo(card1._clueSide);
		}
	};

	static final Comparator<Card> _ByAnswerSideOnly = new Comparator<>() {

		@Override
		public int compare(final Card card0, final Card card1) {
			final int compareValue = StaticUtilities.NullCompare(card0, card1);
			if (-1 <= compareValue && compareValue <= 1) {
				return compareValue;
			}
			return card0._answerSide.compareTo(card1._answerSide);
		}
	};

	static final Comparator<Card> _ByClueThenAnswer = (card0, card1) -> {
		final int compareValue = _ByClueSideOnly.compare(card0, card1);
		if (compareValue != 0) {
			return compareValue;
		}
		return _ByAnswerSideOnly.compare(card0, card1);
	};

	@Override
	public boolean equals(final Object o) {
		if (o == null || !(o instanceof Card)) {
			return false;
		}
		return _ByClueThenAnswer.compare(this, (Card) o) == 0;
	}

	String getString() {
		final int nCommentLines = _commentLines == null ? 0 : _commentLines.length;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			final PrintStream ps = new PrintStream(baos);
			for (int k = 0; k < nCommentLines; ++k) {
				ps.println(FlashCardStatics._CommentString + _commentLines[k]);
			}
			ps.printf("%04d.\t%s:\t%s", _cardNumber, _clueSide._trimmedInputString, _answerSide._trimmedInputString);
			if (_sheetName != null && _rowNumber >= 0) {
				ps.printf(" (from [Sheet/Row]=[%s/%03d])", _sheetName, _rowNumber);
			}
			ps.flush();
			return baos.toString();
		} catch (final IOException e) {
		}
		return null;
	}

	public String getStringPart(final boolean forClue) {
		return (forClue ? _clueSide : _answerSide)._stringPart;
	}

	public String getSoundFileString(final boolean forClue) {
		final String fullSoundFileString = (forClue ? _clueSide : _answerSide)._fullSoundFileString;
		final int len = fullSoundFileString == null ? 0 : fullSoundFileString.length();
		if (fullSoundFileString != null && fullSoundFileString.length() > 2) {
			return fullSoundFileString.substring(1, len - 1);
		}
		return null;
	}

	public String getTrimmedInputString(final boolean forClue) {
		return (forClue ? _clueSide : _answerSide)._trimmedInputString;
	}

	@Override
	public String toString() {
		return getString();
	}

}