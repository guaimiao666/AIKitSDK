package com.example.aikitsdk;

import java.io.File;

public class Utils {

    /**
     * 获取能力字符串
     * @param abilityArray 能力数组
     * @return
     */
    public static String getAbilityStr(String[] abilityArray) {
        if (abilityArray == null || abilityArray.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < abilityArray.length; i++) {
            builder.append(abilityArray[i]);
            if (i != abilityArray.length - 1) {
                builder.append(";");
            }
        }
        return builder.toString();
    }
}
