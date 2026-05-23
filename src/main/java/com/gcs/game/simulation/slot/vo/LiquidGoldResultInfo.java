package com.gcs.game.simulation.slot.vo;


import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import lombok.Data;

/**
 * DakotaGold result information
 *
 * @author Jiangqx
 * @create 2025-09-20
 **/
@Data
public class LiquidGoldResultInfo extends BaseResultInfo {
    private long[] baseTypeHit = new long[2];
    private long[] baseTypeWin = new long[2];
    private long[] baseMysteryTypeHit = new long[2];
    private long[] baseMysteryTypeWin = new long[2];
    private long[] baseWildRHit = new long[10];
    private long[] baseWildRWin = new long[10];
    private long[] baseStickyWildHit = new long[105];
    private long[] baseStickyWildWin = new long[105];

    private long[] fsHit = new long[3];
    private long[] fsTimes = new long[3];
    private long[] fsWin = new long[3];

    //FS WR
    private long[] fsWildRTimes = new long[10];
    private long[] fsWildRWin = new long[10];
    //FS Sticky
    private long[] fsStickyWildHit = new long[15];
    private long[] fsStickyWildTimes = new long[15];
    private long[] fsStickyWildWin = new long[15];
    //FS 3RW
    private long[] fs3RwHit = new long[10];
    private long[] fs3RwTimes = new long[10];
    private long[] fs3RwWin = new long[10];
    private long totalPayCapHit = 0;

}
