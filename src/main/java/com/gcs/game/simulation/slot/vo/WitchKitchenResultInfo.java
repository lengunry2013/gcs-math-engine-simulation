package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import lombok.Data;

@Data
public class WitchKitchenResultInfo extends BaseResultInfo {
    private long[] baseWildHit = new long[3];
    private long[] baseWildWin = new long[3];
    private long[] sc1TriggerHit = new long[2];
    private long[] sc2TriggerHit = new long[2];
    private long[] sc3TriggerHit = new long[4];

    private long[] fsTimes = new long[3];

    private long[] fsWin = new long[3];

    private long[] fsHit = new long[3];
    private long[][] fsScTypeHit = new long[3][14];
    private long[][] fsScTypeWin = new long[3][14];
    private long[] jpBonusHit = new long[4];
    private long[] jpBonusWin = new long[4];
    private long[] jpBonusLettersHit = new long[19];


}
