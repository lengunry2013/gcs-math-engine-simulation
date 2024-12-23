package com.gcs.game.testengine.math.model6060630;

import com.gcs.game.engine.math.model6060630.Model6060630;
import com.gcs.game.engine.poker.vo.PokerGameLogicBean;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model6060630Test extends Model6060630 implements IConfigWeight {
    public static ConfigWeight configWeight;

    @Override
    public void setConfigWeight(ConfigWeight configWeight) {
        this.configWeight = configWeight;
    }


    @Override
    public long[] getPayTable(PokerGameLogicBean gameLogicBean) {
        if (configWeight != null && configWeight.getPayTable() != null) {
            return configWeight.getPayTable();
        }
        return super.getPayTable(gameLogicBean);
    }

    public long maxTotalPay() {
        if (configWeight != null) {
            return configWeight.getTotalPayCap();
        }
        return 80000;
    }

    protected int[] getGoldCardWeight() {
        if (configWeight != null && configWeight.getGoldCardTriggerWeight() != null) {
            return configWeight.getGoldCardTriggerWeight();
        }
        return GOLD_CARD_WEIGHT;
    }

    protected int[][] fsTimesWeight() {
        if (configWeight != null && configWeight.getFsWeight() != null) {
            return configWeight.getFsWeight();
        }
        return FS_TIMES_WEIGHT;
    }

    protected int[][] instantCashPayWeight() {
        if (configWeight != null && configWeight.getInstantCashPayWeight() != null) {
            return configWeight.getInstantCashPayWeight();
        }
        return INSTANT_CASH_PAY_WEIGHT;
    }

    protected int[] getGoldCardBonusWeight() {
        if (configWeight != null && configWeight.getBaseWeight() != null) {
            return configWeight.getBaseWeight()[0];
        }
        return GOLD_CARD_BONUS_WEIGHT;
    }

    protected int[][] getFsMulWeight() {
        if (configWeight != null && configWeight.getFsMulWeight() != null) {
            return configWeight.getFsMulWeight();
        }
        return FS_MUL_WEIGHT;
    }

}
