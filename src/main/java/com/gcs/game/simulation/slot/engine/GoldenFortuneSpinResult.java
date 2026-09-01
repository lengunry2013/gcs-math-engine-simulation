package com.gcs.game.simulation.slot.engine;


import cn.hutool.core.util.ObjectUtil;
import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.math.model20260825.Model20260825SpinResult;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.vo.SlotBonusResult;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.engine.slots.vo.SlotWheelBonusResult;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.simulation.slot.vo.GoldenFortuneResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model20260825.Model20260825Test;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.utils.RandomWeightUntil;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GoldenFortuneSpinResult extends LittleDragonBunsSpinResult {

    public static final String FS_FILE = "fsResult1.txt";
    public static final long[] BET_LEVEL = new long[]{2, 4, 6, 8, 10, 12, 14, 16, 18, 20};

    public GoldenFortuneSpinResult() {

    }

    public void cycleSpinForGoldenFortune(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel baseSlotModel) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            double playerCredit = initCredit;
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            SlotGameLogicBean gameLogicBean = (SlotGameLogicBean) baseGameLogicBean;
            Model20260825Test model = (Model20260825Test) baseSlotModel;

            GoldenFortuneResultInfo resultInfo = new GoldenFortuneResultInfo();
            double totalWon = 0.0;
            GameEngineCompute.initPayTableHit(model.getPayTable(), resultInfo);
            initFsSymbolInfo(model, resultInfo);
            MissSpookySpinResult.initJackpotMeter(model.getJackpotInitMeter(), resultInfo);
            long minBet = model.minLines() * model.minBetPerLine();
            int[] initJackpotMeter = model.getJackpotInitMeter();
            boolean isWagerSaver = false;
            for (int i = 0; i < simulationCount; i++) {
                totalWon = 0;

                Map gameLogicMap = new LinkedHashMap();
                gameLogicMap.put("lines", slotConfigInfo.getLines());
                if (slotConfigInfo.isRandomBet()) {
                    int betIndex = RandomUtil.getRandomInt(BET_LEVEL.length);
                    gameLogicMap.put("bet", BET_LEVEL[betIndex]);
                } else {
                    gameLogicMap.put("bet", slotConfigInfo.getBet());
                }
                gameLogicMap.put("denom", slotConfigInfo.getDenom());
                if (playerCredit <= 0) {
                    playerCredit = initCredit;
                }
                isWagerSaver = false;
                //spin之前判断是否小于minBet,小于minBet在去随机
                if (playerCredit > 0 && playerCredit < minBet) {
                    double remainRate = playerCredit / minBet;
                    int weight = (int) (remainRate * 10000);
                    int[] remainCreditWeight = new int[]{10000 - weight, weight};
                    RandomWeightUntil randomWeightUntil = new RandomWeightUntil(remainCreditWeight);
                    int triggerWagerSaver = randomWeightUntil.getRandomResult();
                    if (triggerWagerSaver == 1) {
                        resultInfo.setTotalCoinIn(resultInfo.getTotalCoinIn() + playerCredit);
                        playerCredit = minBet;
                        resultInfo.setWagerSaverHitCount(resultInfo.getWagerSaverHitCount() + 1);
                        gameLogicMap.put("bet", model.minBetPerLine());
                        isWagerSaver = true;
                    } else {
                        resultInfo.setTotalCoinIn(resultInfo.getTotalCoinIn() + playerCredit);
                        playerCredit = 0;
                        playerCredit += initCredit;
                        i--;
                        continue;
                    }
                }
                spinCount++;
                gameLogicBean = (SlotGameLogicBean) engine.gameStart(gameLogicBean, gameLogicMap, null, null);
                long totalBet = gameLogicBean.getSumBetCredit();
                playerCredit -= totalBet;
                double jackpotWin = computeJackpot(resultInfo, totalBet, model);
                playerCredit += jackpotWin;
                totalWon += jackpotWin;
                long winCredit = gameLogicBean.getSumWinCredit();
                totalWon += winCredit;
                GameEngineCompute.computePayTableHit(gameLogicBean, gameLogicBean.getSlotSpinResult(), resultInfo, getScatterSymbol());
                if (winCredit > resultInfo.getBaseGameTopAward()) {
                    resultInfo.setBaseGameTopAward(winCredit);
                    resultInfo.setBaseTopAwardReelStop(StringUtil.IntegerArrayToStr(gameLogicBean.getSlotSpinResult().getSlotReelStopPosition(), " "));
                    resultInfo.setBaseTopAwardType("Base Normal");
                }
                Model20260825SpinResult spinResult = (Model20260825SpinResult) gameLogicBean.getSlotSpinResult();
                int swType = spinResult.getSwType();
                if (swType > 0) {
                    resultInfo.getSwTypeHit()[swType - 1]++;
                }
                if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                    if (winCredit > 0) {
                        resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                        resultInfo.setBaseGameTotalWin(resultInfo.getBaseGameTotalWin() + winCredit);
                    }
                } else {
                    resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                    if (winCredit > 0) {
                        resultInfo.setBaseGameTotalWin(resultInfo.getBaseGameTotalWin() + winCredit);
                    }
                    long fsCoinOut = 0L;
                    long fsTotalTimes = 0L;
                    List<long[]> swPaysList = null;
                    List<Long> colTotalPays = null;
                    List<Boolean> isFullBalls = null;
                    //start freespin or bonus
                    while (true) {
                        if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                            while (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                                PlayerInputInfo playerInput = new PlayerInputInfo();
                                playerInput.setRequestGameStatus(200);
                                gameLogicBean = (SlotGameLogicBean) engine.gameProgress(gameLogicBean, gameLogicMap, playerInput, null, null, null);

                                Model20260825SpinResult fsSpinResult = (Model20260825SpinResult) gameLogicBean.getSlotFsSpinResults().get(gameLogicBean.getSlotFsSpinResults().size() - 1);
                                long freespinWon = fsSpinResult.getSlotPay();

                                totalWon += freespinWon;
                                fsCoinOut += freespinWon;
                                fsTotalTimes++;
                                swPaysList = fsSpinResult.getLinkBonusSwPays();
                                colTotalPays = fsSpinResult.getColTotalPays();
                                isFullBalls = fsSpinResult.getIsFullBalls();
                                GameEngineCompute.addFreeSpinSymbolDetailInfo(fsSpinResult,
                                        resultInfo);
                                if (freespinWon > resultInfo.getFreespinTopAward()) {
                                    resultInfo.setFreespinTopAward(freespinWon);
                                    resultInfo.setFsTopAwardReelStop(StringUtil.IntegerArrayToStr(fsSpinResult.getSlotReelStopPosition(), " "));
                                    resultInfo.setFsTopAwardType("FS");
                                }
                            }

                            //end freespin
                            long swTotalPays = computeLinkBonusPays(swPaysList);
                            long swTotalColPays = computeLinkBonusColPays(colTotalPays);
                            fsCoinOut += swTotalPays;
                            fsCoinOut += swTotalColPays;
                            totalWon += fsCoinOut;
                            boolean isFull = computeIsFullBalls(isFullBalls);
                            resultInfo.getSwTypeWin()[swType - 1] += fsCoinOut;
                            resultInfo.getSwCoinWin()[swType - 1] += swTotalPays;
                            resultInfo.getSwColWin()[swType - 1] += swTotalColPays;
                            resultInfo.getSwSpinTimes()[swType - 1] += fsTotalTimes;
                            if (isFull) {
                                resultInfo.getSwFullHit()[swType - 1]++;
                            }
                            if (fsTotalTimes > 0) {
                                resultInfo.setFreespinTotalTimes(resultInfo.getFreespinTotalTimes() + fsTotalTimes);
                                resultInfo.setFreespinTotalHit(resultInfo.getFreespinTotalHit() + 1);
                                resultInfo.setFreespinTotalWin(resultInfo.getFreespinTotalWin() + fsCoinOut);
                            }
                        } else if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_BONUS) {
                            PlayerInputInfo playerInput = new PlayerInputInfo();
                            playerInput.setRequestGameStatus(500);
                            //bonusChoice random Index
                            int bonusChoiceIndex = ((SlotConfigInfo) configInfo).getChoiceFsOrBonusIndex();
                            for (int pick = 0; pick < 100; pick++) {
                                if (pick > 0) {
                                    int[] picks = GameEngineCompute.initArray(pick, bonusChoiceIndex);
                                    playerInput.setBonusPickInfos(picks);
                                }
                                gameLogicBean = (SlotGameLogicBean) engine.gameProgress(gameLogicBean, gameLogicMap, playerInput, null, null, null);

                                SlotBonusResult baseBonusResult = gameLogicBean.getSlotBonusResult();
                                if (baseBonusResult.getBonusPlayStatus() == 1000) {
                                    long bonusWon = baseBonusResult.getTotalPay();
                                    if (baseBonusResult instanceof SlotWheelBonusResult) {
                                        int hitLevel = ((SlotWheelBonusResult) baseBonusResult).getHitLevel() - 1;
                                        double bonusWin = resultInfo.getJackpotMeter()[hitLevel];
                                        resultInfo.getHitLevelCount()[hitLevel]++;
                                        resultInfo.getBonusWinComboHit()[hitLevel]++;
                                        resultInfo.getBonusWinComboWin()[hitLevel] += bonusWin;
                                        resultInfo.getJackpotHitMeter()[hitLevel] += bonusWin;
                                        resultInfo.getJackpotMeter()[hitLevel] = initJackpotMeter[hitLevel];
                                        totalWon += bonusWin;
                                    }
                                    if (bonusWon > 0) {
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

                playerCredit += totalWon;
                if (isWagerSaver) {
                    resultInfo.setWagerSaverWin(resultInfo.getWagerSaverWin() + totalWon);
                }
                resultInfo.setSpinCount(spinCount);
                resultInfo.setBetPerLine((int) gameLogicBean.getBet());
                resultInfo.setLine((int) gameLogicBean.getLines());
                if (!isWagerSaver) {
                    resultInfo.setTotalCoinIn(resultInfo.getTotalCoinIn() + gameLogicBean.getSumBetCredit());
                }
                if (totalWon > 0) {
                    resultInfo.setTotalHit(resultInfo.getTotalHit() + 1);
                    if (totalWon > resultInfo.getScreenMaxAward()) {
                        resultInfo.setScreenMaxAward(totalWon);
                        resultInfo.setScreenMaxAwardHit(1);
                    } else if (totalWon == resultInfo.getScreenMaxAward()) {
                        resultInfo.setScreenMaxAwardHit(resultInfo.getScreenMaxAwardHit() + 1);
                    }
                }
                resultInfo.setTotalCoinOut(resultInfo.getTotalCoinOut() + totalWon);
                resultInfo.setTotalAmount(totalWon);
                resultInfo.setLeftCredit(playerCredit);
                if (spinCount > 0 && spinCount % playTime == 0) {
                    outResultInfo(slotConfigInfo, resultInfo);
                }
            }

        } catch (InvalidGameStateException e) {
            log.error("engine gameStart", e);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            log.error("cycleSpinForGoldenFortune run exception", e);
        }

    }

    private boolean computeIsFullBalls(List<Boolean> isFullBalls) {
        if (ObjectUtil.isNotEmpty(isFullBalls)) {
            for (boolean isFull : isFullBalls) {
                if (isFull) {
                    return true;
                }
            }
        }
        return false;
    }

    private long computeLinkBonusColPays(List<Long> colTotalPays) {
        long totalColPay = 0;
        if (ObjectUtil.isNotEmpty(colTotalPays)) {
            for (long colPay : colTotalPays) {
                totalColPay += colPay;
            }
        }
        return totalColPay;
    }

    private long computeLinkBonusPays(List<long[]> swPaysList) {
        long totalPay = 0;
        if (ObjectUtil.isNotEmpty(swPaysList)) {
            for (long[] swPays : swPaysList) {
                for (long pay : swPays) {
                    totalPay += pay;
                }
            }
        }
        return totalPay;
    }


    private double computeJackpot(GoldenFortuneResultInfo resultInfo, long totalBet, Model20260825Test model) {
        int[] initJackpotMeter = model.getJackpotInitMeter();
        int[] maxJackpotMeter = model.getJackpotMaxMeter();
        double[] contributionRate = model.getJackpotContributionRate();
        double jackpotWin = 0.0;
        for (int i = 0; i < maxJackpotMeter.length; i++) {
            resultInfo.getJackpotMeter()[i] += contributionRate[i] * totalBet;
            if (resultInfo.getJackpotMeter()[i] >= maxJackpotMeter[i]) {
                resultInfo.getJackpotHitMeter()[i] += resultInfo.getJackpotMeter()[i];
                resultInfo.getJackpotMeter()[i] = initJackpotMeter[i];
                resultInfo.getHitLevelCount()[i]++;
                jackpotWin += resultInfo.getJackpotMeter()[i];
            }
        }
        return jackpotWin;
    }

    protected int[] getScatterSymbol() {
        return new int[]{12, 13, 14, 15};
    }

    private void outResultInfo(SlotConfigInfo configInfo, GoldenFortuneResultInfo resultInfo) {

        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append(StringUtil.getCommonHeaderInfo(resultInfo));
            strbHeader.append(StringUtil.getBonusHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getSwTypeHit().length; i++) {
                strbHeader.append("SW Type").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getSwTypeWin().length; i++) {
                strbHeader.append("SW Type").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getSwCoinWin().length; i++) {
                strbHeader.append("SW Type").append(i + 1).append(" CoinWin").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getSwColWin().length; i++) {
                strbHeader.append("SW Type").append(i + 1).append(" ColWin").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getSwFullHit().length; i++) {
                strbHeader.append("SW Type").append(i + 1).append(" Full Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getSwSpinTimes().length; i++) {
                strbHeader.append("SW Type").append(i + 1).append(" SpinNum").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBonusWinComboHit().length; i++) {
                strbHeader.append("Bonus Combo").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBonusWinComboWin().length; i++) {
                strbHeader.append("Bonus Combo").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getJackpotMeter().length; i++) {
                strbHeader.append("Jackpot Level").append(i + 1).append(" Meter").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getHitLevelCount().length; i++) {
                strbHeader.append("Jackpot Level").append(i + 1).append(" Count").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getJackpotHitMeter().length; i++) {
                strbHeader.append("Jackpot Level").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            strbHeader.append("WagerSaver Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("WagerSaver Win").append(BaseConstant.TAB_STR);
            strbHeader.append(StringUtil.getPayTableHeaderInfo(resultInfo));
            FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), strbHeader.toString());
        }
        StringBuilder strContent = new StringBuilder();
        strContent.append(StringUtil.getBaseResultInfo(resultInfo));
        double totalBonusWin = 0.0;
        for (double jackpotWin : resultInfo.getBonusWinComboWin()) {
            totalBonusWin += jackpotWin;
        }
        strContent.append(resultInfo.getBonusTotalHit()).append(BaseConstant.TAB_STR);
        strContent.append(totalBonusWin).append(BaseConstant.TAB_STR);
        double bonusTotalHitRate = resultInfo.getBonusTotalHit() * 1.0 / resultInfo.getSpinCount();
        double bonusTotalPayback = totalBonusWin / resultInfo.getTotalCoinIn();
        strContent.append(bonusTotalHitRate).append(BaseConstant.TAB_STR);
        strContent.append(bonusTotalPayback).append(BaseConstant.TAB_STR);
        for (long swHit : resultInfo.getSwTypeHit()) {
            strContent.append(swHit).append(BaseConstant.TAB_STR);
        }
        for (long swWin : resultInfo.getSwTypeWin()) {
            strContent.append(swWin).append(BaseConstant.TAB_STR);
        }
        for (long swCoinWin : resultInfo.getSwCoinWin()) {
            strContent.append(swCoinWin).append(BaseConstant.TAB_STR);
        }
        for (long swColWin : resultInfo.getSwColWin()) {
            strContent.append(swColWin).append(BaseConstant.TAB_STR);
        }
        for (long swFullHit : resultInfo.getSwFullHit()) {
            strContent.append(swFullHit).append(BaseConstant.TAB_STR);
        }
        for (long swSpinNum : resultInfo.getSwSpinTimes()) {
            strContent.append(swSpinNum).append(BaseConstant.TAB_STR);
        }

        for (long bonusWinHit : resultInfo.getBonusWinComboHit()) {
            strContent.append(bonusWinHit).append(BaseConstant.TAB_STR);
        }
        for (double bonusWin : resultInfo.getBonusWinComboWin()) {
            strContent.append(bonusWin).append(BaseConstant.TAB_STR);
        }
        for (double jackpotMeter : resultInfo.getJackpotMeter()) {
            strContent.append(jackpotMeter).append(BaseConstant.TAB_STR);
        }
        for (double hitLevel : resultInfo.getHitLevelCount()) {
            strContent.append(hitLevel).append(BaseConstant.TAB_STR);
        }
        for (double jackpotWin : resultInfo.getJackpotHitMeter()) {
            strContent.append(jackpotWin).append(BaseConstant.TAB_STR);
        }
        strContent.append(resultInfo.getWagerSaverHitCount()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getWagerSaverWin()).append(BaseConstant.TAB_STR);
        strContent.append(StringUtil.getPayTableHit(resultInfo));
        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getOutputFileName(), configInfo, 0);
    }

}
