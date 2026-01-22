package com.gcs.game.testengine.math.model20260201;


import com.gcs.game.engine.math.model20260201.Model20260201Bonus;
import com.gcs.game.testengine.model.ConfigWeight;

public class Model20260201BonusTest extends Model20260201Bonus {


    protected int[] getPickAwardWeight() {
        ConfigWeight configWeight = Model20260201Test.configWeight;
        if (configWeight != null && configWeight.getBonusWeight() != null
                && configWeight.getBonusWeight().length > 0) {
            return configWeight.getBonusWeight()[0];
        }
        return super.getPickAwardWeight();
    }

}
