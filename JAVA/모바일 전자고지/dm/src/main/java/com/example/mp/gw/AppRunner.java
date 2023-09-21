package com.example.mp.gw;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.example.mp.gw.bc.domain.BcAccessTokenResponse;
import com.example.mp.gw.bc.service.BcTokenService;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.RedisStructure.H_BC_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.H_KISA_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_SESSION;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.kisa.domain.KisaAccessTokenResponse;
import com.example.mp.gw.kisa.service.KisaTokenService;
import com.example.mp.gw.maintain.service.MaintainSingleService;


@Component
public class AppRunner implements ApplicationRunner
{
	@Autowired
	MaintainSingleService maintainSingleService;

	@Autowired
	private BcTokenService bcTokenService;

	@Autowired
	private KisaTokenService kisaTokenService;

	@Autowired
	private RedisService<String, BcAccessTokenResponse> redisBizCenterAccessTokenService;

	@Autowired
	private RedisService<String, KisaAccessTokenResponse> redisAccessTokenService;


    @Override
    public void run(ApplicationArguments args) throws Exception
    {
    	try
    	{
    		BcAccessTokenResponse bizCenterAccessTokenResponse = redisBizCenterAccessTokenService.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if(bizCenterAccessTokenResponse != null)
				redisBizCenterAccessTokenService.hmdel(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);
		}
    	catch (Exception e)
    	{
        	redisBizCenterAccessTokenService.lpush(Q_BC_SESSION.STRUCTURE_NAME, BcAccessTokenResponse.builder()
    				.error("issueSessionError")
    				.error_description(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
    				.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
    				.build());
		}

    	try
    	{
			KisaAccessTokenResponse accessTokenResponse = redisAccessTokenService.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (accessTokenResponse != null)
				redisAccessTokenService.hmdel(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);
		}
    	catch (Exception e)
    	{
        	redisAccessTokenService.lpush(Q_KISA_SESSION.STRUCTURE_NAME, KisaAccessTokenResponse.builder()
    				.errCode("0")
    				.errMsg(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
    				.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
    				.build());
		}

    	maintainSingleService.startWorker();

    	bcTokenService.issueBcToken();

    	kisaTokenService.issueKisaToken();
    }
}
