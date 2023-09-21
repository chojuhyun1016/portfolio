package com.example.mp.gw.kisa.service;


import java.util.List;

import com.example.mp.gw.common.exception.KeepRunningToWorkerException;
import com.example.mp.gw.common.exception.StopRunningToWorkerException;
import com.example.mp.gw.doc.domain.Document;
import com.example.mp.gw.kisa.domain.IssueDocumentRequest;
import com.example.mp.gw.kisa.domain.IssueDocumentResponse;
import com.example.mp.gw.kisa.domain.IssueDocumentStatConfirmationRequest;
import com.example.mp.gw.kisa.domain.IssueDocumentStatConfirmationResponse;
import com.example.mp.gw.kisa.domain.RegisterDocumentCirculationResponse;
import com.example.mp.gw.kisa.domain.RegisterDocumentCirculationsResponse;
import com.example.mp.gw.kisa.exception.FailRequestDocumentIssueToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestDocumentRegisterRdToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestDocumentRegisterToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestDocumentStatsConfirmationIssueToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseDocumentIssueToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseDocumentRegisterRdToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseDocumentRegisterToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseDocumentStatsConfirmationIssueToKisaException;


public interface KisaDocumentService
{
	public void popAndSendDocToKisa(int KISA_DCMNT_SEND_CNT);

	public void popAndSendDocRdToKisa(int KISA_DCMNT_RD_SEND_CNT);

	public void popAndUpdateDocRslt() throws KeepRunningToWorkerException, StopRunningToWorkerException;

	public RegisterDocumentCirculationsResponse registerDocument(Document document) throws FailRequestDocumentRegisterToKisaException, FailResponseDocumentRegisterToKisaException;

	public List<RegisterDocumentCirculationResponse> registerDocumentMulti(List<Document> document) throws FailRequestDocumentRegisterToKisaException;

	public RegisterDocumentCirculationsResponse registerDocumentRd(Document document) throws FailRequestDocumentRegisterRdToKisaException, FailResponseDocumentRegisterRdToKisaException;

	public List<RegisterDocumentCirculationResponse> registerDocumentRdMulti(List<Document> document) throws FailRequestDocumentRegisterRdToKisaException;

	public IssueDocumentResponse issueDocument(IssueDocumentRequest issueRequest) throws FailRequestDocumentIssueToKisaException, FailResponseDocumentIssueToKisaException;

	public IssueDocumentStatConfirmationResponse issueDocumentStatsConfirmation(IssueDocumentStatConfirmationRequest issueRequest) throws FailRequestDocumentStatsConfirmationIssueToKisaException, FailResponseDocumentStatsConfirmationIssueToKisaException;
}
