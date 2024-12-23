package com.gcs.game.simulation.poker.engine;

import com.alibaba.fastjson.JSON;
import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.poker.model.BasePokerModel;
import com.gcs.game.engine.poker.utils.PokerGameConstant;
import com.gcs.game.engine.poker.vo.PokerBonusResult;
import com.gcs.game.engine.poker.vo.PokerGameLogicBean;
import com.gcs.game.engine.poker.vo.PokerROrBBonusResult;
import com.gcs.game.engine.poker.vo.PokerResult;
import com.gcs.game.exception.InvalidBetException;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.exception.InvalidPlayerInputException;
import com.gcs.game.simulation.poker.vo.PokerConfigInfo;
import com.gcs.game.simulation.poker.vo.PokerResultInfo;
import com.gcs.game.simulation.slot.engine.GameEngineCompute;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.model6060630.Model6060630BonusTest;
import com.gcs.game.testengine.math.model6060630.Model6060630Test;
import com.gcs.game.testengine.math.model6080630.Model6080630BonusTest;
import com.gcs.game.testengine.math.model6080630.Model6080630Test;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.vo.BaseGameLogicBean;
import com.gcs.game.vo.PlayerInputInfo;

import java.util.*;

public class PokerEngineResult {
    public PokerEngineResult() {

    }

    public void DealsResult(IGameEngine engine, BaseGameLogicBean gameLogicBean, BaseConfigInfo configInfo, BasePokerModel pokerModel) throws InvalidGameStateException, InvalidBetException, InvalidPlayerInputException {
        PokerConfigInfo pokerConfigInfo = (PokerConfigInfo) configInfo;
        PokerGameLogicBean pokerGameLogicBean = (PokerGameLogicBean) gameLogicBean;
        PokerResultInfo resultInfo = new PokerResultInfo();
        long spinCount = 0L;
        long simulationCount = configInfo.getSimulationCount();
        int playTime = configInfo.getPlayTimesPerPlayer();
        long initCredit = configInfo.getInitCredit();
        resultInfo.setDenom(configInfo.getDenom());
        initPokersWinInfo(resultInfo);
        PokerResult pokerResult;
        long totalWon;
        FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), StringUtil.getPokersHeadInfo(configInfo, pokerModel));
        for (int i = 0; i < simulationCount; i++) {
            //deal
            spinCount++;
            totalWon = 0L;
            Map gameLogicMap = new LinkedHashMap();
            gameLogicMap.put("lines", pokerConfigInfo.getLines());
            gameLogicMap.put("bet", pokerConfigInfo.getBet());
            gameLogicMap.put("denom", configInfo.getDenom());
            pokerGameLogicBean = (PokerGameLogicBean) engine.gameStart(pokerGameLogicBean, gameLogicMap, null);
            int gamePlayStatus = pokerGameLogicBean.getGamePlayStatus();
            Map<String, String> engineContextMap = engine.getEngineContext();
            pokerResult = pokerGameLogicBean.getPokerResult();
            long totalBet = gameLogicBean.getSumBetCredit();
            initCredit -= totalBet;

            long maxTotalPay = pokerModel.maxTotalPay();
            long winCredit;
            if (gamePlayStatus == GameConstant.POKER_GAME_STATUS_SWITCH_CARD) {
                List<Integer> holdPositions = getHoldPositions(pokerResult, pokerModel);
                //System.out.println("holdPositions=" + JSON.toJSONString(holdPositions));
                gameLogicMap.put("holdPositions", holdPositions);
                pokerGameLogicBean = (PokerGameLogicBean) engine.gameProgress(pokerGameLogicBean, gameLogicMap, null, engineContextMap, null);
                pokerResult = pokerGameLogicBean.getPokerResult();
                winCredit = gameLogicBean.getSumWinCredit();
                List<Integer> goldHandPokers = pokerResult.getGoldHandPokers();
                /*boolean isMaxTotalPay = false;
                if (maxTotalPay > 0 && pokerResult.getPokerPay() >= maxTotalPay) {
                    isMaxTotalPay = true;
                }*/
                if (winCredit > 0 || (goldHandPokers != null && goldHandPokers.size() == 6)) {
                    resultInfo.setBaseTotalHit(resultInfo.getBaseTotalHit() + 1);
                    resultInfo.setBaseTotalWin(resultInfo.getBaseTotalWin() + pokerResult.getPokerPay());
                }
                resultInfo.setHandPokers(pokerResult.getHandPokers());
                int payType = pokerResult.getPokerPayType();
                resultInfo.getHandPayHit().set(payType, resultInfo.getHandPayHit().get(payType) + 1L);
                resultInfo.getHandPayWin().set(payType, resultInfo.getHandPayWin().get(payType) + pokerResult.getPokerPay());
                System.out.println("lastHandPokers=" + pokerResult.getHandPokers());
                if (pokerResult.getGoldHandPokers() != null && goldHandPokers.size() == 6 && goldHandPokers.contains(PokerGameConstant.GOLD_CARD)) {
                    resultInfo.setGoldCardTriggerHit(resultInfo.getGoldCardTriggerHit() + 1l);
                    int bonusTypeIndex = pokerResult.getGoldCardBonusType() - 1;
                    System.out.println("GoldType=" + bonusTypeIndex);
                    resultInfo.getGoldCardFeatureHit().set(bonusTypeIndex, resultInfo.getGoldCardFeatureHit().get(bonusTypeIndex) + 1);
                    //trigger instant cash
                    if (bonusTypeIndex == 1) {
                        long instantPay = pokerResult.getInstantCashPay() / totalBet;
                        if (pokerModel instanceof Model6080630Test) {
                            for (int j = 0; j < Model6080630Test.INSTANT_CASH_PAY_WEIGHT[0].length; j++) {
                                if (instantPay == Model6080630Test.INSTANT_CASH_PAY_WEIGHT[0][j]) {
                                    resultInfo.getInstantCashHit().set(j, resultInfo.getInstantCashHit().get(j) + 1l);
                                    resultInfo.getInstantCashWin().set(j, resultInfo.getInstantCashWin().get(j) + pokerResult.getInstantCashPay());
                                    break;
                                }
                            }
                        } else {
                            for (int j = 0; j < Model6060630Test.INSTANT_CASH_PAY_WEIGHT[0].length; j++) {
                                if (instantPay == Model6060630Test.INSTANT_CASH_PAY_WEIGHT[0][j]) {
                                    resultInfo.getInstantCashHit().set(j, resultInfo.getInstantCashHit().get(j) + 1l);
                                    resultInfo.getInstantCashWin().set(j, resultInfo.getInstantCashWin().get(j) + pokerResult.getInstantCashPay());
                                    break;
                                }
                            }
                        }

                        long tempInstantPay = pokerResult.getInstantCashPay();
                        resultInfo.setInstantTotalWin(resultInfo.getInstantTotalWin() + tempInstantPay);
                        resultInfo.getGoldCashFeatureWin().set(bonusTypeIndex, resultInfo.getGoldCashFeatureWin().get(bonusTypeIndex) + tempInstantPay);
                    }
                }
            }
            if (pokerGameLogicBean.getGamePlayStatus() != GameConstant.POKER_GAME_BONUS_STATUS_COMPLETE) {
                long fsCoinOut = 0L;
                long fsTotalTimes = 0L;
                while (true) {
                    if (pokerGameLogicBean.getGamePlayStatus() == GameConstant.POKER_GAME_STATUS_TRIGGER_FREESPIN) {
                        while (pokerGameLogicBean.getGamePlayStatus() == GameConstant.POKER_GAME_STATUS_TRIGGER_FREESPIN) {
                            PlayerInputInfo playerInput = new PlayerInputInfo();
                            playerInput.setRequestGameStatus(200);
                            if (pokerGameLogicBean.getPokerFsResult() != null && !pokerGameLogicBean.getPokerFsResult().isEmpty() && pokerGameLogicBean.getPokerFsResult().size() > 0) {
                                PokerResult fsPokerResult = pokerGameLogicBean.getPokerFsResult().get(pokerGameLogicBean.getPokerFsResult().size() - 1);
                                if (fsPokerResult.getPokerPlayStatus() == PokerGameConstant.POKER_STATUS_SWITCH_CARD) {
                                    List<Integer> holdPositions = getHoldPositions(fsPokerResult, pokerModel);
                                    gameLogicMap.put("holdPositions", holdPositions);
                                    pokerGameLogicBean = (PokerGameLogicBean) engine.gameProgress(pokerGameLogicBean, gameLogicMap, playerInput, engineContextMap, null);
                                    long freespinWon = fsPokerResult.getPokerPay();
                                    int fsMul = fsPokerResult.getFsMul();

                                    fsCoinOut += freespinWon;
                                    fsTotalTimes++;
                                    if (pokerModel instanceof Model6060630Test) {
                                        for (int fsMulIndex = 0; fsMulIndex < Model6060630Test.FS_MUL_WEIGHT[0].length; fsMulIndex++) {
                                            if (fsMul == Model6060630Test.FS_MUL_WEIGHT[0][fsMulIndex]) {
                                                resultInfo.getFsMulHit().set(fsMulIndex, resultInfo.getFsMulHit().get(fsMulIndex) + 1L);
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    pokerGameLogicBean = (PokerGameLogicBean) engine.gameProgress(pokerGameLogicBean, gameLogicMap, playerInput, engineContextMap, null);
                                    engineContextMap = engine.getEngineContext();
                                }
                            } else {
                                pokerGameLogicBean = (PokerGameLogicBean) engine.gameProgress(pokerGameLogicBean, gameLogicMap, playerInput, engineContextMap, null);
                                engineContextMap = engine.getEngineContext();
                            }
                        }
                        if (fsTotalTimes > 0) {
                            resultInfo.setFsTotalHit(resultInfo.getFsTotalHit() + 1);
                            resultInfo.setFsTotalTimes(resultInfo.getFsTotalTimes() + fsTotalTimes);
                            //每次game spin大于$800的话奖金只能是$800
                            resultInfo.setFsTotalWin(resultInfo.getFsTotalWin() + fsCoinOut);
                            resultInfo.getGoldCashFeatureWin().set(0, resultInfo.getGoldCashFeatureWin().get(0) + fsCoinOut);
                            //ShootingForRoyalPoker
                            if (pokerModel instanceof Model6080630Test) {
                                for (int fsIndex = 0; fsIndex < Model6080630Test.FS_TIMES_WEIGHT[0].length; fsIndex++) {
                                    if (fsTotalTimes == Model6080630Test.FS_TIMES_WEIGHT[0][fsIndex]) {
                                        resultInfo.getFsHit().set(fsIndex, resultInfo.getFsHit().get(fsIndex) + 1l);
                                        resultInfo.getFsTimes().set(fsIndex, resultInfo.getFsTimes().get(fsIndex) + fsTotalTimes);
                                        resultInfo.getFsWin().set(fsIndex, resultInfo.getFsWin().get(fsIndex) + fsCoinOut);
                                        break;
                                    }
                                }
                            } else {
                                for (int fsIndex = 0; fsIndex < Model6060630Test.FS_TIMES_WEIGHT[0].length; fsIndex++) {
                                    if (fsTotalTimes == Model6060630Test.FS_TIMES_WEIGHT[0][fsIndex]) {
                                        resultInfo.getFsHit().set(fsIndex, resultInfo.getFsHit().get(fsIndex) + 1l);
                                        resultInfo.getFsTimes().set(fsIndex, resultInfo.getFsTimes().get(fsIndex) + fsTotalTimes);
                                        resultInfo.getFsWin().set(fsIndex, resultInfo.getFsWin().get(fsIndex) + fsCoinOut);
                                        break;
                                    }
                                }
                            }

                        }
                    } else if (pokerGameLogicBean.getGamePlayStatus() == GameConstant.POKER_GAME_STATUS_TRIGGER_BONUS) {
                        computePickBonus(engine, pokerGameLogicBean, gameLogicMap, resultInfo, pokerConfigInfo, pokerModel);
                    } else if (pokerGameLogicBean.getGamePlayStatus() == GameConstant.GAME_STATUS_COMPLETE) {
                        break;
                    }
                }
            }
            totalWon += pokerGameLogicBean.getSumWinCredit();
            initCredit += totalWon;
            setBaseCommInfo(spinCount, initCredit, totalWon, pokerGameLogicBean, resultInfo);
            if (spinCount > 0 && spinCount % playTime == 0) {
                outResultInfo(pokerConfigInfo, resultInfo);
            }

        }
    }

    protected void outResultInfo(PokerConfigInfo pokerConfigInfo, PokerResultInfo resultInfo) {
        StringBuilder strContent = new StringBuilder();
        strContent.append(resultInfo.getSpinCount())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getLeftCredit())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getLines()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBet())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalBet()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getDenom()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalAmount())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalHit())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalCoinIn())
                .append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getTotalCoinOut())
                .append(BaseConstant.TAB_STR);
        double hitRate = resultInfo.getTotalHit() * 1.0
                / resultInfo.getSpinCount();
        strContent.append(hitRate).append(BaseConstant.TAB_STR);
        double payBack = resultInfo.getTotalCoinOut() * 1.0
                / resultInfo.getTotalCoinIn();
        strContent.append(payBack).append(BaseConstant.TAB_STR);
        strContent.append(JSON.toJSONString(resultInfo.getHandPokers())).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBaseTotalHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBaseTotalWin()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getGoldCardTriggerHit()).append(BaseConstant.TAB_STR);
        resultInfo.getGoldCardFeatureHit().forEach(goldHit -> {
            strContent.append(goldHit).append(BaseConstant.TAB_STR);
        });
        resultInfo.getGoldCashFeatureWin().forEach(goldWin -> {
            strContent.append(goldWin).append(BaseConstant.TAB_STR);
        });
        strContent.append(resultInfo.getFsTotalHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFsTotalTimes()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getFsTotalWin()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBonusTotalHit()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getBonusTotalWin()).append(BaseConstant.TAB_STR);
        strContent.append(resultInfo.getInstantTotalWin()).append(BaseConstant.TAB_STR);
        resultInfo.getInstantCashHit().forEach(instantHit -> {
            strContent.append(instantHit).append(BaseConstant.TAB_STR);
        });
        resultInfo.getInstantCashWin().forEach(instantWin -> {
            strContent.append(instantWin).append(BaseConstant.TAB_STR);
        });
        resultInfo.getFsHit().forEach(fsHit -> {
            strContent.append(fsHit).append(BaseConstant.TAB_STR);
        });
        resultInfo.getFsTimes().forEach(fsTimes -> {
            strContent.append(fsTimes).append(BaseConstant.TAB_STR);
        });
        resultInfo.getFsWin().forEach(fsWin -> {
            strContent.append(fsWin).append(BaseConstant.TAB_STR);
        });
        resultInfo.getROrBBonusHit().forEach(bonusHit -> {
            strContent.append(bonusHit).append(BaseConstant.TAB_STR);
        });
        resultInfo.getROrBBonusWin().forEach(bonusWin -> {
            strContent.append(bonusWin).append(BaseConstant.TAB_STR);
        });
        resultInfo.getHandPayHit().forEach(payHit -> {
            strContent.append(payHit).append(BaseConstant.TAB_STR);
        });
        resultInfo.getHandPayWin().forEach(payWin -> {
            strContent.append(payWin).append(BaseConstant.TAB_STR);
        });
        resultInfo.getFsMulHit().forEach(fsMulHit ->
                strContent.append(fsMulHit).append(BaseConstant.TAB_STR));
        FileWriteUtil.outputPrint(strContent.toString(), pokerConfigInfo.getOutputFileName(), pokerConfigInfo, 0);
    }

    protected List<Integer> getHoldPositions(PokerResult pokerResult, BasePokerModel pokerModel) {
        List<Integer> holdPositions = new ArrayList<>();
        List<Integer> handPokers = pokerResult.getHandPokers();
        long initPay = pokerResult.getInitPay();
        long initType = pokerResult.getInitPayType();
        if (initPay == 0) {
            List<Integer> flushPositions = getFlushPositions(handPokers, pokerModel);
            List<Integer> straightPosition = getStraightPositions(handPokers, pokerModel);
            List<Integer> twoOfKindPositions = getTwoOfKindPositions(handPokers, pokerModel);
            if (flushPositions != null && !flushPositions.isEmpty()) {
                holdPositions.addAll(flushPositions);
            } else if (straightPosition != null && !straightPosition.isEmpty()) {
                holdPositions.addAll(straightPosition);
            } else if (twoOfKindPositions != null && !twoOfKindPositions.isEmpty()) {
                holdPositions.addAll(twoOfKindPositions);
            } else {
                holdPositions = null;
            }
        } else {
            if (initType == PokerGameConstant.ACES_TYPE) {
                holdPositions.addAll(pokerModel.readAcesPositions(handPokers));
            } else if (initType == PokerGameConstant.TWO_PAIR_TYPE) {
                holdPositions.addAll(pokerModel.readTwoPairPositions(handPokers));
            } else if (initType == PokerGameConstant.THREE_OF_KIND_TYPE) {
                holdPositions.addAll(pokerModel.readThreeOfKindPositions(handPokers));
            } else {
                for (int i = 0; i < 5; i++) {
                    holdPositions.add(i + 1);
                }
            }

        }
        return holdPositions;
    }

    protected List<Integer> getStraightPositions(List<Integer> handPokers, BasePokerModel pokerModel) {
        int[] tempPokers = com.gcs.game.utils.StringUtil.ListToIntegerArray(handPokers);
        int match = 0;
        for (int i = 0; i < tempPokers.length; ++i) {
            tempPokers[i] = tempPokers[i] % PokerGameConstant.FLUSH_MAX_CARD;
        }
        Arrays.sort(tempPokers);
        List<Integer> matchPokers = new ArrayList<>();
        for (int i = 1; i < tempPokers.length; ++i) {
            if (tempPokers[i - 1] == (tempPokers[i] - 1)) {
                match++;
                if (matchPokers != null && !matchPokers.isEmpty()) {
                    if (!matchPokers.contains(tempPokers[i - 1])) {
                        matchPokers.add(tempPokers[i - 1]);
                    }
                    if (!matchPokers.contains(tempPokers[i])) {
                        matchPokers.add(tempPokers[i]);
                    }
                } else {
                    matchPokers.add(tempPokers[i]);
                    matchPokers.add(tempPokers[i - 1]);
                }
            } else {
                if (match >= 3) {
                    break;
                } else {
                    match = 0;
                    matchPokers.clear();
                }
            }
        }
        //A * 10 11 12
        if (match >= 2 && tempPokers[0] == 0 && tempPokers[2] == 10) {
            matchPokers.add(tempPokers[0]);
        } else if (match == 1 && tempPokers[0] == 0 && tempPokers[3] == 11 && tempPokers[4] == 12) {
            matchPokers.add(tempPokers[0]);
        }
        List<Integer> positions = null;
        if (matchPokers != null && !matchPokers.isEmpty() && matchPokers.size() >= 4) {
            positions = new ArrayList<>();
            for (int i = 0; i < handPokers.size(); i++) {
                int card = handPokers.get(i) % PokerGameConstant.FLUSH_MAX_CARD;
                for (int tempCard : matchPokers) {
                    if (card == tempCard) {
                        positions.add(i + 1);
                    }
                }
            }
        }
        return positions;
    }

    protected List<Integer> getFlushPositions(List<Integer> handPokers, BasePokerModel pokerModel) {
        List<Integer> positions = null;
        int[] flushArray = new int[4];
        for (int i = 0; i < handPokers.size(); ++i) {
            if (handPokers.get(i) / PokerGameConstant.FLUSH_MAX_CARD == PokerGameConstant.SPADE_CARD) {
                flushArray[PokerGameConstant.SPADE_CARD]++;
            } else if (handPokers.get(i) / PokerGameConstant.FLUSH_MAX_CARD == PokerGameConstant.HEARTS_CARD) {
                flushArray[PokerGameConstant.HEARTS_CARD]++;
            } else if (handPokers.get(i) / PokerGameConstant.FLUSH_MAX_CARD == PokerGameConstant.CLUBS_CARD) {
                flushArray[PokerGameConstant.CLUBS_CARD]++;
            } else {
                flushArray[PokerGameConstant.DIAMONDS_CARD]++;
            }
        }
        int flushIndex = -1;
        for (int i = 0; i < flushArray.length; i++) {
            if (flushArray[i] >= 4) {
                flushIndex = i;
                break;
            }
        }
        if (flushIndex >= 0) {
            positions = new ArrayList<>();
            for (int i = 0; i < handPokers.size(); i++) {
                if (handPokers.get(i) / PokerGameConstant.FLUSH_MAX_CARD == flushIndex) {
                    positions.add(i + 1);
                }
            }
        }
        return positions;
    }

    protected List<Integer> getTwoOfKindPositions(List<Integer> pokers, BasePokerModel pokerModel) {
        List<Integer> positions = null;
        int poker = -1;
        int[] pokersKind = pokerModel.kindSum(pokers);
        for (int i = 0; i < PokerGameConstant.FLUSH_MAX_CARD; ++i) {
            if (pokersKind[i] == PokerGameConstant.TWO_OF_KIND) {
                poker = i;
                break;
            }
        }
        if (poker >= 0) {
            positions = new ArrayList<>();
            for (int i = 0; i < pokers.size(); i++) {
                if (pokers.get(i) % PokerGameConstant.FLUSH_MAX_CARD == poker) {
                    positions.add(i + 1);
                }
            }
        }
        return positions;
    }

    private void computePickBonus(IGameEngine engine, PokerGameLogicBean pokerGameLogicBean, Map
            gameLogicMap, PokerResultInfo resultInfo, PokerConfigInfo pokerConfigInfo, BasePokerModel pokerModel) throws
            InvalidPlayerInputException, InvalidGameStateException {
        PlayerInputInfo playerInput = new PlayerInputInfo();
        playerInput.setRequestGameStatus(500);
        long bonusWon = 0L;
        List<Integer> cardList = null;
        for (int pick = 0; pick < 100; pick++) {
            if (pick > 0) {
                int[] picks = GameEngineCompute.initPokersArray(pick, cardList);
                playerInput.setBonusPickInfos(picks);
            }
            pokerGameLogicBean = (PokerGameLogicBean) engine.gameProgress(pokerGameLogicBean, gameLogicMap, playerInput, null, null);

            PokerBonusResult baseBonusResult = pokerGameLogicBean.getPokerBonusResult();
            if (baseBonusResult instanceof PokerROrBBonusResult) {
                cardList = ((PokerROrBBonusResult) baseBonusResult).getCardList();
            }
            if (baseBonusResult.getBonusPlayStatus() == GameConstant.POKER_GAME_BONUS_STATUS_COMPLETE) {
                bonusWon = baseBonusResult.getTotalPay();
                if (bonusWon > 0) {
                    long bonusPay = bonusWon / pokerGameLogicBean.getSumBetCredit();
                    resultInfo.setBonusTotalHit(resultInfo.getBonusTotalHit() + 1);
                    long bonusTotalWin = 0l;
                    long maxTotalPay = pokerModel.maxTotalPay();
                    //With $800 Cap
                    if (maxTotalPay > 0 && bonusWon >= maxTotalPay) {
                        bonusTotalWin = pokerGameLogicBean.getSumWinCredit() - pokerGameLogicBean.getPokerResult().getPokerPay();
                        resultInfo.setBonusTotalWin(resultInfo.getBonusTotalWin() + bonusTotalWin);
                    } else {
                        resultInfo.setBonusTotalWin(resultInfo.getBonusTotalWin() + bonusWon);
                        bonusTotalWin = bonusWon;
                    }
                    resultInfo.setBonusTotalWin(resultInfo.getBonusTotalWin() + bonusWon);
                    resultInfo.getGoldCashFeatureWin().set(2, resultInfo.getGoldCashFeatureWin().get(2) + bonusTotalWin);
                    if (pokerModel instanceof Model6080630Test) {
                        for (int bonusIndex = 0; bonusIndex < Model6080630BonusTest.ROUND_REWARD.length; bonusIndex++) {
                            if (bonusPay == Model6080630BonusTest.ROUND_REWARD[bonusIndex]) {
                                resultInfo.getROrBBonusHit().set(bonusIndex, resultInfo.getROrBBonusHit().get(bonusIndex) + 1l);
                                resultInfo.getROrBBonusWin().set(bonusIndex, resultInfo.getROrBBonusWin().get(bonusIndex) + bonusWon);
                                break;
                            }
                        }
                    } else {
                        for (int bonusIndex = 0; bonusIndex < Model6060630BonusTest.ROUND_REWARD.length; bonusIndex++) {
                            if (bonusPay == Model6060630BonusTest.ROUND_REWARD[bonusIndex]) {
                                resultInfo.getROrBBonusHit().set(bonusIndex, resultInfo.getROrBBonusHit().get(bonusIndex) + 1l);
                                resultInfo.getROrBBonusWin().set(bonusIndex, resultInfo.getROrBBonusWin().get(bonusIndex) + bonusWon);
                                break;
                            }
                        }
                    }

                }
                break;
            }
        }

    }

    protected void setBaseCommInfo(long spinCount, long initCredit, long totalWon, PokerGameLogicBean
            gameLogicBean, PokerResultInfo resultInfo) {
        resultInfo.setSpinCount(spinCount);
        resultInfo.setBet(gameLogicBean.getBet());
        resultInfo.setLines(gameLogicBean.getLines());
        resultInfo.setDenom(gameLogicBean.getDenom());
        resultInfo.setTotalBet(gameLogicBean.getSumBetCredit());
        resultInfo.setTotalCoinIn(resultInfo.getTotalCoinIn() + gameLogicBean.getSumBetCredit());
        if (totalWon > 0) {
            resultInfo.setTotalHit(resultInfo.getTotalHit() + 1);
        }
        resultInfo.setTotalCoinOut(resultInfo.getTotalCoinOut() + totalWon);
        resultInfo.setTotalAmount(totalWon);
        resultInfo.setLeftCredit(initCredit);
    }

    private void initPokersWinInfo(PokerResultInfo resultInfo) {
        List<Long> goldCardFeatureHit = new ArrayList<>();
        List<Long> goldCashFeatureWin = new ArrayList<>();
        List<Long> instantCashHit = new ArrayList<>();
        List<Long> instantCashWin = new ArrayList<>();
        List<Long> rOrBBonusHit = new ArrayList<>();
        List<Long> rOrBBonusWin = new ArrayList<>();
        List<Long> fsHit = new ArrayList<>();
        List<Long> fsTimes = new ArrayList<>();
        List<Long> fsWin = new ArrayList<>();
        List<Long> handPayHit = new ArrayList<>();
        List<Long> handPayWin = new ArrayList<>();
        List<Long> fsMulHit = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            goldCardFeatureHit.add(0l);
            goldCashFeatureWin.add(0l);
        }
        for (int i = 0; i < Model6080630Test.INSTANT_CASH_PAY_WEIGHT[0].length; i++) {
            instantCashHit.add(0l);
            instantCashWin.add(0l);
        }
        for (int i = 0; i < 6; i++) {
            rOrBBonusHit.add(0l);
            rOrBBonusWin.add(0l);
        }
        for (int i = 0; i < Model6080630Test.FS_TIMES_WEIGHT[0].length; i++) {
            fsHit.add(0l);
            fsTimes.add(0l);
            fsWin.add(0l);
        }
        for (int i = 0; i < Model6060630Test.FS_MUL_WEIGHT[0].length; i++) {
            fsMulHit.add(0l);
        }

        for (int i = 0; i < 10; i++) {
            handPayHit.add(0l);
            handPayWin.add(0l);
        }
        resultInfo.setGoldCardFeatureHit(goldCardFeatureHit);
        resultInfo.setGoldCashFeatureWin(goldCashFeatureWin);
        resultInfo.setInstantCashHit(instantCashHit);
        resultInfo.setInstantCashWin(instantCashWin);
        resultInfo.setROrBBonusHit(rOrBBonusHit);
        resultInfo.setROrBBonusWin(rOrBBonusWin);
        resultInfo.setFsHit(fsHit);
        resultInfo.setFsWin(fsWin);
        resultInfo.setFsTimes(fsTimes);
        resultInfo.setFsMulHit(fsMulHit);
        resultInfo.setHandPayHit(handPayHit);
        resultInfo.setHandPayWin(handPayWin);
    }
}
