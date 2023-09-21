package com.example.mp.gw;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.example.mp.gw.maintain.service.MaintainSingleService;


@Component
public class AppRunner implements ApplicationRunner
{
	@Autowired
	MaintainSingleService maintainSingleService;


    @Override
    public void run(ApplicationArguments args) throws Exception
    {
    	maintainSingleService.startWorker();
    }
}