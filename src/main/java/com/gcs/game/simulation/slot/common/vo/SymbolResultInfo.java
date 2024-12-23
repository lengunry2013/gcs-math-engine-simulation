package com.gcs.game.simulation.slot.common.vo;

import lombok.Data;
@Data
public class SymbolResultInfo {

    private int symbolNo = 1;

    private long[] hitPayCount = null;

    private long[] hitPayAmount = null;

    private long totalHit = 0L;

    private long totalAmount = 0L;

    public SymbolResultInfo(int symbolNo) {
        this.symbolNo = symbolNo;
    }

}
