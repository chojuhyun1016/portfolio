package com.example.controller;


import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.service.PostService;


@RestController
@RequestMapping( "post" )
public class PostController
{
    @Autowired
    PostService postService;
  
    private static final Logger logger = LoggerFactory.getLogger( PostController.class );    
	
	
    /********************************************************************
	*                               Post                                *
    ********************************************************************/
    // 01. Map -> String
	@PostMapping( "/getMapHeaderBody" )
	public String getMapHeaderBody( @RequestHeader Map< String, Object > header
			                      , @RequestBody   Map< String, Object > body
			                      )
	{
		Integer num = (Integer)body.get( "num" );
		String  id  = (String) body.get( "id"  );
		String  pwd = (String) body.get( "pwd" );

		System.out.println( "header : " + header.toString() );
		System.out.println( "body : " + body.toString() );
		System.out.println( "num : " + num + ", "
				          + "id : "  + id  + ", "
				          + "pwd : " + pwd
				          );

		logger.info( "header : {}", header.toString() );
		logger.info( "body : {}", body.toString() );
		logger.info( "num : {}, id : {}, pwd : {}", num, id, pwd );
		
		return "header : " + header.toString() + "\n"
			 + "body : "   + body.toString()
			   ;
	}
	
	// 02. String -> String
	@PostMapping( "/getStringHeaderBody" )
	public String postStringHeaderBody( @RequestHeader Map< String, Object > header
			                          , @RequestBody   String                body 
			                          )
	{
		System.out.println( "header : " + header.toString() );
		System.out.println( "body : " + body.toString() );		

		logger.info( "header : {}", header.toString() );
		logger.info( "body : {}", body.toString() );
		
		return "header : " + header + "\n"
			 + "body : "   + body
			   ;
	}	
	
	// 03. Dto -> String
	@PostMapping( "/getObjectBodyReturnString" )
	public String getObjectBodyReturnString( @RequestBody com.example.domain.UserDto body )
	{
		System.out.println( "body : " + body.toString() );
		
		logger.info( "body : {}", body.toString() );
		
		return body.toString();
	}	
	
	// 04. Dto -> Dto
	@PostMapping( "/getObjectBodyReturnJson" )
	public com.example.domain.UserDto getObjectBodyReturnJson( @RequestBody com.example.domain.UserDto body )
	{
		System.out.println( "body : " + body.toString() );
		
		logger.info( "body : {}", body );
		
		return body;
	}

	// 05. Map -> String
	@PostMapping( value = { "/selectUser/{num}", "/selectUser" } )
	public String selectUser( @PathVariable(required = false) Optional<Integer> num
			                , @RequestHeader Map< String, Object > header
			                , @RequestBody   Map< String, Object > body
			                )
	{
		System.out.println( "header : " + header.toString() );
		System.out.println( "body : " + body.toString() );
		
		logger.info( "header : {}", header.toString() );
		logger.info( "body : {}", body.toString() );
		
		return postService.selectUser( num.orElse( null ) );
	}	
	
	// 06. Map -> String
	@PostMapping( "/insertUser" )
	public String insertUser( @RequestHeader Map< String, Object > header
			               ,  @RequestBody   Map< String, Object > body
			                )
	{
		System.out.println( "header : " + header.toString() );
		System.out.println( "body : " + body.toString() );
		
		logger.info( "header : {}", header.toString() );
		logger.info( "body : {}", body.toString() );
		
		return postService.insertUser( body );
	}
	
	// 07. Map -> String
	@PatchMapping( "/updateUser" )
	public String updateUser( @RequestHeader Map< String, Object > header
			               ,  @RequestBody   Map< String, Object > body
			                )
	{
		System.out.println( "header : " + header.toString() );
		System.out.println( "body : " + body.toString() );
		
		logger.info( "header : {}", header.toString() );
		logger.info( "body : {}", body.toString() );

		return postService.updateUser( body );
	}
	
	// 08. Map -> String
	@DeleteMapping( "/deleteUser" )
	public String deleteUser( @RequestHeader Map< String, Object > header
			               ,  @RequestBody   Map< String, Object > body
			                )
	{
		System.out.println( "header : " + header.toString() );
		System.out.println( "body : " + body.toString() );
		
		logger.info( "header : {}", header.toString() );
		logger.info( "body : {}", body.toString() );
		
		return postService.deleteUser( body );
	}		
}
