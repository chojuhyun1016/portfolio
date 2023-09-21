package com.example.mp.gw.bc.service;


import com.example.mp.gw.bc.domain.DocumentIssueResultRequest;
import com.example.mp.gw.bc.domain.DocumentStatConfirmationIssueResultRequest;
import com.example.mp.gw.bc.exception.FailRequestDcmntIssueToBcException;
import com.example.mp.gw.bc.exception.FailRequestDcmntStsCfmIssueToBcException;
import com.example.mp.gw.bc.exception.FailResponseDcmntIssueToBcException;
import com.example.mp.gw.bc.exception.FailResponseDcmntStsCfmIssueToBcException;


/**
 * @Class Name : BcDocumentService.java
 * @Description : BizCenter 유통증명서 서비스
 * 
 * @author 조주현
 * @since 2021.12.18
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.18	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public interface BcDocumentService
{
	public void popAndSendIssDoc();

	public void popAndSendIssDocStsCfm();

	public void sendIssueDocument(DocumentIssueResultRequest issueResultRequest) throws FailRequestDcmntIssueToBcException, FailResponseDcmntIssueToBcException;

	public void sendIssueDocumentStatsConfirmation(DocumentStatConfirmationIssueResultRequest issueResultRequest) throws FailRequestDcmntStsCfmIssueToBcException, FailResponseDcmntStsCfmIssueToBcException;
}
