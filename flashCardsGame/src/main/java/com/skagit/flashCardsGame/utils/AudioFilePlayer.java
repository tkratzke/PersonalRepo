package com.skagit.flashCardsGame.utils;

/**
 * <pre>
 * From: https://www.geeksforgeeks.org/play-audio-file-using-java/
 * Java class to play an Audio file using Clip Object.
 * </pre>
 * */

/** <pre> To make sure the clip gets played, look at:
 * https://stackoverflow.com/questions/557903/how-can-i-wait-for-a-java-sound-clip-to-finish-playing-back
 * We really don't use this website though.
 * </pre>
 */

/** <pre>
 * Use Audacity to convert iphone's voice memo's m4a files to aiff files as follows:
 * 1. Open Audacity
 * 2. Open the m4a file.
 * 3. Label the breaks
 * 3. File/Export Audio
 * 4. Select "other uncompressed files"
 * 5. Be sure AIFF is in the "Header" box.
 * 6. Use Multiple files and export at labels.
 * </pre>
 *
 * Introduction to lambda expressions:
 * https://www.w3schools.com/java/java_lambda.asp
 * */

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioFilePlayer {

    private final AudioInputStream _ais;
    private final Clip _clip;

    /**
     * <pre>
     * Another approach is given in the following link, but it didn't help
     * the initial lag problem.
     * https://www.baeldung.com/java-play-sound
     * </pre>
     */
    public AudioFilePlayer(final File f) {
	AudioInputStream ais = null;
	Clip clip = null;
	try {
	    ais = AudioSystem.getAudioInputStream(f.getAbsoluteFile());
	    clip = AudioSystem.getClip();
	} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
	}
	_ais = ais;
	_clip = clip;
    }

    private static boolean isValidAfp(final AudioFilePlayer afp) {
	return afp != null && afp._clip != null;
    }

    public static boolean isValidFile(final File f) {
	final AudioFilePlayer afp = new AudioFilePlayer(f);
	afp.close();
	return isValidAfp(afp);
    }

    public static void playFileIfValid(final File f) {
	final AudioFilePlayer afp = new AudioFilePlayer(f);
	if (isValidAfp(afp)) {
	    afp.playOnce();
	    afp.close();
	}
    }

    private void close() {
	if (_clip != null) {
	    try {
		_clip.close();
		_ais.close();
	    } catch (final IOException e) {
		e.printStackTrace();
	    }
	}
    }

    public void playOnce() {
	if (_clip == null) {
	    return;
	}
	final CountDownLatch countDownLatch = new CountDownLatch(1);
	_clip.addLineListener(event -> {
	    final LineEvent.Type eventType = event.getType();
	    if (eventType == LineEvent.Type.STOP) {
		countDownLatch.countDown();
	    }
	});
	try {
	    _clip.open(_ais);
	    _clip.start();
	    try {
		countDownLatch.await();
	    } catch (final InterruptedException e) {
	    }
	} catch (LineUnavailableException | IOException e) {
	}
	close();
    }

    public static void main(final String[] args) {
	final String filePath = "DataDir/soundFiles/Tran01-40/16-SouthVNHasNoWinter.aiff";
	final AudioFilePlayer afp = new AudioFilePlayer(new File(filePath));
	afp.playOnce();
    }

}
