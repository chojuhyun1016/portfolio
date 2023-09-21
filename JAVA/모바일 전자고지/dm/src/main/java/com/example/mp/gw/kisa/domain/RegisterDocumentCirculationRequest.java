package com.example.mp.gw.kisa.domain;


import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : RegisterDocCirculations.java
 * @Description : 전자문서 유통정보 [등록/열람일시 등록] 요청 객체
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
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "전자문서 유통정보 [등록/열람일시 등록]", description = "전자문서 유통정보 [등록/열람일시 등록] 요청 객체")
public class RegisterDocumentCirculationRequest
{

	@NotBlank(message = "[전자문서번호] 값은 필수입니다.")
	@ApiModelProperty(value="전자문서번호", required = true)
	private String edocNum;

	@NotBlank(message = "[전자문서제목] 값은 필수입니다.")
	@ApiModelProperty(value="전자문서제목", required = true)
	private String subject;

	@NotBlank(message = "[송신자 공인전자주소] 값은 필수입니다.")
	@ApiModelProperty(value="송신자 공인전자주소", required = true)
	private String sendEaddr;

	@ApiModelProperty(value="송신자 공인전자주소의 하위 주소", required = true)
	private String sendSubEaddr;

	@NotBlank(message = "[수신자 공인전자주소] 값은 필수입니다.")
	@ApiModelProperty(value="수신자 공인전자주소", required = true)
	private String recvEaddr;

	@ApiModelProperty(value="수신자 공인전자주소의 하위 주소", required = true)
	private String recvSubEaddr;

	@NotBlank(message = "[송신 중계자플랫폼ID] 값은 필수입니다.")
	@ApiModelProperty(value="송신 중계자플랫폼ID", required = true)
	private String sendPlatformId;

	@NotBlank(message = "[수신 중계자플랫폼ID] 값은 필수입니다.")
	@ApiModelProperty(value="수신 중계자플랫폼ID", required = true)
	private String recvPlatformId;

	@NotBlank(message = "[송신일시] 값은 필수입니다.")
	@ApiModelProperty(value="송신일시", required = true)
	private String sendDate;

	@NotBlank(message = "[수신일시] 값은 필수입니다.")
	@ApiModelProperty(value="수신일시", required = false)
	private String recvDate;

	@ApiModelProperty(value="열람일시", required = false)
	private String readDate;

	@NotNull(message = "[전자문서 유형] 값은 필수입니다.")
	@ApiModelProperty(value="전자문서 유형", required = true)
	private Integer docType;

	@NotBlank(message = "[본문해시값] 값은 필수입니다.")
	@ApiModelProperty(value="본문해시값", required = true)
	private String contentHash;

	@ApiModelProperty(value="첨부파일 해시값 리스트", example = "fileHashes : [“hash1”, “hash2”, ..]", required = false)
	private List<String> fileHashes;

	@JsonIgnore
	@ApiModelProperty(value="발송요청관리번호_방송요청일련번호", required = true)
	private String messageId;
}
