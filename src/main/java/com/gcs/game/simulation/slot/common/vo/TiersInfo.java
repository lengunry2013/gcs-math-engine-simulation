package com.gcs.game.simulation.slot.common.vo;

import lombok.Data;

@Data
public class TiersInfo {

    private double totalWon = 0.0;
    private long totalBet = 0L;
    private long baseWin = 0L;
    private double baseJackpotWin = 0.0;
    private long fsWin = 0L;
    private long fsTimes = 0L;
    private long fsHits = 0L;
    private double bonusWin = 0.0;
    private long bonusHit = 0L;
    private int fsType = 0;
    private int bonusType = 0;
    private int highSymbolCount = 0;
    //near miss symbol
    private int stackCount = 0;
    //near miss symbol contain wild
    private int stackWildCount = 0;
    //high and miss symbol contain wild sum
    private int highAndStackSymbolCount = 0;

    //o x x x x
    //o o x x x
    //o x x x o
    //x x o x x 4 pattern horizontal symbol count
    private int horizontalSymbolCount = 0;
}
