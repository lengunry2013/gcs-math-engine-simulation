package com.gcs.game.simulation.slot.common.vo;

import lombok.Data;

import java.util.List;
@Data
public class MissSymbol {

    private int symbolNo = 1;

    private long symbolTotalHit = 0L;

    private List<MissPay> missPayList = null;

    public MissSymbol(int symbolNo) {
        this.symbolNo = symbolNo;
    }

}
