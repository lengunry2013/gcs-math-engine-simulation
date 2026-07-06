package com.gcs.game.testengine.math.model20260701;

import com.gcs.game.engine.math.model20260625.Model20260625;
import com.gcs.game.engine.math.model20260701.Model20260701;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model20260701Test extends Model20260701 implements IConfigWeight, IBaseReelsDefaultConfig {

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
