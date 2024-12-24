/*
 * BaseResultInfo.java
 * @author:jiangqx
 * History:
 *   date              name      Description
 *   Jan 19, 2017        jiangqx      create
 */

package com.gcs.game.simulation.slot.common.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class BaseResultInfo {

    private long spinCount = 0L;
    private long leftCredit = 0L;
    private int line = 0;
    private int betPerLine = 0;
    private double totalAmount = 0L;
    private long totalHit = 0L;
    private long totalCoinIn = 0L;
    private double totalCoinOut = 0L;
    private long baseGameHit = 0L;
    private long baseGameTotalWin = 0L;
    private long baseGameTopAward = 0L;

    private long freespinTopAward = 0L;

    private String baseTopAwardReelStop = "";

    private String fsTopAwardReelStop = "";

    private String baseTopAwardType = "";

    private String fsTopAwardType = "";

    private long freespinTotalHit = 0L;

    private long freespinTotalWin = 0L;

    /**
     * freeSpin total spin times.
     */
    private long freespinTotalTimes = 0L;

    private long bonusTotalHit = 0L;

    private long bonusTotalWin = 0L;

    private Map<Long, Long> payTableHit = new HashMap<Long, Long>();
    private Map<Long, Long> payTablePayOut = new HashMap<Long, Long>();

    /**
     * baseGame and freeSpin every symbol hit and win
     */
    private List<SymbolResultInfo> fsSymbolInfoList = new ArrayList<SymbolResultInfo>();

    /**
     * BaseGame each symbol pay2,pay3,pay4,pay5(not wild symbol win) Achievement
     * e.g S_02 S_02 S_02 S_03 W_01  symbol2 pay3 achievement
     * W_01 W_01 W_01 W_01 S_02 symbol1 pay4 achievement
     * S_02 S_02 W_01 S_02 S_03 not symbol achievement (achievement not wild symbol win)
     */
    private List<AchievementSymbol> baseAchievementSymbol = new ArrayList<AchievementSymbol>();

    private List<MissSymbol> baseMissSymbol = null;

    private String missReelsStop = "";

    private double stdDeviation = 0.0;

    private double baseStdDeviation = 0.0;

    private double[] fsStdDeviation = null;

    private double[] bonusStdDeviation = null;

    private Map<Long, Long> basePayWeightMap = new ConcurrentHashMap<>();
    private List<Map<Long, Long>> fsPayWeightMapList = null;

    private List<Map<Long, Long>> bonusPayWeightMapList = null;

    private long[] maxWinCount = null;

    private long[] maxWinTotalPay = null;

    /**
     * compute tiers  Percentage of max pay baseGame,
     * tier=5 4*totalBet<win<=5*totalBet,maxPay=5*totalBet=2500,baseGameWin=1000,fs=1000,bonusWin=500,basePayPer=5*(1000/2500)=2
     */
    private long[] baseTotalMaxPay = null;

    /**
     * compute tiers Percentage of max pay fs
     * tier=5 4*totalBet<win<=5*totalBet,maxPay=5*totalBet=2500,baseGameWin=1000,fs=1000,bonusWin=500,fsPayPer=5*(1000/2500)=2
     */
    private long[] fsTotalMaxPay = null;

    /**
     * compute tiers Percentage of bonus
     * tier=5 4*totalBet<win<=5*totalBet,maxPay=5*totalBet=2500,baseGameWin=1000,fs=1000,bonusWin=500,fsPayPer=5*(500/2500)=1
     */
    private long[] bonusTotalMaxPay = null;

    private long[] screenMaxPay = null;

    private long[] maxPayTotalHit = null;

    //high symbol W_01 and scatter,or other special symbol
    private long[][] highSymbolPerMaxPayHit = null;

    private long[] baseTotalPay = null;

    private long[] fsTotalPay = null;

    private long[] bonusTotalPay = null;

    //miss symbol S_02 ~ S_05,according to math's own definition
    private long[][] nearMissSymbolHit = null;

    //stack symbol contain wild, S02,S02,W_01 is 1 stack
    private long[][] nearMissWildSymbolHit = null;

    //joint Wild/scatter and Stacks,Example, there are 1 wild, 1 stack, 1 scatter on screen, the near miss table will be 3
    private long[][] allNearMissSymbolHit = null;

    //Win Tier Classification (Free Spin Pays)
    private long[][] fsEntries = null;

    private long[][] fsTypeTimes = null;

    private long[][] fsPerHit = null;

    private long[][] fsTypeTotalWin = null;

    private long[][] fsMaxPay = null;

    //Win Tier Classification (Pick Bonus Pays)
    private long[][] bonusEntries = null;

    private long[][] bonusTimes = null;

    private long[][] bonusPerHit = null;

    private long[][] bonusTypeTotalWin = null;

    private long[][] bonusMaxPay = null;

    //o x x x x
    //o o x x x
    //o x x x o
    //x x o x x 4 pattern horizontal symbol count
    private long[][] horizontalSymbolHit = null;

    /**
     * totalWin max value hit
     */
    private long screenMaxAwardHit = 0L;

    private double screenMaxAward = 0L;


    private Map<Long, Long> payWeightMap = new ConcurrentHashMap<Long, Long>();

    /*
     * last spin info
     */
    private long lastSpinCoinIn = 0L;

    private long lastSpinCoinOut = 0L;

    private long lastSpinCount = 0L;

    private long lastSpinInitBalancePerTime = 0L;

    private long lastSpinTotalCountPerTime = 0L;

    private long lastSpinBalancePerTime = 0L;

    private long lastSpinCountPerTime = 0L;

    private long lastSpinMaxWinPerTime = 0L;

    private boolean lastSpinOutputFirstRow = false;

    private boolean lastSpinOutputPerTime = false;

    private long lastSpinCountTotal = 0L; // all players count
    private long lastSpinHitCountTotal = 0L; // player last spin hit count
    private long[] lastPlaySpinThresholdsBalance = null;
    private long lastSpinCoinInPerTime = 0L;
    private long lastSpinCoinOutPerTime = 0L;
    private Map<Integer, String> lastPlaySpinThresholdsBalanceMap = new HashMap<Integer, String>();

    private long playerNumber = 1L;

    private long spinCountsForEveryPlayer = 0L;

    private long runningBankForEveryPlayer = 0L;

    private long bonusPlaysCountForEveryPlayer = 0L;

    private long[] collectionCountsForMaximumWin = null;

    private long peakBankrollForEveryPlayer = leftCredit;

    //The spin times of the interval between two bonus
    private long[] playsPerBonusHit = null;
    //The spin times of the interval between two Fs
    private long[] playsPerFsHit = null;
    //The spin times of the interval between two bonus/Fs
    private long[] playsPerExtraPlayHit = null;
    private long minPlaysPerBonusSpinCount = 0L;
    private long maxPlaysPerBonusSpinCount = 0L;
    private long minPlaysPerFsSpinCount = 0L;
    private long maxPlaysPerFsSpinCount = 0L;

    //private long screenMaxAwardHit = 0L;
    private int specialSymbol = 2;

    private long jackpotHit = 0;
    //jackpot win cent
    private long jackpotWin = 0;

    private long denom = 0;

    private List<ProgressiveWinInfo> jackpotWinList = new ArrayList<>();

    public BaseResultInfo() {

    }

    public void initLastPlaySpinThresholdsBalanceMap(int[] spinNumArray) {
        Map<Integer, String> tempMap = getLastPlaySpinThresholdsBalanceMap();
        for (int spinNum : spinNumArray) {
            tempMap.put(spinNum, "-1");
        }
        setLastPlaySpinThresholdsBalanceMap(tempMap);
    }

    public void addLastPlaySpinThresholdsBalance(int spinNum, String balanceStr) {
        Map<Integer, String> tempMap = getLastPlaySpinThresholdsBalanceMap();
        if (tempMap.containsKey(spinNum)) {
            tempMap.put(spinNum, balanceStr);
        }
        setLastPlaySpinThresholdsBalanceMap(tempMap);
    }


}
