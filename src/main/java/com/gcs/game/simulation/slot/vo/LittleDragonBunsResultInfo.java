package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import lombok.Data;

@Data
public class LittleDragonBunsResultInfo extends BaseResultInfo {

    private long[] mysteryMulHit = new long[8];

    private long[] fsIncMulTimes = new long[7];
    private long[] fsIncMulHit = new long[7];
    private long[] fsIncMulWin = new long[7];


}
