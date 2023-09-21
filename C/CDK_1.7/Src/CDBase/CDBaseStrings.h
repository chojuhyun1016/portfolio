#ifndef __CD_BASE_STRINGS_H__
#define __CD_BASE_STRINGS_H__


#ifdef __cplusplus
extern "C" {
#endif

#ifndef CD_BASE_STRING_BUFFER_SIZE
#define CD_BASE_STRING_BUFFER_SIZE      256
#endif


void    CDBaseStrLeftTrim( char* _cpString );
void    CDBaseStrLeftTrimToBuffer( char* _cpBuffer, char* _cpString );

void    CDBaseStrRightTrim( char* _cpString );
void    CDBaseStrRightTrimToBuffer( char* _cpBuffer, char* _cpString );

void    CDBaseStrTrim( char* _cpString );
void    CDBaseStrTrimToBuffer( char* _cpBuffer, char* _cpString );

char*   CDBaseStrSub( char* _cpString, int _iStart, int _iLength );
char*   CDBaseStrSubToBuffer( char* _cpBuffer, char* _cpString, int _iStart, int _iLength );

char*   CDBaseStrUp( char* _cpString );
char*   CDBaseStrUpToBuffer( char* _cpBuffer, char* _cpString );

char*   CDBaseStrDown( char* _cpString );
char*   CDBaseStrDownToBuffer( char* _cpBuffer, char* _cpString );

long    CDBaseStrToDec( char* _cpHex );


#ifdef __cplusplus
}
#endif

#endif

