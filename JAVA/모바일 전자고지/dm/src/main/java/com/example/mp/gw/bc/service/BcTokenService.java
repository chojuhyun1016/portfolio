package com.example.mp.gw.bc.service;


import com.example.mp.gw.bc.domain.BcAccessTokenRequest;
import com.example.mp.gw.bc.domain.BcAccessTokenResponse;

/**
 * @Class Name : BcTokenService.java
 * @Description : BizCenter 토큰 서비스
 * 
 * @author 조주현
 * @since 2021.03.27
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.27	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public interface BcTokenService
{
	public void issueBcToken();

	public void popAndIssueBcToken();

	public BcAccessTokenResponse issueBcAccessToken(BcAccessTokenRequest bcAccessTokenRequest);
}
