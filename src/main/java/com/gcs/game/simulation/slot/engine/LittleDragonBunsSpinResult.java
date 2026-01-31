package com.gcs.game.simulation.slot.engine;


import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.engine.slots.vo.SlotBonusResult;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.engine.slots.vo.SlotSpinResult;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.exception.InvalidPlayerInputException;
import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import com.gcs.game.simulation.slot.common.vo.SymbolResultInfo;
import com.gcs.game.simulation.slot.vo.LittleDragonBunsResultInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model1260130.Model1260130Test;
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
public class LittleDragonBunsSpinResult {

    public static final String FS_FILE = "fsResult1.txt";

    public LittleDragonBunsSpinResult() {

    }

    public void cycleSpinForLittleDragonBuns(IGameEngine engine, BaseGameLogicBean baseGameLogicBean, BaseConfigInfo configInfo, BaseSlotModel baseSlotModel) {
        try {
            long spinCount = 0L;
            long simulationCount = configInfo.getSimulationCount();
            int playTime = configInfo.getPlayTimesPerPlayer();
            long initCredit = configInfo.getInitCredit();
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            SlotGameLogicBean gameLogicBean = (SlotGameLogicBean) baseGameLogicBean;
            Model1260130Test model = (Model1260130Test) baseSlotModel;
            LittleDragonBunsResultInfo resultInfo = new LittleDragonBunsResultInfo();
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

                long winCredit = gameLogicBean.getSumWinCredit();
                totalWon += winCredit;
                int baseMul = gameLogicBean.getSlotSpinResult().getBaseGameMul();
                resultInfo.getMysteryMulHit()[baseMul - 1]++;
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
                    long fsCoinOut = 0L;
                    long fsTotalTimes = 0L;
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

                                computeFsMul(fsSpinResult, resultInfo, freespinWon);

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
                            }
                        } else if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_TRIGGER_BONUS) {
                            long bonusWin = computePickBonus(engine, gameLogicBean, gameLogicMap, resultInfo, slotConfigInfo);
                            totalWon += bonusWin;
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
            log.error("cycleSpinForLittleDragonBuns run exception", e);
        }

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

    protected long computePickBonus(IGameEngine engine, SlotGameLogicBean gameLogicBean, Map gameLogicMap, BaseResultInfo resultInfo, SlotConfigInfo configInfo) throws InvalidGameStateException, InvalidPlayerInputException {
        PlayerInputInfo playerInput = new PlayerInputInfo();
        playerInput.setRequestGameStatus(500);
        long bonusWon = 0L;
        //bonusChoice random Index
        int bonusChoiceIndex = configInfo.getChoiceFsOrBonusIndex();
        if (configInfo.isRandomBonusChoice() && bonusChoiceIndex > 0) {
            bonusChoiceIndex = RandomUtil.getRandomInt(bonusChoiceIndex);
        }

        for (int pick = 0; pick < 100; pick++) {
            if (pick > 0) {
                int[] picks = GameEngineCompute.initArray(pick, bonusChoiceIndex);
                playerInput.setBonusPickInfos(picks);
            }
            gameLogicBean = (SlotGameLogicBean) engine.gameProgress(gameLogicBean, gameLogicMap, playerInput, null, null, null);

            SlotBonusResult baseBonusResult = gameLogicBean.getSlotBonusResult();
            if (baseBonusResult.getBonusPlayStatus() == 1000) {
                bonusWon = baseBonusResult.getTotalPay();
                if (bonusWon > 0) {
                    resultInfo.setBonusTotalHit(resultInfo.getBonusTotalHit() + 1);
                    resultInfo.setBonusTotalWin(resultInfo.getBonusTotalWin() + bonusWon);
                }
                break;
            }
        }
        return bonusWon;
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

    private void computeFsMul(SlotSpinResult fsSpinResult, LittleDragonBunsResultInfo resultInfo, long freespinWon) {
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
        return new int[]{2, 3, 4, 5, 6, 7, 8,};
    }

    protected int[] getScatterSymbol() {
        return new int[]{12};
    }

    private void outResultInfo(SlotConfigInfo configInfo, LittleDragonBunsResultInfo resultInfo) {

        if (resultInfo.getSpinCount() == configInfo.getPlayTimesPerPlayer()) {
            StringBuilder strbHeader = new StringBuilder();
            strbHeader.append(StringUtil.getCommonHeaderInfo(resultInfo));
            strbHeader.append(StringUtil.getBonusHeaderInfo(resultInfo));
            for (int i = 0; i < resultInfo.getMysteryMulHit().length; i++) {
                strbHeader.append("Base Mystery Mul").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
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
            strbHeader.append(StringUtil.getPayTableHeaderInfo(resultInfo));
            FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), strbHeader.toString());
        }
        StringBuilder strContent = new StringBuilder();
        strContent.append(StringUtil.getBaseResultInfo(resultInfo));
        strContent.append(StringUtil.getBonusResultInfo(resultInfo));
        for (long mysteryMulHit : resultInfo.getMysteryMulHit()) {
            strContent.append(mysteryMulHit).append(BaseConstant.TAB_STR);
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
