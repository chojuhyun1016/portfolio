package com.example.mp.gw.bc.service;


public interface BcWorker
{
	public void popAndSendAgree();

	public void popAndSendSndRpt();

	public void popAndSendRmdRsvRpt();

	public void popAndSendRmdSndRpt();

	public void popAndSendRsvRpt();

	public void popAndSendRdDtm();

	public void popAndSendIssDoc();

	public void popAndSendIssDocStsCfm();

	public void issueBcToken();

	public void popAndIssueBcToken();

	public void popAndRetryToBc();
}
