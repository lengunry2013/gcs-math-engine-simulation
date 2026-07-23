package com.gcs.game.simulation.keno.vo;

import lombok.Data;

import java.util.List;

@Data
public class MakinBaconKenoResultInfo extends KenoResultInfo {
    private long[] baseSetAHit = new long[6];
    private long[] baseSetBHit = new long[5];
    private long[] baseSetCHit = new long[4];
    private long[] baseSetA6SpotHit = new long[6];
    private long[] baseSetA5SpotHit = new long[5];
    private long[] baseSetB5SpotHit = new long[5];
    private long[] baseSetC4SpotHit = new long[4];

    private long[] fsSetAHit = new long[6];
    private long[] fsSetBHit = new long[5];
    private long[] fsSetCHit = new long[4];
    private long[] fsSetDHit = new long[3];
    private long[] fsExtraDrawHit = new long[18];
}
