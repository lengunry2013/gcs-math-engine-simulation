package com.gcs.game.simulation.slot.engine;


import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.math.model20260618.Model20260618;
import com.gcs.game.engine.math.model20260618.Model20260618SpinResult;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.engine.slots.vo.SlotSpinResult;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.simulation.slot.vo.KingFish2ResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model20260618.Model20260618Test;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class KingFish2SpinResult extends LittleDragonBunsSpinResult {

    public static final String FS_FILE = "fsResult1.txt";

    public KingFish2SpinResult() {

    }

    public void cycleSpinForKingFish2(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel baseSlotModel) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            SlotGameLogicBean gameLogicBean = (SlotGameLogicBean) baseGameLogicBean;
            Model20260618Test model = (Model20260618Test) baseSlotModel;
            KingFish2ResultInfo resultInfo = new KingFish2ResultInfo();
            initFsResultInfo(slotConfigInfo);
            long totalWon = 0L;
            GameEngineCompute.initPayTableHit(model.getPayTable(), resultInfo);
            initFsSymbolInfo(model, resultInfo);
            for (int i = 0; i < simulationCount; i++) {
                spinCount++;
                totalWon = 0;
                Map gameLogicMap = new LinkedHashMap();
                gameLogicMap.put("lines", slotConfigInfo.getLines());
                gameLogicMap.put("bet", slotConfigInfo.getBet());
                gameLogicMap.put("denom", slotConfigInfo.getDenom());

                gameLogicBean = (SlotGameLogicBean) engine.gameStart(gameLogicBean, gameLogicMap, null, null);
                long totalBet = gameLogicBean.getSumBetCredit();
                initCredit -= totalBet;
                Model20260618SpinResult spinResult = (Model20260618SpinResult) gameLogicBean.getSlotSpinResult();

                long winCredit = gameLogicBean.getSumWinCredit();
                totalWon += winCredit;
                GameEngineCompute.computePayTableHit(gameLogicBean, gameLogicBean.getSlotSpinResult(), resultInfo, getScatterSymbol());
                long bonusWin = computeBonusWin(spinResult, resultInfo, totalBet);
                long baseWin = winCredit - bonusWin;
                if (winCredit > resultInfo.getBaseGameTopAward()) {
                    resultInfo.setBaseGameTopAward(winCredit);
                    resultInfo.setBaseTopAwardReelStop(StringUtil.IntegerArrayToStr(gameLogicBean.getSlotSpinResult().getSlotReelStopPosition(), " "));
                    resultInfo.setBaseTopAwardType("Base Normal");
                }

                if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                    if (baseWin > 0) {
                        resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                        resultInfo.setBaseGameTotalWin(resultInfo.getBaseGameTotalWin() + baseWin);
                    }
                } else {
                    resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                    if (baseWin > 0) {
                        resultInfo.setBaseGameTotalWin(resultInfo.getBaseGameTotalWin() + baseWin);
                    }
                    long fsCoinOut = 0L;
                    long fsTotalTimes = 0L;
                    int fsType = spinResult.getFsType();
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
                                computeScatterInfo(fsSpinResult, resultInfo, totalBet, fsType);
                                if (fsType == 2) {
                                    computeFsMul(fsSpinResult, resultInfo, freespinWon);
                                }

                                GameEngineCompute.addFreeSpinSymbolDetailInfo(fsSpinResult, resultInfo);
                                if (freespinWon > resultInfo.getFreespinTopAward()) {
                                    resultInfo.setFreespinTopAward(freespinWon);
                                    resultInfo.setFsTopAwardReelStop(StringUtil.IntegerArrayToStr(fsSpinResult.getSlotReelStopPosition(), " "));
                                    if (fsType == Model20260618.WR_FS) {
                                        resultInfo.setFsTopAwardType("WR FS");
                                    } else {
                                        resultInfo.setFsTopAwardType("SUPER FS");
                                    }
                                }

                            }

                            //end freespin
                            if (fsTotalTimes > 0) {
                                resultInfo.setFreespinTotalTimes(resultInfo.getFreespinTotalTimes() + fsTotalTimes);
                                resultInfo.setFreespinTotalHit(resultInfo.getFreespinTotalHit() + 1);
                                resultInfo.setFreespinTotalWin(resultInfo.getFreespinTotalWin() + fsCoinOut);
                                resultInfo.getFsHits()[fsType - 1]++;
                                resultInfo.getFsTimes()[fsType - 1] += fsTotalTimes;
                                resultInfo.getFsWin()[fsType - 1] += fsCoinOut;
                                if (fsType == Model20260618.WR_FS) {
                                    resultInfo.getWheelBonusWin()[10] += fsCoinOut;
                                } else {
                                    resultInfo.getWheelBonusWin()[11] += fsCoinOut;
                                }
                            }
                        } else if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                            break;
                        }

                    }

                }

                initCredit += totalWon;
                setBaseCommInfo(spinCount, initCredit, totalWon, gameLogicBean, resultInfo);

                if (spinCount > 0 && spinCount % playTime == 0) {
                    outResultInfo(slotConfigInfo, resultInfo);
                    outFsSymbolResultInfo(slotConfigInfo, resultInfo);
                }
            }

        } catch (InvalidGameStateException e) {
            log.error("engine gameStart", e);
            e.printStackTrace();
        } catch (Exception e) {
            log.error("cycleSpinForKingFish2 run exception", e);
        }

    }

    private void computeScatterInfo(SlotSpinResult fsSpinResult, KingFish2ResultInfo resultInfo, long totalBet, int fsType) {
        int[] hitSymbol = fsSpinResult.getHitSlotSymbols();
        long[] hitPay = fsSpinResult.getHitSlotPays();
        int[] hitCount = fsSpinResult.getHitSlotSymbolCount();
        if (hitSymbol != null) {
            for (int i = 0; i < hitSymbol.length; i++) {
                if (hitSymbol[i] == Model20260618Test.SCATTER_SYMBOL && hitCount[i] == 2) {
                    long winPay = hitPay[i] / totalBet;
                    int prizeIndex = getScatterWinIndex(winPay, Model20260618Test.SCATTER_AWARD_WEIGHT[0]);
                    if (prizeIndex >= 0) {
                        if (fsType == Model20260618.WR_FS) {
                            resultInfo.getFsWrScatterPrizeHits()[prizeIndex]++;
                            resultInfo.getFsWrScatterPrizeWin()[prizeIndex] += hitPay[i];
                        } else if (fsType == Model20260618.SUPER_FS) {
                            resultInfo.getFsSuperScatterPrizeHits()[prizeIndex]++;
                            resultInfo.getFsSuperScatterPrizeWin()[prizeIndex] += hitPay[i];
                        }

                    }
                    break;
                }
            }
        }
    }

    private int getScatterWinIndex(long winPay, int[] scatterPrizes) {
        for (int i = 0; i < scatterPrizes.length; i++) {
            if (winPay == scatterPrizes[i]) {
                return i;
            }
        }
        return -1;
    }

    private long computeBonusWin(Model20260618SpinResult spinResult, KingFish2ResultInfo resultInfo, long totalBet) {
        long bonusWin = 0L;
        if (spinResult != null) {
            int[] hitSymbol = spinResult.getHitSlotSymbols();
            long[] hitPay = spinResult.getHitSlotPays();
            int[] hitCount = spinResult.getHitSlotSymbolCount();
            if (hitSymbol != null) {
                for (int i = 0; i < hitSymbol.length; i++) {
                    if (hitSymbol[i] == Model20260618Test.SCATTER_SYMBOL && hitCount[i] == 3) {
                        int bonusType = getBonusType(hitPay[i], spinResult.getFsType(), totalBet);
                        if (hitPay[i] > 0) {
                            bonusWin = hitPay[i];
                            resultInfo.setBonusTotalHit(resultInfo.getBonusTotalHit() + 1);
                            resultInfo.setBonusTotalWin(resultInfo.getBonusTotalWin() + bonusWin);
                        }
                        resultInfo.getWheelBonusHits()[bonusType]++;
                        resultInfo.getWheelBonusWin()[bonusType] += bonusWin;
                        break;
                    }
                }
            }
        }
        return bonusWin;
    }

    private int getBonusType(long bonusWin, int fsType, long totalBet) {
        int bonusType = -1;
        if (bonusWin > 0) {
            long pay = bonusWin / totalBet;
            for (int i = 0; i < Model20260618Test.WHEEL_BONUS_AWARD.length; i++) {
                if (pay == Model20260618Test.WHEEL_BONUS_AWARD[i]) {
                    bonusType = i;
                    break;
                }
            }
        } else if (fsType > 0) {
            bonusType = fsType + 9;
        }
        return bonusType;

    }


    private void computeFsMul(SlotSpinResult fsSpinResult, KingFish2ResultInfo resultInfo, long freespinWon) {
        int fsMul = fsSpinResult.getFsMul();
        int[] fsIncMul = getLevelMultiplier();
        for (int j = 0; j < fsIncMul.length; j++) {
            if (fsMul == fsIncMul[j]) {
                resultInfo.getFsIncMulTimes()[j]++;
                if (freespinWon > 0) {
                    resultInfo.getFsIncMulHit()[j]++;
                    resultInfo.getFsIncMulWin()[j] += freespinWon;
                }
                break;
            }
        }
    }

    protected int[] getLevelMultiplier() {
        return new int[]{2, 3, 4, 5, 10};
    }

    private void outResultInfo(SlotConfigInfo configInfo, KingFish2ResultInfo resultInfo) {

        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append(StringUtil.getCommonHeaderInfo(resultInfo));
            strbHeader.append(StringUtil.getBonusHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getWheelBonusHits().length; i++) {
                strbHeader.append("Bonus Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getWheelBonusWin().length; i++) {
                strbHeader.append("Bonus Index").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsHits().length; i++) {
                strbHeader.append("Fs Type").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsTimes().length; i++) {
                strbHeader.append("Fs Type").append(i + 1).append(" Times").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWin().length; i++) {
                strbHeader.append("Fs Type").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsIncMulTimes().length; i++) {
                strbHeader.append("Fs Inc Mul").append(i + 1).append(" Times").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsIncMulHit().length; i++) {
                strbHeader.append("Fs Inc Mul").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsIncMulWin().length; i++) {
                strbHeader.append("Fs Inc Mul").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWrScatterPrizeHits().length; i++) {
                strbHeader.append("Fs Prize").append(Model20260618Test.SCATTER_AWARD_WEIGHT[0][i]).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWrScatterPrizeWin().length; i++) {
                strbHeader.append("Fs Prize").append(Model20260618Test.SCATTER_AWARD_WEIGHT[0][i]).append(" Win").append(BaseConstant.TAB_STR);
            }
            strbHeader.append(StringUtil.getPayTableHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getFsSuperScatterPrizeHits().length; i++) {
                strbHeader.append("Fs Prize").append(Model20260618Test.SCATTER_AWARD_WEIGHT[0][i]).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsSuperScatterPrizeWin().length; i++) {
                strbHeader.append("Fs Prize").append(Model20260618Test.SCATTER_AWARD_WEIGHT[0][i]).append(" Win").append(BaseConstant.TAB_STR);
            }
            FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), strbHeader.toString());
        }
        StringBuilder strContent = new StringBuilder();
        strContent.append(StringUtil.getBaseResultInfo(resultInfo));
        strContent.append(StringUtil.getBonusResultInfo(resultInfo));
        for (long bonusHit : resultInfo.getWheelBonusHits()) {
            strContent.append(bonusHit).append(BaseConstant.TAB_STR);
        }
        for (long bonusWin : resultInfo.getWheelBonusWin()) {
            strContent.append(bonusWin).append(BaseConstant.TAB_STR);
        }
        for (long fsHit : resultInfo.getFsHits()) {
            strContent.append(fsHit).append(BaseConstant.TAB_STR);
        }
        for (long fsTimes : resultInfo.getFsTimes()) {
            strContent.append(fsTimes).append(BaseConstant.TAB_STR);
        }
        for (long fsWin : resultInfo.getFsWin()) {
            strContent.append(fsWin).append(BaseConstant.TAB_STR);
        }
        for (long fsIncMulTimes : resultInfo.getFsIncMulTimes()) {
            strContent.append(fsIncMulTimes).append(BaseConstant.TAB_STR);
        }
        for (long fsIncMulHit : resultInfo.getFsIncMulHit()) {
            strContent.append(fsIncMulHit).append(BaseConstant.TAB_STR);
        }
        for (long fsIncMulWin : resultInfo.getFsIncMulWin()) {
            strContent.append(fsIncMulWin).append(BaseConstant.TAB_STR);
        }
        for (long prizeHit : resultInfo.getFsWrScatterPrizeHits()) {
            strContent.append(prizeHit).append(BaseConstant.TAB_STR);
        }
        for (long prizeWin : resultInfo.getFsWrScatterPrizeWin()) {
            strContent.append(prizeWin).append(BaseConstant.TAB_STR);
        }
        strContent.append(StringUtil.getPayTableHit(resultInfo));
        for (long prizeHit : resultInfo.getFsSuperScatterPrizeHits()) {
            strContent.append(prizeHit).append(BaseConstant.TAB_STR);
        }
        for (long prizeWin : resultInfo.getFsSuperScatterPrizeWin()) {
            strContent.append(prizeWin).append(BaseConstant.TAB_STR);
        }
        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getOutputFileName(), configInfo, 0);
    }


}
