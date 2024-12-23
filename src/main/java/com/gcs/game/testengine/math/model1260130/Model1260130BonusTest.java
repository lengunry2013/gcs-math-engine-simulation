package com.gcs.game.testengine.math.model1260130;

import com.gcs.game.engine.math.model1260130.Model1260130Bonus;
import com.gcs.game.testengine.model.ConfigWeight;

public class Model1260130BonusTest extends Model1260130Bonus {
    @Override
    protected int[][] getAwardWeight() {
        int[][] result = super.getAwardWeight();
        ConfigWeight configWeight = Model1260130Test.configWeight;
        if (configWeight != null) {
            int[][] bonusWeight = configWeight.getBonusWeight();
            if (bonusWeight != null && bonusWeight.length > 0) {
                result = new int[][]{
                        bonusWeight[0],
                };
            }
        }
        return result;
    }


    @Override
    protected int[][] getMultiplierWeight() {
        int[][] result = super.getMultiplierWeight();
        ConfigWeight configWeight = Model1260130Test.configWeight;
        if (configWeight != null) {
            int[][] bonusWeight = configWeight.getBonusWeight();
            if (bonusWeight != null && bonusWeight.length > 1) {
                result = new int[][]{
                        bonusWeight[1],
                };
            }
        }
        return result;
    }

}
