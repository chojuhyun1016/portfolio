package com.example.mp.gw.common.utils;
//package com.uplus.mp.gw.common.utils;
//
//
//import java.net.URI;
//import java.net.URLDecoder;
//import java.net.URLEncoder;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.HttpServerErrorException;
//import org.springframework.web.client.RestTemplate;
//
//import com.uplus.mp.gw.common.utils.enc.AES256Cipher;
//
//import io.netty.handler.timeout.ReadTimeoutException;
//import lombok.extern.slf4j.Slf4j;
//
///**
// * @Class Name : NcasUtil.java
// * @Description : NCAS API 통신 유틸
// *
// * @author 조주현
// * @since 2021. 4. 19.
// * @version 1.0
// * @see
// *
// *      <pre>
// * << 개정이력(Modification Information) >>
// *
// *   수정일			수정자		수정내용
// *  -----------  ----------	------------------------------
// *  2021. 4. 19.	조주현	    최초 생성
// *
// *      </pre>
// *
// */
//
//
//@Slf4j
//@Component
//public class NcasUtil
//{
//	private static final Logger logger = LoggerFactory.getLogger(NcasUtil.class);
//
//	/** 연결시도 타임아웃 */
//	private static int connectTimeout;
//
//	@Value("${spring.ncas.connect-timeout}")
//	public void setConnectTimeout(int value)
//	{
//		connectTimeout = value;
//	}
//
//	/** 읽기 타임아웃 */
//	private static int readTimeout;
//
//	@Value("${spring.ncas.read-timeout}")
//	public void setReadTimeout(int value)
//	{
//		readTimeout = value;
//	}
//
//	/** 연결 실패 시 재시도 횟수 */
//	private static int maxRetryCnt;
//
//	@Value("${spring.ncas.max-retry-cnt}")
//	public void setMaxRetryCnt(int value)
//	{
//		maxRetryCnt = value;
//	}
//
//	/** 연결 재시도 간 딜레이 시각(밀리세컨드) */
//	private static int sleepMilsec;
//
//	@Value("${spring.ncas.sleep-milsec}")
//	public void setSleepMilsec(int value)
//	{
//		sleepMilsec = value;
//	}
//
//	/** NCAS api url */
//	private static String url;
//
//	@Value("${spring.ncas.url}")
//	public void setUrl(String value)
//	{
//		url = value;
//	}
//
//	/** NCAS api USER */
//	private static String ncasUsr;
//
//	@Value("${spring.ncas.ncasUsr}")
//	public void setNcasUsr(String value)
//	{
//		ncasUsr = value;
//	}
//
//	/** NCAS api KEY */
//	private static String ncasKey;
//
//	@Value("${spring.ncas.ncasKey}")
//	public void setNcasKey(String value)
//	{
//		ncasKey = value;
//	}
//
//	/** 암호화 키 */
//	private static String encryptKey;
//
//	@Value("${spring.ncas.encrypt-key}")
//	public void setEncryptKey(String value)
//	{
//		encryptKey = value;
//	}
//
//	/**
//	 * NCAS API 통신
//	 * ※ NCAS API 통신 후 Map형태로 반환한다.
//	 * @param url : 통신 url
//	 * @param paramVo : 파라미터
//	 * @return
//	 */
//	public static Map<String, Object> callNcas (String url, Map<String,Object> param)
//	{
//		/* 파라미터 인자 존재 시 url에 병합 (S) */
//		if (param != null && param.keySet().size() > 0)
//		{
//			StringBuilder sb = new StringBuilder();
//
//			sb.append("?");
//
//			for (String key : param.keySet())
//			{
//				sb.append(key);
//				sb.append("=");
//				sb.append(param.get(key));
//				sb.append("&");
//			}
//
//			url = url + (sb.toString().substring(0, sb.toString().length() - 1));
//		}
//		/* 파라미터 인자 존재 시 url에 병합 (E) */
//
//		return callNcas(url);
//	}
//
//	public static Map<String, Object> callNcas (String url)
//	{
//		log.debug("url info : "+url);
//		Map<String, Object> resMap = new HashMap<String, Object>();
//		HttpComponentsClientHttpRequestFactory factory = null;
//
//		int retryCnt = 0;	// 재시도 횟수
//
//		while (true)
//		{
//			try
//			{
//				factory = new HttpComponentsClientHttpRequestFactory();
//
//				/* 타임아웃 셋팅 */
//				factory.setConnectTimeout(connectTimeout);	// 연결시도 타임아웃
//				factory.setReadTimeout(readTimeout);		// 읽기 타임아웃
//
//				/* 템플릿 객체 생성 */
//				RestTemplate restTemplate = new RestTemplate(factory);
//
//				/* 헤더 설정 (S) */
//				HttpHeaders header = new HttpHeaders();
//
//				header.setContentType(MediaType.TEXT_HTML);
//
//				HttpEntity<?> entity = new HttpEntity<>(header);
//				/* 헤더 설정 (E) */
//
//				/* 이 한줄의 코드로 API를 호출해 MAP타입으로 전달 받는다. */
//				ResponseEntity<String> respStr = restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, String.class);
//
//				/* Map 형태로 파싱 */
//
//				String tmpResTxt = respStr.getHeaders().get("RESP").get(0);
//
//				for (String keyValue : tmpResTxt.split(" *& *"))
//				{
//					String[] pairs = keyValue.split(" *= *", 2);
//
//					resMap.put(pairs[0], pairs.length == 1 ? "" : URLDecoder.decode(pairs[1], "UTF-8"));
//				}
//
//				break;
//			}
//			catch (HttpClientErrorException e)
//			{
//				logger.error("call().HttpClientErrorException : API 통신 시, 클라이언트 오류 발생");
//				logger.error("errorMessage" + e.getMessage());
//				resMap.put("RESPCODE", "99");
//
//				break;
//			}
//			catch (HttpServerErrorException e)
//			{
//				logger.error("call().HttpServerErrorException : API 통신 시, 서버 오류 발생");
//				resMap.put("RESPCODE", "99");
//
//				break;
//			}
//			catch (ReadTimeoutException e)
//			{
//				if (maxRetryCnt >= retryCnt)
//				{
//					try
//					{
//						Thread.sleep(sleepMilsec);
//					}
//					catch (InterruptedException ie)
//					{
//						logger.error("call().InterruptedException : API 통신 시, 오류발생 시, 재시도 중 오류 발생");
//					}
//					finally
//					{
//						retryCnt++;
//					}
//				}
//				else
//				{
//					logger.error("call().ReadTimeoutException : API 통신 시, 읽기 타임아웃 오류 발생");
//					resMap.put("RESPCODE", "99");
//
//					break;
//				}
//			}
//			catch (Exception e)
//			{
//				logger.error("call().Exception : API 통신 시, 기타 오류 발생");
//				logger.error("errorMessage" + e.getMessage());
//				resMap.put("RESPCODE", "99");
//
//				break;
//			}
//		}
//
//		return resMap;
//	}
//
//	/**
//	 * 매개변수(휴대폰 정보)에 맞는 CI정보(String)를 가져온다.
//	 * @param phone
//	 * @return
//	 */
//	public static String getCi (String phone)
//	{
//		String ipinCi = null;
//
//		try
//		{
//			AES256Cipher aes256Cipher = new AES256Cipher(encryptKey);
//			Map<String, Object> param = new HashMap<String, Object>();
//
//			String frontNo = phone.substring(0, 3);
//
//			phone = phone.substring(3, phone.length());
//			phone = setLPad(phone, 9, "0");
//			phone = frontNo + phone;
//
//			param.put("CPID", aes256Cipher.decrypt(ncasUsr));
//			param.put("CPPWD", aes256Cipher.decrypt(ncasKey));
//			param.put("CASECODE", "TF1666");
//			param.put("CPTYPE", "I");
//			param.put("CTN", phone);
//
//			Map<String, Object> map = callNcas(url, param);
//
//			if (map.get("REAL_IPIN_CI") != null)
//			{
//				ipinCi = map.get("REAL_IPIN_CI").toString();
//			}
//		}
//		catch (Exception e)
//		{
//			// TODO: handle exception
//			ipinCi = null;
//		}
//
//		return ipinCi;
//	}
//
//	/**
//	 * CI정보(String) 폰 소유자명을 가져온다.
//	 * @param phone
//	 * @return
//	 */
//	public static String getPhoneOwnerName (String phone)
//	{
//		String ownerName = null;
//
//		try
//		{
//			AES256Cipher aes256Cipher = new AES256Cipher(encryptKey);
//			Map<String, Object> param = new HashMap<String, Object>();
//
//			String frontNo = phone.substring(0, 3);
//
//			phone = phone.substring(3, phone.length());
//			phone = setLPad(phone, 9, "0");
//			phone = frontNo + phone;
//
//			param.put("CPID", aes256Cipher.decrypt(ncasUsr));
//			param.put("CPPWD", aes256Cipher.decrypt(ncasKey));
//			param.put("CASECODE", "TF1666");
//			param.put("CPTYPE", "I");
//			param.put("CTN", phone);
//
//			Map<String, Object> map = callNcas(url, param);
//
//			if (map.get("PERS_NAME") != null)
//			{
//				ownerName = map.get("PERS_NAME").toString();
//			}
//		}
//		catch (Exception e)
//		{
//			// TODO: handle exception
//			ownerName = null;
//		}
//
//		return ownerName;
//	}
//
//	/**
//	 * 매개변수(ci)에 맞는 휴대폰 목록(List)을 가져온다.
//	 * @param ci
//	 * @return
//	 */
//	public static List<String> getPhoneList (String ci)
//	{
//		List<String> phoneList = new ArrayList<String>();
//
//		try
//		{
//			AES256Cipher aes256Cipher = new AES256Cipher(encryptKey);	// 인증키 셋팅
//			Map<String, Object> param = new HashMap<String, Object>();
//
//			param.put("CPID", aes256Cipher.decrypt(ncasUsr));
//			param.put("CPPWD", aes256Cipher.decrypt(ncasKey));
//			param.put("CASECODE", "TF1668");
//			param.put("CPTYPE", "I");
//			param.put("IPIN_CI", URLEncoder.encode(ci, "UTF-8"));
//
//			Map<String, Object> map = callNcas(url, param);
//
//			if (map.get("CUST_STTS_LIST") != null && !"".equals(map.get("CUST_STTS_LIST")))
//			{
//				String tmpPhone = "";
//				String tmpFirstNo = "";
//
//				if (((String) map.get("CUST_STTS_LIST")).contains("|"))
//				{
//					String[] tmpArr = ((String) map.get("CUST_STTS_LIST")).split("|");
//
//					for (String row : tmpArr)
//					{
//						tmpPhone = row.split(",")[0];
//						tmpFirstNo = tmpPhone.substring(0, 3);
//						tmpPhone = tmpPhone.substring(3, tmpPhone.length());
//
//						if (tmpPhone.startsWith("00"))
//						{
//							tmpPhone = tmpPhone.substring(2, tmpPhone.length());
//						}
//						else
//						{
//							tmpPhone = tmpPhone.substring(1, tmpPhone.length());
//						}
//
//						tmpPhone = tmpFirstNo + tmpPhone;
//
//						phoneList.add(tmpPhone);
//					}
//				}
//				else
//				{
//					tmpPhone = ((String) map.get("CUST_STTS_LIST")).split(",")[0];
//					tmpFirstNo = tmpPhone.substring(0, 3);
//					tmpPhone = tmpPhone.substring(3, tmpPhone.length());
//
//					if (tmpPhone.startsWith("00"))
//					{
//						tmpPhone = tmpPhone.substring(2, tmpPhone.length());
//					}
//					else
//					{
//						tmpPhone = tmpPhone.substring(1, tmpPhone.length());
//					}
//
//					tmpPhone = tmpFirstNo + tmpPhone;
//
//					phoneList.add(tmpPhone);
//				}
//			}
//		}
//		catch (Exception e)
//		{
//			// TODO: handle exception
//			phoneList = null;
//		}
//
//		return phoneList;
//	}
//
//	private static String setLPad( String strContext, int iLen, String strChar )
//	{
//		String strResult = "";
//
//		StringBuilder sbAddChar = new StringBuilder();
//
//		for (int i = strContext.length(); i < iLen; i++)
//		{
//			// iLen길이 만큼 strChar문자로 채운다.
//			sbAddChar.append(strChar);
//		}
//
//		strResult = sbAddChar + strContext; // LPAD이므로, 채울문자열 + 원래문자열로 Concate한다.
//
//		return strResult;
//	}
//}
