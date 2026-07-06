package com.gcs.game.simulation.slot.engine;

import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.math.model20260530.Model20260530SpinResult;
import com.gcs.game.engine.math.model20260701.Model20260701SpinResult;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.simulation.slot.vo.GoldRingCircusResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model20260701.Model20260701Test;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GoldRingCircus spin result
 *
 * @author Jiangqx
 * @create 2026-07-03-8:02
 **/
@Slf4j
public class GoldRingCircusSpinResult extends LittleDragonBunsSpinResult {

    public GoldRingCircusSpinResult() {

    }

    public void cycleSpinForGoldRingCircus(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel model) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            GoldRingCircusResultInfo resultInfo = new GoldRingCircusResultInfo();
            initFsResultInfo(slotConfigInfo);

            long totalWon = 0L;
            SlotGameLogicBean gameSessionBean = (SlotGameLogicBean) baseGameLogicBean;
            Model20260701Test mathModel = (Model20260701Test) model;
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
                Model20260701SpinResult spinResult = (Model20260701SpinResult) gameSessionBean.getSlotSpinResult();
                long winCredit = gameSessionBean.getSumWinCredit();
                totalWon += winCredit;
                GameEngineCompute.computePayTableHit(gameSessionBean, gameSessionBean.getSlotSpinResult(), resultInfo, getScatterSymbol());
                long scEntryWin = computeScWin(resultInfo, spinResult, true);
                long linkBonusWin = computeSwInfo(resultInfo, spinResult, totalBet, true);
                long baseWin = winCredit - linkBonusWin;
                if (winCredit > resultInfo.getBaseGameTopAward()) {
                    resultInfo.setBaseGameTopAward(winCredit);
                    resultInfo.setBaseTopAwardReelStop(StringUtil.IntegerArrayToStr(gameSessionBean.getSlotSpinResult().getSlotReelStopPosition(), " "));
                    resultInfo.setBaseTopAwardType("Base");
                }

                if (gameSessionBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                    if (baseWin > 0) {
                        resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                        resultInfo.setBaseGameTotalWin(
                                resultInfo.getBaseGameTotalWin() + baseWin);
                    }
                } else {
                    if (baseWin > 0) {
                        resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                        resultInfo.setBaseGameTotalWin(
                                resultInfo.getBaseGameTotalWin() + baseWin);
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
                                Model20260701SpinResult fsSpinResult = (Model20260701SpinResult) gameSessionBean.getSlotFsSpinResults().get(gameSessionBean.getSlotFsSpinResults().size() - 1);
                                freespinWon = fsSpinResult.getSlotPay();
                                totalWon += freespinWon;
                                fsCoinOut += freespinWon;
                                fsTotalTimes++;
                                computeSwInfo(resultInfo, fsSpinResult, totalBet, false);
                                computeScWin(resultInfo, fsSpinResult, false);

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
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            log.error("cycleSpinForGoldRingCircus run exception", e);
        }

    }

    private long computeSwInfo(GoldRingCircusResultInfo resultInfo, Model20260701SpinResult spinResult, long totalBet, boolean isSlot) {
        int[] hitSymbols = spinResult.getHitSlotSymbols();
        int[] hitCounts = spinResult.getHitSlotSymbolCount();
        long[] hitPays = spinResult.getHitSlotPays();
        long linkBonusWin = 0;
        if (hitSymbols != null) {
            for (int i = 0; i < hitSymbols.length; i++) {
                if (hitSymbols[i] == Model20260701Test.SW_SYMBOL && hitCounts[i] >= Model20260701Test.TRIGGER_SW) {
                    if (isSlot) {
                        int ballsCount = hitCounts[i];
                        int[] linkRandomIndexs = spinResult.getLinkBonusRandomPayIndexs();
                        int[] linkSwPays = spinResult.getLinkBonusSwPays();
                        long totalCoinPay = 0;
                        for (int j = 0; j < linkRandomIndexs.length; j++) {
                            int randomIndex = linkRandomIndexs[j];
                            if (randomIndex >= 0) {
                                long coinPay = linkSwPays[j] * totalBet;
                                resultInfo.getLinkBonusHit()[randomIndex]++;
                                resultInfo.getLinkBonusWin()[randomIndex] += coinPay;
                                totalCoinPay += coinPay;
                            }
                        }
                        long colWin = spinResult.getColWin() * totalBet;
                        resultInfo.getLinkBonusCount()[ballsCount - 6]++;
                        resultInfo.getLinkBonusEndWin()[ballsCount - 6] += hitPays[i];
                        resultInfo.getBaseCoinWin()[ballsCount - 6] += totalCoinPay;
                        if (colWin > 0) {
                            resultInfo.getColHit()[ballsCount - 6]++;
                            resultInfo.getColTotalWin()[ballsCount - 6] += colWin;
                        }
                        resultInfo.getRespinTotalTimes()[ballsCount - 6] += spinResult.getRespinTimes();
                        long grandWin = spinResult.getGrandWin() * totalBet;
                        if (grandWin > 0) {
                            resultInfo.getGrandHit()[ballsCount - 6]++;
                            resultInfo.getGrandTotalWin()[ballsCount - 6] += grandWin;
                        }
                        //link bonus win
                        resultInfo.setBonusTotalHit(resultInfo.getBonusTotalHit() + 1);
                        resultInfo.setBonusTotalWin(resultInfo.getBonusTotalWin() + hitPays[i]);
                    } else {
                        resultInfo.setFsTriggerBonusHit(resultInfo.getFsTriggerBonusHit() + 1);
                        resultInfo.setFsTriggerBonusWin(resultInfo.getFsTriggerBonusWin() + hitPays[i]);
                    }
                    linkBonusWin = hitPays[i];
                    break;
                }
            }
        }
        return linkBonusWin;
    }

    private long computeScWin(GoldRingCircusResultInfo resultInfo, Model20260701SpinResult spinResult, boolean isSlot) {
        int[] hitSymbols = spinResult.getHitSlotSymbols();
        int[] hitCounts = spinResult.getHitSlotSymbolCount();
        long[] hitPays = spinResult.getHitSlotPays();
        if (hitSymbols != null) {
            for (int i = 0; i < hitSymbols.length; i++) {
                if (hitSymbols[i] == Model20260701Test.SCATTER_SYMBOL) {
                    int count = hitCounts[i];
                    if (isSlot) {
                        resultInfo.getScHit()[count - 3]++;
                        resultInfo.getScEntryWin()[count - 3] += hitPays[i];
                    } else {
                        resultInfo.getFsScHit()[count - 3]++;
                        resultInfo.getFsScEntryWin()[count - 3] += hitPays[i];
                    }
                    return hitPays[i];
                }
            }
        }
        return 0;
    }

    protected int[] getScatterSymbol() {
        return new int[]{12, 13};
    }

    protected void outResultInfo(SlotConfigInfo configInfo,
                                 GoldRingCircusResultInfo resultInfo) {

        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append(StringUtil.getCommonHeaderInfo(resultInfo));
            strbHeader.append(StringUtil.getBonusHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getScHit().length; i++) {
                strbHeader.append("SC").append(i + 3).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getScEntryWin().length; i++) {
                strbHeader.append("SC").append(i + 3).append(" Entry Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getLinkBonusHit().length; i++) {
                strbHeader.append("bonus Pay").append(Model20260701Test.LINK_BALL_AWARDS[0][i]).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getLinkBonusWin().length; i++) {
                strbHeader.append("bonus Pay").append(Model20260701Test.LINK_BALL_AWARDS[0][i]).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getLinkBonusCount().length; i++) {
                strbHeader.append("bonus Balls").append(i + 6).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getLinkBonusEndWin().length; i++) {
                strbHeader.append("bonus Balls").append(i + 6).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseCoinWin().length; i++) {
                strbHeader.append("coin").append(i + 6).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getColHit().length; i++) {
                strbHeader.append("bonus col").append(i + 6).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getColTotalWin().length; i++) {
                strbHeader.append("bonus col").append(i + 6).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getGrandHit().length; i++) {
                strbHeader.append("bonus grand").append(i + 6).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getGrandTotalWin().length; i++) {
                strbHeader.append("bonus grand").append(i + 6).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getRespinTotalTimes().length; i++) {
                strbHeader.append("bonus spin").append(i + 6).append(" Times").append(BaseConstant.TAB_STR);
            }
            strbHeader.append("Fs Trigger Link Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("Fs Trigger Link Win").append(BaseConstant.TAB_STR);
            for (int i = 0; i < resultInfo.getFsScHit().length; i++) {
                strbHeader.append("Fs Sc").append(i + 3).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getScEntryWin().length; i++) {
                strbHeader.append("Fs Sc").append(i + 3).append(" Entry Win").append(BaseConstant.TAB_STR);
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
        for (long scWin : resultInfo.getScEntryWin()) {
            strContent.append(scWin).append(BaseConstant.TAB_STR);
        }
        for (long bonusHit : resultInfo.getLinkBonusHit()) {
            strContent.append(bonusHit).append(BaseConstant.TAB_STR);
        }
        for (long bonusWin : resultInfo.getLinkBonusWin()) {
            strContent.append(bonusWin).append(BaseConstant.TAB_STR);
        }
        for (long bonusCount : resultInfo.getLinkBonusCount()) {
            strContent.append(bonusCount).append(BaseConstant.TAB_STR);
        }
        for (long bonusTotalWin : resultInfo.getLinkBonusEndWin()) {
            strContent.append(bonusTotalWin).append(BaseConstant.TAB_STR);
        }
        for (long bonusCoinWin : resultInfo.getBaseCoinWin()) {
            strContent.append(bonusCoinWin).append(BaseConstant.TAB_STR);
        }
        for (long colHit : resultInfo.getColHit()) {
            strContent.append(colHit).append(BaseConstant.TAB_STR);
        }
        for (long colWin : resultInfo.getColTotalWin()) {
            strContent.append(colWin).append(BaseConstant.TAB_STR);
        }
        for (long grandHit : resultInfo.getGrandHit()) {
            strContent.append(grandHit).append(BaseConstant.TAB_STR);
        }
        for (long grandWin : resultInfo.getGrandTotalWin()) {
            strContent.append(grandWin).append(BaseConstant.TAB_STR);
        }
        for (long totalTimes : resultInfo.getRespinTotalTimes()) {
            strContent.append(totalTimes).append(BaseConstant.TAB_STR);
        }
        strContent.append(resultInfo.getFsTriggerBonusHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFsTriggerBonusWin()).append(BaseConstant.TAB_STR);
        for (long scHit : resultInfo.getFsScHit()) {
            strContent.append(scHit).append(BaseConstant.TAB_STR);
        }
        for (long scWin : resultInfo.getFsScEntryWin()) {
            strContent.append(scWin).append(BaseConstant.TAB_STR);
        }
        strContent.append(StringUtil.getPayTableHit(resultInfo));
        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getOutputFileName(), configInfo, 0);
    }

}
