package com.skagit.miscPuzzles.pentominoes;

import java.util.BitSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.skagit.utils.CombinatoricTools;

public enum TypeOfPiece {
	I("5", "I"), //
	ELL("4/1", "L"), //
	CORNER("3/..1/..1", "Cr"), //
	T("3/.1/.1", "T"), //
	W("1/2/.2", "W"), //
	CROSS(".1/3/.1", "X"), //
	FUNKY(".1/3/1", "F"), //
	UTAH("3/2", "U"), //
	Z("2/.1/.2", "Z"), //
	CUP("1.1/3", "Cp"), //
	SKID_MARK("3/..2", "S"), //
	OFFSET_T("4/.1", "Ot"), //

	SMALL_SKID(".2/2", "Sk"), //
	SMALL_I("4", "Ii"), //
	SMALL_L("3/1", "El"), //

	TWO_BY_THREE("3/3", "Rc"); //

	final static int _IndicatorLength = 2;
	final static String _NotCovered = " ";
	final static TreeMap<String, TypeOfPiece> _LookUpByIndicator = new TreeMap<>();

	static {
		for (final TypeOfPiece typeOfPiece : TypeOfPiece.values()) {
			_LookUpByIndicator.put(typeOfPiece._indicator, typeOfPiece);
		}
	}

	static TypeOfPiece getTypeOfPiece(final String indicator) {
		return _LookUpByIndicator.get(indicator);
	}

	public class Orientation implements Comparable<Orientation> {
		final BitSet[] _bitSets;
		final int _clmn0, _minClmnOffset, _maxClmnOffset;

		private Orientation(final BitSet[] bitSets, final int clmn0, final int minClmnOffset, final int maxClmnOffset) {
			_bitSets = bitSets;
			_clmn0 = clmn0;
			_minClmnOffset = minClmnOffset;
			_maxClmnOffset = maxClmnOffset;
		}

		public String getString(final String prefix0) {
			final String prefix1 = " ".repeat(prefix0.length());
			final StringBuilder s = new StringBuilder();
			final int nBitSets = _bitSets.length;
			for (int k = 0; k < nBitSets; ++k) {
				s.append(k == 0 ? "\n" : "").append(prefix1);
				final BitSet bitSet = _bitSets[k];
				for (int k1 = 0; k1 < bitSet.length(); ++k1) {
					s.append(bitSet.get(k1) ? _indicator : _NotCovered);
				}
			}
			return s.toString();
		}

		@Override
		public String toString() {
			return getString("");
		}

		@Override
		public int compareTo(final Orientation orientation) {
			final BitSet[] bitSets0 = _bitSets;
			final BitSet[] bitSets1 = orientation._bitSets;
			final int n0 = bitSets0.length, n1 = bitSets1.length;
			if (n0 != n1) {
				return n0 < n1 ? -1 : 1;
			}
			NEXT_BITSET: for (int k = 0; k < n0; ++k) {
				final BitSet bitSet0 = bitSets0[k], bitSet1 = bitSets1[k];
				for (int k0 = 0, k1 = 0;; ++k0, ++k1) {
					k0 = bitSet0.nextSetBit(k0);
					k1 = bitSet1.nextSetBit(k1);
					if ((k0 < 0) != (k1 < 0)) {
						return k0 < 0 ? 1 : -1;
					}
					if (k0 < 0) {
						continue NEXT_BITSET;
					}
					if (k0 != k1) {
						return k0 < k1 ? -1 : 1;
					}
				}
			}
			return 0;
		}

		public Orientation deepCopy() {
			return new Orientation(CombinatoricTools.deepCopy(_bitSets), _clmn0, _minClmnOffset, _maxClmnOffset);
		}
	}

	final String _descriptor;
	final BitSet[] _bitSets;
	final String _indicator;
	final int _nSquares;
	final Orientation[] _orientations;

	TypeOfPiece(final String descriptor, final String indicator) {
		_descriptor = descriptor;
		_indicator = indicator;
		final String[] rowTokens = _descriptor.split("/");
		final int nBitSets = rowTokens.length;
		final BitSet[] bitSets = new BitSet[nBitSets];
		int nSquares = 0;
		final Pattern pattern = Pattern.compile("\\d+");
		for (int k0 = 0; k0 < nBitSets; ++k0) {
			final String rowToken = rowTokens[k0];
			final Matcher matcher = pattern.matcher(rowToken);
			final BitSet bitSet = new BitSet();
			int idx = 0;
			int endOfLastMatch = 0;
			while (matcher.find()) {
				idx += matcher.start() - endOfLastMatch;
				final int n = Integer.parseInt(matcher.group());
				bitSet.set(idx, idx + n);
				idx += n;
				endOfLastMatch = matcher.end();
			}
			bitSets[k0] = bitSet;
			nSquares += bitSet.cardinality();
		}
		_bitSets = NormalizeBitSets(bitSets);
		_nSquares = nSquares;
		final TreeSet<Orientation> orientationSet = new TreeSet<>();
		BitSet[] candidate = CombinatoricTools.deepCopy(_bitSets);
		for (int j = 0; j < 8; ++j) {
			if (j == 4) {
				candidate = flipAndNormalize(candidate);
			} else if (j > 0) {
				candidate = rotateRightAndNormalize(candidate);
			}
			final int clmn0 = candidate[0].nextSetBit(0);
			int minClmnOffset = Integer.MAX_VALUE;
			int maxClmnOffset = Integer.MIN_VALUE;
			for (final BitSet bitSet : candidate) {
				minClmnOffset = Math.min(minClmnOffset, bitSet.nextSetBit(0) - clmn0);
				maxClmnOffset = Math.max(maxClmnOffset, bitSet.previousSetBit(bitSet.length() - 1) - clmn0);
			}
			orientationSet.add(new Orientation(candidate, clmn0, minClmnOffset, maxClmnOffset));
		}
		_orientations = orientationSet.toArray(new Orientation[orientationSet.size()]);
	}

	private static BitSet[] flipAndNormalize(final BitSet[] bitSets) {
		final int nBitSets = bitSets.length;
		final BitSet[] newBitSets = new BitSet[nBitSets];
		for (int k = 0; k < nBitSets; ++k) {
			newBitSets[k] = (BitSet) bitSets[nBitSets - 1 - k].clone();
		}
		return NormalizeBitSets(newBitSets);
	}

	private static BitSet[] rotateRightAndNormalize(final BitSet[] bitSets) {
		int maxNClmns = 0;
		final int nBitSets = bitSets.length;
		for (int k = 0; k < nBitSets; ++k) {
			maxNClmns = Math.max(maxNClmns, bitSets[k].length());
		}
		final BitSet[] newBitSets = new BitSet[maxNClmns];
		for (int k = 0; k < nBitSets; ++k) {
			final int newClmn = nBitSets - 1 - k;
			final BitSet bitSet = bitSets[k];
			for (int clmn = bitSet.nextSetBit(0); clmn >= 0; clmn = bitSet.nextSetBit(clmn + 1)) {
				final int newRow = clmn;
				if (newBitSets[newRow] == null) {
					newBitSets[newRow] = new BitSet(nBitSets);
				}
				final BitSet newBitSet = newBitSets[clmn];
				newBitSet.set(newClmn);
			}
		}
		return NormalizeBitSets(newBitSets);
	}

	private static BitSet[] NormalizeBitSets(final BitSet[] bitSets) {
		int minClmn = Integer.MAX_VALUE;
		final int nRows = bitSets.length;
		for (int k0 = 0; k0 < nRows; ++k0) {
			final BitSet bitSet = bitSets[k0];
			minClmn = Math.min(minClmn, bitSet.nextSetBit(0));
		}
		final BitSet[] newBitSets = new BitSet[nRows];
		for (int k0 = 0; k0 < nRows; ++k0) {
			final BitSet bitSet = bitSets[k0];
			final BitSet newBitSet = new BitSet(bitSet.length() - minClmn);
			for (int k1 = bitSet.nextSetBit(0); k1 >= 0; k1 = bitSet.nextSetBit(k1 + 1)) {
				newBitSet.set(k1 - minClmn);
			}
			newBitSets[k0] = newBitSet;
		}
		return newBitSets;
	}

	public String getString() {
		return _indicator;
	}

	@Override
	public String toString() {
		return getString();
	}
}
