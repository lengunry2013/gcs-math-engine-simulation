package com.gcs.game.simulation.blackJack.vo;

import com.gcs.game.engine.blackJack.vo.BlackJackBetInfo;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import lombok.Data;

import java.util.List;

@Data
public class BlackJackConfigInfo extends BaseConfigInfo {

    private List<BlackJackBetInfo> blackJackBetInfoList = null;

    private int handCount = 1;

    private long[] jackpotPay = null;

    private int[] jackpotWeight = null;

}
