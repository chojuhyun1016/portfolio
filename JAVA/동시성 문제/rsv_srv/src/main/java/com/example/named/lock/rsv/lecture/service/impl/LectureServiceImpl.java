package com.example.named.lock.rsv.lecture.service.impl;


import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.named.lock.rsv.lecture.domain.Appl;
import com.example.named.lock.rsv.lecture.domain.DelApplRequest;
import com.example.named.lock.rsv.lecture.domain.DelLectRequest;
import com.example.named.lock.rsv.lecture.domain.GetApplRequest;
import com.example.named.lock.rsv.lecture.domain.GetLectRequest;
import com.example.named.lock.rsv.lecture.domain.Lect;
import com.example.named.lock.rsv.lecture.domain.RegApplRequest;
import com.example.named.lock.rsv.lecture.domain.RegLectRequest;
import com.example.named.lock.rsv.lecture.exception.DuplicateApplException;
import com.example.named.lock.rsv.lecture.exception.NotExistLectException;
import com.example.named.lock.rsv.lecture.exception.OverMaxApplException;
import com.example.named.lock.rsv.lecture.mappers.LectureMapper;
import com.example.named.lock.rsv.lecture.service.LectureService;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("LectureService")
public class LectureServiceImpl implements LectureService
{
	@Autowired
	private LectureMapper lectureMapper;


	@Override
	@Transactional(rollbackFor = Exception.class)
	public int regLect(RegLectRequest request)
	{
		try
		{
			return lectureMapper.regLect(request.toMap());
		}
		catch (DataAccessException e)
		{			
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getCause().getMessage());
			
			throw e;
		}
		catch (Exception e)
		{
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage());
			
			throw e;
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int regAppl(RegApplRequest request)
	{
		try
		{
			Map<String, Object> key = request.toMap();

			key.put("no", request.getLet_no());
			key.remove("appl_no");

			List<Lect> list = lectureMapper.getLect(key);

			if (null == list || (null != list && list.isEmpty()))
				throw new NotExistLectException();

			int cnt = lectureMapper.getApplCnt(key);

			if (list.get(0).getMax_appl() <= cnt)
				throw new OverMaxApplException();	

			key.put("appl_no", request.getAppl_no());

			cnt = lectureMapper.getApplCnt(key);

			if (0 != cnt)
				throw new DuplicateApplException();

			return lectureMapper.regAppl(request.toMap());
		}
		catch (DataAccessException e)
		{			
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getCause().getMessage());
			
			throw e;
		}
		catch (Exception e)
		{
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage());
			
			throw e;
		}
	}	

	@Override
	@Transactional(readOnly = true)
	public List<Lect> getLectList(GetLectRequest request)
	{
		try
		{
			return lectureMapper.getLect(request.toMap());
		}
		catch (DataAccessException e)
		{			
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getCause().getMessage());
			
			throw e;
		}
		catch (Exception e)
		{
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage());
			
			throw e;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Lect> getLectListPop()
	{
		try
		{
			return lectureMapper.getLectPop();
		}
		catch (DataAccessException e)
		{			
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getCause().getMessage());
			
			throw e;
		}
		catch (Exception e)
		{
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage());
			
			throw e;
		}
	}	

	@Override
	@Transactional(readOnly = true)
	public List<Appl> getApplList(GetApplRequest request)
	{
		try
		{
			return lectureMapper.getAppl(request.toMap());
		}
		catch (DataAccessException e)
		{			
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getCause().getMessage());

			throw e;
		}
		catch (Exception e)
		{
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage());

			throw e;
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int delLect(DelLectRequest request)
	{
		try
		{
			return lectureMapper.delLect(request.toMap());
		}
		catch (DataAccessException e)
		{			
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getCause().getMessage());
			
			throw e;
		}
		catch (Exception e)
		{
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage());
			
			throw e;
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int delAppl(DelApplRequest request)
	{
		try
		{
			return lectureMapper.delAppl(request.toMap());
		}
		catch (DataAccessException e)
		{			
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getCause().getMessage());
			
			throw e;
		}
		catch (Exception e)
		{
			log.error(" \"method\": \"{}\",  \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage());
			
			throw e;
		}
	}
}
