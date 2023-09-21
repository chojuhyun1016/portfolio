package com.example.mp.gw.common.utils;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : StringUtil.java
 * @Description : 문자열 유틸리티 
 * 
 * @author 조주현
 * @since 2021.04.04
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.04	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Slf4j
public class StringUtil
{
	final static String DEFAULT_STRING = "";

	public static String getBrowser(HttpServletRequest req)
	{
		String userAgent = req.getHeader("User-Agent");

		if ( userAgent.indexOf("MSIE") > -1 
		 || userAgent.indexOf("Trident") > -1 //IE11 
		 || userAgent.indexOf("Edge") > -1)
		{ 
			return "MSIE"; 
		}
		else if (userAgent.indexOf("Chrome") > -1)
		{ 
			return "Chrome"; 
		}
		else if (userAgent.indexOf("Opera") > -1)
		{ 
			return "Opera"; 
		}
		else if (userAgent.indexOf("Safari") > -1)
		{ 
			return "Safari"; 
		}
		else if (userAgent.indexOf("Firefox") > -1)
		{ 
			return "Firefox"; 
		}
		else
		{ 
			return null; 
		} 
	}

	public static String getFileNm(String browser, String fileName) throws UnsupportedEncodingException
	{
		String convName = "";

		log.debug("browser : " + browser);
		log.debug("get fileName : " + fileName);

		if ("MSIE".equals(browser))
		{
		    convName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
		}
		else if ("Chrome".equals(browser) || "MSIE10".equals(browser) || "MSIE11".equals(browser) || "Edge".equals(browser))
		{
		    StringBuffer sb = new StringBuffer();

		    for (int i = 0; i < fileName.length(); i++)
		    {
		    	char c = fileName.charAt(i);

		    	if (c > '~')
		    	{
		    		sb.append(URLEncoder.encode(Character.toString(c), "UTF-8"));
		    	}
		    	else
		    	{
		    		sb.append(c);
		    	}
		    }

		    convName = sb.toString();
		}
		else if ("Opera".equals(browser))
		{
		    convName = "\"" + new String(fileName.getBytes("UTF-8"), "8859_1") + "\"";
		}
		//Firefox,Mobile
		else
		{
		    convName = "\"" + new String(fileName.getBytes("UTF-8"), "8859_1") + "\"";
		}

		return convName;
	}
	
	public static String toStringFrom(Object value)
	{
		if (null == value)
			return DEFAULT_STRING;

		return String.valueOf(value);
	}

	public static String yyyyMMdd(String date)
	{
		if (null == date)
			return "";

		if (14 == date.length())
		{
			return date.substring(0,8);
		}

		return "";
	}
	
	public static String yyyyMMddHHmmss(String datetime)
	{
		if (!StringUtils.hasText(datetime))
			return "";
		
		String format1 = "yyyyMMddHHmmss";
		String format2 = "yyyy-MM-dd HH:mm:ss";

		SimpleDateFormat fomatter1 = new SimpleDateFormat(format1, Locale.KOREA);
		SimpleDateFormat fomatter2 = new SimpleDateFormat(format2, Locale.KOREA);

		Date targetDate = null;

		try
		{
			targetDate = fomatter1.parse(datetime);
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fomatter2.format(targetDate);
	}
}
