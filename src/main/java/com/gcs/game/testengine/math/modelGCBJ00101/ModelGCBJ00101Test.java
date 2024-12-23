package com.gcs.game.testengine.math.modelGCBJ00101;

import com.gcs.game.engine.math.modelGCBJ00101.ModelGCBJ00101;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IConfigWeight;

public class ModelGCBJ00101Test extends ModelGCBJ00101 implements IConfigWeight {
    private static ConfigWeight configWeight;

    @Override
    public void setConfigWeight(ConfigWeight configWeight) {
        this.configWeight = configWeight;
    }

    protected int[] getJackpotWeight() {
        int[] jackpotWeight = super.getJackpotWeight();
        if (configWeight.getJackpotWeight() != null && configWeight.getJackpotWeight().length > 0) {
            jackpotWeight = configWeight.getJackpotWeight();
        }
        return jackpotWeight;
    }

    protected long[] getPayTable() {
        long[] jackpotPay = super.getPayTable();
        if (configWeight.getJackpotPay() != null && configWeight.getJackpotPay().length > 0) {
            jackpotPay = configWeight.getJackpotPay();
        }
        return jackpotPay;
    }

}
