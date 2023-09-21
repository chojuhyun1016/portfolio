package com.example.mp.gw.ms.domain;


import lombok.Getter;
import lombok.Setter;

/**
 * @Class Name : PmMsg.java
 * @Description : PM_MSG 객체 
 * 
 * @author 조주현
 * @since 2021.04.12
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.12	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Setter
@Getter
public class PmMsg
{
    // 메시지키 
    private String msgkey;

    // 메시지ID 수신키
    private String messageId;

    // 기관코드 
    private String svcOrgCd;

    // 기관명 수요기관
    private String svcOrgNm;

    // 요청일자 요청일자(YYYYMMDD
    private String transDt;

    // 수신번호 
    private String phone;

    // 회신번호 
    private String callback;

    // 메시지 진행상태 00
    private String sendStat;

    // GW 전송결과코드 2차 개발 변경
    private String gwRsltCd;

    // GW 전송결과 설명 2차 개발 변경
    private String gwRsltDesc;

    // 이통사 결과코드 2차 개발 변경
    private String telRsltCd;

    // 이통사 결과설명 2차 개발 변경
    private String telRsltDesc;

    // KT 전송결과코드 
    private String rsltCd;

    // KT 전송결과 설명 
    private String rsltDesc;

    // 메시지타입 S
    private String msgType;

    // URL 포함여부 위
    private Long msgInfo;

    // 메시지 사이즈 
    private Long msgSize;

    // 메시지 내용 2차 개발 변경
    private String msg;

    // MMS 이미지 사이즈 
    private Long mmsImgSize;

    // MMS 이미지 경로 
    private String mmsImgPath;

    // MMS 이미지 이름 
    private String mmsImgNm;

    // 스마트폰 여부 
    private String smrtpYn;

    // 등록일시 
    private String regDt;

    // NCAS 요청 일시 
    private String ncasDt;

    // 폰정보조회 일시 
    private String piDt;

    // 이통사 발송 일시 
    private String gwDt;

    // 리포트 전달 일시 
    private String rptDt;

    // 최종완료 일시 
    private String doneDt;

    // MMS 본문 저장경로 
    private String mmsMsgPath;

    // 문서명 
    private String cnForm;

    // 고지발송 제목 
    private String msgTitle;

    // 고지발송구분 
    private Long optType;

    // 수신거부리스트 
    private String refuseList;

    // 동의리스트 
    private String whiteList;

    // 다회선사용자 발송구분 
    private String multiMblPrcType;

    // 테스트 발송여부 
    private String testSndnYn;

    // Biz Center RCS 대행사 아이디 2차 개발 추가
    private String rcsAgencyId;

    // RCS 메시지베이스 아이디 2차 개발 추가
    private String rcsMessagebaseId;

    // RCS 브랜드 아이디 2차 개발 추가
    private String rcsBrId;

    // RCS 상품코드 2차 개발 추가
    private String rcsProductCode;

    // RCS fallack 여부 (Y,N) 2차 개발 추가
    private String rcsFallbackYn;

    // RCS fallback 메시지 2차 개발 추가
    private String rcsFallbackMsg;

    // RCS fallback 유형 (SMS, LMS, MMS) 2차 개발 추가
    private String rcsFallbackType;

    // RCS fallback 제목 2차 개발 추가
    private String rcsFallbackTitle;

    // RCS fallback 발송여부 (Y,N) 2차 개발 추가
    private String rcsFallbackSndYn;

    // RCS fallback 발송결과 2차 개발 추가
    private String rcsFallbackRslt;

    // RCS fallback 발송일시 2차 개발 추가
    private String rcsFallbackSndDt;

    // RCS fallback 리포트 수신일시 2차 개발 추가
    private String rcsFallbackRptDt;
    
    // 유통정보 아이디 유통정보 SEQ
    private Long dcmntInfoId;
}
