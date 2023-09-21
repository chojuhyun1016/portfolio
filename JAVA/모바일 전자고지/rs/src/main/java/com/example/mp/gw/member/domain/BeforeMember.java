package com.example.mp.gw.member.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : BeforehandMessageMember.java
 * @Description : 자동가입 대기 유저 정보
 * 
 * @author 조주현
 * @since 2021.04.06
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.06	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BeforeMember
{
	// 요청 시간
	private String createdDtm;

	// 메시지ID(발송요청관리번호_발송요청일련번호)
	private String messageId;

	// 서비스코드
	private String svcOrgCd;

	// 개인식별코드
	private String ci;

	// 개인휴대전화번호
	private String phone;

	// 등록시간
	private String regDtm;
}
