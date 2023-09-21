package com.example.mp.gw.member.domain;


import java.util.List;

import com.example.mp.gw.common.utils.enc.Encryptor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : Rejection.java
 * @Description : 수신거부 등록
 * 
 * @author 조주현
 * @since 2022.01.02
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.01.02	    조주현           최초 생성
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
public class Rejection
{
	// 전화전호
	private String phone;

	// 개인식별코드
	private String ci;

	// 메시지 일련번호
	private String messageId;

	// 수신거부(신청/해지) 일시
	private String apctDtm;

	// 법인 목록
	private List<String> rejections;


	@JsonIgnore
	public String getEncPhone()
	{
		try
		{
			return Encryptor.encryptPhone(phone);
		}
		catch(Exception e)
		{
			return phone;
		}
	}
}
