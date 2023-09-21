package com.example.mp.gw.bc.service;


import com.example.mp.gw.common.domain.ApiResponseResults;
import com.example.mp.gw.member.domain.StatusRejection;
import com.example.mp.gw.member.domain.StatusRejectionRequest;

/**
 * @Class Name : BcMemberService.java
 * @Description : BizCenter 회원 서비스
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


public interface BcMemberService
{
	public ApiResponseResults<StatusRejection> statusRejections(StatusRejectionRequest rejectionsSearchRequest);
}
