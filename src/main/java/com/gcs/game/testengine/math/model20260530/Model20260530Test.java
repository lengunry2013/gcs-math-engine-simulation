package com.gcs.game.testengine.math.model20260530;

import com.gcs.game.engine.math.model20260530.Model20260530;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model20260530Test extends Model20260530 implements IConfigWeight, IBaseReelsDefaultConfig {

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
       /* if (configWeight != null && configWeight.getDynamicPayTable() != null) {
            return configWeight.getDynamicPayTable();
        }*/
        return super.getPayTable();
    }

}
