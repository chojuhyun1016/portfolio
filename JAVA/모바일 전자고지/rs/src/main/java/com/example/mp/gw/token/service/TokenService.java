package com.example.mp.gw.token.service;


import com.example.mp.gw.doc.domain.Document;
import com.example.mp.gw.doc.domain.DocumentRd;
import com.example.mp.gw.token.domain.GenerateReadTokenRequest;
import com.example.mp.gw.token.domain.VerifyReadTokenRequest;
import com.example.mp.gw.token.domain.VerifyReplaceTokenRequest;
import com.example.mp.gw.token.domain.VerifyTokenRequest;

/**
 * @Class Name : TokenService.java
 * @Description : 토큰 서비스
 * 
 * @author 조주현
 * @since 2021.04.22
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.22	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public interface TokenService
{
	public String generateReadToken(GenerateReadTokenRequest request);

	public DocumentRd verifyToken(VerifyTokenRequest request);

	public Document verifyReadToken(VerifyReadTokenRequest request);

	public void verifyReplaceToken(VerifyReplaceTokenRequest request);
}
