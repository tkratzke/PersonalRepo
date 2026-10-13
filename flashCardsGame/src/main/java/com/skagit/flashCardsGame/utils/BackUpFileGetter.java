package com.skagit.flashCardsGame.utils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.skagit.utils.StaticUtilities;

public class BackUpFileGetter {
	final static Pattern _Pattern = Pattern.compile("^(.*?)(\\d+)$");

	/**
	 * If the name without the extension ends with digits, then we increment the
	 * number represented by the digits. Otherwise, we add $ and
	 * FlashCardsStatics._NDigitsForBackups 0s.
	 */
	public static File getBackUpFile(final File f) {
		final String fName = f.getName();
		final int fNameLen = fName.length();
		/** Because of OS problems with files that end with a period, let this crash. */
		if (fName.charAt(fNameLen - 1) == '.') {
			return null;
		}

		final String newFName = createNewFName(fName);
		final File parent = f.getParentFile();
		final File newF = parent == null ? new File(newFName) : new File(parent, newFName);
		if (!newF.exists()) {
			return newF;
		}
		return getBackUpFile(newF);
	}

	private static String createNewFName(final String fName) {
		final String[] stemAndExtension = StaticUtilities.getStemAndExtension(fName);
		final String stem = stemAndExtension[0];
		final String extension = stemAndExtension[1];
		final Matcher matcher = _Pattern.matcher(stem);
		String newStem = null;
		if (matcher.matches()) {
			final String group1 = matcher.group(1);
			final String group2 = matcher.group(2);
			try {
				final int k = Integer.parseInt(group2);
				/** Just increment the trailing number. */
				newStem = String.format("%s%0" + FlashCardStatics._NDigitsForBackups + "d", group1, k + 1);
			} catch (final NumberFormatException e) {
			}
		}
		if (newStem == null) {
			/** Put $ and a bunch of 0s into stem. */
			newStem = String.format("%s$%0" + FlashCardStatics._NDigitsForBackups + "d", stem, 0);
		}
		if (extension == null || extension.length() == 0) {
			return newStem;
		}
		return newStem + '.' + extension;
	}

	public static void main(final String[] args) {
		final String fName = "y34x02.1";
		System.out.print(createNewFName(fName));
	}
}
