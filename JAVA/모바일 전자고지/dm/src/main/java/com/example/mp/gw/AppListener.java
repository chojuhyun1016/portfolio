package com.example.mp.gw;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import com.example.mp.gw.maintain.service.MaintainSingleService;


@Component
public class AppListener implements ApplicationListener<ContextClosedEvent>
{
	@Autowired
	MaintainSingleService maintainSingleService;


	@Override
	public void onApplicationEvent(ContextClosedEvent event)
	{
		maintainSingleService.endWorker();
	}
}
