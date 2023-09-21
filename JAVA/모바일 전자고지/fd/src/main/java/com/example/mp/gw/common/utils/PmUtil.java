package com.example.mp.gw.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.SimpleDateFormat;
import java.util.*;

public class PmUtil {
	static Logger logger = LoggerFactory.getLogger(PmUtil.class);

	static String getName() {
		return PmUtil.class.getSimpleName();
	}



	// 날짜 계산
	public static String doDateAdd(int days) {
		return commDateAdd(days, Calendar.DAY_OF_YEAR);
	}
	
	// 시간 계산
	public static String doTimeAdd(int hours) {
		return commDateAdd(hours, Calendar.HOUR);
	}
	
	// 월 계산
	public static String doMonthAdd(int months) {
		return commDateAdd(months, Calendar.MONTH);
	}
	
	// 년, 월, 일, 시 계산
	public static String commDateAdd(int num, int type) {
		Calendar cal = new GregorianCalendar(Locale.KOREA);
		cal.setTime(new Date());
		cal.add(type, num);
		
		SimpleDateFormat fm = null;
		if (Calendar.HOUR == type)
			fm = new SimpleDateFormat("yyyyMMddHHmmss");
		else if (Calendar.MONTH == type)
			fm = new SimpleDateFormat("MM");
		else
			fm = new SimpleDateFormat("yyyyMMdd");
		
		String strDate = fm.format(cal.getTime());
		return strDate;
	}

	

}
