#ifndef __CD_LOG_WRITE_LOG_H__
#define __CD_LOG_WRITE_LOG_H__

#include "CDLogDefinitions.h"

#ifdef _SOLARIS_
    #include <time.h>
    #include <sys/varargs.h>
#elif _CENT_OS_
    #include <time.h>
    #include <stdarg.h>
#else
    #include <time.h>
    #include <sys/varargs.h>
#endif


#ifdef  __cplusplus
extern "C"
{
#endif

#ifndef CD_LOG_BUF_SIZE
#define CD_LOG_BUF_SIZE             4096
#endif

#ifndef CD_LOG_FILE_NAME_BUF_SIZE
#define CD_LOG_FILE_NAME_BUF_SIZE   512
#endif

#ifndef CD_LOG_HOUR_TYPE
#define CD_LOG_HOUR_TYPE            1
#endif

#ifndef CD_LOG_DAY_TYPE
#define CD_LOG_DAY_TYPE             2
#endif


typedef struct _stCDLogHandle
{
    int     iLogType;
    int     iLogLevel;

    char*   cpLogPath;
    char*   cpLogFirstName;
    char*   cpLogLastName;

} stCDLogHandle;


#ifndef     CD_LOG_HANDLE
typedef stCDLogHandle*  CD_LOG_HANDLE;
#endif


stCDLogHandle*  CDLogGetHandle( int _iType, int _iLevel, char* _cpPath, char* _cpFirstName, char* _cpLastName );
void            CDLogDeleteHandle( CD_LOG_HANDLE _stpHandle );

void            CDLog( CD_LOG_HANDLE _stpHandle, int _iLevel, const char* cpFormat, ... );

static int      CDLogMakeFileName( char* _cpBuf, int _iBufSize, CD_LOG_HANDLE _stpHandle, struct tm* _stTmTime );
static int      CDLogWrite( char* _cpFile, char* _cpBuf );


#ifdef  __cplusplus
}
#endif

#endif

