package com.example.mp.gw.backup.domain;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : PmMsgDto.java
 * @Description : 메시지(PM_MSG) 테이블 DTO
 *
 * @author 조주현
 * @since 2023.08.07
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일         수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2023.08.07     조주현          최초 생성
 *
 *  </pre>
 *
 */


@Setter
@Getter
@Builder
@ToString
public class PmMsgDto
{
    // 메시지키
    private String msgkey;

    // CI
    private String ci;

    // 기관코드
    private String svcOrgCd;

    // 기관이름
    private String svcOrgNm;

    // 메시지ID 수신키
    private String messageId;

    // 
    private String transDt;

    // 수신번호
    private String phone;

    // 발송번호
    private String callback;

    // 메시지 진행상태
    private String sendStat;

    // 메시지타입
    private String msgType;

    // 메시지
    private String msg;

    // MMS 이미지 크기
    private Long mmsImgSize;

    //
    private String smrtpYn;

    // 등록일시
    private String regDt;

    // NCAS 연동 시간
    private String ncasDt;

    // 발신 코드
    private String gwSndRsltCd;

    // 발신 설명
    private String gwSndRsltDesc;

    // 발신 시간
    private String gwSndRsltDt;

    // 수신 코드
    private String gwRptRsltCd;

    // 수신 설명
    private String gwRptRsltDesc;

    // 수신 시간
    private String gwRptRsltDt;

    // BC 발송 결과 코드
    private String bcSndRsltCd;

    // BC 수신 결과 코드
    private String bcRptRsltCd;

    // 종료 시간
    private String endDt;

    //
    private String cnForm;    

    // 메시지 제목
    private String msgTitle;

    // 메시지 발송구분
    private Long optType;

    // RCS 브랜드 ID
    private String rcsBrId;

    //
    private String rcsFallbackType;

    //
    private String rcsFallbackSndYn;

    //
    private String rcsFallbackRslt;

    //
    private String rcsFallbackSndDt;

    //
    private String rcsFallbackRptDt;

    //
    private String cnKey;

    //
    private String redirectUrl;

    //
    private String tknRpmtYn;

    //
    private String rdngRpmtYn;

    //
    private Long dcmntInfoId;

    //
    private String expireDt;

    //
    private String rcsAgencyId;

    //
    private String rcsType;

    //
    private String sndPlfmId;

    //
    private String sndNpost;

    //
    private String sndDate;

    //
    private String rcsAttachId;

    // 파티션
    private String partMm;

    // 문서해시
    private String docHash;

    // 테스트 발송여부
    private String testSndnYn;

    // 다회선사용자 발송구분
    private String multiMblPrcType;

    // RCS 메시지
    private String rcsMsg;

    // 클릭 일시
    private String clickDt;

    // 유통정보 미생성여부
    private String distInfoCrtYn;

    // 안내문 확인하기 치환문구
    private String InfoCfrmStr;

    // 수신거부 및 수신 휴대폰 지정하기치환문구
    private String rcveRfStr;

    // 재열람 일수
    private String reopenDay;

    // 전자문서 유형
    private String kisaDocType;

    // BC 리포트 전송 코드
    private Long bcRptSuccCd;

    // MMS 개인휴대전화번호
    private String mdn;
}
