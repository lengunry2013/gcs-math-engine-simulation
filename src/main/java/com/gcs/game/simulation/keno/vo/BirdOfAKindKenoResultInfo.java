package com.gcs.game.simulation.keno.vo;

import lombok.Data;

@Data
public class BirdOfAKindKenoResultInfo extends KenoResultInfo {
    private long[] basePairsHit = new long[7];

    private long[] fsPairsHit = new long[7];
    private long[] fsExtraDrawHit = new long[7];
    private long[] fsMultipliersHit = new long[7];
    private long[] fsExtraHit = new long[5];
}
