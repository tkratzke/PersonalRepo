package com.skagit.irs;

/**
 * <pre>
https://www.bing.com/videos/riverview/relatedvideo?q=capital+gains+carry+forward+calculator&mid=9FF5B95ACA85315B50459FF5B95ACA85315B5045&mcid=54CD6613AE8F4704A193C8B45936499C&FORM=VIRE
 * </pre>
 */
public class CapitalGains {
	/** Inputs: */
	public final int _maxOffset;
	public final int _carryForwardPvsShort;
	public final int _carryForwardPvsLong;
	public final int _netYearShort;
	public final int _netYearLong;
	/** Outputs: */
	public final int _taxReturnShort;
	public final int _taxReturnLong;
	public final int _carryForwardShort;
	public final int _carryForwardLong;

	public CapitalGains(final int maxOffset, final int netYearShort, final int carryForwardPvsShort,
			final int netYearLong, final int carryForwardPvsLong) {
		_maxOffset = maxOffset;
		/** CarryForwards should be non-positive. */
		_carryForwardPvsShort = Math.min(0, carryForwardPvsShort);
		_carryForwardPvsLong = Math.min(0, carryForwardPvsLong);
		_netYearShort = netYearShort;
		_netYearLong = netYearLong;

		final int short0 = _netYearShort + _carryForwardPvsShort;
		final int long0 = _netYearLong + _carryForwardPvsLong;
		if (short0 >= 0 && long0 >= 0) {
			_taxReturnShort = short0;
			_taxReturnLong = long0;
			_carryForwardShort = _carryForwardLong = 0;
		} else {
			final int negMaxOffset = -_maxOffset;
			if (short0 < 0 && long0 < 0) {
				_taxReturnShort = Math.max(short0, negMaxOffset);
				_taxReturnLong = Math.max(long0, negMaxOffset - _taxReturnShort);
				_carryForwardShort = short0 - _taxReturnShort;
				_carryForwardLong = long0 - _taxReturnLong;
			} else if (short0 >= 0 && long0 < 0) {
				/** short0 >= 0 && long0 < 0; give some loss to short. */
				final int lossFromLongToShort = Math.min(short0, -long0);
				_taxReturnShort = short0 - lossFromLongToShort;
				_carryForwardShort = 0;
				final int long1 = long0 + lossFromLongToShort;
				_taxReturnLong = Math.max(negMaxOffset, long1);
				_carryForwardLong = long1 - _taxReturnLong;
			} else {
				/** long0 >= 0 && short0 < 0; give some loss to long. */
				final int lossFromShortToLong = Math.min(long0, -short0);
				_taxReturnLong = long0 - lossFromShortToLong;
				_carryForwardLong = 0;
				final int short1 = short0 + lossFromShortToLong;
				_taxReturnShort = Math.max(negMaxOffset, short1);
				_carryForwardShort = short1 - _taxReturnShort;
			}
		}
	}

	public String getString() {
		final String s = String.format(//
				"MxOffst:\t\t\t%d\n"//
						+ "NetShrt:\t\t\t%d\t\ttxRtrnShrt:\t%d\n" //
						+ "CrryFrwrdPvsShrt:\t%d\t\tCrryFrwrdShrt:\t%d\n" //
						+ "NetLng:\t\t\t%d\t\ttxRtrnLng:\t%d\n" //
						+ "CrryFrwrdPvsLng:\t\t%d\t\tCrryFrwrdLng:\t%d", //
				_maxOffset, //
				_netYearShort, _taxReturnShort, //
				_carryForwardPvsShort, _carryForwardShort, //
				_netYearLong, _taxReturnLong, //
				_carryForwardPvsLong, _carryForwardLong //
		);
		return s;
	}

	@Override
	public String toString() {
		return getString();
	}

	public static void main(final String[] args) {
		final int[][] casesData = { //
				{ 3000, 19000, -500, -60000, -5000 }, //
				{ 3000, 19000, -500, -1000, -5000 }, //
				{ 3000, -1000, -500, -1000, -5000 }, //
				{ 3000, -1000, -500, -1000, 0 }, //
				{ 3000, -1000, -500, -10000, -8000 }, //
				{ 3000, -10000, -24000, -10000, -8000 }, //
				{ 3000, 36000, -8000, -11000, -7000 }, //
				{ 3000, 1700, -2000, 4000, -1500 }, //
				{ 3000, 5700, -2000, 4000, -1500 }, //
				{ 3000, -1000, -500, -1000, 0 } //
		};
		final int nCases = casesData.length;
		for (int kCase = 0; kCase < nCases; ++kCase) {
			final int[] caseData = casesData[kCase];
			final int maxOffset = caseData[0];
			final int netYearShort = caseData[1];
			final int carryForwardPvsShort = caseData[2];
			final int netYearLong = caseData[3];
			final int carryForwardPvsLong = caseData[4];
			final CapitalGains capitalGains = new CapitalGains(maxOffset, netYearShort, carryForwardPvsShort,
					netYearLong, carryForwardPvsLong);
			if (kCase > 0) {
				System.out.printf("\n\n");
			}
			System.out.printf("Case %d\n%s", kCase, capitalGains.getString());
		}
	}
}
