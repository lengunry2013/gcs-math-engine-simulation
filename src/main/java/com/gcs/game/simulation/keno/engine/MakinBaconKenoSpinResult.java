package com.gcs.game.simulation.keno.engine;

import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.keno.model.BaseKenoModel;
import com.gcs.game.engine.keno.utils.numbers.KenoShapeDrawUtil;
import com.gcs.game.engine.keno.vo.KenoGameLogicBean;
import com.gcs.game.engine.keno.vo.KenoResult;
import com.gcs.game.engine.math.model20260715.Model20260715;
import com.gcs.game.exception.InvalidBetException;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.exception.InvalidPlayerInputException;
import com.gcs.game.simulation.keno.vo.KenoConfigInfo;
import com.gcs.game.simulation.keno.vo.KenoResultInfo;
import com.gcs.game.simulation.keno.vo.MakinBaconKenoResultInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MakinBaconKenoSpinResult extends KenoEngineResult {

    public static final long[] BET_LEVEL = new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    public MakinBaconKenoSpinResult() {

    }

    public void spinResult(IGameEngine engine, BaseGameLogicBean gameLogicBean, BaseConfigInfo configInfo, BaseKenoModel kenoModel) throws InvalidGameStateException, InvalidBetException, InvalidPlayerInputException {
        KenoConfigInfo kenoConfigInfo = (KenoConfigInfo) configInfo;
        KenoGameLogicBean kenoGameLogicBean = (KenoGameLogicBean) gameLogicBean;
        MakinBaconKenoResultInfo resultInfo = new MakinBaconKenoResultInfo();
        long spinCount = 0L;
        long simulationCount = configInfo.getSimulationCount();
        int playTime = configInfo.getPlayTimesPerPlayer();
        long initCredit = configInfo.getInitCredit();
        resultInfo.setDenom(configInfo.getDenom());
        initKenoWinInfo(kenoGameLogicBean, resultInfo, kenoModel);
        KenoResult kenoResult;
        long totalWon;
        FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), getMakinBaconKenoHeadInfo(configInfo, kenoModel, resultInfo));
        for (int i = 0; i < simulationCount; i++) {
            //spin
            spinCount++;
            totalWon = 0L;
            List<List<Integer>> additionsSetsNumbers = getAdditionSetsNumber(kenoModel, kenoConfigInfo);
            List<Integer> selectNumbers = getSelectNumbers(kenoModel, kenoConfigInfo, additionsSetsNumbers);
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
            gameLogicMap.put("additionsSetsNumbers", additionsSetsNumbers);
            kenoGameLogicBean = (KenoGameLogicBean) engine.gameStart(kenoGameLogicBean, gameLogicMap, null, null);
            kenoResult = kenoGameLogicBean.getKenoResult();
            long totalBet = gameLogicBean.getSumBetCredit();
            initCredit -= totalBet;
            //List<Integer> baseMulList = kenoResult.getWinMul();
            List<Integer> baseFsTimes = kenoResult.getFsCountsList();
            List<Integer> baseSetsMatchCount = kenoResult.getSetsMatchCount();
            int setAMatchCount = baseSetsMatchCount.get(0);
            int setBMatchCount = baseSetsMatchCount.get(1);
            int setCMatchCount = baseSetsMatchCount.get(2);
            int baseTriggerFsTimes = 0;
            if (setAMatchCount > 0) {
                resultInfo.getBaseSetAHit()[setAMatchCount - 1]++;
                if (setAMatchCount >= 6) {
                    int baseFsTime = baseFsTimes.get(0);
                    int index = 0;
                    for (int j = 0; j < Model20260715.FS_SETA_TIMES[0].length; j++) {
                        if (baseFsTime == Model20260715.FS_SETA_TIMES[0][j]) {
                            index = j;
                            break;
                        }
                    }
                    baseTriggerFsTimes += baseFsTime;
                    resultInfo.getBaseSetA6SpotHit()[index]++;
                }
                if (setAMatchCount == 5) {
                    int baseFsTime = baseFsTimes.get(0);
                    int index = 0;
                    for (int j = 0; j < Model20260715.FS_SETA_TIMES[1].length; j++) {
                        if (baseFsTime == Model20260715.FS_SETA_TIMES[1][j]) {
                            index = j;
                            break;
                        }
                    }
                    baseTriggerFsTimes += baseFsTime;
                    resultInfo.getBaseSetA5SpotHit()[index]++;
                }
            }
            if (setBMatchCount > 0) {
                resultInfo.getBaseSetBHit()[setBMatchCount - 1]++;
                if (setBMatchCount == 5) {
                    int baseFsTime = 0;
                    if (baseTriggerFsTimes > 0 && baseFsTimes.size() >= 2) {
                        baseFsTime = baseFsTimes.get(1);
                    } else {
                        baseFsTime = baseFsTimes.get(0);
                    }
                    int index = 0;
                    for (int j = 0; j < Model20260715.FS_SETB_TIMES.length; j++) {
                        if (baseFsTime == Model20260715.FS_SETB_TIMES[j]) {
                            index = j;
                            break;
                        }
                    }
                    resultInfo.getBaseSetB5SpotHit()[index]++;
                }
            }
            if (setCMatchCount > 0) {
                resultInfo.getBaseSetCHit()[setCMatchCount - 1]++;
                if (setCMatchCount == 4) {
                    int baseFsTime = baseFsTimes.get(baseFsTimes.size() - 1);
                    int index = 0;
                    for (int j = 0; j < Model20260715.FS_SETC_TIMES.length; j++) {
                        if (baseFsTime == Model20260715.FS_SETC_TIMES[j]) {
                            index = j;
                            break;
                        }
                    }
                    resultInfo.getBaseSetC4SpotHit()[index]++;
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

                            KenoResult fsKenoResult = ((KenoGameLogicBean) gameLogicBean).getKenoFsResult().get(kenoGameLogicBean.getKenoFsResult().size() - 1);
                            long freespinWon = fsKenoResult.getKenoPay();
                            fsCoinOut += freespinWon;
                            fsTotalTimes++;

                            List<Integer> fsWinMul = fsKenoResult.getWinMul();
                            List<Integer> fsSetsMatchCount = fsKenoResult.getSetsMatchCount();
                            int fsSetAMatchCount = fsSetsMatchCount.get(0);
                            int fsSetBMatchCount = fsSetsMatchCount.get(1);
                            int fsSetCMatchCount = fsSetsMatchCount.get(2);
                            int fsSetDMatchCount = fsSetsMatchCount.get(3);
                            if (fsSetAMatchCount > 0) {
                                resultInfo.getFsSetAHit()[fsSetAMatchCount - 1]++;
                            }
                            if (fsSetBMatchCount > 0) {
                                resultInfo.getFsSetBHit()[fsSetBMatchCount - 1]++;
                            }
                            if (fsSetCMatchCount > 0) {
                                resultInfo.getFsSetCHit()[fsSetCMatchCount - 1]++;
                            }
                            if (fsSetDMatchCount > 0) {
                                resultInfo.getFsSetDHit()[fsSetDMatchCount - 1]++;
                            }
                            List<Integer> extraNumbers = fsKenoResult.getExtraDrawNumbers();
                            if (extraNumbers != null && !extraNumbers.isEmpty()) {
                                int size = extraNumbers.size();
                                resultInfo.getFsExtraDrawHit()[size - 1]++;
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

    private String getMakinBaconKenoHeadInfo(BaseConfigInfo configInfo, BaseKenoModel kenoModel, MakinBaconKenoResultInfo resultInfo) {
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
        for (int i = 0; i < resultInfo.getBaseSetAHit().length; i++) {
            strbHeader.append("Base SetA Count").append(i + 1).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getBaseSetBHit().length; i++) {
            strbHeader.append("Base SetB Count").append(i + 1).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getBaseSetCHit().length; i++) {
            strbHeader.append("Base SetC Count").append(i + 1).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getBaseSetA6SpotHit().length; i++) {
            strbHeader.append("Base SetA FsTime").append(Model20260715.FS_SETA_TIMES[0][i]).append(" Hit").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getBaseSetA5SpotHit().length; i++) {
            strbHeader.append("Base SetA FsTime").append(Model20260715.FS_SETA_TIMES[1][i]).append(" Hit").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getBaseSetB5SpotHit().length; i++) {
            strbHeader.append("Base SetB FsTime").append(Model20260715.FS_SETB_TIMES[i]).append(" Hit").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getBaseSetC4SpotHit().length; i++) {
            strbHeader.append("Base SetC FsTime").append(Model20260715.FS_SETC_TIMES[i]).append(" Hit").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getFsSetAHit().length; i++) {
            strbHeader.append("Fs SetA Count").append(i + 1).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getFsSetBHit().length; i++) {
            strbHeader.append("Fs SetB Count").append(i + 1).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getFsSetCHit().length; i++) {
            strbHeader.append("Fs SetC Count").append(i + 1).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getFsSetDHit().length; i++) {
            strbHeader.append("Fs SetD Count").append(i + 1).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < resultInfo.getFsExtraDrawHit().length; i++) {
            strbHeader.append("Fs ExtraDraw Count").append(i + 1).append(BaseConstant.TAB_STR);
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

    private List<List<Integer>> getAdditionSetsNumber(BaseKenoModel kenoModel, KenoConfigInfo kenoConfigInfo) {
        int[] allNumbers = kenoModel.getAllRandomDigits();
        List<Integer> leftNumbers = com.gcs.game.utils.StringUtil.IntegerArrayToList(allNumbers);
        List<Integer> setANumbers = KenoShapeDrawUtil.drawShapeA(leftNumbers);
        leftNumbers.removeAll(setANumbers);
        List<Integer> setBNumbers = KenoShapeDrawUtil.drawShapeB(leftNumbers);
        leftNumbers.removeAll(setBNumbers);
        List<Integer> setCNumbers = KenoShapeDrawUtil.drawShapeC(leftNumbers);
        leftNumbers.removeAll(setCNumbers);
        List<List<Integer>> additionSetsNumbers = new ArrayList<>();
        additionSetsNumbers.add(setANumbers);
        additionSetsNumbers.add(setBNumbers);
        additionSetsNumbers.add(setCNumbers);
        return additionSetsNumbers;
    }

    private List<Integer> getSelectNumbers(BaseKenoModel kenoModel, KenoConfigInfo kenoConfigInfo, List<List<Integer>> additionsSetsNumbers) {
        int[] selectCounts = kenoConfigInfo.getPicks();
        int countIndex = RandomUtil.getRandomInt(selectCounts.length);
        int count = selectCounts[countIndex];
        int[] allNumbers = kenoModel.getAllRandomDigits();
        List<Integer> leftNumbers = com.gcs.game.utils.StringUtil.IntegerArrayToList(allNumbers);
        for (List<Integer> additionNumbers : additionsSetsNumbers) {
            leftNumbers.removeAll(additionNumbers);
        }
        return kenoModel.getRandomNumbers(StringUtil.changeListToIntegerArray(leftNumbers), count);
    }

    protected void outResultInfo(KenoConfigInfo kenoConfigInfo, MakinBaconKenoResultInfo resultInfo) {
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
        for (long setAHit : resultInfo.getBaseSetAHit()) {
            strContent.append(setAHit).append(BaseConstant.TAB_STR);
        }
        for (long setBHit : resultInfo.getBaseSetBHit()) {
            strContent.append(setBHit).append(BaseConstant.TAB_STR);
        }
        for (long setCHit : resultInfo.getBaseSetCHit()) {
            strContent.append(setCHit).append(BaseConstant.TAB_STR);
        }
        for (long setA6SpotHit : resultInfo.getBaseSetA6SpotHit()) {
            strContent.append(setA6SpotHit).append(BaseConstant.TAB_STR);
        }
        for (long setA5SpotHit : resultInfo.getBaseSetA5SpotHit()) {
            strContent.append(setA5SpotHit).append(BaseConstant.TAB_STR);
        }
        for (long setB5SpotHit : resultInfo.getBaseSetB5SpotHit()) {
            strContent.append(setB5SpotHit).append(BaseConstant.TAB_STR);
        }
        for (long setC4SpotHit : resultInfo.getBaseSetC4SpotHit()) {
            strContent.append(setC4SpotHit).append(BaseConstant.TAB_STR);
        }
        for (long fsSetAHit : resultInfo.getFsSetAHit()) {
            strContent.append(fsSetAHit).append(BaseConstant.TAB_STR);
        }
        for (long fsSetBHit : resultInfo.getFsSetBHit()) {
            strContent.append(fsSetBHit).append(BaseConstant.TAB_STR);
        }
        for (long fsSetCHit : resultInfo.getFsSetCHit()) {
            strContent.append(fsSetCHit).append(BaseConstant.TAB_STR);
        }
        for (long fsSetDHit : resultInfo.getFsSetDHit()) {
            strContent.append(fsSetDHit).append(BaseConstant.TAB_STR);
        }
        for (long fsExtraHit : resultInfo.getFsExtraDrawHit()) {
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
