package com.gcs.game.simulation.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseConfigInfo {
    private String model = "";
    private int payback = 9550;
    private String jackpotGroupCode = "";

    private String gameClass = "";
    private long denom = 1;
    private long initCredit = 100000000;
    private long simulationCount = 1000000;
    private int playTimesPerPlayer = 1000;
    private int outputType = 1;
    private long maxFileSize = 10240000;
    private String outputPath = "";
    private String outputFileName = "";

    //output achievement result file
    private String achievementFileName = "";

    //output winTiers high Miss symbol result file
    private String highMissFileName = "";

    //output winTiers Fs/Bonus result file
    private String fsBonusFileName = "";

}
