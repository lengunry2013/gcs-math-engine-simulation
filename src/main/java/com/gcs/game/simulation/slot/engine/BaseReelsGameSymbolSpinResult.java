package com.gcs.game.simulation.slot.engine;

import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.utils.SlotEngineConstant;
import com.gcs.game.engine.slots.vo.SlotBonusResult;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.engine.slots.vo.SlotSpinResult;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.simulation.slot.common.vo.SymbolResultInfo;
import com.gcs.game.simulation.slot.vo.BaseReelsDetailResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BaseGame symbol spin result information
 *
 * @author Jiangqx
 * @create 2020-03-04-17:14
 **/
@Slf4j
public class BaseReelsGameSymbolSpinResult extends LittleDragonBunsSpinResult {

    private static final String PLAYER_INFO_FILE = "playerInfoResult.txt";
    //private BufferedWriter playerInfoResultWriter = null;
    public static final String TAB_STR = "\t";

    public BaseReelsGameSymbolSpinResult() {

    }

    public void cycleSpinForBaseReelsGameSymbol(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel baseSlotModel) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            int outputInfoType = configInfo.getOutputType();
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            SlotGameLogicBean gameLogicBean = (SlotGameLogicBean) baseGameLogicBean;

            BaseReelsDetailResultInfo resultInfo = new BaseReelsDetailResultInfo();
            int[] maxWin = slotConfigInfo.getMaxWin();
            int maxSpinNumForEveryPlayer = slotConfigInfo.getMaxSpinNumForEveryPlayer();
            if (maxWin != null && outputInfoType == BaseConstant.PLAYER_SPIN_OUTPUT_TYPE) {
                long[] collectionCountsForMaximumWin = new long[maxWin.length + 1];
                resultInfo.setCollectionCountsForMaximumWin(collectionCountsForMaximumWin);
                resultInfo.setPeakBankrollForEveryPlayer(initCredit);
                initPlayerInfoWriter(slotConfigInfo);
            }
            initLineInfo(resultInfo, gameLogicBean);
            initSymbolResultInfo(resultInfo, baseSlotModel);
            //start simulation
            for (int i = 0; i < simulationCount; i++) {
                long totalWon = 0L;
                Map gameLogicMap = new LinkedHashMap();
                gameLogicMap.put("lines", slotConfigInfo.getLines());
                gameLogicMap.put("bet", slotConfigInfo.getBet());
                gameLogicMap.put("denom", slotConfigInfo.getDenom());
                /*if (model instanceof Model1440130Test) {
                    KaiJuBattleSpinResult.setSpecialSymbol(gameSessionBean, resultInfo);
                }*/
                long totalBet = gameLogicBean.getSumBetCredit();
                if (outputInfoType == BaseConstant.PLAYER_SPIN_OUTPUT_TYPE) {
                    resultInfo.setRunningBankForEveryPlayer(initCredit);
                    if (resultInfo.getPeakBankrollForEveryPlayer() < initCredit) {
                        resultInfo.setPeakBankrollForEveryPlayer(initCredit);
                    }
                    if (resultInfo.getSpinCountsForEveryPlayer() >= maxSpinNumForEveryPlayer || initCredit < totalBet) {
                        initCredit = changePlayer(resultInfo, slotConfigInfo);
                    }
                }
                //star baseGame spin
                gameLogicBean = (SlotGameLogicBean) engine.gameStart(gameLogicBean, gameLogicMap, null);
                //special game
                /*if ((model instanceof Model1220130Test && Model1220130Test.isFsRespin) ||
                        (model instanceof Model1220230Test && Model1220230Test.isFsRespin)) {
                    if (gameSessionBean.getTotalWinCredit() == 0) {
                        i--;
                        continue;
                    }
                }*/

                spinCount++;
                resultInfo.setSpinCountsForEveryPlayer(resultInfo.getSpinCountsForEveryPlayer() + 1);
                initCredit -= totalBet;

                resultInfo.setSpinCount(spinCount);
                resultInfo.setBetPerLine((int) gameLogicBean.getBet());
                resultInfo.setLine((int) gameLogicBean.getLines());
                resultInfo.setTotalCoinIn(resultInfo.getTotalCoinIn() + totalBet);
                resultInfo.setReelsStop(StringUtil.IntegerArrayToStr(gameLogicBean.getSlotSpinResult().getSlotReelStopPosition(), " "));
                resultInfo.setPreScene(gameLogicBean.getLastScenes());
                resultInfo.setCurrentScene(gameLogicBean.getLastScenes());
                resultInfo.setNextScene(gameLogicBean.getNextScenes());
                computeHitInfo(gameLogicBean.getSlotSpinResult(), resultInfo, slotConfigInfo, baseSlotModel);
                resultInfo.setRangeTotalCoinIn(resultInfo.getRangeTotalCoinIn() + totalBet);
                long winCredit = gameLogicBean.getSumWinCredit();
                totalWon += winCredit;

                if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                    if (winCredit > 0) {
                        resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                        resultInfo.setBaseGameTotalWin(
                                resultInfo.getBaseGameTotalWin() + winCredit);
                    }
                } else {
                    resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                    if (winCredit > 0) {
                        resultInfo.setBaseGameTotalWin(
                                resultInfo.getBaseGameTotalWin() + winCredit);
                    }
                    boolean isBaseTriggerRespin = false;
                    if (gameLogicBean.getRespinCountsLeft() > 0) {
                        isBaseTriggerRespin = true;
                    }
                    //start freespin or bonus
                    long fsCoinOut = 0L;
                    long fsTotalTimes = 0L;
                    while (true) {
                        if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                            while (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                                PlayerInputInfo playerInput = new PlayerInputInfo();
                                playerInput.setRequestGameStatus(200);
                                gameLogicBean = (SlotGameLogicBean) engine.gameProgress(gameLogicBean, gameLogicMap, playerInput, null, null);

                                SlotSpinResult fsSpinResult = null;
                                long freespinWon = 0L;
                                if (slotConfigInfo.getPlayGameCount() == 1) {
                                    fsSpinResult = gameLogicBean.getSlotFsSpinResults().get(gameLogicBean.getSlotFsSpinResults().size() - 1);
                                    freespinWon = fsSpinResult.getSlotPay();
                                } else if (slotConfigInfo.getPlayGameCount() > 1) {
                                    List<List<SlotSpinResult>> fsSpinResults = gameLogicBean.getSlotFsSpinResults4Multi();
                                    for (List<SlotSpinResult> spinResultList : fsSpinResults) {
                                        if (spinResultList != null && spinResultList.get(spinResultList.size() - 1) != null) {
                                            freespinWon += spinResultList.get(spinResultList.size() - 1).getSlotPay();
                                        }
                                    }
                                }
                                //TODO Respin
                                totalWon += freespinWon;
                                //baseGame respin model
                                //TODO 仅仅为了计算baseGame
                                /*if (isBaseTriggerRespin && fsSpinResult.getSpinType() == SlotEngineConstant.SPIN_TYPE_RESPIN_IN_BASE_GAME) {
                                    computeHitInfo(fsSpinResult, resultInfo, slotConfigInfo, baseSlotModel);
                                } else {
                                    fsCoinOut += freespinWon;
                                    fsTotalTimes++;
                                }*/
                                fsCoinOut += freespinWon;
                                fsTotalTimes++;
                            }
                            if (fsTotalTimes > 0) {
                                resultInfo.setBonusPlaysCountForEveryPlayer(resultInfo.getBonusPlaysCountForEveryPlayer() + 1);
                                resultInfo.setFreespinTotalTimes(resultInfo.getFreespinTotalTimes()
                                        + fsTotalTimes);
                                resultInfo.setFreespinTotalHit(
                                        resultInfo.getFreespinTotalHit() + 1);
                                resultInfo.setFreespinTotalWin(
                                        resultInfo.getFreespinTotalWin()
                                                + fsCoinOut);
                            }

                        } else if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_BONUS) {
                            PlayerInputInfo playerInput = new PlayerInputInfo();
                            playerInput.setRequestGameStatus(500);
                            //bonusChoice random Index
                            int bonusChoiceIndex = slotConfigInfo.getChoiceFsOrBonusIndex();
                            if (slotConfigInfo.isRandomBonusChoice() && bonusChoiceIndex > 0) {
                                bonusChoiceIndex = RandomUtil.getRandomInt(bonusChoiceIndex);
                            }

                            for (int pick = 0; pick < 100; pick++) {
                                if (pick > 0) {
                                    int[] picks = GameEngineCompute.initArray(pick, bonusChoiceIndex);
                                    playerInput.setBonusPickInfos(picks);
                                }
                                gameLogicBean = (SlotGameLogicBean) engine.gameProgress(gameLogicBean, gameLogicMap, playerInput, null, null);

                                SlotBonusResult baseBonusResult = gameLogicBean.getSlotBonusResult();
                                if (baseBonusResult.getBonusPlayStatus() == 1000) {
                                    long bonusWon = baseBonusResult.getTotalPay();
                                    totalWon += bonusWon;
                                    if (bonusWon > 0) {
                                        resultInfo.setBonusPlaysCountForEveryPlayer(resultInfo.getBonusPlaysCountForEveryPlayer() + 1);
                                        resultInfo.setBonusTotalHit(resultInfo.getBonusTotalHit() + 1);
                                        resultInfo.setBonusTotalWin(resultInfo.getBonusTotalWin() + bonusWon);
                                    }
                                    break;
                                }
                            }

                        } else if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                            break;
                        }

                    }

                }
                if (outputInfoType == BaseConstant.PLAYER_SPIN_OUTPUT_TYPE) {
                    computeMaxWinCount(resultInfo, slotConfigInfo, totalWon, totalBet);
                }
                if (totalWon > 0) {
                    resultInfo.setTotalHit(resultInfo.getTotalHit() + 1);
                }
                resultInfo.setTotalCoinOut(
                        resultInfo.getTotalCoinOut() + totalWon);
                resultInfo.setRangeTotalCoinOut(resultInfo.getRangeTotalCoinOut() + totalWon);
                resultInfo.setTotalAmount(totalWon);
                resultInfo.setWinIncremental(totalWon);
                initCredit += totalWon;
                resultInfo.setLeftCredit(initCredit);

                if (spinCount > 0 && spinCount % playTime == 0) {
                    outResultInfo(slotConfigInfo, resultInfo);
                    resultInfo.setRangeTotalCoinIn(0L);
                    resultInfo.setRangeTotalCoinOut(0L);
                }
            }
            if (outputInfoType == BaseConstant.PLAYER_SPIN_OUTPUT_TYPE) {
                printCurrentPlayerInfo(resultInfo, slotConfigInfo);
            }

            //FileWriteUtil.closeFile();
            //FileWriteUtil.closeFsFile(playerInfoResultWriter);
        } catch (InvalidGameStateException e) {
            log.error("engine gameStart", e);
            e.printStackTrace();
        } catch (Exception e) {
            log.error("cycleSpinForBaseReelsGameSymbol run exception", e);
        }
    }

    private void computeMaxWinCount(BaseReelsDetailResultInfo resultInfo, SlotConfigInfo configInfo, long totalWin, long totalBet) {
        if (resultInfo.getCollectionCountsForMaximumWin() != null) {
            int[] maxWin = configInfo.getMaxWin();
            for (int i = 0; i < resultInfo.getCollectionCountsForMaximumWin().length; i++) {
                if (i < maxWin.length && totalWin < (maxWin[i] * totalBet)) {
                    resultInfo.getCollectionCountsForMaximumWin()[i] += 1;
                    break;
                } else if (i == maxWin.length) {
                    resultInfo.getCollectionCountsForMaximumWin()[i] += 1;
                    break;
                }
            }
        }

    }

    private void initPlayerInfoWriter(SlotConfigInfo configInfo) {
        String fileName = configInfo.getOutputPath() + PLAYER_INFO_FILE;
        configInfo.setFsBonusFileName(fileName);
        FileWriteUtil.createNewFile(fileName);
        //playerInfoResultWriter = FileWriteUtil.initFreeSpinWriteFile(fileName, playerInfoResultWriter);
    }

    private long changePlayer(BaseReelsDetailResultInfo resultInfo, SlotConfigInfo configInfo) {
        // print log to file
        printCurrentPlayerInfo(resultInfo, configInfo);
        // reset cache parameters
        long initCredit = resetCachePlayerInfo(resultInfo, configInfo);
        return initCredit;
    }

    private long resetCachePlayerInfo(BaseReelsDetailResultInfo resultInfo, SlotConfigInfo configInfo) {
        long initCredit = configInfo.getInitCredit();
        resultInfo.setPlayerNumber(resultInfo.getPlayerNumber() + 1);
        resultInfo.setSpinCountsForEveryPlayer(0L);
        resultInfo.setRunningBankForEveryPlayer(initCredit);
        resultInfo.setBonusPlaysCountForEveryPlayer(0L);
        resultInfo.setPeakBankrollForEveryPlayer(initCredit);
        if (configInfo.getMaxWin() != null) {
            long[] collectionCountsForMaximumWin = new long[configInfo.getMaxWin().length + 1];
            resultInfo.setCollectionCountsForMaximumWin(collectionCountsForMaximumWin);
        }
        return initCredit;
    }

    private void printCurrentPlayerInfo(BaseReelsDetailResultInfo resultInfo, SlotConfigInfo configInfo) {
        StringBuffer sb = new StringBuffer();
        sb.append(resultInfo.getPlayerNumber()).append(TAB_STR);
        sb.append("Games played").append(TAB_STR);
        sb.append(resultInfo.getSpinCountsForEveryPlayer()).append(TAB_STR);
        sb.append("Running Bank").append(TAB_STR);
        sb.append(resultInfo.getRunningBankForEveryPlayer()).append(TAB_STR);
        sb.append("Bonus plays").append(TAB_STR);
        sb.append(resultInfo.getBonusPlaysCountForEveryPlayer()).append(TAB_STR);
        sb.append("Max Cum Win").append(TAB_STR);
        sb.append(resultInfo.getPeakBankrollForEveryPlayer()).append(TAB_STR);
        if (resultInfo.getCollectionCountsForMaximumWin() != null) {
            for (int i = 0; i < resultInfo.getCollectionCountsForMaximumWin().length; i++) {
                if (i == resultInfo.getCollectionCountsForMaximumWin().length - 1) {
                    sb.append(">= ");
                    sb.append(configInfo.getMaxWin()[i - 1] + "X Bet");
                    sb.append(TAB_STR);
                } else {
                    sb.append("< ");
                    sb.append(configInfo.getMaxWin()[i] + "X Bet");
                    sb.append(TAB_STR);
                }
                sb.append(resultInfo.getCollectionCountsForMaximumWin()[i]);
                sb.append(TAB_STR);
            }
        }
        FileWriteUtil.outputPrint(sb.toString(), configInfo.getFsBonusFileName(), configInfo, 3);
    }


    private void initSymbolResultInfo(BaseReelsDetailResultInfo resultInfo, BaseSlotModel model) {
        int reelsCount = BaseConstant.REELS_COUNT;
        if (model instanceof IBaseReelsDefaultConfig) {
            reelsCount = ((IBaseReelsDefaultConfig) model).getReelsCount();
        }
        List<SymbolResultInfo> symbolInfoList = new ArrayList<SymbolResultInfo>();
        for (int i = 0; i < BaseConstant.SYMBOL_COUNT; i++) {
            SymbolResultInfo symbolInfo = new SymbolResultInfo(i + 1);
            long[] hitCount = new long[reelsCount];
            long[] hitAmount = new long[reelsCount];
            symbolInfo.setHitPayCount(hitCount);
            symbolInfo.setHitPayAmount(hitAmount);
            symbolInfoList.add(symbolInfo);
        }
        resultInfo.setSymbolResultInfoList(symbolInfoList);
    }

    private void initLineInfo(BaseReelsDetailResultInfo resultInfo, SlotGameLogicBean gameSessionBean) {
        long line = gameSessionBean.getLines();
        List<Long> winTimesPerLineList = new ArrayList<>();
        List<Long> winAmountPerLineList = new ArrayList<>();
        for (int i = 0; i < line; i++) {
            winTimesPerLineList.add(0L);
            winAmountPerLineList.add(0L);
        }
        resultInfo.setWinTimesPerLineList(winTimesPerLineList);
        resultInfo.setWinAmountPerLineList(winAmountPerLineList);
    }

    private void computeHitInfo(SlotSpinResult spinResult, BaseReelsDetailResultInfo resultInfo, SlotConfigInfo configInfo, BaseSlotModel model) {
        if (configInfo.getPlayGameCount() == 1) {
            resultInfo.setWinSymbols("0");
            resultInfo.setWinLine("0");
            resultInfo.setWinPays("0");
            resultInfo.setWinLinePosition("0");
            /*if (model instanceof Model1380430Test) {
                return;
            }*/
            int reelsCount = BaseConstant.REELS_COUNT;
            if (model instanceof IBaseReelsDefaultConfig) {
                reelsCount = ((IBaseReelsDefaultConfig) model).getReelsCount();
            }
            int lineNumber4R2L = model.getCardinalLineNumber4R2L();
            if (spinResult != null) {
                int[] hitSymbols = spinResult.getHitSlotSymbolsSound();
                int[] hitLines = spinResult.getHitSlotLines();
                long[] hitAmounts = spinResult.getHitSlotPays();
                int[][] hitPositions = spinResult.getHitSlotPositions();
                int[] hitSymbolCount = spinResult.getHitSlotSymbolCount();
                if (hitSymbols != null && hitLines != null && hitAmounts != null && hitPositions != null) {
                    resultInfo.setWinSymbols(StringUtil.IntegerArrToString(hitSymbols, "*"));
                    resultInfo.setWinLine(StringUtil.IntegerArrToString(hitLines, "*"));
                    resultInfo.setWinPays(StringUtil.LongArrToString(hitAmounts, "*"));
                    resultInfo.setWinLinePosition(StringUtil.arrayToString(hitPositions, "*"));
                    List<SymbolResultInfo> symbolInfoList = resultInfo.getSymbolResultInfoList();
                    for (int i = 0; i < hitLines.length; i++) {
                        int line = hitLines[i];
                        int symbolNo = hitSymbols[i];
                        int symbolCount = hitSymbolCount[i];
                        int hitType = 1; //hit line
                        if (line >= SlotEngineConstant.SCATTER_HIT_LINE) {
                            hitType = 2; //hit scatter
                        }
                        if (hitType == 1 && line > resultInfo.getWinTimesPerLineList().size() && lineNumber4R2L > 0) {
                            line -= lineNumber4R2L;
                        }
                        if (symbolCount > reelsCount) {
                            continue;
                        }
                        if (hitType == 1) {
                            long timesPerLine = resultInfo.getWinTimesPerLineList().get(line - 1) + 1;
                            long winAmountPerLine = resultInfo.getWinAmountPerLineList().get(line - 1) + hitAmounts[i];
                            resultInfo.getWinTimesPerLineList().set(line - 1, timesPerLine);
                            resultInfo.getWinAmountPerLineList().set(line - 1, winAmountPerLine);
                        }
                        SymbolResultInfo symbolInfo = symbolInfoList.get(symbolNo - 1);
                        symbolInfo.getHitPayCount()[symbolCount - 1]++;
                        symbolInfo.getHitPayAmount()[symbolCount - 1] += hitAmounts[i];
                        symbolInfo.setTotalHit(symbolInfo.getTotalHit() + 1);
                        symbolInfo.setTotalAmount(symbolInfo.getTotalAmount() + hitAmounts[i]);
                    }
                }
            }
        }
    }

    private void outResultInfo(SlotConfigInfo configInfo, BaseReelsDetailResultInfo resultInfo) {
        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append("Num of spin").append(TAB_STR);
            strbHeader.append("Location").append(TAB_STR);
            strbHeader.append("ReelsStop").append(TAB_STR);
            strbHeader.append("Left Credit").append(TAB_STR);
            strbHeader.append("Win Incremental").append(TAB_STR);
            strbHeader.append("Line").append(TAB_STR);
            strbHeader.append("BetPerLine").append(TAB_STR);
            strbHeader.append("Pre Scene").append(TAB_STR);
            strbHeader.append("Current Scene").append(TAB_STR);
            strbHeader.append("Next Scene").append(TAB_STR);
            strbHeader.append("Win Lines").append(TAB_STR);
            strbHeader.append("Win Symbols").append(TAB_STR);
            strbHeader.append("Wins Pays").append(TAB_STR);
            strbHeader.append("Win Line Positions").append(TAB_STR);
            strbHeader.append("TotalWin Per Spin").append(TAB_STR);
            strbHeader.append("Left freeSpin").append(TAB_STR);
            strbHeader.append("TotalCoin In").append(TAB_STR);
            strbHeader.append("TotalCoin Out").append(TAB_STR);
            strbHeader.append("RTP").append(TAB_STR);
            strbHeader.append("Coin In Per Period").append(TAB_STR);
            strbHeader.append("Coin Out Per Period").append(TAB_STR);
            strbHeader.append("RTP Per Period").append(TAB_STR);
            strbHeader.append("Bonus").append(TAB_STR);
            strbHeader.append("Bonus Hit Rate").append(TAB_STR);
            strbHeader.append("Bonus Payback").append(TAB_STR);
            strbHeader.append("FreeSpin").append(TAB_STR);
            strbHeader.append("FreeSpin Hit Rate").append(TAB_STR);
            strbHeader.append("FreeSpin Payback").append(TAB_STR);
            for (int i = 0; i < resultInfo.getSymbolResultInfoList().size(); i++) {
                SymbolResultInfo symbolInfo = resultInfo.getSymbolResultInfoList().get(i);
                int symbolNo = symbolInfo.getSymbolNo();
                strbHeader.append("Symbol").append(symbolNo).append(TAB_STR);
                for (int payHitIndex = 1; payHitIndex < symbolInfo.getHitPayCount().length; payHitIndex++) {
                    strbHeader.append(payHitIndex + 1).append(" Symbol").append(symbolNo).append(" Hit").append(TAB_STR);
                }
                strbHeader.append("Symbol").append(symbolNo).append(" Total Hit").append(TAB_STR);
                for (int payHitIndex = 1; payHitIndex < symbolInfo.getHitPayCount().length; payHitIndex++) {
                    strbHeader.append(payHitIndex + 1).append(" Symbol").append(symbolNo).append(" Hit Rate").append(TAB_STR);
                }
                strbHeader.append("Symbol").append(symbolNo).append(" Total Hit Rate").append(TAB_STR);
                for (int payIndex = 1; payIndex < symbolInfo.getHitPayAmount().length; payIndex++) {
                    strbHeader.append(payIndex + 1).append(" Symbol").append(symbolNo).append(" Win").append(TAB_STR);
                }
                strbHeader.append("Symbol").append(symbolNo).append(" Total Win").append(TAB_STR);
                for (int payIndex = 1; payIndex < symbolInfo.getHitPayAmount().length; payIndex++) {
                    strbHeader.append(payIndex + 1).append(" Symbol").append(symbolNo).append(" RTP").append(TAB_STR);
                }
                strbHeader.append("Symbol").append(symbolNo).append(" Total RTP").append(TAB_STR);
            }
            for (int i = 0; i < resultInfo.getWinTimesPerLineList().size(); i++) {
                strbHeader.append("Line").append(i + 1).append(" Times").append(TAB_STR);
                strbHeader.append("Line").append(i + 1).append(" WinPay").append(TAB_STR);
                strbHeader.append("Line").append(i + 1).append(" Hit Rate").append(TAB_STR);
                strbHeader.append("Line").append(i + 1).append(" Payout").append(TAB_STR);
            }
            FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), strbHeader.toString());
        }
        StringBuilder strContent = new StringBuilder();
        strContent.append(resultInfo.getSpinCount()).append(TAB_STR);
        strContent.append(resultInfo.getLocation()).append(TAB_STR);
        strContent.append(resultInfo.getReelsStop()).append(TAB_STR);
        strContent.append(resultInfo.getLeftCredit()).append(TAB_STR);
        strContent.append(resultInfo.getWinIncremental()).append(TAB_STR);
        strContent.append(resultInfo.getLine()).append(TAB_STR);
        strContent.append(resultInfo.getBetPerLine()).append(TAB_STR);
        strContent.append(resultInfo.getPreScene()).append(TAB_STR);
        strContent.append(resultInfo.getCurrentScene()).append(TAB_STR);
        strContent.append(resultInfo.getNextScene()).append(TAB_STR);
        strContent.append(resultInfo.getWinLine()).append(TAB_STR);
        strContent.append(resultInfo.getWinSymbols()).append(TAB_STR);
        strContent.append(resultInfo.getWinPays()).append(TAB_STR);
        strContent.append(resultInfo.getWinLinePosition()).append(TAB_STR);
        strContent.append(resultInfo.getTotalAmount()).append(TAB_STR);
        strContent.append(resultInfo.getLeftFsTimes()).append(TAB_STR);
        strContent.append(resultInfo.getTotalCoinIn()).append(TAB_STR);
        strContent.append(resultInfo.getTotalCoinOut()).append(TAB_STR);
        double payBack = resultInfo.getTotalCoinOut() * 1.0
                / resultInfo.getTotalCoinIn();
        strContent.append(payBack).append(TAB_STR);
        strContent.append(resultInfo.getRangeTotalCoinIn()).append(TAB_STR);
        strContent.append(resultInfo.getRangeTotalCoinOut()).append(TAB_STR);
        double rangPayBack = resultInfo.getRangeTotalCoinOut() * 1.0
                / resultInfo.getRangeTotalCoinIn();
        strContent.append(rangPayBack).append(TAB_STR);
        double bonusHitRate = resultInfo.getBonusTotalHit() * 1.0 / resultInfo.getSpinCount();
        double bonusPayBack = resultInfo.getBonusTotalWin() * 1.0 / resultInfo.getTotalCoinIn();
        strContent.append(resultInfo.getBonusOut()).append(TAB_STR);
        strContent.append(bonusHitRate).append(TAB_STR);
        strContent.append(bonusPayBack).append(TAB_STR);
        double fsHitRate = resultInfo.getFreespinTotalHit() * 1.0 / resultInfo.getSpinCount();
        double fsPayBack = resultInfo.getFreespinTotalWin() * 1.0
                / resultInfo.getTotalCoinIn();
        strContent.append(resultInfo.getFsOut()).append(TAB_STR);
        strContent.append(fsHitRate).append(TAB_STR);
        strContent.append(fsPayBack).append(TAB_STR);
        strContent.append(getBaseSymbolInfo(resultInfo));
        for (int i = 0; i < resultInfo.getWinTimesPerLineList().size(); i++) {
            long hitPerLine = resultInfo.getWinTimesPerLineList().get(i);
            double hitRatePerLine = hitPerLine * 1.0 / resultInfo.getSpinCount();
            long amountPerLine = resultInfo.getWinAmountPerLineList().get(i);
            double payoutPerLine = amountPerLine * 1.0 / resultInfo.getTotalCoinIn();
            strContent.append(hitPerLine).append(TAB_STR);
            strContent.append(amountPerLine).append(TAB_STR);
            strContent.append(hitRatePerLine).append(TAB_STR);
            strContent.append(payoutPerLine).append(TAB_STR);
        }
        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getOutputFileName(), configInfo, 0);
    }

    protected String getBaseSymbolInfo(BaseReelsDetailResultInfo resultInfo) {
        List<SymbolResultInfo> symbolInfoList = resultInfo.getSymbolResultInfoList();
        StringBuilder strContent = new StringBuilder();
        if (symbolInfoList != null && !symbolInfoList.isEmpty()) {
            for (SymbolResultInfo symbolInfo : symbolInfoList) {
                strContent.append("symbol").append(symbolInfo.getSymbolNo())
                        .append(TAB_STR);
                for (int i = 1; i < symbolInfo.getHitPayCount().length; i++) {
                    strContent.append(symbolInfo.getHitPayCount()[i]).append(TAB_STR);
                }
                strContent.append(symbolInfo.getTotalHit()).append(TAB_STR);
                for (int i = 1; i < symbolInfo.getHitPayCount().length; i++) {
                    double hitPayRate = symbolInfo.getHitPayCount()[i] * 1.0 / resultInfo.getSpinCount();
                    strContent.append(hitPayRate).append(TAB_STR);
                }
                double hitPayTotalRate = symbolInfo.getTotalHit() * 1.0 / resultInfo.getSpinCount();
                strContent.append(hitPayTotalRate).append(TAB_STR);
                for (int i = 1; i < symbolInfo.getHitPayAmount().length; i++) {
                    strContent.append(symbolInfo.getHitPayAmount()[i]).append(TAB_STR);
                }
                strContent.append(symbolInfo.getTotalAmount()).append(TAB_STR);
                for (int i = 1; i < symbolInfo.getHitPayAmount().length; i++) {
                    double payBackHitPay = symbolInfo.getHitPayAmount()[i] * 1.0 / resultInfo.getTotalCoinIn();
                    strContent.append(payBackHitPay).append(TAB_STR);
                }
                double payBackTotalHitPay = symbolInfo.getTotalAmount() * 1.0 / resultInfo.getTotalCoinIn();
                strContent.append(payBackTotalHitPay).append(TAB_STR);
            }
        }
        return strContent.toString();
    }

}
