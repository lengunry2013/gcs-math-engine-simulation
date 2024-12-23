package com.gcs.game.testengine.math.model6080630;

import com.gcs.game.engine.math.model6080630.Model6080630Bonus;
import com.gcs.game.testengine.model.ConfigWeight;

public class Model6080630BonusTest extends Model6080630Bonus {

    protected int[] getAward() {
        ConfigWeight configWeight = Model6080630Test.configWeight;
        if (configWeight != null && configWeight.getBonusWeight() != null) {
            return configWeight.getBonusWeight()[0];
        }
        return ROUND_REWARD;
    }

}
