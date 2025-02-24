package com.ctrip.framework.drc.console.utils;

import org.apache.commons.codec.binary.Hex;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * Created by dengquanliang
 * 2023/9/7 17:03
 */
public class CommonUtils {

    public static final String HEX_PREFIX = "0x";

    public static boolean isSameList(List<String> firstList, List<String> secondList) {
        if (CollectionUtils.isEmpty(firstList) || CollectionUtils.isEmpty(secondList)) {
            return false;
        }
        if (firstList.size() != secondList.size()) {
            return false;
        }
        Collections.sort(firstList);
        Collections.sort(secondList);
        return firstList.equals(secondList);
    }

    /**
     * byte[] -> '0x...'
     */
    public static String byteToHexString(byte[] bytes) {
        return HEX_PREFIX + Hex.encodeHexString(bytes);
    }
}
