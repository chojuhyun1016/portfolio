package com.example.mp.gw.kisa.domain;


import java.util.List;

import javax.validation.constraints.NotEmpty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : RegisterDocCirculations.java
 * @Description : 전자문서 유통정보 [등록/열람일시 등록] 요청 (To. KISA)
 * 
 * @author 조주현
 * @since 2021.08.18
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.08.18	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "전자문서 유통정보 [등록/열람일시 등록]", description = "전자문서 유통정보 [등록/열람일시 등록] 요청 (To. KISA)")
public class RegisterDocumentCirculationsRequest
{
	@NotEmpty(message = "[유통정보 JSON array] 값은 필수입니다.")
	@ApiModelProperty(value="유통정보 JSON array", required = false)
	List<RegisterDocumentCirculationRequest> circulations;
}
