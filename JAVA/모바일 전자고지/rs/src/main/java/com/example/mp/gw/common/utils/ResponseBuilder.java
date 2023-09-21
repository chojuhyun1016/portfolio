package com.example.mp.gw.common.utils;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.mp.gw.common.domain.ApiResponse;
import com.example.mp.gw.common.domain.ApiResponseResult;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.exception.MalformedException;
import com.example.mp.gw.doc.exception.AlreadyExistDocumentException;
import com.example.mp.gw.doc.exception.NotExistDocumentException;
import com.example.mp.gw.kisa.exception.FailRequestDocumentIssueToKisaException;
import com.example.mp.gw.member.exception.AlreadyExistsMemberException;
import com.example.mp.gw.member.exception.AlreadyWithdrawnMemberException;
import com.example.mp.gw.member.exception.FailToAddRejectionsException;
import com.example.mp.gw.member.exception.FailToCancelRejectionsException;
import com.example.mp.gw.member.exception.FailToWithdrawMemberException;
import com.example.mp.gw.member.exception.NoMemberRequestException;
import com.example.mp.gw.member.exception.NoPersonMemberException;
import com.example.mp.gw.member.exception.NoRejectionsException;
import com.example.mp.gw.member.exception.NotExistCorpMemberException;
import com.example.mp.gw.member.exception.NotExistMemberTypeException;
import com.example.mp.gw.member.exception.NotExistPersonMemberException;
import com.example.mp.gw.member.exception.NotExistRejectionsException;
import com.example.mp.gw.member.exception.NotExistsMemberOrNotPersonMemberException;
import com.example.mp.gw.member.exception.NotLgUplusUserException;
import com.example.mp.gw.ms.exception.NoImageFileException;
import com.example.mp.gw.ms.exception.ReportSizeLimitException;
import com.example.mp.gw.token.exception.ExpiredTokenException;
import com.example.mp.gw.token.exception.NotMatchedReadTokenException;

/**
 * @Class Name : ResponseBuilder.java
 * @Description : 응답객체를 만드는 빌더 객체
 * 
 * @author 조주현
 * @since 2021.04.19
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.19	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class ResponseBuilder
{
	@SuppressWarnings("rawtypes")
	private static final Map<Class, ApiResponse> ERROR_MAP = new HashMap<>();


	public static interface API_RESPONSE_CODE
	{
		public static String SUCCESS = "00";
		public static String ERROR   = "01";
	}

	static
	{
		//정상이 아닌경우는 모두 오류로 판단해서 01로셋팅
		putErrorMap("01", new AlreadyExistsMemberException()             , HttpStatus.OK);
		putErrorMap("01", new AlreadyWithdrawnMemberException()          , HttpStatus.OK);
		putErrorMap("01", new MalformedException()                       , HttpStatus.OK);
		putErrorMap("01", new FailToWithdrawMemberException()            , HttpStatus.OK);
		putErrorMap("01", new NoMemberRequestException()                 , HttpStatus.OK);
		putErrorMap("01", new NoPersonMemberException()                  , HttpStatus.OK);
		putErrorMap("01", new NoRejectionsException()                    , HttpStatus.OK);
		putErrorMap("01", new NotExistCorpMemberException()              , HttpStatus.OK);
		putErrorMap("01", new NotExistRejectionsException()              , HttpStatus.OK);
		putErrorMap("01", new NotExistsMemberOrNotPersonMemberException(), HttpStatus.OK);
		putErrorMap("01", new NotExistPersonMemberException()            , HttpStatus.OK);
		putErrorMap("01", new NotExistMemberTypeException()              , HttpStatus.OK);
		putErrorMap("01", new NotLgUplusUserException()                  , HttpStatus.OK);
		putErrorMap("01", new FailToAddRejectionsException()             , HttpStatus.OK);
		putErrorMap("01", new FailToCancelRejectionsException()          , HttpStatus.OK);
		putErrorMap("01", new NotMatchedReadTokenException()             , HttpStatus.OK);
		putErrorMap("01", new ExpiredTokenException()                    , HttpStatus.OK);
		putErrorMap("01", new ReportSizeLimitException()                 , HttpStatus.OK);
		putErrorMap("01", new AlreadyExistDocumentException()            , HttpStatus.OK);
		putErrorMap("01", new FailRequestDocumentIssueToKisaException()  , HttpStatus.OK);
		putErrorMap("01", new NotExistDocumentException()                , HttpStatus.OK); 
		putErrorMap("01", new NoImageFileException()                     , HttpStatus.OK);
		putErrorMap("01", new RuntimeException()                         , HttpStatus.INTERNAL_SERVER_ERROR);
		putErrorMap("01", new Exception()                                , HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public static ResponseEntity<ApiResponseResult> response(List<Map<String, Object>> errors, Exception e)
	{
		ApiResponseResult res = (ApiResponseResult) ERROR_MAP.get(e.getClass());

		if (null != errors && false == errors.isEmpty())
		{
			 res.setErrors(errors);
		}

		if ("00" == res.getResult_cd() || "01" == res.getResult_cd())
		{
			return new ResponseEntity<ApiResponseResult>(res, HttpStatus.OK);
		}
		else
		{
			return new ResponseEntity<ApiResponseResult>(res, res.getStatus());
		}
	}	

	public static <T> ResponseEntity<T> error(T result, Exception e)
	{
		return new ResponseEntity<T>(result, HttpStatus.OK);
	}

	public static ResponseEntity<ApiResponseResult> error(Exception e)
	{
		ApiResponseResult        res      = ERROR_MAP.get(e.getClass()) == null ? (ApiResponseResult) ERROR_MAP.get(Exception.class) : (ApiResponseResult) ERROR_MAP.get(e.getClass()) ;
		List<Map<String,Object>> listMap  = new ArrayList<Map<String,Object>>();
		Map<String, Object>      errorMap = new HashMap<>();

		errorMap = new HashMap<>();
		errorMap.put("error_msg", e.getMessage());
		listMap.add(errorMap);

		res.setErrors(listMap);

		res.setResult_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

		return new ResponseEntity<ApiResponseResult>(res, res.getStatus());
	}

	public static ResponseEntity<ApiResponseResult> success()
	{
		return new ResponseEntity<ApiResponseResult>(ApiResponseResult.builder()
																	.result_cd(API_RESPONSE_CODE.SUCCESS)
																	.result_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
																	.errors(Collections.emptyList())
														  .build()
											 , HttpStatus.OK);
	}

	public static ResponseEntity<ApiResponseResult> success(long mills)
	{
		return new ResponseEntity<ApiResponseResult>(ApiResponseResult.builder()
																	.result_cd(API_RESPONSE_CODE.SUCCESS)
																	.result_dt(FORMATTER.yyyyMMddHHmmss.val().format(mills))
																	.errors(Collections.emptyList())
														  .build()
											 , HttpStatus.OK);
	}

	public static <T> ResponseEntity<T> success(T result)
	{
		return new ResponseEntity<T>(result, HttpStatus.OK);
	}

	@SuppressWarnings("unused")
	private static void putErrorMap(String code, Exception e)
	{
		ERROR_MAP.put(e.getClass(), buildApiResponse(code, e));
	}

	private static void putErrorMap(String code, Exception e, HttpStatus status)
	{
		ERROR_MAP.put(e.getClass(), buildApiResponse(code, status));
	}

	private static ApiResponseResult buildApiResponse(String code, HttpStatus status)
	{
		return ApiResponseResult.builder().result_cd(code).status(status).errors(Collections.emptyList()).build();
	}

	private static ApiResponseResult buildApiResponse(String code, Exception e)
	{
		return ApiResponseResult.builder().result_cd(code).status(HttpStatus.BAD_REQUEST).errors(Collections.emptyList()).build();
	}
}
