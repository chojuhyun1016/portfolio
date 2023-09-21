package com.example.board;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;


@Controller
public class BoardController
{    
    @Autowired
    BoardService boardService;
    
    private static final Logger logger = LoggerFactory.getLogger( BoardController.class );

    public void list()
    {   
        List<Board> boardList = boardService.board();
       
        for( Board obj : boardList )
        {
            System.out.println( "id: " + obj.getId() + "title: " + obj.getTitle() );
            logger.info( "id: {} title: {}", obj.getId(), obj.getTitle() );
        }
        
        return;
    }

    public void listFromObject()
    { 
        Board param = new Board();
        
        param.setId( 2 );
        
        List<Board> boardList = boardService.boardFromObject( param );
       
        for( Board obj : boardList )
        {
            System.out.println( "Object id: " + obj.getId() + "Object title: " + obj.getTitle() );
            logger.info( "Object id: {} Object title: {}", obj.getId(), obj.getTitle() );
        }
        
        return;
    }    

    public void listFromMap()
    { 
        Map<String, Object> param = new HashMap<String, Object>();

        param.put( "id", 2 );
        param.put( "title", "제목2" );
        
        List<Board> boardList = boardService.boardFromMap( param );
       
        for( Board obj : boardList )
        {
            System.out.println( "Map id: " + obj.getId() + "Map title: " + obj.getTitle() );
            logger.info( "Map id: {} Map title: {}", obj.getId(), obj.getTitle() );
        }
        
        return;
    }      
}
