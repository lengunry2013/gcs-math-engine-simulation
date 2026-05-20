package com.gcs.game.simulation.slot.vo;

import com.gcs.game.simulation.slot.common.vo.BaseResultInfo;
import lombok.Data;

@Data
public class CheddarQuestLinkResultInfo extends BaseResultInfo {
    private long[] baseSwHit = new long[10];
    private long[] baseSwWin = new long[10];
    private long[] baseReelsTypeHit = new long[2];
    private long[] baseReelsTypeWin = new long[2];

    private long[] fsReelsTypeHit = new long[2];
    private long[] fsReelsTypeWin = new long[2];

    private long[] scTriggerFsHit = new long[3];
    private long[] scTriggerFsWin = new long[3];
    private long[] scTriggerFsTimes = new long[3];

    private long[] triggerSwHit = new long[11];
    private long[] triggerSwWin = new long[11];
    private long[][] levelHit = new long[4][11];
    private long[][] levelWin = new long[4][11];
    private long grandHit = 0;
    private long grandWin = 0;
    private long[] bonusSwHit = new long[10];
    private long[] bonusSwWin = new long[10];
    private long[] triggerLinkBonusHit = new long[2];
    private long[] triggerLinkBonusWin = new long[2];

}
