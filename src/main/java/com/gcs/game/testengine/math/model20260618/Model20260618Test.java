package com.gcs.game.testengine.math.model20260618;

import com.gcs.game.engine.math.model20260618.Model20260618;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model20260618Test extends Model20260618 implements IConfigWeight, IBaseReelsDefaultConfig {

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

    protected int[] getWheelBonusWeight() {
        int[] result = super.getWheelBonusWeight();
        if (configWeight != null) {
            int[][] bonusWeight = configWeight.getBonusWeight();
            if (bonusWeight != null && bonusWeight.length > 0) {
                result = bonusWeight[0];
            }
        }
        return result;
    }

    protected int[][] getScatterAwardWeight() {
        int[][] result = super.getScatterAwardWeight();
        if (configWeight != null) {
            int[][] fsWeight = configWeight.getFsWeight();
            if (fsWeight != null && fsWeight.length > 0) {
                result = fsWeight;
            }
        }
        return result;
    }


}
