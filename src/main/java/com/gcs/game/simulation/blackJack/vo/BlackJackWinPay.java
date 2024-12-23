package com.gcs.game.simulation.blackJack.vo;

import lombok.Data;

@Data
public class BlackJackWinPay {
    private int handIndex;
    private long betWinHit = 0L;
    private long betWinPay = 0L;
    private long jackpotWinHit = 0L;
    private long jackpotWinPay = 0L;
    private long splitWinHit = 0L;
    private long splitWinPay = 0L;
    private long insuranceWinHit = 0L;
    private long insuranceWinPay = 0L;

    public BlackJackWinPay(int handIndex) {
        this.handIndex = handIndex;
    }
}
