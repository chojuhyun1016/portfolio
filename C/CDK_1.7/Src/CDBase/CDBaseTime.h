#ifndef __CD_BASE_TIME_H__
#define __CD_BASE_TIME_H__

#ifdef  _SOLARIS_
    #include <time.h>
#elif _CENT_OS_   
    #include <time.h>
#else
    #include <time.h>
#endif

#ifdef __cplusplus
extern "C" {
#endif

char*	CDBaseGetNowTimeStr( char* _cpBuffer );
char*	CDBaseGetNowTimeFormatStr( char* _cpBuffer, char* _cpFormat );

char*	CDBaseTimeToFormatStr( char* _cpBuffer, time_t _iTime, char* _cpFormat );
time_t	CDBaseFormatStrToTime( char* _cpString, char* _cpFormat );


#ifdef __cplusplus
}
#endif

#endif

