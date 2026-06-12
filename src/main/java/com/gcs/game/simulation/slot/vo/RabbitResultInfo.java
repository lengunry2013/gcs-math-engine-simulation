package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import lombok.Data;

@Data
public class RabbitResultInfo extends BaseResultInfo {
    private long[] ScHit = new long[2];
    private long[] baseFeatureHit = new long[3];
    private long[] baseFeatureWin = new long[3];
    private long[] baseScTriggerHit = new long[2];
    private long[] baseWlHit = new long[2];
    private long[] baseWinHit = new long[2];
    private long[] normalScatterHit = new long[2];
    private long[] normalScatterWin = new long[2];
    private long[] fsScatterPrizeHit = new long[4];
    private long[] fsScatterPrizeWin = new long[4];
    private long[] fsFeatureHit = new long[5];
    private long[] fsFeatureWin = new long[5];
    private long normalFsScatterHit = 0;
    private long[] fsScTriggerHit = new long[2];
    private long[] fsWlExpandHit = new long[2];
    private long[] fsWlMulHit = new long[4];
    private long[] fsWlAddHit = new long[6];
    private long[] fsWinHit = new long[2];


}
