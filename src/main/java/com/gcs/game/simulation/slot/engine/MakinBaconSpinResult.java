package com.gcs.game.simulation.slot.engine;


import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.math.model20260103.Model20260103;
import com.gcs.game.engine.math.model20260103.Model20260103SpinResult;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.vo.*;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import com.gcs.game.simulation.slot.vo.MakinBaconResultInfo;
import com.gcs.game.simulation.slot.vo.MissSpookyResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model20260103.Model20260103Test;
import com.gcs.game.testengine.math.model20260201.Model20260201Test;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MakinBaconSpinResult extends LittleDragonBunsSpinResult {

    public static final String FS_FILE = "fsResult1.txt";
    public static final long[] BET_LEVEL = new long[]{2, 4, 6, 8, 10, 12, 14, 16, 18, 20};
    private static BufferedWriter[] fsWriter = new BufferedWriter[3];

    public MakinBaconSpinResult() {

    }

    public void cycleSpinForMakinBacon(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel baseSlotModel) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            double playerCredit = initCredit;
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            SlotGameLogicBean gameLogicBean = (SlotGameLogicBean) baseGameLogicBean;
            Model20260103Test model = (Model20260103Test) baseSlotModel;

            MakinBaconResultInfo resultInfo = new MakinBaconResultInfo();
            MakinBaconResultInfo[] fsResultInfo = new MakinBaconResultInfo[3];
            initFsResultInfo(fsResultInfo, model, slotConfigInfo);
            double totalWon = 0.0;
            GameEngineCompute.initPayTableHit(model.getPayTable(), resultInfo);
            initFsSymbolInfo(model, resultInfo);
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
                spinCount++;
                gameLogicBean = (SlotGameLogicBean) engine.gameStart(gameLogicBean, gameLogicMap, null, null);
                long totalBet = gameLogicBean.getSumBetCredit();
                playerCredit -= totalBet;
                long winCredit = gameLogicBean.getSumWinCredit();
                totalWon += winCredit;
                computeFsResult(fsResultInfo, spinCount, gameLogicBean);
                GameEngineCompute.computePayTableHit(gameLogicBean, gameLogicBean.getSlotSpinResult(), resultInfo, getScatterSymbol());
                if (winCredit > resultInfo.getBaseGameTopAward()) {
                    resultInfo.setBaseGameTopAward(winCredit);
                    resultInfo.setBaseTopAwardReelStop(StringUtil.IntegerArrayToStr(gameLogicBean.getSlotSpinResult().getSlotReelStopPosition(), " "));
                    resultInfo.setBaseTopAwardType("Base Normal");
                }
                Model20260103SpinResult spinResult = (Model20260103SpinResult) gameLogicBean.getSlotSpinResult();
                int baseReelsType = spinResult.getReelsType();
                resultInfo.getBaseReelsTypeHit()[baseReelsType - 1]++;
                resultInfo.getBaseReelsTypeWin()[baseReelsType - 1] += winCredit;
                int mysterySymbol = spinResult.getMysterySymbol();
                resultInfo.getBaseMySymbol()[mysterySymbol - 2]++;
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

                                Model20260103SpinResult fsSpinResult = (Model20260103SpinResult) gameLogicBean.getSlotFsSpinResults().get(gameLogicBean.getSlotFsSpinResults().size() - 1);
                                long freespinWon = fsSpinResult.getSlotPay();

                                totalWon += freespinWon;
                                fsCoinOut += freespinWon;
                                fsTotalTimes++;
                                int fsReelsType = fsSpinResult.getReelsType();
                                resultInfo.getFsReelsTypeHit()[fsType - 1][fsReelsType - 1]++;
                                resultInfo.getFsReelsTypeWin()[fsType - 1][fsReelsType - 1] += freespinWon;
                                int scSymbol = Model20260103Test.SC1_SYMBOL;
                                switch (fsType) {
                                    case 1:
                                        computeScTypeResult(fsSpinResult, scSymbol, fsType, resultInfo, totalBet);
                                        break;
                                    case 2:
                                        scSymbol = Model20260103Test.SC2_SYMBOL;
                                        computeScTypeResult(fsSpinResult, scSymbol, fsType, resultInfo, totalBet);
                                        break;
                                    case 3:
                                        scSymbol = Model20260103Test.SC3_SYMBOL;
                                        computeScTypeResult(fsSpinResult, scSymbol, fsType, resultInfo, totalBet);
                                        break;
                                    default:
                                        break;
                                }
                                List<Integer> scPositions = model.computeFsScPosition(fsSpinResult.getSlotDisplaySymbols(), scSymbol);
                                if (!scPositions.isEmpty()) {
                                    resultInfo.getFsScAddHit()[fsType - 1][scPositions.size()]++;
                                } else {
                                    resultInfo.getFsScAddHit()[fsType - 1][0]++;
                                }
                                resultInfo.getFsTimes()[fsType - 1]++;
                                resultInfo.getFsWin()[fsType - 1] += freespinWon;
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
                                resultInfo.setFreespinTotalTimes(resultInfo.getFreespinTotalTimes() + fsTotalTimes);
                                resultInfo.setFreespinTotalHit(resultInfo.getFreespinTotalHit() + 1);
                                resultInfo.setFreespinTotalWin(resultInfo.getFreespinTotalWin() + fsCoinOut);
                            }
                        } else if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                            break;
                        }

                    }

                }

                playerCredit += totalWon;
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
                resultInfo.setLeftCredit(playerCredit);
                if (spinCount > 0 && spinCount % playTime == 0) {
                    outResultInfo(slotConfigInfo, resultInfo);
                    outFsSymbolResultInfo(slotConfigInfo, fsResultInfo);
                    //outFsSymbolResultInfo(slotConfigInfo, resultInfo);
                }
            }

        } catch (InvalidGameStateException e) {
            log.error("engine gameStart", e);
            e.printStackTrace();
        } catch (Exception e) {
            log.error("cycleSpinForMakinBacon run exception", e);
        }

    }

    private void computeFsResult(MakinBaconResultInfo[] fsResultInfo, long spinCount, SlotGameLogicBean gameLogicBean) {
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

    private void initFsResultInfo(MakinBaconResultInfo[] fsResultInfo, Model20260103Test model, SlotConfigInfo slotConfigInfo) {
        if (fsResultInfo != null) {
            for (int i = 0; i < fsResultInfo.length; i++) {
                fsResultInfo[i] = new MakinBaconResultInfo();
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

    private void computeScTypeResult(Model20260103SpinResult fsSpinResult, int scSymbol, int fsType, MakinBaconResultInfo resultInfo, long totalBet) {
        if (fsSpinResult != null) {
            int[] hitSymbol = fsSpinResult.getHitSlotSymbols();
            long[] hitPay = fsSpinResult.getHitSlotPays();
            if (hitSymbol != null) {
                for (int i = 0; i < hitSymbol.length; i++) {
                    if (hitSymbol[i] == scSymbol && hitPay[i] > 0) {
                        int xPrize = (int) (hitPay[i] / totalBet);
                        resultInfo.getFsScTypeHit()[fsType - 1][xPrize - 1]++;
                        resultInfo.getFsScTypeWin()[fsType - 1][xPrize - 1] += hitPay[i];
                    } else if (hitSymbol[i] > 1000) {
                        //+1 FREE,+2 FREE
                        int fsTime = hitSymbol[i] % 1000;
                        resultInfo.getFsScTypeHit()[fsType - 1][fsTime + 9]++;
                    } else if (hitSymbol[i] >= 100) {
                        //trigger JPBonus feature
                        resultInfo.getFsScTypeHit()[fsType - 1][12]++;
                        resultInfo.getFsScTypeWin()[fsType - 1][12] += hitPay[i];
                        int jpBonusIndex = hitSymbol[i] % 100;
                        resultInfo.getJpBonusLettersHit()[jpBonusIndex]++;
                        int jpHitLevel = getJpHitLevel(fsSpinResult, jpBonusIndex);
                        if (hitPay[i] > 0 && jpHitLevel > 0) {
                            resultInfo.getJpBonusHit()[jpHitLevel - 1]++;
                            resultInfo.getJpBonusWin()[jpHitLevel - 1] += hitPay[i];
                        }
                    }
                }
            }
        }

    }

    private static int getJpHitLevel(Model20260103SpinResult fsSpinResult, int jpBonusIndex) {
        int hitLevelIndex = -1;
        if (jpBonusIndex < 4) {
            hitLevelIndex = 0;
        } else if (jpBonusIndex < 9) {
            hitLevelIndex = 1;
        } else if (jpBonusIndex < 14) {
            hitLevelIndex = 2;
        } else if (jpBonusIndex < 19) {
            hitLevelIndex = 3;
        }
        //可能中奖两个levels
        return fsSpinResult.getHitLevels()[hitLevelIndex];
    }

    private void computeScTrigger(Model20260103SpinResult spinResult, Model20260103Test model, MakinBaconResultInfo resultInfo) {
        if (spinResult != null) {
            int[] displaySymbols = spinResult.getSlotDisplaySymbols();
            int sc1Count = model.computeScPosition(displaySymbols, Model20260103.SC1_SYMBOL);
            int sc2count = model.computeScPosition(displaySymbols, Model20260103.SC2_SYMBOL);
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

    private void outResultInfo(SlotConfigInfo configInfo, MakinBaconResultInfo resultInfo) {

        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append(StringUtil.getCommonHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getBaseMySymbol().length; i++) {
                strbHeader.append("Base My Symbol").append(i + 2).append(" Hit").append(BaseConstant.TAB_STR);
            }
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
            for (int i = 0; i < resultInfo.getFsReelsTypeHit().length; i++) {
                for (int j = 0; j < resultInfo.getFsReelsTypeHit()[i].length; j++) {
                    strbHeader.append("Fs Type").append(i + 1).append(" Reelstrip").append(j + 3).append(" Hit").append(BaseConstant.TAB_STR);
                }
            }
            for (int i = 0; i < resultInfo.getFsReelsTypeWin().length; i++) {
                for (int j = 0; j < resultInfo.getFsReelsTypeWin()[i].length; j++) {
                    strbHeader.append("Fs Type").append(i + 1).append(" Reelstrip").append(j + 3).append(" Win").append(BaseConstant.TAB_STR);
                }
            }
            for (int i = 0; i < resultInfo.getFsScAddHit().length; i++) {
                for (int j = 0; j < resultInfo.getFsScAddHit()[i].length; j++) {
                    strbHeader.append("Fs Type").append(i + 1).append(" SC Add").append(j).append(" Hit").append(BaseConstant.TAB_STR);
                }
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
            for (int i = 0; i < resultInfo.getJpBonusHit().length; i++) {
                strbHeader.append("Jp Bonus Level").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getJpBonusWin().length; i++) {
                strbHeader.append("Jp Bonus Level").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getJpBonusLettersHit().length; i++) {
                strbHeader.append("Jp Bonus Letters").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
            }

            strbHeader.append(StringUtil.getPayTableHeaderInfo(resultInfo));
            FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), strbHeader.toString());
        }
        StringBuilder strContent = new StringBuilder();
        strContent.append(StringUtil.getBaseResultInfo(resultInfo));
        for (long mySymbolHit : resultInfo.getBaseMySymbol()) {
            strContent.append(mySymbolHit).append(BaseConstant.TAB_STR);
        }
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
        for (long[] fsReelsTypes : resultInfo.getFsReelsTypeHit()) {
            for (long reelsTypeHit : fsReelsTypes) {
                strContent.append(reelsTypeHit).append(BaseConstant.TAB_STR);
            }
        }
        for (long[] fsReelsTypes : resultInfo.getFsReelsTypeWin()) {
            for (long reelsTypeWin : fsReelsTypes) {
                strContent.append(reelsTypeWin).append(BaseConstant.TAB_STR);
            }
        }
        for (long[] fsScAdd : resultInfo.getFsScAddHit()) {
            for (long scAddHit : fsScAdd) {
                strContent.append(scAddHit).append(BaseConstant.TAB_STR);
            }
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
        for (long bonusHit : resultInfo.getJpBonusHit()) {
            strContent.append(bonusHit).append(BaseConstant.TAB_STR);
        }
        for (long bonusWin : resultInfo.getJpBonusWin()) {
            strContent.append(bonusWin).append(BaseConstant.TAB_STR);
        }
        for (long lettersHit : resultInfo.getJpBonusLettersHit()) {
            strContent.append(lettersHit).append(BaseConstant.TAB_STR);
        }
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
                                       MakinBaconResultInfo[] fsResultInfo) {
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
