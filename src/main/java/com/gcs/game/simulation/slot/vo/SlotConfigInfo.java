package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.vo.BaseConfigInfo;
import lombok.Data;

@Data
public class SlotConfigInfo extends BaseConfigInfo {
    private long lines = 1;
    private long bet = 1;

    private int choiceFsOrBonusIndex = -1;

    private boolean isRandomBonusChoice = false;
    private int[] maxWin = null;

    //input baseGame,freeSpin,bonus weight parameter
    private int[][] baseWeight = null;
    private int[][] fsWeight = null;
    private int[][] bonusWeight = null;
    //input symbol pay table value
    private long[][] symbolPayTable = null;

    //miss report
    private int[] missSymbol = null;

    private boolean isLineGame = true;

    private boolean isChangeBet = false;

    //high value symbol may is W_01 and Scatter symbol
    private int[] highSymbol = null;

    private int wildSymbol = 1;

    private boolean isFsPay = true;

    private boolean isBonusPay = true;

    private int fsType = 0;

    private int bonusType = 0;

    private double[] fsRtp = null;

    private double[] bonusRtp = null;

    private boolean isSumHorizontalSymbol = false;

    private int[] scatterSymbol = null;

    private int[] scatterCount = null;

    private int playGameCount = 1;

    private String playerChooseSymbol = "2";

    private int fsMeterTimes = 0;

    private int maxSpinNumForEveryPlayer = 1000;

    private int[] achievementSymbol = null;

    private int[] achievementPay = null;

    //max Achievement symbol pay interval index,
    //More than this number write the cached data to the file,clear the cache data at the same time
    private int maxFileAchievementPayIndex = 500000;

    private long totalPayCap = 0L;
    private long[][] payTables = null;

}
