package com.skagit.audioDirProcessor;

import java.io.File;
import java.util.TreeMap;
import java.util.TreeSet;

public class RestoreToBeforeConversion extends AudioDirProcessor {

	public RestoreToBeforeConversion() {
		super();
	}

	@Override
	String getJobString() {
		return "RESTORING to BEFORE CONVERSION";
	}

	@Override
	String getLogFNameBase() {
		return "Restore to Before Conversion";
	}

	@Override
	boolean isHealthyDir(final DirInfo dirInfo) {
		return true;
	}

	@Override
	void doTheDirItself(final DirInfo dirInfo) {
		final File d = dirInfo._d;
		final TreeSet<String> toDelete = dirInfo._toDelete;
		for (final String fName : toDelete) {
			new File(d, fName).delete();
		}
		final TreeMap<String, String> srcFName_to_cnvrtdFName = dirInfo._srcFName_to_cnvrtdFName;
		for (final String fName : srcFName_to_cnvrtdFName.values()) {
			new File(d, fName).delete();
		}
	}

	@Override
	void doAfterSubdirsAreDone(final DirInfo dirInfo) {
		final File destnDir = dirInfo._destnDir;
		if (destnDir != null && destnDir.isDirectory()) {
			if (destnDir.list().length == 0) {
				destnDir.delete();
			}
		}
	}

	@Override
	String getCautionString(final DirInfo dirInfo) {
		return "";
	}

}
