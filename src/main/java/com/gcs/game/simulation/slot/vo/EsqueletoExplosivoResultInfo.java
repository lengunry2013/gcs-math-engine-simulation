package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import lombok.Data;

@Data
public class EsqueletoExplosivoResultInfo extends BaseResultInfo {

    private long[] baseReelsHit = new long[5];
    private long[] baseReelsWin = new long[5];
    private long[] baseNotRespinWin = new long[5];
    private long baseWildHit = 0L;
    private long[][] baseReelsMulHit = new long[5][7];
    private long[][] baseReelsMulWin = new long[5][7];
    private boolean isBaseChainReaction = false;
    private long[][] baseScatterHit = new long[5][3];
    private long[][] baseScatterWin = new long[5][3];
    private long[] baseRespinHit = new long[5];
    private long[] baseRespinTime = new long[5];
    private long[] baseRespinWin = new long[5];
    //fs in fs

    private long[] fsReelsHit = new long[5];
    private long[] fsReelsTimes = new long[5];
    private long[] fsReelsWin = new long[5];
    private long[][] fsScatterHit = new long[5][4];
    private long[][] fsReelsMulHit = new long[5][7];
    private long[][] fsReelsMulWin = new long[5][7];
    private long[] fsRespinHit = new long[5];
    private long[] fsRespinTime = new long[5];
    private long[] fsRespinWin = new long[5];
    private long fsWildHit = 0L;
    private boolean isFsChainReaction = false;
    private long fsTriCollectWildHit = 0L;
    private boolean isFsTriCollectWild = false;
    private long upMaxWinHit = 0L;

    private int[] baseDisplaySymbols = new int[15];
    private int[] fsDisplaySymbols = new int[15];

}
