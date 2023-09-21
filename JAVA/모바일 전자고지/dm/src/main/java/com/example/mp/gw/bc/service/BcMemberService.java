package com.example.mp.gw.bc.service;


import java.util.List;

import com.example.mp.gw.bc.domain.ApprovesRequest;
import com.example.mp.gw.bc.domain.StatusRejection;
import com.example.mp.gw.bc.domain.StatusRejectionRequest;
import com.example.mp.gw.bc.exception.FailRequestApproveToBcException;
import com.example.mp.gw.bc.exception.FailRequestRejectionToBcException;
import com.example.mp.gw.bc.exception.FailRequestStatusRejectionToBcException;
import com.example.mp.gw.bc.exception.FailRequestWhitelistToBcException;
import com.example.mp.gw.bc.exception.FailResponseApproveToBcException;
import com.example.mp.gw.bc.exception.FailResponseRejectionToBcException;
import com.example.mp.gw.bc.exception.FailResponseStatusRejectionToBcException;
import com.example.mp.gw.bc.exception.FailResponseWhitelistToBcException;
import com.example.mp.gw.member.domain.Member;
import com.example.mp.gw.member.domain.Rejection;
import com.example.mp.gw.member.domain.Whitelist;

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
	public void popAndSendAgree(Integer BIZ_AGREE_SEND_CNT);

	public void transferApproves(ApprovesRequest approvesRequest) throws FailRequestApproveToBcException, FailResponseApproveToBcException;

	public void transferRejections(Rejection rejectionRequest) throws FailRequestRejectionToBcException, FailResponseRejectionToBcException;

	public void transferAllRejections(Member rejectionRequest) throws FailRequestRejectionToBcException, FailResponseRejectionToBcException;

	public void transferCencelRejections(Rejection rejectionRequest) throws FailRequestRejectionToBcException, FailResponseRejectionToBcException;

	public List<StatusRejection> transferStatusRejections(StatusRejectionRequest rejectionRequest) throws FailRequestStatusRejectionToBcException, FailResponseStatusRejectionToBcException;

	public void transferWhitelist(Whitelist whitelistRequest) throws FailRequestWhitelistToBcException, FailResponseWhitelistToBcException;
}
