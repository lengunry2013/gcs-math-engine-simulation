package com.gcs.game.testengine.math.model8140802;

import com.gcs.game.engine.math.model8140802.Model8140802;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model8140802Test extends Model8140802 implements IConfigWeight, IBaseReelsDefaultConfig {
    private static ConfigWeight configWeight;

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
