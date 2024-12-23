package com.gcs.game.simulation.keno.vo;

import lombok.Data;

import java.util.List;

@Data
public class KenoResultInfo {
    private long spinCount = 0L;
    private long leftCredit = 0L;
    private long lines = 0l;
    private long bet = 0l;
    private long denom = 0L;
    private long totalBet = 0L;
    private long totalAmount = 0L;
    private long totalHit = 0L;
    private long totalCoinIn = 0L;
    private long totalCoinOut = 0L;
    private long baseTotalHit = 0l;
    private long baseTotalWin = 0l;

    private long fsTotalHit = 0L;

    private long fsActualTotalWin = 0L;
    private long fsTotalWin = 0l;

    /**
     * freeSpin total spin times.
     */
    private long fsTotalTimes = 0L;
    private List<Long> baseMulHit = null;
    private long baseAll4SpotsHit = 0L;
    private long base3Out4SpotsHit = 0L;
    private List<Long> baseAll3SetTimesHit = null;
    private List<Long> fsMulHit = null;
    private long fsAll4SpotsHit = 0L;
    private long fs3Out4SpotsHit = 0L;
    private long fsAll3SpotsHit = 0L;
    private long fs2Out3SpotsHit = 0L;
    private List<Long> mixHit3SetCount = null;
    private List<Long> mixHit4SetCount = null;
    private List<List<Long>> payTableHit = null;
    private List<List<Long>> payTableWin = null;

}
