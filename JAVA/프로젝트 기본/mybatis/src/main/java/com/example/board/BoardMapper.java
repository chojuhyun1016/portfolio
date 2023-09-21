package com.example.board;


import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface BoardMapper
{
    List<Board> selectBoard();
    
    List<Board> selectBoardFromObject( Board param );
    
    List<Board> selectBoardFromMap( Map<String, Object> param );
}
