package com.gcs.game.testengine.math.model20260201;


import com.gcs.game.engine.math.model20260201.Model20260201;
import com.gcs.game.engine.math.model20260201.Model20260201FSBonus;
import com.gcs.game.testengine.model.ConfigWeight;

public class Model20260201FSBonusTest extends Model20260201FSBonus {


    protected int[] getGroundScriptFSWeight() {
        ConfigWeight configWeight = Model20260201Test.configWeight;
        if (configWeight != null && configWeight.getFsWeight() != null
                && configWeight.getFsWeight().length > 2) {
            return configWeight.getFsWeight()[2];
        }
        return Model20260201.GROUND_FREESPIN_SCRIPT_WEIGHT;
    }

    protected int[] getLightingScriptFSWeight() {
        ConfigWeight configWeight = Model20260201Test.configWeight;
        if (configWeight != null && configWeight.getFsWeight() != null
                && configWeight.getFsWeight().length > 1) {
            return configWeight.getFsWeight()[1];
        }
        return Model20260201.LIGHTNING_FREESPIN_SCRIPT_WEIGHT;
    }


}
