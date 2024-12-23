package com.gcs.game.simulation.slot.common.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AchievementPay {
    /**
     * BaseGame each symbol pay2,pay3,pay4,pay5(not wild symbol win) Achievement
     * e.g S_02 S_02 S_02 S_03 W_01  symbol2 pay3 achievement
     * W_01 W_01 W_01 W_01 S_02 symbol1 pay4 achievement
     * W_01 W_01 W_01 S_02 S_02 symbol1 pay3 achievement (wild symbol special handle)
     * S_02 S_02 W_01 S_02 S_03 not symbol achievement (achievement not wild symbol win)
     * S_02 S_02 S_02 W_01 W_01 not symbol achievement
     */
    private int pay = 0;

    private List<Long> payAchievement = new ArrayList<Long>();

    private long paySpinCount = 0L;

    private long payCacheCount = 0L;

    public AchievementPay(int pay) {
        this.pay = pay;
    }

}