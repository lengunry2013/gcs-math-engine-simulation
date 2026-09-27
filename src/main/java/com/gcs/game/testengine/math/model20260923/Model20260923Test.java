package com.gcs.game.testengine.math.model20260923;

import com.gcs.game.engine.keno.vo.KenoGameLogicBean;
import com.gcs.game.engine.math.model20260923.Model20260923;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model20260923Test extends Model20260923 implements IConfigWeight {
    public static ConfigWeight configWeight;

    @Override
    public void setConfigWeight(ConfigWeight configWeight) {
        this.configWeight = configWeight;
    }


    @Override
    public long[][] getPayTable(KenoGameLogicBean gameLogicBean) {
        return super.getPayTable(gameLogicBean);
    }

    public long maxTotalPay() {
        if (configWeight != null) {
            return configWeight.getTotalPayCap();
        }
        return 80000;
    }

}
