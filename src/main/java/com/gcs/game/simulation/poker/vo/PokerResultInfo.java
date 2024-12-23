package com.gcs.game.simulation.poker.vo;

import lombok.Data;

import java.util.List;

@Data
public class PokerResultInfo {
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

    private List<Long> handPayHit = null;
    private List<Long> handPayWin = null;
    private List<Integer> handPokers = null;
    private long goldCardTriggerHit = 0l;
    private List<Long> goldCardFeatureHit = null;
    private List<Long> goldCashFeatureWin = null;
    private long fsTotalHit = 0L;

    private long fsTotalWin = 0L;

    /**
     * freeSpin total spin times.
     */
    private long fsTotalTimes = 0L;

    private long bonusTotalHit = 0L;

    private long bonusTotalWin = 0L;

    private long instantTotalWin = 0l;
    private List<Long> instantCashHit = null;
    private List<Long> instantCashWin = null;
    private List<Long> rOrBBonusHit = null;
    private List<Long> rOrBBonusWin = null;
    private List<Long> fsHit = null;
    private List<Long> fsTimes = null;
    private List<Long> fsWin = null;
    private List<Long> fsMulHit = null;

}
