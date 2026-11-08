package com.skagit.giBill;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class GiBill {

	private enum BufferType {
		UNKNOWN(false), BODY(false), NEW_FOOTNOTE(true), CONTINUE_FOOTNOTE(true);

		final boolean _isPartOfFootnote;

		BufferType(final boolean isPartOfFootnote) {
			_isPartOfFootnote = isPartOfFootnote;
		}
	}

	final private static String _EndChapterMarkup = "<END CHAPTER>";
	final private static String _NewFootnoteMarkup = "(Footnote";
	final private static String _ContinueFootnoteMarkup = "<Continue Footnote>";
	final private static String _NewFootnoteMarkupLc = _NewFootnoteMarkup.toLowerCase();
	final private static String _ContinueFootnoteMarkupLc = _ContinueFootnoteMarkup.toLowerCase();
	final private static String _EndChapterOutput = "\n<END CHAPTER>\n\n\n\n";

	// static final File _InFile = new File("../GiBill/TestFile.txt");
	final private static File _InFile = new File("../GiBill/GI Bill SourceText.txt");
	final private static File _OutFile;
	static {
		_OutFile = new File(_InFile.getParentFile(), "$$" + _InFile.getName());
	}

	public static void main(final String[] args) {
		String buffer = "";
		boolean havePrinted = false;
		BufferType bufferType = BufferType.UNKNOWN;
		BufferType lastPrintedBufferType = BufferType.UNKNOWN;
		try (final FileReader fileReader = new FileReader(_InFile);
				final BufferedReader bufferedReader = new BufferedReader(fileReader);
				final PrintWriter pw = new PrintWriter(_OutFile)) {
			for (;;) {
				try {
					final String line = bufferedReader.readLine().trim();
					/** An empty line dumps the buffer and sets bufferType to UNKNOWN. */
					if (line.length() == 0) {
						dumpBuffer(pw, havePrinted, buffer, bufferType, lastPrintedBufferType);
						if (buffer.length() > 0) {
							havePrinted = true;
							buffer = "";
							lastPrintedBufferType = bufferType;
							bufferType = BufferType.UNKNOWN;
						}
						continue;
					}
					/** It's not an empty line. */
					final String lineLc = line.toLowerCase();
					final boolean bufferIsFootnote = bufferType._isPartOfFootnote;
					final boolean lineStartsFootnote = lineLc.startsWith(_NewFootnoteMarkupLc);
					final boolean lineStartsFootnoteContinuation;
					if (!lineLc.startsWith(_ContinueFootnoteMarkupLc)) {
						lineStartsFootnoteContinuation = false;
					} else if (bufferIsFootnote) {
						lineStartsFootnoteContinuation = true;
					} else {
						lineStartsFootnoteContinuation = false;
					}
					if (lineStartsFootnote) {
						dumpBuffer(pw, havePrinted, buffer, bufferType, lastPrintedBufferType);
						havePrinted = buffer.length() > 0;
						buffer = line;
						lastPrintedBufferType = bufferType;
						bufferType = BufferType.NEW_FOOTNOTE;
					} else if (lineStartsFootnoteContinuation) {
						dumpBuffer(pw, havePrinted, buffer, bufferType, lastPrintedBufferType);
						havePrinted = buffer.length() > 0;
						buffer = line.substring(_ContinueFootnoteMarkup.length()).trim();
						lastPrintedBufferType = bufferType;
						bufferType = BufferType.CONTINUE_FOOTNOTE;
					} else if (bufferType == BufferType.UNKNOWN) {
						bufferType = BufferType.BODY;
						buffer = line;
					} else {
						buffer = buffer + " " + line;
					}
				} catch (final IOException | NullPointerException e) {
					dumpBuffer(pw, havePrinted, buffer, bufferType, lastPrintedBufferType);
					break;
				}
			}
		} catch (final IOException e) {
		}
	}

	private static boolean dumpBuffer(final PrintWriter pw, boolean havePrinted, final String buffer0,
			final BufferType bufferType, final BufferType lastPrintedBufferType0) {
		final String buffer = buffer0.trim().replace("  ", " ");
		if (buffer.length() == 0) {
			return /* dumpedSomething= */false;
		}
		if (bufferType == BufferType.BODY) {
			final String[] bodyParts = buffer.split(_EndChapterMarkup);
			final int nBodyParts = bodyParts.length;
			BufferType lastPrintedBufferType = lastPrintedBufferType0;
			for (int k = 0; k < nBodyParts; ++k) {
				final String bodyPart = bodyParts[k].trim();
				final String initialString;
				if (!havePrinted) {
					if (bodyPart.length() == 0) {
						lastPrintedBufferType = BufferType.BODY;
						continue;
					}
					initialString = "";
				} else if (k > 0) {
					initialString = _EndChapterOutput;
				} else if (lastPrintedBufferType._isPartOfFootnote) {
					initialString = "\n\n";
				} else {
					initialString = "\n";
				}
				pw.printf("%s", initialString + bodyPart);
				System.out.printf("%s", initialString + bodyPart);
				havePrinted = true;
				lastPrintedBufferType = BufferType.BODY;
			}
			return /* dumpedSomething= */true;
		}
		final String initialString;
		if (bufferType == BufferType.NEW_FOOTNOTE) {
			if (!havePrinted) {
				initialString = "";
			} else {
				initialString = "\n";
			}
		} else if (bufferType == BufferType.CONTINUE_FOOTNOTE) {
			if (!havePrinted) {
				initialString = "";
			} else {
				initialString = "\n";
			}
		} else {
			initialString = null;
			System.exit(33);
		}
		pw.printf("%s", initialString + buffer);
		System.out.printf("%s", initialString + buffer);
		return /* dumpedSomething= */true;
	}

}
