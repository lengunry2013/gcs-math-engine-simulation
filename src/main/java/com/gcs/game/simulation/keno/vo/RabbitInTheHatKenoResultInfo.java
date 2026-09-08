package com.gcs.game.simulation.keno.vo;

import lombok.Data;

@Data
public class RabbitInTheHatKenoResultInfo extends KenoResultInfo {
    private long[] baseSetAHit = new long[5];
    private long[] baseSetBHit = new long[2];

    private long[] fsSetAHit = new long[5];
    private long[] fsSetBHit = new long[4];
    private long[] fsSetBMulHit = new long[2];
    private long[] fsExtraDrawHit = new long[9];
}
