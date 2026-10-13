package com.skagit.audioDirProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

public class EliminateBoringFNamesAndTitles extends AudioDirProcessor {

	public EliminateBoringFNamesAndTitles() {
		super();
	}

	@Override
	void doTheDirItself(final DirInfo dirInfo) {
		getCautionString(dirInfo, /* doTheWork= */true);
	}

	@Override
	void doAfterSubdirsAreDone(final DirInfo dirInfo) {
	}

	final private static boolean _ForceFNameIsBoring = false;
	final private static boolean _ForceTitleIsBoring = false;

	protected String getCautionString(final DirInfo dirInfo, final boolean doTheWork) {
		final File d = dirInfo._d;
		final TreeSet<String> audioFNamesToNotDelete = new TreeSet<>();
		audioFNamesToNotDelete.addAll(dirInfo._toCopy);
		audioFNamesToNotDelete.addAll(dirInfo._needsCnvrtng);
		audioFNamesToNotDelete.addAll(dirInfo._srcFName_to_cnvrtdFName.keySet());
		audioFNamesToNotDelete.addAll(dirInfo._srcFName_to_cnvrtdFName.values());
		final String indentString = dirInfo._indentString;
		final StringBuilder cautionString = new StringBuilder();
		for (final String fName : audioFNamesToNotDelete) {
			final File f = new File(d, fName);
			final boolean isBoringFName = isBoringFName(fName);
			try {
				AudioFile audioFile;
				try {
					audioFile = AudioFileIO.read(f);
				} catch (final CannotReadException e) {
					cautionString
							.append(String.format("\n%s  Cannot AudioRead %s. Leaving alone.", indentString, fName));
					continue;
				}
				Tag fTag = audioFile.getTag();
				final String title = fTag == null ? null : fTag.getFirst(FieldKey.TITLE);
				final boolean isBoringTitle = isBoringTitle(title);
				if (isBoringFName == isBoringTitle) {
					continue;
				}
				if (isBoringFName) {
					final String newFName = getNewFNameFromTitle(f, title);
					if (newFName != null) {
						cautionString.append(String.format("\n%s  Moving %s to %s", indentString, fName, newFName));
						if (doTheWork) {
							final File newF = new File(d, newFName);
							final Path oldPath = Paths.get(f.getPath());
							final Path newPath = Paths.get(newF.getPath());
							try {
								Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
							} catch (final NoSuchFileException e) {
								cautionString.append(String.format(":: FAIL For Reason AA.", indentString));
								continue;
							}
						}
					}
				} else {
					final String baseName = FilenameUtils.getBaseName(fName);
					cautionString.append(String.format("\n%s  For %s, changing title.", indentString, fName));
					if (doTheWork) {
						if (fTag == null) {
							fTag = audioFile.getTagOrCreateAndSetDefault();
						}
						fTag.setField(FieldKey.TITLE, baseName);
						audioFile.commit();
						audioFile.setTag(fTag);
						AudioFileIO.write(audioFile);
					}
				}
			} catch (final KeyNotFoundException | IOException | TagException | ReadOnlyFileException
					| InvalidAudioFrameException | CannotWriteException e) {
				cautionString.append(String.format(":: FAIL For Reason BB.", indentString));
			}
		}
		return cautionString.toString();
	}

	@Override
	boolean isHealthyDir(final DirInfo dirInfo) {
		return true;
	}

	@Override
	String getJobString() {
		return "ELIMINATING BORING NAMES";
	}

	@Override
	String getLogFNameBase() {
		return "Eliminating Boring Names";
	}

	final private static Pattern ForbiddenCharsPattern = Pattern.compile("[/\\\\<>:?*\\\"]+");
	final private static Pattern BoringFNamePattern = Pattern.compile("\\d*\\s*[Tt]rack\\s+\\d+\\.[^\\.]+$");
	final private static Pattern BoringTitlePattern0 = Pattern.compile("[Tt]rack\\s\\d+\\s*$");
	final private static Pattern BoringTitlePattern1 = Pattern.compile("^\\s*d*$");

	static String getNewFNameFromTitle(final File f, final String title0) {
		/**
		 * Must clear illegal characters from the proposed name before looking for the baseName and
		 * extension.
		 */
		final String title1 = ForbiddenCharsPattern.matcher(title0).replaceAll("'").trim();
		final String newBaseName0;
		if (getMatchingAudioExtension(title1) != null) {
			/** The title already has an audio extension built into it. Strip it. */
			newBaseName0 = FilenameUtils.getBaseName(title1);
		} else {
			newBaseName0 = title1;
		}
		final String fName = f.getName();
		if (newBaseName0.length() == 0) {
			return fName;
		}
		final String fExtension = FilenameUtils.getExtension(fName);
		final String newFName = newBaseName0 + (fExtension == null ? "" : ("." + fExtension));
		final File newF = new File(f.getParentFile(), newFName);
		final boolean alreadyThere = newF.isFile();
		return alreadyThere ? null : newFName;
	}

	static boolean isBoringFName(final String fName) {
		if (_ForceFNameIsBoring) {
			return true;
		}
		return BoringFNamePattern.matcher(fName).find();
	}

	static public void main(final String[] args) {
		final String fName = "01 Track 1.wma";
		final boolean isBoringFName = isBoringFName(fName);
		System.out.printf("%s:: %b", fName, isBoringFName);
	}

	static boolean isBoringTitle(String title) {
		if (title == null || _ForceTitleIsBoring) {
			return true;
		}
		title = title.trim();
		if (title.length() == 0) {
			return true;
		}
		if (BoringTitlePattern0.matcher(title).find()) {
			return true;
		}
		return BoringTitlePattern1.matcher(title).find();
	}

	@Override
	String getCautionString(final DirInfo dirInfo) {
		return getCautionString(dirInfo, /* doTheWork= */false);
	}

}
