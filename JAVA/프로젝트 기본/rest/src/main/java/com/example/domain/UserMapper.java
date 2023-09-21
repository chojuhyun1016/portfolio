package com.example.domain;


import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface UserMapper
{
    List<Map< String, Object >> selectUser( Integer num );
    
    Integer insertUser( UserDto user );
    
    Integer updateUser( Map< String, Object > user );
    
    Integer deleteUser( Integer num );
}
