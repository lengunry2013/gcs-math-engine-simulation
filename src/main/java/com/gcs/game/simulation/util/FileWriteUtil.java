/*
 * FileWirteUtil.java
 * @author:jiangqx
 * History:
 *   date              name      Description
 *   Jan 19, 2017        jiangqx      create
 */

package com.gcs.game.simulation.util;

import com.gcs.game.simulation.vo.BaseConfigInfo;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class FileWriteUtil {

    public static final String OUTPUT_FILE_NAME = "simulationResult1.txt";
    private static final String NEW_LINE = "\r\n";

    private static int fileIndex = 1;

    public static int fileIndex1 = 1;

    public static void simulationDataOutputPath(BaseConfigInfo configInfo) {
        String model = configInfo.getModel();
        String tempStr = configInfo.getOutputPath();
        String outputPath = (tempStr == null || ("").equals(tempStr))
                ? System.getProperty("user.dir") : tempStr;
        String simpleDataPath = "";
        if (outputPath != null && !"".equals(outputPath)) {
            simpleDataPath = outputPath.replaceAll("\\\\", "/");
        }
        if (simpleDataPath.endsWith("/")) {
            simpleDataPath = simpleDataPath + "model" + model
                    + "_Simulation_Data/";
        } else {
            simpleDataPath = simpleDataPath + "/" + "model" + model
                    + "_Simulation_Data/";
        }

        // create path
        File path = new File(simpleDataPath);
        if (!path.exists()) {
            path.mkdirs();
        }
        String fileName = simpleDataPath + OUTPUT_FILE_NAME;
        configInfo.setOutputPath(simpleDataPath);
        configInfo.setOutputFileName(fileName);
    }

    /**
     * out put print
     *
     * @param str
     * @param configInfo
     * @author:jiangqx Jan 11, 2017
     */
    public static void outputPrint(String str, String fileName, BaseConfigInfo configInfo, int fileType) {
        try {
            File file = new File(fileName);
            if (file.exists()) {
                @Cleanup
                FileWriter fileWriter = new FileWriter(file, true);
                @Cleanup
                BufferedWriter bufferWriter = new BufferedWriter(fileWriter);
                //log.debug(str);
                System.out.println(str);
                checkFileSize(configInfo, fileName, fileType);
                bufferWriter.write(str);
                bufferWriter.write(NEW_LINE);
                bufferWriter.flush();
            }
        } catch (IOException e) {
            log.error("output simulation information error", e);
            throw new RuntimeException(e);
        }
    }

    private static void checkFileSize(BaseConfigInfo configInfo, String fileName, int fileType) {
        if (configInfo.getMaxFileSize() <= 0) {
            return;
        }
        File file = new File(fileName);
        if (file.exists()) {
            long size = file.length();
            if (size >= configInfo.getMaxFileSize()) {
                fileName = fileName.replace(fileIndex + ".txt", (fileIndex + 1)
                        + ".txt");
                if (fileType == 0) {
                    configInfo.setOutputFileName(fileName);
                } else if (fileType == 1) {
                    configInfo.setAchievementFileName(fileName);
                } else if (fileType == 2) {
                    configInfo.setHighMissFileName(fileName);
                } else if (fileType == 3) {
                    configInfo.setFsBonusFileName(fileName);
                }
                fileIndex++;
                createNewFile(fileName);
            }
        }
    }

    public static void createNewFile(String fileName) {
        try {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
                file.createNewFile();
            } else {
                file.createNewFile();
            }
        } catch (IOException e) {
            log.error("", e);
        }
    }

    public static void writeFileHeadInfo(String fileName, String fileHeadInfo) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            @Cleanup
            FileWriter fileWriter = new FileWriter(file, true);
            @Cleanup
            BufferedWriter bufferWriter = new BufferedWriter(fileWriter);
            //log.debug(fileHeadInfo);
            System.out.println(fileHeadInfo);
            bufferWriter.write(fileHeadInfo);
            bufferWriter.write(NEW_LINE);
            bufferWriter.flush();
        } catch (IOException e) {
            log.error("", e);
        }
    }


}
