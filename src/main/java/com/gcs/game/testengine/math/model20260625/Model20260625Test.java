package com.gcs.game.testengine.math.model20260625;

import com.gcs.game.engine.math.model20260625.Model20260625;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.testengine.model.IConfigWeight;

public class Model20260625Test extends Model20260625 implements IConfigWeight, IBaseReelsDefaultConfig {

    public static ConfigWeight configWeight;
    public static final int[] INIT_JACKPOT_METER = new int[]{500, 2500, 12500, 25000};
    public static final int[] MAX_JACKPOT_METER = new int[]{2000, 10000, 25000, 50000};
    public static final double[] CONTRIBUTION_RATE = new double[]{0.0005, 0.0005, 0.0005, 0.00025};

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
        if (configWeight != null && configWeight.getBaseWeight() != null) {
            int[][] baseWeights = configWeight.getBaseWeight();
            if (baseWeights.length > 0) {
                baseReelsWeight = baseWeights[0];
            }
        }
        return baseReelsWeight;
    }

    public int[] getJackpotInitMeter() {
        if (configWeight != null && configWeight.getBonusWeight() != null) {
            int[][] bonusWeight = configWeight.getBonusWeight();
            if (bonusWeight.length > 0) {
                return bonusWeight[0];
            }
        }
        return INIT_JACKPOT_METER;
    }

    public int[] getJackpotMaxMeter() {
        if (configWeight != null && configWeight.getBonusWeight() != null) {
            int[][] bonusWeight = configWeight.getBonusWeight();
            if (bonusWeight.length > 1) {
                return bonusWeight[1];
            }
        }
        return MAX_JACKPOT_METER;
    }

    public double[] getJackpotContributionRate() {
        double[] rate = CONTRIBUTION_RATE.clone();
        if (configWeight != null && configWeight.getBonusWeight() != null) {
            int[][] bonusWeight = configWeight.getBonusWeight();
            if (bonusWeight.length > 2) {
                for (int i = 0; i < bonusWeight[2].length; i++) {
                    rate[i] = bonusWeight[2][i] / 100.00 / 1000;
                }
            }
        }
        return rate;
    }

}
