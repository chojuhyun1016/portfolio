package com.example.mp.gw.common.utils;

import java.text.DecimalFormat;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import yoyozo.util.Timex;
import yoyozo.util.Util;

public class PmKeyMaker {

    public static final String TT_CAB = "1";
    public static final String TT_IMG = "2";
    DecimalFormat df2;
    DecimalFormat df3;
    DecimalFormat df4;
    DecimalFormat df5;
    DecimalFormat df8;
    DecimalFormat df9;
    DecimalFormat df10;
    DecimalFormat df14;
    private static final char CA[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    String mRSIDX;
    int pos[];
    int totalLen = 30;

    public PmKeyMaker(int rs_num) {
        df2 = new DecimalFormat("00");
        df3 = new DecimalFormat("000");
        df4 = new DecimalFormat("0000");
        df5 = new DecimalFormat("00000");
        df8 = new DecimalFormat("00000000");
        df9 = new DecimalFormat("000000000");
        df10 = new DecimalFormat("0000000000");
        df14 = new DecimalFormat("00000000000000");
        mRSIDX = "";
        pos = new int[3];
        for (int i = 0; i < CA.length; i++) {
            if (CA[i] == 'A')
                pos[0] = i;
            if (CA[i] == '0')
                pos[1] = i;
            if (CA[i] == 'a')
                pos[2] = i;
        }

        mRSIDX = df2.format(rs_num);
    }

    public synchronized String nextKey(String transDt, String seq, String phone) {
        if (StringUtils.isEmpty(phone))
            return null;

        if (StringUtils.isEmpty(transDt) || transDt.length() < 8)
            return null;

        /**
         * mms 20자리를 만들기 위해 30자리까지만 사용해야 됨
         * 또한, SMS 는 bind4를 사용해야 됨
         */
        String decStr = new StringBuffer("")
                .append(df10.format(Long.parseLong(seq))) // 10
                .append(transDt.substring(0,8)) // 8
                .append(df10.format(Long.parseLong(phone))) // 10, 01012345678 -> 1012345678
                .append(mRSIDX) // 2
                .toString();
        String encStr = encode(decStr);
//		Util.llog("dec={}, enc={}", decStr, encStr);

        if ("INVALID_FORMAT".equals(encStr)) return null;
        if ("EXCEED_NUM".equals(encStr)) return null;

        return encStr;
    }

    public synchronized String nextKey(String messageId, String phone) {
        if (StringUtils.isEmpty(phone))
            return null;

        if (StringUtils.isEmpty(messageId))
            return null;

        /**
         * mms 20자리를 만들기 위해 30자리까지만 사용해야 됨
         * 또한, SMS 는 bind4를 사용해야 됨
         */
        String decStr = new StringBuffer("")
                .append(df9.format(Long.parseLong(messageId.split("_")[1]))) // 9
                .append(df9.format(Long.parseLong(messageId.split("_")[0]))) // 9
                .append(df10.format(Long.parseLong(phone))) // 10, 01012345678 -> 1012345678
                .append(mRSIDX) // 2
                .toString();
        String encStr = encode(decStr);
//		Util.llog("dec={}, enc={}", decStr, encStr);

        if ("INVALID_FORMAT".equals(encStr)) return null;
        if ("EXCEED_NUM".equals(encStr)) return null;

        return encStr;
    }

//	public synchronized String nextKey() {
//
//		/**
//		 * mms 20자리를 만들기 위해 30자리까지만 사용해야 됨
//		 * 또한, SMS 는 bind4를 사용해야 됨
//		 */
//
//		int length = 12;
//	    boolean useLetters = true;
//	    boolean useNumbers = true;
//	    String generatedString = RandomStringUtils.random(length, useLetters, useNumbers);
//
//		// ex) LGU_yyyyMMdd_XXXXXXXXXX
//		String StrKey = new StringBuffer("")
//				.append("LGU_") // 4
//				.append(Timex.toFormat("yyyyMMdd") + "_") // 9
//				.append(generatedString)
//				.toString();
//
//		return StrKey;
//	}

    public synchronized String nextKey() {

        /**
         * mms 20자리를 만들기 위해 30자리까지만 사용해야 됨
         * 또한, SMS 는 bind4를 사용해야 됨
         */

        //다회선발송을 위한 msgkey 증가.
        int length = 5;
        boolean useLetters = true;
        boolean useNumbers = true;
        String generatedString = RandomStringUtils.random(length, useLetters, useNumbers);

        // ex) LGU_yyyyMMddHH24MISS_XXXXXX
        String StrKey = new StringBuffer("")
                .append("LGU_") // 4
                .append(Timex.toFormat14() + "_") // 15
                .append(generatedString)
                .toString();

        return StrKey;
    }

    public synchronized String findKey(String encKey) {

//		if (encKey.contains("_")) {
//			return encKey;
//		}

        // ex) LGU_yyyyMMddHHmmss_XXXXXXXXXX
        String StrKey = new StringBuffer("")
                .append("LGU_") // 4
                .append(encKey.substring(0, 14) + "_") // 15
                .append(encKey.substring(14))
                .toString();

        return StrKey;
    }

    String encode(String str) {
        if (str.length() % 5 != 0)
            return "INVALID_FORMAT";
        int n = str.length();
        String r_str = "";
        for (int i = 0; i * 5 < n; i++)
            r_str = (new StringBuilder(String.valueOf(r_str)))
                    .append(encode(Util.atoi(str.substring(i * 5, i * 5 + 5)))).toString();

        return r_str;
    }

    String encode(long num) {
        return encode((int) num);
    }

    String encode(int num) {
        String str = "";
        if (num > (int) Math.pow(CA.length, 3D))
            return "EXCEED_NUM";
        for (int i = 2; i >= 0; i--) {
            int x = (int) Math.pow(CA.length, i);
            if (num < x) {
                str = (new StringBuilder(String.valueOf(str))).append(CA[0]).toString();
            } else {
                int dn = num / x;
                str = (new StringBuilder(String.valueOf(str))).append(CA[dn]).toString();
                num -= x * dn;
            }
        }

        return str;
    }

    public String getSeq(String key) {
        if (key == null)
            return null;
        String decode_str = decode(key);
        if (decode_str.length() != totalLen)
            return null;
        else
            return decode_str.substring(0, 10);
    }

    public String getDate(String key) {
        if (key == null)
            return null;
        String decode_str = decode(key);
        if (decode_str.length() != totalLen)
            return null;
        else
            return decode_str.substring(10, 18);
    }

    public String getPhone(String key) {
        if (key == null)
            return null;
        String decode_str = decode(key);
        if (decode_str.length() != totalLen)
            return null;
        else
            return "0"+decode_str.substring(18, 28);
    }


    public String getSvrNum(String key) {
        if (key == null)
            return null;
        String decode_str = decode(key);
        if (decode_str.length() != totalLen)
            return null;
        else
            return decode_str.substring(28, 30);
    }

    String decode(String key) {
        if (key.length() % 3 != 0)
            return "INVALID_KEY";
        String r_str = "";
        for (int i = 0; i < key.length() / 3; i++) {
            int n1 = searchIdx(key.charAt(i * 3));
            int n2 = searchIdx(key.charAt(i * 3 + 1));
            int n3 = searchIdx(key.charAt(i * 3 + 2));
            if (n1 < 0 || n2 < 0 || n3 < 0)
                return "INVALID_KEY_TYPE";
            int num = (int) ((double) n1 * Math.pow(CA.length, 2D) + (double) n2 * Math.pow(CA.length, 1.0D)
                    + (double) n3 * Math.pow(CA.length, 0.0D));
            r_str = (new StringBuilder(String.valueOf(r_str))).append(df5.format(num)).toString();
        }

        return r_str;
    }

    int searchIdx(char c) {
        if (c >= 'A' && c <= 'Z')
            return pos[0] + (c - 65);
        if (c >= '0' && c <= '9')
            return pos[1] + (c - 48);
        if (c >= 'a' && c <= 'z')
            return pos[2] + (c - 97);
        else
            return -1;
    }

    public static void main(String args[]) {
        PmKeyMaker mm = new PmKeyMaker(1);
//        HashMap<String, Integer> genKey = new HashMap<>();
//        HashMap<String, String> msgIdList = new HashMap<>();
//        HashMap<String, Integer> genTime = new HashMap<>();

//		int testCnt = 100000;
//		for (int i = 0; i < testCnt; i++) {
//			String tt = Timex.toFormat14();
//			String msgId = (i+1)+"";
//			String svr_key = mm.nextKey(tt, msgId, "01012345678");
//			if (genKey.get(svr_key)==null)
//				genKey.put(svr_key, 1);
//			else
//				genKey.put(svr_key, genKey.get(svr_key)+1);
//			msgIdList.put(msgId, msgId);
//			if (genTime.get(tt)==null)
//				genTime.put(tt, 1);
//			else
//				genTime.put(tt, genTime.get(tt)+1);
//		}
//		Util.llog("testCnt = {}, genKey = {}, gap = {}, genTime.size={}, msgIdList.size={}",
//				testCnt, genKey.size(), testCnt - genKey.size(), genTime.size(), msgIdList.size());

        Util.llog("{}", mm.nextKey("20180808", "131408001", "01023029234"));

    }

}