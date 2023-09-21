package com.example.mp.gw.member.service;


import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import org.springframework.dao.DataAccessException;

import com.example.mp.gw.member.domain.AgreeRequest;
import com.example.mp.gw.member.domain.RegisterMemberRequest;
import com.example.mp.gw.member.domain.RejectionRequest;
import com.example.mp.gw.member.domain.WhitelistRequest;
import com.example.mp.gw.member.domain.WithdrawMemberRequest;
import com.example.mp.gw.member.exception.FailToCancelRejectionsException;
import com.example.mp.gw.member.exception.FailToWithdrawMemberException;
import com.example.mp.gw.member.exception.NoMemberRequestException;
import com.example.mp.gw.member.exception.NotExistCorpMemberException;
import com.example.mp.gw.member.exception.NotExistMemberTypeException;
import com.example.mp.gw.member.exception.NotLgUplusUserException;


public interface MemberService
{
	public void register(RegisterMemberRequest memberRequest) throws DataAccessException, NoMemberRequestException, NotLgUplusUserException, NoSuchPaddingException, NoSuchAlgorithmException, IOException, NotExistMemberTypeException;

	public void modify(RegisterMemberRequest memberRequest) throws DataAccessException, NoMemberRequestException, NotLgUplusUserException, NoSuchPaddingException, NoSuchAlgorithmException, IOException, NotExistMemberTypeException;

	public void withdraw(WithdrawMemberRequest memberRequest) throws NoSuchAlgorithmException, NoSuchPaddingException, DataAccessException, NotExistCorpMemberException, FailToWithdrawMemberException, NotLgUplusUserException;

	public void addAgree(AgreeRequest agreeRequest);

	public void cancelAgree(AgreeRequest agreeRequest);

	public void addRejections(RejectionRequest rejectionRequest) throws NoSuchAlgorithmException, DataAccessException;

	public void cancelRejections(RejectionRequest rejectionRequest) throws DataAccessException, NoSuchAlgorithmException, FailToCancelRejectionsException;

	public void addWhiteList(WhitelistRequest whiteListRequest) throws NoSuchAlgorithmException, DataAccessException;

	public void cancelWhiteList(WhitelistRequest whiteListRequest) throws NoSuchAlgorithmException, DataAccessException;
}
