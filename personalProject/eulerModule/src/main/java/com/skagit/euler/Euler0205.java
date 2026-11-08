package com.skagit.euler;

public class Euler0205 {

    private static int getCount(final int targetSum, final int maxOnDie, final int nDice) {
	final int maxRollPossible = nDice * maxOnDie;
	if (targetSum == nDice || targetSum == maxRollPossible) {
	    return 1;
	}
	if ((targetSum < nDice) || (targetSum > maxRollPossible)) {
	    return 0;
	}
	final int minUsesOfMaxOnDie, maxUsesOfMaxOnDie;
	if (maxOnDie == 1) {
	    minUsesOfMaxOnDie = maxUsesOfMaxOnDie = nDice;
	} else {
	    final int maxMinus1 = maxOnDie - 1;
	    minUsesOfMaxOnDie = Math.max(0, targetSum - nDice * maxMinus1);
	    maxUsesOfMaxOnDie = (targetSum - nDice) / maxMinus1;
	}
	int cum = 0;
	for (int nUsesOfMaxOnDie = minUsesOfMaxOnDie; nUsesOfMaxOnDie <= maxUsesOfMaxOnDie; ++nUsesOfMaxOnDie) {
	    final int count = getCount(//
		    targetSum - nUsesOfMaxOnDie * maxOnDie, maxOnDie - 1, nDice - nUsesOfMaxOnDie);
	    final int nChooseK = getNChooseK(nDice, nUsesOfMaxOnDie);
	    cum += nChooseK * count;
	}
	return cum;
    }

    private static int[] getCumCounts(final int maxOnDie, final int nDice) {
	final int maxValue = maxOnDie * nDice;
	final int[] cumCounts = new int[maxValue + 1];
	for (int targetSum = nDice; targetSum <= maxValue; ++targetSum) {
	    if (targetSum < nDice) {
		cumCounts[targetSum] = 0;
		continue;
	    }
	    final int count = getCount(targetSum, maxOnDie, nDice);
	    cumCounts[targetSum] = count + cumCounts[targetSum - 1];
	}
	return cumCounts;
    }

    private static int getNChooseK(final int n, final int k) {
	if (n < k || n < 0 || k < 0) {
	    return 0;
	}
	if (k == 0) {
	    return 1;
	}
	return getNChooseK(n - 1, k - 1) + getNChooseK(n - 1, k);
    }

    public static double getProb(final int maxOnDieA, final int nDiceA, final int maxOnDieB, final int nDiceB) {
	final int[] cumCountsA = getCumCounts(maxOnDieA, nDiceA);
	final int aLength = cumCountsA.length;
	final double denomA = cumCountsA[aLength - 1];
	final int[] cumCountsB = getCumCounts(maxOnDieB, nDiceB);
	final int bLength = cumCountsB.length;
	final double denomB = cumCountsB[bLength - 1];

	double cumProb = 0d;
	for (int k = nDiceA; k <= nDiceA * maxOnDieA; ++k) {
	    final double probA = (cumCountsA[k] - cumCountsA[k - 1]) / denomA;
	    final int cumB = cumCountsB[Math.min(k - 1, bLength - 1)];
	    final double probB = cumB / denomB;
	    cumProb += probA * probB;
	}
	return cumProb;
    }

    public static void main(final String[] args) {
	final int maxOnDieA = 4, nDiceA = 9;
	final int maxOnDieB = 6, nDiceB = 6;
	final double prob = getProb(maxOnDieA, nDiceA, maxOnDieB, nDiceB);
	System.out.printf("%.7f", prob);
    }
}
