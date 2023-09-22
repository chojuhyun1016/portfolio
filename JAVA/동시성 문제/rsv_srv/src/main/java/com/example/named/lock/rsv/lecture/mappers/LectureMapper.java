package com.example.named.lock.rsv.lecture.mappers;


import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.example.named.lock.rsv.lecture.domain.Appl;
import com.example.named.lock.rsv.lecture.domain.Lect;


@Mapper
public interface LectureMapper
{
	public int        regLect(Map<String, Object> regRow);
	public List<Lect> getLect(Map<String, Object> schKey);
	public List<Lect> getLectPop();
	public int        delLect(Map<String, Object> delKey);
	
	public int        regAppl(Map<String, Object> regRow);
	public List<Appl> getAppl(Map<String, Object> schKey);
	public int        getApplCnt(Map<String, Object> getKey);
	public int        delAppl(Map<String, Object> delKey);
}
