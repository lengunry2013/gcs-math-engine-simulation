package com.gcs.game.simulation.slot.engine;

import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.utils.SlotEngineConstant;
import com.gcs.game.engine.slots.vo.*;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.simulation.slot.common.vo.AchievementPay;
import com.gcs.game.simulation.slot.common.vo.AchievementSymbol;
import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import com.gcs.game.simulation.slot.common.vo.TiersInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.GameModelFactoryTest;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.util.*;

/**
 * slot game spin result
 *
 * @author Jiangqx
 * @create 2020-02-17-8:56
 **/
@Slf4j
public class BaseReelsGameSpinResult {

    //write type=1 is AchievementResult1,type=2 is highMissSymbolResult1,type=3 is fsBonusResult1
    private static final String ACHIEVEMENT_SYMBOL_FILE = "AchievementResult1.txt";
    private static final String HIGH_MISS_SYMBOL_FILE = "highMissSymbolResult1.txt";
    private static final String FS_BONUS_FILE = "fsBonusResult1.txt";

    public BaseReelsGameSpinResult() {

    }

    public void cycleSpinForBaseReelsGame(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel model) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            SlotGameLogicBean gameLogicBean = (SlotGameLogicBean) baseGameLogicBean;
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;

            BaseResultInfo resultInfo = new BaseResultInfo();
            initAchievementWriter(slotConfigInfo);
            boolean isAchievement = initAchievementSymbol(model, slotConfigInfo, resultInfo);

            if (!isAchievement) {
                initWinTierWriter(slotConfigInfo);
            }
            TiersInfo tiersInfo = null;
            boolean isMissStackSymbol = false;
            if (!isAchievement) {
                initMaxWinCount(slotConfigInfo, resultInfo, model);
                tiersInfo = new TiersInfo();
                if (slotConfigInfo.getMissSymbol() != null && slotConfigInfo.getMissSymbol().length > 0) {
                    isMissStackSymbol = true;
                }
                initFsBonusStdDev(slotConfigInfo, resultInfo);
            }
            int outputIndex = 0;

            //start simulation
            for (int i = 0; i < simulationCount; i++) {
                spinCount++;
                long totalWon = 0L;
                long baseCoinOut = 0L;
                long fsCoinOut = 0;
                long bonusCoinOut = 0;
                setTiersInfo(tiersInfo, slotConfigInfo);
                Map gameLogicMap = new LinkedHashMap();
                gameLogicMap.put("lines", slotConfigInfo.getLines());
                gameLogicMap.put("bet", slotConfigInfo.getBet());
                gameLogicMap.put("denom", slotConfigInfo.getDenom());

                //star baseGame spin
                gameLogicBean = (SlotGameLogicBean) engine.gameStart(gameLogicBean, gameLogicMap, null, null);
                long totalBet = gameLogicBean.getSumBetCredit();
                initCredit -= totalBet;

                resultInfo.setSpinCount(spinCount);
                resultInfo.setBetPerLine((int) gameLogicBean.getBet());
                resultInfo.setLine((int) gameLogicBean.getLines());
                resultInfo.setTotalCoinIn(resultInfo.getTotalCoinIn() + totalBet);
                long winCredit = gameLogicBean.getSumWinCredit();
                long scatterWin = computeScatterWin(gameLogicBean.getSlotSpinResult(), model);
                baseCoinOut += winCredit - scatterWin;  //TODO Scatter symbol win
                totalWon += winCredit;

                if (!isAchievement && tiersInfo != null) {
                    int highSymbolCount = computeHighSymbolHit(gameLogicBean, slotConfigInfo);
                    tiersInfo.setHighSymbolCount(highSymbolCount);
                    if (isMissStackSymbol) {
                        int stackCount = computeMissSymbolHit(gameLogicBean, model, slotConfigInfo);
                        tiersInfo.setStackCount(stackCount);
                        int stackWildCount = computeWildMissSymbolHit(gameLogicBean, model, slotConfigInfo);
                        tiersInfo.setStackWildCount(stackWildCount);
                        tiersInfo.setHighAndStackSymbolCount(highSymbolCount + stackWildCount);
                    }
                    if (slotConfigInfo.isSumHorizontalSymbol()) {
                        int horizontalSymbolCount = computeHorizontalSymbolHit(gameLogicBean, model, slotConfigInfo);
                        tiersInfo.setHorizontalSymbolCount(horizontalSymbolCount);
                        if (tiersInfo.getHighAndStackSymbolCount() == 0) {
                            tiersInfo.setHighAndStackSymbolCount(highSymbolCount + horizontalSymbolCount);
                        } else {
                            tiersInfo.setHighAndStackSymbolCount(tiersInfo.getHighAndStackSymbolCount() + horizontalSymbolCount);
                        }
                    }
                }
                //compute BaseGame Achievement
                if (isAchievement) {
                    GameEngineCompute.computeBaseAchievement(gameLogicBean, model,
                            resultInfo, slotConfigInfo);
                }

                if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
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
                    boolean isBaseTriggerRespin = false;
                    if (gameLogicBean.getRespinCountsLeft() > 0) {
                        isBaseTriggerRespin = true;
                    }
                    int fsType = 0;
                    //start freespin or bonus
                    while (true) {
                        if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                            while (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_FREESPIN) {
                                PlayerInputInfo playerInput = new PlayerInputInfo();
                                playerInput.setRequestGameStatus(200);
                                gameLogicBean = (SlotGameLogicBean) engine.gameProgress(gameLogicBean, gameLogicMap, playerInput, null, null, null);

                                long freespinWon = 0L;
                                SlotSpinResult fsSpinResult = null;
                                if (slotConfigInfo.getPlayGameCount() == 1) {
                                    fsSpinResult = gameLogicBean.getSlotFsSpinResults().get(gameLogicBean.getSlotFsSpinResults().size() - 1);
                                    freespinWon = fsSpinResult.getSlotPay();
                                } else if (slotConfigInfo.getPlayGameCount() > 1) {
                                    List<List<SlotSpinResult>> fsSpinResults = gameLogicBean.getSlotFsSpinResults4Multi();
                                    for (List<SlotSpinResult> spinResultList : fsSpinResults) {
                                        if (spinResultList != null && spinResultList.get(spinResultList.size() - 1) != null) {
                                            freespinWon += spinResultList.get(spinResultList.size() - 1).getSlotPay();
                                        }
                                    }
                                }
                                totalWon += freespinWon;
                                //TODO PlayGameCount>1
                                if (isBaseTriggerRespin && fsSpinResult.getSpinType() == SlotEngineConstant.SPIN_TYPE_RESPIN_IN_BASE_GAME) {
                                    baseCoinOut += freespinWon;
                                    resultInfo.setBaseGameTotalWin(resultInfo.getBaseGameTotalWin() + freespinWon);
                                } else {
                                    //TODO model8050802 and 8130802 bonusWheel
                                    fsCoinOut += freespinWon;
                                    if (!isAchievement && tiersInfo != null) {
                                        setTierFsInfo(gameLogicBean, fsType, freespinWon, slotConfigInfo, tiersInfo);
                                    }
                                }

                            }
                            if (scatterWin > 0) {
                                fsCoinOut += scatterWin;
                            }
                        } else if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_BONUS) {
                            int bonusType;
                            if ("bonus".equalsIgnoreCase(gameLogicBean.getNextScenes()) || "bonus1".equalsIgnoreCase(gameLogicBean.getNextScenes())) {
                                bonusType = 1;
                            } else {
                                bonusType = 2;
                            }
                            PlayerInputInfo playerInput = new PlayerInputInfo();
                            playerInput.setRequestGameStatus(500);
                            //bonusChoice random Index
                            int bonusChoiceIndex = slotConfigInfo.getChoiceFsOrBonusIndex();
                            if (slotConfigInfo.isRandomBonusChoice() && bonusChoiceIndex > 0) {
                                bonusChoiceIndex = RandomUtil.getRandomInt(bonusChoiceIndex);
                            }

                            int[] cardList = null;
                            for (int pick = 0; pick < 100; pick++) {
                                if (pick > 0) {
                                    int[] picks = GameEngineCompute.initArray(pick, bonusChoiceIndex);
                                    /*if (model instanceof Model1010131Test || model instanceof Model1010132Test ||
                                            model instanceof Model1010133Test) {
                                        picks = GameEngineCompute.computePokeCardPickIndex(pick, cardList);
                                    }*/
                                    playerInput.setBonusPickInfos(picks);
                                }
                                gameLogicBean = (SlotGameLogicBean) engine.gameProgress(gameLogicBean, gameLogicMap, playerInput, null, null, null);

                                SlotBonusResult baseBonusResult = gameLogicBean.getSlotBonusResult();
                                /*if (baseBonusResult instanceof BaseHighOrLowBonusResult) {
                                    cardList = ((BaseHighOrLowBonusResult) baseBonusResult).getCardList();
                                }*/
                                if (baseBonusResult.getBonusPlayStatus() == 1000) {
                                    if (baseBonusResult instanceof SlotChoice2FsOrPickBonusResult) {
                                        fsType = ((SlotChoice2FsOrPickBonusResult) baseBonusResult).getFsType();
                                    } else if (baseBonusResult instanceof SlotChoiceFSBonusResult) {
                                        fsType = ((SlotChoiceFSBonusResult) baseBonusResult).getFsType();
                                    }
                                    /*else if (baseBonusResult instanceof BaseChoiceFSBonusResult) {
                                        fsType = ((BaseChoiceFSBonusResult) baseBonusResult).getFreespinType();
                                    } else if (baseBonusResult instanceof BaseJungleTourWithSIPBonusResult) {
                                        fsType = ((BaseJungleTourWithSIPBonusResult) baseBonusResult).getFreeSpinType();
                                    } else if (baseBonusResult instanceof BaseMultiSlotFsOrPickBonusResult) {
                                        fsType = ((BaseMultiSlotFsOrPickBonusResult) baseBonusResult).getFreespinType();
                                    }*/
                                    long bonusWon = baseBonusResult.getTotalPay();
                                    bonusCoinOut += bonusWon;
                                    totalWon += bonusWon;
                                    if (!isAchievement && tiersInfo != null) {
                                        setTierBonusInfo(bonusType, bonusWon, slotConfigInfo, tiersInfo);
                                    }
                                    break;
                                }
                            }

                        } else if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                            break;
                        }

                    }

                }
                if (totalWon > 0) {
                    resultInfo.setTotalHit(resultInfo.getTotalHit() + 1);
                }
                resultInfo.setTotalCoinOut(
                        resultInfo.getTotalCoinOut() + totalWon);
                resultInfo.setTotalAmount(totalWon);
                initCredit += totalWon;
                resultInfo.setLeftCredit(initCredit);
                //Total Win
                if (resultInfo.getPayWeightMap().containsKey(totalWon)) {
                    long value = resultInfo.getPayWeightMap().get(totalWon) + 1;
                    resultInfo.getPayWeightMap().put(totalWon, value);
                } else {
                    resultInfo.getPayWeightMap().put(totalWon, 1L);
                }
                //BaseGame Win
                if (resultInfo.getBasePayWeightMap().containsKey(baseCoinOut)) {
                    long value = resultInfo.getPayWeightMap().get(baseCoinOut) + 1;
                    resultInfo.getBasePayWeightMap().put(baseCoinOut, value);
                } else {
                    resultInfo.getBasePayWeightMap().put(baseCoinOut, 1L);
                }
                computeStdDev(slotConfigInfo, resultInfo, totalWon, totalBet);
                computeBaseStdDev(slotConfigInfo, resultInfo, baseCoinOut, totalBet);
                if (!isAchievement) {
                    tiersInfo.setTotalWon(totalWon);
                    tiersInfo.setTotalBet(totalBet);
                    tiersInfo.setBaseWin(baseCoinOut);
                    tiersInfo.setFsWin(fsCoinOut);
                    tiersInfo.setBonusWin(bonusCoinOut);
                    computeFsBonus(slotConfigInfo, resultInfo, tiersInfo);
                    computeMaxWinCount(resultInfo, slotConfigInfo, tiersInfo);
                }
                if (spinCount > 0 && spinCount % playTime == 0) {
                    outResultInfo(slotConfigInfo, resultInfo, isAchievement);
                    if (!isAchievement) {
                        outHighMissResultInfo(slotConfigInfo, resultInfo, isMissStackSymbol);
                        if (slotConfigInfo.isFsPay() || slotConfigInfo.isBonusPay()) {
                            outFsBonusResultInfo(slotConfigInfo, resultInfo);
                        }
                    }
                }
                if (isAchievement) {
                    boolean isReset = isResetCacheData(slotConfigInfo, resultInfo);
                    if (isReset) {
                        if (outputIndex > 0) {
                            reInitWriteFile(slotConfigInfo);
                        }
                        outAchievementResult(slotConfigInfo, resultInfo, outputIndex);
                        outputIndex++;
                        resetPayAchievementCacheData(slotConfigInfo, resultInfo);
                    } else if (spinCount == simulationCount) {
                        if (outputIndex > 0) {
                            reInitWriteFile(slotConfigInfo);
                        }
                        outAchievementResult(slotConfigInfo, resultInfo, outputIndex);
                        resetPayAchievementCacheData(slotConfigInfo, resultInfo);
                    }
                }

            }

        } catch (InvalidGameStateException e) {
            log.error("engine gameStart", e);
            e.printStackTrace();
        } catch (Exception e) {
            log.error("cycleSpinForBaseReelsGame run exception", e);
        }
    }

    private void computeBaseStdDev(SlotConfigInfo slotConfigInfo, BaseResultInfo resultInfo, long baseCoinOut, long totalBet) {
        //compute Base Deviation
        double basePayBack = baseCoinOut * 1.0 / totalBet;
        double expPayBack = slotConfigInfo.getBaseExpRtp() * 1.0 / 100;
        double deviation = Math.pow((basePayBack - expPayBack), 2);
        resultInfo.setBaseStdDeviation(resultInfo.getBaseStdDeviation() + deviation);
    }

    private long computeScatterWin(SlotSpinResult baseSpinResult, BaseSlotModel model) {
        long scatterWin = 0;
        /*if (baseSpinResult != null) {
            int[] hitSymbols = baseSpinResult.getHitSlotSymbolsSound();
            long[] hitAmounts = baseSpinResult.getHitSlotPays();
            int[] hitSymbolCount = baseSpinResult.getHitSlotSymbolCount();
            if (model instanceof Model1030130Test) {
                if (hitSymbols != null && hitAmounts != null) {
                    for (int i = 0; i < hitSymbols.length; i++) {
                        if (hitSymbols[i] == BaseConstant.SCATTER_COMM_SYMBOL) {
                            scatterWin += hitAmounts[i];
                            break;
                        }
                    }
                }
            } else if (model instanceof Model1160130Test) {
                if (hitSymbols != null && hitAmounts != null) {
                    for (int i = 0; i < hitSymbols.length; i++) {
                        if (hitSymbols[i] == BaseConstant.SCATTER_CON_SYMBOL && hitSymbolCount[i] == 3) {
                            scatterWin += hitAmounts[i];
                            break;
                        }
                    }
                }
            } else if (model instanceof Model1180130Test) {
                if (hitSymbols != null && hitAmounts != null) {
                    for (int i = 0; i < hitSymbols.length; i++) {
                        if (hitSymbols[i] == BaseConstant.SCATTER_COMM_SYMBOL) {
                            scatterWin += hitAmounts[i];
                            break;
                        }
                    }
                }
            }
        }*/
        return scatterWin;
    }

    private void outAchievementResult(SlotConfigInfo configInfo, BaseResultInfo resultInfo, int outputIndex) {
        int index = 0;
        if (outputIndex > 0) {
            index += outputIndex * configInfo.getMaxFileAchievementPayIndex();
        }
        int sizeIndex = 0;
        while (true) {
            StringBuilder strbContent = new StringBuilder();
            strbContent.append(index + 1).append(BaseConstant.TAB_STR);
            int count = 0;
            for (AchievementSymbol achievementSymbol : resultInfo.getBaseAchievementSymbol()) {
                strbContent.append("Symbol").append(achievementSymbol.getSymbolNo()).append(BaseConstant.TAB_STR);
                List<AchievementPay> payAchievementList = achievementSymbol.getAchievementPayList();
                for (int pay : configInfo.getAchievementPay()) {
                    AchievementPay achievementPay = payAchievementList.get(pay - 1);
                    if (achievementPay.getPayAchievement().size() > sizeIndex) {
                        strbContent.append(achievementPay.getPayAchievement().get(sizeIndex)).append(BaseConstant.TAB_STR);
                    } else {
                        count++;
                        strbContent.append(BaseConstant.TAB_STR);
                    }
                }
                if (achievementSymbol.getPayTotalAchievement().size() > sizeIndex) {
                    strbContent.append(achievementSymbol.getPayTotalAchievement().get(sizeIndex)).append(BaseConstant.TAB_STR);
                } else {
                    strbContent.append(BaseConstant.TAB_STR);
                }
            }
            int len = configInfo.getAchievementSymbol().length * configInfo.getAchievementPay().length;
            if (count == len) {
                break;
            }
            FileWriteUtil.outputPrint(strbContent.toString(), configInfo.getAchievementFileName(), configInfo, 1);
            index++;
            sizeIndex++;
        }
    }

    private void reInitWriteFile(SlotConfigInfo configInfo) {
        String fileName = configInfo.getAchievementFileName();
        fileName = fileName.replace(FileWriteUtil.fileIndex1 + ".txt", (FileWriteUtil.fileIndex1 + 1)
                + ".txt");
        configInfo.setAchievementFileName(fileName);
        FileWriteUtil.fileIndex1++;
        FileWriteUtil.createNewFile(fileName);
    }


    private void resetPayAchievementCacheData(SlotConfigInfo configInfo, BaseResultInfo resultInfo) {
        for (AchievementSymbol achievementSymbol : resultInfo.getBaseAchievementSymbol()) {
            List<AchievementPay> payAchievementList = achievementSymbol.getAchievementPayList();
            for (int pay : configInfo.getAchievementPay()) {
                AchievementPay achievementPay = payAchievementList.get(pay - 1);
                achievementPay.getPayAchievement().clear();
            }
            achievementSymbol.getPayTotalAchievement().clear();
        }
    }

    private boolean isResetCacheData(SlotConfigInfo configInfo, BaseResultInfo resultInfo) {
        for (AchievementSymbol achievementSymbol : resultInfo.getBaseAchievementSymbol()) {
            List<AchievementPay> payAchievementList = achievementSymbol.getAchievementPayList();
            for (int pay : configInfo.getAchievementPay()) {
                AchievementPay achievementPay = payAchievementList.get(pay - 1);
                if (achievementPay.getPayAchievement().size() >= configInfo.getMaxFileAchievementPayIndex()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void outFsBonusResultInfo(SlotConfigInfo configInfo, BaseResultInfo resultInfo) {
        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append("Num of Spin").append(BaseConstant.TAB_STR);
            if (configInfo.isFsPay()) {
                for (int i = 0; i < resultInfo.getFsEntries().length; i++) {
                    for (int j = 0; j < resultInfo.getFsEntries()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(j).append(" FsType").append(i + 1).append(" fsEntries").append(BaseConstant.TAB_STR);
                    }
                    for (int j = 0; j < resultInfo.getFsTypeTimes()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(j).append(" FsType").append(i + 1).append(" FreeSpins#").append(BaseConstant.TAB_STR);
                    }
                    for (int j = 0; j < resultInfo.getFsPerHit()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(j).append(" FsType").append(i + 1).append(" Hits#").append(BaseConstant.TAB_STR);
                    }
                    for (int j = 0; j < resultInfo.getFsTypeTotalWin()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(j).append(" FsType").append(i + 1).append(" FsTotalPay").append(BaseConstant.TAB_STR);
                    }
                    for (int j = 0; j < resultInfo.getFsMaxPay()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(j).append(" FsType").append(i + 1).append(" FsMax").append(BaseConstant.TAB_STR);
                    }
                }

            }
            if (configInfo.isBonusPay()) {
                for (int i = 0; i < resultInfo.getBonusEntries().length; i++) {
                    for (int j = 0; j < resultInfo.getBonusEntries()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(j).append(" BonusType").append(i + 1).append(" BonusEntries").append(BaseConstant.TAB_STR);
                    }
                    for (int j = 0; j < resultInfo.getBonusTimes()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(j).append(" BonusType").append(i + 1).append(" PickBonus#").append(BaseConstant.TAB_STR);
                    }
                    for (int j = 0; j < resultInfo.getBonusPerHit()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(j).append(" BonusType").append(i + 1).append(" Hits#").append(BaseConstant.TAB_STR);
                    }
                    for (int j = 0; j < resultInfo.getBonusTypeTotalWin()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(j).append(" BonusType").append(i + 1).append(" BonusTotalPay#").append(BaseConstant.TAB_STR);
                    }
                    for (int j = 0; j < resultInfo.getBonusMaxPay()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(j).append(" BonusType").append(i + 1).append(" BonusMax").append(BaseConstant.TAB_STR);
                    }
                }

            }
            FileWriteUtil.writeFileHeadInfo(configInfo.getFsBonusFileName(), strbHeader.toString());
        }
        StringBuilder strContent = new StringBuilder();
        strContent.append(resultInfo.getSpinCount())
                .append(BaseConstant.TAB_STR);
        if (configInfo.isFsPay()) {
            for (int i = 0; i < resultInfo.getFsEntries().length; i++) {
                for (long fsEntries : resultInfo.getFsEntries()[i]) {
                    strContent.append(fsEntries).append(BaseConstant.TAB_STR);
                }
                for (long fsTimes : resultInfo.getFsTypeTimes()[i]) {
                    strContent.append(fsTimes).append(BaseConstant.TAB_STR);
                }
                for (long fsHits : resultInfo.getFsPerHit()[i]) {
                    strContent.append(fsHits).append(BaseConstant.TAB_STR);
                }
                for (long fsWin : resultInfo.getFsTypeTotalWin()[i]) {
                    strContent.append(fsWin).append(BaseConstant.TAB_STR);
                }
                for (long fsMax : resultInfo.getFsMaxPay()[i]) {
                    strContent.append(fsMax).append(BaseConstant.TAB_STR);
                }
            }

        }
        if (configInfo.isBonusPay()) {
            for (int i = 0; i < resultInfo.getBonusEntries().length; i++) {
                for (long bonusEntries : resultInfo.getBonusEntries()[i]) {
                    strContent.append(bonusEntries).append(BaseConstant.TAB_STR);
                }
                for (long bonusTimes : resultInfo.getBonusTimes()[i]) {
                    strContent.append(bonusTimes).append(BaseConstant.TAB_STR);
                }
                for (long bonusHits : resultInfo.getBonusPerHit()[i]) {
                    strContent.append(bonusHits).append(BaseConstant.TAB_STR);
                }
                for (long bonusWin : resultInfo.getBonusTypeTotalWin()[i]) {
                    strContent.append(bonusWin).append(BaseConstant.TAB_STR);
                }
                for (long bonusMax : resultInfo.getBonusMaxPay()[i]) {
                    strContent.append(bonusMax).append(BaseConstant.TAB_STR);
                }
            }
        }

        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getFsBonusFileName(), configInfo, 3);
    }

    private void outHighMissResultInfo(SlotConfigInfo configInfo, BaseResultInfo resultInfo, boolean isMissStackSymbol) {
        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append("Num of Spin").append(BaseConstant.TAB_STR);
            for (int i = 0; i < resultInfo.getHighSymbolPerMaxPayHit().length; i++) {
                for (int j = 0; j < resultInfo.getHighSymbolPerMaxPayHit()[i].length; j++) {
                    strbHeader.append("Win Tier ").append(i).append(" High SymbolCount").append(j).append(" Hit").append(BaseConstant.TAB_STR);
                }
            }
            if (isMissStackSymbol) {
                for (int i = 0; i < resultInfo.getNearMissSymbolHit().length; i++) {
                    for (int j = 0; j < resultInfo.getNearMissSymbolHit()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(i).append(" Miss Symbol Stack").append(j).append(" Hit").append(BaseConstant.TAB_STR);
                    }
                }
                for (int i = 0; i < resultInfo.getNearMissWildSymbolHit().length; i++) {
                    for (int j = 0; j < resultInfo.getNearMissWildSymbolHit()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(i).append(" Miss Symbol Contain Wild Stack").append(j).append(" Hit").append(BaseConstant.TAB_STR);
                    }
                }
            }
            if (configInfo.isSumHorizontalSymbol()) {
                for (int i = 0; i < resultInfo.getHorizontalSymbolHit().length; i++) {
                    for (int j = 0; j < resultInfo.getHorizontalSymbolHit()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(i).append(" Horizontal SymbolCount").append(j).append(" Hit").append(BaseConstant.TAB_STR);
                    }
                }
                for (int i = 0; i < resultInfo.getAllNearMissSymbolHit().length; i++) {
                    for (int j = 0; j < resultInfo.getAllNearMissSymbolHit()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(i).append(" All Miss Symbol").append(j).append(" Hit").append(BaseConstant.TAB_STR);
                    }
                }
            } else if (isMissStackSymbol) {
                for (int i = 0; i < resultInfo.getAllNearMissSymbolHit().length; i++) {
                    for (int j = 0; j < resultInfo.getAllNearMissSymbolHit()[i].length; j++) {
                        strbHeader.append("Win Tier ").append(i).append(" All Miss Symbol").append(j).append(" Hit").append(BaseConstant.TAB_STR);
                    }
                }
            }
            FileWriteUtil.writeFileHeadInfo(configInfo.getHighMissFileName(), strbHeader.toString());
        }
        StringBuilder strContent = new StringBuilder();
        strContent.append(resultInfo.getSpinCount())
                .append(BaseConstant.TAB_STR);
        for (long[] tierHighSymbol : resultInfo.getHighSymbolPerMaxPayHit()) {
            for (long highSymbolHit : tierHighSymbol) {
                strContent.append(highSymbolHit).append(BaseConstant.TAB_STR);
            }
        }
        //stack symbol
        if (isMissStackSymbol) {
            for (long[] tierMissSymbol : resultInfo.getNearMissSymbolHit()) {
                for (long missSymbolHit : tierMissSymbol) {
                    strContent.append(missSymbolHit).append(BaseConstant.TAB_STR);
                }
            }
            for (long[] tierWildMissSymbol : resultInfo.getNearMissWildSymbolHit()) {
                for (long missSymbolHit : tierWildMissSymbol) {
                    strContent.append(missSymbolHit).append(BaseConstant.TAB_STR);
                }
            }
        }
        //horizontal symbol count
        if (configInfo.isSumHorizontalSymbol()) {
            for (long[] tierHorizontalSymbol : resultInfo.getHorizontalSymbolHit()) {
                for (long horizontalSymbolHit : tierHorizontalSymbol) {
                    strContent.append(horizontalSymbolHit).append(BaseConstant.TAB_STR);
                }
            }
            for (long[] tierAllMissSymbol : resultInfo.getAllNearMissSymbolHit()) {
                for (long missSymbolHit : tierAllMissSymbol) {
                    strContent.append(missSymbolHit).append(BaseConstant.TAB_STR);
                }
            }
        } else if (isMissStackSymbol) {
            for (long[] tierAllMissSymbol : resultInfo.getAllNearMissSymbolHit()) {
                for (long missSymbolHit : tierAllMissSymbol) {
                    strContent.append(missSymbolHit).append(BaseConstant.TAB_STR);
                }
            }
        }

        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getHighMissFileName(), configInfo, 2);
    }

    private void outResultInfo(SlotConfigInfo configInfo, BaseResultInfo resultInfo, boolean isAchievement) {
        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append("Num of Spin").append(BaseConstant.TAB_STR);
            strbHeader.append("TotalWinAmount").append(BaseConstant.TAB_STR);
            strbHeader.append("Total CoinIn").append(BaseConstant.TAB_STR);
            strbHeader.append("Total CoinOut").append(BaseConstant.TAB_STR);
            strbHeader.append("PayBack").append(BaseConstant.TAB_STR);
            strbHeader.append("Win Standard Deviation").append(BaseConstant.TAB_STR);
            strbHeader.append("RTP Standard Deviation").append(BaseConstant.TAB_STR);
            if (!isAchievement) {
                for (int i = 0; i < resultInfo.getMaxWinCount().length; i++) {
                    strbHeader.append("Win Tier ").append(i).append(" Count").append(BaseConstant.TAB_STR);
                }
                for (int i = 0; i < resultInfo.getMaxWinTotalPay().length; i++) {
                    strbHeader.append("Win Tier ").append(i).append(" TotalWin").append(BaseConstant.TAB_STR);
                }
                for (int i = 0; i < resultInfo.getBaseTotalMaxPay().length; i++) {
                    strbHeader.append("Base ").append(i).append(" Total Max Pay").append(BaseConstant.TAB_STR);
                }
                for (int i = 0; i < resultInfo.getFsTotalMaxPay().length; i++) {
                    strbHeader.append("Fs ").append(i).append(" Total Max Pay").append(BaseConstant.TAB_STR);
                }
                for (int i = 0; i < resultInfo.getBonusTotalMaxPay().length; i++) {
                    strbHeader.append("Bonus ").append(i).append(" Total Max Pay").append(BaseConstant.TAB_STR);
                }
                for (int i = 0; i < resultInfo.getMaxPayTotalHit().length; i++) {
                    strbHeader.append("Max Pay ").append(i).append(" Total Hit").append(BaseConstant.TAB_STR);
                }
                for (int i = 0; i < resultInfo.getScreenMaxPay().length; i++) {
                    strbHeader.append("Max Pay ").append(i).append(" Award").append(BaseConstant.TAB_STR);
                }
                for (int i = 0; i < resultInfo.getBaseTotalPay().length; i++) {
                    strbHeader.append("Base ").append(i).append(" Total Pay").append(BaseConstant.TAB_STR);
                }
                for (int i = 0; i < resultInfo.getFsTotalPay().length; i++) {
                    strbHeader.append("Fs ").append(i).append(" Total Pay").append(BaseConstant.TAB_STR);
                }
                for (int i = 0; i < resultInfo.getBonusTotalPay().length; i++) {
                    strbHeader.append("Bonus ").append(i).append(" Total Pay").append(BaseConstant.TAB_STR);
                }
                if (configInfo.isFsPay()) {
                    for (int i = 0; i < resultInfo.getFsStdDeviation().length; i++) {
                        strbHeader.append("Fs").append(i + 1).append(" Total Entries").append(BaseConstant.TAB_STR);
                        strbHeader.append("Fs").append(i + 1).append(" Total Win").append(BaseConstant.TAB_STR);
                        strbHeader.append("Fs").append(i + 1).append(" Win StdDev").append(BaseConstant.TAB_STR);
                        strbHeader.append("Fs").append(i + 1).append(" RTP StdDev").append(BaseConstant.TAB_STR);
                    }
                }
                if (configInfo.isBonusPay()) {
                    for (int i = 0; i < resultInfo.getBonusStdDeviation().length; i++) {
                        strbHeader.append("Bonus").append(i + 1).append(" Total Entries").append(BaseConstant.TAB_STR);
                        strbHeader.append("Bonus").append(i + 1).append(" Total Win").append(BaseConstant.TAB_STR);
                        strbHeader.append("Bonus").append(i + 1).append(" Win StdDev").append(BaseConstant.TAB_STR);
                        strbHeader.append("Bonus").append(i + 1).append(" RTP StdDev").append(BaseConstant.TAB_STR);
                    }
                }
                strbHeader.append("Base Total Hit").append(BaseConstant.TAB_STR);
                strbHeader.append("Base Total Win").append(BaseConstant.TAB_STR);
                strbHeader.append("Base Win Standard Deviation").append(BaseConstant.TAB_STR);
                strbHeader.append("Base RTP Standard Deviation").append(BaseConstant.TAB_STR);
            }
            FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), strbHeader.toString());
        }
        StringBuilder strContent = new StringBuilder();
        strContent.append(resultInfo.getSpinCount())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalAmount())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalCoinIn())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalCoinOut())
                .append(BaseConstant.TAB_STR);
        double payBack = resultInfo.getTotalCoinOut() * 1.0
                / resultInfo.getTotalCoinIn();
        strContent.append(payBack).append(BaseConstant.TAB_STR);
        double averageWin = resultInfo.getTotalCoinOut() * 1.0 / resultInfo.getSpinCount();
        double varWin = 0.0;
        for (Map.Entry<Long, Long> entry : resultInfo.getPayWeightMap().entrySet()) {
            long pay = entry.getKey();
            long weight = entry.getValue();
            varWin += weight * Math.pow((pay - averageWin), 2);
        }
        double winStdDev = Math.sqrt(varWin / resultInfo.getSpinCount());
        strContent.append(winStdDev).append(BaseConstant.TAB_STR);
        double stdDev = Math.sqrt(resultInfo.getStdDeviation() / resultInfo.getSpinCount());
        strContent.append(stdDev).append(BaseConstant.TAB_STR);
        if (!isAchievement) {
            for (long maxWinCount : resultInfo.getMaxWinCount()) {
                strContent.append(maxWinCount).append(BaseConstant.TAB_STR);
            }
            for (long maxWinPay : resultInfo.getMaxWinTotalPay()) {
                strContent.append(maxWinPay).append(BaseConstant.TAB_STR);
            }
            for (long maxPayBase : resultInfo.getBaseTotalMaxPay()) {
                strContent.append(maxPayBase).append(BaseConstant.TAB_STR);
            }
            for (long maxPayFs : resultInfo.getFsTotalMaxPay()) {
                strContent.append(maxPayFs).append(BaseConstant.TAB_STR);
            }
            for (long maxPayBonus : resultInfo.getBonusTotalMaxPay()) {
                strContent.append(maxPayBonus).append(BaseConstant.TAB_STR);
            }
            for (long maxPayHit : resultInfo.getMaxPayTotalHit()) {
                strContent.append(maxPayHit).append(BaseConstant.TAB_STR);
            }
            for (long maxPayAward : resultInfo.getScreenMaxPay()) {
                strContent.append(maxPayAward).append(BaseConstant.TAB_STR);
            }
            for (long basePay : resultInfo.getBaseTotalPay()) {
                strContent.append(basePay).append(BaseConstant.TAB_STR);
            }
            for (long fsPay : resultInfo.getFsTotalPay()) {
                strContent.append(fsPay).append(BaseConstant.TAB_STR);
            }
            for (long bonusPay : resultInfo.getBonusTotalPay()) {
                strContent.append(bonusPay).append(BaseConstant.TAB_STR);
            }
            if (configInfo.isFsPay()) {
                //compute fs Win/RTP Standard Deviation and
                for (int i = 0; i < resultInfo.getFsStdDeviation().length; i++) {
                    Map<Long, Long> fsPayWeight = resultInfo.getFsPayWeightMapList().get(i);
                    long[] fsEntries = resultInfo.getFsEntries()[i];
                    long[] fsTotalWin = resultInfo.getFsTypeTotalWin()[i];
                    long fsTotalEntries = 0L;
                    long fsSumWin = 0L;
                    for (long fsHit : fsEntries) {
                        fsTotalEntries += fsHit;
                    }
                    for (long fsWin : fsTotalWin) {
                        fsSumWin += fsWin;
                    }
                    strContent.append(fsTotalEntries).append(BaseConstant.TAB_STR);
                    strContent.append(fsSumWin).append(BaseConstant.TAB_STR);
                    double fsVarWin = 0.0;
                    double fsAverageWin = 0.0;
                    if (fsTotalEntries > 0) {
                        fsAverageWin = fsSumWin / fsTotalEntries;
                    }
                    for (Map.Entry<Long, Long> entry : fsPayWeight.entrySet()) {
                        long pay = entry.getKey();
                        long weight = entry.getValue();
                        fsVarWin += weight * Math.pow((pay - fsAverageWin), 2);
                    }
                    double fsWinStdDev = Math.sqrt(fsVarWin / fsTotalEntries);
                    strContent.append(fsWinStdDev).append(BaseConstant.TAB_STR);
                    double fsStdDev = Math.sqrt(resultInfo.getFsStdDeviation()[i] / fsTotalEntries);
                    strContent.append(fsStdDev).append(BaseConstant.TAB_STR);
                }
            }
            if (configInfo.isBonusPay()) {
                //compute bonus Win/RTP Standard Deviation
                for (int i = 0; i < resultInfo.getBonusStdDeviation().length; i++) {
                    Map<Long, Long> bonusPayWeight = resultInfo.getBonusPayWeightMapList().get(i);
                    long[] bonusEntries = resultInfo.getBonusEntries()[i];
                    long[] bonusTotalWin = resultInfo.getBonusTypeTotalWin()[i];
                    long bonusTotalEntries = 0L;
                    long bonusSumWin = 0L;
                    for (long bonusHit : bonusEntries) {
                        bonusTotalEntries += bonusHit;
                    }
                    for (long bonusWin : bonusTotalWin) {
                        bonusSumWin += bonusWin;
                    }
                    strContent.append(bonusTotalEntries).append(BaseConstant.TAB_STR);
                    strContent.append(bonusSumWin).append(BaseConstant.TAB_STR);
                    double bonusVarWin = 0.0;
                    double bonusAverageWin = 0.0;
                    if (bonusTotalEntries > 0) {
                        bonusAverageWin = bonusSumWin / bonusTotalEntries;
                    }
                    for (Map.Entry<Long, Long> entry : bonusPayWeight.entrySet()) {
                        long pay = entry.getKey();
                        long weight = entry.getValue();
                        bonusVarWin += weight * Math.pow((pay - bonusAverageWin), 2);
                    }
                    double bonusWinStdDev = Math.sqrt(bonusVarWin / bonusTotalEntries);
                    strContent.append(bonusWinStdDev).append(BaseConstant.TAB_STR);
                    double bonusStdDev = Math.sqrt(resultInfo.getBonusStdDeviation()[i] / bonusTotalEntries);
                    strContent.append(bonusStdDev).append(BaseConstant.TAB_STR);
                }
            }
            //compute Base win std dev
            strContent.append(resultInfo.getTotalHit()).append(BaseConstant.TAB_STR);
            strContent.append(resultInfo.getBaseGameTotalWin()).append(BaseConstant.TAB_STR);
            double baseAverageWin = resultInfo.getBaseGameTotalWin() * 1.0 / resultInfo.getSpinCount();
            double baseVarWin = 0.0;
            for (Map.Entry<Long, Long> entry : resultInfo.getBasePayWeightMap().entrySet()) {
                long pay = entry.getKey();
                long weight = entry.getValue();
                baseVarWin += weight * Math.pow((pay - baseAverageWin), 2);
            }
            double baseWinStdDev = Math.sqrt(baseVarWin / resultInfo.getSpinCount());
            strContent.append(baseWinStdDev).append(BaseConstant.TAB_STR);
            double baseStdDev = Math.sqrt(resultInfo.getBaseStdDeviation() / resultInfo.getSpinCount());
            strContent.append(baseStdDev).append(BaseConstant.TAB_STR);
        }
        FileWriteUtil.outputPrint(strContent.toString(), configInfo.getOutputFileName(), configInfo, 0);
    }

    private void computeMaxWinCount(BaseResultInfo resultInfo, SlotConfigInfo configInfo, TiersInfo tiersInfo) {
        if (resultInfo.getMaxWinCount() != null && resultInfo.getMaxWinCount().length > 0) {
            if (tiersInfo.getTotalWon() != (tiersInfo.getBaseWin() + tiersInfo.getFsWin() + tiersInfo.getBonusWin())) {
                System.out.println("totalWon=" + tiersInfo.getTotalWon() + ",baseWin=" + tiersInfo.getBaseWin() + ",fsWin=" + tiersInfo.getFsWin() + ",bonusWin=" + tiersInfo.getBonusWin());
                log.debug("totalWon=" + tiersInfo.getTotalWon() + ",baseWin=" + tiersInfo.getBaseWin() + ",fsWin=" + tiersInfo.getFsWin() + ",bonusWin=" + tiersInfo.getBonusWin());
                log.error("computeMaxWinCount(), compute baseWin+fsWin+bonusWin!=totalWon error!");
                throw new IllegalArgumentException("compute baseWin+fsWin+bonusWin!=totalWon error!");
            }
            int fsType = tiersInfo.getFsType();
            int bonusType = tiersInfo.getBonusType();
            int horizontalSymbolCount = tiersInfo.getHorizontalSymbolCount();
            for (int i = 0; i < resultInfo.getMaxWinCount().length; i++) {
                if (i == 0 && tiersInfo.getTotalWon() == 0) {
                    resultInfo.getMaxWinCount()[i]++;
                    resultInfo.getMaxWinTotalPay()[i] += tiersInfo.getTotalWon();


                    resultInfo.getMaxPayTotalHit()[i]++;
                    resultInfo.getHighSymbolPerMaxPayHit()[i][tiersInfo.getHighSymbolCount()]++;
                    if (configInfo.getMissSymbol() != null && configInfo.getMissSymbol().length > 0) {
                        resultInfo.getNearMissSymbolHit()[i][tiersInfo.getStackCount()]++;
                        resultInfo.getNearMissWildSymbolHit()[i][tiersInfo.getStackWildCount()]++;
                    }
                    //Horizontal symbol count
                    if (configInfo.isSumHorizontalSymbol()) {
                        resultInfo.getHorizontalSymbolHit()[i][horizontalSymbolCount]++;
                        resultInfo.getAllNearMissSymbolHit()[i][tiersInfo.getHighAndStackSymbolCount()]++;
                    } else if (configInfo.getMissSymbol() != null) {
                        resultInfo.getAllNearMissSymbolHit()[i][tiersInfo.getHighAndStackSymbolCount()]++;
                    }

                    //fsTotalWin=0
                    if (configInfo.isFsPay() && tiersInfo.getFsTimes() > 0) {
                        resultInfo.getFsEntries()[fsType][i]++;
                        resultInfo.getFsTypeTimes()[fsType][i] += tiersInfo.getFsTimes();
                        //fsWin=0,so fsHit=FsTimes
                        resultInfo.getFsPerHit()[fsType][i] += tiersInfo.getFsTimes();
                    }
                    break;
                } else if (i > 0) {
                    if (i < configInfo.getMaxWin().length && tiersInfo.getTotalWon() <= configInfo.getMaxWin()[i] * tiersInfo.getTotalBet()) {
                        resultInfo.getMaxWinCount()[i]++;
                        resultInfo.getMaxWinTotalPay()[i] += tiersInfo.getTotalWon();
                        resultInfo.getHighSymbolPerMaxPayHit()[i][tiersInfo.getHighSymbolCount()]++;
                        resultInfo.getBaseTotalPay()[i] += tiersInfo.getBaseWin();
                        resultInfo.getFsTotalPay()[i] += tiersInfo.getFsWin();
                        resultInfo.getBonusTotalPay()[i] += tiersInfo.getBonusWin();
                        if (configInfo.getMissSymbol() != null && configInfo.getMissSymbol().length > 0) {
                            resultInfo.getNearMissSymbolHit()[i][tiersInfo.getStackCount()]++;
                            resultInfo.getNearMissWildSymbolHit()[i][tiersInfo.getStackWildCount()]++;
                        }
                        //Horizontal symbol count
                        if (configInfo.isSumHorizontalSymbol()) {
                            resultInfo.getHorizontalSymbolHit()[i][horizontalSymbolCount]++;
                            resultInfo.getAllNearMissSymbolHit()[i][tiersInfo.getHighAndStackSymbolCount()]++;
                        } else if (configInfo.getMissSymbol() != null) {
                            resultInfo.getAllNearMissSymbolHit()[i][tiersInfo.getHighAndStackSymbolCount()]++;
                        }
                        if (tiersInfo.getTotalWon() > resultInfo.getScreenMaxPay()[i]) {
                            resultInfo.getScreenMaxPay()[i] = tiersInfo.getTotalWon();
                            resultInfo.getMaxPayTotalHit()[i] = 1L;
                            resultInfo.getBaseTotalMaxPay()[i] = tiersInfo.getBaseWin();
                            resultInfo.getFsTotalMaxPay()[i] = tiersInfo.getFsWin();
                            resultInfo.getBonusTotalMaxPay()[i] = tiersInfo.getBonusWin();
                        } else if (tiersInfo.getTotalWon() == resultInfo.getScreenMaxPay()[i]) {
                            resultInfo.getMaxPayTotalHit()[i]++;
                            resultInfo.getBaseTotalMaxPay()[i] += tiersInfo.getBaseWin();
                            resultInfo.getFsTotalMaxPay()[i] += tiersInfo.getFsWin();
                            resultInfo.getBonusTotalMaxPay()[i] += tiersInfo.getBonusWin();
                        }
                        //Win Tier Classification (Free Spin Pays)
                        if (configInfo.isFsPay() && tiersInfo.getFsTimes() > 0) {
                            resultInfo.getFsEntries()[fsType][i]++;
                            resultInfo.getFsTypeTimes()[fsType][i] += tiersInfo.getFsTimes();
                            resultInfo.getFsPerHit()[fsType][i] += tiersInfo.getFsHits();
                            resultInfo.getFsTypeTotalWin()[fsType][i] += tiersInfo.getFsWin();
                            if (tiersInfo.getFsWin() > resultInfo.getFsMaxPay()[fsType][i]) {
                                resultInfo.getFsMaxPay()[fsType][i] = tiersInfo.getFsWin();
                            }
                        }
                        //Win Tier Classification (Pick Bonus Pays)
                        if (configInfo.isBonusPay() && tiersInfo.getBonusHit() > 0) {
                            resultInfo.getBonusEntries()[bonusType][i]++;
                            resultInfo.getBonusTimes()[bonusType][i] += tiersInfo.getBonusHit();
                            resultInfo.getBonusPerHit()[bonusType][i] += tiersInfo.getBonusHit();
                            resultInfo.getBonusTypeTotalWin()[bonusType][i] += tiersInfo.getBonusWin();
                            if (tiersInfo.getBonusWin() > resultInfo.getBonusMaxPay()[bonusType][i]) {
                                resultInfo.getBonusMaxPay()[bonusType][i] = tiersInfo.getBonusWin();
                            }
                        }
                        break;
                    } else if (i == configInfo.getMaxWin().length && tiersInfo.getTotalWon() > configInfo.getMaxWin()[i - 1] * tiersInfo.getTotalBet()) {
                        resultInfo.getMaxWinCount()[i]++;
                        resultInfo.getMaxWinTotalPay()[i] += tiersInfo.getTotalWon();
                        resultInfo.getHighSymbolPerMaxPayHit()[i][tiersInfo.getHighSymbolCount()]++;
                        resultInfo.getBaseTotalPay()[i] += tiersInfo.getBaseWin();
                        resultInfo.getFsTotalPay()[i] += tiersInfo.getFsWin();
                        resultInfo.getBonusTotalPay()[i] += tiersInfo.getBonusWin();
                        if (configInfo.getMissSymbol() != null && configInfo.getMissSymbol().length > 0) {
                            resultInfo.getNearMissSymbolHit()[i][tiersInfo.getStackCount()]++;
                            resultInfo.getNearMissWildSymbolHit()[i][tiersInfo.getStackWildCount()]++;
                        }
                        //Horizontal symbol count
                        if (configInfo.isSumHorizontalSymbol()) {
                            resultInfo.getHorizontalSymbolHit()[i][horizontalSymbolCount]++;
                            resultInfo.getAllNearMissSymbolHit()[i][tiersInfo.getHighAndStackSymbolCount()]++;
                        } else if (configInfo.getMissSymbol() != null) {
                            resultInfo.getAllNearMissSymbolHit()[i][tiersInfo.getHighAndStackSymbolCount()]++;
                        }
                        if (tiersInfo.getTotalWon() > resultInfo.getScreenMaxPay()[i]) {
                            resultInfo.getScreenMaxPay()[i] = tiersInfo.getTotalWon();
                            resultInfo.getBaseTotalMaxPay()[i] = tiersInfo.getBaseWin();
                            resultInfo.getFsTotalMaxPay()[i] = tiersInfo.getFsWin();
                            resultInfo.getBonusTotalMaxPay()[i] = tiersInfo.getBonusWin();
                            resultInfo.getMaxPayTotalHit()[i] = 1L;
                        } else if (tiersInfo.getTotalWon() == resultInfo.getScreenMaxPay()[i]) {
                            resultInfo.getMaxPayTotalHit()[i]++;
                            resultInfo.getBaseTotalMaxPay()[i] += tiersInfo.getBaseWin();
                            resultInfo.getFsTotalMaxPay()[i] += tiersInfo.getFsWin();
                            resultInfo.getBonusTotalMaxPay()[i] += tiersInfo.getBonusWin();
                        }
                        //Win Tier Classification (Free Spin Pays)
                        if (configInfo.isFsPay() && tiersInfo.getFsTimes() > 0) {
                            resultInfo.getFsEntries()[fsType][i]++;
                            resultInfo.getFsTypeTimes()[fsType][i] += tiersInfo.getFsTimes();
                            resultInfo.getFsPerHit()[fsType][i] += tiersInfo.getFsHits();
                            resultInfo.getFsTypeTotalWin()[fsType][i] += tiersInfo.getFsWin();
                            if (tiersInfo.getFsWin() > resultInfo.getFsMaxPay()[fsType][i]) {
                                resultInfo.getFsMaxPay()[fsType][i] = tiersInfo.getFsWin();
                            }
                        }
                        //Win Tier Classification (Pick Bonus Pays)
                        if (configInfo.isBonusPay() && tiersInfo.getBonusHit() > 0) {
                            resultInfo.getBonusEntries()[bonusType][i]++;
                            resultInfo.getBonusTimes()[bonusType][i] += tiersInfo.getBonusHit();
                            resultInfo.getBonusPerHit()[bonusType][i] += tiersInfo.getBonusHit();
                            resultInfo.getBonusTypeTotalWin()[bonusType][i] += tiersInfo.getBonusWin();
                            if (tiersInfo.getBonusWin() > resultInfo.getBonusMaxPay()[bonusType][i]) {
                                resultInfo.getBonusMaxPay()[bonusType][i] = tiersInfo.getBonusWin();
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    private void computeFsBonus(SlotConfigInfo configInfo, BaseResultInfo resultInfo, TiersInfo tiersInfo) {
        if (configInfo.isFsPay() && tiersInfo.getFsTimes() > 0) {
            long fsWin = tiersInfo.getFsWin();
            int fsType = tiersInfo.getFsType();
            double fsPayBack = fsWin * 1.0 / tiersInfo.getTotalBet();
            double fsExpPayBack = configInfo.getFsRtp()[fsType] * 1.0 / 100;
            double fsDeviation = Math.pow((fsPayBack - fsExpPayBack), 2);
            resultInfo.getFsStdDeviation()[fsType] += fsDeviation;
            Map<Long, Long> fsPayWeight = resultInfo.getFsPayWeightMapList().get(fsType);
            if (fsPayWeight.containsKey(fsWin)) {
                long value = fsPayWeight.get(fsWin) + 1;
                fsPayWeight.put(fsWin, value);
            } else {
                fsPayWeight.put(fsWin, 1L);
            }
        }
        if (configInfo.isBonusPay() && tiersInfo.getBonusHit() > 0) {
            long bonusWin = tiersInfo.getBonusWin();
            int bonusType = tiersInfo.getBonusType();
            double bonusPayBack = bonusWin * 1.0 / tiersInfo.getTotalBet();
            double bonusExpPayBack = configInfo.getBonusRtp()[bonusType] * 1.0 / 100;
            double bonusDeviation = Math.pow((bonusPayBack - bonusExpPayBack), 2);
            resultInfo.getBonusStdDeviation()[bonusType] += bonusDeviation;
            Map<Long, Long> bonusPayWeight = resultInfo.getBonusPayWeightMapList().get(bonusType);
            if (bonusPayWeight.containsKey(bonusWin)) {
                long value = bonusPayWeight.get(bonusWin) + 1;
                bonusPayWeight.put(bonusWin, value);
            } else {
                bonusPayWeight.put(bonusWin, 1L);
            }
        }
    }

    private void computeStdDev(SlotConfigInfo configInfo, BaseResultInfo resultInfo, long totalWon, long totalBet) {
        //compute Total deviation
        double payBack = totalWon * 1.0 / totalBet;
        double expPayBack = configInfo.getPayback() / 10000;
        double deviation = Math.pow((payBack - expPayBack), 2);
        resultInfo.setStdDeviation(resultInfo.getStdDeviation() + deviation);
    }

    private void setTierBonusInfo(int bonusType, long bonusWon, SlotConfigInfo configInfo, TiersInfo tiersInfo) {
        //TODO special game 8050802,8130802
        if (bonusWon > 0) {
            if (configInfo.getBonusType() > 1 && bonusType >= 1) {
                tiersInfo.setBonusType(bonusType - 1);  //TODO
            } else {
                tiersInfo.setBonusType(0);
            }
            tiersInfo.setBonusHit(tiersInfo.getBonusHit() + 1);
        }
    }

    private void setTierFsInfo(SlotGameLogicBean gameLogicBean, int fsType, long freespinWon, SlotConfigInfo configInfo, TiersInfo tiersInfo) {
        //TODO special game 8050802,8130802
        if (freespinWon > 0) {
            tiersInfo.setFsHits(tiersInfo.getFsHits() + 1);
        }
        tiersInfo.setFsTimes(tiersInfo.getFsTimes() + 1);
        if (configInfo.getFsType() > 1 && fsType >= 1) {
            tiersInfo.setFsType(fsType - 1); //TODO
        } else {
            tiersInfo.setFsType(0);
        }
    }

    /**
     * @param gameLogicBean
     * @param model
     * @param configInfo
     * @return
     * @author Jiangqx
     * @Description o x x x x
     * o o x x x
     * o x x x o
     * x x o x x  Computing four types of patterns
     * @Date 10:21 2023/05/04
     */
    private int computeHorizontalSymbolHit(SlotGameLogicBean gameLogicBean, BaseSlotModel model, SlotConfigInfo configInfo) {
        int wildSymbol = configInfo.getWildSymbol();
        int[] scatterSymbol = configInfo.getScatterSymbol();
        int[] scatterCount = configInfo.getScatterCount();
        int rowsCount = BaseConstant.ROWS_COUNT;
        int reelsCount = BaseConstant.REELS_COUNT;
        if (model instanceof IBaseReelsDefaultConfig) {
            rowsCount = ((IBaseReelsDefaultConfig) model).getRowsCount();
            reelsCount = ((IBaseReelsDefaultConfig) model).getReelsCount();
        }
        int patternsCount = 0;
        if (configInfo.getPlayGameCount() == 1) {
            SlotSpinResult spinResult = gameLogicBean.getSlotSpinResult();
            int[] displaySymbols = spinResult.getSlotDisplaySymbols();
            int[][] symbols = GameEngineCompute.extractHorizontalSymbols(displaySymbols, reelsCount, rowsCount);
            if (reelsCount == 5) {
                for (int i = 0; i < symbols.length; i++) {
                    int[] horizontalSymbol = symbols[i];
                    int tempSymbol = 1;
                    boolean isRowEnd = false;
                    //every row match 4 pattern
                    for (int k = 1; k < reelsCount; k++) {
                        int index = 0;
                        int wildCount = 0;
                        int notWildCount = 0;
                        if (k == 1) {
                            if (horizontalSymbol[0] != wildSymbol) {
                                for (int j = 1; j < horizontalSymbol.length; j++) {
                                    //delete o 1 1 x x (3oak)
                                    if (j < horizontalSymbol.length - 2) {
                                        if (horizontalSymbol[j] == wildSymbol) {
                                            wildCount++;
                                        }
                                    }
                                    if (horizontalSymbol[j] != wildSymbol) {
                                        tempSymbol = horizontalSymbol[j];
                                        break;
                                    }
                                }
                                if (wildCount < 2) {
                                    for (int j = 1; j < horizontalSymbol.length; j++) {
                                        if (horizontalSymbol[0] != horizontalSymbol[j] &&
                                                (tempSymbol == horizontalSymbol[j] || horizontalSymbol[j] == wildSymbol)) {
                                            index++;
                                        }
                                        //x is not contain wild count
                                        if (horizontalSymbol[0] != horizontalSymbol[j] && tempSymbol == horizontalSymbol[j]) {
                                            notWildCount++;
                                        }
                                    }
                                }
                                if (scatterSymbol != null && scatterSymbol.length > 0) {
                                    for (int scatterI = 0; scatterI < scatterSymbol.length; scatterI++) {
                                        //x is scatter
                                        if (tempSymbol == scatterSymbol[scatterI] && notWildCount >= scatterCount[scatterI]) {
                                            isRowEnd = true;
                                            break;
                                        }
                                    }
                                }
                                //hit scatter,end compute pattern
                                if (isRowEnd) {
                                    break;
                                }
                                if (index == horizontalSymbol.length - 1) {
                                    patternsCount++;
                                    break;
                                }
                            }
                        } else if (k == 2) {
                            if (horizontalSymbol[0] == horizontalSymbol[1] && horizontalSymbol[0] != wildSymbol) {
                                //delete o o 1 x x (3oak)
                                if (horizontalSymbol[2] == wildSymbol) {
                                    break;
                                }
                                for (int j = 2; j < horizontalSymbol.length; j++) {
                                    if (horizontalSymbol[j] != wildSymbol) {
                                        tempSymbol = horizontalSymbol[j];
                                        break;
                                    }
                                }
                                for (int j = 2; j < horizontalSymbol.length; j++) {
                                    if (horizontalSymbol[0] != horizontalSymbol[j] &&
                                            (tempSymbol == horizontalSymbol[j] || horizontalSymbol[j] == wildSymbol)) {
                                        index++;
                                    }
                                    //x is not contain wild count
                                    if (horizontalSymbol[0] != horizontalSymbol[j] && tempSymbol == horizontalSymbol[j]) {
                                        notWildCount++;
                                    }
                                }
                                if (scatterSymbol != null && scatterSymbol.length > 0) {
                                    for (int scatterI = 0; scatterI < scatterSymbol.length; scatterI++) {
                                        //o is scatter
                                        if (horizontalSymbol[0] == scatterSymbol[scatterI] && scatterCount[scatterI] <= 2) {
                                            isRowEnd = true;
                                            break;
                                        }
                                        //x is scatter
                                        if (tempSymbol == scatterSymbol[scatterI] && notWildCount >= scatterCount[scatterI]) {
                                            isRowEnd = true;
                                            break;
                                        }
                                    }
                                }
                                //hit scatter,end compute pattern
                                if (isRowEnd) {
                                    break;
                                }
                                if (index == horizontalSymbol.length - 2) {
                                    patternsCount++;
                                    break;
                                }
                            }
                        } else if (k == 3) {
                            if (horizontalSymbol[0] == horizontalSymbol[horizontalSymbol.length - 1] && horizontalSymbol[0] != wildSymbol) {
                                for (int j = 1; j < horizontalSymbol.length - 1; j++) {
                                    //delete o 1 1 x x
                                    if (j < horizontalSymbol.length - 2) {
                                        if (horizontalSymbol[j] == wildSymbol) {
                                            wildCount++;
                                        }
                                    }
                                    if (horizontalSymbol[j] != wildSymbol) {
                                        tempSymbol = horizontalSymbol[j];
                                        break;
                                    }
                                }
                                if (wildCount < 2) {
                                    for (int j = 1; j < horizontalSymbol.length - 1; j++) {
                                        if (horizontalSymbol[0] != horizontalSymbol[j] &&
                                                (tempSymbol == horizontalSymbol[j] || horizontalSymbol[j] == wildSymbol)) {
                                            index++;
                                        }
                                        //x is not contain wild count
                                        if (horizontalSymbol[0] != horizontalSymbol[j] && tempSymbol == horizontalSymbol[j]) {
                                            notWildCount++;
                                        }
                                    }
                                }
                                if (scatterSymbol != null && scatterSymbol.length > 0) {
                                    for (int scatterI = 0; scatterI < scatterSymbol.length; scatterI++) {
                                        //o is scatter
                                        if (horizontalSymbol[0] == scatterSymbol[scatterI] && scatterCount[scatterI] <= 2) {
                                            isRowEnd = true;
                                            break;
                                        }
                                        //x is scatter
                                        if (tempSymbol == scatterSymbol[scatterI] && notWildCount >= scatterCount[scatterI]) {
                                            isRowEnd = true;
                                            break;
                                        }
                                    }
                                }
                                //hit scatter,end compute pattern
                                if (isRowEnd) {
                                    break;
                                }
                                if (index == horizontalSymbol.length - 2) {
                                    patternsCount++;
                                    break;
                                }
                            }
                        } else if (k == 4) {
                            int centerIndex = reelsCount / 2;
                            if (horizontalSymbol[centerIndex] != wildSymbol) {
                                for (int j = 0; j < centerIndex; j++) {
                                    //delete 1 1 o x x
                                    if (horizontalSymbol[j] == wildSymbol) {
                                        wildCount++;
                                    }
                                    if (horizontalSymbol[j] != wildSymbol) {
                                        tempSymbol = horizontalSymbol[j];
                                        break;
                                    }
                                }
                                if (wildCount < 2 && tempSymbol > wildSymbol) {
                                    for (int j = 0; j < centerIndex; j++) {
                                        if (horizontalSymbol[centerIndex] != horizontalSymbol[j] &&
                                                (tempSymbol == horizontalSymbol[j] || horizontalSymbol[j] == wildSymbol)) {
                                            index++;
                                        }
                                        //x is not contain wild count
                                        if (horizontalSymbol[centerIndex] != horizontalSymbol[j] && tempSymbol == horizontalSymbol[j]) {
                                            notWildCount++;
                                        }
                                    }

                                    for (int j = centerIndex + 1; j < horizontalSymbol.length; j++) {
                                        if (horizontalSymbol[centerIndex] != horizontalSymbol[j] &&
                                                (tempSymbol == horizontalSymbol[j] || horizontalSymbol[j] == wildSymbol)) {
                                            index++;
                                        }
                                        //x is not contain wild count
                                        if (horizontalSymbol[centerIndex] != horizontalSymbol[j] && tempSymbol == horizontalSymbol[j]) {
                                            notWildCount++;
                                        }
                                    }
                                }
                                if (scatterSymbol != null && scatterSymbol.length > 0) {
                                    for (int scatterI = 0; scatterI < scatterSymbol.length; scatterI++) {
                                        //x is scatter
                                        if (tempSymbol == scatterSymbol[scatterI] && notWildCount >= scatterCount[scatterI]) {
                                            isRowEnd = true;
                                            break;
                                        }
                                    }
                                }
                                //hit scatter,end compute pattern
                                if (isRowEnd) {
                                    break;
                                }
                                if (index == horizontalSymbol.length - 1) {
                                    patternsCount++;
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if (reelsCount == 3) {
                for (int i = 0; i < symbols.length; i++) {
                    int[] horizontalSymbol = symbols[i];
                    int tempSymbol = 1;
                    boolean isRowEnd = false;
                    //every row match 2 pattern
                    for (int k = 1; k < reelsCount; k++) {
                        int index = 0;
                        int wildCount = 0;
                        int notWildCount = 0;
                        if (k == 1) {
                            if (horizontalSymbol[0] != wildSymbol) {
                                for (int j = 1; j < horizontalSymbol.length; j++) {
                                    //delete o 1 1  (3oak)
                                    if (j < horizontalSymbol.length - 2) {
                                        if (horizontalSymbol[j] == wildSymbol) {
                                            wildCount++;
                                        }
                                    }
                                    if (horizontalSymbol[j] != wildSymbol) {
                                        tempSymbol = horizontalSymbol[j];
                                        break;
                                    }
                                }
                                if (wildCount < 2) {
                                    for (int j = 1; j < horizontalSymbol.length; j++) {
                                        if (horizontalSymbol[0] != horizontalSymbol[j] &&
                                                (tempSymbol == horizontalSymbol[j] || horizontalSymbol[j] == wildSymbol)) {
                                            index++;
                                        }
                                        //x is not contain wild count
                                        if (horizontalSymbol[0] != horizontalSymbol[j] && tempSymbol == horizontalSymbol[j]) {
                                            notWildCount++;
                                        }
                                    }
                                }
                                if (scatterSymbol != null && scatterSymbol.length > 0) {
                                    for (int scatterI = 0; scatterI < scatterSymbol.length; scatterI++) {
                                        //x is scatter
                                        if (tempSymbol == scatterSymbol[scatterI] && notWildCount >= scatterCount[scatterI]) {
                                            isRowEnd = true;
                                            break;
                                        }
                                    }
                                }
                                //hit scatter,end compute pattern
                                if (isRowEnd) {
                                    break;
                                }
                                if (index == horizontalSymbol.length - 1) {
                                    patternsCount++;
                                    break;
                                }
                            }
                        } else if (k == 2) {
                            int centerIndex = 2;
                            if (horizontalSymbol[centerIndex] != wildSymbol) {
                                for (int j = 0; j < centerIndex; j++) {
                                    //delete 1 1 o
                                    if (horizontalSymbol[j] == wildSymbol) {
                                        wildCount++;
                                    }
                                    if (horizontalSymbol[j] != wildSymbol) {
                                        tempSymbol = horizontalSymbol[j];
                                        break;
                                    }
                                }
                                if (wildCount < 2 && tempSymbol > wildSymbol) {
                                    for (int j = 0; j < centerIndex; j++) {
                                        if (horizontalSymbol[centerIndex] != horizontalSymbol[j] &&
                                                (tempSymbol == horizontalSymbol[j] || horizontalSymbol[j] == wildSymbol)) {
                                            index++;
                                        }
                                        //x is not contain wild count
                                        if (horizontalSymbol[centerIndex] != horizontalSymbol[j] && tempSymbol == horizontalSymbol[j]) {
                                            notWildCount++;
                                        }
                                    }

                                    for (int j = centerIndex + 1; j < horizontalSymbol.length; j++) {
                                        if (horizontalSymbol[centerIndex] != horizontalSymbol[j] &&
                                                (tempSymbol == horizontalSymbol[j] || horizontalSymbol[j] == wildSymbol)) {
                                            index++;
                                        }
                                        //x is not contain wild count
                                        if (horizontalSymbol[centerIndex] != horizontalSymbol[j] && tempSymbol == horizontalSymbol[j]) {
                                            notWildCount++;
                                        }
                                    }
                                }
                                if (scatterSymbol != null && scatterSymbol.length > 0) {
                                    for (int scatterI = 0; scatterI < scatterSymbol.length; scatterI++) {
                                        //x is scatter
                                        if (tempSymbol == scatterSymbol[scatterI] && notWildCount >= scatterCount[scatterI]) {
                                            isRowEnd = true;
                                            break;
                                        }
                                    }
                                }
                                //hit scatter,end compute pattern
                                if (isRowEnd) {
                                    break;
                                }
                                if (index == horizontalSymbol.length - 1) {
                                    patternsCount++;
                                    break;
                                }
                            }
                        }
                    }
                }

            }
        } //TODO gameCount>1
        return patternsCount;


    }

    private int computeWildMissSymbolHit(SlotGameLogicBean gameLogicBean, BaseSlotModel model, SlotConfigInfo configInfo) {
        int count = 0;
        int[] missSymbol = configInfo.getMissSymbol();
        int wildSymbol = configInfo.getWildSymbol();
        int rowsCount = BaseConstant.ROWS_COUNT;
        int reelsCount = BaseConstant.REELS_COUNT;
        if (model instanceof IBaseReelsDefaultConfig) {
            rowsCount = ((IBaseReelsDefaultConfig) model).getRowsCount();
            reelsCount = ((IBaseReelsDefaultConfig) model).getReelsCount();
        }
        //stack symbol count,same column S02~S05
        if (configInfo.getPlayGameCount() == 1) {
            SlotSpinResult spinResult = gameLogicBean.getSlotSpinResult();
            if (spinResult != null) {
                int[] displaySymbols = spinResult.getSlotDisplaySymbols();
                for (int k = 0; k < missSymbol.length; k++) {
                    for (int i = 0; i < reelsCount; i++) {
                        int temp = 0;
                        for (int j = 0; j < rowsCount; j++) {
                            if (missSymbol[k] == displaySymbols[j * reelsCount + i] || wildSymbol == displaySymbols[j * reelsCount + i]) {
                                temp++;
                            } else {
                                break;
                            }
                        }
                        int wildSymbolCount = 0;
                        for (int j = 0; j < rowsCount; j++) {
                            if (wildSymbol == displaySymbols[j * reelsCount + i]) {
                                wildSymbolCount++;
                            }
                        }
                        //missSymbol=WildSymbol
                        if ((missSymbol[k] != wildSymbol && wildSymbolCount < rowsCount) && temp == rowsCount) {
                            count++;
                        } else if (missSymbol[k] == wildSymbol && wildSymbolCount == rowsCount && temp == rowsCount) {
                            count++;
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("computeMissSymbolHit() base spin result error!");
            }
        }//TODO gameCount>1
        return count;
    }

    private int computeMissSymbolHit(SlotGameLogicBean gameLogicBean, BaseSlotModel model, SlotConfigInfo configInfo) {
        int count = 0;
        int[] missSymbol = configInfo.getMissSymbol();
        int rowsCount = BaseConstant.ROWS_COUNT;
        int reelsCount = BaseConstant.REELS_COUNT;
        if (model instanceof IBaseReelsDefaultConfig) {
            rowsCount = ((IBaseReelsDefaultConfig) model).getRowsCount();
            reelsCount = ((IBaseReelsDefaultConfig) model).getReelsCount();
        }
        //stack symbol count,same column S02~S05
        if (configInfo.getPlayGameCount() == 1) {
            SlotSpinResult spinResult = gameLogicBean.getSlotSpinResult();
            if (spinResult != null) {
                int[] displaySymbols = spinResult.getSlotDisplaySymbols();
                for (int k = 0; k < missSymbol.length; k++) {
                    for (int i = 0; i < reelsCount; i++) {
                        int temp = 0;
                        for (int j = 0; j < rowsCount; j++) {
                            if (missSymbol[k] == displaySymbols[j * reelsCount + i]) {
                                temp++;
                            } else {
                                break;
                            }
                        }
                        if (temp == rowsCount) {
                            count++;
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("computeMissSymbolHit() base spin result error!");
            }
        }//TODO gameCount>1
        return count;
    }

    private int computeHighSymbolHit(SlotGameLogicBean gameLogicBean, SlotConfigInfo configInfo) {
        int[] highSymbol = configInfo.getHighSymbol();
        int count = 0;
        if (configInfo.getPlayGameCount() == 1) {
            SlotSpinResult spinResult = gameLogicBean.getSlotSpinResult();
            if (spinResult != null) {
                int[] displaySymbols = spinResult.getSlotDisplaySymbols();
                for (int i = 0; i < displaySymbols.length; i++) {
                    for (int k = 0; k < highSymbol.length; k++) {
                        if (highSymbol[k] == displaySymbols[i]) {
                            count++;
                        }
                    }
                }
            }
        } else if (configInfo.getPlayGameCount() > 1) {
            //TODO
        }
        return count;
    }


    private void setTiersInfo(TiersInfo tiersInfo, SlotConfigInfo configInfo) {
        if (tiersInfo != null) {
            tiersInfo.setTotalWon(0L);
            tiersInfo.setTotalBet(0L);
            tiersInfo.setBaseWin(0L);
            tiersInfo.setFsWin(0L);
            tiersInfo.setBonusWin(0L);
            tiersInfo.setHighSymbolCount(0);
            tiersInfo.setStackCount(0);
            tiersInfo.setStackWildCount(0);
            tiersInfo.setHighAndStackSymbolCount(0);
            tiersInfo.setHorizontalSymbolCount(0);
            if (configInfo.isFsPay() && configInfo.getFsType() > 0) {
                tiersInfo.setFsTimes(0);
                tiersInfo.setFsHits(0);
                tiersInfo.setFsType(0);
            }
            if (configInfo.isBonusPay() && configInfo.getBonusType() > 0) {
                tiersInfo.setBonusHit(0);
                tiersInfo.setBonusType(0);
            }

        }
    }

    private void initFsBonusStdDev(SlotConfigInfo configInfo, BaseResultInfo resultInfo) {
        if (configInfo.isFsPay()) {
            double[] fsStdDev = new double[configInfo.getFsRtp().length];
            resultInfo.setFsStdDeviation(fsStdDev);
            List<Map<Long, Long>> fsPayWeightMapList = new ArrayList<Map<Long, Long>>();
            for (int i = 0; i < configInfo.getFsRtp().length; i++) {
                Map<Long, Long> fsPayWeightMap = new HashMap<Long, Long>();
                fsPayWeightMapList.add(fsPayWeightMap);
            }
            resultInfo.setFsPayWeightMapList(fsPayWeightMapList);
        }
        if (configInfo.isBonusPay()) {
            double[] bonusStdDev = new double[configInfo.getBonusRtp().length];
            List<Map<Long, Long>> bonusPayWeightMapList = new ArrayList<Map<Long, Long>>();
            for (int i = 0; i < configInfo.getBonusRtp().length; i++) {
                Map<Long, Long> bonusPayWeightMap = new HashMap<Long, Long>();
                bonusPayWeightMapList.add(bonusPayWeightMap);
            }
            resultInfo.setBonusStdDeviation(bonusStdDev);
            resultInfo.setBonusPayWeightMapList(bonusPayWeightMapList);
        }
    }

    private void initMaxWinCount(SlotConfigInfo configInfo, BaseResultInfo resultInfo, BaseSlotModel model) {
        int[] maxWin = configInfo.getMaxWin();
        if (maxWin != null && maxWin.length > 0) {
            int rowCount = BaseConstant.REELS_COUNT;
            int reelsCount = BaseConstant.ROWS_COUNT;
            if (model instanceof IBaseReelsDefaultConfig) {
                reelsCount = ((IBaseReelsDefaultConfig) model).getReelsCount();
                rowCount = ((IBaseReelsDefaultConfig) model).getRowsCount();
            }
            int highSymbolLen = configInfo.getPlayGameCount() * rowCount * reelsCount + 1;
            long[] maxWinCount = new long[maxWin.length + 1];
            long[] maxWinPay = new long[maxWin.length + 1];
            long[] maxWinBasePay = new long[maxWin.length + 1];
            long[] maxWinFsPay = new long[maxWin.length + 1];
            long[] maxWinBonusPay = new long[maxWin.length + 1];
            long[] maxPayTotalHit = new long[maxWin.length + 1];
            long[] maxPay = new long[maxWin.length + 1];
            long[][] highSymbol = new long[maxWin.length + 1][highSymbolLen];
            long[] baseTotalPay = new long[maxWin.length + 1];
            long[] fsTotalPay = new long[maxWin.length + 1];
            long[] bonusTotalPay = new long[maxWin.length + 1];
            long[][] horizontalSymbol = new long[maxWin.length + 1][rowCount + 1];

            if (configInfo.getMissSymbol() != null && configInfo.getMissSymbol().length > 0) {
                long[][] nearMissSymbol = new long[maxWin.length + 1][reelsCount + 1];
                long[][] nearMissWildSymbol = new long[maxWin.length + 1][reelsCount + 1];
                //screen size+ReelsCount
                long[][] allNearMissSymbol = new long[maxWin.length + 1][highSymbolLen];
                //long[][] allNearMissSymbol = new long[maxWin.length + 1][highSymbolLen + reelsCount];
                resultInfo.setNearMissSymbolHit(nearMissSymbol);
                resultInfo.setNearMissWildSymbolHit(nearMissWildSymbol);
                resultInfo.setAllNearMissSymbolHit(allNearMissSymbol);
            }
            resultInfo.setMaxWinCount(maxWinCount);
            resultInfo.setMaxWinTotalPay(maxWinPay);
            resultInfo.setBaseTotalMaxPay(maxWinBasePay);
            resultInfo.setFsTotalMaxPay(maxWinFsPay);
            resultInfo.setBonusTotalMaxPay(maxWinBonusPay);
            resultInfo.setMaxPayTotalHit(maxPayTotalHit);
            resultInfo.setScreenMaxPay(maxPay);
            resultInfo.setHighSymbolPerMaxPayHit(highSymbol);
            resultInfo.setBaseTotalPay(baseTotalPay);
            resultInfo.setFsTotalPay(fsTotalPay);
            resultInfo.setBonusTotalPay(bonusTotalPay);
            if (configInfo.isSumHorizontalSymbol()) {
                resultInfo.setHorizontalSymbolHit(horizontalSymbol);
                if (resultInfo.getAllNearMissSymbolHit() == null) {
                    long[][] allNearMissSymbol = new long[maxWin.length + 1][highSymbolLen];
                    resultInfo.setAllNearMissSymbolHit(allNearMissSymbol);
                }
            }

            //Win Tier Classification (Free Spin Pays)
            if (configInfo.isFsPay() && configInfo.getFsType() > 0) {
                long[][] fsEntries = new long[configInfo.getFsType()][maxWin.length + 1];
                long[][] fsTimes = new long[configInfo.getFsType()][maxWin.length + 1];
                long[][] fsHits = new long[configInfo.getFsType()][maxWin.length + 1];
                long[][] fsMaxPay = new long[configInfo.getFsType()][maxWin.length + 1];
                long[][] fsTotalWin = new long[configInfo.getFsType()][maxWin.length + 1];
                resultInfo.setFsEntries(fsEntries);
                resultInfo.setFsTypeTimes(fsTimes);
                resultInfo.setFsPerHit(fsHits);
                resultInfo.setFsTypeTotalWin(fsTotalWin);
                resultInfo.setFsMaxPay(fsMaxPay);
            }
            if (configInfo.isBonusPay() && configInfo.getBonusType() > 0) {
                long[][] bonusEntries = new long[configInfo.getBonusType()][maxWin.length + 1];
                long[][] bonusTimes = new long[configInfo.getBonusType()][maxWin.length + 1];
                long[][] bonusHits = new long[configInfo.getBonusType()][maxWin.length + 1];
                long[][] bonusMaxPay = new long[configInfo.getBonusType()][maxWin.length + 1];
                long[][] bonusTotalWin = new long[configInfo.getBonusType()][maxWin.length + 1];
                resultInfo.setBonusEntries(bonusEntries);
                resultInfo.setBonusTimes(bonusTimes);
                resultInfo.setBonusPerHit(bonusHits);
                resultInfo.setBonusTypeTotalWin(bonusTotalWin);
                resultInfo.setBonusMaxPay(bonusMaxPay);
            }

        }
    }

    private void initWinTierWriter(SlotConfigInfo configInfo) {
        String fileName1 = configInfo.getOutputPath() + HIGH_MISS_SYMBOL_FILE;
        configInfo.setHighMissFileName(fileName1);
        FileWriteUtil.createNewFile(fileName1);

        if (configInfo.isFsPay() || configInfo.isBonusPay()) {
            String fileName2 = configInfo.getOutputPath() + FS_BONUS_FILE;
            configInfo.setFsBonusFileName(fileName2);
            FileWriteUtil.createNewFile(fileName2);
        }
    }

    /**
     * @Author: jiangqx
     * @param: * @param configInfo
     * @Description: init achievement writer file
     * @Date: 9:22 2017/7/18
     * @return:
     */
    private void initAchievementWriter(SlotConfigInfo configInfo) {
        if (configInfo.getAchievementSymbol() != null && !"".equals(configInfo.getAchievementSymbol())) {
            String fileName = configInfo.getOutputPath() + ACHIEVEMENT_SYMBOL_FILE;
            configInfo.setAchievementFileName(fileName);
            FileWriteUtil.createNewFile(fileName);
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append("PlayIndex").append(BaseConstant.TAB_STR);
            for (int symbol : configInfo.getAchievementSymbol()) {
                strbHeader.append("Symbol").append(symbol).append(BaseConstant.TAB_STR);
                for (int pay : configInfo.getAchievementPay()) {
                    strbHeader.append("Pay").append(pay).append(BaseConstant.TAB_STR);
                }
                strbHeader.append("PayTotal").append(BaseConstant.TAB_STR);
            }
            FileWriteUtil.writeFileHeadInfo(fileName, strbHeader.toString());
        }
    }

    private boolean initAchievementSymbol(BaseSlotModel model, SlotConfigInfo configInfo, BaseResultInfo resultInfo) {
        if (configInfo.getAchievementSymbol() != null && configInfo.getAchievementSymbol().length > 0) {
            if (model instanceof IBaseReelsDefaultConfig) {
                int reelCount = ((IBaseReelsDefaultConfig) model).getReelsCount();
                for (int symbol : configInfo.getAchievementSymbol()) {
                    AchievementSymbol achievementSymbol = new AchievementSymbol(symbol);
                    List<AchievementPay> achievementPayList = new ArrayList<AchievementPay>();
                    for (int i = 0; i < reelCount; i++) {
                        AchievementPay achievementPay = new AchievementPay(i + 1);
                        achievementPayList.add(achievementPay);
                    }
                    achievementSymbol.setAchievementPayList(achievementPayList);
                    resultInfo.getBaseAchievementSymbol().add(achievementSymbol);
                }
            } else {
                throw new IllegalArgumentException("initAchievementSymbol() get reelsCount error");
            }
            return true;
        }
        return false;
    }

}
