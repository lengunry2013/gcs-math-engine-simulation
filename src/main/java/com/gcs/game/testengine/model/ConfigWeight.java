package com.gcs.game.testengine.model;

import lombok.Data;

@Data
public class ConfigWeight {
    private long[] jackpotPay = null;
    private int[] jackpotWeight = null;
    private int[][] baseWeight = null;
    private int[][] fsWeight = null;
    private int[][] bonusWeight = null;
    private long[][] dynamicPayTable = null;
    //engine hit jackpot
    private double contributionPercent = 1.0;
    private int[] levelDistribute = null;

    //pokers
    private int[] GoldCardTriggerWeight = null;
    private int[][] InstantCashPayWeight = null;
    private long[] payTable = null;
    private long totalPayCap = 0L;
    private int[][] fsMulWeight = null;

    //keno
    private int[][] fsTimes = null;
    private int[][] fs3SetsTimes = null;
    private int[][] fs3SetsWeight = null;
    private int[][] fs4SetsTimes = null;
    private int[][] fs4SetsWeight = null;
    private double[][] payTables = null;

}
