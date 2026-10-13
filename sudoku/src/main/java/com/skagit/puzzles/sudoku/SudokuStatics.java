package com.skagit.puzzles.sudoku;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.TreeMap;

import com.skagit.utils.PermutationTools;

public class SudokuStatics {

	public static <T extends NamedEntity> T[] augmentArray(final T[] oldArray, final T newT) {
		final int idx = Arrays.binarySearch(oldArray, newT);
		if (idx >= 0) {
			return oldArray;
		}
		final int nOld = oldArray.length;
		@SuppressWarnings("rawtypes")
		final Class clazz = newT.getClass();
		@SuppressWarnings("unchecked")
		final T[] newArray = (T[]) Array.newInstance(clazz, nOld + 1);
		System.arraycopy(oldArray, 0, newArray, 0, nOld);
		newArray[nOld] = newT;
		Arrays.sort(newArray);
		return newArray;
	}

	public static String[] getLinesOfPrintout(final Variable[] vbles) {
		/**
		 * Get the offset we will use for the columns and rows, and the maximum rowLabel
		 * length.
		 */
		int rOffset = Integer.MAX_VALUE;
		int cOffset = Integer.MAX_VALUE;
		int maxC = Integer.MIN_VALUE;
		int maxNDigitsInRowNumber = Integer.MIN_VALUE;
		for (final Variable vble : vbles) {
			final int[] rc = vble.getRc();
			final int rawR = rc[0];
			rOffset = Math.min(rOffset, rawR);
			final int rawC = rc[1];
			cOffset = Math.min(cOffset, rawC);
			maxC = Math.max(maxC, rawC);
			final int nDigitsInRowNumber = rawR == 0 ? 1 : (int) (1L + Math.round(Math.floor(Math.log10(rawR + 0.5))));
			maxNDigitsInRowNumber = Math.max(maxNDigitsInRowNumber, nDigitsInRowNumber);
		}
		/** Do we have multi-digit domain values? */
		boolean haveMultiDigitDomainValue = false;
		for (final Variable variable : vbles) {
			final BitSet domain = variable._domain;
			for (int bit = domain.nextSetBit(0); bit >= 0; bit = domain.nextSetBit(bit + 1)) {
				if (bit >= 10) {
					haveMultiDigitDomainValue = true;
					break;
				}
			}
			if (haveMultiDigitDomainValue) {
				break;
			}
		}
		/**
		 * For each vble, get its string and for each column, the maxLength of its
		 * strings.
		 */
		final TreeMap<Variable, String> vbleStrings = new TreeMap<>();
		final int nClmns = maxC - cOffset + 1;
		final int[] maxClmnLengths = new int[nClmns];
		Arrays.fill(maxClmnLengths, 1);
		for (final Variable variable : vbles) {
			final StringBuilder sb = new StringBuilder();
			final BitSet possibles = variable.getPossibles();
			for (int bit = possibles.nextSetBit(1); bit >= 0; bit = possibles.nextSetBit(bit + 1)) {
				if (!sb.isEmpty() && haveMultiDigitDomainValue) {
					sb.append('.');
				}
				sb.append(bit);
			}
			if (sb.isEmpty()) {
				sb.append('\u2205');
			}
			final String vbleString = sb.toString();
			vbleStrings.put(variable, vbleString);
			final int len = vbleString.length();
			final int cAfterOffset = variable.getRc()[1] - cOffset;
			maxClmnLengths[cAfterOffset] = Math.max(maxClmnLengths[cAfterOffset], len);
		}
		/** Create the row label format and, for each column its format. */
		final String rowLabelFormat = String.format("%%%ds: ", maxNDigitsInRowNumber);
		final String[] formats = new String[nClmns];
		for (int rawC = cOffset; rawC <= maxC; ++rawC) {
			final int cAfterOffset = rawC - cOffset;
			int spacer = cAfterOffset > 0 ? 1 : 0;
			if (haveMultiDigitDomainValue) {
				++spacer;
			}
			formats[cAfterOffset] = String.format("%%%ds", maxClmnLengths[cAfterOffset] + spacer);
		}
		/** Sort the vbles. */
		final Variable[] vblesClone = new Variable[vbles.length];
		System.arraycopy(vbles, 0, vblesClone, 0, vbles.length);
		Arrays.sort(vblesClone, new Comparator<Variable>() {

			@Override
			public int compare(final Variable vble0, final Variable vble1) {
				final int[] rc0 = vble0.getRc(), rc1 = vble1.getRc();
				if (rc0[0] != rc1[0]) {
					return rc0[0] < rc1[0] ? -1 : 1;
				}
				if (rc0[1] != rc1[1]) {
					return rc0[1] < rc1[1] ? -1 : 1;
				}
				return 0;
			}
		});

		/** Compose the lines. */
		final ArrayList<String> lines = new ArrayList<>();
		final StringBuilder lineBuffer = new StringBuilder();
		int thisR_AfterOffset = -1;
		int nextC_AfterOffset = 0;
		for (final Variable vblel : vblesClone) {
			final int[] rc = vblel.getRc();
			final int rawR = rc[0];
			final int rawC = rc[1];
			final int rAfterOffset = rawR - rOffset;
			final int cAfterOffset = rawC - cOffset;
			if (rAfterOffset > thisR_AfterOffset) {
				/** Store the old lineBuffer. */
				if (lineBuffer.length() > 0) {
					lines.add(lineBuffer.toString());
				}
				/** Create a new lineBuffer. */
				lineBuffer.delete(0, lineBuffer.length());
				lineBuffer.append(String.format(rowLabelFormat, rawR));
				thisR_AfterOffset = rAfterOffset;
				nextC_AfterOffset = 0;
			}
			/** Print formatted blanks until we get to cAfterOffset. */
			for (; nextC_AfterOffset < cAfterOffset; ++nextC_AfterOffset) {
				lineBuffer.append(String.format(formats[nextC_AfterOffset], ""));
			}
			final String vbleString = vbleStrings.get(vblel);
			lineBuffer.append(String.format(formats[cAfterOffset], vbleString));
			++nextC_AfterOffset;
		}
		if (lineBuffer.length() > 0) {
			lines.add(lineBuffer.toString());
		}
		return lines.toArray(new String[lines.size()]);
	}

	static String getNumberSuffix(final int k0) {
		final int k = k0 % 100;
		final boolean isTeen = 10 < k && k < 20;
		switch (k % 10) {
		case 1:
			return isTeen ? "th" : "st";
		case 2:
			return isTeen ? "th" : "nd";
		case 3:
			return isTeen ? "th" : "rd";
		default:
			return "th";
		}
	}

	public static void dumpString(final PrintStream ps, final String s) {
		System.out.print(s);
		ps.print(s);
	}

	public static int[] getPrimeFactors(final int n) {
		final int sqrt = (int) Math.floor(Math.sqrt(n));
		final ArrayList<Integer> primeFactorList = new ArrayList<>();
		final BitSet primes = new BitSet(sqrt + 3);
		primes.set(2, sqrt + 2);
		for (int bit = 2; bit >= 0; bit = primes.nextSetBit(bit + 1)) {
			for (int k = 2 * bit; k <= sqrt; k += bit) {
				primes.clear(k);
			}
		}
		int n1 = n;
		for (int prime = primes.nextSetBit(2); prime >= 0; prime = primes.nextSetBit(prime + 1)) {
			while (n1 % prime == 0) {
				primeFactorList.add(prime);
				n1 /= prime;
			}
		}
		final int nFactors0 = primeFactorList.size();
		if (nFactors0 == 0) {
			primeFactorList.add(n);
		}
		final int nPrimeFactors = primeFactorList.size();
		final int[] primeFactors = new int[nPrimeFactors];
		for (int k = 0; k < nPrimeFactors; ++k) {
			primeFactors[k] = primeFactorList.get(k);
		}
		return primeFactors;
	}

	public static int[] getAllFactors(final int n) {
		final int[] primeFactors = getPrimeFactors(n);
		final int nPrimeFactors = primeFactors.length;
		final ArrayList<Integer> listOfFactors = new ArrayList<>();
		final int[] subset = new int[nPrimeFactors];
		Arrays.fill(subset, nPrimeFactors);
		while (PermutationTools.lexicographicalNextSet(subset) != 0) {
			int product = 1;
			for (int k = 0; k < nPrimeFactors; ++k) {
				final int kk = subset[k];
				if (kk < nPrimeFactors) {
					final int primeFactor = primeFactors[kk];
					product *= primeFactor;
				}
			}
			listOfFactors.add(product);
		}
		final int nFactors = listOfFactors.size();
		final int[] factors = new int[nFactors];
		for (int k = 0; k < nFactors; ++k) {
			factors[k] = listOfFactors.get(k);
		}
		Arrays.sort(factors);
		return factors;
	}

	/**
	 * <pre>
	 * Strings and ints are default shoved to the right.
	 *   OFFSET_T dash in front of the number shoves them both to the left.
	 * </pre>
	 */
	public static void main(final String[] args) {
		final int[] allFactors = getAllFactors(2025);
		for (int k = 0; k < allFactors.length; ++k) {
			System.out.println(allFactors[k]);
		}
		System.exit(33);
		final int[] primeFactors = getPrimeFactors(2025);
		final int nPrimeFactors = primeFactors.length;
		final int[] subset = new int[nPrimeFactors];
		Arrays.fill(subset, nPrimeFactors);
		while (PermutationTools.lexicographicalNextSet(subset) != 0) {
			final StringBuilder sb1 = new StringBuilder();
			final StringBuilder sb2 = new StringBuilder();
			int product = 1;
			for (int k = 0; k < nPrimeFactors; ++k) {
				final int kk = subset[k];
				if (kk < nPrimeFactors) {
					final int primeFactor = primeFactors[kk];
					product *= primeFactor;
					sb2.append(String.format("%s%d", k == 0 ? "{" : ",", primeFactor));
				}
				sb1.append(String.format("%s%d%s", k == 0 ? "\n{" : ",", subset[k], k == nPrimeFactors - 1 ? "}" : ""));
			}
			sb2.append(String.format("}: %d", product));
			System.out.print(sb1.toString());
			System.out.printf(": %s", sb2.toString());
		}
		System.exit(0);
		final String s = "";
		System.out.printf("|%5s|", s);
		final int k = 123;
		System.out.printf("\n|%-5d|", k);
	}
}
