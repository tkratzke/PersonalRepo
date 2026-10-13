package com.skagit.birdCalls;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.skagit.utils.CombinatoricTools;
import com.skagit.utils.PermutationTools;
import com.skagit.utils.StaticUtilities;

public class BirdCallsTestGenerator {

	final private static String _CallsDirName = "Calls";
	final private static String _NamesDirName = "Names";
	final private static int _Seed = 0;
	final private static int _NSeeds = 20;
	final private static Pattern _Pattern = Pattern.compile("\\p{Alpha}");
	final private static Comparator<File> _FileComparator = new Comparator<>() {

		@Override
		public int compare(final File f0, final File f1) {
			return f0.getName().compareTo(f1.getName());
		}
	};

	public static void main(final String[] args) {
		/** Read in the arguments. */
		final int nArgs = args.length;
		int iArg = 0;

		final String mainInDirPath = args[iArg++];
		final String mainOutDirPath = args[iArg++];
		final File mainInDir = new File(mainInDirPath);
		final File mainOutDirX = new File(mainOutDirPath);
		final File mainOutDir = mainOutDirX.isDirectory() ? mainOutDirX : mainInDir;
		final String extension;
		if (iArg >= nArgs) {
			extension = "mp3";
		} else {
			extension = args[iArg++];
		}

		/** Gather the files of calls and names, and pair them up. */
		final FileFilter fileFilter = new FileFilter() {

			@Override
			public boolean accept(final File f) {
				final String filePath = f.getAbsolutePath();
				return FilenameUtils.getExtension(filePath).equalsIgnoreCase(extension);
			}
		};
		final File callsDir = new File(mainInDir, _CallsDirName);
		final File[] callsFiles = callsDir.listFiles(fileFilter);
		final int nGlobal = callsFiles.length;
		if (nGlobal == 0) {
			System.exit(1);
		}
		final File namesDir = new File(mainInDir, _NamesDirName);
		final File[] namesFiles = namesDir.listFiles(fileFilter);
		final int nGlobalNameFiles = namesFiles.length;
		if (nGlobalNameFiles != nGlobal) {
			System.exit(2);
		}
		Arrays.parallelSort(callsFiles, _FileComparator);
		Arrays.parallelSort(namesFiles, _FileComparator);
		final File[][] globalPairs = new File[nGlobal][];
		for (int kGlobal = 0; kGlobal < nGlobal; ++kGlobal) {
			final File callFile = callsFiles[kGlobal];
			final File nameFile = namesFiles[kGlobal];
			if (!FilenameUtils.getBaseName(callFile.getName())
					.equalsIgnoreCase(FilenameUtils.getBaseName(callFile.getName()))) {
				System.exit(3);
			}
			globalPairs[kGlobal] = new File[] { callFile, nameFile };
		}

		/** Declare the variables that specify a test, and initialize them. */
		int coreSeed = _Seed % _NSeeds;
		boolean allowDuplicates = true;
		boolean spinThroughPracticeQuestions = false;
		boolean independentDraws = false;
		boolean shuffle = true;
		int nPerBirdInPractice = 3;
		BitSet mainSetBits = new BitSet(nGlobal);
		mainSetBits.set(0, nGlobal, true);

		/*
		 * Because System.in will be closed when we use try with resources as in the
		 * following, the scanner must be built only once, and the iTest loop must go
		 * inside the try with resources block.
		 */
		try (final Scanner sc = new Scanner(System.in)) {
			for (int iTest = 0;; ++iTest) {
				String s = null;
				final StringBuilder consoleTranscript = new StringBuilder().append(String.format("Test[%d]", iTest));
				if (iTest > 0) {
					System.out.print("\n\n");
				}
				s = String.format("Enter Seed (Return=[%d], Q=quit): ", coreSeed);
				System.out.print(s);
				consoleTranscript.append("\n").append(s);
				s = sc.nextLine();
				consoleTranscript.append(s);
				if (s.length() > 0 && s.toUpperCase().charAt(0) == 'Q') {
					break;
				}
				try {
					coreSeed = Math.abs(Integer.parseInt(s) % _NSeeds);
				} catch (final NumberFormatException e) {
				}
				final Random r = new Random(coreSeed);
				final long seed1 = Math.abs(r.nextLong());
				final long seed2 = Math.abs(r.nextLong());
				final long seed3 = Math.abs(r.nextLong());
				final long seed4 = Math.abs(r.nextLong());
				final long seed5 = Math.abs(r.nextLong());
				final long seed6 = Math.abs(r.nextLong());

				/** Get selectedBitSet. */
				String mainSetBitsString = CombinatoricTools.bitSetToString(mainSetBits);

				s = String.format("Enter BitSet String (Return=[%s], Rdd=\"Randomly choose dd.\"): ",
						mainSetBitsString);
				System.out.print(s);
				consoleTranscript.append("\n").append(s);
				mainSetBitsString = sc.nextLine().toUpperCase();
				s = mainSetBitsString;
				consoleTranscript.append(s);
				if (mainSetBitsString.length() > 0) {
					if (mainSetBitsString.charAt(0) == 'R') {
						try {
							final int nInMainSet = Integer.parseInt(mainSetBitsString.substring(1));
							final Random r1 = new Random(seed1);
							mainSetBits = CombinatoricTools
									.intArrayToBitSet(PermutationTools.randomKofN(nInMainSet, nGlobal, r1));
						} catch (final NumberFormatException e) {
						}
					} else {
						mainSetBits = CombinatoricTools.stringToBitset(mainSetBitsString, 0, nGlobal - 1);
					}
				}
				final int nInMainSet = mainSetBits.cardinality();
				if (nInMainSet == 0) {
					continue;
				}
				mainSetBitsString = CombinatoricTools.bitSetToString(mainSetBits, ',');

				/** Get nExtra. */
				final BitSet leftOutBits = (BitSet) mainSetBits.clone();
				leftOutBits.flip(0, nGlobal);
				final int nLeftOut = leftOutBits.cardinality();
				int nExtra0 = Math.min(3, nLeftOut);
				if (nLeftOut > 0) {
					s = String.format("Enter # extra Test Questions  (Return=[%d]): ", nExtra0);
					System.out.print(s);
					consoleTranscript.append("\n").append(s);
					s = sc.nextLine();
					consoleTranscript.append(s);
					if (s.length() > 0) {
						try {
							nExtra0 = Integer.parseInt(s);
							if (nExtra0 < 0 || nExtra0 > nLeftOut) {
								continue;
							}
						} catch (final NumberFormatException e) {
							continue;
						}
					}
				}
				final int nExtra = nExtra0;

				/** Get the number of normal questions. */
				int nNormalQuestions = 2 * nInMainSet;
				s = String.format("Enter Number of Normal Questions  (Return=[%d]): ", nNormalQuestions);
				System.out.print(s);
				consoleTranscript.append("\n").append(s);
				try {
					s = sc.nextLine();
					consoleTranscript.append(s);
					nNormalQuestions = Integer.parseInt(s);
					if (nNormalQuestions <= 0) {
						continue;
					}
				} catch (final NumberFormatException e) {
				}

				/** Get allowDuplicates. */
				if (nNormalQuestions > nInMainSet) {
					allowDuplicates = true;
				} else {
					s = String.format("Allow Duplicates? (Y=Yes, TWO_BY_THREE=No, Return=%s): ",
							allowDuplicates ? "Y" : "TWO_BY_THREE");
					System.out.print(s);
					consoleTranscript.append("\n").append(s);
					s = sc.nextLine().toUpperCase();
					consoleTranscript.append(s);
					allowDuplicates = s.length() == 0 ? allowDuplicates : (s.charAt(0) == 'Y');
				}

				/** Get nPerBirdInPractice. */
				s = String.format("Enter NPerBird in practice questions (Return=[%d]): ", nPerBirdInPractice);
				System.out.print(s);
				consoleTranscript.append("\n").append(s);
				try {
					s = sc.nextLine();
					consoleTranscript.append(s);
					nPerBirdInPractice = Integer.parseInt(s);
					if (nPerBirdInPractice <= 0) {
						continue;
					}
				} catch (final NumberFormatException e) {
				}

				/** Do we spin through practice questions? */
				s = String.format("Spin through practice qustions?  (Return=[%s]): ",
						spinThroughPracticeQuestions ? "Y" : "TWO_BY_THREE");
				System.out.print(s);
				consoleTranscript.append("\n").append(s);
				try {
					s = sc.nextLine();
					consoleTranscript.append(s);
					spinThroughPracticeQuestions = s.length() == 0 ? spinThroughPracticeQuestions
							: (s.charAt(0) == 'Y');
				} catch (final NumberFormatException e) {
				}

				/** Do we use independent draws or level? */
				s = String.format("Independent Draws? (Y=Yes, TWO_BY_THREE=No, Return=%s): ",
						independentDraws ? "Y" : "TWO_BY_THREE");
				System.out.print(s);
				consoleTranscript.append("\n").append(s);
				s = sc.nextLine().toUpperCase();
				consoleTranscript.append(s);
				independentDraws = s.length() == 0 ? independentDraws : (s.charAt(0) == 'Y');

				/** Do we shuffle or cycle? */
				s = String.format("Shuffle? (Y=Yes, TWO_BY_THREE=No, Return=%s): ", shuffle ? "Y" : "TWO_BY_THREE");
				System.out.print(s);
				consoleTranscript.append("\n").append(s);
				s = sc.nextLine().toUpperCase();
				consoleTranscript.append(s);
				shuffle = s.length() == 0 ? shuffle : (s.charAt(0) == 'Y');

				/** Gather indexes of normalQuestions. */
				int[] normalIndexes = new int[nNormalQuestions];
				final Random r2 = new Random(seed2);
				if (allowDuplicates) {
					if (independentDraws) {
						for (int k = 0; k < nNormalQuestions; ++k) {
							normalIndexes[k] = r2.nextInt(nInMainSet);
						}
					} else {
						final int[] permutation = PermutationTools.randomPermutation(nInMainSet, r2);
						for (int k = 0, k0 = 0; k < nNormalQuestions; ++k, k0 = ++k0 % nInMainSet) {
							normalIndexes[k] = permutation[k0];
						}
					}
				} else {
					normalIndexes = PermutationTools.randomKofN(nNormalQuestions, nInMainSet, r2);
				}

				/** Shuffle or cycle the indexes. */
				if (!shuffle) {
					final Random r3 = new Random(seed3);
					final int[][] counts = getCounts(normalIndexes);
					final int nNonZeros = counts.length;
					final int[] permutation = PermutationTools.randomPermutation(nNonZeros, r3);
					for (int k = 0, k0 = 0; k < nNormalQuestions; k0 = ++k0 % nInMainSet) {
						final int[] pair = counts[permutation[k0]];
						final int count = pair[1];
						if (count > 0) {
							normalIndexes[k++] = pair[0];
							--pair[1];
						}
					}
				}

				/** Final step for setting up normalQuestions. */
				final int[] idxToMain = CombinatoricTools.bitSetToIntArray(mainSetBits);
				final int[] normalQuestions = new int[nNormalQuestions];
				for (int k = 0; k < nNormalQuestions; ++k) {
					normalQuestions[k] = idxToMain[normalIndexes[k]];
				}

				/** AllDifferent up extraQuestions. */
				final int[] extraQuestions;
				if (nExtra > 0) {
					final int[] idxToLeftOut = CombinatoricTools.bitSetToIntArray(leftOutBits);
					final Random r4 = new Random(seed4);
					final int[] leftOutIndexes = PermutationTools.randomKofN(nExtra, nLeftOut, r4);
					PermutationTools.permute(leftOutIndexes, r4);
					extraQuestions = new int[nExtra];
					for (int k = 0; k < nExtra; ++k) {
						extraQuestions[k] = idxToLeftOut[leftOutIndexes[k]];
					}
				} else {
					extraQuestions = new int[] {};
				}

				final int nTestQuestions = nNormalQuestions + nExtra;
				final int[] testQuestions = new int[nTestQuestions];
				System.arraycopy(normalQuestions, 0, testQuestions, 0, nNormalQuestions);
				System.arraycopy(extraQuestions, 0, testQuestions, nNormalQuestions, nExtra);
				if (shuffle) {
					final Random r5 = new Random(seed5);
					PermutationTools.permute(testQuestions, r5);
				}

				/** If we shuffle, shuffle testQuestions. */
				if (shuffle) {
					final Random r5 = new Random(seed5);
					permuteAndAvoidConsecutives(testQuestions, r5);
				}

				/** AllDifferent up practice questions. */
				final int nPracticeQuestions = nPerBirdInPractice * nInMainSet;
				final int[] practiceQuestions = new int[nPracticeQuestions];
				final int[] permutation;
				if (shuffle) {
					final Random r6 = new Random(seed6);
					permutation = PermutationTools.randomPermutation(nInMainSet, r6);
				} else {
					permutation = CombinatoricTools.getInitial(nInMainSet);
				}
				for (int k = 0; k < nPracticeQuestions; ++k) {
					final int blockNumber = k / nPerBirdInPractice;
					final int ordinalInMain;
					if (spinThroughPracticeQuestions) {
						final int positionWithinBlock = k % nPerBirdInPractice;
						ordinalInMain = (blockNumber + positionWithinBlock) % nInMainSet;
					} else {
						ordinalInMain = blockNumber;
					}
					final int idx = permutation[ordinalInMain];
					practiceQuestions[k] = idxToMain[idx];
				}

				/** AllDifferent up allQuestions. */
				final int nAllQuestions = nPracticeQuestions + nTestQuestions;
				final int[] allQuestions = new int[nAllQuestions];
				System.arraycopy(practiceQuestions, 0, allQuestions, 0, nPracticeQuestions);
				System.arraycopy(testQuestions, 0, allQuestions, nPracticeQuestions, nTestQuestions);

				/** Create the name of the directory and make it. */
				final String outDirName = String.format( //
						"NPrac%d_NTest%d_NEx%d_Seed%d_%s_%s%s%s", //
						nPerBirdInPractice, nNormalQuestions, nExtra, //
						coreSeed, //
						mainSetBitsString, //
						nNormalQuestions > nInMainSet ? "" : (allowDuplicates ? "Dups" : "~Dups"), //
						independentDraws ? "Ind" : "Lvl", //
						shuffle ? "Shffl" : "Cycl");
				final File outDir = new File(mainOutDir, outDirName);

				/** Delete outDir. */
				try {
					FileUtils.forceDelete(outDir);
				} catch (final Exception e) {
				}
				if (outDir.exists()) {
					/** FileUtilsfailed. Try StaticUtilities. */
					System.err.printf("Failure on Local FileUtils.forceDelete(%s)", outDir.getAbsolutePath());
					final int errReturn = StaticUtilities.deleteAnyFileWithErrReturn(outDir);
					if (errReturn != 0) {
						System.err.printf("ErrReturn[%d] for StaticUtilities.deleteAnyFile(%s).", errReturn,
								outDir.getAbsolutePath());
						continue;
					}
				}

				/** Create outDir. */
				try {
					FileUtils.forceMkdir(outDir);
				} catch (final IOException e) {
				}
				if (!outDir.isDirectory()) {
					/** Local FileUtils failed. Try StaticUtilities. */
					System.err.printf("Failure on Local FileUtils.forceMkdir(%s).", outDir.getAbsolutePath());
					final File outDirX = StaticUtilities.makeDirectory(outDir);
					if (outDirX == null) {
						System.err.printf("Could not StaticUtilities.makeDirectory(%s)", outDir.getAbsolutePath());
						continue;
					}
				}

				/**
				 * Write a file that lists the console transcript, a histogram, and the
				 * questions.
				 */
				final File textFile = new File(outDir, outDir.getName() + ".txt");
				try (final PrintStream printStream = new PrintStream(textFile)) {
					/** Console Transcript: */
					printStream.print(consoleTranscript.toString());
					/** Histogram: */
					printStream.printf("\n\nHistogram:");
					final int[][] counts = getCounts(testQuestions);
					for (final int[] pair : counts) {
						try {
							final File callFile = callsFiles[pair[0]];
							final String baseName = FilenameUtils.getBaseName(callFile.getName());
							printStream.printf("\n%2d: %s", pair[1], baseName);
						} catch (final Exception e) {
						}
					}

					/** Questions. */
					printStream.printf("\n\nPractice questions:");
					for (int k = 0; k < nAllQuestions; ++k) {
						if (k == nPracticeQuestions) {
							printStream.printf("\n\nTest Questions:");
						}
						final File[] globalPair = globalPairs[allQuestions[k]];
						final File callFile = globalPair[0];
						String baseName = FilenameUtils.getBaseName(callFile.getName());
						final Matcher matcher = _Pattern.matcher(baseName);
						if (matcher.find()) {
							baseName = baseName.substring(matcher.start());
						}
						printStream.printf("\n%2d. %s", k, baseName);
					}
				} catch (final IOException e) {
				}

				/**
				 * Write the individual files for combining or, if mp3, combine them.
				 */
				final boolean concatenateFiles = "mp3".equalsIgnoreCase(extension);
				final File mergedFile;
				if (concatenateFiles) {
					mergedFile = concatenateFiles ? new File(outDir, outDir.getName() + "." + extension) : null;
					if ((mergedFile.exists() && !mergedFile.delete()) || !mergedFile.createNewFile()) {
						continue;
					}
				} else {
					mergedFile = null;
				}
				for (int k0 = 0, k1 = 0; k0 < nAllQuestions; ++k0, ++k1) {
					if (k0 == 0 || k0 == nPracticeQuestions) {
						final File f = new File(mainInDir, (k0 == 0 ? "Practice." : "Test.") + extension);
						if (f.exists()) {
							if (concatenateFiles) {
								concatenate(f, mergedFile);
							} else {
								FileUtils.copyFile(f, new File(outDir, String.format("%02d_", k1++, f.getName())));
							}
						}
					}
					final File[] globalPair = globalPairs[allQuestions[k0]];
					final File callFile = globalPair[0];
					final File nameFile = globalPair[1];
					try {
						String baseName = FilenameUtils.getBaseName(callFile.getName());
						final Matcher matcher = _Pattern.matcher(baseName);
						if (matcher.find()) {
							baseName = baseName.substring(matcher.start());
						}
						if (!concatenateFiles) {
							FileUtils.copyFile(callFile,
									new File(outDir, String.format("%02d_%s.%s", k1++, baseName, extension)));
							FileUtils.copyFile(nameFile,
									new File(outDir, String.format("%02d_%s.%s", k1++, baseName, extension)));
						} else {
							concatenate(callFile, mergedFile);
							concatenate(nameFile, mergedFile);
						}
					} catch (final IOException e) {
					}
				}
			}
		} catch (final Exception e) {
		}
	}

	/**
	 * a is assumed to be non-negative. Returns counts of those that have non-zero
	 * counts.
	 */
	private static int[][] getCounts(final int[] a) {
		int nNonZeros = 0;
		final TreeMap<Integer, int[]> counts = new TreeMap<>();
		final int length = a.length;
		for (int k = 0; k < length; ++k) {
			final int value = a[k];
			final int[] entry = counts.get(value);
			if (entry != null) {
				++entry[1];
			} else {
				counts.put(value, new int[] { value, 1 });
				++nNonZeros;
			}
		}
		final int[][] returnValue = counts.values().toArray(new int[nNonZeros][]);
		return returnValue;
	}

	/** This allows 2 in a row, but avoids 3 or more. */
	private static void permuteAndAvoidConsecutives(final int[] letters, final Random r) {
		final int n = letters.length;
		for (int i = 0; i < n - 1; ++i) {
			final int letterI = letters[i];
			/** Give up after trying twice. */
			for (int k = 0; k < 2; ++k) {
				final int j = r.nextInt(n - i);
				final int letterJ = letters[i + j];
				if (i < 2 || letterJ != letters[i - 1] || letterJ != letters[i - 2]) {
					letters[i] = letterJ;
					letters[i + j] = letterI;
					break;
				}
			}
		}
	}

	/** Appends mergedFile with f. */
	private static void concatenate(final File f, final File mergedFile) {
		final byte[] bytes = new byte[1024];
		int nRead;
		try (final FileInputStream in = new FileInputStream(f)) {
			try (final FileOutputStream out = new FileOutputStream(mergedFile, /* append= */true)) {
				while ((nRead = in.read(bytes)) != -1) {
					out.write(bytes, 0, nRead);
				}
			} catch (final IOException e) {
			}
		} catch (final IOException e) {
		}
	}

}
