package com.gcs.game.simulation.slot.engine;


import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.math.model20260507.Model20260507SpinResult;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import com.gcs.game.simulation.slot.common.vo.SymbolResultInfo;
import com.gcs.game.simulation.slot.vo.CheddarQuestLinkResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model20260507.Model20260507Test;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.InputInfo;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CheddarQuestLinkSpinResult {

    public static final String FS_FILE = "fsResult1.txt";

    public CheddarQuestLinkSpinResult() {

    }

    public void cycleSpinForCheddarQuestLink(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel baseSlotModel) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            SlotGameLogicBean gameLogicBean = (SlotGameLogicBean) baseGameLogicBean;
            Model20260507Test model = (Model20260507Test) baseSlotModel;
            CheddarQuestLinkResultInfo resultInfo = new CheddarQuestLinkResultInfo();
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

                /*InputInfo inputInfo = new InputInfo();
                List<int[]> inputPositions = new ArrayList<>();
                inputPositions.add(new int[]{17, 40, 39, 14, 23});
                inputInfo.setInputPosition(inputPositions);
                gameLogicBean = (SlotGameLogicBean) engine.gameStart(gameLogicBean, gameLogicMap, inputInfo, null);*/
                gameLogicBean = (SlotGameLogicBean) engine.gameStart(gameLogicBean, gameLogicMap, null, null);
                long totalBet = gameLogicBean.getSumBetCredit();
                initCredit -= totalBet;

                long winCredit = gameLogicBean.getSumWinCredit();
                totalWon += winCredit;

                GameEngineCompute.computePayTableHit(gameLogicBean, gameLogicBean.getSlotSpinResult(), resultInfo, getScatterSymbol());
                if (winCredit > resultInfo.getBaseGameTopAward()) {
                    resultInfo.setBaseGameTopAward(winCredit);
                    resultInfo.setBaseTopAwardReelStop(StringUtil.IntegerArrayToStr(gameLogicBean.getSlotSpinResult().getSlotReelStopPosition(), " "));
                    resultInfo.setBaseTopAwardType("Base Normal");
                }
                computeBaseSw(gameLogicBean, resultInfo);
                Model20260507SpinResult spinResult = (Model20260507SpinResult) gameLogicBean.getSlotSpinResult();
                long linkBonusWin = computeLinkBonusWin(spinResult, resultInfo, gameLogicBean);
                if (linkBonusWin > 0) {
                    resultInfo.getTriggerLinkBonusHit()[0]++;
                    resultInfo.getTriggerLinkBonusWin()[0] += linkBonusWin;
                }
                //baseGame win
                long baseWin = winCredit - linkBonusWin;
                int reelsTypes = spinResult.getReelsType();
                resultInfo.getBaseReelsTypeHit()[reelsTypes - 1]++;
                resultInfo.getBaseReelsTypeWin()[reelsTypes - 1] += baseWin;

                if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                    if (baseWin > 0) {
                        resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                        resultInfo.setBaseGameTotalWin(resultInfo.getBaseGameTotalWin() + baseWin);
                    }
                } else {
                    if (baseWin > 0) {
                        resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                        resultInfo.setBaseGameTotalWin(resultInfo.getBaseGameTotalWin() + baseWin);
                    }
                    int scCount = getScatterCount(spinResult);
                    if (scCount >= 3) {
                        resultInfo.getScTriggerFsHit()[scCount - 3]++;
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

                                Model20260507SpinResult fsSpinResult = (Model20260507SpinResult) gameLogicBean.getSlotFsSpinResults().get(gameLogicBean.getSlotFsSpinResults().size() - 1);
                                long freespinWon = fsSpinResult.getSlotPay();
                                int fsReelsType = fsSpinResult.getReelsType();
                                totalWon += freespinWon;
                                long fsLinkBonusWin = computeLinkBonusWin(fsSpinResult, resultInfo, gameLogicBean);
                                if (fsLinkBonusWin > 0) {
                                    freespinWon -= fsLinkBonusWin;
                                    resultInfo.getTriggerLinkBonusHit()[1]++;
                                    resultInfo.getTriggerLinkBonusWin()[1] += fsLinkBonusWin;
                                }
                                resultInfo.getFsReelsTypeHit()[fsReelsType - 1]++;
                                resultInfo.getFsReelsTypeWin()[fsReelsType - 1] += freespinWon;
                                fsCoinOut += freespinWon;
                                fsTotalTimes++;

                                GameEngineCompute.addFreeSpinSymbolDetailInfo(fsSpinResult, resultInfo);
                                if (freespinWon > resultInfo.getFreespinTopAward()) {
                                    resultInfo.setFreespinTopAward(freespinWon);
                                    resultInfo.setFsTopAwardReelStop(StringUtil.IntegerArrayToStr(fsSpinResult.getSlotReelStopPosition(), " "));
                                    resultInfo.setFsTopAwardType("FS");
                                }

                            }
                            //end freespin
                            if (fsTotalTimes > 0) {
                                resultInfo.setFreespinTotalTimes(resultInfo.getFreespinTotalTimes() + fsTotalTimes);
                                resultInfo.setFreespinTotalHit(resultInfo.getFreespinTotalHit() + 1);
                                resultInfo.setFreespinTotalWin(resultInfo.getFreespinTotalWin() + fsCoinOut);
                                resultInfo.getScTriggerFsWin()[scCount - 3] += fsCoinOut;
                                resultInfo.getScTriggerFsTimes()[scCount - 3] += fsTotalTimes;
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
            System.out.println(e);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e);
            log.error("cycleSpinForCheddarQuestLink run exception", e);
        }

    }

    private long computeLinkBonusWin(Model20260507SpinResult spinResult, CheddarQuestLinkResultInfo resultInfo, SlotGameLogicBean gameLogicBean) {
        long linkBonusWin = 0;
        if (spinResult != null) {
            int[] hitSymbols = spinResult.getHitSlotSymbols();
            long[] hitPays = spinResult.getHitSlotPays();
            if (hitSymbols != null) {
                for (int i = 0; i < hitSymbols.length; i++) {
                    if (hitSymbols[i] == Model20260507Test.LINK_BONUS_SYMBOL) {
                        linkBonusWin = hitPays[i];
                        break;
                    }
                }
            }
            if (linkBonusWin > 0) {
                int triggerSwCount = spinResult.getTriggerSwCount();
                if (triggerSwCount < 5) {
                    System.out.println("triggerSwCount====" + triggerSwCount);
                }
                resultInfo.getTriggerSwHit()[triggerSwCount - 5]++;
                resultInfo.getTriggerSwWin()[triggerSwCount - 5] += linkBonusWin;
                int endLevel = spinResult.getEndActiveLevel();
                if (endLevel < 0) {
                    //LEVEL_no_unlock
                    resultInfo.getLevelHit()[0][triggerSwCount - 5]++;
                    resultInfo.getLevelWin()[0][triggerSwCount - 5] += linkBonusWin;
                } else {
                    //level0,level1,level2
                    resultInfo.getLevelHit()[endLevel + 1][triggerSwCount - 5]++;
                    resultInfo.getLevelWin()[endLevel + 1][triggerSwCount - 5] += linkBonusWin;
                }
                if (spinResult.getGrantWin() > 0) {
                    resultInfo.setGrandHit(resultInfo.getGrandHit() + 1);
                    resultInfo.setGrandWin(resultInfo.getGrandWin() + spinResult.getGrantWin());
                }
                List<Long> swSymbolsWin = spinResult.getSwSymbolsWin();
                if (!swSymbolsWin.isEmpty()) {
                    for (long swWin : swSymbolsWin) {
                        long winPay = swWin / gameLogicBean.getSumBetCredit();
                        int winIndex = getSwWinPayIndex((int) winPay);
                        resultInfo.getBonusSwHit()[winIndex]++;
                        resultInfo.getBonusSwWin()[winIndex] += swWin;
                    }
                    int len = swSymbolsWin.size();
                    resultInfo.getEndLinkBonusHit()[len - 5]++;
                    resultInfo.getEndLinkBonusWin()[len - 5] += linkBonusWin;
                }
            }
        }

        return linkBonusWin;

    }

    private int getScatterCount(Model20260507SpinResult spinResult) {
        if (spinResult != null) {
            int[] hitSymbols = spinResult.getHitSlotSymbols();
            int[] hitCounts = spinResult.getHitSlotSymbolCount();
            if (hitSymbols != null) {
                for (int i = 0; i < hitSymbols.length; i++) {
                    if (hitSymbols[i] == Model20260507Test.SCATTER_SYMBOL) {
                        return hitCounts[i];
                    }
                }
            }
        }
        return 0;
    }

    private void computeBaseSw(SlotGameLogicBean gameLogicBean, CheddarQuestLinkResultInfo resultInfo) {
        Model20260507SpinResult spinResult = (Model20260507SpinResult) gameLogicBean.getSlotSpinResult();
        if (spinResult != null) {
            int[] hitSymbols = spinResult.getHitSlotSymbols();
            long[] hitPays = spinResult.getHitSlotPays();
            if (hitSymbols != null) {
                for (int i = 0; i < hitSymbols.length; i++) {
                    if (hitSymbols[i] == Model20260507Test.SW_SYMBOL) {
                        if (hitPays[i] > 0) {
                            int winPay = (int) (hitPays[i] / gameLogicBean.getSumBetCredit());
                            int winIndex = getSwWinPayIndex(winPay);
                            resultInfo.getBaseSwHit()[winIndex]++;
                            resultInfo.getBaseSwWin()[winIndex] += hitPays[i];
                        }
                    }
                }
            }
        }
    }

    private int getSwWinPayIndex(int winPay) {
        int index = -1;
        for (int i = 0; i < Model20260507Test.SW_AWARD.length; i++) {
            if (Model20260507Test.SW_AWARD[i] == winPay) {
                index = i;
                break;
            }
        }
        return index;
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
        return new int[]{12, 13, 15};
    }

    private void outResultInfo(SlotConfigInfo configInfo, CheddarQuestLinkResultInfo resultInfo) {

        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append(StringUtil.getCommonHeaderInfo(resultInfo));
            strbHeader.append(StringUtil.getBonusHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getBaseSwHit().length; i++) {
                strbHeader.append("SW Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseSwWin().length; i++) {
                strbHeader.append("SW Index").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseReelsTypeHit().length; i++) {
                strbHeader.append("Base Reel Strips").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseReelsTypeWin().length; i++) {
                strbHeader.append("Base Reel Strips").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsReelsTypeHit().length; i++) {
                strbHeader.append("Fs Reel Strips").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getFsReelsTypeWin().length; i++) {
                strbHeader.append("Fs Reel Strips").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getScTriggerFsHit().length; i++) {
                strbHeader.append("Fs Sc").append(i + 3).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getScTriggerFsTimes().length; i++) {
                strbHeader.append("Fs Sc").append(i + 3).append(" Times").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getScTriggerFsWin().length; i++) {
                strbHeader.append("Fs Sc").append(i + 3).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getTriggerSwHit().length; i++) {
                strbHeader.append("LinkBonus Trigger SW").append(i + 5).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getTriggerSwWin().length; i++) {
                strbHeader.append("LinkBonus Trigger SW").append(i + 5).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getLevelHit().length; i++) {
                for (int j = 0; j < resultInfo.getLevelHit()[i].length; j++) {
                    strbHeader.append(getLevelStr(i)).append(" Trigger SW").append(j + 5).append(" Hit").append(BaseConstant.TAB_STR);
                }
            }
            for (int i = 0; i < resultInfo.getLevelWin().length; i++) {
                for (int j = 0; j < resultInfo.getLevelWin()[i].length; j++) {
                    strbHeader.append(getLevelStr(i)).append(" Trigger SW").append(j + 5).append(" Win").append(BaseConstant.TAB_STR);
                }
            }
            strbHeader.append("Grand Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("Grand Win").append(BaseConstant.TAB_STR);
            for (int i = 0; i < resultInfo.getBonusSwHit().length; i++) {
                strbHeader.append("LinkBonus SW Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBonusSwWin().length; i++) {
                strbHeader.append("LinkBonus SW Index").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            strbHeader.append(StringUtil.getPayTableHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getTriggerLinkBonusHit().length; i++) {
                String Str = i == 0 ? "Base " : "Fs";
                strbHeader.append(Str).append(" Link Bonus").append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getTriggerLinkBonusWin().length; i++) {
                String Str = i == 0 ? "Base " : "Fs";
                strbHeader.append(Str).append(" Link Bonus").append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getEndLinkBonusHit().length; i++) {
                strbHeader.append("End LinkBonus SW").append(i + 5).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getEndLinkBonusHit().length; i++) {
                strbHeader.append("End LinkBonus SW").append(i + 5).append(" Win").append(BaseConstant.TAB_STR);
            }
            FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), strbHeader.toString());
        }
        StringBuilder strContent = new StringBuilder();
        strContent.append(StringUtil.getBaseResultInfo(resultInfo));
        strContent.append(StringUtil.getBonusResultInfo(resultInfo));
        for (long swHit : resultInfo.getBaseSwHit()) {
            strContent.append(swHit).append(BaseConstant.TAB_STR);
        }
        for (long swWin : resultInfo.getBaseSwWin()) {
            strContent.append(swWin).append(BaseConstant.TAB_STR);
        }
        for (long reelsTypeHit : resultInfo.getBaseReelsTypeHit()) {
            strContent.append(reelsTypeHit).append(BaseConstant.TAB_STR);
        }
        for (long reelsTypeWin : resultInfo.getBaseReelsTypeWin()) {
            strContent.append(reelsTypeWin).append(BaseConstant.TAB_STR);
        }
        for (long reelsTypeHit : resultInfo.getFsReelsTypeHit()) {
            strContent.append(reelsTypeHit).append(BaseConstant.TAB_STR);
        }
        for (long reelsTypeWin : resultInfo.getFsReelsTypeWin()) {
            strContent.append(reelsTypeWin).append(BaseConstant.TAB_STR);
        }
        for (long scFsHit : resultInfo.getScTriggerFsHit()) {
            strContent.append(scFsHit).append(BaseConstant.TAB_STR);
        }
        for (long scFsTimes : resultInfo.getScTriggerFsTimes()) {
            strContent.append(scFsTimes).append(BaseConstant.TAB_STR);
        }
        for (long scFsWin : resultInfo.getScTriggerFsWin()) {
            strContent.append(scFsWin).append(BaseConstant.TAB_STR);
        }
        for (long swHit : resultInfo.getTriggerSwHit()) {
            strContent.append(swHit).append(BaseConstant.TAB_STR);
        }
        for (long swWin : resultInfo.getTriggerSwWin()) {
            strContent.append(swWin).append(BaseConstant.TAB_STR);
        }
        for (long[] levelHit : resultInfo.getLevelHit()) {
            for (long hit : levelHit) {
                strContent.append(hit).append(BaseConstant.TAB_STR);
            }
        }
        for (long[] levelWin : resultInfo.getLevelWin()) {
            for (long win : levelWin) {
                strContent.append(win).append(BaseConstant.TAB_STR);
            }
        }
        strContent.append(resultInfo.getGrandHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getGrandWin()).append(BaseConstant.TAB_STR);
        for (long swHit : resultInfo.getBonusSwHit()) {
            strContent.append(swHit).append(BaseConstant.TAB_STR);
        }
        for (long swWin : resultInfo.getBonusSwWin()) {
            strContent.append(swWin).append(BaseConstant.TAB_STR);
        }
        strContent.append(StringUtil.getPayTableHit(resultInfo));
        for (long bonusHit : resultInfo.getTriggerLinkBonusHit()) {
            strContent.append(bonusHit).append(BaseConstant.TAB_STR);
        }
        for (long bonusWin : resultInfo.getTriggerLinkBonusWin()) {
            strContent.append(bonusWin).append(BaseConstant.TAB_STR);
        }
        for (long bonusHit : resultInfo.getEndLinkBonusHit()) {
            strContent.append(bonusHit).append(BaseConstant.TAB_STR);
        }
        for (long bonusWin : resultInfo.getEndLinkBonusWin()) {
            strContent.append(bonusWin).append(BaseConstant.TAB_STR);
        }
        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getOutputFileName(), configInfo, 0);
    }

    private String getLevelStr(int index) {
        String str = "";
        switch (index) {
            case 0:
                str = "LEVEL_no";
                break;
            case 1:
                str = "LEVEL_LV0";
                break;
            case 2:
                str = "LEVEL_LV1";
                break;
            case 3:
                str = "LEVEL_LV2";
                break;
            default:
                break;
        }
        return str;
    }

    protected void outFsSymbolResultInfo(SlotConfigInfo configInfo, BaseResultInfo fsResultInfo) {
        StringBuilder strContent = new StringBuilder();
        strContent.append(StringUtil.getFSBaseResultInfo(fsResultInfo));
        strContent.append(fsResultInfo.getFreespinTotalTimes()).append(BaseConstant.TAB_STR);
        strContent.append(StringUtil.getFreespinSymbolInfo(fsResultInfo.getFsSymbolInfoList()));
        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getFsBonusFileName(), configInfo, 3);
    }


}
