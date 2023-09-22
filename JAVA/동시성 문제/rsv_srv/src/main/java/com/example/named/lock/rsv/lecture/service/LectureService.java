package com.example.named.lock.rsv.lecture.service;


import java.util.List;

import com.example.named.lock.rsv.lecture.domain.Appl;
import com.example.named.lock.rsv.lecture.domain.DelApplRequest;
import com.example.named.lock.rsv.lecture.domain.DelLectRequest;
import com.example.named.lock.rsv.lecture.domain.GetApplRequest;
import com.example.named.lock.rsv.lecture.domain.GetLectRequest;
import com.example.named.lock.rsv.lecture.domain.Lect;
import com.example.named.lock.rsv.lecture.domain.RegApplRequest;
import com.example.named.lock.rsv.lecture.domain.RegLectRequest;


public interface LectureService
{
	public List<Lect> getLectList(GetLectRequest request);
	public List<Lect> getLectListPop();
	public List<Appl> getApplList(GetApplRequest request);

	public int        regLect(RegLectRequest request);
	public int        regAppl(RegApplRequest request);

	public int        delLect(DelLectRequest request);
	public int        delAppl(DelApplRequest request);
}
