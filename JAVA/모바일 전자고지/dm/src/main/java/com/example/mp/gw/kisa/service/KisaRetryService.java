package com.example.mp.gw.kisa.service;


import java.util.Map;

import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.exception.KeepRunningToWorkerException;
import com.example.mp.gw.common.exception.StopRunningToWorkerException;
import com.example.mp.gw.doc.domain.DocumentFail;
import com.example.mp.gw.member.domain.MemberEaddrFail;


public interface KisaRetryService
{
	public void popAndRetryToKisa() throws KeepRunningToWorkerException, StopRunningToWorkerException;

	public void caseEaddrToSend(RedisQueueDataWrapper<?> request);

	public void caseDocRegToSend(RedisQueueDataWrapper<?> request);

	public void caseDocRegRdToSend(RedisQueueDataWrapper<?> request);

	public void caseDocRsltUpdate(RedisQueueDataWrapper<?> request);

	public void popAndRetryEaddrFromDateToKisa(int KISA_EADDR_RETRY_DATE_CNT);

	public void popAndRetryEaddrFromSeqToKisa(int KISA_EADDR_RETRY_SEQ_CNT);

	public void selectAndSendEaddrToKisa(int KISA_EADDR_RETRY_CNT);

	public void sendRetryEaddrToKisa(MemberEaddrFail req);

	public int sendRetryEaddrToKisaT(Map<String, Object> searchKey, String now);

	public void popAndFixDocFromDate(int KISA_DCMNT_FIX_DATE_CNT);

	public void popAndFixDocFromSeq(int KISA_DCMNT_FIX_SEQ_CNT);

	public void popAndRetryDocFromDateToKisa(int KISA_DCMNT_RETRY_DATE_CNT);

	public void popAndRetryDocFromSeqToKisa(int KISA_DCMNT_RETRY_SEQ_CNT);

	public void selectAndFixDoc(int KISA_DCMNT_FIX_CNT);

	public void selectAndSendDocToKisa(int KISA_DCMNT_RETRY_CNT);

	public void selectAndSendDocRdToKisa(int KISA_DCMNT_RD_RETRY_CNT);

	public void sendRetryDocToKisa(DocumentFail req);

	public int fixRetryDocRsvDtm(Map<String, Object> searchKey);

	public int sendRetryDocToKisaT(Map<String, Object> searchKey, String now, String structure);
}
