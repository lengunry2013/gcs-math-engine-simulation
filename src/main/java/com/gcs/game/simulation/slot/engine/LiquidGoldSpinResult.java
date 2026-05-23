package com.gcs.game.simulation.slot.engine;

import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.engine.slots.vo.SlotSpinResult;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.simulation.slot.vo.LiquidGoldResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model20260520.Model20260520Test;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * LiquidGold spin result
 *
 * @author Jiangqx
 * @create 2020-08-27-8:02
 **/
@Slf4j
public class LiquidGoldSpinResult extends LittleDragonBunsSpinResult {

    public LiquidGoldSpinResult() {

    }

    public void cycleSpinForLiquidGold(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel model) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            LiquidGoldResultInfo resultInfo = new LiquidGoldResultInfo();
            initFsResultInfo(slotConfigInfo);

            long totalWon = 0L;
            SlotGameLogicBean gameSessionBean = (SlotGameLogicBean) baseGameLogicBean;
            Model20260520Test mathModel = (Model20260520Test) model;
            initFsSymbolInfo(mathModel, resultInfo);
            long totalPayCap = mathModel.maxTotalPay();

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

                long winCredit = gameSessionBean.getSumWinCredit();
                if (winCredit >= totalPayCap) {
                    winCredit = totalPayCap;
                    resultInfo.setTotalPayCapHit(resultInfo.getTotalPayCapHit() + 1);
                }
                totalWon += winCredit;
                //GameEngineCompute.computePayTableHit(gameSessionBean, gameSessionBean.getBaseSpinResult(), resultInfo, getScatterSymbol());
                int baseMysteryType = Model20260520Test.baseMysteryType;
                if (winCredit > resultInfo.getBaseGameTopAward()) {
                    resultInfo.setBaseGameTopAward(winCredit);
                    resultInfo.setBaseTopAwardReelStop(StringUtil.IntegerArrayToStr(gameSessionBean.getSlotSpinResult().getSlotReelStopPosition(), " "));
                    if (baseMysteryType > 0) {
                        resultInfo.setBaseTopAwardType("Base Mystery");
                    } else {
                        resultInfo.setBaseTopAwardType("Base Regular");
                    }
                }
                if (baseMysteryType > 0) {
                    resultInfo.getBaseTypeHit()[1]++;
                    resultInfo.getBaseTypeWin()[1] += winCredit;
                    resultInfo.getBaseMysteryTypeHit()[baseMysteryType - 1]++;
                    resultInfo.getBaseMysteryTypeWin()[baseMysteryType - 1] += winCredit;
                    int baseWrIndex = getWildIndex(mathModel.getWildReels(), gameSessionBean.getSlotSpinResult());
                    int baseStickyWildIndex = getBaseWildPositionIndex(mathModel.getStickyWild(), gameSessionBean.getSlotSpinResult(), mathModel.getReelsCount(), mathModel.getRowsCount());
                    switch (baseMysteryType) {
                        case 1:
                            resultInfo.getBaseWildRHit()[baseWrIndex]++;
                            resultInfo.getBaseWildRWin()[baseWrIndex] += winCredit;
                            break;
                        case 2:
                            resultInfo.getBaseStickyWildHit()[baseStickyWildIndex]++;
                            resultInfo.getBaseStickyWildWin()[baseStickyWildIndex] += winCredit;
                            break;
                        default:
                            break;
                    }
                } else {
                    resultInfo.getBaseTypeHit()[0]++;
                    resultInfo.getBaseTypeWin()[0] += winCredit;
                }

                if (gameSessionBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
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
                    int fsType = Model20260520Test.freespinType;
                    long fsCoinOut = 0L;
                    long fsTotalTimes = 0L;
                    int fsScriptIndex = Model20260520Test.freespinRandomIndex;
                    //start freespin or bonus
                    while (true) {
                        if (gameSessionBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                            while (gameSessionBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                                PlayerInputInfo playerInput = new PlayerInputInfo();
                                playerInput.setRequestGameStatus(200);
                                gameSessionBean = (SlotGameLogicBean) engine.gameProgress(gameSessionBean, gameLogicMap, playerInput, null, null, null);

                                long freespinWon = 0L;
                                SlotSpinResult fsSpinResult = gameSessionBean.getSlotFsSpinResults().get(gameSessionBean.getSlotFsSpinResults().size() - 1);
                                freespinWon = fsSpinResult.getSlotPay();
                                if (freespinWon >= totalPayCap) {
                                    freespinWon = totalPayCap;
                                    resultInfo.setTotalPayCapHit(resultInfo.getTotalPayCapHit() + 1);
                                }
                                totalWon += freespinWon;
                                fsCoinOut += freespinWon;
                                fsTotalTimes++;
                                int fsWildIndex = getWildIndex(mathModel.getWildReels(), fsSpinResult);
                                switch (fsType) {
                                    case 1:
                                        resultInfo.getFsStickyWildTimes()[fsScriptIndex]++;
                                        resultInfo.getFsStickyWildWin()[fsScriptIndex] += freespinWon;
                                        break;
                                    case 2:
                                        resultInfo.getFsWildRTimes()[fsWildIndex]++;
                                        resultInfo.getFsWildRWin()[fsWildIndex] += freespinWon;
                                        break;
                                    case 3:
                                        resultInfo.getFs3RwTimes()[fsScriptIndex]++;
                                        resultInfo.getFs3RwWin()[fsScriptIndex] += freespinWon;
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
                                    resultInfo.setFsTopAwardType(getFsHead(fsType - 1));
                                }
                            }

                            //end freespin
                            if (fsTotalTimes > 0) {
                                resultInfo.getFsHit()[fsType - 1]++;
                                if (fsType == 1) {
                                    resultInfo.getFsStickyWildHit()[fsScriptIndex]++;
                                } else if (fsType == 3) {
                                    resultInfo.getFs3RwHit()[fsScriptIndex]++;
                                }
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
            e.printStackTrace();
        } catch (Exception e) {
            log.error("cycleSpinForLiquidGold run exception", e);
        }

    }


    private int getBaseWildPositionIndex(int[][] stickyWild, SlotSpinResult baseSpinResult, int reelsCount, int rowsCount) {
        int wildIndex = 0;
        int[] wildPosition = baseSpinResult.getSlotWildPositions();
        if (wildPosition != null && wildPosition.length > 0) {
            int[] wildReelsCount = new int[reelsCount];
            for (int i = 0; i < wildPosition.length; i++) {
                int col = wildPosition[i] % reelsCount;
                wildReelsCount[col]++;
            }
            for (int i = 0; i < stickyWild.length; i++) {
                if (Arrays.equals(stickyWild[i], wildReelsCount)) {
                    wildIndex = i;
                    break;
                }
            }
        }
        return wildIndex;
    }

    private int getWildIndex(int[][] baseWildReels, SlotSpinResult baseSpinResult) {
        int wildIndex = 0;
        int[] wildReels = baseSpinResult.getSlotWildReels();
        if (wildReels != null && wildReels.length > 0) {
            for (int i = 0; i < baseWildReels.length; i++) {
                if (Arrays.equals(baseWildReels[i], wildReels)) {
                    wildIndex = i;
                    break;
                }
            }
        }
        return wildIndex;
    }

    protected int[] getScatterSymbol() {
        return new int[]{11};
    }

    protected void outResultInfo(SlotConfigInfo configInfo,
                                 LiquidGoldResultInfo resultInfo) {

        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append(StringUtil.getCommonHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getBaseTypeHit().length; i++) {
                strbHeader.append(getBaseHead(i)).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseTypeWin().length; i++) {
                strbHeader.append(getBaseHead(i)).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseMysteryTypeHit().length; i++) {
                strbHeader.append(getBaseMysteryHead(i)).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseMysteryTypeWin().length; i++) {
                strbHeader.append(getBaseMysteryHead(i)).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseWildRHit().length; i++) {
                strbHeader.append("Base WR Type").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseWildRWin().length; i++) {
                strbHeader.append("Base WR Type").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseStickyWildHit().length; i++) {
                strbHeader.append("Base SW Type").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseStickyWildWin().length; i++) {
                strbHeader.append("Base SW Type").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsHit().length; i++) {
                strbHeader.append(getFsHead(i)).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsTimes().length; i++) {
                strbHeader.append(getFsHead(i)).append(" Times").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWin().length; i++) {
                strbHeader.append(getFsHead(i)).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWildRTimes().length; i++) {
                strbHeader.append("Fs WR").append(i + 1).append(" Times").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsWildRWin().length; i++) {
                strbHeader.append("Fs WR").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsStickyWildHit().length; i++) {
                strbHeader.append("Fs SW").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsStickyWildTimes().length; i++) {
                strbHeader.append("Fs SW").append(i + 1).append(" Times").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsStickyWildWin().length; i++) {
                strbHeader.append("Fs SW").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFs3RwHit().length; i++) {
                strbHeader.append("Fs RW").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFs3RwTimes().length; i++) {
                strbHeader.append("Fs RW").append(i + 1).append(" Times").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFs3RwWin().length; i++) {
                strbHeader.append("Fs RW").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            strbHeader.append("Total Pay Cap Hit").append(BaseConstant.TAB_STR);
            FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), strbHeader.toString());
        }
        StringBuilder strContent = new StringBuilder();
        strContent.append(StringUtil.getBaseResultInfo(resultInfo));

        for (long baseTypeHit : resultInfo.getBaseTypeHit()) {
            strContent.append(baseTypeHit).append(BaseConstant.TAB_STR);
        }
        for (long baseTypeWin : resultInfo.getBaseTypeWin()) {
            strContent.append(baseTypeWin).append(BaseConstant.TAB_STR);
        }
        for (long baseMysteryTypeHit : resultInfo.getBaseMysteryTypeHit()) {
            strContent.append(baseMysteryTypeHit).append(BaseConstant.TAB_STR);
        }
        for (long baseMysteryTypeWin : resultInfo.getBaseMysteryTypeWin()) {
            strContent.append(baseMysteryTypeWin).append(BaseConstant.TAB_STR);
        }
        for (long baseWildRHit : resultInfo.getBaseWildRHit()) {
            strContent.append(baseWildRHit).append(BaseConstant.TAB_STR);
        }
        for (long baseWildRWin : resultInfo.getBaseWildRWin()) {
            strContent.append(baseWildRWin).append(BaseConstant.TAB_STR);
        }
        for (long baseSwHit : resultInfo.getBaseStickyWildHit()) {
            strContent.append(baseSwHit).append(BaseConstant.TAB_STR);
        }
        for (long baseSwWin : resultInfo.getBaseStickyWildWin()) {
            strContent.append(baseSwWin).append(BaseConstant.TAB_STR);
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
        for (long fsWrTimes : resultInfo.getFsWildRTimes()) {
            strContent.append(fsWrTimes).append(BaseConstant.TAB_STR);
        }
        for (long fsWrWin : resultInfo.getFsWildRWin()) {
            strContent.append(fsWrWin).append(BaseConstant.TAB_STR);
        }

        for (long fsSwHit : resultInfo.getFsStickyWildHit()) {
            strContent.append(fsSwHit).append(BaseConstant.TAB_STR);
        }
        for (long fsSwTimes : resultInfo.getFsStickyWildTimes()) {
            strContent.append(fsSwTimes).append(BaseConstant.TAB_STR);
        }
        for (long fsSwWin : resultInfo.getFsStickyWildWin()) {
            strContent.append(fsSwWin).append(BaseConstant.TAB_STR);
        }

        for (long fsRwHit : resultInfo.getFs3RwHit()) {
            strContent.append(fsRwHit).append(BaseConstant.TAB_STR);
        }
        for (long fsRwTimes : resultInfo.getFs3RwTimes()) {
            strContent.append(fsRwTimes).append(BaseConstant.TAB_STR);
        }
        for (long fsRwWin : resultInfo.getFs3RwWin()) {
            strContent.append(fsRwWin).append(BaseConstant.TAB_STR);
        }
        strContent.append(resultInfo.getTotalPayCapHit()).append(BaseConstant.TAB_STR);
        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getOutputFileName(), configInfo, 0);
    }

    private String getBaseMysteryHead(int index) {
        String str = "";
        switch (index) {
            case 0:
                str = "Base Wild Reels";
                break;
            case 1:
                str = "Base Sticky Wild";
                break;
            default:
                break;
        }
        return str;
    }

    private String getBaseHead(int index) {
        String str = "";
        switch (index) {
            case 0:
                str = "Base Regular";
                break;
            case 1:
                str = "Base Mystery";
                break;
            default:
                break;
        }
        return str;
    }

    private String getFsHead(int index) {
        String str = "";
        switch (index) {
            case 0:
                str = "FS Sticky Wild";
                break;
            case 1:
                str = "FS Wild Reels";
                break;
            case 2:
                str = "FS Random Wild";
                break;
            default:
                break;
        }
        return str;
    }


}
