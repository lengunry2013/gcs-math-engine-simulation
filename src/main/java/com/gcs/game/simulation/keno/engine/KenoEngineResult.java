package com.gcs.game.simulation.keno.engine;

import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.keno.model.BaseKenoModel;
import com.gcs.game.engine.keno.vo.KenoGameLogicBean;
import com.gcs.game.engine.keno.vo.KenoResult;
import com.gcs.game.exception.InvalidBetException;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.exception.InvalidPlayerInputException;
import com.gcs.game.simulation.keno.vo.KenoConfigInfo;
import com.gcs.game.simulation.keno.vo.KenoResultInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;

import java.util.*;

public class KenoEngineResult {

    public static final int[] BASE_3SETS_FS_TIMES = new int[]{5, 6, 8, 12};
    public static final int[] BASE_3SETS_MUL = new int[]{2, 3, 4, 5};
    private static final int[] FS_4SETS_MUL = new int[]{2, 3, 4, 10};

    public KenoEngineResult() {

    }

    public void spinResult(IGameEngine engine, BaseGameLogicBean gameLogicBean, BaseConfigInfo configInfo, BaseKenoModel kenoModel) throws InvalidGameStateException, InvalidBetException, InvalidPlayerInputException {
        KenoConfigInfo kenoConfigInfo = (KenoConfigInfo) configInfo;
        KenoGameLogicBean kenoGameLogicBean = (KenoGameLogicBean) gameLogicBean;
        KenoResultInfo resultInfo = new KenoResultInfo();
        long spinCount = 0L;
        long simulationCount = configInfo.getSimulationCount();
        int playTime = configInfo.getPlayTimesPerPlayer();
        long initCredit = configInfo.getInitCredit();
        resultInfo.setDenom(configInfo.getDenom());
        initKenoWinInfo(kenoGameLogicBean, resultInfo, kenoModel);
        KenoResult kenoResult;
        long totalWon;
        FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), StringUtil.getKenoHeadInfo(configInfo, kenoModel, resultInfo));
        for (int i = 0; i < simulationCount; i++) {
            //spin
            spinCount++;
            totalWon = 0L;
            List<Integer> selectNumbers = getSelectNumbers(kenoModel, kenoConfigInfo);
            Map gameLogicMap = new LinkedHashMap();
            gameLogicMap.put("lines", kenoConfigInfo.getLines());
            gameLogicMap.put("bet", kenoConfigInfo.getBet());
            gameLogicMap.put("denom", configInfo.getDenom());
            gameLogicMap.put("selectNumbers", selectNumbers);
            kenoGameLogicBean = (KenoGameLogicBean) engine.gameStart(kenoGameLogicBean, gameLogicMap, null, null);
            kenoResult = kenoGameLogicBean.getKenoResult();
            long totalBet = gameLogicBean.getSumBetCredit();
            initCredit -= totalBet;
            List<Integer> baseMulList = kenoResult.getWinMul();
            List<Integer> baseSetsMatchCount = kenoResult.getSetsMatchCount();
            List<Integer> baseFsTimes = kenoResult.getFsCountsList();
            if (baseMulList != null && !baseMulList.isEmpty()) {
                for (int winMul : baseMulList) {
                    if (winMul == BASE_3SETS_MUL[0]) {
                        resultInfo.getBaseMulHit().set(0, resultInfo.getBaseMulHit().get(0) + 1L);
                    }
                }
                for (int baseIndex = 1; baseIndex < BASE_3SETS_MUL.length; baseIndex++) {
                    if (baseMulList.contains(BASE_3SETS_MUL[baseIndex])) {
                        resultInfo.getBaseMulHit().set(baseIndex, resultInfo.getBaseMulHit().get(baseIndex) + 1L);
                    }
                }
            }
            for (int count : baseSetsMatchCount) {
                if (count == 4) {
                    resultInfo.setBaseAll4SpotsHit(resultInfo.getBaseAll4SpotsHit() + 1L);
                } else if (count == 3) {
                    resultInfo.setBase3Out4SpotsHit(resultInfo.getBase3Out4SpotsHit() + 1l);
                }
            }
            if (baseFsTimes != null && !baseFsTimes.isEmpty()) {
                for (int fsTime : baseFsTimes) {
                    for (int fsIndex = 0; fsIndex < BASE_3SETS_FS_TIMES.length; fsIndex++) {
                        if (fsTime == BASE_3SETS_FS_TIMES[fsIndex]) {
                            resultInfo.getBaseAll3SetTimesHit().set(fsIndex, resultInfo.getBaseAll3SetTimesHit().get(fsIndex) + 1);
                        }
                    }
                }
            }
            int mixHitMatchCount = kenoResult.getMixHitMatchCount();
            for (int index = 0; index < kenoModel.mixHitOnAll3Sets()[0].length; index++) {
                if (mixHitMatchCount == kenoModel.mixHitOnAll3Sets()[0][index]) {
                    resultInfo.getMixHit3SetCount().set(index, resultInfo.getMixHit3SetCount().get(index) + 1l);
                    break;
                } else if (index == kenoModel.mixHitOnAll3Sets()[0].length - 1 && mixHitMatchCount >= kenoModel.mixHitOnAll3Sets()[0][index]) {
                    resultInfo.getMixHit3SetCount().set(index, resultInfo.getMixHit3SetCount().get(index) + 1l);
                    break;
                }
            }
            long maxTotalPay = kenoModel.maxTotalPay();
            long winCredit = gameLogicBean.getSumWinCredit();
            int matchCount = kenoResult.getMatchCount();
            int selectIndex = kenoResult.getSelectNumbers().size() - 2;
            resultInfo.getPayTableHit().get(selectIndex).set(matchCount, resultInfo.getPayTableHit().get(selectIndex).get(matchCount) + 1l);
            resultInfo.getPayTableWin().get(selectIndex).set(matchCount, resultInfo.getPayTableWin().get(selectIndex).get(matchCount) + winCredit);
            if (gameLogicBean.getGamePlayStatus() == GameConstant.SLOT_GAME_STATUS_COMPLETE) {
                if (winCredit > 0) {
                    resultInfo.setBaseTotalHit(resultInfo.getBaseTotalHit() + 1);
                    resultInfo.setBaseTotalWin(resultInfo.getBaseTotalWin() + winCredit);
                }
            } else {
                resultInfo.setBaseTotalHit(resultInfo.getBaseTotalHit() + 1);
                if (winCredit > 0) {
                    resultInfo.setBaseTotalWin(resultInfo.getBaseTotalWin() + winCredit);
                }
                long fsCoinOut = 0L;
                long fsTotalTimes = 0L;
                while (true) {
                    if (kenoGameLogicBean.getGamePlayStatus() == GameConstant.KENO_GAME_STATUS_TRIGGER_FREESPIN) {
                        while (kenoGameLogicBean.getGamePlayStatus() == GameConstant.KENO_GAME_STATUS_TRIGGER_FREESPIN) {
                            PlayerInputInfo playerInput = new PlayerInputInfo();
                            playerInput.setRequestGameStatus(200);
                            kenoGameLogicBean = (KenoGameLogicBean) engine.gameProgress(gameLogicBean, gameLogicMap, playerInput, null, null, null);

                            KenoResult fsKenoResult = ((KenoGameLogicBean) gameLogicBean).getKenoFsResult().get(kenoGameLogicBean.getKenoFsResult().size() - 1);
                            long freespinWon = fsKenoResult.getKenoPay();
                            fsCoinOut += freespinWon;
                            fsTotalTimes++;

                            List<Integer> fsWinMul = fsKenoResult.getWinMul();
                            if (fsWinMul != null && !fsWinMul.isEmpty()) {
                                for (int winMul : fsWinMul) {
                                    if (winMul == FS_4SETS_MUL[0]) {
                                        resultInfo.getFsMulHit().set(0, resultInfo.getFsMulHit().get(0) + 1l);
                                    }
                                }
                                for (int mulIndex = 1; mulIndex < FS_4SETS_MUL.length; mulIndex++) {
                                    if (fsWinMul.contains(FS_4SETS_MUL[mulIndex])) {
                                        resultInfo.getFsMulHit().set(mulIndex, resultInfo.getFsMulHit().get(mulIndex) + 1l);
                                    }
                                }
                            }
                            List<Integer> fsSetsMatchCount = fsKenoResult.getSetsMatchCount();
                            int size = 0;
                            for (int count : fsSetsMatchCount) {
                                if (size == fsSetsMatchCount.size() - 1) {
                                    if (count == 3) {
                                        resultInfo.setFsAll3SpotsHit(resultInfo.getFsAll3SpotsHit() + 1l);
                                    } else if (count == 2) {
                                        resultInfo.setFs2Out3SpotsHit(resultInfo.getFs2Out3SpotsHit() + 1l);
                                    }
                                } else {
                                    if (count == 4) {
                                        resultInfo.setFsAll4SpotsHit(resultInfo.getFsAll4SpotsHit() + 1L);
                                    } else if (count == 3) {
                                        resultInfo.setFs3Out4SpotsHit(resultInfo.getFs3Out4SpotsHit() + 1l);
                                    }
                                }
                                size++;
                            }

                            mixHitMatchCount = fsKenoResult.getMixHitMatchCount();
                            for (int index = 0; index < kenoModel.mixHitOnAll4Sets()[0].length; index++) {
                                if (mixHitMatchCount == kenoModel.mixHitOnAll4Sets()[0][index]) {
                                    resultInfo.getMixHit4SetCount().set(index, resultInfo.getMixHit4SetCount().get(index) + 1l);
                                    break;
                                } else if (index == kenoModel.mixHitOnAll4Sets()[0].length - 1 && mixHitMatchCount >= kenoModel.mixHitOnAll4Sets()[0][index]) {
                                    resultInfo.getMixHit4SetCount().set(index, resultInfo.getMixHit4SetCount().get(index) + 1l);
                                    break;
                                }
                            }

                        }
                        if (fsTotalTimes > 0) {
                            resultInfo.setFsTotalHit(resultInfo.getFsTotalHit() + 1);
                            resultInfo.setFsTotalTimes(resultInfo.getFsTotalTimes() + fsTotalTimes);
                            resultInfo.setFsTotalWin(resultInfo.getFsTotalWin() + fsCoinOut);
                            //大于$800的话奖金只能是$800,所以fsTotalWin可能少于实际值
                            long fsTotalWin = 0l;
                            if (maxTotalPay > 0 && kenoGameLogicBean.getSumWinCredit() >= maxTotalPay) {
                                fsTotalWin = kenoGameLogicBean.getSumWinCredit() - kenoResult.getKenoPay();
                                resultInfo.setFsActualTotalWin(resultInfo.getFsActualTotalWin() + fsTotalWin);
                            } else {
                                resultInfo.setFsActualTotalWin(resultInfo.getFsActualTotalWin() + fsCoinOut);
                            }
                        }
                    } else if (kenoGameLogicBean.getGamePlayStatus() == GameConstant.GAME_STATUS_COMPLETE) {
                        break;
                    }
                }
            }
            totalWon += kenoGameLogicBean.getSumWinCredit();
            initCredit += totalWon;
            setBaseCommInfo(spinCount, initCredit, totalWon, kenoGameLogicBean, resultInfo);
            if (spinCount > 0 && spinCount % playTime == 0) {
                outResultInfo(kenoConfigInfo, resultInfo);
            }

        }
    }

    private List<Integer> getSelectNumbers(BaseKenoModel kenoModel, KenoConfigInfo kenoConfigInfo) {
        int[] selectCounts = kenoConfigInfo.getPicks();
        int countIndex = RandomUtil.getRandomInt(selectCounts.length);
        int count = selectCounts[countIndex];
        List<Integer> selectNumbers = kenoModel.getRandomNumbers(kenoModel.getAllRandomDigits(), count);
        return selectNumbers;
    }

    protected void outResultInfo(KenoConfigInfo kenoConfigInfo, KenoResultInfo resultInfo) {
        StringBuilder strContent = new StringBuilder();
        strContent.append(resultInfo.getSpinCount())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getLeftCredit())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getLines()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBet())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalBet()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getDenom()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalAmount())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalHit())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalCoinIn())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalCoinOut())
                .append(BaseConstant.TAB_STR);
        double hitRate = resultInfo.getTotalHit() * 1.0
                / resultInfo.getSpinCount();
        strContent.append(hitRate).append(BaseConstant.TAB_STR);
        double payBack = resultInfo.getTotalCoinOut() * 1.0
                / resultInfo.getTotalCoinIn();
        strContent.append(payBack).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBaseTotalHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBaseTotalWin()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFsTotalHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFsTotalTimes()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFsActualTotalWin()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFsTotalWin()).append(BaseConstant.TAB_STR);
        resultInfo.getBaseMulHit().forEach(baseMulHit ->
                strContent.append(baseMulHit).append(BaseConstant.TAB_STR));
        strContent.append(resultInfo.getBaseAll4SpotsHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBase3Out4SpotsHit()).append(BaseConstant.TAB_STR);
        resultInfo.getFsMulHit().forEach(fsMulHit ->
                strContent.append(fsMulHit).append(BaseConstant.TAB_STR));
        strContent.append(resultInfo.getFsAll4SpotsHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFs3Out4SpotsHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFsAll3SpotsHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFs2Out3SpotsHit()).append(BaseConstant.TAB_STR);
        resultInfo.getMixHit3SetCount().forEach(mixHit -> strContent.append(mixHit).append(BaseConstant.TAB_STR));
        resultInfo.getMixHit4SetCount().forEach(mixHit -> strContent.append(mixHit).append(BaseConstant.TAB_STR));
        resultInfo.getPayTableHit().forEach(payTableHit ->
                payTableHit.forEach(payHit -> strContent.append(payHit).append(BaseConstant.TAB_STR)));
        resultInfo.getPayTableWin().forEach(payTableWin ->
                payTableWin.forEach(payWin -> strContent.append(payWin).append(BaseConstant.TAB_STR)));
        resultInfo.getBaseAll3SetTimesHit().forEach(baseFsTimes -> strContent.append(baseFsTimes).append(BaseConstant.TAB_STR));
        FileWriteUtil.outputPrint(strContent.toString(), kenoConfigInfo.getOutputFileName(), kenoConfigInfo, 0);
    }

    protected void setBaseCommInfo(long spinCount, long initCredit, long totalWon, KenoGameLogicBean
            gameLogicBean, KenoResultInfo resultInfo) {
        resultInfo.setSpinCount(spinCount);
        resultInfo.setBet(gameLogicBean.getBet());
        resultInfo.setLines(gameLogicBean.getLines());
        resultInfo.setDenom(gameLogicBean.getDenom());
        resultInfo.setTotalBet(gameLogicBean.getSumBetCredit());
        resultInfo.setTotalCoinIn(resultInfo.getTotalCoinIn() + gameLogicBean.getSumBetCredit());
        if (totalWon > 0) {
            resultInfo.setTotalHit(resultInfo.getTotalHit() + 1);
        }
        resultInfo.setTotalCoinOut(resultInfo.getTotalCoinOut() + totalWon);
        resultInfo.setTotalAmount(totalWon);
        resultInfo.setLeftCredit(initCredit);
    }

    private void initKenoWinInfo(KenoGameLogicBean kenoGameLogicBean, KenoResultInfo resultInfo, BaseKenoModel kenoModel) {
        List<Long> baseMulList = new ArrayList<>();
        List<Long> base3SetTimesHit = new ArrayList<>();
        List<Long> fsMulList = new ArrayList<>();
        List<Long> mixHit3SetCount = new ArrayList<>();
        List<Long> mixHit4SetCount = new ArrayList<>();
        List<List<Long>> payTableHit = new ArrayList<>();
        List<List<Long>> payTableWin = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            baseMulList.add(0L);
            fsMulList.add(0L);
        }
        for (int i = 0; i < 4; i++) {
            base3SetTimesHit.add(0L);
        }
        for (int i = 0; i < 6; i++) {
            mixHit3SetCount.add(0L);
            mixHit4SetCount.add(0L);
        }
        long[][] payTable = kenoModel.getPayTable(kenoGameLogicBean);
        for (int i = 0; i < payTable.length; i++) {
            List<Long> tempHitList = new ArrayList<>();
            List<Long> tempWinList = new ArrayList<>();
            for (int j = 0; j < payTable[i].length; j++) {
                tempHitList.add(0L);
                tempWinList.add(0L);
            }
            payTableHit.add(tempHitList);
            payTableWin.add(tempWinList);
        }
        resultInfo.setBaseMulHit(baseMulList);
        resultInfo.setBaseAll3SetTimesHit(base3SetTimesHit);
        resultInfo.setFsMulHit(fsMulList);
        resultInfo.setMixHit3SetCount(mixHit3SetCount);
        resultInfo.setMixHit4SetCount(mixHit4SetCount);
        resultInfo.setPayTableHit(payTableHit);
        resultInfo.setPayTableWin(payTableWin);
    }
}
