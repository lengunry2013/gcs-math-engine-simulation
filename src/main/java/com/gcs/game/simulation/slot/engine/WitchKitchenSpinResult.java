package com.gcs.game.simulation.slot.engine;


import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.math.model20260103.Model20260103;
import com.gcs.game.engine.math.model20260103.Model20260103SpinResult;
import com.gcs.game.engine.math.model20260804.Model20260804;
import com.gcs.game.engine.math.model20260804.Model20260804SpinResult;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.simulation.slot.vo.MakinBaconResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.slot.vo.WitchKitchenResultInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model20260103.Model20260103Test;
import com.gcs.game.testengine.math.model20260804.Model20260804Test;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class WitchKitchenSpinResult extends LittleDragonBunsSpinResult {

    public static final String FS_FILE = "fsResult1.txt";
    public static final long[] BET_LEVEL = new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    private static BufferedWriter[] fsWriter = new BufferedWriter[3];

    public WitchKitchenSpinResult() {

    }

    public void cycleSpinForWitchKitchen(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel baseSlotModel) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            double playerCredit = initCredit;
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            SlotGameLogicBean gameLogicBean = (SlotGameLogicBean) baseGameLogicBean;
            Model20260804Test model = (Model20260804Test) baseSlotModel;

            WitchKitchenResultInfo resultInfo = new WitchKitchenResultInfo();
            WitchKitchenResultInfo[] fsResultInfo = new WitchKitchenResultInfo[3];
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
                Model20260804SpinResult spinResult = (Model20260804SpinResult) gameLogicBean.getSlotSpinResult();
                int wildCount = 0;
                for (int symbol : spinResult.getSlotDisplaySymbols()) {
                    if (symbol == Model20260804Test.WILD_SYMBOL) {
                        wildCount++;
                    }
                }
                int[] wildReels = spinResult.getSlotWildReels();
                if (wildCount > 0) {
                    if (wildReels != null) {
                        resultInfo.getBaseWildHit()[2]++;
                        resultInfo.getBaseWildWin()[2] += winCredit;
                    } else {
                        resultInfo.getBaseWildHit()[1]++;
                        resultInfo.getBaseWildWin()[1] += winCredit;
                    }
                } else {
                    //normal spin result
                    resultInfo.getBaseWildHit()[0]++;
                    resultInfo.getBaseWildWin()[0] += winCredit;
                }
                computeScTrigger(spinResult, model, resultInfo);

                if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                    if (winCredit > 0) {
                        resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                        resultInfo.setBaseGameTotalWin(resultInfo.getBaseGameTotalWin() + winCredit);
                    }
                } else {
                    if (winCredit > 0) {
                        resultInfo.setBaseGameHit(resultInfo.getBaseGameHit() + 1);
                        resultInfo.setBaseGameTotalWin(resultInfo.getBaseGameTotalWin() + winCredit);
                    }
                    int fsType = spinResult.getFsType();
                    long fsCoinOut = 0L;
                    long fsTotalTimes = 0L;
                    //start freespin or bonus
                    while (true) {
                        if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                            while (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                                PlayerInputInfo playerInput = new PlayerInputInfo();
                                playerInput.setRequestGameStatus(200);
                                gameLogicBean = (SlotGameLogicBean) engine.gameProgress(gameLogicBean, gameLogicMap, playerInput, null, null, null);

                                Model20260804SpinResult fsSpinResult = (Model20260804SpinResult) gameLogicBean.getSlotFsSpinResults().get(gameLogicBean.getSlotFsSpinResults().size() - 1);
                                long freespinWon = fsSpinResult.getSlotPay();

                                totalWon += freespinWon;
                                fsCoinOut += freespinWon;
                                fsTotalTimes++;
                                int scSymbol = Model20260804Test.SC1_SYMBOL;
                                switch (fsType) {
                                    case 1:
                                    case 3:
                                        computeScTypeResult(fsSpinResult, scSymbol, fsType, resultInfo, totalBet);
                                        break;
                                    case 2:
                                        scSymbol = Model20260103Test.SC2_SYMBOL;
                                        computeScTypeResult(fsSpinResult, scSymbol, fsType, resultInfo, totalBet);
                                        break;
                                    default:
                                        break;
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
                }
            }

        } catch (InvalidGameStateException e) {
            log.error("engine gameStart", e);
            e.printStackTrace();
        } catch (Exception e) {
            log.error("cycleSpinForMakinBacon run exception", e);
        }

    }

    private void computeFsResult(WitchKitchenResultInfo[] fsResultInfo, long spinCount, SlotGameLogicBean gameLogicBean) {
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

    private void initFsResultInfo(WitchKitchenResultInfo[] fsResultInfo, Model20260804Test model, SlotConfigInfo slotConfigInfo) {
        if (fsResultInfo != null) {
            for (int i = 0; i < fsResultInfo.length; i++) {
                fsResultInfo[i] = new WitchKitchenResultInfo();
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

    private void computeScTypeResult(Model20260804SpinResult fsSpinResult, int scSymbol, int fsType, WitchKitchenResultInfo resultInfo, long totalBet) {
        if (fsSpinResult != null) {
            int[] hitSymbol = fsSpinResult.getHitSlotSymbols();
            long[] hitPay = fsSpinResult.getHitSlotPays();
            long scSymbolWin = 0;
            boolean isScatterWin = false;
            if (hitSymbol != null) {
                for (int i = 0; i < hitSymbol.length; i++) {
                    if (hitSymbol[i] == scSymbol && hitPay[i] > 0) {
                        int xPrize = (int) (hitPay[i] / totalBet);
                        resultInfo.getFsScTypeHit()[fsType - 1][xPrize]++;
                        resultInfo.getFsScTypeWin()[fsType - 1][xPrize] += hitPay[i];
                        scSymbolWin += hitPay[i];
                        isScatterWin = true;
                    } else if (hitSymbol[i] > 1000) {
                        //+1 FREE,+2 FREE
                        int fsTime = hitSymbol[i] % 1000;
                        resultInfo.getFsScTypeHit()[fsType - 1][fsTime + 10]++;
                        isScatterWin = true;
                    } else if (hitSymbol[i] >= 100) {
                        //trigger JPBonus feature
                        resultInfo.getFsScTypeHit()[fsType - 1][13]++;
                        resultInfo.getFsScTypeWin()[fsType - 1][13] += hitPay[i];
                        int jpBonusIndex = hitSymbol[i] % 100;
                        resultInfo.getJpBonusLettersHit()[jpBonusIndex]++;
                        int jpHitLevel = getJpHitLevel(fsSpinResult, jpBonusIndex);
                        if (hitPay[i] > 0 && jpHitLevel > 0) {
                            resultInfo.getJpBonusHit()[jpHitLevel - 1]++;
                            resultInfo.getJpBonusWin()[jpHitLevel - 1] += hitPay[i];
                        }
                        scSymbolWin += hitPay[i];
                        isScatterWin = true;
                    }
                }
                if (!isScatterWin) {
                    resultInfo.getFsScTypeHit()[fsType - 1][0]++;
                }
                long normalWin = fsSpinResult.getSlotPay() - scSymbolWin;
                resultInfo.getFsScTypeWin()[fsType - 1][0] += normalWin;
            }
        }

    }

    private static int getJpHitLevel(Model20260804SpinResult fsSpinResult, int jpBonusIndex) {
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

    private void computeScTrigger(Model20260804SpinResult spinResult, Model20260804Test model, WitchKitchenResultInfo resultInfo) {
        if (spinResult != null) {
            int[] displaySymbols = spinResult.getSlotDisplaySymbols();
            int sc1Count = model.computeScPosition(displaySymbols, Model20260804.SC1_SYMBOL);
            int sc2count = model.computeScPosition(displaySymbols, Model20260804.SC2_SYMBOL);
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

    private void outResultInfo(SlotConfigInfo configInfo, WitchKitchenResultInfo resultInfo) {

        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append(StringUtil.getCommonHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getBaseWildHit().length; i++) {
                strbHeader.append("Base Wild Index").append(i).append(" Hit").append(BaseConstant.TAB_STR);
            }
            for (int i = 0; i < resultInfo.getBaseWildWin().length; i++) {
                strbHeader.append("Base Wild Index").append(i).append(" Win").append(BaseConstant.TAB_STR);
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
                    strbHeader.append("Fs Type").append(i + 1).append(" SC Index").append(j).append(" Hit").append(BaseConstant.TAB_STR);
                }
            }
            for (int i = 0; i < resultInfo.getFsScTypeWin().length; i++) {
                for (int j = 0; j < resultInfo.getFsScTypeWin()[i].length; j++) {
                    strbHeader.append("Fs Type").append(i + 1).append(" SC Index").append(j).append(" Win").append(BaseConstant.TAB_STR);
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
        for (long wildHit : resultInfo.getBaseWildHit()) {
            strContent.append(wildHit).append(BaseConstant.TAB_STR);
        }
        for (long wildWin : resultInfo.getBaseWildWin()) {
            strContent.append(wildWin).append(BaseConstant.TAB_STR);
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
                                       WitchKitchenResultInfo[] fsResultInfo) {
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
