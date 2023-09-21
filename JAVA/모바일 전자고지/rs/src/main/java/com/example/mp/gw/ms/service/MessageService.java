package com.example.mp.gw.ms.service;


import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.example.mp.gw.ms.domain.BeforehandRequest;
import com.example.mp.gw.ms.domain.MessageRequest;
import com.example.mp.gw.ms.domain.RemindMessageRequest;
import com.example.mp.gw.ms.domain.ReportRequest;
import com.example.mp.gw.ms.exception.FailRegisterRcsImageException;

/**
 * @Class Name : MessageService.java
 * @Description : 메시지 서비스 인터페이스
 * 
 * @author 조주현
 * @since 2021.03.29
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.29	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public interface MessageService
{
	public void sendRemindMessages(RemindMessageRequest messageRequest) throws IOException;

	public void sendBeforehandMessages(BeforehandRequest messageRequest);

	public void sendMainMessages(MessageRequest messageRequest) throws IOException;

	public void sendReports(ReportRequest messageRequest) throws IOException;

	public void registerRcsImage(String attachId, MultipartFile image) throws IllegalStateException, IOException, FailRegisterRcsImageException;

	public void registerRcsImageForMsg(MessageRequest messageRequest, String NOWForAttachId) throws IllegalStateException, IOException, FailRegisterRcsImageException;
}
