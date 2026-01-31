package com.gcs.game.simulation.blackJack.engine;

import com.gcs.game.engine.IGameEngine;
import com.gcs.game.engine.blackJack.model.BaseBlackJackModel;
import com.gcs.game.engine.blackJack.utils.BlackJackGameConstant;
import com.gcs.game.engine.blackJack.vo.BlackJackBetInfo;
import com.gcs.game.engine.blackJack.vo.BlackJackGameLogicBean;
import com.gcs.game.engine.blackJack.vo.BlackJackResult;
import com.gcs.game.engine.blackJack.vo.DealerResult;
import com.gcs.game.engine.math.modelGCBJ00101.ModelGCBJ00101;
import com.gcs.game.engine.math.modelGCBJ00102.ModelGCBJ00102;
import com.gcs.game.exception.InvalidBetException;
import com.gcs.game.exception.InvalidGameStateException;
import com.gcs.game.exception.InvalidPlayerInputException;
import com.gcs.game.simulation.blackJack.vo.BlackJackConfigInfo;
import com.gcs.game.simulation.blackJack.vo.BlackJackResultInfo;
import com.gcs.game.simulation.blackJack.vo.BlackJackWinPay;
import com.gcs.game.simulation.util.BaseConstant;
import com.gcs.game.simulation.util.FileWriteUtil;
import com.gcs.game.simulation.util.StringUtil;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import com.gcs.game.testengine.math.modelGCBJ00101.ModelGCBJ00101Test;
import com.gcs.game.testengine.math.modelGCBJ00102.ModelGCBJ00102Test;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.vo.BaseGameLogicBean;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class BlackJackEngineResult {
    public BlackJackEngineResult() {

    }

    public void DealResult(IGameEngine engine, BaseGameLogicBean gameLogicBean, BaseConfigInfo configInfo, BaseBlackJackModel blackJackModel) throws InvalidGameStateException, InvalidBetException, InvalidPlayerInputException, CloneNotSupportedException {
        BlackJackConfigInfo blackJackConfigInfo = (BlackJackConfigInfo) configInfo;
        BlackJackGameLogicBean blackJackGameLogicBean = (BlackJackGameLogicBean) gameLogicBean;
        BlackJackResultInfo resultInfo = new BlackJackResultInfo();
        long spinCount = 0L;
        long simulationCount = configInfo.getSimulationCount();
        int playTime = configInfo.getPlayTimesPerPlayer();
        long initCredit = configInfo.getInitCredit();
        initBlackJackWinInfo(resultInfo, blackJackConfigInfo.getHandCount(), blackJackModel);
        resultInfo.setDenom(configInfo.getDenom());
        long totalWon;
        FileWriteUtil.writeFileHeadInfo(configInfo.getOutputFileName(), StringUtil.getBlackJackHeadInfo(blackJackConfigInfo, blackJackModel));
        for (int i = 0; i < simulationCount; i++) {
            //deal
            spinCount++;
            totalWon = 0L;
            long totalBet;
            Map gameLoginMap = new LinkedHashMap();
            gameLoginMap.put("denom", configInfo.getDenom());
            gameLoginMap.put("gamePlayStatus", GameConstant.BJ_BLACKJACK_GAME_STATUS_DEAL);
            gameLoginMap.put("currentHandIndex", 1);
            gameLoginMap.put("splitIndex", 0);
            List<BlackJackBetInfo> betInfoList = new ArrayList<>();
            for (BlackJackBetInfo configBetInfo : blackJackConfigInfo.getBlackJackBetInfoList()) {
                betInfoList.add(configBetInfo.clone());
            }
            gameLoginMap.put("blackJackBetInfos", betInfoList);
            //game-start
            blackJackGameLogicBean = (BlackJackGameLogicBean) engine.gameStart(blackJackGameLogicBean, gameLoginMap, null, null);

            List<BlackJackResult> blackJackResultList;
            if (blackJackGameLogicBean.getGamePlayStatus() == GameConstant.GAME_STATUS_COMPLETE) {
                blackJackResultList = blackJackGameLogicBean.getBlackJackResults();
                totalBet = blackJackGameLogicBean.getSumBetCredit();
                long sumWin = blackJackGameLogicBean.getSumWinCredit();
                initCredit -= totalBet;
                totalWon += sumWin;
            } else {
                int handIndex = 1;
                while (true) {
                    Map<String, String> engineContextMap = engine.getEngineContext();
                    DealerResult dealerResult = blackJackGameLogicBean.getDealerResult();
                    int dealerStatus = dealerResult.getDealerStatus();
                    blackJackResultList = blackJackGameLogicBean.getBlackJackResults();
                    if (dealerStatus == BlackJackGameConstant.DEALER_WAIT_FOR_INSURANCE) {
                        gameLoginMap.put("gamePlayStatus", GameConstant.BJ_BLACKJACK_GAME_STATUS_INSURANCE);
                        gameLoginMap.put("currentHandIndex", handIndex);
                        gameLoginMap.put("blackJackBetInfos", blackJackGameLogicBean.getBlackJackBetInfos());
                        blackJackGameLogicBean = (BlackJackGameLogicBean) engine.gameProgress(blackJackGameLogicBean, gameLoginMap, null, engineContextMap, null, null);
                        if (handIndex == blackJackConfigInfo.getHandCount()) {
                            handIndex = 1;
                        } else if (handIndex < blackJackConfigInfo.getHandCount()) {
                            handIndex++;
                        }
                    } else if (dealerStatus == BlackJackGameConstant.DEALER_PEEK_BJ) {
                        gameLoginMap.put("gamePlayStatus", GameConstant.BJ_BLACKJACK_GAME_STATUS_PEEK_BLACKJACK);
                        gameLoginMap.put("blackJackBetInfos", blackJackGameLogicBean.getBlackJackBetInfos());
                        blackJackGameLogicBean = (BlackJackGameLogicBean) engine.gameProgress(blackJackGameLogicBean, gameLoginMap, null, engineContextMap, null, null);
                    } else if (dealerStatus == BlackJackGameConstant.DEALER_WAIT_FOR_HANDS) {
                        BlackJackResult blackJackResult = blackJackResultList.get(handIndex - 1);
                        List<Integer> handCardPointList = blackJackResult.getCardsPoint();
                        int handPoint = handCardPointList.get(handCardPointList.size() - 1);
                        int handStatus = blackJackResult.getHandStatus();
                        int splitHandStatus = blackJackResult.getSplitHandStatus();
                        boolean isSplitSetIndex = isSetSplitIndex(blackJackResult, blackJackGameLogicBean.getSplitIndex());
                        boolean isNextHand = isNextHand(handStatus);
                        int gamePlayStatus = blackJackGameLogicBean.getGamePlayStatus();
                        if (isSplitSetIndex) {
                            gameLoginMap.put("splitIndex", 1);
                            blackJackGameLogicBean.setSplitIndex(1);
                            continue;
                        } else if ((blackJackResult.isHasSplit() && (splitHandStatus == BlackJackGameConstant.HAND_STATUS_BUST || splitHandStatus == BlackJackGameConstant.HAND_STATUS_STAND)) && isNextHand) {
                            if (handIndex < blackJackConfigInfo.getHandCount()) {
                                handIndex++;
                                gameLoginMap.put("splitIndex", 0);
                                blackJackGameLogicBean.setSplitIndex(0);
                                continue;
                            }
                        } else if (!blackJackResult.isHasSplit() && isNextHand) {
                            if (handIndex < blackJackConfigInfo.getHandCount()) {
                                handIndex++;
                                gameLoginMap.put("splitIndex", 0);
                                blackJackGameLogicBean.setSplitIndex(0);
                                continue;
                            }
                        }
                        if (handStatus == BlackJackGameConstant.HAND_STATUS_SPLIT) {
                            gamePlayStatus = GameConstant.BJ_BLACKJACK_GAME_STATUS_SPLIT;
                        } else if (handStatus == BlackJackGameConstant.HAND_STATUS_DOUBLE) {
                            if (handPoint >= BlackJackGameConstant.DEALER_CARD_COMPLETE_POINT) {
                                gamePlayStatus = GameConstant.BJ_BLACKJACK_GAME_STATUS_STAND;
                            } else if (handPoint >= 14) {
                                gamePlayStatus = GameConstant.BJ_BLACKJACK_GAME_STATUS_DOUBLE;
                            } else {
                                gamePlayStatus = GameConstant.BJ_BLACKJACK_GAME_STATUS_HIT;
                            }
                        } else if (blackJackResult.isHasSplit() && splitHandStatus == BlackJackGameConstant.HAND_STATUS_DOUBLE && blackJackGameLogicBean.getSplitIndex() == 1) {
                            int splitPoint = blackJackResult.getSplitCardsPoint().get(blackJackResult.getSplitCardsPoint().size() - 1);
                            if (splitPoint >= BlackJackGameConstant.DEALER_CARD_COMPLETE_POINT) {
                                gamePlayStatus = GameConstant.BJ_BLACKJACK_GAME_STATUS_STAND;
                            } else if (splitPoint >= 14) {
                                gamePlayStatus = GameConstant.BJ_BLACKJACK_GAME_STATUS_DOUBLE;
                            } else {
                                gamePlayStatus = GameConstant.BJ_BLACKJACK_GAME_STATUS_HIT;
                            }
                        } else if (handStatus == BlackJackGameConstant.HAND_STATUS_HIT) {
                            if (handPoint >= BlackJackGameConstant.DEALER_CARD_COMPLETE_POINT) {
                                gamePlayStatus = GameConstant.BJ_BLACKJACK_GAME_STATUS_STAND;
                            } else {
                                gamePlayStatus = GameConstant.BJ_BLACKJACK_GAME_STATUS_HIT;
                            }
                        } else if (blackJackResult.isHasSplit() && splitHandStatus == BlackJackGameConstant.HAND_STATUS_HIT && blackJackGameLogicBean.getSplitIndex() == 1) {
                            int splitPoint = blackJackResult.getSplitCardsPoint().get(blackJackResult.getSplitCardsPoint().size() - 1);
                            if (splitPoint >= BlackJackGameConstant.DEALER_CARD_COMPLETE_POINT) {
                                gamePlayStatus = GameConstant.BJ_BLACKJACK_GAME_STATUS_STAND;
                            } else {
                                gamePlayStatus = GameConstant.BJ_BLACKJACK_GAME_STATUS_HIT;
                            }
                        }
                        //game-progress
                        gameLoginMap.put("currentHandIndex", handIndex);
                        gameLoginMap.put("gamePlayStatus", gamePlayStatus);
                        gameLoginMap.put("blackJackBetInfos", blackJackGameLogicBean.getBlackJackBetInfos());
                        blackJackGameLogicBean = (BlackJackGameLogicBean) engine.gameProgress(blackJackGameLogicBean, gameLoginMap, null, engineContextMap, null, null);
                    } else if (dealerStatus == BlackJackGameConstant.DEALER_DEAL) {
                        gameLoginMap.put("gamePlayStatus", GameConstant.BJ_BLACKJACK_GAME_STATUS_DEALER_DRAW);
                        gameLoginMap.put("blackJackBetInfos", blackJackGameLogicBean.getBlackJackBetInfos());
                        blackJackGameLogicBean = (BlackJackGameLogicBean) engine.gameProgress(blackJackGameLogicBean, gameLoginMap, null, engineContextMap, null, null);
                    }
                    if (blackJackGameLogicBean.getGamePlayStatus() == GameConstant.GAME_STATUS_COMPLETE) {
                        blackJackResultList = blackJackGameLogicBean.getBlackJackResults();
                        totalBet = blackJackGameLogicBean.getSumBetCredit();
                        long sumWin = blackJackGameLogicBean.getSumWinCredit();
                        initCredit -= totalBet;
                        totalWon += sumWin;
                        break;
                    }
                }
            }
            int tmpHandIndex = 0;
            for (BlackJackResult blackJackResult : blackJackResultList) {
                BlackJackWinPay blackJackWinPay = resultInfo.getBlackJackWinPayList().get(tmpHandIndex);
                BlackJackBetInfo betInfo = blackJackGameLogicBean.getBlackJackBetInfos().get(tmpHandIndex);
                if (blackJackResult.getBetPay() > 0) {
                    blackJackWinPay.setBetWinHit(blackJackWinPay.getBetWinHit() + 1);
                    blackJackWinPay.setBetWinPay(blackJackWinPay.getBetWinPay() + blackJackResult.getBetPay());
                }
                if (blackJackResult.getJackpotPay() > 0) {
                    blackJackWinPay.setJackpotWinHit(blackJackWinPay.getJackpotWinHit() + 1);
                    blackJackWinPay.setJackpotWinPay(blackJackWinPay.getJackpotWinPay() + blackJackResult.getJackpotPay());
                }
                if (blackJackResult.getSplitPay() > 0) {
                    blackJackWinPay.setSplitWinHit(blackJackWinPay.getSplitWinHit() + 1);
                    blackJackWinPay.setSplitWinPay(blackJackWinPay.getSplitWinPay() + blackJackResult.getSplitPay());
                }
                if (blackJackResult.getInsurancePay() > 0) {
                    blackJackWinPay.setInsuranceWinHit(blackJackWinPay.getInsuranceWinHit() + 1);
                    blackJackWinPay.setInsuranceWinPay(blackJackWinPay.getInsuranceWinPay() + blackJackResult.getInsurancePay());
                }
                if (betInfo.getJackpotBet() > 0) {
                    long[] jackpotPay = null;
                    if (blackJackModel instanceof ModelGCBJ00101Test) {
                        jackpotPay = ModelGCBJ00101.JACKPOT_PAY;
                    } else if (blackJackModel instanceof ModelGCBJ00102Test) {
                        jackpotPay = ModelGCBJ00102.JACKPOT_PAY;
                    }
                    for (int index = 0; index < jackpotPay.length; index++) {
                        long payCredit = jackpotPay[index] / configInfo.getDenom();
                        if (payCredit == blackJackResult.getJackpotPay()) {
                            resultInfo.getJackpotWinHit().set(index, resultInfo.getJackpotWinHit().get(index) + 1);
                        }
                    }
                }
                tmpHandIndex++;
            }
            resultInfo.setSpinCount(spinCount);
            resultInfo.setLeftCredit(initCredit);
            resultInfo.setTotalBet(totalBet);
            resultInfo.setTotalAmount(totalWon);
            if (totalWon > 0) {
                resultInfo.setTotalHit(resultInfo.getTotalHit() + 1);
            }
            resultInfo.setTotalCoinIn(resultInfo.getTotalCoinIn() + totalBet);
            resultInfo.setTotalCoinOut(resultInfo.getTotalCoinOut() + totalWon);
            if (spinCount > 0 && spinCount % playTime == 0) {
                outputResult(resultInfo, blackJackConfigInfo);
            }
        }

    }

    private void outputResult(BlackJackResultInfo resultInfo, BlackJackConfigInfo blackJackConfigInfo) {
        StringBuilder strBContent = new StringBuilder();
        strBContent.append(resultInfo.getSpinCount()).append(BaseConstant.TAB_STR);
        strBContent.append(resultInfo.getLeftCredit()).append(BaseConstant.TAB_STR);
        strBContent.append(blackJackConfigInfo.getHandCount()).append(BaseConstant.TAB_STR);
        strBContent.append(resultInfo.getTotalBet()).append(BaseConstant.TAB_STR);
        strBContent.append(resultInfo.getDenom()).append(BaseConstant.TAB_STR);
        strBContent.append(resultInfo.getTotalAmount()).append(BaseConstant.TAB_STR);
        strBContent.append(resultInfo.getTotalHit()).append(BaseConstant.TAB_STR);
        strBContent.append(resultInfo.getTotalCoinIn()).append(BaseConstant.TAB_STR);
        strBContent.append(resultInfo.getTotalCoinOut()).append(BaseConstant.TAB_STR);
        double hitRate = resultInfo.getTotalHit() * 1.0 / resultInfo.getSpinCount();
        double payback = resultInfo.getTotalCoinOut() * 1.0 / resultInfo.getTotalCoinIn();
        strBContent.append(hitRate).append(BaseConstant.TAB_STR);
        strBContent.append(payback).append(BaseConstant.TAB_STR);
        List<BlackJackWinPay> blackJackWinPayList = resultInfo.getBlackJackWinPayList();
        for (BlackJackWinPay blackJackWinPay : blackJackWinPayList) {
            strBContent.append(blackJackWinPay.getBetWinHit()).append(BaseConstant.TAB_STR);
            strBContent.append(blackJackWinPay.getBetWinPay()).append(BaseConstant.TAB_STR);
            strBContent.append(blackJackWinPay.getJackpotWinHit()).append(BaseConstant.TAB_STR);
            strBContent.append(blackJackWinPay.getJackpotWinPay()).append(BaseConstant.TAB_STR);
            strBContent.append(blackJackWinPay.getSplitWinHit()).append(BaseConstant.TAB_STR);
            strBContent.append(blackJackWinPay.getSplitWinPay()).append(BaseConstant.TAB_STR);
            strBContent.append(blackJackWinPay.getInsuranceWinHit()).append(BaseConstant.TAB_STR);
            strBContent.append(blackJackWinPay.getInsuranceWinPay()).append(BaseConstant.TAB_STR);
        }
        resultInfo.getJackpotWinHit().forEach(jackpotHit -> strBContent.append(jackpotHit).append(BaseConstant.TAB_STR));
        FileWriteUtil.outputPrint(strBContent.toString(), blackJackConfigInfo.getOutputFileName(), blackJackConfigInfo, 0);
    }

    private static boolean isNextHand(int handStatus) {
        return handStatus == BlackJackGameConstant.HAND_STATUS_BJ_WIN || handStatus == BlackJackGameConstant.HAND_STATUS_BUST ||
                handStatus == BlackJackGameConstant.HAND_STATUS_STAND || handStatus == BlackJackGameConstant.HAND_STATUS_SITTINGOUT;
    }

    private boolean isSetSplitIndex(BlackJackResult blackJackResult, int splitIndex) {
        boolean isSetSplit = false;
        int splitHandStatus = blackJackResult.getSplitHandStatus();
        int handStatus = blackJackResult.getHandStatus();
        if (blackJackResult.isHasSplit() && splitIndex == 0) {
            if (handStatus == BlackJackGameConstant.HAND_STATUS_BUST || handStatus == BlackJackGameConstant.HAND_STATUS_STAND) {
                if (splitHandStatus != BlackJackGameConstant.HAND_STATUS_BUST && splitHandStatus != BlackJackGameConstant.HAND_STATUS_STAND) {
                    isSetSplit = true;
                }
            }
        }
        return isSetSplit;
    }

    protected void initBlackJackWinInfo(BlackJackResultInfo resultInfo, int handCount, BaseBlackJackModel blackJackModel) {
        List<BlackJackWinPay> blackJackWinList = new ArrayList<>();
        for (int i = 0; i < handCount; i++) {
            BlackJackWinPay blackJackWinPay = new BlackJackWinPay(i + 1);
            blackJackWinList.add(blackJackWinPay);
        }
        resultInfo.setBlackJackWinPayList(blackJackWinList);
        List<Long> jackpotHitList = new ArrayList<>();
        long[] jackpotPay = null;
        if (blackJackModel instanceof ModelGCBJ00101Test) {
            jackpotPay = ModelGCBJ00101.JACKPOT_PAY;

        } else if (blackJackModel instanceof ModelGCBJ00102Test) {
            jackpotPay = ModelGCBJ00102.JACKPOT_PAY;
        }
        for (int i = 0; i < jackpotPay.length; i++) {
            jackpotHitList.add(0L);
        }
        resultInfo.setJackpotWinHit(jackpotHitList);
    }

}
