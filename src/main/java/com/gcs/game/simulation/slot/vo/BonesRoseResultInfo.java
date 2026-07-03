package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import lombok.Data;

@Data
public class BonesRoseResultInfo extends BaseResultInfo {
    private long[] baseReelsTypeHit = new long[2];
    private long[] baseReelsTypeWin = new long[2];
    private long[] sc1TriggerHit = new long[2];
    private long[] sc2TriggerHit = new long[2];
    private long[] sc3TriggerHit = new long[4];

    private long[] fsTimes = new long[3];

    private long[] fsWin = new long[3];

    private long[] fsHit = new long[3];
    private long[] fsExpandWildHit = new long[2];
    private long[] fsWinExpandWin = new long[2];

    private long[][] fsScTypeHit = new long[3][12];
    private long[][] fsScTypeWin = new long[3][12];

    private long[] BonusWinComboHit = new long[4];

    private double[] BonusWinComboWin = new double[4];

    private long wagerSaverHitCount = 0L;
    private double wagerSaverWin = 0.0;

}
