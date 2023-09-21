package com.example.board;


import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class BoardService
{
    @Autowired
    private BoardMapper boardMapper;

    
    public List<Board> board()
    {
        List<Board> list = boardMapper.selectBoard();

        return list;
    }

    public List<Board> boardFromObject( Board param )
    {
        List<Board> list = boardMapper.selectBoardFromObject( param );

        return list;
    }
    
    public List<Board> boardFromMap( Map<String, Object> param )
    {
        List<Board> list = boardMapper.selectBoardFromMap( param );

        return list;
    }    
}
