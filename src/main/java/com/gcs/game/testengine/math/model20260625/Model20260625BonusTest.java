package com.gcs.game.testengine.math.model20260625;


import com.gcs.game.engine.math.model20260625.Model20260625Bonus;
import com.gcs.game.engine.slots.vo.SlotBonusResult;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.engine.slots.vo.SlotWheelBonusResult;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.vo.PlayerInputInfo;
import com.gcs.game.vo.RecoverInfo;

public class Model20260625BonusTest extends Model20260625Bonus {

    @Override
    public SlotBonusResult computeBonusStart(SlotGameLogicBean gameSessionBean, int payback) {
        SlotWheelBonusResult result = new SlotWheelBonusResult();
        int bonusStatus = GameConstant.SLOT_GAME_BONUS_STATUS_START;

        int[] pickIndexs = null;
        long totalPay = 0;
        long payForPick = 0;

        int betLevel = (int) (gameSessionBean.getBet() - 1);
        int[] awardWeights = getWheelAwardWeight()[betLevel];
        int randomIndex = RandomUtil.getRandomIndexFromArrayWithWeight(awardWeights);
        totalPay = WHEEL_AWARDS[randomIndex];
        result.setBonusPlayStatus(bonusStatus);
        result.setPickIndexInfos(pickIndexs);
        result.setTotalPay(totalPay);
        result.setPayForPickIndex(payForPick);
        result.setHitLevel(randomIndex + 1);
        return result;
    }

    @Override
    public SlotBonusResult computeBonusPick(SlotGameLogicBean gameSessionBean, PlayerInputInfo playerInfo, SlotBonusResult bonus, RecoverInfo recoverInfo) {
        if (bonus != null) {
            bonus.setBonusPlayStatus(1000);
        }
        return bonus;
    }

}
