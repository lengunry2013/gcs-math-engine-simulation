package com.gcs.game.simulation.keno.vo;

import com.gcs.game.simulation.vo.BaseConfigInfo;
import lombok.Data;

@Data
public class KenoConfigInfo extends BaseConfigInfo {

    private long lines = 1;
    private long bet = 1;

    private int[][] fsTimes = null;
    private int[][] fsWeight = null;
    private int[][] fs3SetsTimes = null;
    private int[][] fs3SetsWeight = null;
    private int[][] fs4SetsTimes = null;
    private int[][] fs4SetsWeight = null;
    private double[][] payTable = null;
    private long totalPayCap = 80000l;
    private int[] picks = null;


}
