package com.example.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.domain.UserDto;
import com.example.domain.UserMapper;


@Service
public class PostService
{
    @Autowired
    UserMapper userDao;	
    
    private static final Logger logger = LoggerFactory.getLogger( PostService.class );    

    
	public String selectUser( Integer num )
	{
		Integer count = 0;
		
		StringBuffer result = new StringBuffer();
		
		// String -> List
		List< Map< String, Object > > list = userDao.selectUser( num );

		result.append( "[" );
		
        for( Map< String, Object > user : list )
        {
        	if( count > 0 )
        	{
        		result.append( "," );
        	}

        	result.append( "{"
   			               + "num : " + user.get( "num" ) + ", "
   			               + "id : "  + user.get( "id"  ) + ", "
   			               + "pwd : " + user.get( "pwd" )
   			               + "}"
   			               );        	
        	
            System.out.println( "num : " + user.get( "num" ) + ", "
                              + "id : "  + user.get( "id"  ) + ", "
                              + "pwd : " + user.get( "pwd" )
                              );
        	
            logger.info( "num: {}, id: {}, pwd: {}"
            		   , user.get( "num" ).toString()
            		   , user.get( "id"  ).toString()
            		   , user.get( "pwd" ).toString()
            		   );
            
            count++;
        }
        
        result.append( "]" );
  
        System.out.println( "result : " + result.toString() );
                
        logger.info( "result : {}", result.toString() );        
		
		return result.toString();
	}	
	
	public String insertUser( Map< String, Object > body )
	{
		UserDto user = new UserDto();
		
		user.setId ( body.get( "id"  ).toString() );
		user.setPwd( body.get( "pwd" ).toString() );
		
		// Dto -> Int
		Integer result = userDao.insertUser( user );
		
		logger.info( "result : {}", result );
		
		System.out.println( "result : " + result.toString() );
		
		return result.toString();
	}
	
	public String updateUser( Map< String, Object > body )
	{
		Map< String, Object > user = new HashMap< String, Object >();
		
		user.put( "num", body.get( "num" ).toString() );
		
		if( body.containsKey( "id"  ) == true )
		{
			user.put( "id" , body.get( "id"  ).toString() );
		}
		
		if( body.containsKey( "pwd" ) == true )
		{
			user.put( "pwd", body.get( "pwd" ).toString() );
		}
		
		// Map -> Int
		Integer result = userDao.updateUser( user );
		
		System.out.println( "result : " + result.toString() );
		
		return result.toString();
	}	
	
	public String deleteUser( Map< String, Object > body )
	{
		Integer num = (Integer) body.get( "num" );
		
		// Int -> Int
		Integer result = userDao.deleteUser( num );
		
		System.out.println( "result : " + result.toString() );

		return result.toString();
	}	    
}
