package com.example.mp.gw.kisa.service;


import java.util.List;

import org.springframework.http.ResponseEntity;

import com.example.mp.gw.common.exception.KeepRunningToWorkerException;
import com.example.mp.gw.common.exception.StopRunningToWorkerException;
import com.example.mp.gw.kisa.exception.FailReponseRegisterToKisaException;
import com.example.mp.gw.kisa.exception.FailReponseWithdrawToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestEaddrSearchWithdrawHistoryToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestModifyToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestRegisterToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestSearchToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestWithdrawToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseEaddrSearchWithdrawHistoryToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseModifyToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseSearchToKisaException;
import com.example.mp.gw.member.domain.MemberEaddr;


public interface KisaMemberService
{
	public void popAndSendEaddrToKisa() throws KeepRunningToWorkerException, StopRunningToWorkerException;

	public List<MemberEaddr> findEaddrReq(MemberEaddr member);

	public void sendEaddrToKisa(MemberEaddr member);

	public ResponseEntity<?> registerEaddr(MemberEaddr member) throws FailRequestRegisterToKisaException, FailReponseRegisterToKisaException;

	public ResponseEntity<?> withdrawEaddr(MemberEaddr member) throws FailRequestWithdrawToKisaException, FailReponseWithdrawToKisaException;

	public ResponseEntity<?> modifyEaddr(MemberEaddr member) throws FailRequestModifyToKisaException, FailResponseModifyToKisaException;

	public ResponseEntity<?> searchEaddr(MemberEaddr member) throws FailRequestSearchToKisaException, FailResponseSearchToKisaException;

	public ResponseEntity<?> searchEaddrWithdrawHist(MemberEaddr member) throws FailRequestEaddrSearchWithdrawHistoryToKisaException, FailResponseEaddrSearchWithdrawHistoryToKisaException;

	public ResponseEntity<?> searchEaddrOwnerInfo(MemberEaddr member) throws FailRequestEaddrSearchWithdrawHistoryToKisaException, FailResponseEaddrSearchWithdrawHistoryToKisaException;
}
