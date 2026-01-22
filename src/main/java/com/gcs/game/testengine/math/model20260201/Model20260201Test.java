package com.gcs.game.testengine.math.model20260201;

import com.gcs.game.engine.math.model20260201.Model20260201;
import com.gcs.game.engine.math.model8140802.Model8140802;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model20260201Test extends Model20260201 implements IConfigWeight, IBaseReelsDefaultConfig {

    public static ConfigWeight configWeight;

    @Override
    public void setConfigWeight(ConfigWeight configWeight) {
        this.configWeight = configWeight;
    }

    @Override
    public int getReelsCount() {
        return super.reelsCount();
    }

    @Override
    public int getRowsCount() {
        return super.rowsCount();
    }

    @Override
    public long[][] getPayTable() {
        if (configWeight != null && configWeight.getDynamicPayTable() != null) {
            return configWeight.getDynamicPayTable();
        }
        return super.getPayTable();
    }


    protected int[] wildMysteryMultiplierWeight(int payback) {
        if (configWeight != null && configWeight.getBaseWeight() != null) {
            int[][] baseWeight = configWeight.getBaseWeight();
            if (baseWeight.length > 0) {
                return baseWeight[0];
            }
        }
        return super.wildMysteryMultiplierWeight(payback);
    }
}
