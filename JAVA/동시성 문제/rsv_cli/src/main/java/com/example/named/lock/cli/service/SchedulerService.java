package com.example.named.lock.cli.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.named.lock.cli.domain.RegApplRequest;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class SchedulerService
{
    @Autowired 
    RestTemplate restTemplate;


    @Scheduled(cron = "10 * * * * *")
    public void cron1()
    {	
		HttpEntity<RegApplRequest> request  = null;

		RegApplRequest appl = null;
		HttpHeaders header = new HttpHeaders();

		for (int i =1; i < 1000; i++)
		{
			if((i % 100) == 0)
				log.info("Cron [01] Send");

			appl = new RegApplRequest();
			
			appl.setLet_no(1L);
			appl.setAppl_no(String.format("01%03d", i));
			
			request = new HttpEntity<>(appl, header);
			
			try
			{
				restTemplate.exchange("http://localhost:8080/reg/appl", HttpMethod.PUT, request, String.class);
			}
			catch (Exception e)
			{
				if((i % 100) == 0)
					log.error("exception : {}", e.getMessage());
			}
		}
    }
    
    @Scheduled(cron = "10 * * * * *")
    public void cron2()
    {	
		HttpEntity<RegApplRequest> request  = null;

		RegApplRequest appl = null;
		HttpHeaders header = new HttpHeaders();

		for (int i =1; i < 1000; i++)
		{
			if((i % 100) == 0)
				log.info("Cron [02] Send");

			appl = new RegApplRequest();
			
			appl.setLet_no(1L);
			appl.setAppl_no(String.format("02%03d", i));
			
			request = new HttpEntity<>(appl, header);
			
			try
			{
				restTemplate.exchange("http://localhost:8080/reg/appl", HttpMethod.PUT, request, String.class);
			}
			catch (Exception e)
			{
				if((i % 100) == 0)
					log.error("exception : {}", e.getMessage());
			}
		}
    }
    
    @Scheduled(cron = "10 * * * * *")
    public void cron3()
    {	
		HttpEntity<RegApplRequest> request  = null;

		RegApplRequest appl = null;
		HttpHeaders header = new HttpHeaders();

		for (int i =1; i < 1000; i++)
		{
			if((i % 100) == 0)
				log.info("Cron [03] Send");

			appl = new RegApplRequest();
			
			appl.setLet_no(1L);
			appl.setAppl_no(String.format("03%03d", i));
			
			request = new HttpEntity<>(appl, header);
			
			try
			{
				restTemplate.exchange("http://localhost:8080/reg/appl", HttpMethod.PUT, request, String.class);
			}
			catch (Exception e)
			{
				if((i % 100) == 0)
					log.error("exception : {}", e.getMessage());
			}
		}
    }
    
    @Scheduled(cron = "10 * * * * *")
    public void cron4()
    {	
		HttpEntity<RegApplRequest> request  = null;

		RegApplRequest appl = null;
		HttpHeaders header = new HttpHeaders();

		for (int i =1; i < 1000; i++)
		{
			if((i % 100) == 0)
				log.info("Cron [04] Send");

			appl = new RegApplRequest();
			
			appl.setLet_no(1L);
			appl.setAppl_no(String.format("04%03d", i));
			
			request = new HttpEntity<>(appl, header);
			
			try
			{
				restTemplate.exchange("http://localhost:8080/reg/appl", HttpMethod.PUT, request, String.class);
			}
			catch (Exception e)
			{
				if((i % 100) == 0)
					log.error("exception : {}", e.getMessage());
			}
		}
    }
    
    @Scheduled(cron = "10 * * * * *")
    public void cron5()
    {	
		HttpEntity<RegApplRequest> request  = null;

		RegApplRequest appl = null;
		HttpHeaders header = new HttpHeaders();

		for (int i =1; i < 1000; i++)
		{
			if((i % 100) == 0)
				log.info("Cron [05] Send");

			appl = new RegApplRequest();
			
			appl.setLet_no(1L);
			appl.setAppl_no(String.format("05%03d", i));
			
			request = new HttpEntity<>(appl, header);
			
			try
			{
				restTemplate.exchange("http://localhost:8080/reg/appl", HttpMethod.PUT, request, String.class);
			}
			catch (Exception e)
			{
				if((i % 100) == 0)
					log.error("exception : {}", e.getMessage());
			}
		}
    }
}
