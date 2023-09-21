package com.example.mp.gw.kisa.domain;


import java.util.List;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : SearchEaddrWithdrawHistResponse.java
 * @Description : 공인전자주소 탈퇴이력 조회 응답 (From. KISA)
 * 
 * @author 조주현
 * @since 2022.04.04
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.04.04	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "공인전자주소 탈퇴이력 조회", description = "공인전자주소 탈퇴이력 조회 응답 (From. KISA)")
public class SearchEaddrWithdrawHistResponse
{
	@NotNull(message = "[처리결과 (1:성공, 0:실패)] 값은 필수입니다.")
	@ApiModelProperty(value="처리결과 (1:성공, 0:실패)")
	private Integer resultCode;

	@ApiModelProperty(value="이용자가 등록한 공인전자주소")
	private List<String> eaddrs;

	@ApiModelProperty(value="오류코드")
	private String errCode;

	@ApiModelProperty(value="오류메시지")
	private String errMsg;
}
