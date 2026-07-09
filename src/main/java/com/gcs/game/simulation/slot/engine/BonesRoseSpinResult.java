package com.gcs.game.simulation.slot.engine;


import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.math.model20260103.Model20260103;
import com.gcs.game.engine.math.model20260103.Model20260103SpinResult;
import com.gcs.game.engine.math.model20260625.Model20260625;
import com.gcs.game.engine.math.model20260625.Model20260625SpinResult;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.vo.*;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.simulation.slot.vo.BonesRoseResultInfo;
import com.gcs.game.simulation.slot.vo.MakinBaconResultInfo;
import com.gcs.game.simulation.slot.vo.MissSpookyResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model20260103.Model20260103Test;
import com.gcs.game.testengine.math.model20260201.Model20260201Test;
import com.gcs.game.testengine.math.model20260625.Model20260625Test;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.utils.RandomWeightUntil;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BonesRoseSpinResult extends LittleDragonBunsSpinResult {

    public static final String FS_FILE = "fsResult1.txt";
    public static final long[] BET_LEVEL = new long[]{2, 4, 6, 8, 10, 12, 14, 16, 18, 20};
    private static BufferedWriter[] fsWriter = new BufferedWriter[3];

    public BonesRoseSpinResult() {

    }

    public void cycleSpinForBonesRose(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel baseSlotModel) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            double playerCredit = initCredit;
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            SlotGameLogicBean gameLogicBean = (SlotGameLogicBean) baseGameLogicBean;
            Model20260625Test model = (Model20260625Test) baseSlotModel;

            BonesRoseResultInfo resultInfo = new BonesRoseResultInfo();
            BonesRoseResultInfo[] fsResultInfo = new BonesRoseResultInfo[3];
            initFsResultInfo(fsResultInfo, model, slotConfigInfo);
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
                computeFsResult(fsResultInfo, spinCount, gameLogicBean);
                GameEngineCompute.computePayTableHit(gameLogicBean, gameLogicBean.getSlotSpinResult(), resultInfo, getScatterSymbol());
                if (winCredit > resultInfo.getBaseGameTopAward()) {
                    resultInfo.setBaseGameTopAward(winCredit);
                    resultInfo.setBaseTopAwardReelStop(StringUtil.IntegerArrayToStr(gameLogicBean.getSlotSpinResult().getSlotReelStopPosition(), " "));
                    resultInfo.setBaseTopAwardType("Base Normal");
                }
                Model20260625SpinResult spinResult = (Model20260625SpinResult) gameLogicBean.getSlotSpinResult();
                int baseReelsType = gameLogicBean.getBaseReelsType();
                resultInfo.getBaseReelsTypeHit()[baseReelsType]++;
                resultInfo.getBaseReelsTypeWin()[baseReelsType] += winCredit;
                computeScTrigger(spinResult, model, resultInfo);
                int fsType = spinResult.getFsType();

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
                    //start freespin or bonus
                    while (true) {
                        if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                            while (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                                PlayerInputInfo playerInput = new PlayerInputInfo();
                                playerInput.setRequestGameStatus(200);
                                gameLogicBean = (SlotGameLogicBean) engine.gameProgress(gameLogicBean, gameLogicMap, playerInput, null, null, null);

                                Model20260625SpinResult fsSpinResult = (Model20260625SpinResult) gameLogicBean.getSlotFsSpinResults().get(gameLogicBean.getSlotFsSpinResults().size() - 1);
                                long freespinWon = fsSpinResult.getSlotPay();

                                totalWon += freespinWon;
                                fsCoinOut += freespinWon;
                                fsTotalTimes++;
                                int scSymbol = Model20260625Test.SC1_SYMBOL;
                                if (fsType == Model20260625Test.FS_JACKPOT_BONUS) {
                                    scSymbol = Model20260625Test.SC2_SYMBOL;
                                }
                                computeScTypeResult(fsSpinResult, scSymbol, fsType, resultInfo, totalBet);
                                int[] wildReels = fsSpinResult.getSlotWildReels();
                                if (wildReels != null && wildReels.length > 0) {
                                    resultInfo.getFsExpandWildHit()[fsType - 2]++;
                                    resultInfo.getFsWinExpandWin()[fsType - 2] += freespinWon;

                                }
                                GameEngineCompute.addFreeSpinSymbolDetailInfo(fsSpinResult,
                                        resultInfo);
                                for (int j = 0; j < fsResultInfo.length; j++) {
                                    fsResultInfo[j].getFsTimes()[fsType - 1]++;
                                    fsResultInfo[j].getFsWin()[fsType - 1] += freespinWon;
                                    if (fsType == j + 1) {
                                        GameEngineCompute.addFreeSpinSymbolDetailInfo(fsSpinResult,
                                                fsResultInfo[j]);
                                    }
                                }
                                if (freespinWon > resultInfo.getFreespinTopAward()) {
                                    resultInfo.setFreespinTopAward(freespinWon);
                                    resultInfo.setFsTopAwardReelStop(StringUtil.IntegerArrayToStr(fsSpinResult.getSlotReelStopPosition(), " "));
                                    resultInfo.setFsTopAwardType("FS" + fsType);
                                }
                            }

                            //end freespin
                            if (fsTotalTimes > 0) {
                                for (int j = 0; j < fsResultInfo.length; j++) {
                                    fsResultInfo[j].setFreespinTotalTimes(fsResultInfo[j].getFreespinTotalTimes()
                                            + fsTotalTimes);
                                }
                                resultInfo.getFsHit()[fsType - 1]++;
                                resultInfo.getFsTimes()[fsType - 1] += fsTotalTimes;
                                resultInfo.getFsWin()[fsType - 1] += fsCoinOut;
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
                    outFsSymbolResultInfo(slotConfigInfo, fsResultInfo);
                }
            }

        } catch (InvalidGameStateException e) {
            log.error("engine gameStart", e);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            log.error("cycleSpinForBonesRose run exception", e);
        }

    }


    private double computeJackpot(BonesRoseResultInfo resultInfo, long totalBet, Model20260625Test model) {
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

    private void computeFsResult(BonesRoseResultInfo[] fsResultInfo, long spinCount, SlotGameLogicBean gameLogicBean) {
        if (fsResultInfo != null) {
            for (int i = 0; i < fsResultInfo.length; i++) {
                fsResultInfo[i].setSpinCount(spinCount);
                fsResultInfo[i].setBetPerLine(
                        (int) gameLogicBean.getBet());
                fsResultInfo[i].setLine((int) gameLogicBean.getLines());
                fsResultInfo[i].setTotalCoinIn(
                        fsResultInfo[i].getTotalCoinIn()
                                + gameLogicBean.getSumBetCredit());
            }
        }
    }

    private void initFsResultInfo(BonesRoseResultInfo[] fsResultInfo, Model20260625Test model, SlotConfigInfo slotConfigInfo) {
        if (fsResultInfo != null) {
            for (int i = 0; i < fsResultInfo.length; i++) {
                fsResultInfo[i] = new BonesRoseResultInfo();
                initFsSymbolInfo(model, fsResultInfo[i]);
            }
        }
        if (fsWriter != null) {
            for (int i = 0; i < fsWriter.length; i++) {
                String fileName = slotConfigInfo.getOutputPath() + getMakinBaconFSHead(i).replace(" ", "")
                        + "_Result.txt";
                fsWriter[i] = FileWriteUtil.initFreeSpinWriteFile(fileName,
                        fsWriter[i]);
                StringBuilder strbHeader = new StringBuilder();
                strbHeader.append(StringUtil.getFsHeaderInfo());
                strbHeader.append("fsTimes").append(BaseConstant.TAB_STR);
                strbHeader.append(StringUtil.getFreespinSymbolHeaderInfo());
                FileWriteUtil.outputFsInfo(strbHeader.toString(), fsWriter[i]);
            }
        }

    }

    private void computeScTypeResult(Model20260625SpinResult fsSpinResult, int scSymbol, int fsType, BonesRoseResultInfo resultInfo, long totalBet) {
        if (fsSpinResult != null) {
            int[] hitSymbol = fsSpinResult.getHitSlotSymbols();
            long[] hitPay = fsSpinResult.getHitSlotPays();
            int[] hitSlotSymbolsSound = fsSpinResult.getHitSlotSymbolsSound();
            if (hitSymbol != null) {
                for (int i = 0; i < hitSymbol.length; i++) {
                    if (hitSymbol[i] == scSymbol && hitPay[i] > 0) {
                        int xPrize = (int) (hitPay[i] / totalBet);
                        resultInfo.getFsScTypeHit()[fsType - 1][xPrize - 1]++;
                        resultInfo.getFsScTypeWin()[fsType - 1][xPrize - 1] += hitPay[i];
                    } else if (hitSlotSymbolsSound[i] > 1000) {
                        //+1 FREE,+2 FREE
                        int fsTime = hitSlotSymbolsSound[i] % 1000;
                        resultInfo.getFsScTypeHit()[fsType - 1][fsTime + 9]++;
                    }
                }
            }
        }

    }


    private void computeScTrigger(Model20260625SpinResult spinResult, Model20260625Test model, BonesRoseResultInfo resultInfo) {
        if (spinResult != null) {
            int[] displaySymbols = spinResult.getSlotDisplaySymbols();
            int sc1Count = model.computeScPosition(displaySymbols, Model20260625.SC1_SYMBOL);
            int sc2count = model.computeScPosition(displaySymbols, Model20260625.SC2_SYMBOL);
            int fsType = spinResult.getFsType();
            if (sc1Count > 0 && sc2count > 0) {
                if (fsType > 0) {
                    resultInfo.getSc3TriggerHit()[fsType]++;
                } else {
                    resultInfo.getSc3TriggerHit()[0]++;
                }
            } else if (sc1Count > 0) {
                if (fsType > 0) {
                    resultInfo.getSc1TriggerHit()[1]++;
                } else {
                    resultInfo.getSc1TriggerHit()[0]++;
                }
            } else if (sc2count > 0) {
                if (fsType > 0) {
                    resultInfo.getSc2TriggerHit()[1]++;
                } else {
                    resultInfo.getSc2TriggerHit()[0]++;
                }
            }
        }

    }

    protected int[] getScatterSymbol() {
        return new int[]{12, 13, 14};
    }

    private void outResultInfo(SlotConfigInfo configInfo, BonesRoseResultInfo resultInfo) {

        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append(StringUtil.getCommonHeaderInfo(resultInfo));
            strbHeader.append(StringUtil.getBonusHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getBaseReelsTypeHit().length; i++) {
                strbHeader.append("Base Reelstrip").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseReelsTypeWin().length; i++) {
                strbHeader.append("Base Reelstrip").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getSc1TriggerHit().length; i++) {
                strbHeader.append("Base Trigger SC1 Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getSc2TriggerHit().length; i++) {
                strbHeader.append("Base Trigger SC2 Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getSc3TriggerHit().length; i++) {
                strbHeader.append("Base Trigger SC1+SC2 Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsHit().length; i++) {
                strbHeader.append(getMakinBaconFSHead(i)).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsTimes().length; i++) {
                strbHeader.append(getMakinBaconFSHead(i)).append(" Times").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWin().length; i++) {
                strbHeader.append(getMakinBaconFSHead(i)).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsScTypeHit().length; i++) {
                for (int j = 0; j < resultInfo.getFsScTypeHit()[i].length; j++) {
                    strbHeader.append("Fs Type").append(i + 1).append(" SC Index").append(j + 1).append(" Hit").append(BaseConstant.TAB_STR);
                }
            }
            for (int i = 0; i < resultInfo.getFsScTypeWin().length; i++) {
                for (int j = 0; j < resultInfo.getFsScTypeWin()[i].length; j++) {
                    strbHeader.append("Fs Type").append(i + 1).append(" SC Index").append(j + 1).append(" Win").append(BaseConstant.TAB_STR);
                }
            }
            for (int i = 0; i < resultInfo.getFsExpandWildHit().length; i++) {
                strbHeader.append(getMakinBaconFSHead(i + 1)).append(" Expand Wild Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWinExpandWin().length; i++) {
                strbHeader.append(getMakinBaconFSHead(i + 1)).append(" Expand Wild Win").append(BaseConstant.TAB_STR);
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
        for (long reelsTypeHit : resultInfo.getBaseReelsTypeHit()) {
            strContent.append(reelsTypeHit).append(BaseConstant.TAB_STR);
        }
        for (long reelsTypeWin : resultInfo.getBaseReelsTypeWin()) {
            strContent.append(reelsTypeWin).append(BaseConstant.TAB_STR);
        }
        for (long sc1Hit : resultInfo.getSc1TriggerHit()) {
            strContent.append(sc1Hit).append(BaseConstant.TAB_STR);
        }
        for (long sc2Hit : resultInfo.getSc2TriggerHit()) {
            strContent.append(sc2Hit).append(BaseConstant.TAB_STR);
        }
        for (long sc3Hit : resultInfo.getSc3TriggerHit()) {
            strContent.append(sc3Hit).append(BaseConstant.TAB_STR);
        }
        for (long fsHit : resultInfo.getFsHit()) {
            strContent.append(fsHit).append(BaseConstant.TAB_STR);
        }
        for (long fsTimes : resultInfo.getFsTimes()) {
            strContent.append(fsTimes).append(BaseConstant.TAB_STR);
        }
        for (long fsWin : resultInfo.getFsWin()) {
            strContent.append(fsWin).append(BaseConstant.TAB_STR);
        }
        for (long[] fsScType : resultInfo.getFsScTypeHit()) {
            for (long scTypeHit : fsScType) {
                strContent.append(scTypeHit).append(BaseConstant.TAB_STR);
            }
        }
        for (long[] fsScType : resultInfo.getFsScTypeWin()) {
            for (long scTypeWin : fsScType) {
                strContent.append(scTypeWin).append(BaseConstant.TAB_STR);
            }
        }
        for (long fsExpandWildHit : resultInfo.getFsExpandWildHit()) {
            strContent.append(fsExpandWildHit).append(BaseConstant.TAB_STR);
        }
        for (long fsExpandWildWin : resultInfo.getFsWinExpandWin()) {
            strContent.append(fsExpandWildWin).append(BaseConstant.TAB_STR);
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

    private String getMakinBaconFSHead(int fsType) {
        String str = "";
        switch (fsType) {
            case 0:
                str = "FREE EXPAND";
                break;
            case 1:
                str = "FREE JACKPOT";
                break;
            case 2:
                str = "FREE SUPER";
                break;
            default:
                str = "FREE EXPAND";
                break;
        }
        return str;
    }

    private void outFsSymbolResultInfo(SlotConfigInfo configInfo,
                                       BonesRoseResultInfo[] fsResultInfo) {
        for (int i = 0; i < fsResultInfo.length; i++) {
            StringBuilder strContent = new StringBuilder();
            strContent.append(StringUtil.getFSBaseResultInfo(fsResultInfo[i]));
            strContent.append(fsResultInfo[i].getFsTimes()[i]).append(BaseConstant.TAB_STR);
            strContent.append(StringUtil.getFreespinSymbolInfo(
                    fsResultInfo[i].getFsSymbolInfoList()));
            FileWriteUtil.outputFsInfo(strContent.toString(), fsWriter[i]);
        }
    }


}
