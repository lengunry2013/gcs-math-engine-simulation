package com.gcs.game.simulation.slot.common.vo;

import lombok.Data;

@Data
public class TiersInfo {

    private long totalWon = 0L;
    private long totalBet = 0L;
    private long baseWin = 0L;
    private long fsWin = 0L;
    private long fsTimes = 0L;
    private long fsHits = 0L;
    private long bonusWin = 0L;
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
