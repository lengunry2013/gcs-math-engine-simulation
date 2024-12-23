package com.gcs.game.testengine.math.model5070530;

import com.gcs.game.engine.keno.vo.KenoGameLogicBean;
import com.gcs.game.engine.math.model5070530.Model5070530;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model5070530Test extends Model5070530 implements IConfigWeight {
    public static ConfigWeight configWeight;

    @Override
    public void setConfigWeight(ConfigWeight configWeight) {
        this.configWeight = configWeight;
    }


    @Override
    public long[][] getPayTable(KenoGameLogicBean gameLogicBean) {
        if (configWeight != null && configWeight.getPayTables() != null) {
            double[][] payTables = configWeight.getPayTables();
            long[][] payTableResult = computePayTables(payTables, gameLogicBean);
            return payTableResult;
        }
        return super.getPayTable(gameLogicBean);
    }

    public long maxTotalPay() {
        if (configWeight != null) {
            return configWeight.getTotalPayCap();
        }
        return 80000;
    }

    protected int[][] getFsTimes() {
        if (configWeight != null && configWeight.getFsTimes() != null) {
            return configWeight.getFsTimes();
        }
        return super.getFsTimes();
    }

    protected int[][] getFsWeight() {
        if (configWeight != null && configWeight.getFsWeight() != null) {
            return configWeight.getFsWeight();
        }
        return super.getFsWeight();
    }

    protected int[][] getFs3setsTimes() {
        if (configWeight != null && configWeight.getFs3SetsTimes() != null) {
            return configWeight.getFs3SetsTimes();
        }
        return super.getFs3setsTimes();
    }

    protected int[][] getFs3setsWeight() {
        if (configWeight != null && configWeight.getFs3SetsWeight() != null) {
            return configWeight.getFs3SetsWeight();
        }
        return super.getFs3setsWeight();
    }

    protected int[][] getFs4setsTimes() {
        if (configWeight != null && configWeight.getFs4SetsTimes() != null) {
            return configWeight.getFs4SetsTimes();
        }
        return super.getFs4setsTimes();
    }

    protected int[][] getFs4setsWeight() {
        if (configWeight != null && configWeight.getFs4SetsWeight() != null) {
            return configWeight.getFs4SetsWeight();
        }
        return super.getFs4setsWeight();
    }

}
