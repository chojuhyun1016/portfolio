package com.example.mp.gw.common.utils;


public class ResultCode {

    private String code  = "";
    private String relayCode  = "";
    private String relayDesc  = "";

public ResultCode(String code, String relayCode, String relayDesc){
    this.code = code;
    this.relayCode = relayCode;
    this.relayDesc =relayDesc;
}
    public String code() {
        return code;
    }

    public String relayCode() {
        return relayCode;
    }

    public String relayDesc() {
        return relayDesc;
    }

	public static ResultCode  findcode(String mapFld) {
		ResultCode f_code = R_SMS_20;
		switch (mapFld){
			case "R_SMS_DLV_6": f_code = R_SMS_DLV_6;
				break;
			case"R_SMS_RPT_6": f_code = R_SMS_RPT_6;
				break;
			case"R_SMS_1": f_code = R_SMS_1;
				break;
			case"R_SMS_2": f_code = R_SMS_2;
				break;
			case"R_SMS_3": f_code = R_SMS_3;
				break;
			case"R_SMS_4": f_code = R_SMS_4;
				break;
			case"R_SMS_5": f_code = R_SMS_5;
				break;
			case"R_SMS_7": f_code = R_SMS_7;
				break;
			case"R_SMS_8": f_code = R_SMS_8;
				break;
			case"R_SMS_9": f_code = R_SMS_9;
				break;
			case"R_SMS_10": f_code = R_SMS_10;
				break;
			case"R_SMS_11": f_code = R_SMS_11;
				break;
			case"R_SMS_14": f_code = R_SMS_14;
				break;
			case"R_SMS_17": f_code = R_SMS_17;
				break;
			case"R_SMS_18": f_code = R_SMS_18;
				break;
			case"R_SMS_19": f_code = R_SMS_19;
				break;
			case"R_SMS_21": f_code = R_SMS_21;
				break;
			case"R_SMS_22": f_code = R_SMS_22;
				break;
			case"R_SMS_23": f_code = R_SMS_23;
				break;
			case"R_SMS_24": f_code = R_SMS_24;
				break;
			case"R_SMS_25": f_code = R_SMS_25;
				break;
			case"R_SMS_26": f_code = R_SMS_26;
				break;
			case"R_SMS_27": f_code = R_SMS_27;
				break;
			case"R_SMS_28": f_code = R_SMS_28;
				break;
			case"R_SMS_29": f_code = R_SMS_29;
				break;
			case"R_SMS_30": f_code = R_SMS_30;
				break;
			case"R_SMS_31": f_code = R_SMS_31;
				break;
			case"R_SMS_40": f_code = R_SMS_40;
				break;
			case"R_SMS_97": f_code = R_SMS_97;
				break;
			case"R_SMS_98": f_code = R_SMS_98;
				break;
			case"R_SMS_99": f_code = R_SMS_99;
				break;
			default:
				f_code = R_SMS_20;
				break;
		}
		return f_code;
	}
    public static final ResultCode R_SMS_DLV_6					= new ResultCode("1000", "6",  "성공");
	public static final ResultCode R_SMS_RPT_6					= new ResultCode("1000", "6",  "성공");
    public static final ResultCode R_SMS_1					= new ResultCode("5001", "1",  "시스템 장애");
	public static final ResultCode R_SMS_2					= new ResultCode("5002", "2",  "인증실패, 직후 연결을 끊음");
	public static final ResultCode R_SMS_3					= new ResultCode("5003", "3",  "포맷 에러");
	public static final ResultCode R_SMS_4					= new ResultCode("5004", "4",  "BIND 안됨");
	public static final ResultCode R_SMS_5					= new ResultCode("5005", "5",  "착신가입자 없음(미등록) (현재 사용안함)");
	public static final ResultCode R_SMS_7					= new ResultCode("5006", "7",  "비가입자,결번,서비스정지");
	public static final ResultCode R_SMS_8					= new ResultCode("5007", "8",  "단말기 Power-off 상태");
	public static final ResultCode R_SMS_9					= new ResultCode("5008", "9",  "음영지역");
	public static final ResultCode R_SMS_10					= new ResultCode("5009", "10", "단말기 메시지 저장개수 초과");
	public static final ResultCode R_SMS_11					= new ResultCode("5010", "11", "전송시간 초과");
	public static final ResultCode R_SMS_14					= new ResultCode("5011", "14", "통신사 내부 실패(무선망단)");
	public static final ResultCode R_SMS_17					= new ResultCode("5012", "17", "기타에러");
	public static final ResultCode R_SMS_18					= new ResultCode("5013", "18", "중복된키접수차단");
	public static final ResultCode R_SMS_19					= new ResultCode("5014", "19", "월 송신 건수 초과");
    public static final ResultCode R_SMS_20					= new ResultCode("5015", "20", "기타에러");
	public static final ResultCode R_SMS_21					= new ResultCode("5016", "21", "착신번호 에러(자리수에러)");
	public static final ResultCode R_SMS_22					= new ResultCode("5017", "22", "착신번호 에러(없는 국번)");
	public static final ResultCode R_SMS_23					= new ResultCode("5018", "23", "수신거부 메시지 없음");
	public static final ResultCode R_SMS_24					= new ResultCode("5019", "24", "21시 이후 광고");
	public static final ResultCode R_SMS_25					= new ResultCode("5020", "25", "성인광고, 대출광고 등 기타 제한");
	public static final ResultCode R_SMS_26					= new ResultCode("5021", "26", "스팸데이콤 스팸 필터링");
	public static final ResultCode R_SMS_27					= new ResultCode("5022", "27", "기타에러");
	public static final ResultCode R_SMS_28               	= new ResultCode("5023", "28", "사전 미등록 발신번호 사용");
	public static final ResultCode R_SMS_29               	= new ResultCode("5024", "29", "전화번호 세칙 미준수 발신번호 사용");
	public static final ResultCode R_SMS_30               	= new ResultCode("5025", "30", "발신번호 변작으로 등록된 발신번호 사용");
	public static final ResultCode R_SMS_31               	= new ResultCode("5026", "31", "번호도용문자차단서비스에 가입된 발신번호 사용");
	public static final ResultCode R_SMS_40               	= new ResultCode("5026", "40", "단말기착신거부(스팸등)");
	public static final ResultCode R_SMS_97               	= new ResultCode("5097", "97", "전송시간 초과");
	public static final ResultCode R_SMS_98               	= new ResultCode("5098", "98", "중복키");
	public static final ResultCode R_SMS_99	              	= new ResultCode("5099", "99", "건수없음");


}
