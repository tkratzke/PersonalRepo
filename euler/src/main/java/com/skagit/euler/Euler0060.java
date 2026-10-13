package com.skagit.euler;

import java.time.Duration;
import java.util.Arrays;

import com.skagit.euler.utils.Eratosthenes;
import com.skagit.utils.CombinatoricTools;

public class Euler0060 {

	private static int _InitialEndOfBox = 63;
	private static int _UnsetWinningSum = Integer.MAX_VALUE;

	final public int _vectorLength;
	final public int[] _currentVector;
	final public int[] _winningVector;
	public int _winningSum;

	public int _endOfBox;

	public Euler0060(final int vectorLength) {
		_vectorLength = vectorLength;
		_currentVector = new int[vectorLength];
		_winningVector = new int[vectorLength];
		Arrays.fill(_currentVector, 0);
		Arrays.fill(_winningVector, 0);
		_winningSum = _UnsetWinningSum;
		_endOfBox = _InitialEndOfBox;

		for (int iTry = 0;; ++iTry) {
			completeWinningPrimes(/* nSet= */0, /* currentSum= */0);
			System.out.printf("%sTry #[%d], endOfBox[%d]", iTry > 0 ? "\n" : "", iTry, _endOfBox);
			if (_winningSum != _UnsetWinningSum) {
				System.out.printf(".  Success 1: winningSum[%d]", _winningSum);
				if (_winningSum <= _endOfBox) {
					System.out.printf(" ...Complete Success!");
					return;
				}
			} else {
				System.out.printf(".  Fail.");
			}
			extendBox();
		}
	}

	private void extendBox() {
		final int newEndOfBox = _winningSum == _UnsetWinningSum ? (2 * _endOfBox + 1) : _winningSum;
		if (newEndOfBox <= _endOfBox) {
			return;
		}
		_endOfBox = newEndOfBox;
	}

	private void completeWinningPrimes(final int nSet, final int currentSum) {
		final int lastPrime = nSet > 0 ? _currentVector[nSet - 1] : 1;
		final int nLeft = _vectorLength - nSet;
		for (int p = Eratosthenes.nextPrimeAfter(lastPrime); 0 <= p
				&& p <= _endOfBox; p = Eratosthenes.nextPrimeAfter(p)) {
			if (nSet == 0 && (p == 2 || p == 5)) {
				continue;
			}
			if (isCompatible(p)) {
				_currentVector[nSet] = p;
				final int newSum = currentSum + p;
				if (nSet + 1 == _vectorLength) {
					/** We have an answer. Is it better than what we had? */
					if (_winningSum == _UnsetWinningSum || newSum < _winningSum) {
						_winningSum = newSum;
						System.arraycopy(_currentVector, 0, _winningVector, 0, _vectorLength);
					}
				}
				completeWinningPrimes(nSet + 1, newSum);
				_currentVector[nSet] = 0;
			}
			if (_winningSum != _UnsetWinningSum && currentSum + nLeft * p >= _winningSum) {
				break;
			}
		}
	}

	private boolean isCompatible(final int p) {
		for (int k = 0; _currentVector[k] > 0; ++k) {
			final int oldPrime = _currentVector[k];
			final long newOld = concatenate(p, oldPrime);
			if (!Eratosthenes.isPrime(newOld)) {
				return false;
			}
			final long oldNew = concatenate(oldPrime, p);
			if (!Eratosthenes.isPrime(oldNew)) {
				return false;
			}
		}
		return true;
	}

	private static long concatenate(final int k1, final int k2) {
		final int nDigitsK2 = (int) (Math.log10(k2) + 1d);
		long result = k1;
		for (int k = 0; k < nDigitsK2; ++k) {
			result *= 10;
		}
		return result + k2;
	}

	public static void main(final String[] args) {
		final int n = 5;
		final long millis0 = System.currentTimeMillis();
		final Euler0060 euler0060 = new Euler0060(n);
		final long millis1 = System.currentTimeMillis();
		final Duration duration = Duration.ofMillis(millis1 - millis0);
		final long hrs = duration.toHours();
		final long mints = duration.toMinutesPart();
		final long secs = duration.toSecondsPart();
		System.out.printf(" %s, sum=%d, took %02d:%02d:%02d.", CombinatoricTools.getString(euler0060._winningVector),
				euler0060._winningSum, hrs, mints, secs);
	}

}
