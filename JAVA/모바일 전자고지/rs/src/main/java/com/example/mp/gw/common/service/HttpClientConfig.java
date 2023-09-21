package com.example.mp.gw.common.service;


import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.example.mp.gw.common.domain.Const.MEMBER;


/**
 * @Class Name : HttpClientConfig.java
 * @Description : HTTP 통신 관련 Config
 * 
 * @author 조주현
 * @since 2021.04.23
 * @version 1.0
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.23	    조주현          최초 생성
 * 
 *      </pre>
 * 
 */


@Configuration
public class HttpClientConfig
{
	@Value("${spring.kisa.token.kces}")
	private String TOKEN_KCES;
	
	@Value("${spring.kisa.token.dstrbtn}")
	private String TOKEN_DSTRBTN;
	
	@Autowired
	private RestTemplateLoggingInterceptor restTemplateLoggingInterceptor;
	

	@Bean
	public RestTemplate restTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException
	{
		TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;

		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, acceptingTrustStrategy).build();
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslsf).register("http", new PlainConnectionSocketFactory()).build();

	    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
	    connectionManager.setMaxTotal(200);
	    connectionManager.setDefaultMaxPerRoute(20);

	    CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf)
				.setConnectionManager(connectionManager).build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		restTemplate.getInterceptors().add(restTemplateLoggingInterceptor);

		return restTemplate;
	}
	
	@SuppressWarnings("serial")
	@Bean(name = "kcesHeaders")
	public HttpHeaders kcesHeaders()
	{
		return new HttpHeaders() {{
			add("platform-id", MEMBER.KISA.LGUPLUS.RLY_PLTFM_ID.val());
		}};	
	}
	
	@SuppressWarnings("serial")
	@Bean(name = "kcesMultipartHeaders")
	public HttpHeaders kcesMultipartHeaders()
	{
		return new HttpHeaders() {{
			setContentType(MediaType.MULTIPART_MIXED);
			add("platform-id", MEMBER.KISA.LGUPLUS.RLY_PLTFM_ID.val());
			setBearerAuth(TOKEN_KCES);
		}};	
	}

	@SuppressWarnings("serial")
	@Bean(name = "dstrbtnHeaders")
	@Primary
	public HttpHeaders dstrbtnHeaders()
	{
		return new HttpHeaders() {{
			setBearerAuth(TOKEN_DSTRBTN);
		}};	
	}
	
	@SuppressWarnings("serial")
	@Bean(name = "bizCenterHeaders")
	public HttpHeaders bizCenterHeaders()
	{
		return new HttpHeaders() {{
			add("client-id", "1000000004");
			add("client-tp", "30");
		}};	
	}
	
	
	@SuppressWarnings("serial")
	@Bean(name = "bizCenterMutlipartHeaders")
	public HttpHeaders bizCenterMutlipartHeaders()
	{
		return new HttpHeaders() {{
			setContentType(MediaType.MULTIPART_FORM_DATA);
		}};	
	}
}
