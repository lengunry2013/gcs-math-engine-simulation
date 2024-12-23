package com.gcs.game.testengine.math.model6060630;

import com.gcs.game.engine.math.model6060630.Model6060630Bonus;
import com.gcs.game.testengine.model.ConfigWeight;

public class Model6060630BonusTest extends Model6060630Bonus {

    protected int[] getAward() {
        ConfigWeight configWeight = Model6060630Test.configWeight;
        if (configWeight != null && configWeight.getBonusWeight() != null) {
            return configWeight.getBonusWeight()[0];
        }
        return ROUND_REWARD;
    }

}
