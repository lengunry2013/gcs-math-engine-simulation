package com.gcs.game.simulation.slot.common.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AchievementSymbol {
    private int symbolNo = 1;

    private List<AchievementPay> AchievementPayList = null;

    /**
     * each symbol pay3,pay4,pay5 achievement
     */
    private List<Long> payTotalAchievement = new ArrayList<Long>();

    private long payTotalSpinCount = 0L;


    public AchievementSymbol(int symbolNo) {
        this.symbolNo = symbolNo;
    }

}
