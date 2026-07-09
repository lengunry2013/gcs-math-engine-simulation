package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import lombok.Data;

@Data
public class GoldRingCircusResultInfo extends BaseResultInfo {

    private long[] ScHit = new long[3];
    private long[] ScEntryWin = new long[3];
    /**
     * TABLE 1  link bonus coin pay
     */
    private long[] linkBonusHit = new long[13];
    private long[] linkBonusWin = new long[13];
    private long[] linkBonusCount = new long[9];
    private long[] linkBonusEndWin = new long[9];
    private long[] baseCoinWin = new long[9];
    private long[] colHit = new long[9];
    private long[] colTotalWin = new long[9];
    private long[] grandHit = new long[9];
    private long[] grandTotalWin = new long[9];
    private long[] respinTotalTimes = new long[9];

    private long fsTriggerBonusHit = 0L;
    private long fsTriggerBonusWin = 0L;
    private long[] fsScHit = new long[3];
    private long[] fsScEntryWin = new long[3];
    private long[] fsTimes = new long[3];
    private long[] fsWin = new long[3];

}
