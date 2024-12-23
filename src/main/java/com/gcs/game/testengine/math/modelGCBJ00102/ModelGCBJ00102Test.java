package com.gcs.game.testengine.math.modelGCBJ00102;

import com.gcs.game.engine.math.modelGCBJ00102.ModelGCBJ00102;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IConfigWeight;

public class ModelGCBJ00102Test extends ModelGCBJ00102 implements IConfigWeight {
    private static ConfigWeight configWeight;

    @Override
    public void setConfigWeight(ConfigWeight configWeight) {
        this.configWeight = configWeight;
    }


    protected long[] getPayTable() {
        long[] jackpotPay = super.getPayTable();
        if (configWeight.getJackpotPay() != null && configWeight.getJackpotPay().length > 0) {
            jackpotPay = configWeight.getJackpotPay();
        }
        return jackpotPay;
    }

}
