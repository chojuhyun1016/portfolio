package com.example.mp.gw.member.service.impl;


import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.FileInfo;
import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.Const.MEMBER;
import com.example.mp.gw.common.domain.RedisStructure.Q_DEL_AGREE_RQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_DEL_REJECTION_RQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_DEL_WHITE_LIST_RQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_MB_DEL_RQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_MB_MOD_RQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_MB_REG_RQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_REG_AGREE_RQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_REG_REJECTION_RQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_REG_WHITE_LIST_RQ;
import com.example.mp.gw.common.exception.MalformedException;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.common.utils.enc.Encryptor;
import com.example.mp.gw.member.domain.Agree;
import com.example.mp.gw.member.domain.AgreeRequest;
import com.example.mp.gw.member.domain.Member;
import com.example.mp.gw.member.domain.RegisterCorpMemberRequest;
import com.example.mp.gw.member.domain.RegisterMemberRequest;
import com.example.mp.gw.member.domain.RegisterPersonMemberRequest;
import com.example.mp.gw.member.domain.Rejection;
import com.example.mp.gw.member.domain.RejectionRequest;
import com.example.mp.gw.member.domain.Whitelist;
import com.example.mp.gw.member.domain.WhitelistRequest;
import com.example.mp.gw.member.domain.WithdrawMemberRequest;
import com.example.mp.gw.member.domain.WithdrawPersonMemberRequest;
import com.example.mp.gw.member.exception.AlreadyExistsMemberException;
import com.example.mp.gw.member.exception.FailToCancelRejectionsException;
import com.example.mp.gw.member.exception.FailToWithdrawMemberException;
import com.example.mp.gw.member.exception.NoMemberRequestException;
import com.example.mp.gw.member.exception.NotExistCorpMemberException;
import com.example.mp.gw.member.exception.NotExistMemberTypeException;
import com.example.mp.gw.member.exception.NotExistPersonMemberException;
import com.example.mp.gw.member.exception.NotExistRejectionsException;
import com.example.mp.gw.member.exception.NotLgUplusUserException;
import com.example.mp.gw.member.mappers.altibase.MemberMapper;
import com.example.mp.gw.member.service.MemberService;


@Service("MemberService")
public class MemberServiceImpl implements MemberService
{
	@Autowired
	private MemberMapper memberMapper;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Member>> redisRegister;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Member>> redisWithdraw;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Member>> redisModify;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Agree>> redisAgree;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Rejection>> redisAddRejection;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Rejection>> redisRemoveRejection;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Whitelist>> redisAddWhitelist;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Whitelist>> redisCanceleWhitelist;


	/**
	 * 회원(개인, 법인) 가입 
	 * @param RegisterMemberRequest
	 * @return
	 * @throws DataAccessException 
	 * @throws NoMemberRequestException
	 * @throws NotLgUplusUserException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws NotExistMemberTypeException
	 */
	@Override
	@Transactional(readOnly = true)
	public void register(RegisterMemberRequest memberRequest) throws DataAccessException, NoMemberRequestException, NotLgUplusUserException, NoSuchPaddingException, NoSuchAlgorithmException, IOException, NotExistMemberTypeException
	{
		// 요청 객체 확인
		if (null == memberRequest)
			throw new NoMemberRequestException();

		// 요청일자(가입일자) - 미존재시 현재시각
		final String reqDtm = StringUtils.hasText(memberRequest.getReqDt()) ? memberRequest.getReqDt() : FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());		

		// 등록 요청 객체
		Member willRegisterMemeber = null;

		// 일반 회원 가입
		if (memberRequest instanceof RegisterPersonMemberRequest)
		{
			RegisterPersonMemberRequest personMemberRequest = (RegisterPersonMemberRequest) memberRequest;

			// 1. 기존 탈퇴 이력에서 공인전자주소(CEA) 조회
			final String cea = memberMapper.getWithdrawCeaByCi(Encryptor.encryptCi(personMemberRequest.getCi()));

			// 2. 회원 객체 만들기
			willRegisterMemeber = Member.builder()
												  .type(MEMBER.TYPE.PERSON.val())
												  .cea(StringUtils.hasText(cea)?cea:"LGU_" + UUID.randomUUID().toString())
												  .ci(personMemberRequest.getCi())
												  .phone(personMemberRequest.getPhone())
												  .birthday(personMemberRequest.getBirthday())
												  .name(personMemberRequest.getName())
												  .gender(personMemberRequest.getGender())
												  .createdDtm(reqDtm)
												  .reqType(Const.MEMBER.REQ_TYPE.REGISTER.val())
												  .reqRoute(personMemberRequest.getReqRoute())
												  .messageId(personMemberRequest.getMessageId())
												  .regDtm(personMemberRequest.getRegDt())
												  .agreeDtm(personMemberRequest.getApproveDt())
												  .svcOrgCd(personMemberRequest.getSvcOrgCd())
										.build();

			// 3, 회원 객체 없을시 에러
			if (null == willRegisterMemeber)
				throw new NoMemberRequestException();

			// 4. 공인전자주소 중복확인 (개인)
	        if (0 != memberMapper.getCeaCountByCi(Encryptor.encryptCi(willRegisterMemeber.getCi())))
	        	throw new AlreadyExistsMemberException();
		}
		// 법인 회원 가입(현재 Biz-center 에서만 가입 및 요청이 가능하므로 쓰이지 않음)
		else if (memberRequest instanceof RegisterCorpMemberRequest)
		{
			RegisterCorpMemberRequest corpMemberRequest = (RegisterCorpMemberRequest) memberRequest;

			// 1. 회원 객체 만들기
			willRegisterMemeber = Member.builder()
												  .cea(corpMemberRequest.getPart1().getOfap_elct_addr())
												  .name(corpMemberRequest.getPart1().getBrno_nm())
												  .createdDtm(reqDtm)
												  .type(MEMBER.TYPE.CORP.val())
												  .busiNum(corpMemberRequest.getPart1().getBrno())
												  .reqType(Const.MEMBER.REQ_TYPE.REGISTER.val())
												  .reqRoute(Const.MEMBER.ROUTE.BC.val())
												  .svcOrgCd(corpMemberRequest.getPart1().getService_cd())
												  .svcOrgName(corpMemberRequest.getPart1().getBiz_nm())
												  .svcOrgType(corpMemberRequest.getPart1().getType())
												  .agreeYn(true == StringUtils.hasText(corpMemberRequest.getPart1().getAgree_yn())
												  					? corpMemberRequest.getPart1().getAgree_yn()
												  					: "N")								// 수신동의상태 전송 여부
												  .file1(new FileInfo(corpMemberRequest.getPart2()))	// 사업자 등록증
												  .file2(new FileInfo(corpMemberRequest.getPart3()))	// 공인전자주소 등록 신청서
										.build();

			// 2, 회원 객체 없을시 에러
			if (null == willRegisterMemeber)
				throw new NoMemberRequestException();

			// 3. 공인전자주소 중복확인 (법인)
	        if (0 != memberMapper.existsSameCorpMemberBySvcOrgCd(willRegisterMemeber))
	        	throw new AlreadyExistsMemberException();
		}
		else
		{
			throw new NotExistMemberTypeException();
		}

		// 5. 회원가입 레디스 요청큐에 넣어주기
		redisRegister.lpush(
							Q_MB_REG_RQ.STRUCTURE_NAME
						  , RedisQueueDataWrapper.<Member>builder()
								.structure_name(Q_MB_REG_RQ.STRUCTURE_NAME)
								.data(willRegisterMemeber)
							.build()
						   );
	}

	/**
	 * 회원(개인, 법인) 소유자정보 수정 
	 * @param RegisterCorpMemberRequest
	 * @return
	 * @throws DataAccessException 
	 * @throws NoMemberRequestException
	 * @throws NotLgUplusUserException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws NotExistMemberTypeException 
	 */
	@Override
	@Transactional(readOnly = true)
	public void modify(RegisterMemberRequest memberRequest) throws DataAccessException, NoMemberRequestException, NotLgUplusUserException, NoSuchPaddingException, NoSuchAlgorithmException, IOException, NotExistMemberTypeException
	{
		if (null == memberRequest)
			throw new NoMemberRequestException();

		final String reqDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

		Member modifyMemeber = null;

		if (memberRequest instanceof RegisterPersonMemberRequest)
		{
			RegisterPersonMemberRequest personMemberRequest = (RegisterPersonMemberRequest) memberRequest;

			// 1. 공인전자주소(CEA) 조회
			Member member = memberMapper.findPersonMemberByCi(Encryptor.encryptCi(personMemberRequest.getCi()));

			if (null == member)
				throw new NotExistPersonMemberException();

			// 2. 회원 객체 만들기
			modifyMemeber = Member.builder()
											.type(MEMBER.TYPE.PERSON.val())
											.cea(member.getCea())
											.ci(personMemberRequest.getCi())
											.phone(personMemberRequest.getPhone())
											.birthday(personMemberRequest.getBirthday())
											.name(personMemberRequest.getName())
											.gender(personMemberRequest.getGender())
											.createdDtm(reqDtm)
											.reqType(Const.MEMBER.REQ_TYPE.MODIFY.val())
											.reqRoute(personMemberRequest.getReqRoute())
											.messageId(personMemberRequest.getMessageId())
											.regDtm(personMemberRequest.getRegDt())
											.agreeDtm(personMemberRequest.getApproveDt())
											.svcOrgCd(personMemberRequest.getSvcOrgCd())
								  .build();

			// 3. 수정 객체 없을시 에러
			if (null == modifyMemeber)
				throw new NoMemberRequestException();
		}
		else if (memberRequest instanceof RegisterCorpMemberRequest)
		{
			RegisterCorpMemberRequest corpMemberRequest = (RegisterCorpMemberRequest) memberRequest;

			// 1. 수정 요청 객체
			modifyMemeber = Member.builder()
											.type(MEMBER.TYPE.CORP.val())
											.cea(corpMemberRequest.getPart1().getOfap_elct_addr())
											.name(corpMemberRequest.getPart1().getBrno_nm())
											.createdDtm(reqDtm)
											.busiNum(corpMemberRequest.getPart1().getBrno())
											.svcOrgCd(corpMemberRequest.getPart1().getService_cd())
											.svcOrgName(corpMemberRequest.getPart1().getBiz_nm())
											.svcOrgType(corpMemberRequest.getPart1().getType())
											.reqType(Const.MEMBER.REQ_TYPE.MODIFY.val())
											.reqRoute(Const.MEMBER.ROUTE.BC.val())
											.file1(true == StringUtils.hasText(corpMemberRequest.getPart1().getBrno_nm())
													? new FileInfo(corpMemberRequest.getPart2())
													: null)	// 사업자 등록증
											.file2(true == StringUtils.hasText(corpMemberRequest.getPart1().getBrno_nm())
													? new FileInfo(corpMemberRequest.getPart3())
													: null)	// 공인전자주소 등록 신청서
											.regDtm(reqDtm)
								  .build();

			// 2. 수정 객체 없을시 에러
			if (null == modifyMemeber)
				throw new NoMemberRequestException();

			// 3. "사업자명" 변경 시 파일("사업자등록증","공인전자주소 등록신청서") 데이터 확인
			if (true == StringUtils.hasText(corpMemberRequest.getPart1().getBrno_nm())
				&& ((null == corpMemberRequest.getPart2() || true == corpMemberRequest.getPart2().isEmpty())
				 || (null == corpMemberRequest.getPart3() || true == corpMemberRequest.getPart3().isEmpty())
				   )
			   )
			{
				throw new MalformedException();
			}

			// 4. 공인전자주소 정보 확인(법인)
	        if (0 == memberMapper.existsSameCorpMemberBySvcOrgCd(modifyMemeber))
	        	throw new NotExistCorpMemberException();
		}
		else
		{
			throw new NotExistMemberTypeException();
		}

		// 4. 회원가입 레디스 요청큐에 넣어주기
        redisModify.lpush(
        				  Q_MB_MOD_RQ.STRUCTURE_NAME
        				, RedisQueueDataWrapper.<Member>builder()
        						.structure_name(Q_MB_MOD_RQ.STRUCTURE_NAME)
        						.data(modifyMemeber)
						  .build()
						 );
	}

	/**
	 * 회원(개인) 탈퇴 
	 * @param WithdrawMemberRequest
	 * @return
	 * @throws NoSuchAlgorithmException 
	 * @throws NoSuchPaddingException
	 * @throws DataAccessException
	 * @throws NotExistCorpMemberException
	 * @throws FailToWithdrawMemberException
	 * @throws NotLgUplusUserException
	 */
	@Override
	@Transactional(readOnly = true)
	public void withdraw(WithdrawMemberRequest memberRequest) throws NoSuchAlgorithmException, NoSuchPaddingException, DataAccessException, NotExistCorpMemberException, FailToWithdrawMemberException, NotLgUplusUserException
	{
		if (memberRequest instanceof WithdrawPersonMemberRequest)
		{
			WithdrawPersonMemberRequest personMemberRequest = (WithdrawPersonMemberRequest) memberRequest;

			// 1. 회원 조회
			Member member = memberMapper.findPersonMemberByCi(Encryptor.encryptCi(personMemberRequest.getCi()));

			// 2. 회원정보를 찾을 수 없을 경우, 에러발생
			if (null == member)
				throw new NotExistPersonMemberException();

			// 3. 암호화되지 않은 ci
			member.setCi(personMemberRequest.getCi());

			// 4. 회원 유형("0":개인, "1":법인)
			member.setType(Const.MEMBER.TYPE.PERSON.val());

			// 5. 암호화되지 않은 phone
			member.setPhone(personMemberRequest.getPhone());

			// 6. 생년월일
			member.setBirthday(personMemberRequest.getBirthday());

			// 7. 성별
			member.setGender(personMemberRequest.getGender());

			// 8. 탈퇴 요청 시간
			member.setCreatedDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

			// 9. 요청 유형 설정("0":탈퇴, "1":가입, "2":수정)
			member.setReqType(Const.MEMBER.REQ_TYPE.WITHDRAW.val());

			// 10. 탈퇴 요청 경로("W":웹, "M":메시지)
			member.setReqRoute(personMemberRequest.getReqRoute());

			// 11. 신청일시(수신동의 등록/해제 발생일시)
			member.setAgreeDtm(personMemberRequest.getApproveDt());

			// 12. 탈퇴요청 레디스 요청큐에 넣어주기
			redisWithdraw.lpush(
								Q_MB_DEL_RQ.STRUCTURE_NAME
							  , RedisQueueDataWrapper.<Member>builder()
									.structure_name(Q_MB_DEL_RQ.STRUCTURE_NAME)
									.data(member)
								.build()
							   );
		}
		else
		{
			throw new FailToWithdrawMemberException();
		}
	}

	/**
	 * 수신동의 요청 
	 * @param AgreeRequest
	 * @return
	 */
	@Override
	public void addAgree(AgreeRequest agreeRequest)
	{
		// 1. 수신동의 요청 레디스 요청큐 삽입
		redisAgree.lpush(
						 Q_REG_AGREE_RQ.STRUCTURE_NAME
					   , RedisQueueDataWrapper.<Agree>builder()
					   		.structure_name(Q_REG_AGREE_RQ.STRUCTURE_NAME)
							.data(Agree.builder()
												 .gubun(Const.MEMBER.AGREE_TYPE.AGREE_REGISTER)
												 .phone(agreeRequest.getPhone())
												 .ci(agreeRequest.getCi())
												 .svcOrgCd(agreeRequest.getSvcOrgCd())
												 .messageId(agreeRequest.getMessageId())
												 .agreeDtm(agreeRequest.getApproveDt())
									   .build()
								 )
						 .build()
						);
	}

	/**
	 * 수신동의 해제 요청 
	 * @param AgreeRequest
	 * @return
	 */
	@Override
	public void cancelAgree(AgreeRequest agreeRequest)
	{
		// 1. 수신동의 해제 요청 레디스 요청큐 삽입
		redisAgree.lpush(
						 Q_DEL_AGREE_RQ.STRUCTURE_NAME
					   , RedisQueueDataWrapper.<Agree>builder()
					   		.structure_name(Q_DEL_AGREE_RQ.STRUCTURE_NAME)
							.data(Agree.builder()
												 .gubun(Const.MEMBER.AGREE_TYPE.AGREE_WITHDRAW)
												 .phone(agreeRequest.getPhone())
												 .ci(agreeRequest.getCi())
												 .svcOrgCd(agreeRequest.getSvcOrgCd())
												 .messageId(agreeRequest.getMessageId())
												 .agreeDtm(agreeRequest.getApproveDt())
									   .build()
								 )
						 .build()
						);
	}

	/**
	 * 수신거부 목록 추가 
	 * @param RejectionRequest
	 * @return
	 * @throws NoSuchAlgorithmException 
	 * @throws DataAccessException
	 */
	@Override
	@Transactional(readOnly = true)
	public void addRejections(RejectionRequest rejectionRequest) throws NoSuchAlgorithmException, DataAccessException
	{
		Rejection rejection = Rejection.builder()
								                 .phone(rejectionRequest.getPhone())
								                 .ci(rejectionRequest.getCi())
								                 .messageId(rejectionRequest.getMessageId())
								                 .apctDtm(rejectionRequest.getApctDtm())
								                 .rejections(rejectionRequest.getRejections())
								       .build();

		// 1. 만약 등록되지 않은 법인 공인전자주소가 수신거부목록에 있을 경우 END, 에러발생
		if (rejection.getRejections().size() != memberMapper.getCorpMembersCountBySvcOrgCd(rejection.getRejections()))
			throw new NotExistRejectionsException();

		// 2. 레디스 수신거부 요청 목록 저장(Q_REG_REJECTION_RQ:수신거부 요청 큐)
		redisAddRejection.lpush(
								Q_REG_REJECTION_RQ.STRUCTURE_NAME
							  , RedisQueueDataWrapper.<Rejection>builder()
									.structure_name(Q_REG_REJECTION_RQ.STRUCTURE_NAME)
									.data(rejection)
								.build()
							   );
	}

	/**
	 * 수신거부 목록 삭제 
	 * @param RejectionRequest
	 * @return
	 * @throws DataAccessException 
	 * @throws NoSuchAlgorithmException
	 * @throws FailToCancelRejectionsException
	 */
	@Override
	@Transactional(readOnly = true)
	public void cancelRejections(RejectionRequest rejectionRequest) throws DataAccessException, NoSuchAlgorithmException, FailToCancelRejectionsException
	{
		Rejection rejection = Rejection.builder()
								                 .phone(rejectionRequest.getPhone())
								                 .ci(rejectionRequest.getCi())
								                 .messageId(rejectionRequest.getMessageId())
								                 .apctDtm(rejectionRequest.getApctDtm())
								                 .rejections(rejectionRequest.getRejections())
								       .build();

		// 1. <FIXME:만약 등록되지 않은 법인 공인전자주소가 수신거부목록에 있을 경우 에러발생>
		if (rejectionRequest.getRejections().size() != memberMapper.getCorpMembersCountBySvcOrgCd(rejectionRequest.getRejections())) 
			throw new NotExistRejectionsException();

		// 2. 레디스 큐 저장
		redisRemoveRejection.lpush(
								   Q_DEL_REJECTION_RQ.STRUCTURE_NAME
								 , RedisQueueDataWrapper.<Rejection>builder()
										.structure_name(Q_DEL_REJECTION_RQ.STRUCTURE_NAME)
										.data(rejection)
								   .build()
								  );
	}

	/**
	 * 화이트 리스트 등록 
	 * @param WhitelistRequest
	 * @return
	 * @throws NoSuchAlgorithmException 
	 * @throws DataAccessException
	 */	
	@Override
	@Transactional(readOnly = true)
	public void addWhiteList(WhitelistRequest whitelistRequest) throws NoSuchAlgorithmException, DataAccessException
	{
		Whitelist whitelist =  Whitelist.builder()
												  .ci(whitelistRequest.getCi())
												  .whitelists(whitelistRequest.getWhitelists())
												  .carrier(whitelistRequest.getCarrier())
												  .apctAcctCls(Const.MEMBER.APCT_ACCT_CLS.REGISTER.val())
												  .phone(whitelistRequest.getPhone())
												  .inCls(whitelistRequest.getInCls())
												  .messageId(whitelistRequest.getMessageId())
										.build();

		// 1. 만약 등록되지 않은 법인 공인전자주소가 수신거부목록에 있을 경우 END, 에러발생
		if (whitelist.getWhitelists().size() != memberMapper.getCorpMembersCountBySvcOrgCd(whitelist.getWhitelists())) 
			throw new NotExistRejectionsException();

		// 2. 레디스에 화이트 리스트 등록 요청 목록 저장(Q_REG_WHITE_LIST_RQ:화이트리스트 등록 요청 큐)
		redisAddWhitelist.lpush(
								Q_REG_WHITE_LIST_RQ.STRUCTURE_NAME
							  , RedisQueueDataWrapper.<Whitelist>builder()
									.structure_name(Q_REG_WHITE_LIST_RQ.STRUCTURE_NAME)
									.data(whitelist)
								.build()
							   );
	}

	/**
	 * 화이트 리스트 등록 취소 
	 * @param WhitelistRequest
	 * @return
	 * @throws NoSuchAlgorithmException 
	 * @throws DataAccessException 
	 */	
	@Override
	@Transactional(readOnly = true)
	public void cancelWhiteList(WhitelistRequest whitelistRequest) throws NoSuchAlgorithmException, DataAccessException
	{
		Whitelist whitelist = Whitelist.builder()
												 .ci(whitelistRequest.getCi())
												 .whitelists(whitelistRequest.getWhitelists())
												 .carrier(whitelistRequest.getCarrier())
												 .apctAcctCls(Const.MEMBER.APCT_ACCT_CLS.WITHDRAW.val())
												 .phone(whitelistRequest.getPhone())
												 .inCls(whitelistRequest.getInCls())
												 .messageId(whitelistRequest.getMessageId())
									   .build();

		// 1. 만약 등록되지 않은 법인 공인전자주소가 수신거부목록에 있을 경우 END, 에러발생
		if (whitelist.getWhitelists().size() != memberMapper.getCorpMembersCountBySvcOrgCd(whitelist.getWhitelists())) 
			throw new NotExistRejectionsException();

		// 2. 레디스에 화이트 리스트 등록 취소 요청 목록 저장(Q_REG_WHITE_LIST_RQ:화이트리스트 등록 취소 요청 큐)
		redisCanceleWhitelist.lpush(
									Q_DEL_WHITE_LIST_RQ.STRUCTURE_NAME
								  , RedisQueueDataWrapper.<Whitelist>builder()
										.structure_name(Q_DEL_WHITE_LIST_RQ.STRUCTURE_NAME)
										.data(whitelist)
									.build()
								   );
	}	
}
