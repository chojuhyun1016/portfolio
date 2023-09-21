package com.example.mp.gw.backup.domain;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : MpDcmntInfoDto.java
 * @Description : 유통정보(MP_DCMNT_INFO) 테이블 DTO
 * 
 * @author 조주현
 * @since 2023.08.07
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2023.08.07	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Data
@Builder
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MpDcmntInfoDto
{
	// 유통정보 아이디 유통정보 SEQ
    private Integer dcmntInfoId;

    // 메시지아이디 
    private String messageId;

    // 전자문서번호 전자문서번호
    private String elctrcDcmntNo;

    // 전자문서제목 전자문서번호 전자문서제목
    private String elctrcDcmntSbjct;

    // 송신공인전자주소 송신자공인전자주소
    private String sndCea;

    // 수신공인전자주소 수신자공인전자주소
    private String rsvCea;

    // 송신일시 송신일시
    private String sndDtm;

    // 수신일시 수신일시
    private String rsvDtm;

    // 열람일시 열람일시
    private String rdDtm;

    // 본문해쉬값 내용해쉬값
    private String cntntHash;

    // 첨부파일해쉬값 파일해쉬값
    private String fileHash;

    // 송신자 명 
    private String sndName;

    // 수신자 명 
    private String rsvName;

    // 송신자 플랫폼  ID
    private String sndPlfmId;

    // 수신자 플랫폼 ID
    private String rsvPlfmId;   

    // 문서상태(M:생성, R:등록, U:업데이트)
    private String dcmntStat;
    
    // 유통정보 등록 결과(Y:성공, N:실패)
    private String dcmntRslt;
    
    // 유통정보 등록 결과 코드
    private String dcmntRsltCd;

    // 유통정보 등록 결과 등록 시간(YYYYMMDDhhmmss)
    private String dcmntRsltDtm;

    // 생성시간
    private String regDtm;

    // 파티션 키(메시지 수신 월(MM))
    private String partMm;

    // 테스트 발송여부
    private String testSndnYn;

    // 전자문서 유형("0":일반문서, "1":알림서비스, "2":알림서비스(열람일시 미전송)
    private Integer docType;
}
