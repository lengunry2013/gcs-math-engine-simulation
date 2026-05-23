package com.gcs.game.testengine.math.model20260520;

import com.gcs.game.engine.math.model20260520.Model20260520;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.testengine.math.model20260507.Model20260507Test;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IBaseReelsDefaultConfig;
import com.gcs.game.testengine.model.IConfigWeight;

/**
 * @author Jiangqx
 * @create 2025-09-30-8:52
 **/
public class Model20260520Test extends Model20260520 implements IConfigWeight, IBaseReelsDefaultConfig {

    public static ConfigWeight configWeight;

    private static boolean isInitWeight = true;

    public static int baseMysteryType = -1;

    public static int classType = 2;

    protected static int[][] BASE_MYSTERY_WEIGHT = new int[][]{
            {971, 29},   //88%
            {965, 35},   //90%
            {949, 51},   //92%
            {929, 71},   //94%
            {929, 71},   //96%
    };

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
        if (classType == 3) {
            return new long[][]{
                    {0, 0, 0, 0, 0},      // 1
                    {0, 0, 150, 500, 1500},  // 2
                    {0, 0, 100, 300, 600},  // 3
                    {0, 0, 75, 200, 400},  // 4
                    {0, 0, 50, 150, 300},  // 5

                    {0, 0, 40, 100, 200},    // 6
                    {0, 0, 30, 80, 120},    // 7
                    {0, 0, 20, 60, 100},    // 8
                    {0, 0, 10, 40, 50},     // 9
                    {0, 0, 5, 10, 15},     // 10

                    {0, 0, 0, 0, 0},      // 11
                    {0, 0, 0, 0, 0},      // 12
                    {0, 0, 0, 0, 0},      // 13
                    {0, 0, 0, 0, 0},      // 14
                    {0, 0, 0, 0, 0}       // 15
            };
        }
        return super.getPayTable();
    }


    @Override
    public void setConfigWeight(ConfigWeight configWeight) {
        this.configWeight = configWeight;
    }

    protected int[] getBaseMysteryFeatureWeight(SlotGameLogicBean gameSessionBean) {
        int[] result = super.getBaseMysteryFeatureWeight(gameSessionBean);
        if (classType == 3) {
            result = getBaseMysterWeight(gameSessionBean);
        }
        if (configWeight != null) {
            int[][] baseWeight = configWeight.getBaseWeight();
            if (baseWeight != null && baseWeight.length > 0) {
                result = baseWeight[0];
            }
        }
        return result;
    }

    public long maxTotalPay() {
        if (configWeight != null && configWeight.getTotalPayCap() > 0) {
            return configWeight.getTotalPayCap();
        }
        return super.maxTotalPay();
    }

    private int[] getBaseMysterWeight(SlotGameLogicBean gameSessionBean) {
        int payback = gameSessionBean.getPercentage();
        int[] result = BASE_MYSTERY_WEIGHT[0];
        switch (payback) {
            case 8800:
                result = BASE_MYSTERY_WEIGHT[0];
                break;
            case 9000:
                result = BASE_MYSTERY_WEIGHT[1];
                break;
            case 9200:
                result = BASE_MYSTERY_WEIGHT[2];
                break;
            case 9400:
                result = BASE_MYSTERY_WEIGHT[3];
                break;
            case 9600:
                result = BASE_MYSTERY_WEIGHT[4];
                break;
            default:
                break;

        }
        return result;
    }


    protected void initGameSymbols() {
        super.initGameSymbols();
        if (isInitWeight && configWeight != null) {
            int[][] baseWeight = configWeight.getBaseWeight();
            if (baseWeight != null && baseWeight.length > 1) {
                BASE_MYSTERY_FEATURE_WEIGHT = baseWeight[1];
            }
            if (baseWeight != null && baseWeight.length > 2) {
                BASE_WILD_REELS_WEIGHT = baseWeight[2];
            }
            if (baseWeight != null && baseWeight.length > 3) {
                BASE_STICKY_WILD_WEIGHT = baseWeight[3];
            }
            int[][] fsWeight = configWeight.getFsWeight();
            if (fsWeight != null && fsWeight.length > 1) {
                FS_WR_WEIGHT = fsWeight[1];
            }
            isInitWeight = false;
        }
    }

    protected int randomBaseGameMysteryType(SlotGameLogicBean gameSessionBean) {
        int baseGameMysteryType = super.randomBaseGameMysteryType(gameSessionBean);
        baseMysteryType = baseGameMysteryType;
        return baseGameMysteryType;
    }

    public int[][] getWildReels() {
        return WILD_REELS;
    }

    public int[][] getStickyWild() {
        return BASE_STICKY_WILD;
    }

    protected int[] getFreeSpinWeight() {
        if (Model20260507Test.configWeight != null) {
            int[][] fsWeight = Model20260507Test.configWeight.getFsWeight();
            if (fsWeight != null && fsWeight.length > 0) {
                FS_WEIGHT = fsWeight[0];
            }
        }
        return FS_WEIGHT;
    }

    protected int[] getFreeSpinStickyWeight() {
        if (Model20260507Test.configWeight != null) {
            int[][] fsWeight = Model20260507Test.configWeight.getFsWeight();
            if (fsWeight != null && fsWeight.length > 2) {
                FS_STICKY_WILD_WEIGHT = fsWeight[2];
            }
        }
        return FS_STICKY_WILD_WEIGHT;
    }

    protected int[] getFreeSpinRandomWeight() {
        if (Model20260507Test.configWeight != null) {
            int[][] fsWeight = Model20260507Test.configWeight.getFsWeight();
            if (fsWeight != null && fsWeight.length > 3) {
                FS_RANDOM_WILD_WEIGHT = fsWeight[3];
            }
        }
        return FS_RANDOM_WILD_WEIGHT;
    }


}
