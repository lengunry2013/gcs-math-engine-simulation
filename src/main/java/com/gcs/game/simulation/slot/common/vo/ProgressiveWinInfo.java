package com.gcs.game.simulation.slot.common.vo;


import lombok.Data;

import java.util.List;
@Data
public class ProgressiveWinInfo {
    private int hitLevel;
    private String hitLevelName;

    private int intervalSpinTimes;

    private long hitAmount;

    private int[] levelIntervalSpinTimes = null;

    private List<Long> levelsAmount = null;

}
