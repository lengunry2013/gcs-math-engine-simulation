package com.gcs.game.testengine.math.model1010802;

import com.gcs.game.engine.math.model1010802.Model1010802;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model1010802Test extends Model1010802 implements IConfigWeight, IBaseReelsDefaultConfig {
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
        long[][] payTables = super.getPayTable();
        if (configWeight != null && configWeight.getDynamicPayTable() != null) {
            payTables = configWeight.getDynamicPayTable();
        }
        return payTables;
    }

    public int[] getBaseReelSetWeight() {
        int[] result = super.getBaseReelSetWeight();
        if (configWeight != null) {
            int[][] baseWeight = configWeight.getBaseWeight();
            if (baseWeight != null && baseWeight.length > 0) {
                result = baseWeight[0];
            }
        }
        return result;
    }

    public int[] getBaseIncreaseMul() {
        int[] result = super.getBaseIncreaseMul();
        if (configWeight != null) {
            int[][] baseWeight = configWeight.getBaseWeight();
            if (baseWeight != null && baseWeight.length > 1) {
                result = baseWeight[1];
            }
        }
        return result;
    }

    public int[] getFsReelSetWeight() {
        int[] result = super.getFsReelSetWeight();
        if (configWeight != null) {
            int[][] fsWeight = configWeight.getFsWeight();
            if (fsWeight != null && fsWeight.length > 0) {
                result = fsWeight[0];
            }
        }
        return result;
    }

    public int[] getFsIncreaseMul() {
        int[] result = super.getFsIncreaseMul();
        if (configWeight != null) {
            int[][] fsWeight = configWeight.getFsWeight();
            if (fsWeight != null && fsWeight.length > 1) {
                result = fsWeight[1];
            }
        }
        return result;
    }

    public int[] fsTimes() {
        int[] result = super.fsTimes();
        if (configWeight != null) {
            int[][] fsWeight = configWeight.getFsWeight();
            if (fsWeight != null && fsWeight.length > 2) {
                result = fsWeight[2];
            }
        }
        return result;
    }

    public int[] fsInFsTimes() {
        int[] result = super.fsInFsTimes();
        if (configWeight != null) {
            int[][] fsWeight = configWeight.getFsWeight();
            if (fsWeight != null && fsWeight.length > 3) {
                result = fsWeight[3];
            }
        }
        return result;
    }

    public long maxWin() {
        long maxPay = super.maxWin();
        if (configWeight != null) {
            maxPay = configWeight.getTotalPayCap();
        }
        return maxPay;
    }

}
