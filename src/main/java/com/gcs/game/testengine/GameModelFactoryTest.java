package com.gcs.game.testengine;

import com.gcs.game.engine.GameModelFactory;
import com.gcs.game.engine.blackJack.model.BaseBlackJackModel;
import com.gcs.game.engine.keno.model.BaseKenoModel;
import com.gcs.game.engine.math.model20260201.Model20260201Bonus;
import com.gcs.game.engine.math.model20260201.Model20260201FSBonus;
import com.gcs.game.engine.math.model5070530.Model5070530;
import com.gcs.game.engine.poker.bonus.PokerBonus;
import com.gcs.game.engine.poker.model.BasePokerModel;
import com.gcs.game.engine.slots.bonus.BaseBonus;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.testengine.math.model1010802.Model1010802Test;
import com.gcs.game.testengine.math.model1260130.Model1260130BonusTest;
import com.gcs.game.testengine.math.model1260130.Model1260130Test;
import com.gcs.game.testengine.math.model20260201.Model20260201BonusTest;
import com.gcs.game.testengine.math.model20260201.Model20260201FSBonusTest;
import com.gcs.game.testengine.math.model20260201.Model20260201Test;
import com.gcs.game.testengine.math.model5070530.Model5070530Test;
import com.gcs.game.testengine.math.model6060630.Model6060630BonusTest;
import com.gcs.game.testengine.math.model6060630.Model6060630Test;
import com.gcs.game.testengine.math.model6080630.Model6080630BonusTest;
import com.gcs.game.testengine.math.model6080630.Model6080630Test;
import com.gcs.game.testengine.math.model8140802.Model8140802Test;
import com.gcs.game.testengine.math.modelGCBJ00101.ModelGCBJ00101Test;
import com.gcs.game.testengine.math.modelGCBJ00102.ModelGCBJ00102Test;

public class GameModelFactoryTest extends GameModelFactory {
    public BaseBlackJackModel getBlackJackModel(String mathModel) {
        BaseBlackJackModel model = null;
        switch (mathModel) {
            case "GCBJ00101":
                model = new ModelGCBJ00101Test();
                break;
            case "GCBJ00102":
                model = new ModelGCBJ00102Test();
                break;
            default:
                break;
        }
        return model;
    }

    public BasePokerModel getPokerModel(String mathModel) {
        BasePokerModel model = null;
        switch (mathModel) {
            case "6080630":
                model = new Model6080630Test();
                break;
            case "6060630":
                model = new Model6060630Test();
                break;
            default:
                break;
        }
        return model;
    }

    public BaseSlotModel getSlotsModel(String mathModel) {
        BaseSlotModel model = null;
        switch (mathModel) {
            case "8140802":
                model = new Model8140802Test();
                break;
            case "1260130":
                model = new Model1260130Test();
                break;
            case "1010802":
                model = new Model1010802Test();
                break;
            case "20260201":
                model = new Model20260201Test();
                break;
            default:
                break;
        }
        return model;
    }

    public BaseBonus getSlotsBonusModel(String gameModel, String bonusAsset) {
        BaseBonus model = null;
        switch (gameModel) {
            case "1260130":
                model = new Model1260130BonusTest();
                break;
            case "20260201":
                if ("bonus1".equalsIgnoreCase(bonusAsset)) {
                    model = new Model20260201FSBonusTest();
                } else if ("bonus2".equalsIgnoreCase(bonusAsset)) {
                    model = new Model20260201BonusTest();
                }
                break;
            default:
                break;
        }
        return model;
    }

    public PokerBonus getPokerBonusModel(String gameModel, String bonusAsset) {
        PokerBonus bonusModel = null;
        switch (gameModel) {
            case "6080630":
                bonusModel = new Model6080630BonusTest();
                break;
            case "6060630":
                bonusModel = new Model6060630BonusTest();
                break;
            default:
                break;
        }
        return bonusModel;
    }

    public BaseKenoModel getKenoModel(String mathModel) {
        BaseKenoModel model = null;
        switch (mathModel) {
            case "5070530":
                model = new Model5070530Test();
                break;
            default:
                break;
        }
        return model;
    }

}
