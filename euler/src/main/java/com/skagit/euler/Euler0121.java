package com.skagit.euler;

public class Euler0121 {

    static double getProb(int nReds, int nTurnsLeft, final int nBluesNeeded) {
	if (nBluesNeeded > nTurnsLeft) {
	    return 0d;
	}
	if (nBluesNeeded == 0) {
	    return 1d;
	}
	final double probBlue = 1d / (1d + nReds);
	++nReds;
	--nTurnsLeft;
	final double prob0 = probBlue * getProb(nReds, nTurnsLeft, nBluesNeeded - 1);
	final double prob1 = (1d - probBlue) * getProb(nReds, nTurnsLeft, nBluesNeeded);
	return prob0 + prob1;
    }

    public static void main(final String[] args) {
	final int nTurns = 15, nBluesNeeded = 8;
	final double prob = getProb(/* nRedsIn= */1, nTurns, nBluesNeeded);
	final int payout = (int) Math.floor(1d / prob);
	final double expectedGainByHouse = 1d - prob * payout;
	System.out.printf("nTurns[%d] nBluesNeeded[%d], prob[%f] payout[%d], expectedGainByHouse[%f]", //
		nTurns, nBluesNeeded, prob, payout, expectedGainByHouse);
    }
}
