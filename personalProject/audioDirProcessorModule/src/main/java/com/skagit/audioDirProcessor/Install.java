package com.skagit.audioDirProcessor;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.datatype.Artwork;

public class Install extends AudioDirProcessor {

	/** ctor. */
	public Install() {
		super();
	}

	/** Overrides. */
	@Override
	String getJobString() {
		return "INSTALLING AFTER CONVERSION";
	}

	@Override
	String getLogFNameBase() {
		return "Installing after Conversion";
	}

	@Override
	void doTheDirItself(final DirInfo dirInfo) {
		final File d = dirInfo._d;

		final File destnDir = dirInfo._destnDir;
		final String introString = dirInfo._introString;

		/** If destnDir is an existing file, abort. */
		if (destnDir.isFile()) {
			final String errString = String.format("FAIL on INSTALL: %s is an existing file and not a directory.",
					destnDir.getName());
			dumpString(String.format("%s:: %s", introString, errString), /* e= */null);
			return;
		}

		/** Delete the files slated for deletion. */
		for (final String fName : dirInfo._toDelete) {
			new File(d, fName).delete();
		}

		destnDir.mkdirs();

		/** COPY over the jpeg files. */
		if (_CopyJpgFiles) {
			final TreeSet<String> jPegFNames = dirInfo._jPegFNames;
			for (final String fName : jPegFNames) {
				final File destnF = new File(destnDir, fName);
				if (!destnF.isFile()) {
					try {
						FileUtils.copyFile(new File(d, fName), destnF);
					} catch (final Exception e) {
						final String errString = String.format("FAIL on INSTALL, while copying %s.", fName);
						dumpString(String.format("%s:: %s", introString, errString), e);
						return;
					}
				}
			}
		}

		/**
		 * MOVE each cnvrtd file f, unless a file destnF in destnDir has the same name as f. In that case,
		 * simply delete f. If we do move f, fix the tags since we don't know if foobar2000 got all of them.
		 */
		final TreeMap<String, String> srcFName_to_cnvrtdFName = dirInfo._srcFName_to_cnvrtdFName;
		for (final Map.Entry<String, String> entry : srcFName_to_cnvrtdFName.entrySet()) {
			final String fName = entry.getValue();
			final File f = new File(d, fName);
			final File destnF = new File(destnDir, fName);
			final File srcF = new File(d, entry.getKey());
			if (destnF.exists()) {
				f.delete();
			} else {
				try {
					FileUtils.moveFile(f, destnF);
				} catch (final IOException e) {
					continue;
				}
			}
			helpWithTags(dirInfo, srcF, destnF);
		}

		/** COPY the files in toCopy. */
		final TreeSet<String> toCopy = dirInfo._toCopy;
		for (final String fName : toCopy) {
			final File srcF = new File(d, fName);
			final File destnF = new File(destnDir, fName);
			if (!destnF.exists()) {
				try {
					FileUtils.copyFile(srcF, destnF);
				} catch (final IOException e) {
				}
			}
			helpWithTags(dirInfo, srcF, destnF);
		}
	}

	final private static EnumSet<FieldKey> _RequiresNumber = EnumSet.of( //
			FieldKey.TRACK, FieldKey.TRACK_TOTAL, FieldKey.DISC_NO, FieldKey.DISC_TOTAL //
	);
	final private static Pattern _NumberFinder = Pattern.compile("^\\s*\\-{0,1}\\d+\\.{0,1}\\d*");

	private static void helpWithTags(final DirInfo dirInfo, final File srcF, final File destnF) {
		AudioFile destnAudioF = null;
		Tag destnTag = null;

		/** Look for art in destnF first. If there is something, don't try to fix it. */
		boolean haveArt = false;
		List<Artwork> destnArtworkList = null;
		try {
			destnAudioF = AudioFileIO.read(destnF);
			destnTag = destnAudioF.getTagOrCreateAndSetDefault();
			destnArtworkList = destnTag.getArtworkList();
			haveArt = destnArtworkList.size() > 0;
		} catch (IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException
				| CannotReadException e) {
			return;
		}

		/** Read in srcF. */
		AudioFile srcAudioF = null;
		Tag srcTag = null;
		List<Artwork> srcArtworkList = null;
		try {
			srcAudioF = AudioFileIO.read(srcF);
			srcTag = srcAudioF.getTag();
			if (srcTag != null) {
				srcArtworkList = srcTag.getArtworkList();
			}
		} catch (IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException
				| CannotReadException e) {
		}

		if (srcTag != null && destnTag != null) {
			/** Copy over tags that jAudioTagger knows about, but aren't in destnF. */
			for (final FieldKey fieldKey : FieldKey.values()) {
				try {
					final String destnValue = destnTag.getFirst(fieldKey);
					if (destnValue == null || destnValue.length() == 0) {
						final String srcValue0 = srcTag.getFirst(fieldKey);
						final String srcValue;
						if (_RequiresNumber.contains(fieldKey)) {
							final Matcher matcher = _NumberFinder.matcher(srcValue0);
							if (matcher.find()) {
								srcValue = srcValue0.substring(0, matcher.end());
							} else {
								srcValue = null;
							}
						} else {
							srcValue = srcValue0;
						}
						if (srcValue != null && srcValue.length() > 0) {
							destnTag.setField(fieldKey, srcValue);
						}
					}
				} catch (KeyNotFoundException | FieldDataInvalidException | UnsupportedOperationException e) {
				}
			}
		}

		if (!haveArt && srcArtworkList != null && destnTag != null) {
			for (final Artwork artwork : srcArtworkList) {
				if (srcArtworkList != null) {
					final byte[] byteArray = artwork == null ? new byte[0] : artwork.getBinaryData();
					if (byteArray.length > 0) {
						final Artwork newArtwork = new Artwork();
						newArtwork.setBinaryData(byteArray);
						try {
							destnTag.addField(newArtwork);
							haveArt = true;
							break;
						} catch (final FieldDataInvalidException e) {
						}
					}
				}
			}
		}

		final File srcDir = srcF.getParentFile();
		final String srcDirName = srcDir.getName();

		/**
		 * Add a jpeg file as artwork if we don't have one already; this is where we use ignoreJPegFName.
		 */
		if (destnTag != null && !haveArt) {
			final File jpegDir = _CopyJpgFiles ? dirInfo._destnDir : srcDir;
			final File[] allJPegFs = getSortedJPegFsArray(jpegDir);
			for (final File jPegF : allJPegFs) {
				if (ignoreJPegFName(jPegF.getName())) {
					continue;
				}
				try {
					destnTag.addField(Artwork.createArtworkFromFile(jPegF));
					haveArt = true;
					break;
				} catch (IOException | FieldDataInvalidException e) {
				}
			}
		}

		if (destnTag != null) {
			final String album = destnTag.getFirst(FieldKey.ALBUM);
			if (album == null || album.length() == 0) {
				try {
					destnTag.setField(FieldKey.ALBUM, srcDirName);
				} catch (KeyNotFoundException | FieldDataInvalidException e) {
				}
			}
			try {
				destnTag.setField(FieldKey.IS_COMPILATION, "Yes");
			} catch (KeyNotFoundException | FieldDataInvalidException e) {
			}
			destnAudioF.setTag(destnTag);
		}

		if (destnAudioF != null) {
			try {
				destnAudioF.commit();
				AudioFileIO.write(destnAudioF);
			} catch (final CannotWriteException e) {
			}
		}
	}

	@Override
	void doAfterSubdirsAreDone(final DirInfo dirInfo) {
	}

	@Override
	boolean isHealthyDir(final DirInfo dirInfo) {
		/**
		 * To be healthy, there cannot be anything left to convert, we cannot have multiple srcFNamess
		 * mapped to the same cnvrtdFNames, and destnDir must not be a file0.
		 */
		if (!dirInfo._needsCnvrtng.isEmpty()) {
			return false;
		}
		if (dirInfo._cnvrtdFName_to_srcFName.size() != dirInfo._srcFName_to_cnvrtdFName.size()) {
			return false;
		}
		final File destnDir = dirInfo._destnDir;
		return destnDir == null || !destnDir.exists();
	}

	@Override
	String getCautionString(final DirInfo dirInfo) {
		final String indentString = dirInfo._indentString;
		final StringBuilder cautionString = new StringBuilder();
		if (dirInfo._allAreBlocked) {
			cautionString.append(String.format("\n%s  Not Installing, just cleaning:", indentString));
		}
		for (final String fName : dirInfo._toDelete) {
			cautionString.append(String.format("\n%s  Deleting %s.", indentString, fName));
		}
		for (final Map.Entry<String, String[]> entry : dirInfo._cnvrtdFName_to_srcFName.entrySet()) {
			final String cnvrtdFName = entry.getKey();
			final String[] srcFNames = entry.getValue();
			if (srcFNames.length != 1) {
				cautionString.append(String.format("\n%s  CnvrtdFName[%s]: SrcFNames[", indentString, cnvrtdFName));
				boolean doneOne = false;
				for (final String srcFName : srcFNames) {
					final String commaString = doneOne ? "," : "";
					cautionString.append(String.format("%s%s", commaString, srcFName));
					doneOne = true;
				}
				cautionString.append("]");
			}
		}
		return cautionString.toString();
	}

	public static void main(final String[] args) {
		final String s = "  -33";
		final Pattern pattern = Pattern.compile("^\\s*\\-{0,1}\\d+\\.{0,1}\\d*");
		final Matcher matcher = pattern.matcher(s);
		if (matcher.find()) {
			final String ss = s.substring(0, matcher.end());
			System.out.print("|" + ss + "|");
		} else {
		}
	}
}
