package com.example.mp.gw.member.domain;


import javax.validation.constraints.NotNull;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * @Class Name : RegisterCorpMemberRequest.java
 * @Description : 법인(기관) 회원 가입/수정 요청 (From. 비즈센터) 
 * 
 * @author 조주현
 * @since 2021.04.04
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.04	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"part2", "part3"})
@ApiModel(value = "법인(기관) 회원 가입/수정 요청", description = "법인(기관) 회원 가입/수정 요청 (From. 비즈센터)")
public class RegisterCorpMemberRequest extends RegisterMemberRequest
{
	@NotNull(message = "[기관정보] 값은 필수입니다.")
	@ApiModelProperty(value = "기관정보", notes = "기관정보", required = true)
	private RegisterCorpMemberRequestPart1 part1;

	@NotNull(message = "[사업자등록증] 값은 필수입니다.")
	@ApiModelProperty(value = "사업자 등록증", notes = "사업자 등록증 바이너리", required = true)
	private MultipartFile part2;

	@NotNull(message = "[공인전자주소 등록 신청서] 값은 필수입니다.")
	@ApiModelProperty(value = "공인전자주소 등록 신청서", notes = "공인전자주소 등록 신청서 바이너리", required = true)
	private MultipartFile part3;
}
