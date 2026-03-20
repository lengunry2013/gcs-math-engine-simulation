package com.gcs.game.simulation.slot.engine;


import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.vo.*;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import com.gcs.game.simulation.slot.vo.MissSpookyResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model20260201.Model20260201Test;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.utils.RandomWeightUntil;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class MissSpookySpinResult extends LittleDragonBunsSpinResult {

    public static final String FS_FILE = "fsResult1.txt";
    public static final long[] BET_LEVEL= new long[]{2, 4, 6, 8, 10, 12, 14, 16, 18, 20};

    public MissSpookySpinResult() {

    }

    public void cycleSpinForMissSpooky(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel baseSlotModel) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            double playerCredit = initCredit;
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            SlotGameLogicBean gameLogicBean = (SlotGameLogicBean) baseGameLogicBean;
            Model20260201Test model = (Model20260201Test) baseSlotModel;

            MissSpookyResultInfo resultInfo = new MissSpookyResultInfo();
            initFsResultInfo(slotConfigInfo);
            initJackpotMeter(model.getJackpotInitMeter(), resultInfo);
            double totalWon = 0.0;
            GameEngineCompute.initPayTableHit(model.getPayTable(), resultInfo);
            initFsSymbolInfo(model, resultInfo);
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
                int baseMul = gameLogicBean.getSlotSpinResult().getBaseGameMul();
                computeBaseMystery(baseMul, resultInfo);
                GameEngineCompute.computePayTableHit(gameLogicBean, gameLogicBean.getSlotSpinResult(), resultInfo, getScatterSymbol());
                if (winCredit > resultInfo.getBaseGameTopAward()) {
                    resultInfo.setBaseGameTopAward(winCredit);
                    resultInfo.setBaseTopAwardReelStop(StringUtil.IntegerArrayToStr(gameLogicBean.getSlotSpinResult().getSlotReelStopPosition(), " "));
                    resultInfo.setBaseTopAwardType("Base Normal");
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
                    int fsType = -1;
                    long fsCoinOut = 0L;
                    long fsTotalTimes = 0L;
                    int fsScriptIndex = -1;
                    //start freespin or bonus
                    while (true) {
                        if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                            while (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                                PlayerInputInfo playerInput = new PlayerInputInfo();
                                playerInput.setRequestGameStatus(200);
                                gameLogicBean = (SlotGameLogicBean) engine.gameProgress(gameLogicBean, gameLogicMap, playerInput, null, null, null);

                                SlotSpinResult fsSpinResult = gameLogicBean.getSlotFsSpinResults().get(gameLogicBean.getSlotFsSpinResults().size() - 1);
                                long freespinWon = fsSpinResult.getSlotPay();

                                totalWon += freespinWon;
                                fsCoinOut += freespinWon;
                                fsTotalTimes++;

                                switch (fsType) {
                                    case 1:
                                        resultInfo.getGroundFsScriptHit()[fsScriptIndex]++;
                                        resultInfo.getGetGroundFsScriptWin()[fsScriptIndex] += freespinWon;
                                        break;
                                    case 2:
                                        int wildReelsIndex = getWildReelsIndex(fsSpinResult.getSlotWildReels());
                                        if (wildReelsIndex >= 0) {
                                            resultInfo.getWildFsHit()[wildReelsIndex]++;
                                            resultInfo.getWildFsWin()[wildReelsIndex] += freespinWon;
                                        }
                                        break;
                                    case 3:
                                        resultInfo.getLightningFsScriptHit()[fsScriptIndex]++;
                                        resultInfo.getLightningFsScriptWin()[fsScriptIndex] += freespinWon;
                                        break;
                                    default:
                                        break;
                                }
                                resultInfo.getFsTimes()[fsType - 1]++;
                                resultInfo.getFsWin()[fsType - 1] += freespinWon;
                                GameEngineCompute.addFreeSpinSymbolDetailInfo(fsSpinResult,
                                        resultInfo);
                                if (freespinWon > resultInfo.getFreespinTopAward()) {
                                    resultInfo.setFreespinTopAward(freespinWon);
                                    resultInfo.setFsTopAwardReelStop(StringUtil.IntegerArrayToStr(fsSpinResult.getSlotReelStopPosition(), " "));
                                    resultInfo.setFsTopAwardType("FS" + fsType);
                                }
                            }

                            //end freespin
                            if (fsTotalTimes > 0) {
                                resultInfo.getFsHit()[fsType - 1]++;
                                resultInfo.setFreespinTotalTimes(resultInfo.getFreespinTotalTimes() + fsTotalTimes);
                                resultInfo.setFreespinTotalHit(resultInfo.getFreespinTotalHit() + 1);
                                resultInfo.setFreespinTotalWin(resultInfo.getFreespinTotalWin() + fsCoinOut);
                            }
                        } else if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_BONUS) {
                            boolean bonus1 = true;
                            if ("bonus".equalsIgnoreCase(gameLogicBean.getNextScenes()) || "bonus1".equalsIgnoreCase(gameLogicBean.getNextScenes())) {
                                bonus1 = true;
                            } else {
                                bonus1 = false;
                            }
                            PlayerInputInfo playerInput = new PlayerInputInfo();
                            playerInput.setRequestGameStatus(500);
                            //bonusChoice random Index
                            int bonusChoiceIndex = ((SlotConfigInfo) configInfo).getChoiceFsOrBonusIndex();
                            if (((SlotConfigInfo) configInfo).isRandomBonusChoice() && bonusChoiceIndex > 0 && bonus1) {
                                bonusChoiceIndex = RandomUtil.getRandomInt(bonusChoiceIndex);
                            } else {
                                if (!bonus1) {
                                    bonusChoiceIndex = -1;
                                }
                            }
                            for (int pick = 0; pick < 100; pick++) {
                                if (pick > 0) {
                                    int[] picks = GameEngineCompute.initArray(pick, bonusChoiceIndex);
                                    playerInput.setBonusPickInfos(picks);
                                }
                                gameLogicBean = (SlotGameLogicBean) engine.gameProgress(gameLogicBean, gameLogicMap, playerInput, null, null, null);

                                SlotBonusResult baseBonusResult = gameLogicBean.getSlotBonusResult();
                                if (baseBonusResult.getBonusPlayStatus() == 1000) {
                                    long bonusWon = baseBonusResult.getTotalPay();
                                    if (baseBonusResult instanceof SlotChoiceFSBonusResult) {
                                        fsType = ((SlotChoiceFSBonusResult) baseBonusResult).getFsType();
                                        fsScriptIndex = ((SlotChoiceFSBonusResult) baseBonusResult).getRandomIndex4FS();
                                        totalWon += bonusWon;
                                    } else if (baseBonusResult instanceof SlotChoiceBonusResult) {
                                        int hitLevel = ((SlotChoiceBonusResult) baseBonusResult).getHitLevel();
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
                    outFsSymbolResultInfo(slotConfigInfo, resultInfo);
                }
            }

        } catch (InvalidGameStateException e) {
            log.error("engine gameStart", e);
            e.printStackTrace();
        } catch (Exception e) {
            log.error("cycleSpinForMissSpooky run exception", e);
        }

    }

    private double computeJackpot(MissSpookyResultInfo resultInfo, long totalBet, Model20260201Test model) {
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

    public static void initJackpotMeter(int[] jackpotInitMeter, BaseResultInfo resultInfo) {
        double[] jackpotMeter = new double[jackpotInitMeter.length];
        for (int i = 0; i < jackpotInitMeter.length; i++) {
            jackpotMeter[i] = jackpotInitMeter[i];
        }
        resultInfo.setJackpotMeter(jackpotMeter);
    }

    private static final int[] BONUS_REWARD = new int[]{
            8, 26, 66, 34, 74, 92, 100
    };

    private int computeBonusPayIndex(long bonusPay, int bonusMul) {
        int bonusWinPay = (int) (bonusPay / bonusMul);
        for (int i = 0; i < BONUS_REWARD.length; i++) {
            if (bonusWinPay == BONUS_REWARD[i]) {
                if (bonusMul > 1) {
                    return i + 7;
                } else {
                    return i;
                }
            }
        }
        return -1;
    }

    private static final int[][] WIND_FS_WILD_REEL = new int[][]{
            {3, 4}, {2, 4}, {1, 4}, {0, 4}, {2, 3}, {1, 3},
            {0, 3}, {1, 2}, {0, 2}, {0, 1}, {4}, {3},
            {2}, {1}, {0}};

    private int getWildReelsIndex(int[] slotWildReels) {
        int[][] wildReels = WIND_FS_WILD_REEL;
        for (int i = 0; i < wildReels.length; i++) {
            if (Arrays.equals(wildReels[i], slotWildReels)) {
                return i;
            }
        }
        return -1;
    }

    private void computeBaseMystery(int baseMul, MissSpookyResultInfo resultInfo) {
        int[] baseMysteryMul = Model20260201Test.BASE_MYSTERY_MUL;
        for (int i = 0; i < baseMysteryMul.length; i++) {
            if (baseMul == baseMysteryMul[i]) {
                resultInfo.getBaseMysteryMulHit()[i]++;
                break;
            }
        }
    }


    protected int[] getScatterSymbol() {
        return new int[]{11, 12};
    }

    private void outResultInfo(SlotConfigInfo configInfo, MissSpookyResultInfo resultInfo) {

        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append(StringUtil.getCommonHeaderInfo(resultInfo));
            strbHeader.append(StringUtil.getBonusHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getBaseMysteryMulHit().length; i++) {
                strbHeader.append("Base Mystery Mul").append(Model20260201Test.BASE_MYSTERY_MUL[i]).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsHit().length; i++) {
                strbHeader.append(getMonsterPageantFSHead(i)).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsTimes().length; i++) {
                strbHeader.append(getMonsterPageantFSHead(i)).append(" Times").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWin().length; i++) {
                strbHeader.append(getMonsterPageantFSHead(i)).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getLightningFsScriptHit().length; i++) {
                strbHeader.append("Fs Sticky Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getLightningFsScriptWin().length; i++) {
                strbHeader.append("Fs Sticky Index").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getGroundFsScriptHit().length; i++) {
                strbHeader.append("Fs 3W Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getGetGroundFsScriptWin().length; i++) {
                strbHeader.append("Fs 3W Index").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getWildFsHit().length; i++) {
                strbHeader.append("Fs WR Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getWildFsWin().length; i++) {
                strbHeader.append("Fs WR Index").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
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
        for (long mysteryMulHit : resultInfo.getBaseMysteryMulHit()) {
            strContent.append(mysteryMulHit).append(BaseConstant.TAB_STR);
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
        for (long fsStickyHit : resultInfo.getLightningFsScriptHit()) {
            strContent.append(fsStickyHit).append(BaseConstant.TAB_STR);
        }
        for (long fsStickyWin : resultInfo.getLightningFsScriptWin()) {
            strContent.append(fsStickyWin).append(BaseConstant.TAB_STR);
        }
        for (long fs3WHit : resultInfo.getGroundFsScriptHit()) {
            strContent.append(fs3WHit).append(BaseConstant.TAB_STR);
        }
        for (long fs3WWin : resultInfo.getGetGroundFsScriptWin()) {
            strContent.append(fs3WWin).append(BaseConstant.TAB_STR);
        }
        for (long fsWrHit : resultInfo.getWildFsHit()) {
            strContent.append(fsWrHit).append(BaseConstant.TAB_STR);
        }
        for (long fsWrWin : resultInfo.getWildFsWin()) {
            strContent.append(fsWrWin).append(BaseConstant.TAB_STR);
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

    private String getMonsterPageantFSHead(int fsType) {
        String str = "";
        switch (fsType) {
            case 0:
                str = "FS 3W";
                break;
            case 1:
                str = "FS WR";
                break;
            case 2:
                str = "FS Sticky";
                break;
            default:
                str = "FS Sticky";
                break;
        }
        return str;
    }


}
