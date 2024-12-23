package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import com.gcs.game.simulation.slot.common.vo.SymbolResultInfo;
import lombok.Data;

import java.util.List;

/**
 * @author Jiangqx
 * @create 2020-03-04-17:26
 **/
@Data
public class BaseReelsDetailResultInfo extends BaseResultInfo {
    private String location = "SPIN";
    private String reelsStop = "";
    private long winIncremental = 0L;
    private String preScene = "";
    private String currentScene = "";
    private String nextScene = "";
    private String winLine = "";
    private String winSymbols = "";
    private String winPays = "";
    private String winLinePosition = "";
    private int leftFsTimes = 0;
    private long rangeTotalCoinIn = 0L;
    private long rangeTotalCoinOut = 0L;
    private String bonusOut = "bonus";
    private String fsOut = "freeSpin";
    private List<SymbolResultInfo> symbolResultInfoList = null;
    private List<Long> winTimesPerLineList = null;
    private List<Long> winAmountPerLineList = null;

}
