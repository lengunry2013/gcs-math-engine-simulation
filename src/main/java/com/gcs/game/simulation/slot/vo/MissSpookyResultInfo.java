package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import lombok.Data;

@Data
public class MissSpookyResultInfo extends BaseResultInfo {
    private long[] baseMysteryMulHit = new long[5];

    private long[] fsTimes = new long[3];

    private long[] fsWin = new long[3];

    private long[] fsHit = new long[3];

    private long[] lightningFsScriptHit = new long[10];

    private long[] lightningFsScriptWin = new long[10];

    private long[] groundFsScriptHit = new long[10];

    private long[] getGroundFsScriptWin = new long[10];

    private long[] wildFsHit = new long[15];

    private long[] wildFsWin = new long[15];

    private long[] BonusWinComboHit = new long[14];

    private long[] BonusWinComboWin = new long[14];

}
