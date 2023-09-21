package com.example.mp.gw.common.domain;


import com.example.mp.gw.common.domain.Const.FORMATTER;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : RedisQueueDataWrapper.java
 * @Description : Redis Queue Data Wrapper 객체 
 * 
 * @author 조주현
 * @since 2022.01.20
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.01.20	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Data
@Builder
@Setter
@Getter
@ToString
public class RedisQueueDataWrapper<T>
{
	// Redis Queue Name
	private String    structure_name;

	// Packet 전송 횟수
	private int       send_count;

	// Packet 마지막 전송 시간
	private String	  send_dtm;

	// Packet 데이터
	private T 		  data;


    public RedisQueueDataWrapper()
    {
    	this.structure_name = "";
    	
    	this.send_count = 0;
    	
    	this.send_dtm = "";
    }

    public void incSndCnt()
    {
    	this.send_count++;
    }

    public void decSndCnt()
    {
    	this.send_count--;
    }

    public void setSndDtm()
    {
    	this.send_dtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());
    }

	/**
	 * @param structure_name
	 * @param send_count
	 * @param send_dtm
	 * @param data
	 */
	public RedisQueueDataWrapper(String structure_name, Integer send_count, String send_dtm, T data)
	{
		this.structure_name	= structure_name;
		this.send_count		= send_count;
		this.send_dtm		= send_dtm;
		this.data			= data;
	}
}
