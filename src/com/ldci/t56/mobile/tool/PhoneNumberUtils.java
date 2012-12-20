
package com.ldci.t56.mobile.tool;

import java.util.Locale;

public class PhoneNumberUtils {
    // Engle,去掉国家代号的方法
    public final static String trimSmsNumber(String number) {
        String s = number;
        String prefix = "";
        if (Locale.CHINA.equals(Locale.getDefault())) {
            prefix = "+86";
        }
        if (prefix.length() > 0 && number.startsWith(prefix)) {
            s = number.substring(prefix.length());
        }
        return s;
    }
}
