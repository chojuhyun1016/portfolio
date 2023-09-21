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
public class GetService
{
    @Autowired
    UserMapper userDao;
    
	private static final Logger logger = LoggerFactory.getLogger( GetService.class ); 

	
	public String selectUser( Integer num )
	{
		Integer count = 0;
		
		StringBuffer result = new StringBuffer();
		
		// None -> List
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
        	
            logger.info( "num : {}, id : {}, pwd : {}"
            		   , user.get( "num" )
            		   , user.get( "id"  )
            		   , user.get( "pwd" )
            		   );
            
            count++;
        }		
        
        result.append( "]" );
          
        System.out.println( "result : " + result.toString() );
        
        logger.info( "result : {}", result.toString() );
		
		return result.toString();		
	}

	public String insertUser( String id
                            , String pwd
                            )
	{
		UserDto user = new UserDto();
		
		user.setId ( id  );
		user.setPwd( pwd );
		
		// Dto -> Int
		Integer result = userDao.insertUser( user );
				
		System.out.println( "result : " + result.toString() );
		
		logger.info( "result : {}", result.toString() );
		
		return result.toString();		
	}
	
	public String updatetUser( Integer num
			                 , String  id
			                 , String  pwd
			                 )
	{
		Map< String, Object > user = new HashMap< String, Object >();

		user.put( "num", num );
		user.put( "id" , id  );
		user.put( "pwd", pwd );
		
		// Map -> Int
		Integer result = userDao.updateUser( user );

		System.out.println( "result : " + result.toString() );

		logger.info( "result : {}", result.toString() );
		
		return result.toString();
	}		
	
	public String deletetUser( Integer num )
	{
		// Int -> Int
		Integer result = userDao.deleteUser( num );
	
		System.out.println( "result : " + result.toString() );
		
		logger.info( "result : {}", result.toString() );

		return result.toString();
	}		
}
