package com.example;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class PropertiesApplication implements ApplicationRunner
{
	@Value( "${default.ip}" )
	private String defaultIp;
	
	@Value( "${default.port}" )
	private String defaultPort;

	@Value( "${server.name}" )
	private String serverNum;	
	
	@Value( "${server.num}" )
	private String serverName;	
	
	@Value( "${server.info.ip}" )
	private String infoIp;	

	@Value( "${server.info.port}" )
	private String infoPort;	

	@Value( "${append.ip}" )
	private String appendIp;	

	@Value( "${append.port}" )
	private String appendPort;		

	@Value( "${optional.ip}" )
	private String optionalIp;
	
	@Value( "${optional.port}" )
	private String optionalPort;	
	
	public static void main( String[] args )
	{
		SpringApplication.run( PropertiesApplication.class, args );
	}
	
    @Override
    public void run( ApplicationArguments args )
    {
    	System.out.println( "default.ip :" + defaultIp );
    	System.out.println( "default.port :" + defaultPort );
  
    	System.out.println( "server.name :" + serverName );
    	System.out.println( "server.num :" + serverNum );
    	
    	System.out.println( "server.info.ip :" + infoIp );
    	System.out.println( "server.info.port :" + infoPort );
    	
    	System.out.println( "append.ip :" + appendIp );
    	System.out.println( "append.port :" + appendPort );
    	
    	System.out.println( "optional.ip :" + optionalIp );
    	System.out.println( "optional.port :" + optionalPort );     	
    }
}
