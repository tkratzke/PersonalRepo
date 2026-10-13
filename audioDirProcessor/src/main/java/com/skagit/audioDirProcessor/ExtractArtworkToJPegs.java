package com.skagit.audioDirProcessor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.datatype.Artwork;

public class ExtractArtworkToJPegs extends AudioDirProcessor {

	public ExtractArtworkToJPegs() {
	}

	@Override
	String getLogFNameBase() {
		return "Extracting Artwork to JPegs";
	}

	@Override
	void doTheDirItself(final DirInfo dirInfo) {
		final File d = dirInfo._d;
		final TreeSet<String> allFNames = new TreeSet<>();
		allFNames.addAll(dirInfo._toDelete);
		allFNames.addAll(dirInfo._toCopy);
		allFNames.addAll(dirInfo._needsCnvrtng);
		allFNames.addAll(dirInfo._srcFName_to_cnvrtdFName.keySet());
		allFNames.addAll(dirInfo._srcFName_to_cnvrtdFName.values());
		allFNames.addAll(dirInfo._cnvrtdFName_to_srcFName.keySet());
		for (final String[] srcFNames : dirInfo._cnvrtdFName_to_srcFName.values()) {
			allFNames.addAll(Arrays.asList(srcFNames));
		}
		for (final String fName : allFNames) {
			final File srcF = new File(d, fName);
			AudioFile srcAudioF = null;
			Tag srcTag = null;
			List<Artwork> srcArtworkList = null;
			try {
				srcAudioF = AudioFileIO.read(srcF);
				srcTag = srcAudioF.getTag();
				srcArtworkList = srcTag.getArtworkList();
			} catch (IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException
					| CannotReadException e) {
				continue;
			}
			if (srcArtworkList == null) {
				continue;
			}
			for (final Artwork artwork : srcArtworkList) {
				final byte[] b = artwork == null ? null : artwork.getBinaryData();
				if (b != null && b.length > 0) {
					try (ByteArrayInputStream bis = new ByteArrayInputStream(b)) {
						final BufferedImage image = ImageIO.read(bis);
						if (image == null) {
							continue;
						}
						for (int k = 0, nWritten = 0; nWritten == 0; ++k) {
							final String artworkFName = String.format("Artwork from %s%02d.jpeg", fName, k);
							final File jpegF = new File(d, artworkFName);
							try {
								ImageIO.write(image, "jpeg", jpegF);
								++nWritten;
							} catch (final Exception e) {
								if (k >= 10) {
									break;
								}
							}
						}
					} catch (final IOException e) {
						continue;
					}
				}
			}
		}
	}

	@Override
	void doAfterSubdirsAreDone(final DirInfo dirInfo) {
	}

	@Override
	boolean isHealthyDir(final DirInfo dirInfo) {
		return true;
	}

	@Override
	String getJobString() {
		return "EXTRACTING ARTWORK TO JPEGS";
	}

	@Override
	String getCautionString(final DirInfo dirInfo) {
		return "";
	}
}
