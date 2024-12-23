package com.gcs.game.simulation.poker.vo;

import com.gcs.game.simulation.vo.BaseConfigInfo;
import lombok.Data;

@Data
public class PokerConfigInfo extends BaseConfigInfo {

    private long lines = 1;
    private long bet = 1;
    private int[] GoldCardTriggerWeight = null;
    private int[][] baseWeight = null;
    private int[][] fsWeight = null;
    private int[][] bonusWeight = null;
    private int[][] InstantCashPayWeight = null;
    private long[] payTable = null;
    private long totalPayCap = 80000l;
    private int[][] fsMulWeight = null;
}
