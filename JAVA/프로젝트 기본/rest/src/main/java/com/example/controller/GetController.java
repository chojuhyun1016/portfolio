package com.example.controller;


import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.service.GetService;


@RestController
@RequestMapping( "get" )
public class GetController
{
    @Autowired
    GetService getService;
    
    private static final Logger logger = LoggerFactory.getLogger( GetController.class );    

    
    /********************************************************************
	*                               Get                                 *
    ********************************************************************/
    // 01. Map -> String
	@GetMapping( "/getMapHeader" )
	public String getMapHeader( @RequestHeader Map< String, Object > header )
	{
        System.out.println( "header : " + header.toString() );
		
		logger.info( "header : {}", header.toString() );
		
		return header.toString();
	}
	
	// 02. Param -> String
	@GetMapping( "/getParamHeader" )
	public String getParamHeader( @RequestHeader Map< String, Object > header
			                    , Integer num                                   // 필수값 아님
			                    , @RequestParam String  id                      // 필수값
			                    , @RequestParam( name = "pwd" ) String password	// 필수값 + 변수명 변경
			                    )
	{
		System.out.println( "header : " + header.toString() );
        System.out.println( "num : " + num      + ", "
                          + "id : "  + id       + ", "
                          + "pwd : " + password 
                          );
        
        logger.info( "header : {}", header.toString() );
		logger.info( "num : {}, id : {}, pwd : {}", num, id, password );
		
		return "num : " + num.toString() + ", "
		     + "id : "  + id             + ", "
		     + "pwd : " + password
		       ;
	}	
	
	// 03. Dto -> String
	@GetMapping( "/getObjectHeader" )
	public String getObjectHeader( com.example.domain.UserDto header )
	{
		System.out.println( "header : " + header.toString() );
        System.out.println( "num: " + header.getNum() + " "
                          + "id: "  + header.getId()  + " "
                          + "pwd: " + header.getPwd() 
                          );
        
        logger.info( "header : {}", header.toString() );
		logger.info( "num : {}, id : {}, pwd : {}", header.getNum().toString(),  header.getId(), header.getPwd() );
		
		return "num : " + header.getNum().toString() + ", "
	         + "id : "  + header.getId()             + ", "
	         + "pwd : " + header.getPwd()
	           ;
	}
	
	// 04. Dto -> Dto(Json)
	@GetMapping( "/getObjectHeaderReturnJson" )
	public com.example.domain.UserDto getVoObjectReturnJson( com.example.domain.UserDto header )
	{
		System.out.println( "header : " + header.toString() );
        System.out.println( "num : " + header.getNum() + ", "
                          + "id : "  + header.getId()  + ", "
                          + "pwd : " + header.getPwd() 
                          );
        
        logger.info( "header : {}", header.toString() );
		logger.info( "num : {}, id : {}, pwd : {}", header.getNum().toString(),  header.getId(), header.getPwd() );

		// object 로 return 하는 경우 자동으로 json 규격으로 변형하여 전송
		return header;
	}

	// 05. Map -> String
	@GetMapping( "/selectUser" )
	public String selectUser( @RequestParam(required = false) Integer num 
			                , @RequestHeader Map< String, Object > header )
	{
		System.out.println( "header : " + header.toString() );
		
		logger.info( "header : {}", header.toString() );
          
		return getService.selectUser( num );
	}		
	
	// 6. Param -> String
	@GetMapping( "/insertUser" )
	public String insertUser( @RequestHeader Map< String, Object > header
			                , @RequestParam String id
			                , @RequestParam String pwd
			                )
	{
		System.out.println( "header : " + header.toString() );
        System.out.println( "id : " + id + ", " + "pwd : " + pwd );
		
		logger.info( "header : {}", header.toString() );
        logger.info( "id : {}, pwd : {}", id, pwd );
				
		return getService.insertUser( id, pwd );
	}		

	// 07. Param -> String
	@GetMapping( "/updatetUser" )
	public String updatetUser( @RequestHeader Map< String, Object > header
			                 , @RequestParam Integer num
			                 , @RequestParam String  id
			                 , @RequestParam String  pwd
			                 )
	{
		System.out.println( "header : " + header.toString() );
        System.out.println( "num : " + num + ", "
		                  + "id : "  + id  + ", "
        		          + "pwd : " + pwd
        		          );
		
		logger.info( "header : {}", header.toString() );
        logger.info( "num : {}, id : {}, pwd : {}", num, id, pwd );
		
		return getService.updatetUser( num, id, pwd );
	}			

	// 08. Param -> String
	@GetMapping( "/deletetUser" )
	public String deletetUser( @RequestHeader Map< String, Object > header
			                 , @RequestParam Integer num
			                 )
	{
		System.out.println( "header : " + header.toString() );
		System.out.println( "num : " + num.toString() );
		
		logger.info( "header : {}", header.toString() );
		logger.info( "num : {}", num.toString() );
		
		return getService.deletetUser( num );
	}		
}
