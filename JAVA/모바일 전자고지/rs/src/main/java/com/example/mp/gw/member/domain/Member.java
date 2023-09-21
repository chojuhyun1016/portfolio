package com.example.mp.gw.member.domain;


import com.example.mp.gw.common.domain.FileInfo;
import com.example.mp.gw.common.utils.enc.Encryptor;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * @Class Name : Member.java
 * @Description : 회원(가입, 탈퇴, 수정) 객체
 * 
 * @author 조주현
 * @since 2022.02.23
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.02.23	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
public class Member
{
	// 맴버 아이디(내부)
	private Integer mbId;

	// 고유아이디 (공인전자주소) 
	private String cea;

	// CI 
	private String ci;

	// 암호화 CI 
	@SuppressWarnings("unused")
	private String encCi;

	// 개인휴대전화번호
	private String phone;

	// 암호화 개인휴대전화번호 
	@SuppressWarnings("unused")
	private String encPhone;

	// 생년월일 (900101)
	private String birthday;

	// 이름
	private String name;

	// "1":남성 (1900-1999년생),"1":여성 (1900-1999년생), "3":남성 (2000년생부터),"4":여성 (2000년생부터),"5":외국남성 (1900-1999년생),"6":외국여성 (1900-1999년생), "7":외국남성 (2000년생부터),"8":외국여성 (2000년생부터)
	private String gender;

	// 요청(가입/탈퇴/소유자 수정) 시간(yyyymmddHHmmss)
	private String createdDtm;

	// 회원유형(0:개인, 1:법인)
	private String type;

	// 사업자등록 번호
	private String busiNum;

	// 사업자등록 이름(법인명)
	private String busiName;
	
	// 기관 코드 
	private String svcOrgCd;

	// 기관 이름
	private String svcOrgName;

	// 기관 구분 코드 
	private String svcOrgType;

	// 요청유형("0":탈퇴, "1":가입, "2":수정)
	private String reqType;

	// 가입경로(W:웹, M:문자, B:비즈센터, A:자동가입)
	private String reqRoute;

	// 공인전자주소 등록 신청서
	private FileInfo file1;

	// 사업자등록증
	private FileInfo file2;

	// 메시지ID
	private String messageId;

	// 메시지 수신 시간
	private String regDtm;

	// 동의 일시
	private String agreeDtm;

	// 수신동의상태 전송 여부(Y:전송, N:미전송)
	private String agreeYn;


	public String getEncCi()
	{
		try
		{
			return Encryptor.encryptCi(ci);
		}
		catch (Exception e)
		{
			return ci;
		}
	}

	public String getEncPhone()
	{
		try
		{
			return Encryptor.encryptPhone(phone);
		}
		catch (Exception e)
		{
			return phone;
		}
	}

	/**
	 * @param mbId
	 * @param cea
	 * @param ci
	 * @param encCi
	 * @param phone
	 * @param encPhone 
	 * @param birthday
	 * @param name
	 * @param gender
	 * @param createdDtm
	 * @param type
	 * @param busiNum
	 * @param svcOrgCd 
	 * @param svcOrgName
	 * @param svcOrgType
	 * @param reqType
	 * @param reqRoute
	 * @param file1
	 * @param file2
	 * @param messageId
	 * @param regDtm
	 * @param agreeDtm
	 * @param agreeYn
	 */
	public Member(Integer mbId, String cea, String ci, String encCi, String phone, String encPhone, String birthday, String name, String gender
				, String createdDtm, String type, String busiNum, String busiName, String svcOrgCd, String svcOrgName, String svcOrgType
				, String reqType, String reqRoute, FileInfo file1, FileInfo file2, String messageId, String regDtm, String agreeDtm, String agreeYn 
				 )
	{
		this.mbId          = mbId;
		this.cea           = cea;
		this.ci            = ci;
		this.phone         = phone;
		this.birthday      = birthday;
		this.name          = name;
		this.gender        = gender;
		this.createdDtm    = createdDtm;
		this.type          = type;
		this.busiNum       = busiNum;
		this.busiName      = busiName;
		this.svcOrgCd      = svcOrgCd;
		this.svcOrgName    = svcOrgName;
		this.svcOrgType    = svcOrgType;
		this.reqType       = reqType;
		this.reqRoute      = reqRoute;
		this.file1         = file1;
		this.file2         = file2;
		this.messageId     = messageId;
		this.regDtm        = regDtm;
		this.agreeDtm      = agreeDtm;
		this.agreeYn       = agreeYn;

		try
		{
			this.encCi = Encryptor.encryptCi(ci);
		}
		catch (Exception e)
		{
			this.encCi = ci;
		}

		try
		{
			this.encPhone = Encryptor.encryptPhone(phone);
		}
		catch (Exception e)
		{
			this.encPhone = phone;
		}
	}
}
