package com.gcs.game.simulation.slot.engine;

import com.gcs.game.engine.math.model1010802.Model1010802SpinResult;
import com.gcs.game.engine.poker.utils.PokerGameConstant;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.model.IWildPositionsChange;
import com.gcs.game.engine.slots.model.IWildReelsChange;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.engine.slots.vo.SlotSpinResult;
import com.gcs.game.simulation.slot.common.vo.AchievementPay;
import com.gcs.game.simulation.slot.common.vo.AchievementSymbol;
import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import com.gcs.game.simulation.slot.common.vo.SymbolResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.utils.RandomUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jiangqx
 * @create 2020-02-11-14:50
 **/
public class GameEngineCompute {

    public static void computePayTableHit(SlotGameLogicBean gameLogicBean, SlotSpinResult spinResult, BaseResultInfo resultInfo, int[] scatterSymbol) {
        Map<Long, Long> payTableHit = resultInfo.getPayTableHit();
        Map<Long, Long> payTablePayOut = resultInfo.getPayTablePayOut();
        if (spinResult != null) {
            int[] hitLine = spinResult.getHitSlotLines();
            long[] hitAmounts = spinResult.getHitSlotPays();
            int[] hitSymbols = spinResult.getHitSlotSymbols();
            int[] hitMultiplier = spinResult.getHitSlotMuls();
            if (hitLine != null && hitAmounts != null) {
                if (hitLine.length == hitAmounts.length) {
                    for (int i = 0; i < hitAmounts.length; i++) {
                        boolean scatterFlag = isExistScatter(scatterSymbol, hitSymbols[i]);
                        if (scatterFlag) {
                            continue;
                        }
                        int hitMul = hitMultiplier[i];
                        long payWin = hitAmounts[i] / gameLogicBean.getBet() / spinResult.getBaseGameMul();
                        payWin /= hitMul;
                        if (spinResult instanceof Model1010802SpinResult) {
                            payWin = hitAmounts[i] / spinResult.getBaseGameMul();
                        } else if (gameLogicBean.getMmID().equalsIgnoreCase("20260201")) {
                            payWin = (hitAmounts[i] * 2) / gameLogicBean.getBet() / spinResult.getBaseGameMul();
                        }
                        /*if (spinResult instanceof Model8100802SpinResult) {
                            payWin = hitAmounts[i] / gameSessionBean.getBetPerLine();
                            int specialSymbol = getSpecialSymbol(gameSessionBean);
                            if (hitSymbols[i] == specialSymbol) {
                                payWin /= spinResult.getBaseGameMultiplier();
                            }
                        } else if (spinResult instanceof Model1200130SpinResult) {
                            if (hitLine[i] == SlotEngineConstant.SCATTER_HIT_LINE) {
                                continue;
                            }
                        } else if (spinResult instanceof Model1390230SpinResult) {
                            payWin = hitAmounts[i] / gameSessionBean.getBetPerLine() / hitMul;
                        } else if (spinResult instanceof Model1410130SpinResult) {
                            payWin = hitAmounts[i] / gameSessionBean.getBetPerLine() / spinResult.getBaseGameMultiplier();
                            if (resultInfo instanceof DragonDeluxe125xResultInfo) {
                                if (hitLine[i] > 8) {
                                    continue;
                                }
                                payWin = hitAmounts[i] / gameSessionBean.getBetPerLine() / hitMul;
                            }
                        } else */
                        if (gameLogicBean.isRespin() && spinResult.getFsMul() > 1) {
                            payWin /= spinResult.getFsMul();
                        }
                        if (payWin > 0) {
                            if (payTableHit.containsKey(payWin)) {
                                long hitCount = payTableHit.get(payWin) + 1;
                                payTableHit.put(payWin, hitCount);
                            } else {
                                payTableHit.put(payWin, 1L);
                            }
                            if (payTablePayOut.containsKey(payWin)) {
                                long winAmount = payTablePayOut.get(payWin) + hitAmounts[i];
                                payTablePayOut.put(payWin, winAmount);
                            } else {
                                payTablePayOut.put(payWin, hitAmounts[i]);
                            }
                        }
                    }
                } else {
                    throw new IllegalArgumentException("baseGame hit information error!");
                }
            }
        }

    }

    private static int getSpecialSymbol(SlotGameLogicBean gameLogicBean) {
        //TODO
        int specialSymbol = 1;
        Map<String, String> specialSettingsMap = null;
        if (specialSettingsMap != null && specialSettingsMap.containsKey("specialSymbol")) {
            specialSymbol = Integer.parseInt(specialSettingsMap.get("specialSymbol"));
        }
        return specialSymbol;
    }

    private static boolean isExistScatter(int[] scatterSymbol, int symbolNo) {
        if (scatterSymbol != null && scatterSymbol.length > 0) {
            for (int scatterNo : scatterSymbol) {
                if (symbolNo == scatterNo) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void initPayTableHit(long[][] payTable, BaseResultInfo resultInfo) {
        Map<Long, Long> payTableHit = new HashMap<Long, Long>();
        Map<Long, Long> payTablePayOut = new HashMap<Long, Long>();
        if (payTable != null) {
            for (int i = 0; i < payTable.length; i++) {
                for (int j = 0; j < payTable[i].length; j++) {
                    long pay = payTable[i][j];
                    if (pay > 0) {
                        payTableHit.put(pay, 0L);
                        payTablePayOut.put(pay, 0L);
                    }
                }
            }
            resultInfo.setPayTableHit(payTableHit);
            resultInfo.setPayTablePayOut(payTablePayOut);
        }
    }

    public static void addFreeSpinSymbolDetailInfo(SlotSpinResult spinResult, BaseResultInfo resultInfo) {
        if (spinResult != null) {
            int[] hitSymbols = spinResult.getHitSlotSymbols();
            long[] hitAmounts = spinResult.getHitSlotPays();
            int[] hitSymbolCount = spinResult.getHitSlotSymbolCount();
            List<SymbolResultInfo> fsSymbolInfoList = resultInfo.getFsSymbolInfoList();
            if (hitSymbols != null && hitAmounts != null) {
                if (hitSymbols.length == hitAmounts.length) {
                    for (int i = 0; i < hitSymbols.length; i++) {
                        int symbolNo = hitSymbols[i];
                        int symbolCount = hitSymbolCount[i];
                        if (symbolNo <= fsSymbolInfoList.size()) {
                            SymbolResultInfo symbolInfo = fsSymbolInfoList.get(symbolNo - 1);
                            symbolInfo.getHitPayCount()[symbolCount - 1]++;
                            symbolInfo.getHitPayAmount()[symbolCount - 1] += hitAmounts[i];
                            symbolInfo.setTotalHit(symbolInfo.getTotalHit() + 1);
                            symbolInfo.setTotalAmount(symbolInfo.getTotalAmount() + hitAmounts[i]);
                        } else {
                            continue;
                        }
                    }
                } else {
                    throw new IllegalArgumentException("hitSymbols.length!=hitAmounts.length error");
                }
            }
        }
    }

    public static int[][] extractHorizontalSymbols(int[] displaySymbols, int reelsCount, int rowsCount) {
        if (displaySymbols != null && displaySymbols.length > 0) {
            int[][] symbols = new int[rowsCount][reelsCount];
            for (int i = 0; i < rowsCount; i++) {
                for (int j = 0; j < reelsCount; j++) {
                    symbols[i][j] = displaySymbols[i * reelsCount + j];
                }
            }
            return symbols;
        } else {
            return null;
        }
    }

    public static void computeBaseAchievement(SlotGameLogicBean gameLogicBean, BaseSlotModel model, BaseResultInfo resultInfo, SlotConfigInfo configInfo) {
        computePaySpinCount(resultInfo, configInfo);
        long[][] payTables = null;
        int minWildSymbolIndex = 0;
        int reelsCount = BaseConstant.REELS_COUNT;
        int rowsCount = BaseConstant.ROWS_COUNT;
        int wildSymbolNo = BaseConstant.WILD_SYMBOL;
        if (model instanceof IWildReelsChange) {
            wildSymbolNo = ((IWildReelsChange) model).wildSymbolNo();
        }
        if (model instanceof IWildPositionsChange) {
            wildSymbolNo = ((IWildPositionsChange) model).wildSymbolNo();
        }
        if (model instanceof IBaseReelsDefaultConfig) {
            payTables = ((IBaseReelsDefaultConfig) model).getPayTable();
            reelsCount = ((IBaseReelsDefaultConfig) model).getReelsCount();
            rowsCount = ((IBaseReelsDefaultConfig) model).getRowsCount();
            long[] payAmount = payTables[wildSymbolNo - 1];
            for (int i = 0; i < payAmount.length; i++) {
                if (payAmount[i] > 0) {
                    minWildSymbolIndex = i + 1;
                    break;
                }
            }
        }

        if (configInfo.getPlayGameCount() == 1) {
            SlotSpinResult spinResult = gameLogicBean.getSlotSpinResult();
            if (spinResult != null) {
                int[] displaySymbols = getDisplaySymbols(spinResult, reelsCount, rowsCount, wildSymbolNo);
                long[] hitAmounts = spinResult.getHitSlotPays();
                int[] hitSymbols = spinResult.getHitSlotSymbolsSound();
                Map<Integer, List<Integer>> winSymbolMap = new HashMap<Integer, List<Integer>>();
                if (hitSymbols != null && hitAmounts != null) {
                    if (hitSymbols.length == hitAmounts.length) {
                        for (int i = 0; i < hitAmounts.length; i++) {
                            int symbolNo = hitSymbols[i];
                            long symbolWin = hitAmounts[i];
                            int symbolCount = spinResult.getHitSlotSymbolCount()[i];
                            int[] hitPositions = spinResult.getHitSlotPositions()[i];
                            boolean isContainWild = false;
                            int wildSymbolCount = 0;
                            //special wild symbol handle
                            if (symbolWin > 0) {
                                List<Integer> winSymbolList = new ArrayList<Integer>();
                                for (int position : hitPositions) {
                                    if (position == 0) {
                                        continue;
                                    }
                                    winSymbolList.add(displaySymbols[position - 1]);
                                    if (displaySymbols[position - 1] != symbolNo) {
                                        isContainWild = true;
                                    }
                                }
                                // win symbol: W_01 W_01 W_01 S_02 statistics to wild symbol achievement
                                if (winSymbolList != null && !winSymbolList.isEmpty()) {
                                    for (int symbol : winSymbolList) {
                                        if (symbol != symbolNo) {
                                            wildSymbolCount++;
                                        } else {
                                            break;
                                        }
                                    }
                                }
                            }
                            //win symbol: W_01 W_01 W_01 S_02 statistics to wild symbol achievement
                            if (isContainWild && (minWildSymbolIndex <= 0 || (minWildSymbolIndex > 0 && wildSymbolCount < minWildSymbolIndex))) {
                                continue;
                            } else if (minWildSymbolIndex > 0 && wildSymbolCount >= minWildSymbolIndex) {
                                symbolNo = BaseConstant.WILD_SYMBOL;
                                symbolCount = wildSymbolCount;
                            }
                            if (symbolCount > 0) {
                                isComputeBaseAchievement(resultInfo, winSymbolMap, symbolNo, symbolCount, configInfo);
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("hitSymbols.length!=hitAmounts.length error");
                    }
                }
            }

        }

    }

    private static void isComputeBaseAchievement(BaseResultInfo resultInfo, Map<Integer, List<Integer>> winSymbolMap, int symbolNo, int symbolCount, SlotConfigInfo configInfo) {
        boolean isRepeatPayCount = false;
        if (winSymbolMap.containsKey(symbolNo)) {
            List<Integer> list = winSymbolMap.get(symbolNo);
            if (list.contains(symbolCount)) {
                isRepeatPayCount = true;
            } else {
                list.add(symbolCount);
                winSymbolMap.put(symbolNo, list);
            }
        } else {
            List<Integer> symbolCountList = new ArrayList<Integer>();
            symbolCountList.add(symbolCount);
            winSymbolMap.put(symbolNo, symbolCountList);
        }
        if (!isRepeatPayCount) {
            getBaseAchievement(resultInfo, symbolNo, symbolCount, configInfo);
        }
    }

    /**
     * @param resultInfo
     * @param symbolNo
     * @param symbolCount
     * @param configInfo
     * @return
     * @author Jiangqx
     * @Date 15:34 2020/2/18
     */
    private static void getBaseAchievement(BaseResultInfo resultInfo, int symbolNo, int symbolCount, SlotConfigInfo configInfo) {
        List<AchievementSymbol> achievementSymbolList = resultInfo.getBaseAchievementSymbol();
        for (AchievementSymbol achievementSymbol : achievementSymbolList) {
            if (achievementSymbol.getSymbolNo() == symbolNo) {
                List<AchievementPay> achievementPayList = achievementSymbol.getAchievementPayList();
                if (symbolCount > 0 && achievementPayList.size() >= symbolCount) {
                    AchievementPay achievementPay = achievementPayList.get(symbolCount - 1);
                    if (achievementPay.getPaySpinCount() > 0) {
                        achievementPay.getPayAchievement().add(achievementPay.getPaySpinCount());
                        achievementPay.setPayCacheCount(achievementPay.getPaySpinCount());
                        achievementPay.setPaySpinCount(0L);
                    }
                } else {
                    throw new IllegalStateException(
                            "symbolCount < 1 or symbolCount > 5("
                                    + symbolCount + ")");
                }
                computeTotalAchievement(achievementSymbol, resultInfo, configInfo);
                break;
            }
        }

    }

    private static void computeTotalAchievement(AchievementSymbol achievementSymbol, BaseResultInfo resultInfo, SlotConfigInfo configInfo) {
        int[] achievementPay = configInfo.getAchievementPay();
        if (achievementPay != null) {
            int size = achievementPay.length;
            int tempSize = 0;
            for (int pay : achievementPay) {
                if (achievementSymbol.getAchievementPayList().get(pay - 1).getPayCacheCount() > 0) {
                    tempSize++;
                }
            }
            if (tempSize == size) {
                achievementSymbol.getPayTotalAchievement().add(achievementSymbol.getPayTotalSpinCount());
                achievementSymbol.setPayTotalSpinCount(0L);
                for (int pay : achievementPay) {
                    achievementSymbol.getAchievementPayList().get(pay - 1).setPayCacheCount(0L);
                }
            }
        }

    }


    private static int[] getDisplaySymbols(SlotSpinResult spinResult, int reelsCount, int rowsCount, int wildSymbolNo) {
        int[] oldDisplaySymbols = spinResult.getSlotDisplaySymbols();
        int[] wildReels = spinResult.getSlotWildReels();
        int[] wildPositions = spinResult.getSlotWildPositions();
        int[] displaySymbols = oldDisplaySymbols.clone();
        if (wildReels != null) {
            for (int wildReelsIndex : wildReels) {
                for (int i = 0; i < rowsCount; i++) {
                    displaySymbols[wildReelsIndex + i * reelsCount] = wildSymbolNo;
                }
            }
        }
        if (wildPositions != null && wildPositions.length > 0) {
            for (int position : wildPositions) {
                displaySymbols[position] = wildSymbolNo;
            }
        }
        return displaySymbols;
    }

    private static void computePaySpinCount(BaseResultInfo resultInfo, SlotConfigInfo configInfo) {
        List<AchievementSymbol> achievementSymbolList = resultInfo.getBaseAchievementSymbol();
        int[] configPay = configInfo.getAchievementPay();
        if (achievementSymbolList != null && !achievementSymbolList.isEmpty()) {
            for (AchievementSymbol symbol : achievementSymbolList) {
                if (configPay != null) {
                    for (int pay : configPay) {
                        AchievementPay achievementPay = symbol.getAchievementPayList().get(pay - 1);
                        achievementPay.setPaySpinCount(achievementPay.getPaySpinCount() + 1);
                    }
                }
                symbol.setPayTotalSpinCount(symbol.getPayTotalSpinCount() + 1);
            }
        }
    }

    /**
     * @param length
     * @param bonusChoiceIndex
     * @return
     * @author Jiangqx
     * @Description pick bonus
     * @Date 9:55 2020/3/6
     */
    public static int[] initArray(int length, int bonusChoiceIndex) {
        if (length > 0) {
            int[] result = new int[length];
            for (int i = 0; i < length; i++) {
                if (i == 0) {
                    result[i] = 0;
                    //choice bonus
                    if (bonusChoiceIndex > 0) {
                        result[i] = bonusChoiceIndex;
                    }
                } else {
                    result[i] = i - 1;
                    //result[i] = i;
                }
            }
            return result;
        } else {
            return null;
        }

    }

    /**
     * @param length(pick length)
     * @param cardList
     * @return pick index
     * @author Jiangqx
     * @Description poke high or low bonus
     * @Date 9:56 2020/3/6
     */
    public static int[] computePokeCardPickIndex(int length, int[] cardList) {
        if (cardList != null && cardList.length > 0) {
            int[] result = new int[length];
            int half = 13 / 2 + 1;
            for (int i = 0; i < length; i++) {
                int prevCard = cardList[i] % 13 == 0 ? 13 : cardList[i] % 13;
                if (prevCard > half) {
                    result[i] = 0;
                } else {
                    result[i] = 1;
                }
            }
            return result;
        } else {
            return null;
        }
    }

    public static int getSymbolReelCount(int[] symbols, int reelsCount, int rowsCount, int symbolNo, int reelIndex) {
        int count = 0;
        for (int i = 0; i < rowsCount; i++) {
            if (symbols[reelsCount * i + reelIndex] == symbolNo) {
                count++;
            }
        }
        return count;
    }

    public static int getSymbolCount(int[] symbols, int symbolNo) {
        int count = 0;
        if (symbols != null) {
            for (int symbol : symbols) {
                if (symbol == symbolNo) {
                    count++;
                }
            }
        }
        return count;
    }

    public static int[] initPokersArray(int len, List<Integer> lastCardList) {
        int[] result = null;
        if (len > 0) {
            result = new int[len];
            if (lastCardList != null && !lastCardList.isEmpty() && lastCardList.size() == len - 1) {
                for (int i = 0; i < len - 1; i++) {
                    int prevCard = lastCardList.get(i) / 13;
                    if (prevCard == PokerGameConstant.HEARTS_CARD || prevCard == PokerGameConstant.DIAMONDS_CARD) {
                        result[i] = 0;
                    } else if (prevCard == PokerGameConstant.SPADE_CARD || prevCard == PokerGameConstant.CLUBS_CARD) {
                        result[i] = 1;
                    }
                }
            }
            result[len - 1] = RandomUtil.getRandomInt(2);
        }
        return result;
    }
}
