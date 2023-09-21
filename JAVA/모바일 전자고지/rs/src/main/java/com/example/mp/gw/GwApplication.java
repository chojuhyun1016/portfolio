package com.example.mp.gw;


import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@SpringBootApplication
@EnableAspectJAutoProxy
public class GwApplication
{
	@Value("${tomcat.ajp.protocol}")
	String ajpProtocol;
	
	@Value("${tomcat.ajp.port}")
	int ajpPort;

	
	public static void main(String[] args)
	{
		SpringApplication.run(GwApplication.class, args);
	}

	@Bean
	public TomcatServletWebServerFactory tomcatFactory() throws UnknownHostException
	{
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory()
		{
			@Override  
			protected void postProcessContext(Context context)
			{
				((StandardJarScanner) context.getJarScanner()).setScanManifest(false);  
			}  
		};

		factory.addAdditionalTomcatConnectors(createAjpConnector());

		return factory;
	}

	private Connector createAjpConnector() throws UnknownHostException
	{
	      Connector ajpConnector = new Connector(ajpProtocol);
	      
	      ajpConnector.setPort(ajpPort);
	      ajpConnector.setSecure(false);
	      ajpConnector.setAllowTrace(false); // TRACE 메서드 허용 여부
	      ajpConnector.setScheme("http");

	      ((AbstractAjpProtocol<?>) ajpConnector.getProtocolHandler()).setSecretRequired(false);
	      ((AbstractAjpProtocol<?>) ajpConnector.getProtocolHandler()).setAddress(InetAddress.getByName("0.0.0.0"));
	      
	      return ajpConnector;
	}
}
