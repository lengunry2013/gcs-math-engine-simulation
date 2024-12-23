package com.gcs.game.simulation.slot.common.vo;

import lombok.Data;

import java.util.List;

@Data
public class MissPay {

    private int pay = 0;

    private long totalHit = 0L;

    private List<Integer> missLine = null;

    private List<int[]> missSymbol = null;

    private List<int[]> missSymbolPosition = null;

    public MissPay(int pay) {
        this.pay = pay;
    }
}
