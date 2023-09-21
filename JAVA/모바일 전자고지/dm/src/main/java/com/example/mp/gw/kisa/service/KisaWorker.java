package com.example.mp.gw.kisa.service;


public interface KisaWorker
{
	public void popAndSendEaddrToKisa();

	public void popAndSendDocToKisa();

	public void popAndSendDocRdToKisa();

	public void popAndUpdateDocRslt();

	public void popAndRetryToKisa();

	public void popAndRetryEaddrFromDateToKisa();

	public void popAndRetryEaddrFromSeqToKisa();

	public void selectAndSendEaddrToKisa();

	public void popAndFixDocFromDate();

	public void popAndFixDocFromSeq();

	public void popAndRetryDocFromDateToKisa();

	public void popAndRetryDocFromSeqToKisa();

	public void selectAndFixDoc();

	public void selectAndSendDocToKisa();

	public void selectAndSendDocRdToKisa();

	public void issueKisaToken();

	public void popAndIssueKisaToken();

}
