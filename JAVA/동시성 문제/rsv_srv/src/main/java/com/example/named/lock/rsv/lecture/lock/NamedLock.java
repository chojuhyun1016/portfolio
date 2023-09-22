package com.example.named.lock.rsv.lecture.lock;


import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

import com.example.named.lock.rsv.lecture.exception.UserLockException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;


@Slf4j
public class NamedLock
{
    private static final String GET_LOCK     = "SELECT GET_LOCK(?, ?)";
    private static final String RELEASE_LOCK = "SELECT RELEASE_LOCK(?)";

    private final DataSource dataSource;


    public NamedLock(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public <T> T executeWithLock(String lockName
                               , int timeout
                               , Supplier<T> supplier
                                )
    {
        try (Connection connection = dataSource.getConnection())
        {
            try
            {
                getLock(connection, lockName, timeout);

                return supplier.get();

            }
            finally
            {
                releaseLock(connection, lockName);
            }
        }
    	catch (SQLException e)
    	{
    		log.error(" \"method\": \"{}\", \"msg\" : \"{}\" \"state\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage(), e.getSQLState());
    		
    		throw new RuntimeException(e.getMessage(), e);
    	}
    	catch (Exception e)
    	{
    		log.error(" \"method\": \"{}\", \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage());
    		
    		throw e;
    	}
    }

    private void getLock(Connection connection
                       , String lockName
                       , int timeout
                        )
    {
    	try
    	{
	        PreparedStatement preparedStatement = connection.prepareStatement(GET_LOCK);
	
	        preparedStatement.setString(1, lockName);
	        preparedStatement.setInt(2, timeout);
	
	        checkResultSet(preparedStatement);
    	}
    	catch (SQLException e)
    	{
    		log.error(" \"method\": \"{}\", \"msg\" : \"{}\" \"state\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage(), e.getSQLState());
    		
    		throw new RuntimeException(e.getMessage(), e);
    	}
    	catch (Exception e)
    	{
    		log.error(" \"method\": \"{}\", \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage());
    		
    		throw e;
    	}
    }

    private void releaseLock(Connection connection, String lockName)
    {
    	try
    	{
	        PreparedStatement preparedStatement = connection.prepareStatement(RELEASE_LOCK);
	
	        preparedStatement.setString(1, lockName);
	
	        checkResultSet(preparedStatement);
    	}
    	catch (SQLException e)
    	{
    		log.error(" \"method\": \"{}\", \"msg\" : \"{}\" \"state\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage(), e.getSQLState());
    		
    		throw new RuntimeException(e.getMessage(), e);
    	}
    	catch (Exception e)
    	{
    		log.error(" \"method\": \"{}\", \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage());
    		
    		throw e;
    	}
    }

    private void checkResultSet(PreparedStatement preparedStatement)
    {
    	try
    	{
	        ResultSet resultSet = preparedStatement.executeQuery();
	
	        if (!resultSet.next())
	        {
	            throw new UserLockException((new UserLockException()).getMessage() + " result : [null]");
	        }
	
	        int result = resultSet.getInt(1);
	
	        if (result != 1)
	        {
	            throw new UserLockException((new UserLockException()).getMessage() + " result : [" + result + "]");
	        }
    	}
    	catch (SQLException e)
    	{
    		log.error(" \"method\": \"{}\", \"msg\" : \"{}\" \"state\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage(), e.getSQLState());
    		
    		throw new RuntimeException(e.getMessage(), e);
    	}
    	catch (UserLockException e)
    	{
    		log.error(" \"method\": \"{}\", \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage());
    		
    		throw new RuntimeException(e.getMessage(), e);
    	}
    	catch (Exception e)
    	{
    		log.error(" \"method\": \"{}\", \"msg\" : \"{}\" ", new Object() {}.getClass().getEnclosingMethod(), e.getMessage());
    		
    		throw e;
    	}
    }
}
