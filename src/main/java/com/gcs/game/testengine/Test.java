package com.gcs.game.testengine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class Test {
    public static void main(String[] args) {
        double d=0.000012;
        String str="0.000012";
        int a=MathContext.DECIMAL32.getPrecision();
        System.out.println(a);

        System.out.println(new BigDecimal("0.000012",
                new MathContext(4, RoundingMode.HALF_UP)));
        System.out.println(new BigDecimal("0.000012",
                new MathContext(2,RoundingMode.HALF_UP)));
        System.out.println(new BigDecimal("0.000012",
                new MathContext(2,RoundingMode.CEILING)));
        System.out.println(new BigDecimal("0.000012",
                new MathContext(1,RoundingMode.CEILING)));
    }
}
