package com.example.mp.gw.member.domain;


import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : CheckLguplusMemberRequest.java
 * @Description : 수신자 이동통신 가입여부조회 요청 (From. 비즈센터)
 * 
 * @author 조주현
 * @since 2022.03.17
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.03.17	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@ApiModel(value = "수신자 이동통신 가입여부조회", description = "수신자 이동통신 가입여부조회 요청 (From. 비즈센터)")
public class CheckLguplusMemberRequest
{
	@Valid
	@NotEmpty(message = "[배열] 값은 필수입니다.")
	@ApiModelProperty(value = "배열", example = "[{\"ci\":\"test-ci\"}]", required = true)
	private List<CheckLguplusMember> reqs;
}
