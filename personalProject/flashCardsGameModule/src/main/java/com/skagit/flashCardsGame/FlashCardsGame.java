package com.skagit.flashCardsGame;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.skagit.flashCardsGame.enums.ChangeType;
import com.skagit.flashCardsGame.enums.Clumping;
import com.skagit.flashCardsGame.enums.DiacriticsTreatment;
import com.skagit.flashCardsGame.enums.Mode;
import com.skagit.flashCardsGame.enums.PropertyPlus;
import com.skagit.flashCardsGame.utils.AudioFilePlayer;
import com.skagit.flashCardsGame.utils.BackUpFileGetter;
import com.skagit.flashCardsGame.utils.CommentParts;
import com.skagit.flashCardsGame.utils.FlashCardStatics;
import com.skagit.flashCardsGame.utils.FlashCardStatics.YesNoResponse;
import com.skagit.flashCardsGame.utils.FlashCardsGameDirsTracker;
import com.skagit.flashCardsGame.utils.InputString;
import com.skagit.flashCardsGame.utils.LineBreakDown;
import com.skagit.flashCardsGame.utils.MyProperties;
import com.skagit.util.poi.ExcelFileChecker;
import com.skagit.utils.StaticUtilities;
import com.skagit.utils.StringUtilities;

/**
 * <pre>
 * Interesting note on deleting remote branches from within Eclipse:
 * https://stackoverflow.com/questions/8625406/how-to-delete-a-branch-in-the-remote-repository-using-egit
 * </pre>
 */
public class FlashCardsGame {

	final private File _gameFile;
	final private File _cardsFile;
	final private File _soundFilesDir;

	final private TreeMap<String, File> _soundFilesMap;
	final private TreeMap<String, String> _partToStem;

	final private MyProperties _myProperties;
	final private long _randomSeed;
	final private Mode _mode;
	final private int _blockSize;
	final private boolean _beSilent;
	final private DiacriticsTreatment _diacriticsTreatment;
	final private Clumping _clumping;
	final private QuizGenerator _quizGenerator;

	private Card[] _cards;
	private QuizPlus _quizPlus;
	private boolean _needLineFeed;

	FlashCardsGame(final String gameFileString) {
		_gameFile = FlashCardStatics.getGameFile(gameFileString);
		_needLineFeed = false;
		/** Back up _gameFile. */
		final File gameFileBackUp = BackUpFileGetter.getBackUpFile(_gameFile);
		if (!StaticUtilities.copyNonDirectoryFile(/* logger= */null, StringUtilities.getCanonicalPath(_gameFile),
				StringUtilities.getCanonicalPath(gameFileBackUp))) {
			System.err.println(
					String.format("Failed to copy %s to %s.", _gameFile.toString(), gameFileBackUp.toString()));
			_cardsFile = _soundFilesDir = null;
			_soundFilesMap = null;
			_partToStem = null;
			_myProperties = null;
			_randomSeed = 0;
			_mode = null;
			_blockSize = -1;
			_diacriticsTreatment = null;
			_clumping = null;
			_quizGenerator = null;
			_beSilent = true;
			return;
		}
		final String[] stemAndExtension = StaticUtilities.getStemAndExtension(_gameFile.getName());
		final String stem = stemAndExtension[0];
		System.out.println(String.format("Copied %s to %s.", _gameFile.toString(), gameFileBackUp.toString()));

		/** Load the Game. */
		_myProperties = new MyProperties();
		try (InputStreamReader isr = new InputStreamReader(new FileInputStream(_gameFile), "UTF-8")) {
			final MyProperties myProperties = new MyProperties();
			myProperties.load(isr);
			final int nPropertyPluses = PropertyPlus._Values.length;
			for (int k = 0; k < nPropertyPluses; ++k) {
				final PropertyPlus propertyPlus = PropertyPlus._Values[k];
				final String key = propertyPlus._propertyName;
				final String propertiesString = (String) myProperties.get(key);
				final String overrideString;
				if (propertiesString == null
						&& (propertyPlus == PropertyPlus.CARDS_FILE || propertyPlus == PropertyPlus.SOUND_FILES_DIR)) {
					overrideString = stem;
				} else {
					overrideString = null;
				}
				final String validatedString = myProperties.getValidString(propertyPlus, overrideString);
				_myProperties.put(key, validatedString);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}

		/** Get CardsFile and SoundsDir. */
		final String cardsFileString = (String) _myProperties.get(PropertyPlus.CARDS_FILE._propertyName);
		_cardsFile = FlashCardStatics.getCardsFile(_gameFile.getParentFile(), cardsFileString);
		/** Back up _cardsFile. */
		final File cardsFileBackUp = BackUpFileGetter.getBackUpFile(_cardsFile);
		if (!StaticUtilities.copyNonDirectoryFile(/* logger= */null, StringUtilities.getCanonicalPath(_cardsFile),
				StringUtilities.getCanonicalPath(cardsFileBackUp))) {
			System.err.println(
					String.format("Failed to copy %s to %s.", _cardsFile.toString(), cardsFileBackUp.toString()));
			_soundFilesDir = null;
			_diacriticsTreatment = null;
			_mode = null;
			_blockSize = -1;
			_randomSeed = 0;
			_clumping = null;
			_beSilent = false;
			_soundFilesMap = null;
			_partToStem = null;
			_quizGenerator = null;
			return;
		}
		System.out.println(String.format("Copied %s to %s.", _cardsFile.toString(), cardsFileBackUp.toString()));

		final String soundFilesDirString = (String) _myProperties.get(PropertyPlus.SOUND_FILES_DIR._propertyName);
		_soundFilesDir = FlashCardStatics.getSoundFilesDir(_gameFile.getParentFile(), soundFilesDirString);
		_diacriticsTreatment = DiacriticsTreatment
				.valueOf(_myProperties.getValidString(PropertyPlus.DIACRITICS_TREATMENT, /* overrideString= */null));
		_mode = Mode.valueOf(_myProperties.getValidString(PropertyPlus.MODE, /* overrideString= */null));
		_clumping = Clumping.valueOf(_myProperties.getValidString(PropertyPlus.CLUMPING, /* overrideString= */null));
		_beSilent = Boolean
				.parseBoolean(_myProperties.getValidString(PropertyPlus.BE_SILENT, /* overrideString= */null));
		_blockSize = Integer.parseInt(_myProperties.getValidString(PropertyPlus.BLOCK_SIZE, /* overrideString= */null));
		_randomSeed = Long.parseLong(_myProperties.getValidString(PropertyPlus.RANDOM_SEED, /* overrideString= */null));
		_soundFilesMap = new TreeMap<>();
		_partToStem = new TreeMap<>();

		/** Load the Cards. */
		loadCards(/* announceCompleteDups= */true);
		if (_clumping != Clumping.NO_CLUMPING) {
			final int nDups = announceAndClumpDuplicates();
			if (nDups > 0) {
				if (_needLineFeed) {
					System.out.println();
				}
				final String dupType = "Dup " + (_clumping == Clumping.BY_CLUE ? "Clues" : "Answers");
				System.out.println(String.format("n%s=%d.", dupType, nDups));
			}
		}
		dumpCardsToFile();
		if (Mode._DumpCardsAndAbort.contains(_mode)) {
			_quizGenerator = null;
			return;
		}

		_quizGenerator = new QuizGenerator(_mode, _myProperties, _cards.length, _randomSeed);
		shuffleCards(_cards);
		_quizPlus = null;
	}

	private void addToSoundFiles(final File mainDir) {
		if (mainDir == null) {
			return;
		}
		/** Add sub-directories' soundFiles. */
		final File[] subDirs = mainDir.listFiles((FileFilter) f -> {
			if (!f.isDirectory()) {
				return false;
			}
			final String fName = f.getName();
			if (0 <= "(){}[]".indexOf(fName.charAt(0))) {
				return false;
			}
			final int fNameLen = fName.length();
			if (0 <= "(){}[]".indexOf(fName.charAt(fNameLen - 1))) {
				return false;
			}
			return true;
		});
		for (final File subDir : subDirs) {
			addToSoundFiles(subDir);
		}
		/** Add mainDir's own sound files. */
		final File[] mainDirSoundFiles = mainDir.listFiles((FileFilter) AudioFilePlayer::isValidFile);
		for (final File f : mainDirSoundFiles) {
			final String fName = f.getName();
			final ArrayList<String> keyList = new ArrayList<>();
			/** fName is a valid key. */
			keyList.add(fName);
			/** Strip the extension from fName to make stem, which is a valid key. */
			final String[] stemAndExtension = StaticUtilities.getStemAndExtension(fName);
			final String stem = stemAndExtension[0];
			if (stem != fName) {
				keyList.add(stem);
			}
			final Matcher matcher = Pattern.compile("^\\d+\\-").matcher(stem);
			if (matcher.find()) {
				final int stemLen = stem.length();
				final int start = matcher.start();
				final int end = matcher.end();
				/** The digits make up a valid key. */
				final String digitsString = stem.substring(start, end - 1);
				keyList.add(digitsString);
				_partToStem.put(digitsString, stem);
				if (end < stemLen) {
					/** Everything in stem beyond the dash make up a valid key. */
					final String beyondDashString = stem.substring(end, stemLen);
					keyList.add(beyondDashString);
					_partToStem.put(beyondDashString, stem);
				}
			}
			File parent = f.getParentFile();
			String parentNamePlus = parent.getName() + File.separator;
			for (int k = 0; !keyList.isEmpty();) {
				k %= keyList.size();
				final String thisKey = keyList.get(k);
				final File oldFile = _soundFilesMap.get(thisKey);
				if (oldFile == null) {
					_soundFilesMap.put(thisKey, f);
					keyList.remove(k);
				} else {
					keyList.set(k, parentNamePlus + thisKey);
					++k;
				}
			}
			parent = parent.getParentFile();
			parentNamePlus = parent.getName() + File.separator + parentNamePlus;
		}
	}

	private void loadCards(final boolean announceCompleteDups) {
		_soundFilesMap.clear();
		addToSoundFiles(_soundFilesDir);
		final TreeMap<Card, Card> cardMap = loadCardMap(announceCompleteDups);
		final int nCards = cardMap.size();
		_cards = cardMap.keySet().toArray(new Card[nCards]);
		Arrays.sort(_cards, Mode._StrangeSort.contains(_mode) ? Card._StrangeSort : Card._ByCardNumberOnly);
	}

	private void loadCardMapFromExcel(final TreeMap<Card, Card> cardMap, final boolean announceCompleteDups) {
		try (FileInputStream fis = new FileInputStream(_cardsFile); XSSFWorkbook workBook = new XSSFWorkbook(fis)) {
			final int nSheets = workBook.getNumberOfSheets();
			for (int k = 0; k < nSheets; ++k) {
				final XSSFSheet sheet = workBook.getSheetAt(k);
				final String sheetName = sheet.getSheetName();
				for (final Row row : sheet) {
					final Cell clueCell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
					final Cell answerCell = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
					if (clueCell == null || answerCell == null) {
						continue;
					}
					final String clueSide = clueCell.getStringCellValue();
					final String answerSide = answerCell.getStringCellValue();
					if (clueSide.toLowerCase().contains("clues") && answerSide.toLowerCase().contains("answers")) {
						continue;
					}
					wrapUpForExcelFile(cardMap, clueSide, answerSide, sheetName, row.getRowNum(), announceCompleteDups);
				}
			}
		} catch (final IOException e) {
		}
	}

	/** Excel files do not handle comments or sound files right now (2026-09-03) */
	private void wrapUpForExcelFile(final TreeMap<Card, Card> cardMap, final String clueSide, final String answerSide,
			final String sheetName, final int rowNumber, final boolean announceCompleteDups) {
		final boolean switchSides = _mode == Mode.SWITCH_AND_DUMP;
		if (clueSide != null && answerSide != null && clueSide.length() > 0 && answerSide.length() > 0) {
			final Card newCard = new Card(switchSides, _partToStem, cardMap.size(), clueSide, answerSide, sheetName,
					rowNumber);
			final Card oldCard = cardMap.get(newCard);
			if ((oldCard != null) && announceCompleteDups) {
				System.out
						.println(String.format("Merging Card #%d into #%d:", newCard._cardNumber, oldCard._cardNumber));
				System.out.println(oldCard.getString());
				System.out.println(newCard.getString());
			}
			cardMap.put(newCard, newCard);
		}
	}

	private void loadCardMapFromTxt(final TreeMap<Card, Card> cardMap, final boolean announceCompleteDups) {
			/**
			 * <pre>
			 * Interesting note on text files and the "right" way to do things:
			 * https://stackoverflow.com/questions/17405165/first-character-of-the-reading-from-the-text-file-%C3%AF
			 * </pre>
			 */
			try (final BufferedReader in = new BufferedReader(
					new InputStreamReader(new FileInputStream(_cardsFile), "UTF-8"))) {
			/** Mark the stream at the beginning of the file. */
			if (in.markSupported()) {
				/**
				 * Mark where we're at in the file (the beginning). The "1" is a "read ahead
				 * limit."
				 */
				in.mark(1);
				/**
				 * If the first character is NOT feff, go back to the beginning of the file. If
				 * it IS feff, ignore it and continue on.
				 */
				if (in.read() != 0xFEFF) {
					in.reset();
				}
			}

			String fullClueString = null;
			String fullAnswerString = null;
			final ArrayList<String> commentLinesList = new ArrayList<>();
			String comment = null;
			try (final Scanner fileSc = new Scanner(in)) {
				while (fileSc.hasNext()) {
					final String nextLine = fileSc.nextLine();
					final String trimmed = nextLine.trim();
					if (trimmed.isBlank()) {
						continue;
					}
					if (FlashCardStatics._EndCardsString.equalsIgnoreCase(trimmed)) {
						break;
					}
					final boolean newComment = trimmed.charAt(0) == FlashCardStatics._CommentChar;
					final boolean existingComment = comment != null;
					if (newComment || existingComment) {
						final boolean hasContinuation = nextLine.charAt(nextLine.length() - 1) == '\t';
						if (newComment) {
							if (existingComment) {
								commentLinesList.add(comment);
							}
							comment = trimmed.substring(1).trim();
							if (!hasContinuation) {
								commentLinesList.add(comment);
								comment = null;
							}
							continue;
						}
						/** This is a continuation of an existing comment. */
						comment += " " + trimmed;
						if (!hasContinuation) {
							commentLinesList.add(comment);
							comment = null;
						}
						continue;
					}

					/** Not a comment and not part of a comment. */
					final LineBreakDown lbd = new LineBreakDown(nextLine);
					final String lbdClueSide = lbd._clueSide;
					final String lbdAnswerSide = lbd._answerSide;
					if (lbdClueSide == null && lbdAnswerSide == null) {
						/** Essentially, nextLine is blank. */
						continue;
					}
					if (fullClueString == null) {
						fullClueString = lbdClueSide;
						fullAnswerString = lbdAnswerSide;
					} else {
						if (lbdClueSide.length() > 0) {
							fullClueString += " " + lbdClueSide;
						}
						if (lbdAnswerSide.length() > 0) {
							fullAnswerString += " " + lbdAnswerSide;
						}
					}
					if (!lbd._nextLineIsContinuation) {
						wrapUpForTxtFile(cardMap, fullClueString, fullAnswerString, commentLinesList, comment,
								announceCompleteDups);
						fullClueString = fullAnswerString = comment = null;
					}
				}
				wrapUpForTxtFile(cardMap, fullClueString, fullAnswerString, commentLinesList, comment,
						announceCompleteDups);
				fullClueString = fullAnswerString = comment = null;
			} catch (final Exception e) {
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private TreeMap<Card, Card> loadCardMap(final boolean announceCompleteDups) {
		final TreeMap<Card, Card> cardMap = new TreeMap<>(Card._ByClueThenAnswer);

		if (ExcelFileChecker.endsWithXlsPlusOne(_cardsFile.getName())) {
			loadCardMapFromExcel(cardMap, announceCompleteDups);
		} else {
			loadCardMapFromTxt(cardMap, announceCompleteDups);
		}
		return cardMap;
	}

	private void wrapUpForTxtFile(final TreeMap<Card, Card> cardMap, final String clueSide, final String answerSide,
			final ArrayList<String> commentList, final String comment, final boolean announceCompleteDups) {
		if (comment != null && comment.length() > 0) {
			commentList.add(comment);
		}
		final boolean switchSides = _mode == Mode.SWITCH_AND_DUMP;
		if (clueSide != null && answerSide != null && clueSide.length() > 0 && answerSide.length() > 0) {
			final int nNewCommentLines = commentList.size();
			final String[] newCommentLines = commentList.toArray(new String[nNewCommentLines]);
			final Card newCard = new Card(switchSides, _partToStem, cardMap.size(), clueSide, answerSide,
					newCommentLines);
			final Card oldCard = cardMap.get(newCard);
			if (oldCard != null) {
				if (announceCompleteDups) {
					System.out.println(
							String.format("Merging Card #%d into #%d:", newCard._cardNumber, oldCard._cardNumber));
					System.out.println(oldCard.getString());
					System.out.println(newCard.getString());
				}
				if (nNewCommentLines > 0) {
					final String[] oldCommentLines = oldCard._commentLines;
					final int nOldCommentLines = oldCommentLines == null ? 0 : oldCommentLines.length;
					final int nAllCommentLines = nOldCommentLines + nNewCommentLines;
					final String[] allCommentLines = new String[nAllCommentLines];
					if (nOldCommentLines > 0) {
						System.arraycopy(oldCommentLines, 0, allCommentLines, 0, nOldCommentLines);
					}
					System.arraycopy(newCommentLines, 0, allCommentLines, nOldCommentLines, nNewCommentLines);
					oldCard._commentLines = allCommentLines;
					commentList.clear();
					return;
				}
			}
			cardMap.put(newCard, newCard);
			commentList.clear();
		}
	}

	private int announceAndClumpDuplicates() {
		if (_clumping != Clumping.BY_CLUE && _clumping != Clumping.BY_ANSWER) {
			return 0;
		}
		final boolean byClue = _clumping == Clumping.BY_CLUE;
		final Comparator<Card> comparator = byClue ? Card._ByClueSideOnly : Card._ByAnswerSideOnly;
		final int nCards = _cards.length;
		int nDups = 0;
		final String sideBeingChecked = byClue ? "Clue Side" : "Answer Side";
		final TreeMap<Card, ArrayList<Card>> kingToSlaves = new TreeMap<>(comparator);
		for (int k = 0; k < nCards; ++k) {
			final Card card = _cards[k];
			final ArrayList<Card> slaves = kingToSlaves.get(card);
			if (slaves != null) {
				/** card has an existing king. */
				final Card oldCard = kingToSlaves.ceilingKey(card);
				if (_needLineFeed) {
					System.out.println();
				}
				System.out.println(String.format("Duplicate %s #%d:", sideBeingChecked, nDups++));
				System.out.println(oldCard.getString());
				System.out.println(card.getString());
				_needLineFeed = true;
				slaves.add(card);
				_cards[k] = null;
			} else {
				kingToSlaves.put(card, new ArrayList<>());
			}
		}
		final Card[] newCards = new Card[nCards];
		for (int k0 = 0, k1 = 0; k0 < nCards; ++k0) {
			final Card card = _cards[k0];
			if (card == null) {
				/** The card formerly at k0 is now somebody's slave. */
				continue;
			}
			card._cardNumber = k1;
			newCards[k1++] = card;
			final ArrayList<Card> slaves = kingToSlaves.get(card);
			final int nSlaves = slaves.size();
			for (int k2 = 0; k2 < nSlaves; ++k2) {
				final Card slave = slaves.get(k2);
				slave._cardNumber = k1;
				newCards[k1++] = slave;
			}
		}
		System.arraycopy(newCards, 0, _cards, 0, nCards);
		return nDups;
	}

	private void dumpCardsToFile() {
		final int nCards = _cards.length;
		if (nCards == 0) {
			return;
		}
		final int nDigits = (int) (Math.log10(nCards) + 1d);
		final String intFormat = String.format("%%%dd.", nDigits);
		final String blankIntFormat = String.format("%%-%ds ", nDigits);
		final String blankIntString = String.format(blankIntFormat, "");
		int maxCluePartLen = 0;
		for (final Card card : _cards) {
			final FullSideStringParts aParts = new FullSideStringParts(card.getTrimmedInputString(/* clueSide= */true),
					FlashCardStatics._MaxLenForCardPart);
			maxCluePartLen = Math.max(maxCluePartLen, aParts._maxLen);
		}
		final String cluePartFormat = String.format("%%-%ds", maxCluePartLen);

		final File dumpFile;
		if (ExcelFileChecker.endsWithXlsPlusOne(_cardsFile.getName())) {
			final File parentFile = _cardsFile.getParentFile();
			final String fName = _cardsFile.getName();
			final String[] stemAndExtension = StaticUtilities.getStemAndExtension(fName);
			final File dumpFileX = new File(parentFile, stemAndExtension[0] + ".txt");
			dumpFile = BackUpFileGetter.getBackUpFile(dumpFileX);
		} else {
			dumpFile = _cardsFile;
		}

		try (PrintWriter pw = new PrintWriter(dumpFile)) {
			boolean recentWasMultiLine = false;
			for (int kCard = 0, nPrinted = 0; kCard < nCards; ++kCard) {
				final Card card = _cards[kCard];
				final String clueSideTrimmedInputString = card.getTrimmedInputString(/* clueSide= */true);
				final String answerSideTrimmedInputString;
				final String[] commentLines;
				if (_mode != Mode.SUPPRESS_ANSWERS_AND_DUMP) {
					answerSideTrimmedInputString = card.getTrimmedInputString(/* clueSide= */false);
					commentLines = card._commentLines;
				} else {
					answerSideTrimmedInputString = "";
					commentLines = new String[0];
				}
				final FullSideStringParts clueParts = new FullSideStringParts(clueSideTrimmedInputString,
						FlashCardStatics._MaxLenForCardPart);
				final FullSideStringParts answerParts = new FullSideStringParts(answerSideTrimmedInputString,
						FlashCardStatics._MaxLenForCardPart);
				final int nClueParts = clueParts.size();
				final int nAnswerParts = answerParts.size();
				final int nDataParts = Math.max(nClueParts, nAnswerParts);
				final int nCommentLines = commentLines == null ? 0 : commentLines.length;
				final boolean isMultiLine = nCommentLines > 0 || nDataParts > 1;
				if ((kCard > 0) && (isMultiLine || recentWasMultiLine || (nPrinted % _blockSize == 0))) {
					pw.println();
				}
				recentWasMultiLine = isMultiLine;
				for (int kComment = 0; kComment < nCommentLines; ++kComment) {
					final CommentParts commentParts = new CommentParts(commentLines[kComment],
							FlashCardStatics._MaxLineLen);
					for (int kCommentPart = 0; kCommentPart < commentParts.size(); ++kCommentPart) {
						pw.print(commentParts.get(kCommentPart));
						if (kCommentPart < commentParts.size() - 1) {
							pw.print('\t');
						}
						pw.println();
					}
				}
				for (int kDataPart = 0; kDataPart < nDataParts; ++kDataPart) {
					final String cluePart = kDataPart < nClueParts ? clueParts.get(kDataPart) : null;
					final String answerPart = kDataPart < nAnswerParts ? answerParts.get(kDataPart) : null;
					if (kDataPart == 0) {
						pw.printf(intFormat, card._cardNumber);
					} else {
						pw.print(blankIntString);
					}
					pw.print('\t');
					pw.printf(answerPart != null ? cluePartFormat : "%s", cluePart != null ? cluePart : "");
					if (answerPart != null) {
						pw.printf("\t%s", answerPart);
					}
					if (kDataPart < nDataParts - 1) {
						pw.print('\t');
					}
					pw.println();
				}
				++nPrinted;
			}
		} catch (final FileNotFoundException e) {
		}
	}

	void updateProperties() {
		updateChangeableProperties();
		_quizGenerator.updateChangeableProperties(_myProperties);
	}

	void updateChangeableProperties() {
	}

	void overwriteGameFile() {
		final MyProperties myProperties;
		final long seed = Long.parseLong(_myProperties.getValidString(PropertyPlus.RANDOM_SEED,
				/* overrideString= */Long.toString(_randomSeed)));
		if (seed < 0) {
			myProperties = (MyProperties) _myProperties.clone();
			final int topCardIdx0 = Integer.parseInt(_myProperties.getValidString(PropertyPlus.TOP_CARD_INDEX,
					/* overrideString= */Integer.toString(_quizGenerator._topCardIndex)));
			final int maxNNewWords = Integer.parseInt(
					_myProperties.getValidString(PropertyPlus.NUMBER_OF_NEW_WORDS, /* overrideString= */null));
			final int maxNRecentWords = Integer.parseInt(
					_myProperties.getValidString(PropertyPlus.NUMBER_OF_RECENT_WORDS, /* overrideString= */null));
			final int topIndexCardIdx1 = Math.min(topCardIdx0, maxNNewWords + maxNRecentWords - 1);
			myProperties.put(PropertyPlus.TOP_CARD_INDEX._propertyName, Integer.toString(topIndexCardIdx1));
		} else {
			myProperties = _myProperties;
		}

		try (PrintWriter pw = new PrintWriter(new FileOutputStream(_gameFile))) {
			final int nPropertyPluses = PropertyPlus._Values.length;
			for (int k0 = 0; k0 < nPropertyPluses; ++k0) {
				final PropertyPlus propertyPlus = PropertyPlus._Values[k0];
				final String comment = propertyPlus._comment;
				if (comment != null && comment.length() > 0) {
					if (k0 > 0) {
						pw.println();
					}
					final String[] lines = comment.trim().split("\n");
					final int nLines = lines.length;
					for (int k1 = 0; k1 < nLines; ++k1) {
						pw.print(FlashCardStatics._CommentString + lines[k1]);
						pw.println();
					}
				}
				final String realPropertyName = propertyPlus._propertyName;
				pw.printf("%s=%s", realPropertyName, myProperties.get(realPropertyName));
				pw.println();
			}
		} catch (final IOException e) {
		}
	}

	private void shuffleCards(final Card[] cards) {
		if (_randomSeed == 0) {
			return;
		}
		final Random r = new Random();
		if (_randomSeed > 0) {
			r.setSeed(_randomSeed);
		}
		while (Math.abs(r.nextLong()) < Long.MAX_VALUE / 5) {
		}
		final int nCards = cards.length;
		for (int k0 = 0; k0 < nCards; ++k0) {
			final int k1 = k0 + r.nextInt(nCards - k0);
			final Card card = cards[k0];
			cards[k0] = cards[k1];
			cards[k1] = card;
		}
	}

	final private String getTypeIPrompt(final int cardIdx, final boolean currentQuestionWasWrongAtLeastOnce) {
		final int cardNumber = _cards[cardIdx]._cardNumber;
		final int quizLen = _quizPlus.getCurrentQuizLen();
		if (_mode == Mode.STEP) {
			return String.format("CrdIdx=%d,#%d", cardIdx, cardNumber);
		}
		final StringBuilder typeIPrompt = new StringBuilder();
		final int currentIdxInQuiz = _quizPlus.getCurrentIdxInQuiz();
		final boolean criticalQuizIdx = _quizPlus.isCriticalQuizIndex(currentIdxInQuiz);
		if (criticalQuizIdx) {
			typeIPrompt.append("*");
		}
		typeIPrompt
				.append(String.format("%d of %d(CrdIdx=%d,#%d)", currentIdxInQuiz + 1, quizLen, cardIdx, cardNumber));
		final int nRights = _quizPlus.getNRights();
		int nWrongs = _quizPlus.getNWrongs();
		int nTrials = nRights + nWrongs;
		if (criticalQuizIdx && currentQuestionWasWrongAtLeastOnce) {
			++nWrongs;
			++nTrials;
		}
		if (nRights > 0 || nWrongs > 0) {
			final long successPerCent = Math.round((100d * nRights) / nTrials);
			typeIPrompt.append(String.format(",(Rt:Wr=%d:%d SccRt=%d%%)", nRights, nWrongs, successPerCent));
		}
		return typeIPrompt.toString();
	}

	final private String getTypeIIPrompt() {
		final String prompt = String.format("Enter: %c=Done", FlashCardStatics._ReturnChar);
		return prompt + _quizGenerator.getTypeIIPrompt();
	}

	private static PropertyPlus getPropertyPlus(final String propertyPlusShortName) {
		if (propertyPlusShortName == null) {
			return null;
		}
		for (final PropertyPlus propertyPlus : PropertyPlus._Values) {
			final String shortName = propertyPlus._shortName;
			if (shortName != null && shortName.equals(propertyPlusShortName)) {
				return propertyPlus;
			}
		}
		return null;
	}

	final private void modifyProperties(final Scanner sc) {
		for (;;) {
			if (_needLineFeed) {
				System.out.println();
			}
			final String prompt = getTypeIIPrompt();
			System.out.print(prompt);
			System.out.print(": ");
			final InputString inputString = new InputString(sc);
			final String inputLine = inputString._inputString;
			_needLineFeed = !inputString._lastLineWasBlank;
			if (inputLine.length() == 0) {
				return;
			}
			final String inputLineUc = inputLine.toUpperCase();
			final String[] fields = inputLineUc.split(StringUtilities._WhiteSpace);
			final int nFields = fields == null ? 0 : fields.length;
			final String propertyPlusShortName = (nFields < 1) ? null : fields[0].toUpperCase();
			final String propertyPlusValue = (nFields < 2) ? null : fields[1].toUpperCase();
			final PropertyPlus propertyPlus = getPropertyPlus(propertyPlusShortName);
			if (propertyPlus == null) {
				continue;
			}
			if (PropertyPlus._ChangeableCoreProperties.contains(propertyPlus)) {
				modifyProperty(propertyPlus, propertyPlusValue);
			} else if (PropertyPlus._ChangeableQuizGeneratorProperties.contains(propertyPlus)) {
				_quizGenerator.modifyProperty(propertyPlus, propertyPlusValue);
			}
		}
	}

	private void modifyProperty(final PropertyPlus propertyPlus, final String propertyPlusValue) {
		/** No core myProperties can be modified. */
	}

	private long[] getChangeableLongPropertyValues() {
		return new long[0];
	}

	final private boolean madeChangesFrom(final long[] oldValues) {
		final long[] newValues = getAllCurrentChangeableValues();
		final int nValues = newValues.length;
		for (int k = 0; k < nValues; ++k) {
			if (newValues[k] != oldValues[k]) {
				return true;
			}
		}
		return false;
	}

	final private long[] getAllCurrentChangeableValues() {
		final long[] coreValues = getChangeableLongPropertyValues();
		final long[] quizGeneratorValues = _quizGenerator.getChangeableLongPropertyValues();
		final int nA = coreValues.length;
		final int nB = quizGeneratorValues.length;
		final long[] allChangeableValues = new long[nA + nB];
		System.arraycopy(coreValues, 0, allChangeableValues, 0, nA);
		System.arraycopy(quizGeneratorValues, 0, allChangeableValues, nA, nB);
		return allChangeableValues;
	}

	final public String getString() {
		final String s0 = String.format(//
				"""
						GameFile     : %s
						CardsFile    : %s
						SoundFilesDir: %s""", //
				_gameFile == null ? "NULL" : _gameFile, //
				_cardsFile == null ? "NULL" : _cardsFile, //
				_soundFilesDir == null ? "NULL" : _soundFilesDir //
		);
		final String s1 = getString0();
		return s0 + "\n" + s1;
	}

	final private String getString0() {
		final StringBuilder s = new StringBuilder().append(String.format("%s RandomSeed[%d]", //
				_cards != null ? String.format("(w/ %d cards)", _cards.length) : "", //
				_randomSeed));
		if (_quizGenerator != null) {
			s.append("\n ").append(_quizGenerator.getString());
		}
		return s.toString();
	}

	@Override
	public String toString() {
		return getString();
	}

	void mainLoop(final Scanner sysInScanner) {
		int nCards = _cards.length;
		long[] oldChangeableValues = getAllCurrentChangeableValues();
		_quizPlus = null;
		boolean restarted = false;

		OUTSIDE_LOOP: for (boolean keepGoing = true; keepGoing;) {
			/** Check for a _status change from _quizPlus. */
			final QuizPlusTransition quizPlusTransition = _quizGenerator.getStatusChange(_mode, nCards, restarted,
					_quizPlus);
			_quizPlus = quizPlusTransition._newQuizPlus;
			restarted = false;
			final ChangeType changeType = quizPlusTransition._changeType;
			if (ChangeType._NewQuizSet.contains(changeType)) {
				System.out.println();
				if (_needLineFeed) {
					System.out.println();
				}
				if (ChangeType._ReallyNewQuizSet.contains(changeType)) {
					/** For a really new one, add another lineFeed. */
					System.out.println();
					System.out.println(getString());
				}
				System.out.print(changeType._reasonForChangeString);
				System.out.println(" " + quizPlusTransition._transitionString);
				_needLineFeed = true;
			}
			if (madeChangesFrom(oldChangeableValues)) {
				updateProperties();
				overwriteGameFile();
				oldChangeableValues = getAllCurrentChangeableValues();
			}

			final int cardIdx = _quizPlus.getCurrentQuiz_CardIndex();
			final Card card = _cards[cardIdx];

			final String clueStringPart = card.getStringPart(/* clueSide= */true);
			final int clueStringPartLen = clueStringPart != null ? clueStringPart.length() : 0;
			final boolean clueHasStringPart = clueStringPartLen > 0;

			final String answerStringPart = card.getStringPart(/* clueSide= */false);
			final int answerStringPartLen = answerStringPart != null ? answerStringPart.length() : 0;
			final boolean answerHasStringPart = answerStringPartLen > 0;

			boolean wasWrongAtLeastOnce = false;
			for (boolean gotItRight = false; !gotItRight;) {
				final String typeIPrompt = getTypeIPrompt(cardIdx, wasWrongAtLeastOnce);
				final int typeIPromptLen = typeIPrompt.length();
				final String terminalString;
				final boolean haveAnswerSound;
				if (!_beSilent) {
					final String answerSoundFileString = card.getSoundFileString(/* clueSide= */false);
					if (answerSoundFileString == null) {
						haveAnswerSound = false;
					} else {
						final File answerSoundFile = _soundFilesMap.get(answerSoundFileString);
						haveAnswerSound = AudioFilePlayer.isValidFile(answerSoundFile);
					}
				} else {
					haveAnswerSound = false;
				}
				if (haveAnswerSound && answerHasStringPart) {
					terminalString = String.format("%s%s%c   ", FlashCardStatics._Sep2, FlashCardStatics._SoundString,
							FlashCardStatics._keyboardSymbol);
				} else if (haveAnswerSound) {
					terminalString = String.format("%s%s  ", FlashCardStatics._Sep2, FlashCardStatics._SoundString);
				} else if (answerHasStringPart) {
					terminalString = String.format("%s%c  ", FlashCardStatics._Sep2, FlashCardStatics._keyboardSymbol);
				} else {
					terminalString = null;
				}
				final int terminalStringLen = terminalString.length();
				final int len1 = typeIPromptLen + FlashCardStatics._Sep1Len + clueStringPartLen + terminalStringLen
						+ Math.min(FlashCardStatics._RoomLen, answerStringPartLen);
				final String[] clueFields = clueStringPart.split(StringUtilities._WhiteSpace);
				final int nClueFields = clueFields.length;
				final String[] answerFields;
				if (!answerHasStringPart) {
					answerFields = new String[0];
				} else {
					answerFields = answerStringPart.split(StringUtilities._WhiteSpace);
				}
				final int nAnswerFields = answerFields.length;
				_needLineFeed = _needLineFeed || len1 > FlashCardStatics._MaxLineLen;
				if (_needLineFeed) {
					System.out.println();
				}
				boolean longQuestion = false;

				/** Expose the clue. */
				if (!_beSilent) {
					final String clueSoundFileString = card.getSoundFileString(/* clueSide= */true);
					if (clueSoundFileString != null) {
						final File clueSoundFile = _soundFilesMap.get(clueSoundFileString);
						AudioFilePlayer.playFileIfValid(clueSoundFile);
					}
				}
				if (len1 <= FlashCardStatics._MaxLineLen) {
					System.out.printf("%s%s%s%s", typeIPrompt, FlashCardStatics._Sep1, clueStringPart, terminalString);
				} else {
					System.out.println(typeIPrompt);
					System.out.print(FlashCardStatics._PrefaceForNewLine);
					int nUsedOnCurrentLine = FlashCardStatics._PrefaceForNewLineLen;
					longQuestion = true;
					/** Break up the clue. */
					for (int k = 0; k < nClueFields; ++k) {
						final String clueField = clueFields[k];
						final int clueFieldLen = clueField.length();
						final boolean justStartedLine = nUsedOnCurrentLine == (k == 0
								? FlashCardStatics._PrefaceForNewLineLen
								: FlashCardStatics._IndentLen);
						if (justStartedLine) {
							System.out.print(clueField);
							nUsedOnCurrentLine += clueFieldLen;
						} else if (nUsedOnCurrentLine + 1 + clueFieldLen
								+ FlashCardStatics._RoomLen >= FlashCardStatics._MaxLineLen) {
							System.out.println();
							System.out.print(FlashCardStatics._IndentString);
							System.out.print(clueField);
							nUsedOnCurrentLine = FlashCardStatics._IndentLen + clueFieldLen;
						} else {
							System.out.printf(" %s", clueField);
							nUsedOnCurrentLine += 1 + clueFieldLen;
						}
					}
					System.out.print(terminalString);
				}
				/** Get the response. */
				final InputString inputString = new InputString(sysInScanner);
				longQuestion = longQuestion || inputString._nLinesOfResponse > 1;
				final String responseStringPart = inputString._inputString;
				final int responseStringPartLen = responseStringPart.length();
				if (responseStringPartLen == 1) {
					/**
					 * <pre>
					 * The response is a command.
					 * quit, restart, edit myProperties, or fall through,
					 * letting this be a bona fide response (such as ở).
					 * </pre>
					 */
					final char char0Uc = Character.toUpperCase(responseStringPart.charAt(0));
					if (char0Uc == FlashCardStatics._QuitChar) {
						System.out.print(FlashCardStatics.getFullYesNoPrompt("Really quit?", true));
						final YesNoResponse yesNoResponse = new YesNoResponse(sysInScanner, /* defaultYesNo= */true);
						if (!yesNoResponse._yesValue) {
							continue OUTSIDE_LOOP;
						}
						keepGoing = false;
						return;
					}
					if (char0Uc == FlashCardStatics._EditPropertiesChar) {
						modifyProperties(sysInScanner);
						continue OUTSIDE_LOOP;
					}
					if (char0Uc == FlashCardStatics._RestartQuizChar) {
						_quizPlus.resetForFullMode();
						restarted = true;
						continue OUTSIDE_LOOP;
					}
					if (char0Uc == FlashCardStatics._ReloadCardsChar) {
						final int oldTci = _quizGenerator._topCardIndex;
						final Card topCard = _cards[oldTci];
						final String trimmedInputString = topCard.getTrimmedInputString(/* clueSide= */true);
						loadCards(/* announceCompleteDups= */false);
						nCards = _cards.length;
						int tci = -1;
						for (int kCard = 0; kCard < nCards; ++kCard) {
							final Card card1 = _cards[kCard];
							if (trimmedInputString.compareToIgnoreCase(card1.getTrimmedInputString(true)) == 0) {
								tci = kCard;
								break;
							}
						}
						_quizGenerator.reactToReloadOfCards(_cards.length, tci);
						continue OUTSIDE_LOOP;
					}
					if (char0Uc == FlashCardStatics._HelpChar) {
						System.out.println();
						System.out.print(FlashCardStatics._HelpString);
						System.out.println();
						continue OUTSIDE_LOOP;
					}
				}

				if (_beSilent && (!clueHasStringPart || !answerHasStringPart)) {
					_needLineFeed = true;
					gotItRight = true;
					continue;
				}
				/** Must process a response to the clue. */
				if (haveAnswerSound) {
					final File answerSideSoundFile = _soundFilesMap.get(card.getSoundFileString(/* forClue= */false));
					AudioFilePlayer.playFileIfValid(answerSideSoundFile);
				}
				if (responseStringPartLen == 0) {
					int nUsedOnCurrentLine = 0;
					/**
					 * User just wants a check so we have to print out the String part of the
					 * answer. Break it up.
					 */
					for (int k = 0; k < nAnswerFields;) {
						final String answerField = answerFields[k];
						final int answerFieldLen = answerField.length();
						if (nUsedOnCurrentLine == 0) {
							System.out.printf("%s%s", FlashCardStatics._IndentString, answerField);
							nUsedOnCurrentLine = FlashCardStatics._IndentLen + answerFieldLen;
							++k;
						} else if (nUsedOnCurrentLine + 1 + answerFieldLen > FlashCardStatics._MaxLineLen) {
							System.out.println();
							nUsedOnCurrentLine = 0;
						} else {
							System.out.printf(" %s", answerField);
							++k;
						}
					}

					if (_mode == Mode.STEP) {
						gotItRight = true;
						_needLineFeed = true;
					} else {
						final boolean defaultYesValue = true;
						final String prompt = FlashCardStatics.getFullYesNoPrompt(FlashCardStatics._CountAsRightString,
								defaultYesValue);
						final int promptLen = prompt.length();
						if (nUsedOnCurrentLine + FlashCardStatics._Sep2Len + promptLen
								+ FlashCardStatics._RoomLen <= FlashCardStatics._MaxLineLen) {
							System.out.printf("%s%s", FlashCardStatics._Sep2, prompt);
						} else {
							System.out.println();
							System.out.printf("%s%s%", FlashCardStatics._IndentString, prompt);
						}
						final YesNoResponse yesNoResponse = new YesNoResponse(sysInScanner, defaultYesValue);
						gotItRight = yesNoResponse._yesValue;
						_needLineFeed = !yesNoResponse._lastLineWasBlank;
						wasWrongAtLeastOnce = wasWrongAtLeastOnce || !gotItRight;
					}
					continue;
				}
				/** User typed in a non-trivial response with at least two characters. */
				final ResponseEvaluator responseEvaluator = new ResponseEvaluator(sysInScanner, _diacriticsTreatment,
						answerStringPart, responseStringPart);
				final String[] diffStrings = responseEvaluator._diffStrings;
				gotItRight = responseEvaluator._gotItRight;
				if (diffStrings != null) {
					/** Because diffStrings is not null, print out the answer, but break it up. */
					int nUsedOnCurrentLine = 0;
					for (int k = 0; k < nAnswerFields;) {
						final String answerField;
						if (gotItRight && k == 0) {
							/** Combine the first answer field with a Heavy Check Mark. */
							answerField = String.format("%c %s", FlashCardStatics._HeavyCheckChar, answerFields[0]);
						} else {
							answerField = answerFields[k];
						}
						final int answerFieldLen = answerField.length();
						if (nUsedOnCurrentLine == 0) {
							System.out.printf("%s%s", FlashCardStatics._IndentString, answerField);
							nUsedOnCurrentLine = FlashCardStatics._IndentLen + answerFieldLen;
							++k;
						} else if (nUsedOnCurrentLine + 1 + answerFieldLen > FlashCardStatics._MaxLineLen) {
							System.out.println();
							nUsedOnCurrentLine = 0;
						} else {
							System.out.printf(" %s", answerField);
							++k;
						}
					}
					/** And now the diffs; consolidate to a single String. */
					final int nDiffStrings = diffStrings.length;
					String fullDiffString = "";
					for (int k = 0; k < nDiffStrings; ++k) {
						fullDiffString += (k == 0 ? "" : " ") + diffStrings[k];
					}
					final int fullDiffStringLen = fullDiffString.length();
					final String fullYesNoPrompt;
					if (gotItRight) {
						fullYesNoPrompt = FlashCardStatics.getFullYesNoPrompt(FlashCardStatics._CountAsRightString,
								true);
					} else {
						fullYesNoPrompt = FlashCardStatics.getFullYesNoPrompt(FlashCardStatics._CountAsRightString,
								false);
					}
					final int fullYesNoPromptLen = fullYesNoPrompt.length();
					/** Put diffString and the prompt onto the current line if there's room. */
					if (nUsedOnCurrentLine + 1 + fullDiffStringLen + FlashCardStatics._Sep2Len + fullYesNoPromptLen
							+ FlashCardStatics._RoomLen <= FlashCardStatics._MaxLineLen) {
						System.out.printf(" %s%s%s", fullDiffString, FlashCardStatics._Sep2, fullYesNoPrompt);
					} else {
						/**
						 * Need a new line, but put diffString and the prompt onto a single line if
						 * there's room.
						 */
						System.out.println();
						if ( //
						FlashCardStatics._IndentLen + fullDiffStringLen + FlashCardStatics._Sep2Len + //
								fullYesNoPromptLen + FlashCardStatics._RoomLen <= //
								FlashCardStatics._MaxLineLen //
						) {
							System.out.printf("%s%s%s%s", FlashCardStatics._IndentString, fullDiffString,
									FlashCardStatics._Sep2, fullYesNoPrompt);
						} else {
							/** Otherwise, separate the diffString and the prompt string. */
							System.out.printf("%s%s", FlashCardStatics._IndentString, fullDiffString);
							System.out.println();
							System.out.printf("%s%s", FlashCardStatics._IndentString, fullYesNoPrompt);
						}
					}
					if (_mode == Mode.STEP) {
						gotItRight = true;
						_needLineFeed = true;
					} else {
						final YesNoResponse yesNoResponse = new YesNoResponse(sysInScanner, gotItRight);
						gotItRight = yesNoResponse._yesValue;
						_needLineFeed = !yesNoResponse._lastLineWasBlank;
					}
				} else {
					/** diffStrings == null. Check for acceptable sound. */
					_needLineFeed = longQuestion;
					if (_mode == Mode.STEP) {
						gotItRight = true;
					} else if (haveAnswerSound) {
						final boolean defaultYesValue = true;
						final String fullPrompt = FlashCardStatics.getFullYesNoPrompt("Strings Match.  Sound OK?",
								defaultYesValue);
						System.out.print(fullPrompt);
						final YesNoResponse yesNoResponse = new YesNoResponse(sysInScanner, defaultYesValue);
						gotItRight = yesNoResponse._yesValue;
						_needLineFeed = true;
					}
				}
				wasWrongAtLeastOnce = wasWrongAtLeastOnce || !gotItRight;
			}
			_quizPlus.reactToRightResponse(wasWrongAtLeastOnce);
		}
	}

	public static void mainx(final String[] args) {
		final String s = "abc]";
		System.out.println(0 <= "(){}[]".indexOf(s.charAt(0)));
		final int sLen = s.length();
		System.out.println(0 <= "(){}[]".indexOf(s.charAt(sLen - 1)));
	}

	public static void main(final String[] args) {
		System.out.printf("SoundString=%s, PenString=%s, IndentString=\"%s\", SpecialChars=\"%s\"",
				FlashCardStatics._SoundString, FlashCardStatics._PenString, FlashCardStatics._IndentString,
				new String(FlashCardStatics._SpecialChars));
		System.out.printf("\n%s\n", FlashCardsGameDirsTracker.getCriticalDirsString());
		int iArg = 0;
		final FlashCardsGame flashCardsGame = new FlashCardsGame(args[iArg++]);
		if (!Mode._DumpCardsAndAbort.contains(flashCardsGame._mode)) {
			try (Scanner sysInScanner = new Scanner(System.in)) {
				flashCardsGame.mainLoop(sysInScanner);
				System.out.println();
				System.out.println("Exiting Program");
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

}
