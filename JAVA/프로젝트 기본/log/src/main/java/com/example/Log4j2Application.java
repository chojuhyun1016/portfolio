package com.example;


import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;


@SpringBootApplication
public class Log4j2Application implements ApplicationRunner
{
    private static final Logger logger = LoggerFactory.getLogger( Log4j2Application.class );

    public static void main( String[] args )
    {
        SpringApplication.run( Log4j2Application.class, args );

        logger.debug( "Hi I'm {} log", "DEBUG" );
        logger.info ( "Hi I'm {} log", "INFO"  );
        logger.error( "Hi I'm {} log", "ERROR" );
    }

    @Override
    public void run( ApplicationArguments args )
    {
        String[]     sourceArgs    = args.getSourceArgs();
        List<String> nonOptionArgs = args.getNonOptionArgs();
        Set<String>  optionNames   = args.getOptionNames();

        System.out.println( "---원본 args---" );
        logger.info( "---원본 args---" );

        for (String sourceArg : sourceArgs )
        {
            System.out.println( sourceArg );
            logger.info( "{}", sourceArg );
        }

        System.out.println( "---옵션아닌 args---" );
        logger.info( "---옵션아닌 args---" );

        for( String nonOptionArg : nonOptionArgs )
        {
            System.out.println( nonOptionArg );
            logger.info( "{}", nonOptionArg );
        }

        System.out.println( "---옵션 args---" );
        logger.info( "---옵션 args---" );

        for( String optionName : optionNames )
        {
            List<String> optionValues = args.getOptionValues( optionName );

            for( String optionValue : optionValues )
            {
                System.out.println( optionName + ":" + optionValue );
                logger.info( "{} : {}", optionName, optionValue );
            }
        }
    }

    @EventListener( ApplicationReadyEvent.class )
    public void init()
    {
        System.out.println( "EventListener!" );
    }
}
