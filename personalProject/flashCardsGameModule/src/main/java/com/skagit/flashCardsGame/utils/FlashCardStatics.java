package com.skagit.flashCardsGame.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import com.skagit.utils.StaticUtilities;

public class FlashCardStatics {

	/**
	 * <pre>
	 *  Good list of characters.
	 *  http://xahlee.info/comp/unicode_arrows.html
	 * </pre>
	 */
	final public static char _keyboardSymbol = '\u2328';
	final public static char _ClubSymbolChar = '\u2663';
	final public static char _DiamondSymbolChar = '\u2666';
	final public static char _EmptySetChar = '\u2205';
	final public static char _HeavyCheckChar = '\u2714';
	final public static char _ReturnChar = '\u23CE';
	final public static char _RtArrowChar = '\u2192';
	final public static char _RtArrowChar2 = '\u21a0';
	final public static char _SpadeSymbolChar = '\u2660';
	final public static char _LtArrowChar = '\u2190';
	final public static char _TabSymbolChar = '\u2409';
	final public static char _CommentChar = '!';
	final public static char _EditPropertiesChar = '@';
	final public static char _HelpChar = 'H';
	final public static char _QuitChar = '!';
	final public static char _ReloadCardsChar = '$';
	final public static char _RestartQuizChar = '#';
	final public static char _NoChar = 'N';
	final public static char _YesChar = 'Y';
	final public static char _FileDelimiter = '%';
	final public static String _SoundString, _PenString;

	final private static String _GameFileExtensionLc = "properties";
	final private static String _CardsFileExtensionTxtLc = "txt";
	final public static int _NDigitsForBackups = 2;

	static {
		final byte[] soundBytes = { (byte) 0xF0, (byte) 0x9F, (byte) 0x94, (byte) 0x8A };
		_SoundString = new String(soundBytes, StandardCharsets.UTF_8);
		final byte[] penBytes = { (byte) 0xF0, (byte) 0x9F, (byte) 0x96, (byte) 0x8A };
		_PenString = new String(penBytes, StandardCharsets.UTF_8);
	}

	public static char[] _SpecialChars = { //
			_keyboardSymbol, _ClubSymbolChar, _CommentChar, _DiamondSymbolChar, _EditPropertiesChar, _EmptySetChar,
			_HeavyCheckChar, _HelpChar, _QuitChar, _ReloadCardsChar, _RestartQuizChar, _ReturnChar, _RtArrowChar,
			_RtArrowChar2, _SpadeSymbolChar, _LtArrowChar, _NoChar, _TabSymbolChar, _YesChar, _FileDelimiter };

	final public static int _RoomLen = 10;
	final public static int _MaxLenForCardPart = 45;
	final public static int _MaxLineLen = 80;
	final public static int _MaxNFailsPerElement = 5;
	final public static int _NominalTabLen = 8;

	final public static String _CommentString = Character.toString(_CommentChar) + ' ';
	final public static String _CountAsRightString = "Count as Right? ";
	final public static String _EndCardsString = "$$";
	/** Either of the following two FieldSeparators seems to work. */
	final public static String _FieldSeparator = "\\s*\\t\\s*";
	final public static String _FieldSeparator1 = "(\\s*\\t\\s*)+";
	final public static String _RegExForPunct = "[,.;:?!]+";
	final public static String _Sep1 = Character.toString(_ClubSymbolChar);
	final public static String _Sep2 = Character.toString(_DiamondSymbolChar) + " ";
	final public static String _HelpString = String.format("""
			%c=Show this Message,\s
			%c=Quit,\s
			%c=Edit Properties,\s
			%c=Restart Quiz,\s
			%c=Reload Cards,\s
			%c=Show-and-ask,\s
			%s=Next Line is Continuation""", //
			_HelpChar, //
			_QuitChar, //
			_EditPropertiesChar, //
			_RestartQuizChar, //
			_ReloadCardsChar, //
			_ReturnChar, //
			Character.toString(_TabSymbolChar) + _ReturnChar//
	);
	final public static String _IndentString = String.format(String.format("%%-%ds", _NominalTabLen), "");
	final public static String _NoString = "No";
	final public static String _PrefaceForNewLine = _IndentString + _Sep1;
	final public static int _PrefaceForNewLineLen = _PrefaceForNewLine.length();
	final public static String _YesString = "Yes";

	/** Derived ints. */
	final public static int _Sep1Len = _Sep1.length();
	final public static int _Sep2Len = _Sep2.length();
	final public static int _IndentLen = _IndentString.length();

	public static class YesNoResponse {
		final public boolean _yesValue, _lastLineWasBlank;

		public YesNoResponse(final Scanner sc, final boolean defaultYesValue) {
			final char otherChar = defaultYesValue ? _NoChar : _YesChar;
			final InputString inputString = new InputString(sc);
			final String response = inputString._inputString;
			final boolean lastLineWasBlank = inputString._lastLineWasBlank;
			if (response.length() == 0) {
				_yesValue = defaultYesValue;
			} else {
				_yesValue = (Character.toUpperCase(response.charAt(0)) == otherChar) ^ defaultYesValue;
			}
			_lastLineWasBlank = lastLineWasBlank;
		}
	}

	/**
	 * <pre>
	 * Good article on dealing with VN diacritics.
	 * https://namnguyen1202.hashnode.dev/removing-vietnamese-diacritic-in-java
	 * Note the use of a default ctor in an anonymous inherited class.
	 * </pre>
	 */
	private static final HashMap<Character, Character> _VnToEngCharMap = new HashMap<>() {
		private static final long serialVersionUID = 1L;
		{
			final String[][] mappings = { //
					{ "áàảãạâấầẩẫậăắằẳẵặ", "a" }, //
					{ "đ", "d" }, //
					{ "éèẻẽẹêếềểễệ", "e" }, //
					{ "íìỉĩị", "i" }, //
					{ "óòỏõọôốồổỗộơớờởỡợ", "o" }, //
					{ "úùủũụưứừửữự", "u" }, //
					{ "ýỳỷỹỵ", "y" }, //
			};
			final int nPairs = mappings.length;
			for (int k0 = 0; k0 < nPairs; ++k0) {
				final String[] pair = mappings[k0];
				final char engChar = pair[1].charAt(0);
				final char engCharUc = Character.toUpperCase(engChar);
				final String vnChars = pair[0];
				final int nVnChars = vnChars.length();
				for (int k1 = 0; k1 < nVnChars; ++k1) {
					final char vnChar = vnChars.charAt(k1);
					final char vnCharUc = Character.toUpperCase(vnChar);
					put(vnChar, engChar);
					put(vnCharUc, engCharUc);
				}
			}
		}
	};

	final public static String StripVNDiacritics(final String s) {
		final StringBuilder sb = new StringBuilder(s);
		boolean didSomething = false;
		for (int k = 0; k < sb.length(); k++) {
			final char c = sb.charAt(k);
			final Character target = _VnToEngCharMap.get(c);
			if (target != null && c != target) {
				sb.setCharAt(k, target);
				didSomething = true;
			}
		}
		return didSomething ? sb.toString() : s;
	}

	public static String KillPunct(final String s) {
		if (s == null) {
			return "";
		}
		final String ss = s.replaceAll(_RegExForPunct, "");
		return ss.length() == s.length() ? s : ss;
	}

	public static String getFullYesNoPrompt(final String prompt, final boolean defaultYesValue) {
		final char otherChar = defaultYesValue ? _NoChar : _YesChar;
		final String defaultString = defaultYesValue ? _YesString : _NoString;
		final String otherString = defaultYesValue ? _NoString : _YesString;
		return String.format("%s %c=%s,%c=%s: ", prompt, _ReturnChar, defaultString, otherChar, otherString);
	}

	/** Avoids having the same value consecutively. */
	public static void shuffleArray(final int[] ints, final Random r, int lastValue) {
		final int n = ints.length;
		for (int k0 = 0; k0 < n; ++k0) {
			for (int nFails = 0; nFails <= _MaxNFailsPerElement; ++nFails) {
				final int vK0 = ints[k0];
				final int k1 = k0 + r.nextInt(n - k0);
				final int vK1 = ints[k1];
				if (vK1 != lastValue || nFails == _MaxNFailsPerElement) {
					lastValue = ints[k0] = vK1;
					ints[k1] = vK0;
					break;
				}
			}
		}
	}

	public static File getGameFile(final String gameFileString0) {
		final String gameFileDir = StaticUtilities.stripExtension(gameFileString0);
		for (int iPass = 0; iPass < 3; ++iPass) {
			final File contextDir;
			if (iPass == 0) {
				contextDir = FlashCardsGameDirsTracker._DataDir;
			} else if (iPass == 1) {
				contextDir = FlashCardsGameDirsTracker._GameDirsDir;
			} else {
				contextDir = FlashCardsGameDirsTracker.getUserDir();
			}
			final File gameDir = new File(contextDir, gameFileDir);
			final String gameFileName = gameDir.getName() + '.' + _GameFileExtensionLc;
			final File gameFile = new File(gameDir, gameFileName);
			if (gameFile.isFile()) {
				return gameFile;
			}
		}
		return null;
	}

	public static File getCardsFile(final File gameDir, final String cardsFileString) {
		for (int iPass = 0; iPass < 2; ++iPass) {
			final File contextDir = iPass == 0 ? gameDir : FlashCardsGameDirsTracker._CardsFilesDir;
			final File cardsFile0 = new File(contextDir, cardsFileString);
			if (cardsFile0.isFile()) {
				return cardsFile0;
			}
			final String cardsFileString1 = StaticUtilities.forceExtension(cardsFileString, _CardsFileExtensionTxtLc);
			final File cardsFile1 = new File(contextDir, cardsFileString1);
			if (cardsFile1.isFile()) {
				return cardsFile1;
			}
		}
		return null;
	}

	public static File getSoundFilesDir(final File gameDir, final String soundFilesString0) {
		final String soundFilesString = StaticUtilities.convertToFileSeparator(soundFilesString0);
		for (int iPass = 0; iPass < 2; ++iPass) {
			final File contextDir = iPass == 0 ? gameDir : FlashCardsGameDirsTracker._SoundFilesDirsDir;
			final File soundFilesDir = new File(contextDir, soundFilesString);
			if (soundFilesDir.isDirectory()) {
				return soundFilesDir;
			}
		}
		return null;
	}

}
