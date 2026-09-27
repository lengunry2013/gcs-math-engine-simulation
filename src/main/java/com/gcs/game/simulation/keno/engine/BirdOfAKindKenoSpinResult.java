package com.gcs.game.simulation.keno.engine;

import cn.hutool.core.util.ObjectUtil;
import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.keno.model.BaseKenoModel;
import com.gcs.game.engine.keno.vo.KenoGameLogicBean;
import com.gcs.game.engine.math.model20260923.Model20260923;
import com.gcs.game.engine.math.model20260923.Model20260923KenoResult;
import com.gcs.game.exception.InvalidBetException;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.exception.InvalidPlayerInputException;
import com.gcs.game.simulation.keno.vo.BirdOfAKindKenoResultInfo;
import com.gcs.game.simulation.keno.vo.KenoConfigInfo;
import com.gcs.game.simulation.keno.vo.KenoResultInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BirdOfAKindKenoSpinResult extends KenoEngineResult {

    public static final long[] BET_LEVEL = new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    public BirdOfAKindKenoSpinResult() {

    }

    public void spinResult(IGameEngine engine, BaseGameLogicBean gameLogicBean, BaseConfigInfo configInfo, BaseKenoModel kenoModel) throws InvalidGameStateException, InvalidBetException, InvalidPlayerInputException {
        KenoConfigInfo kenoConfigInfo = (KenoConfigInfo) configInfo;
        KenoGameLogicBean kenoGameLogicBean = (KenoGameLogicBean) gameLogicBean;
        BirdOfAKindKenoResultInfo resultInfo = new BirdOfAKindKenoResultInfo();
        long spinCount = 0L;
        long simulationCount = configInfo.getSimulationCount();
        int playTime = configInfo.getPlayTimesPerPlayer();
        long initCredit = configInfo.getInitCredit();
        resultInfo.setDenom(configInfo.getDenom());
        initKenoWinInfo(kenoGameLogicBean, resultInfo, kenoModel);
        Model20260923KenoResult kenoResult;
        long totalWon;
        FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), getBirdOfAKindKenoHeadInfo(configInfo, kenoModel, resultInfo));
        for (int i = 0; i < simulationCount; i++) {
            //spin
            spinCount++;
            totalWon = 0L;
            List<Integer> selectNumbers = getSelectNumbers(kenoModel, kenoConfigInfo);
            Map gameLogicMap = new LinkedHashMap();
            gameLogicMap.put("lines", kenoConfigInfo.getLines());
            if (kenoConfigInfo.isRandomBet()) {
                int betIndex = RandomUtil.getRandomInt(BET_LEVEL.length);
                gameLogicMap.put("bet", BET_LEVEL[betIndex]);
            } else {
                gameLogicMap.put("bet", kenoConfigInfo.getBet());
            }
            gameLogicMap.put("denom", configInfo.getDenom());
            gameLogicMap.put("selectNumbers", selectNumbers);
            kenoGameLogicBean = (KenoGameLogicBean) engine.gameStart(kenoGameLogicBean, gameLogicMap, null, null);
            kenoResult = (Model20260923KenoResult) kenoGameLogicBean.getKenoResult();
            long totalBet = gameLogicBean.getSumBetCredit();
            initCredit -= totalBet;
            int pairsType = kenoResult.getPairsType();
            resultInfo.getBasePairsHit()[pairsType]++;
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
                if (winCredit > 0) {
                    resultInfo.setBaseTotalHit(resultInfo.getBaseTotalHit() + 1);
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

                            Model20260923KenoResult fsKenoResult = (Model20260923KenoResult) ((KenoGameLogicBean) gameLogicBean).getKenoFsResult().get(kenoGameLogicBean.getKenoFsResult().size() - 1);
                            long freespinWon = fsKenoResult.getKenoPay();
                            fsCoinOut += freespinWon;
                            fsTotalTimes++;

                            int fsPairsType = fsKenoResult.getPairsType();
                            resultInfo.getFsPairsHit()[fsPairsType]++;
                            List<Integer> fsWinMuls = fsKenoResult.getWinMul();
                            if (ObjectUtil.isNotEmpty(fsWinMuls)) {
                                int fsMul = fsWinMuls.get(0);
                                for (int j = 0; j < Model20260923.FS_MUL.length; j++) {
                                    if (fsMul == Model20260923.FS_MUL[j]) {
                                        resultInfo.getFsMultipliersHit()[j]++;
                                        break;
                                    }
                                }
                            }

                            List<Integer> fsCounts = fsKenoResult.getFsCountsList();
                            if (ObjectUtil.isNotEmpty(fsCounts)) {
                                int fsCount = fsCounts.get(0);
                                int[] fsExtraTimes = Model20260923.FS_EXTRA_TIMES[0];
                                for (int j = 0; j < fsExtraTimes.length; j++) {
                                    if (fsCount == fsExtraTimes[j]) {
                                        resultInfo.getFsExtraHit()[j]++;
                                        break;
                                    }
                                }
                            }
                            List<Integer> extraNumbers = fsKenoResult.getExtraDrawNumbers();
                            if (extraNumbers != null && !extraNumbers.isEmpty()) {
                                int size = extraNumbers.size();
                                for (int j = 0; j < Model20260923.FS_EXTRA_DRAW.length; j++) {
                                    if (size == Model20260923.FS_EXTRA_DRAW[j]) {
                                        resultInfo.getFsExtraDrawHit()[j]++;
                                        break;
                                    }
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

    private String getBirdOfAKindKenoHeadInfo(BaseConfigInfo configInfo, BaseKenoModel kenoModel, BirdOfAKindKenoResultInfo resultInfo) {
        StringBuilder strbHeader = new StringBuilder();
        strbHeader.append("Num of Spin").append(BaseConstant.TAB_STR);
        strbHeader.append("Left Credit").append(BaseConstant.TAB_STR);
        strbHeader.append("lines").append(BaseConstant.TAB_STR);
        strbHeader.append("bet").append(BaseConstant.TAB_STR);
        strbHeader.append("Total Bet").append(BaseConstant.TAB_STR);
        strbHeader.append("Denom").append(BaseConstant.TAB_STR);
        strbHeader.append("Total Amount").append(BaseConstant.TAB_STR);
        strbHeader.append("Total Hit").append(BaseConstant.TAB_STR);
        strbHeader.append("TotalCoinIn").append(BaseConstant.TAB_STR);
        strbHeader.append("TotalCoinOut").append(BaseConstant.TAB_STR);
        strbHeader.append("Hit Rate").append(BaseConstant.TAB_STR);
        strbHeader.append("Payback").append(BaseConstant.TAB_STR);
        strbHeader.append("BaseTotalHit").append(BaseConstant.TAB_STR);
        strbHeader.append("BaseTotalWin").append(BaseConstant.TAB_STR);
        strbHeader.append("Fs Total Hit").append(BaseConstant.TAB_STR);
        strbHeader.append("Fs Total Times").append(BaseConstant.TAB_STR);
        //strbHeader.append("Fs Actual Total Win").append(BaseConstant.TAB_STR);
        strbHeader.append("Fs Total Win").append(BaseConstant.TAB_STR);
        for (int i = 0; i < resultInfo.getBasePairsHit().length; i++) {
            strbHeader.append("Base Pairs Type").append(i + 1).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getFsPairsHit().length; i++) {
            strbHeader.append("Fs Pairs Type").append(i + 1).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getFsExtraDrawHit().length; i++) {
            strbHeader.append("Fs ExtraDraw").append(Model20260923.FS_EXTRA_DRAW[i]).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getFsMultipliersHit().length; i++) {
            strbHeader.append("Fs Mul").append(Model20260923.FS_MUL[i]).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getFsExtraHit().length; i++) {
            strbHeader.append("Fs ExTra Times").append(Model20260923.FS_EXTRA_TIMES[0][i]).append(" Hit").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getPayTableHit().size(); i++) {
            List<Long> payHit = resultInfo.getPayTableHit().get(i);
            for (int j = 0; j < payHit.size(); j++) {
                strbHeader.append("Pick").append(i + 2).append(" Match").append(j).append(" Hit").append(BaseConstant.TAB_STR);
            }
        }
        for (int i = 0; i < resultInfo.getPayTableWin().size(); i++) {
            List<Long> payWin = resultInfo.getPayTableWin().get(i);
            for (int j = 0; j < payWin.size(); j++) {
                strbHeader.append("Pick").append(i + 2).append(" Match").append(j).append(" Win").append(BaseConstant.TAB_STR);
            }
        }
        return strbHeader.toString();
    }

    protected void outResultInfo(KenoConfigInfo kenoConfigInfo, BirdOfAKindKenoResultInfo resultInfo) {
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
        //strContent.append(resultInfo.getFsActualTotalWin()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFsTotalWin()).append(BaseConstant.TAB_STR);
        for (long pairsHit : resultInfo.getBasePairsHit()) {
            strContent.append(pairsHit).append(BaseConstant.TAB_STR);
        }
        for (long fsPairsHit : resultInfo.getFsPairsHit()) {
            strContent.append(fsPairsHit).append(BaseConstant.TAB_STR);
        }

        for (long extraDrawHit : resultInfo.getFsExtraDrawHit()) {
            strContent.append(extraDrawHit).append(BaseConstant.TAB_STR);
        }
        for (long fsMulHit : resultInfo.getFsMultipliersHit()) {
            strContent.append(fsMulHit).append(BaseConstant.TAB_STR);
        }
        for (long fsExtraHit : resultInfo.getFsExtraHit()) {
            strContent.append(fsExtraHit).append(BaseConstant.TAB_STR);
        }
        resultInfo.getPayTableHit().forEach(payTableHit ->
                payTableHit.forEach(payHit -> strContent.append(payHit).append(BaseConstant.TAB_STR)));
        resultInfo.getPayTableWin().forEach(payTableWin ->
                payTableWin.forEach(payWin -> strContent.append(payWin).append(BaseConstant.TAB_STR)));
        FileWriteUtil.outputPrint(strContent.toString(), kenoConfigInfo.getOutputFileName(), kenoConfigInfo, 0);
    }

    private void initKenoWinInfo(KenoGameLogicBean kenoGameLogicBean, KenoResultInfo resultInfo, BaseKenoModel kenoModel) {

        List<List<Long>> payTableHit = new ArrayList<>();
        List<List<Long>> payTableWin = new ArrayList<>();
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
        resultInfo.setPayTableHit(payTableHit);
        resultInfo.setPayTableWin(payTableWin);
    }
}
