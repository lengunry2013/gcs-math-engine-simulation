package com.gcs.game.simulation.blackJack.vo;

import lombok.Data;

import java.util.List;

@Data
public class BlackJackResultInfo {
    private long spinCount = 0L;
    private long leftCredit = 0L;
    private long denom = 0L;
    private long totalBet = 0L;
    private long totalAmount = 0L;
    private long totalHit = 0L;
    private long totalCoinIn = 0L;
    private long totalCoinOut = 0L;
    private List<BlackJackWinPay> blackJackWinPayList = null;
    private List<Long> jackpotWinHit = null;

}
