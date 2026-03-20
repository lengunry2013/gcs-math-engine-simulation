package com.gcs.game.testengine.math.model20260201;


import com.gcs.game.engine.math.model20260201.Model20260201Bonus;
import com.gcs.game.engine.slots.vo.SlotBonusResult;
import com.gcs.game.engine.slots.vo.SlotChoiceBonusResult;
import com.gcs.game.engine.slots.vo.SlotGameLogicBean;
import com.gcs.game.utils.BonusCharactersUtil;
import com.gcs.game.utils.GameConstant;
import com.gcs.game.utils.RandomUtil;
import com.gcs.game.vo.PlayerInputInfo;
import com.gcs.game.vo.RecoverInfo;

public class Model20260201BonusTest extends Model20260201Bonus {

    @Override
    public SlotBonusResult computeBonusStart(SlotGameLogicBean gameSessionBean, int payback) {
        SlotChoiceBonusResult result = new SlotChoiceBonusResult();
        int count = getCharactersCount();
        int displayCharCount = getDisplayCharactersCount();
        int hitSymbolCount = getHitSymbolCount(gameSessionBean.getSlotSpinResult());
        long[] charactersAwards = getCharactersAwards(payback, hitSymbolCount);
        int bonusStatus = GameConstant.SLOT_GAME_BONUS_STATUS_START;
        int[] pickIndexs = null;
        int[] pickCharacters = new int[displayCharCount];
        int[] charactersCount = new int[count];
        int[] charactersCountWithWild = new int[count];
        int[] hitCharacters = null;
        long[] hitCharactersPay = null;
        int[] displayCharacters4Reveal = new int[displayCharCount];
        long totalPay = 0;
        long payForPick = 0;
        String bonusWinPattern = "";
        int hitLevel = -1;

        int[] allCharacters = getAllCharacters();
        int betLevel = (int) (gameSessionBean.getBet() - 1);
        int[] awardWeights = getPickAwardWeight()[betLevel];
        int randomIndex = RandomUtil.getRandomIndexFromArrayWithWeight(awardWeights);
        int bonusMultiplier = getBonusMultiplier()[randomIndex];
        bonusWinPattern = BonusCharactersUtil.getBonusWinPattern(randomIndex, bonusMultiplier);
        pickCharacters = BonusCharactersUtil.getCharactersResult(bonusWinPattern, allCharacters);
        hitLevel = randomIndex;
        totalPay = charactersAwards[randomIndex];
        payForPick = charactersAwards[randomIndex];
        result.setCharactersRewards(charactersAwards);
        result.setBonusPlayStatus(bonusStatus);
        result.setPickIndexInfos(pickIndexs);
        result.setPickCharacters(pickCharacters);
        result.setCharactersCount(charactersCount);
        result.setCharactersCountWithWild(charactersCountWithWild);
        result.setHitCharacters(hitCharacters);
        result.setHitCharactersPay(hitCharactersPay);
        result.setDisplayCharacters4Reveal(displayCharacters4Reveal);
        result.setTotalPay(totalPay);
        result.setPayForPickIndex(payForPick);
        result.setBonusMul(bonusMultiplier);
        result.setBonusWinPattern(bonusWinPattern);
        result.setHitLevel(hitLevel);
        return result;
    }

    @Override
    public SlotBonusResult computeBonusPick(SlotGameLogicBean gameSessionBean, PlayerInputInfo playerInfo, SlotBonusResult bonus, RecoverInfo recoverInfo) {
        if (bonus != null) {
            bonus.setBonusPlayStatus(1000);
        }
        return bonus;
    }

}
