/*
 * StringUtil.java
 * @author:jiangqx
 * History:
 *   date              name      Description
 *   Feb 13, 2017        jiangqx      create
 */

package com.gcs.game.simulation.util;

import com.gcs.game.engine.blackJack.model.BaseBlackJackModel;
import com.gcs.game.engine.keno.model.BaseKenoModel;
import com.gcs.game.engine.poker.model.BasePokerModel;
import com.gcs.game.simulation.blackJack.vo.BlackJackConfigInfo;
import com.gcs.game.simulation.keno.engine.KenoEngineResult;
import com.gcs.game.simulation.keno.vo.KenoResultInfo;
import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import com.gcs.game.simulation.slot.common.vo.SymbolResultInfo;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model6060630.Model6060630Test;
import com.gcs.game.testengine.math.model6080630.Model6080630Test;
import com.gcs.game.testengine.math.modelGCBJ00101.ModelGCBJ00101Test;
import com.gcs.game.testengine.math.modelGCBJ00102.ModelGCBJ00102Test;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * .
 *
 * @author jiangqx Feb 13, 2017
 * @version 1.0
 */
@Slf4j
public class StringUtil {

    public static String getBlackJackHeadInfo(BaseConfigInfo configInfo, BaseBlackJackModel blackJackModel) {
        BlackJackConfigInfo blackJackConfigInfo = null;
        if (configInfo instanceof BlackJackConfigInfo) {
            blackJackConfigInfo = (BlackJackConfigInfo) configInfo;
        }
        StringBuilder strbHeader = new StringBuilder();
        strbHeader.append("Num of Spin").append(BaseConstant.TAB_STR);
        strbHeader.append("Left Credit").append(BaseConstant.TAB_STR);
        strbHeader.append("Hand Count").append(BaseConstant.TAB_STR);
        strbHeader.append("Total Bet").append(BaseConstant.TAB_STR);
        strbHeader.append("Denom").append(BaseConstant.TAB_STR);
        strbHeader.append("Total Amount").append(BaseConstant.TAB_STR);
        strbHeader.append("Total Hit").append(BaseConstant.TAB_STR);
        strbHeader.append("TotalCoinIn").append(BaseConstant.TAB_STR);
        strbHeader.append("TotalCoinOut").append(BaseConstant.TAB_STR);
        strbHeader.append("Hit Rate").append(BaseConstant.TAB_STR);
        strbHeader.append("Payback").append(BaseConstant.TAB_STR);
        for (int i = 0; i < blackJackConfigInfo.getHandCount(); i++) {
            strbHeader.append("Hand").append(i + 1).append(" BetWin Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("Hand").append(i + 1).append(" BetWin Pay").append(BaseConstant.TAB_STR);
            strbHeader.append("Hand").append(i + 1).append(" JackpotWin Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("Hand").append(i + 1).append(" JackpotWin Pay").append(BaseConstant.TAB_STR);
            strbHeader.append("Hand").append(i + 1).append(" SplitWin Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("Hand").append(i + 1).append(" SplitWin Pay").append(BaseConstant.TAB_STR);
            strbHeader.append("Hand").append(i + 1).append(" InsuranceWin Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("Hand").append(i + 1).append(" InsuranceWin Pay").append(BaseConstant.TAB_STR);
        }
        if (blackJackModel instanceof ModelGCBJ00101Test) {
            strbHeader.append("Jackpot Grand Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("Jackpot Minor Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("Jackpot Mini Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("No Pay Hit").append(BaseConstant.TAB_STR);
        } else if (blackJackModel instanceof ModelGCBJ00102Test) {
            strbHeader.append("1 ACE-IST CARD Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("2 UNSUITED ACES Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("2 SUITED ACES Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("3 UNSUITED ACES Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("4 UNSUITED ACES Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("3 SUITED ACES Hit").append(BaseConstant.TAB_STR);
            strbHeader.append("4 SUITED ACES Hit").append(BaseConstant.TAB_STR);
        }
        return strbHeader.toString();
    }

    public static String getPokersHeadInfo(BaseConfigInfo configInfo, BasePokerModel pokerModel) {
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
        strbHeader.append("HandPokers").append(BaseConstant.TAB_STR);
        strbHeader.append("BaseTotalHit").append(BaseConstant.TAB_STR);
        strbHeader.append("BaseTotalWin").append(BaseConstant.TAB_STR);
        strbHeader.append("Gold Trigger Hit").append(BaseConstant.TAB_STR);
        for (int i = 0; i < 3; i++) {
            strbHeader.append(getPokerGoldFeatureStr(i)).append(" Hit").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < 3; i++) {
            strbHeader.append(getPokerGoldFeatureStr(i)).append(" Win").append(BaseConstant.TAB_STR);
        }
        strbHeader.append("Fs Total Hit").append(BaseConstant.TAB_STR);
        strbHeader.append("Fs Total Times").append(BaseConstant.TAB_STR);
        strbHeader.append("Fs Total Win").append(BaseConstant.TAB_STR);
        strbHeader.append("Bonus Total Hit").append(BaseConstant.TAB_STR);
        strbHeader.append("Bonus Total Win").append(BaseConstant.TAB_STR);
        strbHeader.append("Instant Cash Total Win").append(BaseConstant.TAB_STR);
        for (int i = 0; i < Model6080630Test.INSTANT_CASH_PAY_WEIGHT[0].length; i++) {
            strbHeader.append("Instant Cash").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < Model6080630Test.INSTANT_CASH_PAY_WEIGHT[0].length; i++) {
            strbHeader.append("Instant Cash").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < Model6080630Test.FS_TIMES_WEIGHT[0].length; i++) {
            strbHeader.append("Fs Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < Model6080630Test.FS_TIMES_WEIGHT[0].length; i++) {
            strbHeader.append("Fs Index").append(i + 1).append(" Times").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < Model6080630Test.FS_TIMES_WEIGHT[0].length; i++) {
            strbHeader.append("Fs Index").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < 6; i++) {
            strbHeader.append("Bonus R OR B Round").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < 6; i++) {
            strbHeader.append("Bonus R OR B Round").append(i + 1).append(" Win").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < 10; i++) {
            strbHeader.append(getHandPayType(i)).append(" Hit").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < 10; i++) {
            strbHeader.append(getHandPayType(i)).append(" Win").append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < Model6060630Test.FS_MUL_WEIGHT[0].length; i++) {
            strbHeader.append("FsMul Index").append(i + 1).append(" Hit").append(BaseConstant.TAB_STR);
        }
        return strbHeader.toString();
    }

    private static String getHandPayType(int index) {
        String str = "";
        switch (index) {
            case 1:
                str = "Royal flush";
                break;
            case 2:
                str = "Straight flush";
                break;
            case 3:
                str = "Four of a kind";
                break;
            case 4:
                str = "Full house";
                break;
            case 5:
                str = "Flush";
                break;
            case 6:
                str = "Straight";
                break;
            case 7:
                str = "Three of a kind";
                break;
            case 8:
                str = "Two pair";
                break;
            case 9:
                str = "Jacks or Better";
                break;
            default:
                str = "Nothing";
                break;
        }
        return str;
    }

    private static String getPokerGoldFeatureStr(int index) {
        String str = "";
        switch (index) {
            case 0:
                str = "Free Game";
                break;
            case 1:
                str = "Instant Prize";
                break;
            case 2:
                str = "R or B";
                break;
            default:
                str = "Free Game";
                break;
        }
        return str;
    }

    public static String getCommonBaseHeaderInfo(BaseResultInfo resultInfo) {
        StringBuilder strbHeader = new StringBuilder();
        strbHeader.append("Num of Spin").append(BaseConstant.TAB_STR);
        strbHeader.append("LeftCredit").append(BaseConstant.TAB_STR);
        strbHeader.append("Line").append(BaseConstant.TAB_STR);
        strbHeader.append("BetPerLine").append(BaseConstant.TAB_STR);
        strbHeader.append("TotalAmount").append(BaseConstant.TAB_STR);
        strbHeader.append("TotalHit").append(BaseConstant.TAB_STR);
        strbHeader.append("TotalCoinIn").append(BaseConstant.TAB_STR);
        strbHeader.append("TotalCoinOut").append(BaseConstant.TAB_STR);
        strbHeader.append("Hit Rate").append(BaseConstant.TAB_STR);
        strbHeader.append("PayBack").append(BaseConstant.TAB_STR);
        strbHeader.append("BaseGame Hit").append(BaseConstant.TAB_STR);
        strbHeader.append("BaseGame TotalWin").append(BaseConstant.TAB_STR);
        strbHeader.append("BaseGame Hit Rate").append(BaseConstant.TAB_STR);
        strbHeader.append("BaseGame PayBack").append(BaseConstant.TAB_STR);
        strbHeader.append("BaseGame TopAward").append(BaseConstant.TAB_STR);
        strbHeader.append("BaseGame TopAward ReelsStop").append(BaseConstant.TAB_STR);
        strbHeader.append("BaseGame TopAward Type").append(BaseConstant.TAB_STR);
        return strbHeader.toString();
    }

    /**
     * get baseGame common header information.
     *
     * @param resultInfo
     * @return
     * @author:jiangqx Feb 16, 2017
     */
    public static String getCommonHeaderInfo(BaseResultInfo resultInfo) {
        StringBuilder strbHeader = new StringBuilder();
        strbHeader.append(getCommonBaseHeaderInfo(resultInfo));
        //strbHeader.append("Freespin TopAward").append(BaseConstant.TAB_STR);
        strbHeader.append("Freespin Total Hit").append(BaseConstant.TAB_STR);
        strbHeader.append("Freespin Total Win").append(BaseConstant.TAB_STR);
        strbHeader.append("FreeSpin Total Hit Rate")
                .append(BaseConstant.TAB_STR);
        strbHeader.append("FreeSpin Total PayBack")
                .append(BaseConstant.TAB_STR);
        strbHeader.append("Feespin Total Times").append(BaseConstant.TAB_STR);
        strbHeader.append("Freespin TopAward").append(BaseConstant.TAB_STR);
        strbHeader.append("FS TopAward ReelsStop").append(BaseConstant.TAB_STR);
        strbHeader.append("FS TopAward Type").append(BaseConstant.TAB_STR);
        strbHeader.append("screen Max Award").append(BaseConstant.TAB_STR);
        strbHeader.append("screen Max Award Hit").append(BaseConstant.TAB_STR);
        return strbHeader.toString();
    }

    public static String getBonusHeaderInfo(BaseResultInfo resultInfo) {
        StringBuilder strbHeader = new StringBuilder();
        strbHeader.append("Bonus Total Hit").append(BaseConstant.TAB_STR);
        strbHeader.append("Bonus Total Win").append(BaseConstant.TAB_STR);
        strbHeader.append("Bonus Total Hit Rate").append(BaseConstant.TAB_STR);
        strbHeader.append("Bonus Total Payback").append(BaseConstant.TAB_STR);
        return strbHeader.toString();
    }

    /**
     * get FreeSpin Detail symbol Header info.
     *
     * @return
     * @author:jiangqx Feb 13, 2017
     */
    public static String getFreespinSymbolHeaderInfo() {
        StringBuilder strbHeader = new StringBuilder();
        for (int i = 0; i < BaseConstant.SYMBOL_COUNT; i++) {
            // freeSpin symbol Hit
            strbHeader.append("Symbol").append(i + 1)
                    .append(BaseConstant.TAB_STR);
            strbHeader.append("FreeSpin 2 Symbol").append(i + 1).append(" Hit ")
                    .append(BaseConstant.TAB_STR);
            strbHeader.append("FreeSpin 3 Symbol").append(i + 1).append(" Hit ")
                    .append(BaseConstant.TAB_STR);
            strbHeader.append("FreeSpin 4 Symbol").append(i + 1).append(" Hit ")
                    .append(BaseConstant.TAB_STR);
            strbHeader.append("FreeSpin 5 Symbol").append(i + 1).append(" Hit ")
                    .append(BaseConstant.TAB_STR);
            strbHeader.append("FreeSpin Symbol").append(i + 1)
                    .append(" Total Hit ").append(BaseConstant.TAB_STR);
            // freeSpin symbol win
            strbHeader.append("FreeSpin 2 Symbol").append(i + 1).append(" win ")
                    .append(BaseConstant.TAB_STR);
            strbHeader.append("FreeSpin 3 Symbol").append(i + 1).append(" win ")
                    .append(BaseConstant.TAB_STR);
            strbHeader.append("FreeSpin 4 Symbol").append(i + 1).append(" win ")
                    .append(BaseConstant.TAB_STR);
            strbHeader.append("FreeSpin 5 Symbol").append(i + 1).append(" win ")
                    .append(BaseConstant.TAB_STR);
            strbHeader.append("FreeSpin Symbol").append(i + 1)
                    .append(" Total Win Pay ")
                    .append(BaseConstant.TAB_STR);
        }
        return strbHeader.toString();
    }

    /**
     * get FreeSpin Symbol Detail Information
     *
     * @param fsSymbolInfoList
     * @return
     * @author:jiangqx Feb 13, 2017
     */
    public static String getFreespinSymbolInfo(
            List<SymbolResultInfo> fsSymbolInfoList) {
        StringBuilder strContent = new StringBuilder();
        if (fsSymbolInfoList != null && !fsSymbolInfoList.isEmpty()) {
            for (SymbolResultInfo symbolInfo : fsSymbolInfoList) {
                strContent.append("symbol").append(symbolInfo.getSymbolNo())
                        .append(BaseConstant.TAB_STR);
                for (int i = 1; i < symbolInfo.getHitPayCount().length; i++) {
                    strContent.append(symbolInfo.getHitPayCount()[i])
                            .append(BaseConstant.TAB_STR);
                }
                strContent.append(symbolInfo.getTotalHit())
                        .append(BaseConstant.TAB_STR);
                for (int i = 1; i < symbolInfo.getHitPayAmount().length; i++) {
                    strContent.append(symbolInfo.getHitPayAmount()[i])
                            .append(BaseConstant.TAB_STR);
                }
                strContent.append(symbolInfo.getTotalAmount())
                        .append(BaseConstant.TAB_STR);
            }
        }
        return strContent.toString();
    }

    public static String getBaseGameResultInfo(BaseResultInfo resultInfo) {
        StringBuilder strContent = new StringBuilder();
        strContent.append(resultInfo.getSpinCount())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getLeftCredit())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getLine()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBetPerLine())
                .append(BaseConstant.TAB_STR);
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
        strContent.append(resultInfo.getBaseGameHit())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBaseGameTotalWin())
                .append(BaseConstant.TAB_STR);
        double baseGameHitRate = resultInfo.getBaseGameHit() * 1.0
                / resultInfo.getSpinCount();
        double baseGamePayBack = resultInfo.getBaseGameTotalWin() * 1.0
                / resultInfo.getTotalCoinIn();
        strContent.append(baseGameHitRate).append(BaseConstant.TAB_STR);
        strContent.append(baseGamePayBack).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBaseGameTopAward())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBaseTopAwardReelStop()).
                append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBaseTopAwardType()).
                append(BaseConstant.TAB_STR);
        return strContent.toString();
    }

    /**
     * get base result info.
     *
     * @param resultInfo
     * @return
     * @author:jiangqx Feb 17, 2017
     */
    public static String getBaseResultInfo(BaseResultInfo resultInfo) {
        StringBuilder strContent = new StringBuilder();
        strContent.append(getBaseGameResultInfo(resultInfo));
        //strContent.append(resultInfo.getFreespinTopAward()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFreespinTotalHit())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFreespinTotalWin())
                .append(BaseConstant.TAB_STR);
        double fsHitRate = resultInfo.getFreespinTotalHit() * 1.0
                / resultInfo.getSpinCount();
        double fsPayBack = resultInfo.getFreespinTotalWin() * 1.0
                / resultInfo.getTotalCoinIn();
        strContent.append(fsHitRate).append(BaseConstant.TAB_STR);
        strContent.append(fsPayBack).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFreespinTotalTimes())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFreespinTopAward())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFsTopAwardReelStop()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFsTopAwardType()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getScreenMaxAward()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getScreenMaxAwardHit()).append(BaseConstant.TAB_STR);
        return strContent.toString();
    }

    public static String getBonusResultInfo(BaseResultInfo resultInfo) {
        StringBuilder strContent = new StringBuilder();
        strContent.append(resultInfo.getBonusTotalHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBonusTotalWin()).append(BaseConstant.TAB_STR);
        double bonusTotalHitRate = resultInfo.getBonusTotalHit() * 1.0 / resultInfo.getSpinCount();
        double bonusTotalPayback = resultInfo.getBonusTotalWin() * 1.0 / resultInfo.getTotalCoinIn();
        strContent.append(bonusTotalHitRate).append(BaseConstant.TAB_STR);
        strContent.append(bonusTotalPayback).append(BaseConstant.TAB_STR);
        return strContent.toString();
    }


    /**
     * get freespin symbol Header information
     *
     * @return
     * @author:jiangqx Feb 17, 2017
     */
    public static Object getFsHeaderInfo() {
        StringBuilder strbHeader = new StringBuilder();
        strbHeader.append("Num of Spin").append(BaseConstant.TAB_STR);
        strbHeader.append("Line").append(BaseConstant.TAB_STR);
        strbHeader.append("BetPerLine").append(BaseConstant.TAB_STR);
        strbHeader.append("TotalCoinIn").append(BaseConstant.TAB_STR);
        strbHeader.append("FreeSpin TotalTimes").append(BaseConstant.TAB_STR);
        return strbHeader.toString();
    }

    /**
     * get freeSpin base result information.
     *
     * @param resultInfo
     * @return
     * @author:jiangqx Feb 17, 2017
     */
    public static Object getFSBaseResultInfo(BaseResultInfo resultInfo) {
        StringBuilder strContent = new StringBuilder();
        strContent.append(resultInfo.getSpinCount())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getLine()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBetPerLine())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalCoinIn())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFreespinTotalTimes())
                .append(BaseConstant.TAB_STR);
        return strContent.toString();
    }

    /**
     * get pay Table hit header information.
     *
     * @param resultInfo
     * @return
     * @author:jiangqx Feb 20, 2017
     */
    public static String getPayTableHeaderInfo(BaseResultInfo resultInfo) {
        StringBuilder strbHeader = new StringBuilder();
        Map<Long, Long> payTableHitMap = resultInfo.getPayTableHit();
        if (payTableHitMap != null && !payTableHitMap.isEmpty()) {
            long[][] payTableHit = sort(payTableHitMap);
            for (int i = 0; i < payTableHitMap.size(); i++) {
                strbHeader.append("Pay").append(payTableHit[i][0])
                        .append(BaseConstant.TAB_STR);
                strbHeader.append("Pay").append(payTableHit[i][0]).append("Hit")
                        .append(BaseConstant.TAB_STR);
                strbHeader.append("Pay").append(payTableHit[i][0]).append("PayOut")
                        .append(BaseConstant.TAB_STR);

            }
        }
        return strbHeader.toString();
    }

    /**
     * get pay Table hit value.
     *
     * @param resultInfo
     * @return str
     * @author:jiangqx Feb 20, 2017
     */
    public static String getPayTableHit(BaseResultInfo resultInfo) {
        StringBuilder strContent = new StringBuilder();
        Map<Long, Long> payTableHitMap = resultInfo.getPayTableHit();
        Map<Long, Long> payTablePayOutMap = resultInfo
                .getPayTablePayOut();
        if (payTableHitMap != null && !payTableHitMap.isEmpty()) {
            long[][] payTableHit = sort(payTableHitMap);
            long[][] payTablePayOut = sort(payTablePayOutMap);
            for (int i = 0; i < payTableHitMap.size(); i++) {
                strContent.append(payTableHit[i][0])
                        .append(BaseConstant.TAB_STR);
                strContent.append(payTableHit[i][1])
                        .append(BaseConstant.TAB_STR);
                if (payTableHit[i][0] == payTablePayOut[i][0]) {
                    strContent.append(payTablePayOut[i][1])
                            .append(BaseConstant.TAB_STR);
                }
            }
        }
        return strContent.toString();
    }

    /**
     * hashMap sort
     *
     * @param map
     * @return
     * @author:jiangqx Feb 20, 2017
     */
    public static long[][] sort(Map<Long, Long> map) {
        if (map != null && !map.isEmpty()) {
            int size = map.size();
            long[] tempAllKey = new long[size];
            long[] tempAllValue = new long[size];
            int index = 0;
            for (Map.Entry<Long, Long> entry : map.entrySet()) {
                Long key = entry.getKey();
                Long value = entry.getValue();
                tempAllKey[index] = key;
                tempAllValue[index] = value;
                index++;
            }

            // sort map key
            for (int i = 0; i < size; i++) {
                for (int j = i + 1; j < size; j++) {
                    long left = tempAllKey[i];
                    long rigth = tempAllKey[j];
                    if (left > rigth) {
                        long tempkey = tempAllKey[i];
                        tempAllKey[i] = tempAllKey[j];
                        tempAllKey[j] = tempkey;

                        long tempValue = tempAllValue[i];
                        tempAllValue[i] = tempAllValue[j];
                        tempAllValue[j] = tempValue;
                    }
                }
            }
            long[][] results = new long[size][];
            for (int i = 0; i < size; i++) {
                long[] temp = new long[2];
                temp[0] = tempAllKey[i];
                temp[1] = tempAllValue[i];
                results[i] = temp;
            }
            return results;
        }
        return null;
    }

    public static String[] changeListToStrArray(List<String> list) {
        String[] result = null;
        if (list != null && !list.isEmpty()) {
            result = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i).trim();
            }
        }
        return result;
    }

    public static int[] changeListToIntegerArray(List<Integer> list) {
        int[] result = null;
        if (list != null && !list.isEmpty()) {
            result = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i);
            }
        }
        return result;
    }

    public static long[] changeListToLongArray(List<Long> list) {
        long[] result = null;
        if (list != null && !list.isEmpty()) {
            result = new long[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i);
            }
        }
        return result;
    }

    public static int[] changeStrToArray(String value, String splitStr) {
        int[] result = null;
        if (value != null && !"".equals(value.trim())) {
            try {
                String[] balls = value.trim().split(splitStr);
                if (balls != null) {
                    result = new int[balls.length];
                    for (int i = 0; i < balls.length; i++) {
                        result[i] = Integer.parseInt(balls[i].trim());
                    }
                }
            } catch (Exception e) {
                log.error("", e);
            }
        }
        return result;
    }

    public static long[] changeStrToLongArray(String value, String splitStr) {
        long[] result = null;
        if (value != null && !"".equals(value.trim())) {
            try {
                String[] balls = value.trim().split(splitStr);
                if (balls != null) {
                    result = new long[balls.length];
                    for (int i = 0; i < balls.length; i++) {
                        result[i] = Long.parseLong(balls[i].trim());
                    }
                }
            } catch (Exception e) {
                log.error("", e);
            }
        }
        return result;
    }

    public static int[][] changeStrArrayToIntegerArray(String[] strArray) {
        if (strArray != null && strArray.length > 0) {
            int[][] result = new int[strArray.length][];
            int index = 0;
            for (String str : strArray) {
                int[] weight = changeStrToArray(str, ",");
                result[index] = weight;
                index++;
            }
            return result;
        }
        return null;
    }

    public static long[][] changeStrArrayToLongArray(String[] strArray) {
        if (strArray != null && strArray.length > 0) {
            long[][] result = new long[strArray.length][];
            int index = 0;
            for (String str : strArray) {
                long[] weight = changeStrToLongArray(str, ",");
                result[index] = weight;
                index++;
            }
            return result;
        }
        return null;
    }

    public static String IntegerArrayToStr(int[] array, String str) {
        StringBuilder resultStr = new StringBuilder();
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                resultStr.append((i == 0 ? "" : str)).append(array[i]);
            }
        }
        return resultStr.toString();
    }

    public static boolean containsIgnoreCase(List<String> array, String value) {
        boolean result = false;
        if (array != null && array.size() > 0) {
            for (String temp : array) {
                if (temp != null && temp.equalsIgnoreCase(value)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public static String IntegerArrToString(int[] array, String str) {
        StringBuilder resultStr = new StringBuilder();
        if (array != null) {
            resultStr.append(array.length);
            for (int i = 0; i < array.length; i++) {
                resultStr.append(str).append(array[i]);
            }
        }
        return resultStr.toString();
    }

    public static String LongArrToString(long[] array, String str) {
        StringBuilder resultStr = new StringBuilder();
        if (array != null) {
            resultStr.append(array.length);
            for (int i = 0; i < array.length; i++) {
                resultStr.append(str).append(array[i]);
            }
        }
        return resultStr.toString();
    }

    public static String arrayToString(int[][] array, String str) {
        StringBuilder resultStr = new StringBuilder();
        if (array != null) {
            resultStr.append(array.length);
            for (int i = 0; i < array.length; i++) {
                resultStr.append(str).append(IntegerArrayToStr(array[i], " "));
            }
        }
        return resultStr.toString();
    }

    public static double[] changeStrToDoubleArray(String value, String splitStr) {
        double[] result = null;
        if (value != null && !"".equals(value.trim())) {
            try {
                String[] balls = value.trim().split(splitStr);
                if (balls != null) {
                    result = new double[balls.length];
                    for (int i = 0; i < balls.length; i++) {
                        result[i] = Double.parseDouble(balls[i].trim());
                    }
                }
            } catch (Exception e) {
                log.error("", e);
            }
        }
        return result;
    }

    public static String getKenoHeadInfo(BaseConfigInfo configInfo, BaseKenoModel kenoModel, KenoResultInfo resultInfo) {
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
        strbHeader.append("Fs Actual Total Win").append(BaseConstant.TAB_STR);
        strbHeader.append("Fs Total Win").append(BaseConstant.TAB_STR);
        for (int i = 0; i < KenoEngineResult.BASE_3SETS_MUL.length; i++) {
            strbHeader.append("Base Mul Index").append(i + 1).append(BaseConstant.TAB_STR);
        }
        strbHeader.append("BaseAll4SpotsHit").append(BaseConstant.TAB_STR);
        strbHeader.append("Base3Out4SpotsHit").append(BaseConstant.TAB_STR);
        for (int i = 0; i < KenoEngineResult.BASE_3SETS_MUL.length; i++) {
            strbHeader.append("fs Mul Index").append(i + 1).append(BaseConstant.TAB_STR);
        }
        strbHeader.append("FsAll4SpotsHit").append(BaseConstant.TAB_STR);
        strbHeader.append("Fs3Out4SpotsHit").append(BaseConstant.TAB_STR);
        strbHeader.append("FsAll3SpotsHit").append(BaseConstant.TAB_STR);
        strbHeader.append("Fs2Out3SpotsHit").append(BaseConstant.TAB_STR);
        for (int i = 0; i < kenoModel.mixHitOnAll3Sets()[0].length; i++) {
            strbHeader.append("mixHit3Set Index").append(i + 1).append(BaseConstant.TAB_STR);
        }
        for (int i = 0; i < kenoModel.mixHitOnAll4Sets()[0].length; i++) {
            strbHeader.append("mixHit4Set Index").append(i + 1).append(BaseConstant.TAB_STR);
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
        for (int i = 0; i < KenoEngineResult.BASE_3SETS_FS_TIMES.length; i++) {
            strbHeader.append("Base FsTime").append(KenoEngineResult.BASE_3SETS_FS_TIMES[i]).append(" Hit").append(BaseConstant.TAB_STR);
        }
        return strbHeader.toString();
    }
}
