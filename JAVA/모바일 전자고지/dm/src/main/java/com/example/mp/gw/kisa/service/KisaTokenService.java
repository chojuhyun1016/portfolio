package com.example.mp.gw.kisa.service;


import com.example.mp.gw.kisa.domain.KisaAccessTokenRequest;
import com.example.mp.gw.kisa.domain.KisaAccessTokenResponse;


public interface KisaTokenService
{
	public void issueKisaToken();

	public void popAndIssueKisaToken();

	public KisaAccessTokenResponse issueKisaAccessToken(KisaAccessTokenRequest accessTokenRequest);
}
