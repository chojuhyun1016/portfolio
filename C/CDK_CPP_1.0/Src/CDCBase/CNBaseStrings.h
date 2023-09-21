#ifndef __CN_BASE_STRINGS_H__
#define __CN_BASE_STRINGS_H__

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <strings.h>
#include <sys/time.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifndef CN_BASE_STRING_BUFFER_SIZE
#define CN_BASE_STRING_BUFFER_SIZE		256
#endif

void	LeftTrim( char* _cpString );
void	LeftTrimToBuffer( char* _cpBuffer, char* _cpString );

void	RightTrim( char* _cpString );
void	RightTrimToBuffer( char* _cpBuffer, char* _cpString );

void	Trim( char* _cpString );
void	TrimToBuffer( char* _cpBuffer, char* _cpString );

char*	SubStr( char* _cpString, int _iStart, int _iLength );
char*	SubStrToBuffer( char* _cpBuffer, char* _cpString, int _iStart, int _iLength );

char*	UpStr( char* _cpString );
char*	UpStrToBuffer( char* _cpBuffer, char* _cpString );

char*	DownStr( char* _cpString );
char*	DownStrToBuffer( char* _cpBuffer, char* _cpString );

long	StrToDec( char* _cpHex );

#ifdef __cplusplus
}
#endif

#endif

