package com.gcs.game.simulation.slot.engine;


import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.math.model1010802.Model1010802SpinResult;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.utils.SlotEngineConstant;
import com.gcs.game.engine.slots.vo.SlotBonusResult;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.engine.slots.vo.SlotSpinResult;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.exception.InvalidPlayerInputException;
import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import com.gcs.game.simulation.slot.common.vo.SymbolResultInfo;
import com.gcs.game.simulation.slot.vo.EsqueletoExplosivoResultInfo;
import com.gcs.game.simulation.slot.vo.LittleDragonBunsResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model1010802.Model1010802Test;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class EsqueletoExplosivoSpinResult {

    public static final String FS_FILE = "fsResult1.txt";
    public static final String SYMBOL_FILE = "displaySymbol1.txt";

    public EsqueletoExplosivoSpinResult() {

    }

    public void cycleSpinForEsqueletoExplosivo(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel baseSlotModel) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            SlotGameLogicBean gameLogicBean = (SlotGameLogicBean) baseGameLogicBean;
            Model1010802Test model = (Model1010802Test) baseSlotModel;
            EsqueletoExplosivoResultInfo resultInfo = new EsqueletoExplosivoResultInfo();
            initFsResultInfo(slotConfigInfo);
            //initSymbolResult(slotConfigInfo);
            long totalWon = 0L;
            GameEngineCompute.initPayTableHit(model.getPayTable(), resultInfo);
            initFsSymbolInfo(model, resultInfo);
            for (int i = 0; i < simulationCount; i++) {
                spinCount++;
                totalWon = 0;
                resultInfo.setBaseChainReaction(false);
                resultInfo.setFsChainReaction(false);
                resultInfo.setFsTriCollectWild(false);
                Map gameLogicMap = new LinkedHashMap();
                gameLogicMap.put("lines", slotConfigInfo.getLines());
                gameLogicMap.put("bet", slotConfigInfo.getBet());
                gameLogicMap.put("denom", slotConfigInfo.getDenom());

                gameLogicBean = (SlotGameLogicBean) engine.gameStart(gameLogicBean, gameLogicMap, null, null);
                long totalBet = gameLogicBean.getSumBetCredit();
                initCredit -= totalBet;

                long winCredit = gameLogicBean.getSumWinCredit();
                totalWon += winCredit;
                int baseReelsType = gameLogicBean.getBaseReelsType();
                resultInfo.getBaseReelsHit()[baseReelsType - 1]++;
                resultInfo.getBaseReelsWin()[baseReelsType - 1] += winCredit;
                Model1010802SpinResult baseSpinResult = (Model1010802SpinResult) gameLogicBean.getSlotSpinResult();
                List<Integer> wildPositionsOnReel = baseSpinResult.getWildPositionsOnReel();
                if (wildPositionsOnReel != null && !wildPositionsOnReel.isEmpty()) {
                    resultInfo.setBaseWildHit(resultInfo.getBaseWildHit() + 1);
                }
                int mulIndex = computeMulIndex(model.getBaseIncreaseMul(), baseSpinResult.getBaseGameMul());
                if (winCredit > 0 || (baseSpinResult.getWildPositionsOnReel() != null && baseSpinResult.getWildPositionsOnReel().size() > 0)) {
                    resultInfo.getBaseReelsMulHit()[baseReelsType - 1][mulIndex]++;
                    resultInfo.getBaseReelsMulWin()[baseReelsType - 1][mulIndex] += winCredit;
                }
                int scatterCount = computeScatter(baseSpinResult, resultInfo, true, baseReelsType);
                GameEngineCompute.computePayTableHit(gameLogicBean, gameLogicBean.getSlotSpinResult(), resultInfo, getScatterSymbol());
                if (winCredit > resultInfo.getBaseGameTopAward()) {
                    resultInfo.setBaseGameTopAward(winCredit);
                    resultInfo.setBaseTopAwardReelStop(StringUtil.IntegerArrayToStr(gameLogicBean.getSlotSpinResult().getSlotReelStopPosition(), " "));
                    resultInfo.setBaseTopAwardType("Base Normal");
                }
                resultInfo.setBaseDisplaySymbols(baseSpinResult.getSlotDisplaySymbols());

                if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                    if (winCredit > 0) {
                        resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                        resultInfo.setBaseGameTotalWin(resultInfo.getBaseGameTotalWin() + winCredit);
                        resultInfo.getBaseNotRespinWin()[baseReelsType - 1] += winCredit;
                    }
                } else {
                    resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                    if (winCredit > 0) {
                        resultInfo.setBaseGameTotalWin(resultInfo.getBaseGameTotalWin() + winCredit);
                        resultInfo.getBaseNotRespinWin()[baseReelsType - 1] += winCredit;
                    }
                    if (gameLogicBean.getRespinCountsLeft() > 0) {
                        resultInfo.getBaseRespinHit()[baseReelsType - 1]++;
                        resultInfo.setBaseChainReaction(true);
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

                                Model1010802SpinResult fsSpinResult = (Model1010802SpinResult) gameLogicBean.getSlotFsSpinResults().get(gameLogicBean.getSlotFsSpinResults().size() - 1);
                                Model1010802SpinResult lastFsSpinResult = null;
                                if (gameLogicBean.getSlotFsSpinResults().size() > 1) {
                                    lastFsSpinResult = (Model1010802SpinResult) gameLogicBean.getSlotFsSpinResults().get(gameLogicBean.getSlotFsSpinResults().size() - 2);
                                } else {
                                    lastFsSpinResult = baseSpinResult;
                                }
                                long freespinWon = fsSpinResult.getSlotPay();
                                resultInfo.setFsDisplaySymbols(fsSpinResult.getSlotDisplaySymbols());
                                //baseGame random fs reels type
                                int fsReelsType = baseSpinResult.getFsReelsType();
                                wildPositionsOnReel = fsSpinResult.getWildPositionsOnReel();
                                totalWon += freespinWon;
                                if (fsSpinResult.getSpinType() == SlotEngineConstant.SPIN_TYPE_RESPIN_IN_BASE_GAME) {
                                    resultInfo.setBaseGameTotalWin(resultInfo.getBaseGameTotalWin() + freespinWon);
                                    resultInfo.getBaseReelsWin()[baseReelsType - 1] += freespinWon;
                                    //mulIndex = computeMulIndex(model.getBaseIncreaseMul(), fsSpinResult.getFsMul());
                                    //System.out.println("respin next mul level=" + lastFsSpinResult.getRespinNextMulLevel());
                                    mulIndex = lastFsSpinResult.getRespinNextMulLevel() - 1;
                                    if (freespinWon > 0 || (fsSpinResult.getWildPositionsOnReel() != null && fsSpinResult.getWildPositionsOnReel().size() > 0)) {
                                        resultInfo.getBaseReelsMulHit()[baseReelsType - 1][mulIndex]++;
                                        resultInfo.getBaseReelsMulWin()[baseReelsType - 1][mulIndex] += freespinWon;
                                    }
                                    if (wildPositionsOnReel != null && !wildPositionsOnReel.isEmpty()) {
                                        resultInfo.setBaseWildHit(resultInfo.getBaseWildHit() + 1);
                                    }
                                    resultInfo.getBaseRespinTime()[baseReelsType - 1]++;
                                    resultInfo.getBaseRespinWin()[baseReelsType - 1] += freespinWon;
                                    int respinScatterCount = computeScatter(fsSpinResult, resultInfo, true, baseReelsType);
                                    if (respinScatterCount >= 3) {
                                        scatterCount = respinScatterCount;
                                    }
                                    if (freespinWon > resultInfo.getBaseGameTopAward()) {
                                        resultInfo.setBaseGameTopAward(freespinWon);
                                        resultInfo.setBaseTopAwardReelStop(StringUtil.IntegerArrayToStr(fsSpinResult.getSlotReelStopPosition(), " "));
                                        resultInfo.setBaseTopAwardType("Base Respin");
                                    }
                                } else {
                                    fsCoinOut += freespinWon;
                                    resultInfo.getBaseScatterWin()[baseReelsType - 1][scatterCount - 3] += freespinWon;
                                    mulIndex = computeMulIndex(model.getFsIncreaseMul(), fsSpinResult.getFsMul());
                                    if (freespinWon > 0 || (fsSpinResult.getWildPositionsOnReel() != null && fsSpinResult.getWildPositionsOnReel().size() > 0)) {
                                        resultInfo.getFsReelsMulHit()[fsReelsType - 1][mulIndex]++;
                                        resultInfo.getFsReelsMulWin()[fsReelsType - 1][mulIndex] += freespinWon;
                                    }
                                    if (wildPositionsOnReel != null && !wildPositionsOnReel.isEmpty()) {
                                        resultInfo.setFsWildHit(resultInfo.getFsWildHit() + 1);
                                    }
                                    if (fsSpinResult.isTriggerCollectWild()) {
                                        resultInfo.setFsTriCollectWildHit(resultInfo.getFsTriCollectWildHit() + 1);
                                        resultInfo.setFsTriCollectWild(fsSpinResult.isTriggerCollectWild());
                                    }
                                    computeScatter(fsSpinResult, resultInfo, false, fsReelsType);
                                    if (gameLogicBean.isRespin()) {
                                        resultInfo.getFsRespinTime()[fsReelsType - 1]++;
                                        resultInfo.getFsRespinWin()[fsReelsType - 1] += freespinWon;
                                    } else {
                                        if (fsSpinResult.isTriggerRespin()) {
                                            resultInfo.getFsRespinHit()[fsReelsType - 1]++;
                                            resultInfo.setFsChainReaction(true);
                                        }
                                        resultInfo.getFsReelsTimes()[fsReelsType - 1]++;
                                        fsTotalTimes++;
                                    }
                                    resultInfo.getFsReelsWin()[fsReelsType - 1] += freespinWon;
                                    GameEngineCompute.addFreeSpinSymbolDetailInfo(fsSpinResult, resultInfo);
                                    if (freespinWon > resultInfo.getFreespinTopAward()) {
                                        resultInfo.setFreespinTopAward(freespinWon);
                                        resultInfo.setFsTopAwardReelStop(StringUtil.IntegerArrayToStr(fsSpinResult.getSlotReelStopPosition(), " "));
                                        resultInfo.setFsTopAwardType("FS");
                                    }
                                }
                            }

                            //end freespin
                            if (fsTotalTimes > 0) {
                                int fsReelsType = baseSpinResult.getFsReelsType();
                                if (fsReelsType > 0) {
                                    resultInfo.getFsReelsHit()[fsReelsType - 1]++;
                                }
                                resultInfo.setFreespinTotalTimes(resultInfo.getFreespinTotalTimes() + fsTotalTimes);
                                resultInfo.setFreespinTotalHit(resultInfo.getFreespinTotalHit() + 1);
                                resultInfo.setFreespinTotalWin(resultInfo.getFreespinTotalWin() + fsCoinOut);
                            }
                        } else if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                            break;
                        }

                    }

                }

                if (totalWon >= model.maxWin() * totalBet) {
                    resultInfo.setUpMaxWinHit(resultInfo.getUpMaxWinHit() + 1);
                }
                initCredit += totalWon;
                setBaseCommInfo(spinCount, initCredit, totalWon, gameLogicBean, resultInfo);

                if (spinCount > 0 && spinCount % playTime == 0) {
                    outResultInfo(slotConfigInfo, resultInfo);
                    outFsSymbolResultInfo(slotConfigInfo, resultInfo);
                    //outSymbolResultInfo(slotConfigInfo, resultInfo);
                }
            }

        } catch (InvalidGameStateException e1) {
            log.error("engine gameStart", e1);
            e1.printStackTrace();
        } catch (Exception e) {
            log.error("cycleSpinForLittleDragonBuns run exception", e);
            e.printStackTrace();
        }

    }

    private void outSymbolResultInfo(SlotConfigInfo configInfo, EsqueletoExplosivoResultInfo resultInfo) {
        StringBuilder strContent = new StringBuilder();
        for (int baseSymbol : resultInfo.getBaseDisplaySymbols()) {
            strContent.append(baseSymbol).append(BaseConstant.TAB_STR);
        }
        for (int fsSymbol : resultInfo.getFsDisplaySymbols()) {
            strContent.append(fsSymbol).append(BaseConstant.TAB_STR);
        }
        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getHighMissFileName(), configInfo, 2);
    }

    private int computeScatter(Model1010802SpinResult baseSpinResult, EsqueletoExplosivoResultInfo resultInfo, boolean isSlot, int reelsType) {
        int[] hitSymbols = baseSpinResult.getHitSlotSymbols();
        int[] hitSymbolCount = baseSpinResult.getHitSlotSymbolCount();
        int scatterCount = 0;
        if (hitSymbols != null && hitSymbols.length > 0) {
            for (int i = 0; i < hitSymbols.length; i++) {
                //scatter symbol
                if (hitSymbols[i] == Model1010802Test.SCATTER_SYMBOL) {
                    int hitCount = hitSymbolCount[i];
                    if (isSlot) {
                        resultInfo.getBaseScatterHit()[reelsType - 1][hitCount - 3]++;
                        scatterCount = hitCount;
                    } else {
                        resultInfo.getFsScatterHit()[reelsType - 1][hitCount - 2]++;
                        scatterCount = hitCount;
                    }
                    break;
                }
            }
        }
        return scatterCount;
    }

    /**
     * compute multiplier Index
     *
     * @param multipliers
     * @param mul
     * @return
     */
    private int computeMulIndex(int[] multipliers, int mul) {
        for (int i = 0; i < multipliers.length; i++) {
            if (mul == multipliers[i]) {
                return i;
            }
        }
        return 0;
    }

    protected void initFsResultInfo(SlotConfigInfo configInfo) {
        String fileName = configInfo.getOutputPath() + FS_FILE;
        configInfo.setFsBonusFileName(fileName);
        FileWriteUtil.createNewFile(fileName);
        StringBuilder strbHeader = new StringBuilder();
        strbHeader.append(StringUtil.getFsHeaderInfo());
        strbHeader.append("fsTimes").append(BaseConstant.TAB_STR);
        strbHeader.append(StringUtil.getFreespinSymbolHeaderInfo());
        FileWriteUtil.writeFileHeadInfo(fileName, strbHeader.toString());
    }

    protected void initSymbolResult(SlotConfigInfo configInfo) {
        String fileName = configInfo.getOutputPath() + SYMBOL_FILE;
        configInfo.setHighMissFileName(fileName);
        FileWriteUtil.createNewFile(fileName);
        StringBuilder strbHeader = new StringBuilder();
        for (int i = 0; i < BaseConstant.SYMBOL_COUNT; i++) {
            strbHeader.append("Base Symbol").append(i + 1).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < BaseConstant.SYMBOL_COUNT; i++) {
            strbHeader.append("Chain Reaction Or Fs Symbol").append(i + 1).append(BaseConstant.TAB_STR);
        }
        FileWriteUtil.writeFileHeadInfo(fileName, strbHeader.toString());
    }

    protected void initFsSymbolInfo(BaseSlotModel model, BaseResultInfo resultInfo) {
        int reelsCount = BaseConstant.REELS_COUNT;
        if (model instanceof IBaseReelsDefaultConfig) {
            reelsCount = ((IBaseReelsDefaultConfig) model).getReelsCount();
            //8050802_FruitParty
            if (reelsCount < BaseConstant.REELS_COUNT) {
                reelsCount = BaseConstant.REELS_COUNT;
            }
        }
        List<SymbolResultInfo> fsSymbolInfoList = new ArrayList<SymbolResultInfo>();
        for (int i = 0; i < BaseConstant.SYMBOL_COUNT; i++) {
            SymbolResultInfo symbolInfo = new SymbolResultInfo(i + 1);
            long[] hitCount = new long[reelsCount];
            long[] hitAmount = new long[reelsCount];
            symbolInfo.setHitPayCount(hitCount);
            symbolInfo.setHitPayAmount(hitAmount);
            fsSymbolInfoList.add(symbolInfo);
        }
        resultInfo.setFsSymbolInfoList(fsSymbolInfoList);
    }

    protected void setBaseCommInfo(long spinCount, long initCredit, double totalWon, SlotGameLogicBean gameLogicBean, BaseResultInfo resultInfo) {
        resultInfo.setSpinCount(spinCount);
        resultInfo.setBetPerLine((int) gameLogicBean.getBet());
        resultInfo.setLine((int) gameLogicBean.getLines());
        resultInfo.setTotalCoinIn(resultInfo.getTotalCoinIn() + gameLogicBean.getSumBetCredit());
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
        resultInfo.setLeftCredit(initCredit);
    }

    protected int[] getScatterSymbol() {
        return new int[]{12};
    }

    private void outResultInfo(SlotConfigInfo configInfo, EsqueletoExplosivoResultInfo resultInfo) {

        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append(StringUtil.getCommonHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getBaseReelsHit().length; i++) {
                strbHeader.append("BG Reelset").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseReelsWin().length; i++) {
                strbHeader.append("BG Reelset").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            strbHeader.append("BG Wild Hit").append(BaseConstant.TAB_STR);
            for (int i = 0; i < resultInfo.getBaseReelsMulHit().length; i++) {
                for (int j = 0; j < resultInfo.getBaseReelsMulHit()[i].length; j++) {
                    strbHeader.append("BG Reelset").append(i + 1).append(" MulIndex").append(j + 1).append(" Hit").append(BaseConstant.TAB_STR);
                }
            }
            for (int i = 0; i < resultInfo.getBaseReelsMulWin().length; i++) {
                for (int j = 0; j < resultInfo.getBaseReelsMulWin()[i].length; j++) {
                    strbHeader.append("BG Reelset").append(i + 1).append(" MulIndex").append(j + 1).append(" Win").append(BaseConstant.TAB_STR);
                }
            }
            strbHeader.append("isBaseChainReaction").append(BaseConstant.TAB_STR);
            for (int i = 0; i < resultInfo.getBaseScatterHit().length; i++) {
                for (int j = 0; j < resultInfo.getBaseScatterHit()[i].length; j++) {
                    strbHeader.append("BG Reelset").append(i + 1).append(" Scatter").append(j + 3).append(" Hit").append(BaseConstant.TAB_STR);
                }
            }
            for (int i = 0; i < resultInfo.getBaseScatterWin().length; i++) {
                for (int j = 0; j < resultInfo.getBaseScatterWin()[i].length; j++) {
                    strbHeader.append("BG Reelset").append(i + 1).append(" Scatter").append(j + 3).append(" Win").append(BaseConstant.TAB_STR);
                }
            }
            for (int i = 0; i < resultInfo.getBaseRespinHit().length; i++) {
                strbHeader.append("BG Reelset").append(i + 1).append("Respin Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseRespinTime().length; i++) {
                strbHeader.append("BG Reelset").append(i + 1).append("Respin Time").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseRespinWin().length; i++) {
                strbHeader.append("BG Reelset").append(i + 1).append("Respin Win").append(BaseConstant.TAB_STR);
            }
            //Fs
            for (int i = 0; i < resultInfo.getFsReelsHit().length; i++) {
                strbHeader.append("Fs Reelset").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsReelsTimes().length; i++) {
                strbHeader.append("Fs Reelset").append(i + 1).append(" Time").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsReelsWin().length; i++) {
                strbHeader.append("Fs Reelset").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsScatterHit().length; i++) {
                for (int j = 0; j < resultInfo.getFsScatterHit()[i].length; j++) {
                    strbHeader.append("Fs Reelset").append(i + 1).append("Scatter").append(j + 2).append(" Hit").append(BaseConstant.TAB_STR);
                }
            }
            for (int i = 0; i < resultInfo.getFsReelsMulHit().length; i++) {
                for (int j = 0; j < resultInfo.getFsReelsMulHit()[i].length; j++) {
                    strbHeader.append("Fs Reelset").append(i + 1).append(" MulIndex").append(j + 1).append(" Hit").append(BaseConstant.TAB_STR);
                }
            }
            for (int i = 0; i < resultInfo.getFsReelsMulWin().length; i++) {
                for (int j = 0; j < resultInfo.getFsReelsMulWin()[i].length; j++) {
                    strbHeader.append("Fs Reelset").append(i + 1).append(" MulIndex").append(j + 1).append(" Win").append(BaseConstant.TAB_STR);
                }
            }
            for (int i = 0; i < resultInfo.getFsRespinHit().length; i++) {
                strbHeader.append("Fs Reelset").append(i + 1).append("Respin Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsRespinTime().length; i++) {
                strbHeader.append("Fs Reelset").append(i + 1).append("Respin Time").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsRespinWin().length; i++) {
                strbHeader.append("Fs Reelset").append(i + 1).append("Respin Win").append(BaseConstant.TAB_STR);
            }
            strbHeader.append("Fs Wild Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("isFsChainReaction").append(BaseConstant.TAB_STR);
            strbHeader.append("FsTriggerCollectWildHit").append(BaseConstant.TAB_STR);
            strbHeader.append("isFsTriCollectWild").append(BaseConstant.TAB_STR);
            strbHeader.append("Max Win Hit").append(BaseConstant.TAB_STR);
            //Base NOT Respin win
            for (int i = 0; i < resultInfo.getBaseNotRespinWin().length; i++) {
                strbHeader.append("BG Reelset").append(i + 1).append("Not Respin Win").append(BaseConstant.TAB_STR);
            }
            strbHeader.append(StringUtil.getPayTableHeaderInfo(resultInfo));
            FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), strbHeader.toString());
        }
        StringBuilder strContent = new StringBuilder();
        strContent.append(StringUtil.getBaseResultInfo(resultInfo));
        for (long hit : resultInfo.getBaseReelsHit()) {
            strContent.append(hit).append(BaseConstant.TAB_STR);
        }
        for (long win : resultInfo.getBaseReelsWin()) {
            strContent.append(win).append(BaseConstant.TAB_STR);
        }
        strContent.append(resultInfo.getBaseWildHit()).append(BaseConstant.TAB_STR);
        for (long[] reelsMulHit : resultInfo.getBaseReelsMulHit()) {
            for (long hit : reelsMulHit) {
                strContent.append(hit).append(BaseConstant.TAB_STR);
            }
        }
        for (long[] reelsMulWin : resultInfo.getBaseReelsMulWin()) {
            for (long win : reelsMulWin) {
                strContent.append(win).append(BaseConstant.TAB_STR);
            }
        }
        strContent.append(resultInfo.isBaseChainReaction()).append(BaseConstant.TAB_STR);
        for (long[] scatterHit : resultInfo.getBaseScatterHit()) {
            for (long hit : scatterHit) {
                strContent.append(hit).append(BaseConstant.TAB_STR);
            }
        }
        for (long[] scatterWin : resultInfo.getBaseScatterWin()) {
            for (long win : scatterWin) {
                strContent.append(win).append(BaseConstant.TAB_STR);
            }
        }
        for (long hit : resultInfo.getBaseRespinHit()) {
            strContent.append(hit).append(BaseConstant.TAB_STR);
        }
        for (long time : resultInfo.getBaseRespinTime()) {
            strContent.append(time).append(BaseConstant.TAB_STR);
        }
        for (long win : resultInfo.getBaseRespinWin()) {
            strContent.append(win).append(BaseConstant.TAB_STR);
        }
        for (long hit : resultInfo.getFsReelsHit()) {
            strContent.append(hit).append(BaseConstant.TAB_STR);
        }
        for (long time : resultInfo.getFsReelsTimes()) {
            strContent.append(time).append(BaseConstant.TAB_STR);
        }
        for (long win : resultInfo.getFsReelsWin()) {
            strContent.append(win).append(BaseConstant.TAB_STR);
        }
        for (long[] scatterHit : resultInfo.getFsScatterHit()) {
            for (long hit : scatterHit) {
                strContent.append(hit).append(BaseConstant.TAB_STR);
            }
        }
        for (long[] reelsMulHit : resultInfo.getFsReelsMulHit()) {
            for (long hit : reelsMulHit) {
                strContent.append(hit).append(BaseConstant.TAB_STR);
            }
        }
        for (long[] reelsMulWin : resultInfo.getFsReelsMulWin()) {
            for (long win : reelsMulWin) {
                strContent.append(win).append(BaseConstant.TAB_STR);
            }
        }
        for (long hit : resultInfo.getFsRespinHit()) {
            strContent.append(hit).append(BaseConstant.TAB_STR);
        }
        for (long time : resultInfo.getFsRespinTime()) {
            strContent.append(time).append(BaseConstant.TAB_STR);
        }
        for (long win : resultInfo.getFsRespinWin()) {
            strContent.append(win).append(BaseConstant.TAB_STR);
        }
        strContent.append(resultInfo.getFsWildHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.isFsChainReaction()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFsTriCollectWildHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.isFsTriCollectWild()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getUpMaxWinHit()).append(BaseConstant.TAB_STR);
        //Base Not Respin Win
        for (long win : resultInfo.getBaseNotRespinWin()) {
            strContent.append(win).append(BaseConstant.TAB_STR);
        }
        strContent.append(StringUtil.getPayTableHit(resultInfo));
        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getOutputFileName(), configInfo, 0);
    }

    protected void outFsSymbolResultInfo(SlotConfigInfo configInfo, BaseResultInfo fsResultInfo) {
        StringBuilder strContent = new StringBuilder();
        strContent.append(StringUtil.getFSBaseResultInfo(fsResultInfo));
        strContent.append(fsResultInfo.getFreespinTotalTimes()).append(BaseConstant.TAB_STR);
        strContent.append(StringUtil.getFreespinSymbolInfo(fsResultInfo.getFsSymbolInfoList()));
        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getFsBonusFileName(), configInfo, 3);
    }


}
