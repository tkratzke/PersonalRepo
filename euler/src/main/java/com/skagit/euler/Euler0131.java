package com.skagit.euler;

import java.util.Arrays;

import com.skagit.euler.utils.Eratosthenes;
import com.skagit.utils.StringUtilities;

public class Euler0131 {

	public static void main(final String[] args) {
		final int maxP = 1000000 - 1;
		int[] maxLengths = null;
		for (int iPass = 0; iPass < 2; ++iPass) {
			int n = 0;
			for (int seed = 1;; ++seed) {
				final int diffToNextCube = 3 * (seed * (seed + 1)) + 1;
				if (diffToNextCube > maxP) {
					break;
				}
				if (Eratosthenes.isPrime(diffToNextCube)) {
					final int p = diffToNextCube;
					final int seedSq = seed * seed;
					final int lowCubeRoot = seedSq * seed;
					final int highCubeRoot = seedSq * (int) Math.round(Math.cbrt(lowCubeRoot + p));
					final String[] strings = getStrings(++n, seed, lowCubeRoot, p, highCubeRoot);
					if (iPass == 0) {
						maxLengths = updateMaxLengths(strings, maxLengths);
					} else {
						System.out.printf(n == 1 ? "" : (n % 10 == 1 ? "\n\n" : "\n"));
						for (int k = 0; k < strings.length; ++k) {
							System.out.printf("%s%s", //
									k == 0 ? "" : " ", StringUtilities.padRight(strings[k], maxLengths[k]));
						}
					}
				}
			}
		}
	}

	private static int[] updateMaxLengths(final String[] strings, int[] maxLengths) {
		final int nStrings = strings.length;
		if (maxLengths == null) {
			maxLengths = new int[nStrings];
			Arrays.fill(maxLengths, 0);
		}
		for (int k = 0; k < nStrings; ++k) {
			maxLengths[k] = Math.max(maxLengths[k], strings[k].length());
		}
		return maxLengths;
	}

	private static String[] getStrings(final int nPrimes, final int seed, final int lowCubeRoot, final int prime,
			final int highCubeRoot) {
		return new String[] { //
				String.format("%d.", nPrimes), //
				String.format("seed[%d]", seed), //
				String.format("lowCubeRoot[%d]", lowCubeRoot), //
				String.format("prime[%d]", prime), //
				String.format("highCubeRoot[%d]", highCubeRoot) //
		};
	}
}

/**
 * <pre>
1.   seed[1]   lowCubeRoot[1]         prime[7]      highCubeRoot[2]
2.   seed[2]   lowCubeRoot[8]         prime[19]     highCubeRoot[12]
3.   seed[3]   lowCubeRoot[27]        prime[37]     highCubeRoot[36]
4.   seed[4]   lowCubeRoot[64]        prime[61]     highCubeRoot[80]
5.   seed[6]   lowCubeRoot[216]       prime[127]    highCubeRoot[252]
6.   seed[9]   lowCubeRoot[729]       prime[271]    highCubeRoot[810]
7.   seed[10]  lowCubeRoot[1000]      prime[331]    highCubeRoot[1100]
8.   seed[11]  lowCubeRoot[1331]      prime[397]    highCubeRoot[1452]
9.   seed[13]  lowCubeRoot[2197]      prime[547]    highCubeRoot[2366]
10.  seed[14]  lowCubeRoot[2744]      prime[631]    highCubeRoot[2940]

11.  seed[17]  lowCubeRoot[4913]      prime[919]    highCubeRoot[5202]
12.  seed[23]  lowCubeRoot[12167]     prime[1657]   highCubeRoot[12696]
13.  seed[24]  lowCubeRoot[13824]     prime[1801]   highCubeRoot[14400]
14.  seed[25]  lowCubeRoot[15625]     prime[1951]   highCubeRoot[16250]
15.  seed[27]  lowCubeRoot[19683]     prime[2269]   highCubeRoot[20412]
16.  seed[28]  lowCubeRoot[21952]     prime[2437]   highCubeRoot[22736]
17.  seed[30]  lowCubeRoot[27000]     prime[2791]   highCubeRoot[27900]
18.  seed[32]  lowCubeRoot[32768]     prime[3169]   highCubeRoot[33792]
19.  seed[34]  lowCubeRoot[39304]     prime[3571]   highCubeRoot[40460]
20.  seed[37]  lowCubeRoot[50653]     prime[4219]   highCubeRoot[52022]

21.  seed[38]  lowCubeRoot[54872]     prime[4447]   highCubeRoot[56316]
22.  seed[41]  lowCubeRoot[68921]     prime[5167]   highCubeRoot[70602]
23.  seed[42]  lowCubeRoot[74088]     prime[5419]   highCubeRoot[75852]
24.  seed[45]  lowCubeRoot[91125]     prime[6211]   highCubeRoot[93150]
25.  seed[48]  lowCubeRoot[110592]    prime[7057]   highCubeRoot[112896]
26.  seed[49]  lowCubeRoot[117649]    prime[7351]   highCubeRoot[120050]
27.  seed[52]  lowCubeRoot[140608]    prime[8269]   highCubeRoot[143312]
28.  seed[55]  lowCubeRoot[166375]    prime[9241]   highCubeRoot[169400]
29.  seed[58]  lowCubeRoot[195112]    prime[10267]  highCubeRoot[198476]
30.  seed[62]  lowCubeRoot[238328]    prime[11719]  highCubeRoot[242172]

31.  seed[63]  lowCubeRoot[250047]    prime[12097]  highCubeRoot[254016]
32.  seed[66]  lowCubeRoot[287496]    prime[13267]  highCubeRoot[291852]
33.  seed[67]  lowCubeRoot[300763]    prime[13669]  highCubeRoot[305252]
34.  seed[74]  lowCubeRoot[405224]    prime[16651]  highCubeRoot[410700]
35.  seed[80]  lowCubeRoot[512000]    prime[19441]  highCubeRoot[518400]
36.  seed[81]  lowCubeRoot[531441]    prime[19927]  highCubeRoot[538002]
37.  seed[86]  lowCubeRoot[636056]    prime[22447]  highCubeRoot[643452]
38.  seed[88]  lowCubeRoot[681472]    prime[23497]  highCubeRoot[689216]
39.  seed[90]  lowCubeRoot[729000]    prime[24571]  highCubeRoot[737100]
40.  seed[91]  lowCubeRoot[753571]    prime[25117]  highCubeRoot[761852]

41.  seed[93]  lowCubeRoot[804357]    prime[26227]  highCubeRoot[813006]
42.  seed[95]  lowCubeRoot[857375]    prime[27361]  highCubeRoot[866400]
43.  seed[105] lowCubeRoot[1157625]   prime[33391]  highCubeRoot[1168650]
44.  seed[108] lowCubeRoot[1259712]   prime[35317]  highCubeRoot[1271376]
45.  seed[119] lowCubeRoot[1685159]   prime[42841]  highCubeRoot[1699320]
46.  seed[123] lowCubeRoot[1860867]   prime[45757]  highCubeRoot[1875996]
47.  seed[125] lowCubeRoot[1953125]   prime[47251]  highCubeRoot[1968750]
48.  seed[128] lowCubeRoot[2097152]   prime[49537]  highCubeRoot[2113536]
49.  seed[129] lowCubeRoot[2146689]   prime[50311]  highCubeRoot[2163330]
50.  seed[136] lowCubeRoot[2515456]   prime[55897]  highCubeRoot[2533952]

51.  seed[140] lowCubeRoot[2744000]   prime[59221]  highCubeRoot[2763600]
52.  seed[142] lowCubeRoot[2863288]   prime[60919]  highCubeRoot[2883452]
53.  seed[147] lowCubeRoot[3176523]   prime[65269]  highCubeRoot[3198132]
54.  seed[153] lowCubeRoot[3581577]   prime[70687]  highCubeRoot[3604986]
55.  seed[156] lowCubeRoot[3796416]   prime[73477]  highCubeRoot[3820752]
56.  seed[157] lowCubeRoot[3869893]   prime[74419]  highCubeRoot[3894542]
57.  seed[158] lowCubeRoot[3944312]   prime[75367]  highCubeRoot[3969276]
58.  seed[164] lowCubeRoot[4410944]   prime[81181]  highCubeRoot[4437840]
59.  seed[165] lowCubeRoot[4492125]   prime[82171]  highCubeRoot[4519350]
60.  seed[170] lowCubeRoot[4913000]   prime[87211]  highCubeRoot[4941900]

61.  seed[171] lowCubeRoot[5000211]   prime[88237]  highCubeRoot[5029452]
62.  seed[172] lowCubeRoot[5088448]   prime[89269]  highCubeRoot[5118032]
63.  seed[175] lowCubeRoot[5359375]   prime[92401]  highCubeRoot[5390000]
64.  seed[179] lowCubeRoot[5735339]   prime[96661]  highCubeRoot[5767380]
65.  seed[184] lowCubeRoot[6229504]   prime[102121] highCubeRoot[6263360]
66.  seed[185] lowCubeRoot[6331625]   prime[103231] highCubeRoot[6365850]
67.  seed[186] lowCubeRoot[6434856]   prime[104347] highCubeRoot[6469452]
68.  seed[191] lowCubeRoot[6967871]   prime[110017] highCubeRoot[7004352]
69.  seed[193] lowCubeRoot[7189057]   prime[112327] highCubeRoot[7226306]
70.  seed[195] lowCubeRoot[7414875]   prime[114661] highCubeRoot[7452900]

71.  seed[196] lowCubeRoot[7529536]   prime[115837] highCubeRoot[7567952]
72.  seed[205] lowCubeRoot[8615125]   prime[126691] highCubeRoot[8657150]
73.  seed[207] lowCubeRoot[8869743]   prime[129169] highCubeRoot[8912592]
74.  seed[209] lowCubeRoot[9129329]   prime[131671] highCubeRoot[9173010]
75.  seed[212] lowCubeRoot[9528128]   prime[135469] highCubeRoot[9573072]
76.  seed[216] lowCubeRoot[10077696]  prime[140617] highCubeRoot[10124352]
77.  seed[219] lowCubeRoot[10503459]  prime[144541] highCubeRoot[10551420]
78.  seed[220] lowCubeRoot[10648000]  prime[145861] highCubeRoot[10696400]
79.  seed[224] lowCubeRoot[11239424]  prime[151201] highCubeRoot[11289600]
80.  seed[227] lowCubeRoot[11697083]  prime[155269] highCubeRoot[11748612]

81.  seed[233] lowCubeRoot[12649337]  prime[163567] highCubeRoot[12703626]
82.  seed[237] lowCubeRoot[13312053]  prime[169219] highCubeRoot[13368222]
83.  seed[238] lowCubeRoot[13481272]  prime[170647] highCubeRoot[13537916]
84.  seed[242] lowCubeRoot[14172488]  prime[176419] highCubeRoot[14231052]
85.  seed[245] lowCubeRoot[14706125]  prime[180811] highCubeRoot[14766150]
86.  seed[251] lowCubeRoot[15813251]  prime[189757] highCubeRoot[15876252]
87.  seed[258] lowCubeRoot[17173512]  prime[200467] highCubeRoot[17240076]
88.  seed[259] lowCubeRoot[17373979]  prime[202021] highCubeRoot[17441060]
89.  seed[266] lowCubeRoot[18821096]  prime[213067] highCubeRoot[18891852]
90.  seed[277] lowCubeRoot[21253933]  prime[231019] highCubeRoot[21330662]

91.  seed[279] lowCubeRoot[21717639]  prime[234361] highCubeRoot[21795480]
92.  seed[283] lowCubeRoot[22665187]  prime[241117] highCubeRoot[22745276]
93.  seed[286] lowCubeRoot[23393656]  prime[246247] highCubeRoot[23475452]
94.  seed[289] lowCubeRoot[24137569]  prime[251431] highCubeRoot[24221090]
95.  seed[294] lowCubeRoot[25412184]  prime[260191] highCubeRoot[25498620]
96.  seed[296] lowCubeRoot[25934336]  prime[263737] highCubeRoot[26021952]
97.  seed[298] lowCubeRoot[26463592]  prime[267307] highCubeRoot[26552396]
98.  seed[303] lowCubeRoot[27818127]  prime[276337] highCubeRoot[27909936]
99.  seed[305] lowCubeRoot[28372625]  prime[279991] highCubeRoot[28465650]
100. seed[307] lowCubeRoot[28934443]  prime[283669] highCubeRoot[29028692]

101. seed[308] lowCubeRoot[29218112]  prime[285517] highCubeRoot[29312976]
102. seed[312] lowCubeRoot[30371328]  prime[292969] highCubeRoot[30468672]
103. seed[314] lowCubeRoot[30959144]  prime[296731] highCubeRoot[31057740]
104. seed[315] lowCubeRoot[31255875]  prime[298621] highCubeRoot[31355100]
105. seed[321] lowCubeRoot[33076161]  prime[310087] highCubeRoot[33179202]
106. seed[331] lowCubeRoot[36264691]  prime[329677] highCubeRoot[36374252]
107. seed[333] lowCubeRoot[36926037]  prime[333667] highCubeRoot[37036926]
108. seed[335] lowCubeRoot[37595375]  prime[337681] highCubeRoot[37707600]
109. seed[340] lowCubeRoot[39304000]  prime[347821] highCubeRoot[39419600]
110. seed[342] lowCubeRoot[40001688]  prime[351919] highCubeRoot[40118652]

111. seed[346] lowCubeRoot[41421736]  prime[360187] highCubeRoot[41541452]
112. seed[350] lowCubeRoot[42875000]  prime[368551] highCubeRoot[42997500]
113. seed[352] lowCubeRoot[43614208]  prime[372769] highCubeRoot[43738112]
114. seed[353] lowCubeRoot[43986977]  prime[374887] highCubeRoot[44111586]
115. seed[354] lowCubeRoot[44361864]  prime[377011] highCubeRoot[44487180]
116. seed[357] lowCubeRoot[45499293]  prime[383419] highCubeRoot[45626742]
117. seed[359] lowCubeRoot[46268279]  prime[387721] highCubeRoot[46397160]
118. seed[364] lowCubeRoot[48228544]  prime[398581] highCubeRoot[48361040]
119. seed[368] lowCubeRoot[49836032]  prime[407377] highCubeRoot[49971456]
120. seed[375] lowCubeRoot[52734375]  prime[423001] highCubeRoot[52875000]

121. seed[381] lowCubeRoot[55306341]  prime[436627] highCubeRoot[55451502]
122. seed[388] lowCubeRoot[58411072]  prime[452797] highCubeRoot[58561616]
123. seed[391] lowCubeRoot[59776471]  prime[459817] highCubeRoot[59929352]
124. seed[398] lowCubeRoot[63044792]  prime[476407] highCubeRoot[63203196]
125. seed[399] lowCubeRoot[63521199]  prime[478801] highCubeRoot[63680400]
126. seed[405] lowCubeRoot[66430125]  prime[493291] highCubeRoot[66594150]
127. seed[417] lowCubeRoot[72511713]  prime[522919] highCubeRoot[72685602]
128. seed[419] lowCubeRoot[73560059]  prime[527941] highCubeRoot[73735620]
129. seed[429] lowCubeRoot[78953589]  prime[553411] highCubeRoot[79137630]
130. seed[437] lowCubeRoot[83453453]  prime[574219] highCubeRoot[83644422]

131. seed[441] lowCubeRoot[85766121]  prime[584767] highCubeRoot[85960602]
132. seed[443] lowCubeRoot[86938307]  prime[590077] highCubeRoot[87134556]
133. seed[444] lowCubeRoot[87528384]  prime[592741] highCubeRoot[87725520]
134. seed[445] lowCubeRoot[88121125]  prime[595411] highCubeRoot[88319150]
135. seed[448] lowCubeRoot[89915392]  prime[603457] highCubeRoot[90116096]
136. seed[450] lowCubeRoot[91125000]  prime[608851] highCubeRoot[91327500]
137. seed[451] lowCubeRoot[91733851]  prime[611557] highCubeRoot[91937252]
138. seed[454] lowCubeRoot[93576664]  prime[619711] highCubeRoot[93782780]
139. seed[457] lowCubeRoot[95443993]  prime[627919] highCubeRoot[95652842]
140. seed[465] lowCubeRoot[100544625] prime[650071] highCubeRoot[100760850]

141. seed[468] lowCubeRoot[102503232] prime[658477] highCubeRoot[102722256]
142. seed[471] lowCubeRoot[104487111] prime[666937] highCubeRoot[104708952]
143. seed[479] lowCubeRoot[109902239] prime[689761] highCubeRoot[110131680]
144. seed[480] lowCubeRoot[110592000] prime[692641] highCubeRoot[110822400]
145. seed[482] lowCubeRoot[111980168] prime[698419] highCubeRoot[112212492]
146. seed[485] lowCubeRoot[114084125] prime[707131] highCubeRoot[114319350]
147. seed[494] lowCubeRoot[120553784] prime[733591] highCubeRoot[120797820]
148. seed[497] lowCubeRoot[122763473] prime[742519] highCubeRoot[123010482]
149. seed[503] lowCubeRoot[127263527] prime[760537] highCubeRoot[127516536]
150. seed[506] lowCubeRoot[129554216] prime[769627] highCubeRoot[129810252]

151. seed[507] lowCubeRoot[130323843] prime[772669] highCubeRoot[130580892]
152. seed[511] lowCubeRoot[133432831] prime[784897] highCubeRoot[133693952]
153. seed[513] lowCubeRoot[135005697] prime[791047] highCubeRoot[135268866]
154. seed[520] lowCubeRoot[140608000] prime[812761] highCubeRoot[140878400]
155. seed[524] lowCubeRoot[143877824] prime[825301] highCubeRoot[144152400]
156. seed[528] lowCubeRoot[147197952] prime[837937] highCubeRoot[147476736]
157. seed[531] lowCubeRoot[149721291] prime[847477] highCubeRoot[150003252]
158. seed[536] lowCubeRoot[153990656] prime[863497] highCubeRoot[154277952]
159. seed[541] lowCubeRoot[158340421] prime[879667] highCubeRoot[158633102]
160. seed[543] lowCubeRoot[160103007] prime[886177] highCubeRoot[160397856]

161. seed[546] lowCubeRoot[162771336] prime[895987] highCubeRoot[163069452]
162. seed[550] lowCubeRoot[166375000] prime[909151] highCubeRoot[166677500]
163. seed[552] lowCubeRoot[168196608] prime[915769] highCubeRoot[168501312]
164. seed[555] lowCubeRoot[170953875] prime[925741] highCubeRoot[171261900]
165. seed[556] lowCubeRoot[171879616] prime[929077] highCubeRoot[172188752]
166. seed[557] lowCubeRoot[172808693] prime[932419] highCubeRoot[173118942]
167. seed[559] lowCubeRoot[174676879] prime[939121] highCubeRoot[174989360]
168. seed[563] lowCubeRoot[178453547] prime[952597] highCubeRoot[178770516]
169. seed[569] lowCubeRoot[184220009] prime[972991] highCubeRoot[184543770]
170. seed[570] lowCubeRoot[185193000] prime[976411] highCubeRoot[185517900]

171. seed[573] lowCubeRoot[188132517] prime[986707] highCubeRoot[188460846]
172. seed[574] lowCubeRoot[189119224] prime[990151] highCubeRoot[189448700]
173. seed[576] lowCubeRoot[191102976] prime[997057] highCubeRoot[191434752] *
 * </pre>
 */