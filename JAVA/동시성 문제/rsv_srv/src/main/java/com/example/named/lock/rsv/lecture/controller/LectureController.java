package com.example.named.lock.rsv.lecture.controller;


import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.example.named.lock.rsv.lecture.domain.Appl;
import com.example.named.lock.rsv.lecture.domain.DelApplRequest;
import com.example.named.lock.rsv.lecture.domain.DelLectRequest;
import com.example.named.lock.rsv.lecture.domain.GetApplRequest;
import com.example.named.lock.rsv.lecture.domain.GetLectRequest;
import com.example.named.lock.rsv.lecture.domain.Lect;
import com.example.named.lock.rsv.lecture.domain.RegApplRequest;
import com.example.named.lock.rsv.lecture.domain.RegLectRequest;
import com.example.named.lock.rsv.lecture.exception.DuplicateApplException;
import com.example.named.lock.rsv.lecture.lock.NamedLock;
import com.example.named.lock.rsv.lecture.service.LectureService;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
public class LectureController
{
    @Autowired
    NamedLock userLevelLock;

    @Autowired
    LectureService letcureService;

    private static final String LOCK_LECT_APPL_NAME    = "LECT_APPL_LOCK";
    private static final int    LOCK_LECT_APPL_TIMEOUT = 3;


    @RequestMapping(value = "/list/lect", method = RequestMethod.POST)
	public ResponseEntity<?> getLectList(HttpServletRequest request, @RequestBody GetLectRequest body)
	{
    	try
    	{
    		List<Lect> list = letcureService.getLectList(body);

    		return ResponseEntity.ok().body(new Gson().toJson(list, List.class));
    	}
    	catch (Exception e)
    	{
    		log.error("{}", e.getMessage());

    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(e.getMessage());
    	}
	}

    @RequestMapping(value = "/list/lect/pop", method = RequestMethod.POST)
	public ResponseEntity<?> getLectListPop(HttpServletRequest request)
	{
    	try
    	{
    		List<Lect> list = letcureService.getLectListPop();

    		return ResponseEntity.ok().body(new Gson().toJson(list, List.class));
    	}
    	catch (Exception e)
    	{
    		log.error("{}", e.getMessage());

    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(e.getMessage());
    	}
	}    

    @RequestMapping(value = "/list/appl", method = RequestMethod.POST)
	public ResponseEntity<?> getApplList(HttpServletRequest request, @RequestBody GetApplRequest body)
	{
    	try
    	{
    		List<Appl> list = letcureService.getApplList(body);

    		return ResponseEntity.ok().body(new Gson().toJson(list, List.class));
    	}
    	catch (Exception e)
    	{
    		log.error("{}", e.getMessage());

    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(e.getMessage());
    	}
	}

	@RequestMapping(value = "/reg/lect", method = RequestMethod.PUT)
	public ResponseEntity<?> regLect(HttpServletRequest request, @RequestBody RegLectRequest body)
	{
		try
		{
			letcureService.regLect(body);

			return ResponseEntity.ok().body("등록에 성공하였습니다.");
		}
    	catch (Exception e)
    	{
    		log.error("{}", e.getMessage());

    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(e.getMessage());
    	}
	}

	@RequestMapping(value = "/reg/appl", method = RequestMethod.PUT)
	public ResponseEntity<?> regAppl(HttpServletRequest request, @RequestBody RegApplRequest body)
	{
		try
		{
			userLevelLock.executeWithLock(LOCK_LECT_APPL_NAME
										, LOCK_LECT_APPL_TIMEOUT
										, () -> letcureService.regAppl(body)
										 );

			return ResponseEntity.ok().body("등록에 성공하였습니다.");
		}
		catch (DuplicateApplException e)
		{
    		log.error("{}", e.getMessage());

    		return ResponseEntity.ok().body(e.getMessage());	
		}
    	catch (Exception e)
    	{
    		log.error("{}", e.getMessage());

    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(e.getMessage());
    	}
	}	

	@RequestMapping(value = "/del/lect", method = RequestMethod.DELETE)
	public ResponseEntity<?> delLect(HttpServletRequest request, @RequestBody DelLectRequest body)
	{
		try
		{			
			letcureService.delLect(body);

			return ResponseEntity.ok().body("삭제에 성공하였습니다.");
		}
    	catch (Exception e)
    	{
    		log.error("{}", e.getMessage());

    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(e.getMessage());
    	}
	}

	@RequestMapping(value = "/del/appl", method = RequestMethod.DELETE)
	public ResponseEntity<?> delAppl(HttpServletRequest request, @RequestBody DelApplRequest body)
	{
		try
		{
			userLevelLock.executeWithLock(LOCK_LECT_APPL_NAME
										, LOCK_LECT_APPL_TIMEOUT
										, () -> letcureService.delAppl(body)
					 					 );

			return ResponseEntity.ok().body("삭제에 성공하였습니다.");
		}
    	catch (Exception e)
    	{
    		log.error("{}", e.getMessage());

    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(e.getMessage());
    	}
	}
}
