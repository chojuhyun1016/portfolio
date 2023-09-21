package com.example.mp.gw.maintain.service;


import com.example.mp.gw.maintain.domain.WorkerInfo;


public interface MaintainSingleService
{
	public void startWorker();

	public void updateWorker();

	public void endWorker();

	public void setWorker(WorkerInfo info);

	public void switchWorker(WorkerInfo info);

	public boolean isRunning(); 
}
