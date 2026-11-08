package com.skagit.euler;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.stream.IntStream;

public class Euler0122 {

    /** Constants used in debug printouts: */
    final private static int _SmallNMults = 5;
    final private static int _BigNMults = 9;
    final private static int _SmallNUNknowns = 20;

    public static void main(final String[] args) {
	final int maxPower = 200;

	/** Initialize the answer vector and which ones we have answers for. */
	final int[] m = new int[maxPower + 1];
	Arrays.fill(m, 0);
	final BitSet completed = new BitSet(maxPower + 1);
	completed.set(1);

	/** AllDifferent the initial sets to be just { {1} }. */
	HashSet<BitSet> sets = new HashSet<BitSet>();
	final BitSet initialSet = new BitSet(2);
	initialSet.set(1);
	sets.add(initialSet);

	/**
	 * The highest power, and therefore, the largest set we'll encounter, could have
	 * maxPower-1 multiplications.
	 */
	final int[] setCounts = new int[maxPower];
	Arrays.fill(setCounts, 0);
	setCounts[0] = 1;

	NMULTS_LOOP: for (int nMults = 1; nMults < maxPower; ++nMults) {
	    /** Print details for the small values of nMults. */
	    if (nMults <= _SmallNMults) {
		int setNumber = 1;
		for (final BitSet newSet : sets) {
		    if (nMults == 1 && setNumber == 1) {
		    } else {
			System.out.printf((nMults > 1 && setNumber == 1) ? "\n\n" : "\n");
		    }
		    System.out.printf("%d mults, set# %d: %s", nMults - 1, setNumber++, newSet.toString());
		}
	    } else if (nMults == _SmallNMults + 1) {
		System.out.println("\n\nPress Enter key to continue...");
		try {
		    System.in.read();
		} catch (final Exception e) {
		}
	    }
	    final HashSet<BitSet> newSets = new HashSet<BitSet>();
	    /**
	     * For each set thisSet, compute the new powers that would result from an
	     * additional multiplication. For each new power newPower, add newPower to set
	     * to form newSet and store newSet in newSets.
	     */
	    for (final BitSet thisSet : sets) {
		for (int k1 = thisSet.nextSetBit(1); k1 >= 0; k1 = thisSet.nextSetBit(k1 + 1)) {
		    for (int k2 = thisSet.nextSetBit(1); k2 >= 0; k2 = thisSet.nextSetBit(k2 + 1)) {
			final int newPower = k1 + k2;
			if (newPower > maxPower) {
			    /** No additional values of k2 are of interest. */
			    break;
			}
			if (thisSet.get(newPower)) {
			    /** newPower would not add anything to the set. */
			    continue;
			}
			if (!completed.get(newPower)) {
			    /** We have a new value for which we know m. */
			    m[newPower] = nMults;
			    completed.set(newPower);
			    final int nToGo = maxPower - completed.cardinality();
			    if (nMults > _BigNMults) {
				if (nToGo > _SmallNUNknowns) {
				    System.out.printf("\nm(%03d) = %d, %d new sets, %d unknown m-values.", newPower,
					    nMults, newSets.size(), nToGo);
				} else {
				    System.out.printf("\nm(%03d) = %d, %d new sets, Unknowns: ", newPower, nMults,
					    newSets.size());
				}
			    }
			    /** If almost done, list the ones that still need discovering. */
			    if (nToGo <= _SmallNUNknowns) {
				final BitSet toGo = (BitSet) completed.clone();
				toGo.flip(0, maxPower + 1);
				toGo.set(0, false);
				System.out.printf("  %s", toGo.toString());
			    }
			    if (completed.nextClearBit(2) == maxPower + 1) {
				setCounts[nMults] = newSets.size();
				break NMULTS_LOOP;
			    }
			}
			final BitSet newSet = (BitSet) thisSet.clone();
			newSet.set(newPower);
			newSets.add(newSet);
		    }
		}
	    }
	    sets = newSets;
	    setCounts[nMults] = sets.size();
	}

	/** Print out the m values. */
	String s = "";
	for (int k = 0; k < maxPower && setCounts[k] > 0; ++k) {
	    s += String.format("%sAfter %d mults, we have %d sets.", k == 0 ? "\n\n" : "\n", k, setCounts[k]);
	}

	for (int k = 1; k <= maxPower; ++k) {
	    s += String.format("%sm(%03d) = %d", k % 5 == 1 ? "\n\n" : "\n", k, m[k]);
	}
	final int sum = IntStream.of(m).sum();
	s += String.format("\n\nsum[%d]", sum);
	try (PrintStream ps = new PrintStream(new File("out.txt"))) {
	    ps.printf("%s", s);
	} catch (final IOException e) {
	}
	System.out.printf("%s", s);
    }

}
