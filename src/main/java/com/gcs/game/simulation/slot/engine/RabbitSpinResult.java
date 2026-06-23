package com.gcs.game.simulation.slot.engine;

import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.math.model20260530.Model20260530SpinResult;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.engine.slots.vo.SlotSpinResult;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.simulation.slot.vo.RabbitResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model20260530.Model20260530Test;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LiquidGold spin result
 *
 * @author Jiangqx
 * @create 2020-08-27-8:02
 **/
@Slf4j
public class RabbitSpinResult extends LittleDragonBunsSpinResult {

    public RabbitSpinResult() {

    }

    public void cycleSpinForRabbit(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel model) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            RabbitResultInfo resultInfo = new RabbitResultInfo();
            initFsResultInfo(slotConfigInfo);

            long totalWon = 0L;
            SlotGameLogicBean gameSessionBean = (SlotGameLogicBean) baseGameLogicBean;
            Model20260530Test mathModel = (Model20260530Test) model;
            initFsSymbolInfo(mathModel, resultInfo);
            GameEngineCompute.initPayTableHit(mathModel.getPayTable(), resultInfo);

            for (int i = 0; i < simulationCount; i++) {
                spinCount++;
                totalWon = 0;
                Map gameLogicMap = new LinkedHashMap();
                gameLogicMap.put("lines", slotConfigInfo.getLines());
                gameLogicMap.put("bet", slotConfigInfo.getBet());
                gameLogicMap.put("denom", slotConfigInfo.getDenom());

                long totalBet = gameSessionBean.getSumBetCredit();
                initCredit -= totalBet;

                gameSessionBean = (SlotGameLogicBean) engine.gameStart(gameSessionBean, gameLogicMap, null, null);
                Model20260530SpinResult spinResult = (Model20260530SpinResult) gameSessionBean.getSlotSpinResult();
                long winCredit = gameSessionBean.getSumWinCredit();
                totalWon += winCredit;
                GameEngineCompute.computePayTableHit(gameSessionBean, gameSessionBean.getSlotSpinResult(), resultInfo, getScatterSymbol());
                int featureType = spinResult.getFeatureType();
                if (featureType > 0) {
                    resultInfo.getBaseFeatureHit()[featureType - 1]++;
                    resultInfo.getBaseFeatureWin()[featureType - 1] += winCredit;
                }
                int baseScRandomIndex = spinResult.getBaseScRandomIndex();
                if (baseScRandomIndex >= 0) {
                    resultInfo.getBaseScTriggerHit()[baseScRandomIndex]++;
                }
                int baseWl = spinResult.getBaseWlRandomIndex();
                if (baseWl >= 0) {
                    resultInfo.getBaseWlHit()[baseWl]++;
                }
                int baseWinRandomIndex = spinResult.getBaseWinRandomIndex();
                if (baseWinRandomIndex >= 0) {
                    resultInfo.getBaseWinHit()[baseWinRandomIndex]++;
                }
                computeScType(spinResult, resultInfo);
                if (winCredit > resultInfo.getBaseGameTopAward()) {
                    resultInfo.setBaseGameTopAward(winCredit);
                    resultInfo.setBaseTopAwardReelStop(StringUtil.IntegerArrayToStr(gameSessionBean.getSlotSpinResult().getSlotReelStopPosition(), " "));
                    resultInfo.setBaseTopAwardType("Base");
                }

                if (gameSessionBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                    if (winCredit > 0) {
                        resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                        resultInfo.setBaseGameTotalWin(
                                resultInfo.getBaseGameTotalWin() + winCredit);
                    }
                } else {
                    if (winCredit > 0) {
                        resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                        resultInfo.setBaseGameTotalWin(
                                resultInfo.getBaseGameTotalWin() + winCredit);
                    }
                    long fsCoinOut = 0L;
                    long fsTotalTimes = 0L;
                    //start freespin or bonus
                    while (true) {
                        if (gameSessionBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                            while (gameSessionBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                                PlayerInputInfo playerInput = new PlayerInputInfo();
                                playerInput.setRequestGameStatus(200);
                                gameSessionBean = (SlotGameLogicBean) engine.gameProgress(gameSessionBean, gameLogicMap, playerInput, null, null, null);

                                long freespinWon = 0L;
                                Model20260530SpinResult fsSpinResult = (Model20260530SpinResult) gameSessionBean.getSlotFsSpinResults().get(gameSessionBean.getSlotFsSpinResults().size() - 1);
                                freespinWon = fsSpinResult.getSlotPay();
                                totalWon += freespinWon;
                                fsCoinOut += freespinWon;
                                fsTotalTimes++;
                                long scatterPrize = computeScPrize(fsSpinResult, resultInfo, totalBet);
                                int fsFeatureType = fsSpinResult.getFeatureType();
                                long fsLineWin = freespinWon - scatterPrize;
                                if (fsFeatureType > 0) {
                                    resultInfo.getFsFeatureHit()[fsFeatureType - 1]++;
                                    resultInfo.getFsFeatureWin()[fsFeatureType - 1] += fsLineWin;
                                } else {
                                    resultInfo.getFsFeatureHit()[5]++;
                                    resultInfo.getFsFeatureWin()[5] += fsLineWin;
                                }
                                int fsScRandomIndex = fsSpinResult.getFsScRandomIndex();
                                if (fsScRandomIndex >= 0) {
                                    resultInfo.getFsScTriggerHit()[fsScRandomIndex]++;
                                }
                                if (fsScRandomIndex < 0 && fsFeatureType < 0) {
                                    int[] hitSymbols = fsSpinResult.getHitSlotSymbols();
                                    int[] hitCount = fsSpinResult.getHitSlotSymbolCount();
                                    if (hitSymbols != null) {
                                        for (int j = 0; j < hitSymbols.length; j++) {
                                            if (hitSymbols[j] == Model20260530Test.FS_SCATTER_SYMBOL && hitCount[j] == 3) {
                                                resultInfo.setNormalFsScatterHit(resultInfo.getNormalFsScatterHit() + 1);
                                                break;
                                            }
                                        }
                                    }
                                }
                                int fsWlRandomIndex = fsSpinResult.getFsWlRandomIndex();
                                if (fsWlRandomIndex >= 0) {
                                    resultInfo.getFsWlExpandHit()[fsWlRandomIndex]++;
                                }
                                int wildMul = fsSpinResult.getWildMul();
                                if (wildMul >= 0) {
                                    int wildMulIndex = getWildMulIndex(wildMul);
                                    resultInfo.getFsWlMulHit()[wildMulIndex]++;
                                }
                                int wildAdd = fsSpinResult.getFsWlAddRandomIndex();
                                if (wildAdd > 0) {
                                    resultInfo.getFsWlAddHit()[wildAdd - 1]++;
                                }
                                int winRandomIndex = fsSpinResult.getFsWinRandomIndex();
                                if (winRandomIndex >= 0) {
                                    resultInfo.getFsWinHit()[winRandomIndex]++;
                                }
                                GameEngineCompute.addFreeSpinSymbolDetailInfo(fsSpinResult,
                                        resultInfo);
                                if (freespinWon > resultInfo.getFreespinTopAward()) {
                                    resultInfo.setFreespinTopAward(freespinWon);
                                    resultInfo.setFsTopAwardReelStop(StringUtil.IntegerArrayToStr(fsSpinResult.getSlotReelStopPosition(), " "));
                                    resultInfo.setFsTopAwardType("Fs");
                                }
                            }

                            //end freespin
                            if (fsTotalTimes > 0) {
                                resultInfo.setFreespinTotalTimes(resultInfo.getFreespinTotalTimes()
                                        + fsTotalTimes);
                                resultInfo.setFreespinTotalHit(
                                        resultInfo.getFreespinTotalHit() + 1);
                                resultInfo.setFreespinTotalWin(
                                        resultInfo.getFreespinTotalWin()
                                                + fsCoinOut);
                                if (baseScRandomIndex < 0 && featureType < 0) {
                                    resultInfo.getNormalScatterHit()[1]++;
                                    resultInfo.getNormalScatterWin()[1] += fsCoinOut;
                                }
                                if (featureType == 1) {
                                    resultInfo.getBaseFeatureWin()[featureType - 1] += fsCoinOut;
                                }
                            }
                        } else if (gameSessionBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_BONUS) {
                            long bonusWin = computePickBonus(engine, gameSessionBean, gameLogicMap, resultInfo, slotConfigInfo);
                            totalWon += bonusWin;
                            if (baseScRandomIndex < 0 && featureType < 0) {
                                resultInfo.getNormalScatterHit()[0]++;
                                resultInfo.getNormalScatterWin()[0] += bonusWin;
                            }
                            if (featureType == 1) {
                                resultInfo.getBaseFeatureWin()[featureType - 1] += bonusWin;
                            }
                        } else if (gameSessionBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                            break;
                        }

                    }

                }

                initCredit += totalWon;
                setBaseCommInfo(spinCount, initCredit, totalWon, gameSessionBean, resultInfo);
                if (spinCount > 0 && spinCount % playTime == 0) {
                    outResultInfo(slotConfigInfo, resultInfo);
                    outFsSymbolResultInfo(slotConfigInfo, resultInfo);
                }
            }
        } catch (InvalidGameStateException e) {
            log.error("engine gameStart", e);
            e.printStackTrace();
        } catch (Exception e) {
            log.error("cycleSpinForRabbit run exception", e);
        }

    }

    private void computeScType(Model20260530SpinResult spinResult, RabbitResultInfo resultInfo) {
        int[] displaySymbols = spinResult.getSlotDisplaySymbols();
        for (int symbol : displaySymbols) {
            if (symbol == Model20260530Test.SCATTER_SYMBOL) {
                resultInfo.getScHit()[0]++;
                break;
            }
            if (symbol == Model20260530Test.FS_SCATTER_SYMBOL) {
                resultInfo.getScHit()[1]++;
                break;
            }
        }
    }

    private long computeScPrize(Model20260530SpinResult fsSpinResult, RabbitResultInfo resultInfo, long totalBet) {
        int[] hitSymbols = fsSpinResult.getHitSlotSymbols();
        int[] hitCount = fsSpinResult.getHitSlotSymbolCount();
        long[] hitPay = fsSpinResult.getHitSlotPays();
        long scatterPrize = 0;
        if (hitSymbols != null) {
            for (int j = 0; j < hitSymbols.length; j++) {
                if (hitSymbols[j] == Model20260530Test.FS_SCATTER_SYMBOL && hitCount[j] == 1) {
                    long winPay = hitPay[j] / totalBet;
                    int winPayIndex = getWildMulIndex((int) winPay);
                    resultInfo.getFsScatterPrizeHit()[winPayIndex]++;
                    resultInfo.getFsScatterPrizeWin()[winPayIndex] += hitPay[j];
                    scatterPrize += hitPay[j];
                }
            }
        }
        return scatterPrize;
    }

    private int getWildMulIndex(int wildMul) {
        int index = 0;
        for (int j = 0; j < Model20260530Test.FS_WL_MUL_WEIGHT[0].length; j++) {
            if (wildMul == Model20260530Test.FS_WL_MUL_WEIGHT[0][j]) {
                index = j;
            }
        }
        return index;
    }

    protected int[] getScatterSymbol() {
        return new int[]{12, 13};
    }

    protected void outResultInfo(SlotConfigInfo configInfo,
                                 RabbitResultInfo resultInfo) {

        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append(StringUtil.getCommonHeaderInfo(resultInfo));
            strbHeader.append(StringUtil.getBonusHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getScHit().length; i++) {
                strbHeader.append("SC").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseFeatureHit().length; i++) {
                strbHeader.append("Base Feature").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseFeatureWin().length; i++) {
                strbHeader.append("Base Feature").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseScTriggerHit().length; i++) {
                strbHeader.append("Base Sc Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseWlHit().length; i++) {
                strbHeader.append("Base Wl Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseWinHit().length; i++) {
                strbHeader.append("Base Win Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getNormalScatterHit().length; i++) {
                strbHeader.append("Base Normal Sc").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getNormalScatterWin().length; i++) {
                strbHeader.append("Base Normal Sc").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsScatterPrizeHit().length; i++) {
                strbHeader.append("Fs Scatter Prize").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsScatterPrizeWin().length; i++) {
                strbHeader.append("Fs Scatter Prize").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsFeatureHit().length; i++) {
                strbHeader.append("Fs Feature").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsFeatureWin().length; i++) {
                strbHeader.append("Fs Feature").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            strbHeader.append("Fs Normal Sc2 Hit").append(BaseConstant.TAB_STR);
            for (int i = 0; i < resultInfo.getFsScTriggerHit().length; i++) {
                strbHeader.append("Fs Sc Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWlExpandHit().length; i++) {
                strbHeader.append("Fs Wl Expand Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWlMulHit().length; i++) {
                strbHeader.append("Fs Wl Mul").append(Model20260530Test.FS_WL_MUL_WEIGHT[0][i]).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWlAddHit().length; i++) {
                strbHeader.append("Fs Wl Add").append(Model20260530Test.FS_WL_ADD_WEIGHT[0][i]).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWinHit().length; i++) {
                strbHeader.append("Fs Win Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            strbHeader.append(StringUtil.getPayTableHeaderInfo(resultInfo));
            FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), strbHeader.toString());
        }
        StringBuilder strContent = new StringBuilder();
        strContent.append(StringUtil.getBaseResultInfo(resultInfo));
        strContent.append(StringUtil.getBonusResultInfo(resultInfo));
        for (long scHit : resultInfo.getScHit()) {
            strContent.append(scHit).append(BaseConstant.TAB_STR);
        }
        for (long fHit : resultInfo.getBaseFeatureHit()) {
            strContent.append(fHit).append(BaseConstant.TAB_STR);
        }
        for (long fWin : resultInfo.getBaseFeatureWin()) {
            strContent.append(fWin).append(BaseConstant.TAB_STR);
        }
        for (long scHit : resultInfo.getBaseScTriggerHit()) {
            strContent.append(scHit).append(BaseConstant.TAB_STR);
        }
        for (long wlHit : resultInfo.getBaseWlHit()) {
            strContent.append(wlHit).append(BaseConstant.TAB_STR);
        }
        for (long winHit : resultInfo.getBaseWinHit()) {
            strContent.append(winHit).append(BaseConstant.TAB_STR);
        }
        for (long normalScHit : resultInfo.getNormalScatterHit()) {
            strContent.append(normalScHit).append(BaseConstant.TAB_STR);
        }
        for (long normalScWin : resultInfo.getNormalScatterWin()) {
            strContent.append(normalScWin).append(BaseConstant.TAB_STR);
        }
        for (long scPrizeHit : resultInfo.getFsScatterPrizeHit()) {
            strContent.append(scPrizeHit).append(BaseConstant.TAB_STR);
        }
        for (long scPrizeWin : resultInfo.getFsScatterPrizeWin()) {
            strContent.append(scPrizeWin).append(BaseConstant.TAB_STR);
        }
        for (long FsFeatureHit : resultInfo.getFsFeatureHit()) {
            strContent.append(FsFeatureHit).append(BaseConstant.TAB_STR);
        }
        for (long FsFeatureWin : resultInfo.getFsFeatureWin()) {
            strContent.append(FsFeatureWin).append(BaseConstant.TAB_STR);
        }
        strContent.append(resultInfo.getNormalFsScatterHit()).append(BaseConstant.TAB_STR);
        for (long FsScHit : resultInfo.getFsScTriggerHit()) {
            strContent.append(FsScHit).append(BaseConstant.TAB_STR);
        }
        for (long FsWlExpandHit : resultInfo.getFsWlExpandHit()) {
            strContent.append(FsWlExpandHit).append(BaseConstant.TAB_STR);
        }
        for (long FsWlMulHit : resultInfo.getFsWlMulHit()) {
            strContent.append(FsWlMulHit).append(BaseConstant.TAB_STR);
        }
        for (long FsWlAddHit : resultInfo.getFsWlAddHit()) {
            strContent.append(FsWlAddHit).append(BaseConstant.TAB_STR);
        }
        for (long FsWinHit : resultInfo.getFsWinHit()) {
            strContent.append(FsWinHit).append(BaseConstant.TAB_STR);
        }
        strContent.append(StringUtil.getPayTableHit(resultInfo));
        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getOutputFileName(), configInfo, 0);
    }

}
