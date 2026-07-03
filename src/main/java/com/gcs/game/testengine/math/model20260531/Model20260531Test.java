package com.gcs.game.testengine.math.model20260531;

import com.gcs.game.engine.math.model20260531.Model20260531;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model20260531Test extends Model20260531 implements IConfigWeight, IBaseReelsDefaultConfig {

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

}
