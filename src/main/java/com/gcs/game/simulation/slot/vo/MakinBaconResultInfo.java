package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import lombok.Data;

@Data
public class MakinBaconResultInfo extends BaseResultInfo {
    private long[] baseMySymbol = new long[10];
    private long[] baseReelsTypeHit = new long[2];
    private long[] baseReelsTypeWin = new long[2];
    private long[] sc1TriggerHit = new long[2];
    private long[] sc2TriggerHit = new long[2];
    private long[] sc3TriggerHit = new long[4];

    private long[] fsTimes = new long[3];

    private long[] fsWin = new long[3];

    private long[] fsHit = new long[3];

    private long[][] fsReelsTypeHit = new long[3][2];
    private long[][] fsReelsTypeWin = new long[3][2];

    private long[][] fsScAddHit = new long[3][16];
    private long[][] fsScTypeHit = new long[3][13];
    private long[][] fsScTypeWin = new long[3][13];
    private long[] jpBonusHit = new long[4];
    private long[] jpBonusWin = new long[4];
    private long[] jpBonusLettersHit = new long[19];


}
