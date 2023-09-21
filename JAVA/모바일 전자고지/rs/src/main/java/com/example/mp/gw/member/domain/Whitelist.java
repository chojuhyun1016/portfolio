package com.example.mp.gw.member.domain;


import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : Whitelist.java
 * @Description : 다회선 요청 
 * 
 * @author 조주현
 * @since 2022.03.16
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.03.16	    조주현           최초 생성
 * 
 *  </pre>
 * 
 */


@Setter
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Whitelist
{
	// 개인식별코드
	private String ci;

	// 법인 목록
	private List<String> whitelists;

	// 모바일 사업자 구분
	private String carrier;

	// 신청구분
	private String apctAcctCls;

	// 전화번호
	private String phone;

	// 유입구분
	private String inCls;

	// 메시지 일련번호
	private String messageId;
}
