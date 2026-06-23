package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import lombok.Data;

@Data
public class KingFish2ResultInfo extends BaseResultInfo {

    private long[] fsTimes = new long[2];
    private long[] fsHits = new long[2];
    private long[] fsWin = new long[2];
    private long[] wheelBonusHits = new long[12];
    private long[] wheelBonusWin = new long[12];
    private long[] fsIncMulTimes = new long[5];
    private long[] fsIncMulHit = new long[5];
    private long[] fsIncMulWin = new long[5];
    private long[] fsWrScatterPrizeHits = new long[6];
    private long[] fsWrScatterPrizeWin = new long[6];
    private long[] fsSuperScatterPrizeHits = new long[6];
    private long[] fsSuperScatterPrizeWin = new long[6];


}
