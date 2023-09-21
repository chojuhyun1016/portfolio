package com.example.mp.gw.bc.service;


import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.exception.KeepRunningToWorkerException;
import com.example.mp.gw.common.exception.StopRunningToWorkerException;


public interface BcRetryService
{
	public void popAndRetryToBc() throws KeepRunningToWorkerException, StopRunningToWorkerException;

	public void caseApproveToSend(RedisQueueDataWrapper<?> request);

	public void caseRmdRptSndToSend(RedisQueueDataWrapper<?> request);

	public void caseRmdRptRsvToSend(RedisQueueDataWrapper<?> request);

	public void caseRptSndToSend(RedisQueueDataWrapper<?> request);

	public void caseRptRsvToSend(RedisQueueDataWrapper<?> request);

	public void caseDocReadToSend(RedisQueueDataWrapper<?> request);

	public void caseDocIssSndToSend(RedisQueueDataWrapper<?> request);
	
	public void caseDocStsCfmIssSndToSend(RedisQueueDataWrapper<?> request);
}
