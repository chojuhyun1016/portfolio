package com.example.mp.gw.member.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : Agree.java
 * @Description : 수신동의 요청
 * 
 * @author 조주현
 * @since 2022.05.11
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.05.11	    조주현          최초 생성
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
public class Agree
{
	// 신청구분(등록:"1", 해제:"0")
	private String gubun;

	// 개인휴대전화번호
	private String phone;

	// 개인식별코드
	private String ci;

	// 서비스코드(기관코드)
	private String svcOrgCd;

	// 메시지ID(발송요청관리번호+발송요청일련번호)
	private String messageId;

	// 수신동의 시간
	private String agreeDtm;

	// 저장 시간
	private String regDtm;
}
