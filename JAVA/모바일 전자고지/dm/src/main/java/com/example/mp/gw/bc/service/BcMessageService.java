package com.example.mp.gw.bc.service;


import com.example.mp.gw.bc.domain.MessageResultsRequest;
import com.example.mp.gw.bc.domain.VerifyTokensResultRequest;
import com.example.mp.gw.bc.exception.FailRequestDcmntReadDtmToBcException;
import com.example.mp.gw.bc.exception.FailRequestReportToBcException;
import com.example.mp.gw.bc.exception.FailResponseDcmntReadDtmToBcException;
import com.example.mp.gw.bc.exception.FailResponseReportToBcException;

/**
 * @Class Name : BcMessageService.java
 * @Description : BizCenter 메시지 서비스
 * 
 * @author 조주현
 * @since 2021.07.13
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.07.13	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public interface BcMessageService
{
	public void popAndSendRmdSndRpt(Integer BIZ_RMD_SND_RPT_SEND_CNT);

	public void popAndSendRmdRsvRpt(Integer BIZ_RMD_RSV_RPT_SEND_CNT);

	public void popAndSendSndRpt(Integer BIZ_SND_RPT_SEND_CNT);

	public void popAndSendRsvRpt(Integer BIZ_RSV_RPT_SEND_CNT);

	public void popAndSendRdDtm(Integer BIZ_DOC_RD_DTM_CNT);

	public void sendMsgResult(MessageResultsRequest messageResultsRequest) throws FailRequestReportToBcException, FailResponseReportToBcException;

	public void sendDocumentReadDtm(VerifyTokensResultRequest verifyTokensResultRequest) throws FailRequestDcmntReadDtmToBcException, FailResponseDcmntReadDtmToBcException;
}
