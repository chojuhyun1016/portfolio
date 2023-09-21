#ifndef __CN_BASE_TIME_H__
#define __CN_BASE_TIME_H__


#ifdef __cplusplus
extern "C" {
#endif

char*	GetNowTimeStr( char* _cpBuffer );
char*	GetNowTimeFormatStr( char* _cpBuffer, char* _cpFormat );

char*	TimeToStr( char* _cpBuffer, time_t _iTime, char* _cpFormat );
time_t	StrToTime( char* _cpString, char* _cpFormat );

#ifdef __cplusplus
}
#endif

#endif

