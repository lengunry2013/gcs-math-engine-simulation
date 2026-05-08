package com.gcs.game.testengine.math.model20260103;

import com.gcs.game.engine.math.model20260103.Model20260103;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model20260103Test extends Model20260103 implements IConfigWeight, IBaseReelsDefaultConfig {

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

    protected int[] getBaseReelsWeight(int payBack) {
        int[] baseReelsWeight = super.getBaseReelsWeight(payBack);
        if (configWeight != null) {
            int[][] baseWeight = configWeight.getBaseWeight();
            if (baseWeight != null && baseWeight.length > 0) {
                baseReelsWeight = baseWeight[0];
            }

        }
        return baseReelsWeight;
    }

    protected int[] getFsReelsWeight(int payBack) {
        int[] result = super.getFsReelsWeight(payBack);
        if (configWeight != null) {
            int[][] fsWeight = configWeight.getFsWeight();
            if (fsWeight != null && fsWeight.length > 0) {
                result = fsWeight[0];
            }
        }
        return result;
    }


}
