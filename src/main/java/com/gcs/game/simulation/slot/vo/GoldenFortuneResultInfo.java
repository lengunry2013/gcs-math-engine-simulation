package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import lombok.Data;

@Data
public class GoldenFortuneResultInfo extends BaseResultInfo {
    //sw start
    private long[] swTypeHit = new long[7];
    private long[] swTypeWin = new long[7];
    private long[] swCoinWin = new long[7];
    private long[] swColWin = new long[7];
    private long[] swFullHit = new long[7];
    private long[] swSpinTimes = new long[7];

    private long[] BonusWinComboHit = new long[4];

    private double[] BonusWinComboWin = new double[4];

    private long wagerSaverHitCount = 0L;
    private double wagerSaverWin = 0.0;

}
