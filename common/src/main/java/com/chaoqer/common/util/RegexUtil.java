package com.chaoqer.common.util;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用正则工具类
 *
 * @author toby
 */
public class RegexUtil {

    private final static PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();
    public final static String ID_NUM_REGEX = "(?m)^[1-9][0-7]\\d{4}((19\\d{2}(0[13-9]|1[012])(0[1-9]|[12]\\d|30))|(19\\d{2}(0[13578]|1[02])31)|(19\\d{2}02(0[1-9]|1\\d|2[0-8]))|(19([13579][26]|[2468][048]|0[48])0229))\\d{3}(\\d|X|x)?$";
    public final static String DATE_REGEX = "(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29)";

    /**
     * 是否匹配
     */
    public static boolean match(String regex, String str) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    /**
     * 是否为合法手机号
     */
    public static boolean isValidMobile(String countryCode, String mobile) {
        if (StringUtils.isNumeric(countryCode) && StringUtils.isNumeric(mobile)) {
            Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber();
            phoneNumber.setCountryCode(Integer.parseInt(countryCode));
            phoneNumber.setNationalNumber(Long.parseLong(mobile));
            return PHONE_NUMBER_UTIL.isValidNumber(phoneNumber);
        }
        return false;
    }

}
