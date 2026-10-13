package com.skagit.flashCardsGame.utils;

import java.util.Scanner;

import com.skagit.utils.StringUtilities;

public class InputString {
	final public boolean _lastLineWasBlank;
	final public int _nLinesOfResponse;
	final public String _inputString;

	public InputString(final Scanner sysInScanner) {
		final StringBuilder s = new StringBuilder();
		boolean lastLineWasBlank = false;
		int nLinesOfResponse = 1;
		for (;;) {
			final String thisLine = sysInScanner.nextLine();
			final int len = thisLine.length();
			final boolean moreLines = len > 0 && thisLine.charAt(len - 1) == '\t';
			if (!thisLine.isBlank()) {
				lastLineWasBlank = false;
				s.append(" ").append(thisLine);
			} else if (moreLines) {
				lastLineWasBlank = true;
			}
			if (!moreLines) {
				break;
			}
			/** The next line has indented input. */
			System.out.print(FlashCardStatics._IndentString);
			++nLinesOfResponse;
		}
		_inputString = StringUtilities.CleanWhiteSpace(s.toString());
		_lastLineWasBlank = lastLineWasBlank;
		_nLinesOfResponse = nLinesOfResponse;
	}
}
