package com.skagit.audioDirProcessor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;

/**
 * <pre>
 * To Install, do the following steps:
 * 1.   Copy the contents of music folder to Music-XX, except for Apple Music and (Ignore).
 * 2.   Run foobar2000 on Music-XX, asking it to skip files with name conflict.  It won't skip; it
 *      will simply add files.  The settings within foobar2000 are:
 *        a. convert to Apple AAC
 *        b. write back to source folder, and
 *        c. copy artwork and tags.
 * 2a.  For making mp3 files for Shokz, do NOT copy artwork and tags.
 * 3.   Rename Music-XX to Music-Converted.
 * 4.   Make a copy of Music-Converted and call it Music-Converted-XX.
 * 5.   Run this program with:
 *        a. RootDir set to "C:\\Users\\tkrat\\Music-Converted-XX"
 *        b. DestnRootDirString set to "(Ignore)/My m4a Files."
 *        c. CnvrtdExtension set to "m4a"
 *        d. the static TheAudioDirProcessor set to "new Install()"
 * 5a.  For making mp3 files for Shokz, call this My mp3 Files.
 * 6.   Copy "My m4a Files" into Music/(Ignore)
 * 6a.  For making mp3 files for Shokz, this is a non-step.  Simply copy (Ignore)/My Mp3 Files
 *      back to tkrat.
 * 7.   You might have to re-add "My m4a Files" within Apple Music.
 *
 * Notes:
 * 1. You can also eliminate boring FNames and/or titles by setting the singleton
 *    to "new EliminateBoringFNamesAndTitles()."
 * 2. You can find which names are going to be changed without changing them
 *    by setting the singleton to "new CheckForBoringFNamesAndTitles()."
 * 3. You can undo the conversion by foobar2000 by setting the singleton to
 *    new RestoreToBeforeConversion().  Not recommended.
 * </pre>
 */

public abstract class AudioDirProcessor {

	final private static File RootDir = new File("C:\\Users\\tkrat\\Music-Converted-XX");
	final private static String DestnRootDirString = "(Ignore)\\My M4a Files";
	final private static String CnvrtdExtension = "m4a";
	final private static AudioDirProcessor TheAudioDirProcessor = new Install();

	final static boolean _CopyJpgFiles = true;
	final private static String[] AdditionalDirNamesToIgnore = { "(Ignore)", "Apple Music" };
	final private static String[] AudioExtensionsArray = { "m4a", "mp3", "mp4", "wav", "wma", "wpa", "flac" };
	final private static String[] JPegExtensionsArray = { "jpeg", "jpg" };

	final private static String[] JPegFNamesToIgnore = { "Northwest Tugboat Tales Cover.jpg" };
	final private static String[] DirNamesToIgnore;

	final private static TreeSet<String> AudioExtensions;
	final private static TreeSet<String> ToCnvrtExtensions;
	final private static TreeSet<String> JPegExtensions;

	final static Comparator<String> UniversalComparator = String.CASE_INSENSITIVE_ORDER;

	static {
		AudioExtensions = new TreeSet<>(UniversalComparator);
		AudioExtensions.addAll(Arrays.asList(AudioExtensionsArray));
		ToCnvrtExtensions = new TreeSet<>(UniversalComparator);
		ToCnvrtExtensions.addAll(AudioExtensions);
		ToCnvrtExtensions.remove(CnvrtdExtension);
		JPegExtensions = new TreeSet<>(UniversalComparator);
		JPegExtensions.addAll(Arrays.asList(JPegExtensionsArray));
		final int nAdditional = AdditionalDirNamesToIgnore.length;
		DirNamesToIgnore = new String[nAdditional + 1];
		System.arraycopy(AdditionalDirNamesToIgnore, 0, DirNamesToIgnore, 0, nAdditional);
		final Pattern pattern = Pattern.compile("^[^/\\\\]*");
		final Matcher matcher = pattern.matcher(DestnRootDirString);
		if (matcher.find()) {
			DirNamesToIgnore[nAdditional] = DestnRootDirString.substring(0, matcher.end());
		} else {
			DirNamesToIgnore[nAdditional] = DestnRootDirString;
		}
	}

	public static void main(final String[] args) {
		TheAudioDirProcessor.doAll();
	}

	/** The rest are implementation details. */

	final private static String GoodVerdictString = "";
	final private static String BadVerdictString = "*";

	/** Disable the jaudioTagger logging. */
	static {
		final Logger[] loggers = { Logger.getLogger("org.jaudiotagger") };
		for (final Logger logger : loggers) {
			logger.setLevel(java.util.logging.Level.OFF);
		}
	}

	private PrintStream _ps = null;
	final private File _rootDir;
	final private File _destnRootDir;

	protected AudioDirProcessor() {
		_rootDir = RootDir;
		_destnRootDir = new File(_rootDir, DestnRootDirString);
	}

	final private void doAll() {
		final File logFile = new File(_rootDir, getLogFNameBase() + ".log");
		try (PrintStream ps = new PrintStream(new FileOutputStream(logFile, /* appendToLogFile= */false))) {
			_ps = ps;
			doSelfAndSubdirs(_rootDir, /* level= */0);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	final void dumpString(final String sToDump, final Exception e) {
		if (e != null) {
			_ps.printf("\nRan into %s while\n%s", e.toString(), sToDump);
			System.err.printf("\nRan into %s while\n%s", e.toString(), sToDump);
			e.printStackTrace(_ps);
			e.printStackTrace();
		} else {
			_ps.print(sToDump);
			System.out.print(sToDump);
		}
	}

	/** Static utils: */
	private static String findRelativePath(final Path rootDirPath, final String targetPath0) {
		final Path targetPath = Paths.get(targetPath0).normalize();
		final Path relativePath = rootDirPath.relativize(targetPath);
		return relativePath.toString();
	}

	protected static String getMatchingAudioExtension(final String fName) {
		final String thisExtension = FilenameUtils.getExtension(fName);
		final String incumbent = AudioExtensions.ceiling(thisExtension);
		if (incumbent == null) {
			return null;
		}
		return AudioExtensions.comparator().compare(thisExtension, incumbent) == 0 ? incumbent : null;
	}

	static String getIndentString(final int level) {
		final char[] buffer = new char[level];
		Arrays.fill(buffer, '\t');
		return new String(buffer);
	}

	final static boolean ignoreDirectory(final File d) {
		final String dName = d.getName();
		for (final String dirNameToIgnore : DirNamesToIgnore) {
			if (dName.equalsIgnoreCase(dirNameToIgnore)) {
				return true;
			}
		}
		return false;
	}

	final void doSelfAndSubdirs(final File d, final int level) {
		final String dName = d.getName();
		for (final String dirNameToIgnore : DirNamesToIgnore) {
			if (dName.equalsIgnoreCase(dirNameToIgnore)) {
				return;
			}
		}
		final DirInfo dirInfo = new DirInfo(d, level);
		processDir(dirInfo);
		final File[] subDirs = d.listFiles(new FileFilter() {

			@Override
			public boolean accept(final File f) {
				return f.isDirectory();
			}
		});
		for (final File subDir : subDirs) {
			doSelfAndSubdirs(subDir, level + 1);
		}
		doAfterSubdirsAreDone(dirInfo);
	}

	final class DirInfo {

		final File _d;
		final File _destnDir;
		final int _level;
		final String _indentString;

		final TreeSet<String> _toCopy;
		final TreeSet<String> _toDelete;
		final TreeSet<String> _needsCnvrtng;
		final TreeMap<String, String> _srcFName_to_cnvrtdFName;
		final TreeMap<String, String[]> _cnvrtdFName_to_srcFName;
		final TreeSet<String> _jPegFNames;

		final boolean _allAreBlocked;

		final String _introString;

		private DirInfo(final File d, final int level) {
			_destnDir = getDestnDir(_rootDir, _destnRootDir, d);
			_level = level;
			_indentString = getIndentString(level);
			/** Create an introductory string for the directory d. */
			String introString = level == 0 ? "" : ("\n" + _indentString);
			if (level == 0) {
				introString += String.format("Root[%s] %s", _rootDir, getJobString());
				introString += String.format(", DestnRoot[%s]", _destnDir);
				for (int k = 0; k < DirNamesToIgnore.length; ++k) {
					final String dirNameToIgnore = DirNamesToIgnore[k];
					introString += (k == 0 ? ", Ignoring[" : ",") + dirNameToIgnore;
				}
				introString += "]: ";
			}
			if (level > 0) {
				introString += d.getName();
			}

			_d = d;

			_toCopy = new TreeSet<>(UniversalComparator);
			_toDelete = new TreeSet<>(UniversalComparator);
			_srcFName_to_cnvrtdFName = new TreeMap<>(UniversalComparator);
			_cnvrtdFName_to_srcFName = new TreeMap<>();
			_needsCnvrtng = new TreeSet<>(UniversalComparator);
			_jPegFNames = getAllJPegFNames(d);

			final String[] audioFNames = getAllAudioFNames(d);
			if (audioFNames.length == 0) {
				_introString = introString + ":: NOT AN AUDIO DIR";
				_allAreBlocked = true;
				return;
			}

			/**
			 * Initially, every audioFile f either has a cnvrtd extension, in which case it must be copied, or
			 * else it must be cnvrtd. We will reduce these sets for consideration in the next two steps.
			 */
			for (final String fName : audioFNames) {
				if (getMatchingCnvrtdExtension(fName) != null) {
					_toCopy.add(fName);
				} else {
					_needsCnvrtng.add(fName);
				}
			}
			/**
			 * Find the files created to avoid overwriting something, and mark them for deletion and remove them
			 * from _toCopy and _needsCnvrtng.
			 */
			for (final String fName : audioFNames) {
				final String origFName = getOrigFName(fName);
				if (origFName != null) {
					final File origF = new File(d, origFName);
					if (origF.exists()) {
						_toDelete.add(fName);
						_toCopy.remove(fName);
						_needsCnvrtng.remove(fName);
					}
				}
			}

			/**
			 * <pre>
			 * If fName is in _needsCnvrtng, one of the following two situations exists:
			 * 1. fName has a "cnvrtd partner" cnvrtdFName.
			 *    a. cnvrtdFName does not need copying, only moving.
			 *    b. fName does not need to be cnvrtd.
			 *    c. Map fName to cnvrtdFName.
			 * 2. fName has no cnvrtd partner.
			 *    a. Leave f in _needsCnvrtng.
			 * </pre>
			 */
			final ArrayList<String> removeFromNeedsCnvrtng = new ArrayList<>();
			for (final String srcFName : _needsCnvrtng) {
				final String cnvrtdFName = getCnvrtdFName(srcFName);
				final File cnvrtdF = new File(d, cnvrtdFName);
				if (cnvrtdF.exists()) {
					_toCopy.remove(cnvrtdFName);
					removeFromNeedsCnvrtng.add(srcFName);
					_srcFName_to_cnvrtdFName.put(srcFName, cnvrtdFName);
					final String[] oldArray = _cnvrtdFName_to_srcFName.put(cnvrtdFName, new String[] { srcFName });
					if (oldArray != null) {
						final int nOld = oldArray.length;
						final String[] newArray = new String[nOld + 1];
						System.arraycopy(oldArray, 0, newArray, 0, nOld);
						newArray[nOld] = srcFName;
						_cnvrtdFName_to_srcFName.put(cnvrtdFName, newArray);
					}
				}
			}
			_needsCnvrtng.removeAll(removeFromNeedsCnvrtng);

			final File[] destnFiles0 = _destnDir.listFiles(new FileFilter() {

				@Override
				public boolean accept(final File f) {
					return f.isFile();
				}
			});
			final File[] destnFiles = destnFiles0 == null ? new File[0] : destnFiles0;
			final TreeSet<String> destnFNames = new TreeSet<>(UniversalComparator);
			for (final File f : destnFiles) {
				destnFNames.add(f.getName());
			}
			boolean allAreBlocked = true;
			for (int iPass = 0; iPass < 3 && allAreBlocked; ++iPass) {
				final Collection<String> toCheck;
				if (iPass == 0) {
					toCheck = _toCopy;
				} else if (iPass == 1) {
					toCheck = _cnvrtdFName_to_srcFName.keySet();
				} else {
					toCheck = _jPegFNames;
				}
				for (final String fName : toCheck) {
					if (!destnFNames.contains(fName)) {
						allAreBlocked = false;
						break;
					}
				}
			}
			_allAreBlocked = allAreBlocked;
			_introString = introString + (_allAreBlocked ? ": ALL ARE BLOCKED" : "");
		}

		boolean isRootDir() {
			return _d.equals(_rootDir);
		}
	}

	final void processDir(final DirInfo dirInfo) {
		final TreeSet<String> toDelete = dirInfo._toDelete;
		final TreeSet<String> toCopy = dirInfo._toCopy;
		final TreeMap<String, String> srcFName_to_cnvrtdFName = dirInfo._srcFName_to_cnvrtdFName;
		final TreeSet<String> needsCnvrtng = dirInfo._needsCnvrtng;
		final int nToDelete = toDelete.size();
		final int nToMove = srcFName_to_cnvrtdFName.size();
		final int nToCopy = toCopy.size();
		final int nNeedsCnvrtng = needsCnvrtng.size();
		final int nAudioFiles = nToDelete + 2 * nToMove + nToCopy + nNeedsCnvrtng;
		final String introString = dirInfo._introString;

		if (nAudioFiles == 0) {
			dumpString(introString, /* e= */null);
			return;
		}

		final boolean isHealthyDir = isHealthyDir(dirInfo);
		final String summaryString0 = String.format("n[Delete/Move/Copy/NeedsCnvrtng]=[%d/%d/%d/%d]%s", nToDelete,
				nToMove, nToCopy, nNeedsCnvrtng, isHealthyDir ? GoodVerdictString : BadVerdictString);
		final String cautionString = getCautionString(dirInfo);
		final String failureString = isHealthyDir ? "" : String.format(" FAILED %s", getJobString());
		final String sToDump = String.format("%s%s:: %s%s.", introString, failureString, summaryString0, cautionString);
		if (isHealthyDir) {
			doTheDirItself(dirInfo);
		}
		dumpString(sToDump, /* e= */null);
	}

	final static String[] getAllAudioFNames(final File d) {
		if (!d.isDirectory()) {
			return new String[0];
		}
		final File[] audioFs = d.listFiles(new FileFilter() {

			@Override
			public boolean accept(final File f) {
				if (!f.isFile()) {
					return false;
				}
				final String fName = f.getName();
				final String matchingAudioExtension = getMatchingAudioExtension(fName);
				return matchingAudioExtension != null;
			}
		});
		final int nAudioFs = audioFs == null ? 0 : audioFs.length;
		final String[] fNames = new String[nAudioFs];
		for (int k = 0; k < nAudioFs; ++k) {
			fNames[k] = audioFs[k].getName();
		}
		return fNames;
	}

	final static TreeSet<String> getAllJPegFNames(final File d) {
		final File[] jPegFs = d.listFiles(new FileFilter() {

			@Override
			public boolean accept(final File f) {
				return f.isFile() && (getMatchingJPegExtension(f.getName()) != null);
			}
		});
		final int nJPegFiles = jPegFs == null ? 0 : jPegFs.length;
		final TreeSet<String> jPegFNames = new TreeSet<>(UniversalComparator);
		for (int k = 0; k < nJPegFiles; ++k) {
			jPegFNames.add(jPegFs[k].getName());
		}
		return jPegFNames;
	}

	final static File[] getSortedJPegFsArray(final File d) {
		final File[] jPegFs = d.listFiles(new FileFilter() {

			@Override
			public boolean accept(final File f) {
				return f.isFile() && (getMatchingJPegExtension(f.getName()) != null);
			}
		});
		Arrays.sort(jPegFs, new Comparator<File>() {

			@Override
			public int compare(final File f0, final File f1) {
				final long len0 = f0.length();
				final long len1 = f1.length();
				if (len0 != len1) {
					return len0 > len1 ? -1 : 1;
				}
				return UniversalComparator.compare(f0.getName(), f1.getName());
			}
		});
		return jPegFs;
	}

	final static String getMatchingToCnvrtExtension(final String fName) {
		final String thisExtension = FilenameUtils.getExtension(fName);
		final String incumbent = ToCnvrtExtensions.ceiling(thisExtension);
		if (incumbent == null) {
			return null;
		}
		return ToCnvrtExtensions.comparator().compare(thisExtension, incumbent) == 0 ? incumbent : null;
	}

	final static String getMatchingCnvrtdExtension(final String fName) {
		final String thisExtension = FilenameUtils.getExtension(fName);
		return ToCnvrtExtensions.comparator().compare(thisExtension, CnvrtdExtension) == 0 ? CnvrtdExtension : null;
	}

	final static String getMatchingJPegExtension(final String fName) {
		final String thisExtension = FilenameUtils.getExtension(fName);
		final String incumbent = JPegExtensions.ceiling(thisExtension);
		if (incumbent == null) {
			return null;
		}
		return JPegExtensions.comparator().compare(thisExtension, incumbent) == 0 ? incumbent : null;
	}

	final static String getCnvrtdFName(final String fName) {
		return FilenameUtils.getBaseName(fName) + "." + CnvrtdExtension;
	}

	final static byte[] jPegToByteArray(final File imageFile) {
		if (!imageFile.exists()) {
			return null;
		}
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
			final BufferedImage bufferedImage = ImageIO.read(imageFile);
			ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
			return byteArrayOutputStream.toByteArray();
		} catch (final IOException | IllegalArgumentException e) {
		}
		return null;
	}

	final static boolean ignoreJPegFName(final String jPegFName) {
		for (final String fName : JPegFNamesToIgnore) {
			if (UniversalComparator.compare(jPegFName, fName) == 0) {
				return true;
			}
		}
		return false;
	}

	private static File getDestnDir(final File rootDir, final File destnRootDir, final File d) {
		final String relativePath = findRelativePath(rootDir.toPath(), d.getPath());
		return new File(destnRootDir, relativePath);
	}

	/**
	 * <pre>
	 * Matches:
	 * single white character
	 * groups of "(digits)" (with at least one)
	 * dot
	 * not-a-dots (with at least one)
	 * end of string.
	 * </pre>
	 */
	final private static Pattern MyPattern0 = Pattern.compile("\\s(\\(\\d+\\))+\\.[^\\.]+$");
	/**
	 * <pre>
	 * Matches:
	 * dot
	 * not-a-dots (with at least one)
	 * end of string.
	 * </pre>
	 */
	final private static Pattern MyPattern1 = Pattern.compile("\\.[^\\.]+$");

	final static String getOrigFName(final String fName) {
		final Matcher matcher0 = MyPattern0.matcher(fName);
		if (matcher0.find()) {
			final Matcher matcher1 = MyPattern1.matcher(fName);
			matcher1.find();
			return fName.substring(0, matcher0.start()) + fName.substring(matcher1.start());
		}
		return null;
	}

	/** The abstract methods: */
	abstract String getLogFNameBase();

	abstract void doTheDirItself(final DirInfo dirInfo);

	abstract void doAfterSubdirsAreDone(final DirInfo dirInfo);

	abstract boolean isHealthyDir(final DirInfo dirInfo);

	abstract String getJobString();

	abstract String getCautionString(final DirInfo dirInfo);
}
