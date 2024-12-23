package com.gcs.game.simulation;

import com.gcs.game.engine.GameEngineFactory;
import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.blackJack.model.BaseBlackJackModel;
import com.gcs.game.engine.keno.model.BaseKenoModel;
import com.gcs.game.engine.math.model1010802.Model1010802Engine;
import com.gcs.game.engine.math.model1260130.Model1260130Engine;
import com.gcs.game.engine.math.model5070530.Model5070530Engine;
import com.gcs.game.engine.math.model6060630.Model6060630Engine;
import com.gcs.game.engine.math.model6080630.Model6080630Engine;
import com.gcs.game.engine.math.modelGCBJ00101.ModelGCBJ00101Engine;
import com.gcs.game.engine.math.modelGCBJ00102.ModelGCBJ00102Engine;
import com.gcs.game.engine.poker.model.BasePokerModel;
import com.gcs.game.engine.slots.model.BaseSlotModel;
import com.gcs.game.exception.InvalidBetException;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.exception.InvalidPlayerInputException;
import com.gcs.game.simulation.blackJack.engine.BlackJackEngineResult;
import com.gcs.game.simulation.blackJack.vo.BlackJackConfigInfo;
import com.gcs.game.simulation.keno.engine.KenoEngineResult;
import com.gcs.game.simulation.keno.vo.KenoConfigInfo;
import com.gcs.game.simulation.poker.engine.PokerEngineResult;
import com.gcs.game.simulation.poker.vo.PokerConfigInfo;
import com.gcs.game.simulation.slot.engine.BaseReelsGameSpinResult;
import com.gcs.game.simulation.slot.engine.BaseReelsGameSymbolSpinResult;
import com.gcs.game.simulation.slot.engine.EsqueletoExplosivoSpinResult;
import com.gcs.game.simulation.slot.engine.LittleDragonBunsSpinResult;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.InputConfigReader;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.GameModelFactoryTest;
import com.gcs.game.testengine.model.ConfigWeight;
import com.gcs.game.testengine.model.IConfigWeight;
import com.gcs.game.vo.BaseGameLogicBean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameSimulator {
    private static String simulation_version_info = "20241223_V1.0";

    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equals("-version")) {
                log.debug(simulation_version_info);
            }
        } else {
            BaseConfigInfo configInfo = InputConfigReader.getInstance().readConfigInfo();
            Thread t = new SimulationThread(configInfo);
            t.start();
        }
    }

    private static class SimulationThread extends Thread {

        private BaseConfigInfo configInfo;

        public SimulationThread(BaseConfigInfo configInfo) {
            this.configInfo = configInfo;
        }

        public void run() {
            simulation(configInfo);
        }
    }

    public static void simulation(BaseConfigInfo configInfo) {
        try {
            String mmID = configInfo.getModel();
            int payback = configInfo.getPayback();
            int outputInfoType = configInfo.getOutputType();
            // init simulation file
            FileWriteUtil.simulationDataOutputPath(configInfo);
            FileWriteUtil.createNewFile(configInfo.getOutputFileName());
            BaseGameLogicBean gameLogicBean = null;
            IGameEngine engine = GameEngineFactory.getGameEngine(payback, mmID);
            if (engine != null) {
                gameLogicBean = engine.init(gameLogicBean);
                gameLogicBean.setDenom(configInfo.getDenom());
                gameLogicBean.setJackpotGroupCode(configInfo.getJackpotGroupCode());
                if (gameLogicBean != null) {
                    if (outputInfoType == BaseConstant.ALL_INFO_OUTPUT_TYPE) {
                        if (engine instanceof ModelGCBJ00101Engine || engine instanceof ModelGCBJ00102Engine) {
                            BaseBlackJackModel blackJackModel = GameModelFactoryTest.getInstance().getBlackJackModel(mmID);
                            setBlackJackConfigWeight(configInfo, blackJackModel);
                            BlackJackEngineResult blackJackEngineResult = new BlackJackEngineResult();
                            blackJackEngineResult.DealResult(engine, gameLogicBean, configInfo, blackJackModel);
                        } else if ("Slots".equalsIgnoreCase(configInfo.getGameClass())) {
                            BaseSlotModel slotModel = GameModelFactoryTest.getInstance().getSlotsModel(mmID);
                            setSlotConfigWeight(configInfo, slotModel);
                            if (engine instanceof Model1260130Engine) {
                                LittleDragonBunsSpinResult spinResult = new LittleDragonBunsSpinResult();
                                spinResult.cycleSpinForLittleDragonBuns(engine, gameLogicBean, configInfo, slotModel);
                            } else if (engine instanceof Model1010802Engine) {
                                EsqueletoExplosivoSpinResult spinResult = new EsqueletoExplosivoSpinResult();
                                spinResult.cycleSpinForEsqueletoExplosivo(engine, gameLogicBean, configInfo, slotModel);
                            }
                            //TODO
                        } else if (engine instanceof Model6080630Engine || engine instanceof Model6060630Engine) {
                            BasePokerModel pokerModel = GameModelFactoryTest.getInstance().getPokerModel(mmID);
                            setPokerConfigWeight(configInfo, pokerModel);
                            PokerEngineResult pokerEngineResult = new PokerEngineResult();
                            pokerEngineResult.DealsResult(engine, gameLogicBean, configInfo, pokerModel);
                        } else if (engine instanceof Model5070530Engine) {
                            BaseKenoModel kenoModel = GameModelFactoryTest.getInstance().getKenoModel(mmID);
                            setKenoConfigWeight(configInfo, kenoModel);
                            KenoEngineResult kenoEngineResult = new KenoEngineResult();
                            kenoEngineResult.spinResult(engine, gameLogicBean, configInfo, kenoModel);
                        }
                    } else if (outputInfoType == BaseConstant.WIN_PAY_OUTPUT_TYPE) {
                        if ("Slots".equalsIgnoreCase(configInfo.getGameClass())) {
                            BaseSlotModel slotModel = GameModelFactoryTest.getInstance().getSlotsModel(mmID);
                            setSlotConfigWeight(configInfo, slotModel);
                            BaseReelsGameSpinResult spinResult = new BaseReelsGameSpinResult();
                            spinResult.cycleSpinForBaseReelsGame(engine, gameLogicBean, configInfo, slotModel);
                        }
                    }else if (outputInfoType == BaseConstant.BASE_SPIN_OUTPUT_TYPE) {
                        if ("Slots".equalsIgnoreCase(configInfo.getGameClass())) {
                            BaseSlotModel slotModel = GameModelFactoryTest.getInstance().getSlotsModel(mmID);
                            setSlotConfigWeight(configInfo, slotModel);
                            BaseReelsGameSymbolSpinResult spinResult = new BaseReelsGameSymbolSpinResult();
                            spinResult.cycleSpinForBaseReelsGameSymbol(engine, gameLogicBean, configInfo, slotModel);
                        }
                    }
                }
                //log.debug("===============simulation End=============");
                System.out.println("===============simulation End=============");
            }
        } catch (OutOfMemoryError e) {
            log.error("OutOfMemoryError", e);
        } catch (InvalidGameStateException e) {
            log.error("Invalid Game State", e);
        } catch (InvalidBetException e) {
            log.error("Invalid Bet Update", e);
        } catch (InvalidPlayerInputException e) {
            log.error("Invalid Player Input", e);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setKenoConfigWeight(BaseConfigInfo configInfo, BaseKenoModel model) {
        if (model instanceof IConfigWeight) {
            KenoConfigInfo kenoConfigInfo = (KenoConfigInfo) configInfo;
            ConfigWeight configWeight = new ConfigWeight();
            configWeight.setFsTimes(kenoConfigInfo.getFsTimes());
            configWeight.setFsWeight(kenoConfigInfo.getFsWeight());
            configWeight.setFs3SetsTimes(kenoConfigInfo.getFs3SetsTimes());
            configWeight.setFs3SetsWeight(kenoConfigInfo.getFs3SetsWeight());
            configWeight.setFs4SetsTimes(kenoConfigInfo.getFs4SetsTimes());
            configWeight.setFs4SetsWeight(kenoConfigInfo.getFs4SetsWeight());
            configWeight.setTotalPayCap(kenoConfigInfo.getTotalPayCap());
            configWeight.setPayTables(kenoConfigInfo.getPayTable());
            ((IConfigWeight) model).setConfigWeight(configWeight);
        }
    }

    private static void setPokerConfigWeight(BaseConfigInfo configInfo, BasePokerModel model) {
        if (model instanceof IConfigWeight) {
            PokerConfigInfo pokerConfigInfo = (PokerConfigInfo) configInfo;
            ConfigWeight configWeight = new ConfigWeight();
            configWeight.setBaseWeight(pokerConfigInfo.getBaseWeight());
            configWeight.setFsWeight(pokerConfigInfo.getFsWeight());
            configWeight.setBonusWeight(pokerConfigInfo.getBonusWeight());
            configWeight.setGoldCardTriggerWeight(pokerConfigInfo.getGoldCardTriggerWeight());
            configWeight.setInstantCashPayWeight(pokerConfigInfo.getInstantCashPayWeight());
            configWeight.setPayTable(pokerConfigInfo.getPayTable());
            configWeight.setTotalPayCap(pokerConfigInfo.getTotalPayCap());
            configWeight.setFsMulWeight(pokerConfigInfo.getFsMulWeight());
            ((IConfigWeight) model).setConfigWeight(configWeight);
        }
    }

    protected static void setBlackJackConfigWeight(BaseConfigInfo configInfo, BaseBlackJackModel model) {
        if (model instanceof IConfigWeight) {
            BlackJackConfigInfo blackJackConfigInfo = (BlackJackConfigInfo) configInfo;
            ConfigWeight configWeight = new ConfigWeight();
            configWeight.setJackpotPay(blackJackConfigInfo.getJackpotPay());
            configWeight.setJackpotWeight(blackJackConfigInfo.getJackpotWeight());
            ((IConfigWeight) model).setConfigWeight(configWeight);
        }
    }

    protected static void setSlotConfigWeight(BaseConfigInfo configInfo, BaseSlotModel model) {
        if (model instanceof IConfigWeight) {
            SlotConfigInfo slotConfigInfo = (SlotConfigInfo) configInfo;
            ConfigWeight configWeight = new ConfigWeight();
            configWeight.setBaseWeight(slotConfigInfo.getBaseWeight());
            configWeight.setFsWeight(slotConfigInfo.getFsWeight());
            configWeight.setBonusWeight(slotConfigInfo.getBonusWeight());
            configWeight.setDynamicPayTable(slotConfigInfo.getSymbolPayTable());
            configWeight.setTotalPayCap(slotConfigInfo.getTotalPayCap());
            configWeight.setDynamicPayTable(slotConfigInfo.getPayTables());
            //configWeight.setContributionPercent(slotConfigInfo.getContributionPercent());
            //configWeight.setLevelDistribute(slotConfigInfo.getLevelDistribute());
            ((IConfigWeight) model).setConfigWeight(configWeight);
        }
    }

}
