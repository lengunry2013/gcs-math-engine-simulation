package com.gcs.game.simulation.util;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.gcs.game.engine.blackJack.vo.BlackJackBetInfo;
import com.gcs.game.simulation.blackJack.vo.BlackJackConfigInfo;
import com.gcs.game.simulation.keno.vo.KenoConfigInfo;
import com.gcs.game.simulation.poker.vo.PokerConfigInfo;
import com.gcs.game.simulation.slot.vo.SlotConfigInfo;
import com.gcs.game.simulation.vo.BaseConfigInfo;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Properties;

@Slf4j
public class InputConfigReader {

    private static final String FILE_NAME = "/input/inputConfig.properties";
    private static final Properties PROPERTIES = new Properties();

    private static volatile InputConfigReader instant;
    private static final String MODEL_KEY = "model";
    private static final String PAYBACK_KEY = "payback";
    private static final String DENOM_KEY = "denom";
    private static final String GAME_CLASS_KEY = "gameClass";
    private static final String GROUP_CODE_KEY = "groupCode";
    private static final String INIT_CREDIT_KEY = "initCredit";
    private static final String SIMULATION_COUNT_KEY = "simulationCount";
    private static final String PLAY_TIME_KEY = "playTimesPerPlayer";
    private static final String OUT_PUT_TYPE_KEY = "outputType";
    private static final String OUT_PUT_PATH_KEY = "outputPath";
    private static final String MAX_OUT_PUT_FILE_KEY = "maxOutputFileSize";

    //BlackJack Game
    private static final String BET_INFOS_KEY = "betInfos";
    private static final String HAND_COUNT_KEY = "handCount";
    private static final String BJ_JACKPOT_PAY = "jackpotPay";
    private static final String BJ_JACKPOT_WEIGHT = "jackpotWeight";

    //Slots Game
    private static final String LINES_KEY = "lines";
    private static final String BET_KEY = "bet";
    private static final String CHOICE_FS_BONUS_INDEX_KEY = "choiceFsOrBonusIndex";
    private static final String HAS_RANDOM_CHOICE_FS_BONUS_KEY = "hasRandomBonusChoice";
    private static final String MAX_WIN_KEY = "MaxWin";
    private static final String HIGH_SYMBOL_KEY = "highSymbol";
    private static final String NEAR_MISS_SYMBOL_KEY = "nearMissSymbol";
    private static final String WILD_SYMBOL_KEY = "wildSymbol";
    private static final String HAS_WIN_TIER_FS_PAY_KEY = "hasWinTierFsPay";
    private static final String FS_TYPE_KEY = "fsType";
    private static final String FS_EXP_RTP_KEY = "fsExpRtp";
    private static final String FS_METER_TIMES_KEY = "fsMeterTimes";
    private static final String HAS_WIN_TIER_BONUS_PAY_KEY = "hasWinTierBonusPay";
    private static final String BONUS_TYPE_KEY = "bonusType";
    private static final String BONUS_EXP_RTP_KEY = "bonusExpRtp";
    private static final String SCATTER_SYMBOL_KEY = "scatterSymbol";
    private static final String HAS_ST_HOR_SYMBOL_KEY = "hasStatisticsHorSymbol";
    private static final String SCATTER_COUNT_KEY = "scatterCount";
    private static final String PLAY_GAME_COUNT_KEY = "playGameCount";
    private static final String PLAY_CH_SPECIAL_SYMBOL_KEY = "playChooseSpecialSymbol";
    private static final String ACHIEVEMENT_PAY_KEY = "achievementPay";
    private static final String ACHIEVEMENT_SYMBOL_KEY = "achievementSymbol";
    private static final String MAX_FILE_ACH_PAY_INDEX_KEY = "maxFileAchievementPayIndex";
    private static final String BASE_WEIGHT_KEY = "BaseWeight";
    private static final String BONUS_WEIGHT_KEY = "BonusWeight";
    private static final String FS_WEIGHT_KEY = "fsWeight";

    //Poker
    private static final String GOLD_TRIGGER_WEIGHT_KEY = "GoldCardTriggerWeight";
    private static final String INSTANT_WEIGHT_KEY = "InstantCashPayWeight";
    private static final String PAY_TABLE_KEY = "PayTable";
    private static final String TOTAL_PAY_CAP_KEY = "TotalPayCap";
    private static final String FS_MUL_WEIGHT_KEY = "fsMulWeight";
    //Keno
    private static final String FS_TIMES_KEY = "fsTimes";
    private static final String FS_3SETS_TIMES_KEY = "fs3SetsTimes";
    private static final String FS_3SETS_WEIGHT_KEY = "fs3SetsWeight";
    private static final String FS_4SETS_TIMES_KEY = "fs4SetsTimes";
    private static final String FS_4SETS_WEIGHT_KEY = "fs4SetsWeight";
    private static final String PICKS = "picks";


    public static InputConfigReader getInstance() {
        if (instant == null) {
            synchronized (InputConfigReader.class) {
                if (instant == null) {
                    instant = new InputConfigReader();
                }
            }
        }
        return instant;
    }

    public InputConfigReader() {
        try {
            String rootPath = System.getProperty("user.dir").replace("\\", "/");
            StringBuilder fileName = new StringBuilder();
            fileName.append(rootPath).append(File.separator).append(FILE_NAME);
            File newFile = new File(fileName.toString());
            if (newFile.exists()) {
                @Cleanup
                InputStream propertiesFile = new FileInputStream(newFile);
                PROPERTIES.load(propertiesFile);
            }
        } catch (IOException e) {
            log.error("Cannot load properties file: {}", FILE_NAME, e);
        }
    }

    private String getStringValue(String key) {
        try {
            String property = PROPERTIES.getProperty(key).trim();
            return property;
        } catch (Exception e) {
            log.error("load properties getStringValue() error", FILE_NAME, e);
            return null;
        }
    }

    private int getIntValue(String key) {
        try {
            String property = PROPERTIES.getProperty(key).trim();
            int value = Integer.valueOf(property);
            return value;
        } catch (Exception e) {
            log.error("load properties getIntValue() error", FILE_NAME, e);
            return 0;
        }
    }

    private long getLongValue(String key) {
        try {
            String property = PROPERTIES.getProperty(key).trim();
            long value = Long.valueOf(property);
            return value;
        } catch (Exception e) {
            log.error("load properties getLongValue() error", FILE_NAME, e);
            return 0;
        }
    }

    public BaseConfigInfo readConfigInfo() {
        String model = getStringValue(MODEL_KEY);
        String gameClass = getStringValue(GAME_CLASS_KEY);
        int payback = getIntValue(PAYBACK_KEY);
        int denom = getIntValue(DENOM_KEY);
        String groupCode = getStringValue(GROUP_CODE_KEY);
        long simulationCount = getLongValue(SIMULATION_COUNT_KEY);
        int playTimes = getIntValue(PLAY_TIME_KEY);
        int outputType = getIntValue(OUT_PUT_TYPE_KEY);
        String outputPath = getStringValue(OUT_PUT_PATH_KEY);
        long maxOutputSize = getLongValue(MAX_OUT_PUT_FILE_KEY);
        long initCredit = getLongValue(INIT_CREDIT_KEY);
        BaseConfigInfo configInfo = null;
        if ("TableGame_BlackJack".equalsIgnoreCase(gameClass)) {
            configInfo = new BlackJackConfigInfo();
            String betInfos = getStringValue(BET_INFOS_KEY);
            int handCount = getIntValue(HAND_COUNT_KEY);
            String jackpotPayStr = getStringValue(BJ_JACKPOT_PAY);
            String jackpotWeightStr = getStringValue(BJ_JACKPOT_WEIGHT);
            List<BlackJackBetInfo> blackJackConfigInfoList = JSON.parseArray(betInfos, BlackJackBetInfo.class);
            long[] jackpotPay = StringUtil.changeStrToLongArray(jackpotPayStr, ",");
            int[] jackpotWeight = StringUtil.changeStrToArray(jackpotWeightStr, ",");
            ((BlackJackConfigInfo) configInfo).setBlackJackBetInfoList(blackJackConfigInfoList);
            ((BlackJackConfigInfo) configInfo).setHandCount(handCount);
            ((BlackJackConfigInfo) configInfo).setJackpotPay(jackpotPay);
            ((BlackJackConfigInfo) configInfo).setJackpotWeight(jackpotWeight);

        } else if ("Slots".equalsIgnoreCase(gameClass)) {
            configInfo = new SlotConfigInfo();
            long lines = getLongValue(LINES_KEY);
            long bet = getLongValue(BET_KEY);
            int choiceFsBonusIndex = getIntValue(CHOICE_FS_BONUS_INDEX_KEY);
            boolean hasRandomBonusChoice = getIntValue(HAS_RANDOM_CHOICE_FS_BONUS_KEY) == 1 ? true : false;
            int[] maxWin = StringUtil.changeStrToArray(getStringValue(MAX_WIN_KEY), ",");
            int[] highSymbol = StringUtil.changeStrToArray(getStringValue(HIGH_SYMBOL_KEY), ",");
            int[] missSymbol = StringUtil.changeStrToArray(getStringValue(NEAR_MISS_SYMBOL_KEY), ",");
            int wildSymbol = getIntValue(WILD_SYMBOL_KEY);
            boolean hasWinTierFsPay = getIntValue(HAS_WIN_TIER_FS_PAY_KEY) == 1 ? true : false;
            int fsType = getIntValue(FS_TYPE_KEY);
            double[] fsExpRtp = StringUtil.changeStrToDoubleArray(getStringValue(FS_EXP_RTP_KEY), ",");
            int fsMeterTimes = getIntValue(FS_METER_TIMES_KEY);
            boolean hasWinTierBonusPay = getIntValue(HAS_WIN_TIER_BONUS_PAY_KEY) == 1 ? true : false;
            int bonusType = getIntValue(BONUS_TYPE_KEY);
            double[] bonusExpRtp = StringUtil.changeStrToDoubleArray(getStringValue(BONUS_EXP_RTP_KEY), ",");
            int[] scatterSymbol = StringUtil.changeStrToArray(getStringValue(SCATTER_SYMBOL_KEY), ",");
            int[] scatterCount = StringUtil.changeStrToArray(getStringValue(SCATTER_COUNT_KEY), ",");
            boolean hasStHorSymbol = getIntValue(HAS_ST_HOR_SYMBOL_KEY) == 1 ? true : false;
            int playGameCount = getIntValue(PLAY_GAME_COUNT_KEY);
            String specialSymbol = getStringValue(PLAY_CH_SPECIAL_SYMBOL_KEY);
            int[] achievementPay = StringUtil.changeStrToArray(getStringValue(ACHIEVEMENT_PAY_KEY), ",");
            int[] achievementSymbol = StringUtil.changeStrToArray(getStringValue(ACHIEVEMENT_SYMBOL_KEY), ",");
            int maxFileAchePayIndex = getIntValue(MAX_FILE_ACH_PAY_INDEX_KEY);
            int[][] baseWeight = JSON.parseObject(getStringValue(BASE_WEIGHT_KEY), int[][].class);
            int[][] bonusWeight = JSON.parseObject(getStringValue(BONUS_WEIGHT_KEY), int[][].class);
            int[][] fsWeight = JSON.parseObject(getStringValue(FS_WEIGHT_KEY), int[][].class);
            long maxTotalPay = getLongValue(TOTAL_PAY_CAP_KEY);
            long[][] payTables = JSON.parseObject(getStringValue(PAY_TABLE_KEY), long[][].class);
            ((SlotConfigInfo) configInfo).setLines(lines);
            ((SlotConfigInfo) configInfo).setBet(bet);
            ((SlotConfigInfo) configInfo).setChoiceFsOrBonusIndex(choiceFsBonusIndex);
            ((SlotConfigInfo) configInfo).setRandomBonusChoice(hasRandomBonusChoice);
            ((SlotConfigInfo) configInfo).setMaxWin(maxWin);
            ((SlotConfigInfo) configInfo).setHighSymbol(highSymbol);
            ((SlotConfigInfo) configInfo).setMissSymbol(missSymbol);
            ((SlotConfigInfo) configInfo).setWildSymbol(wildSymbol);
            ((SlotConfigInfo) configInfo).setFsPay(hasWinTierFsPay);
            ((SlotConfigInfo) configInfo).setFsType(fsType);
            ((SlotConfigInfo) configInfo).setFsRtp(fsExpRtp);
            ((SlotConfigInfo) configInfo).setFsMeterTimes(fsMeterTimes);
            ((SlotConfigInfo) configInfo).setBonusPay(hasWinTierBonusPay);
            ((SlotConfigInfo) configInfo).setBonusType(bonusType);
            ((SlotConfigInfo) configInfo).setBonusRtp(bonusExpRtp);
            ((SlotConfigInfo) configInfo).setScatterSymbol(scatterSymbol);
            ((SlotConfigInfo) configInfo).setScatterCount(scatterCount);
            ((SlotConfigInfo) configInfo).setSumHorizontalSymbol(hasStHorSymbol);
            ((SlotConfigInfo) configInfo).setPlayGameCount(playGameCount);
            ((SlotConfigInfo) configInfo).setPlayerChooseSymbol(specialSymbol);
            ((SlotConfigInfo) configInfo).setAchievementPay(achievementPay);
            ((SlotConfigInfo) configInfo).setAchievementSymbol(achievementSymbol);
            ((SlotConfigInfo) configInfo).setMaxFileAchievementPayIndex(maxFileAchePayIndex);
            ((SlotConfigInfo) configInfo).setBaseWeight(baseWeight);
            ((SlotConfigInfo) configInfo).setBonusWeight(bonusWeight);
            ((SlotConfigInfo) configInfo).setFsWeight(fsWeight);
            ((SlotConfigInfo) configInfo).setTotalPayCap(maxTotalPay);
            ((SlotConfigInfo) configInfo).setPayTables(payTables);

        } else if ("TableGame_Poker".equalsIgnoreCase(gameClass)) {
            configInfo = new PokerConfigInfo();
            long lines = getLongValue(LINES_KEY);
            long bet = getLongValue(BET_KEY);
            long maxTotalPay = getLongValue(TOTAL_PAY_CAP_KEY);
            int[] goldTriggerWeight = JSON.parseObject(getStringValue(GOLD_TRIGGER_WEIGHT_KEY), int[].class);
            long[] payTable = JSON.parseObject(getStringValue(PAY_TABLE_KEY), long[].class);
            int[][] baseWeight = JSON.parseObject(getStringValue(BASE_WEIGHT_KEY), int[][].class);
            int[][] bonusWeight = JSON.parseObject(getStringValue(BONUS_WEIGHT_KEY), int[][].class);
            int[][] fsWeight = JSON.parseObject(getStringValue(FS_WEIGHT_KEY), int[][].class);
            int[][] instantPayWeight = JSON.parseObject(getStringValue(INSTANT_WEIGHT_KEY), int[][].class);
            int[][] fsMulWeight = JSON.parseObject(getStringValue(FS_MUL_WEIGHT_KEY), int[][].class);
            ((PokerConfigInfo) configInfo).setLines(lines);
            ((PokerConfigInfo) configInfo).setBet(bet);
            ((PokerConfigInfo) configInfo).setGoldCardTriggerWeight(goldTriggerWeight);
            ((PokerConfigInfo) configInfo).setPayTable(payTable);
            ((PokerConfigInfo) configInfo).setTotalPayCap(maxTotalPay);
            ((PokerConfigInfo) configInfo).setBaseWeight(baseWeight);
            ((PokerConfigInfo) configInfo).setBonusWeight(bonusWeight);
            ((PokerConfigInfo) configInfo).setFsWeight(fsWeight);
            ((PokerConfigInfo) configInfo).setInstantCashPayWeight(instantPayWeight);
            ((PokerConfigInfo) configInfo).setFsMulWeight(fsMulWeight);
        } else if ("TableGame_Keno".equalsIgnoreCase(gameClass)) {
            configInfo = new KenoConfigInfo();
            long lines = getLongValue(LINES_KEY);
            long bet = getLongValue(BET_KEY);
            long maxTotalPay = getLongValue(TOTAL_PAY_CAP_KEY);
            int[][] fsTimes = JSON.parseObject(getStringValue(FS_TIMES_KEY), int[][].class);
            int[][] fsWeight = JSON.parseObject(getStringValue(FS_WEIGHT_KEY), int[][].class);
            int[][] fs3SetsTimes = JSON.parseObject(getStringValue(FS_3SETS_TIMES_KEY), int[][].class);
            int[][] fs3SetsWeight = JSON.parseObject(getStringValue(FS_3SETS_WEIGHT_KEY), int[][].class);
            int[][] fs4SetsTimes = JSON.parseObject(getStringValue(FS_4SETS_TIMES_KEY), int[][].class);
            int[][] fs4SetsWeight = JSON.parseObject(getStringValue(FS_4SETS_WEIGHT_KEY), int[][].class);
            double[][] payTable = JSON.parseObject(getStringValue(PAY_TABLE_KEY), double[][].class);
            int[] picks = JSON.parseObject(getStringValue(PICKS), int[].class);
            ((KenoConfigInfo) configInfo).setLines(lines);
            ((KenoConfigInfo) configInfo).setBet(bet);
            ((KenoConfigInfo) configInfo).setTotalPayCap(maxTotalPay);
            ((KenoConfigInfo) configInfo).setFsTimes(fsTimes);
            ((KenoConfigInfo) configInfo).setFsWeight(fsWeight);
            ((KenoConfigInfo) configInfo).setFs3SetsTimes(fs3SetsTimes);
            ((KenoConfigInfo) configInfo).setFs3SetsWeight(fs3SetsWeight);
            ((KenoConfigInfo) configInfo).setFs4SetsTimes(fs4SetsTimes);
            ((KenoConfigInfo) configInfo).setFs4SetsWeight(fs4SetsWeight);
            ((KenoConfigInfo) configInfo).setPayTable(payTable);
            ((KenoConfigInfo) configInfo).setPicks(picks);
        }
        configInfo.setModel(model);
        configInfo.setPayback(payback);
        configInfo.setDenom(denom);
        configInfo.setJackpotGroupCode(groupCode);
        configInfo.setGameClass(gameClass);
        configInfo.setInitCredit(initCredit);
        configInfo.setSimulationCount(simulationCount);
        configInfo.setPlayTimesPerPlayer(playTimes);
        configInfo.setOutputType(outputType);
        configInfo.setOutputPath(outputPath);
        configInfo.setMaxFileSize(maxOutputSize);
        return configInfo;
    }

}
