package com.skagit.flashCardsGame.utils;

import java.io.File;

import com.skagit.utils.BasicDirsTracker;
import com.skagit.utils.StringUtilities;

public class FlashCardsGameDirsTracker extends BasicDirsTracker {

	final private static String _GameDirsName = "gameDirs";
	final private static String _CardsFilesDirName = "cardsFiles";
	final private static String _SoundFileDirsDirName = "soundFiles";

	final public static File _DataDir;
	final public static File _GameDirsDir;
	final public static File _CardsFilesDir;
	final public static File _SoundFilesDirsDir;

	static {
		final String dataDirPath = StringUtilities.getSystemProperty("Data.Dir", /* useSpaceProxy= */true);
		if (dataDirPath == null) {
			_DataDir = _UserDir;
		} else {
			_DataDir = new File(dataDirPath);
		}
		if (_DataDir != null && _DataDir.isDirectory()) {
			_GameDirsDir = new File(_DataDir, _GameDirsName);
			_CardsFilesDir = new File(_DataDir, _CardsFilesDirName);
			_SoundFilesDirsDir = new File(_DataDir, _SoundFileDirsDirName);
		} else {
			_GameDirsDir = _CardsFilesDir = _SoundFilesDirsDir = null;
		}
	}

	public static String getCriticalDirsString() {
		final StringBuilder s = new StringBuilder("Version 2024-09-04.A");
		s.append("\nUser Dir: ").append(StringUtilities.getCanonicalPath(_UserDir));
		s.append("\nDataDir: ").append(StringUtilities.getCanonicalPath(_DataDir));
		s.append("\nLogDir: ").append(StringUtilities.getCanonicalPath(_LogDir));
		s.append("\nCardsFilesDir: ").append(StringUtilities.getCanonicalPath(_CardsFilesDir));
		s.append("\nSoundFilesDirsDir: ").append(StringUtilities.getCanonicalPath(_SoundFilesDirsDir));
		s.append("\nGameDirsDir: ").append(StringUtilities.getCanonicalPath(_GameDirsDir));
		return s.toString();
	}

}
