package com.gcs.game.testengine.math.model1260130;

import com.gcs.game.engine.math.model1260130.Model1260130;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model1260130Test extends Model1260130 implements IConfigWeight, IBaseReelsDefaultConfig {
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
        return super.getPayTable();
    }

    protected int[] getBaseGameMultiplierWeight(int payback) {
        int[] result = super.getBaseGameMultiplierWeight(payback);
        if (configWeight != null) {
            int[][] baseWeight = configWeight.getBaseWeight();
            if (baseWeight != null && baseWeight.length > 0) {
                result = baseWeight[0];
            }
        }
        return result;
    }
}
