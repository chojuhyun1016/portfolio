#include "CDLogApi.h"
#include "CDLogWrite.h"

#ifdef  _SOLARIS_
    #include <time.h>
    #include <stdio.h>
    #include <errno.h>
    #include <fcntl.h>
    #include <stdlib.h>
    #include <unistd.h>
    #include <strings.h>
    #include <sys/varargs.h>
#elif _CENT_OS_
    #include <time.h>
    #include <stdio.h>
    #include <errno.h>
    #include <fcntl.h>
    #include <stdlib.h>
    #include <unistd.h>
    #include <string.h>
    #include <stdarg.h>
    #include <sys/time.h>
#else
    #include <time.h>
    #include <stdio.h>
    #include <errno.h>
    #include <fcntl.h>
    #include <stdlib.h>
    #include <unistd.h>
    #include <strings.h>
    #include <sys/varargs.h>
#endif


/*******************************************************************************
* Update    : 2010/12/21                                                       *
* Argument  : int, int, char*, char*                                           *
*             _iType  : 로그타입으로 로그파일을 시간단위로 생성할지 일단위로   *
*                       생성 할지를 결정                                       *
*             _iLevel : 로그레벨로 로그데이터를 얼마나 디테일하게 찍을지를 결정*
*             _cpPath : 로그파일의 경로                                        *
*             _cpFirstName : 로그파일명                                        *
*                                                                              *
* Return    : CD_LOG_HANDLE, 성공(CD_LOG_HANDLE), 실패(NULL)                   *
* Stability : MT-Safe                                                          *
* Explain   : Heap 메모리를 할당해서 로그와 관련 된 데이터를 등록한다.         *
*             인자값만 제대로 넘어온다면 특별히 실패 할 부분은 없다.           *
*             Async-Safe 가 아니므로 시그널에 주의 해서 사용하라.              *
*             성공 할 경우 로그와 관련된 데이터 구조체의 포인터(CD_LOG_HANDLE) *
*             를 반환한다. 로그데이터를 쓸 경우 이 구조체의 포인터가 Key 값이  *
*             되므로 저장을 잘 해뒀다가 다시 사용하라.                         *
*             다 쓰거나 쓰레드를 종료 할 경우 메로리릭을 방지하기 위해 꼭      *
*             CDLogDeleteHandle() 함수로 메모리를 해제해줘라.                  *
*******************************************************************************/
CD_LOG_HANDLE CDLogGetHandle( int _iType, int _iLevel, char* _cpPath, char* _cpFirstName, char* _cpLastName )
{
    int         iLen;

    CD_LOG_HANDLE   pHandle;

    pHandle = (CD_LOG_HANDLE)malloc( sizeof( stCDLogHandle ) );

    if( !pHandle )
        return  NULL;

    memset( pHandle, 0x00, sizeof( stCDLogHandle ) );

    pHandle->iLogType = _iType;
    pHandle->iLogLevel = _iLevel;

    if( _cpPath == NULL )
    {
        pHandle->cpLogPath  = (char*)malloc( sizeof( char ) );
        if( !pHandle->cpLogPath )
        {
            CDLogDeleteHandle( pHandle );

            return  NULL;
        }

        pHandle->cpLogPath[0] = 0x00;
    }
    else
    {
        iLen = strlen( _cpPath );

        pHandle->cpLogPath = (char*)malloc( iLen + 1 );
        if( !pHandle->cpLogPath )
        {
            CDLogDeleteHandle( pHandle );

            return  NULL;
        }

        memset( pHandle->cpLogPath, 0x00, iLen + 1 );
        memcpy( pHandle->cpLogPath, _cpPath, iLen );
    }

    if( _cpFirstName == NULL )
    {
        pHandle->cpLogFirstName = (char*)malloc( sizeof( char ) );
        if( !pHandle->cpLogFirstName )
        {
            CDLogDeleteHandle( pHandle );

            return  NULL;
        }

        pHandle->cpLogFirstName[0]  = 0x00;
    }
    else
    {
        iLen = strlen( _cpFirstName );

        pHandle->cpLogFirstName = (char*)malloc( iLen + 1 );
        if( !pHandle->cpLogFirstName )
        {
            CDLogDeleteHandle( pHandle );

            return  NULL;
        }

        memset( pHandle->cpLogFirstName, 0x00, iLen + 1 );
        memcpy( pHandle->cpLogFirstName, _cpFirstName, iLen );
    }

    if( _cpLastName == NULL )
    {
        pHandle->cpLogLastName = (char*)malloc( sizeof( char ) );
        if( !pHandle->cpLogLastName )
        {
            CDLogDeleteHandle( pHandle );

            return  NULL;
        }

        pHandle->cpLogLastName[0]   = 0x00;
    }
    else
    {
        iLen = strlen( _cpLastName );

        pHandle->cpLogLastName = (char*)malloc( iLen + 1 );
        if( !pHandle->cpLogLastName )
        {
            CDLogDeleteHandle( pHandle );

            return  NULL;
        }

        memset( pHandle->cpLogLastName, 0x00, iLen + 1 );
        memcpy( pHandle->cpLogLastName, _cpLastName, iLen );
    }

    return  pHandle;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : CD_LOG_HANDLE                                                    *
*             _stpHandle : 로그데이터 구조체의 포인터                          *
*                                                                              *
* Return    : void                                                             *
* Stability : MT-Safe                                                          *
* Explain   : Heap 영역에 할당 된 데이터(로그 데이터 구조체) 를 해제한다.      *
*             특별히 이상 동작 할 부분은 없다.                                 *
*******************************************************************************/
void CDLogDeleteHandle( CD_LOG_HANDLE _stpHandle )
{
    if( !_stpHandle )
        return;

    if( _stpHandle->cpLogPath )
        free( _stpHandle->cpLogPath );

    _stpHandle->cpLogPath = NULL;

    if( _stpHandle->cpLogFirstName )
        free( _stpHandle->cpLogFirstName );

    _stpHandle->cpLogFirstName = NULL;

    if( _stpHandle->cpLogLastName )
        free( _stpHandle->cpLogLastName );

    _stpHandle->cpLogLastName = NULL;

    free( _stpHandle );

    _stpHandle = NULL;

    return;
}


/*******************************************************************************
* Update    : 2012/10/04                                                       *
* Argument  : CD_LOG_HANDLE, int, 가변인자(char* ...)                          *
*             _stpHandle : 로그데이터 구조체의 포인터                          *
*             _iLevel    : 해당 로그데이터의 레벨, _stpHandle 구조체 안의      *
*                          레벨보다 클 경우 로그데이터를 출력하지 않는다.      *
*             _cpFormat  : 로그 문자열이 가변인자와 함께 넘어온다.             *
*                                                                              *
* Return    : void                                                             *
* Stability : MT-Safe                                                          *
* Explain   : 로그 데이터 생성하고 파일에 출력하는 함수이다.                   *
*             가변인자를 받아들이며 받은 문자열 앞에 시분초[hh:mm:ss]          *
*             문자열을 더해서 파일에 출력한다.                                 *
*             출력 파일에 대한 정보는 인자로 받은 포인터(_stpHandle)           *
*             에 저장되어 있으며 그 정보를 바탕으로 로그 데이터를 출력한다.    *
*             로그파일 이름 생성부와 출력부로 나누어진다.                      *
*******************************************************************************/
void CDLog( CD_LOG_HANDLE _stpHandle, int _iLevel, const char* _cpFormat, ... )
{
    char            caLog[CD_LOG_BUF_SIZE];
    char            caFile[CD_LOG_FILE_NAME_BUF_SIZE];

    struct timeval  stTvTime;
    struct tm       stTmTime;

    va_list         vaArgs;

    if( _stpHandle->iLogLevel < _iLevel )
        return;

    gettimeofday( &stTvTime, NULL );
    localtime_r( &stTvTime.tv_sec, &stTmTime );

    {
        sprintf( caLog, "[%02d:%02d:%02d] ", stTmTime.tm_hour, stTmTime.tm_min, stTmTime.tm_sec );

        va_start( vaArgs, _cpFormat );

        if( vsnprintf( caLog + 11, CD_LOG_BUF_SIZE - 11, _cpFormat, vaArgs ) < 0 )
        {
            fprintf( stderr, "[vsnprintf() Error][E:%d][F:%s][L:%d]\n",
                errno,
                __FILE__,
                __LINE__ );

            return;
        }

        va_end( vaArgs );
    }

    if( CDLogMakeFileName( caFile, CD_LOG_FILE_NAME_BUF_SIZE, _stpHandle, &stTmTime ) == CD_LOG_ERROR )
    {
        fprintf( stderr, "[MakeFileName( %s %d %d %d ) Error][E:%d][F:%s][L:%d]\n",
            caFile,
            CD_LOG_FILE_NAME_BUF_SIZE,
            _stpHandle,
            &stTmTime,
            errno,
            __FILE__,
            __LINE__ );

        return;
    }

    if( CDLogWrite( caFile, caLog ) == CD_LOG_ERROR )
    {
        fprintf( stderr, "[WriteLog( %s %s ) Error][E:%d][F:%s][L:%d]\n",
            caFile,
            caLog,
            errno,
            __FILE__,
            __LINE__ );

        return;
    }
}


/*******************************************************************************
* Update    : 2012/10/31                                                       *
* Argument  : CD_LOG_HANDLE, char*, struct tm*                                 *
*             _cpBuf     : 파일명이 들어갈 Buffer                              *
*             _iBufSize  : 파일명 Buffer의 크기                                *
*             _stpHandle : 로그데이터 구조체의 포인터                          *
*             _stTmTime  : 현재 시간이 담겨있는 구조체의 포인터                *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : 로그를 출력 할 파일의 이름을 완성한다.                           *
*             로그 정보 구조체 포인터(CD_LOG_HANDLE _stpHandle)를 통해         *
*             로그파일 경로 + 파일 이름 + 현재시간 으로 파일 이름을 생성한다.  *
*             로그구조체의 로그타입(_stpHandle->iLogType) 에따라 파일 타입이   *
*             두가지(일로그, 시간로그) 파일 이름으로 생성된다.                 *
*******************************************************************************/
static int CDLogMakeFileName( char* _cpBuf, int _iBufSize, CD_LOG_HANDLE _stpHandle, struct tm* _stTmTime )
{
    switch( _stpHandle->iLogType )
    {
        case CD_LOG_HOUR_TYPE :
            snprintf( _cpBuf, _iBufSize, "%s/%s.%04d%02d%02d%02d%s.log",
                _stpHandle->cpLogPath,
                _stpHandle->cpLogFirstName,
                _stTmTime->tm_year + 1900,
                _stTmTime->tm_mon + 1,
                _stTmTime->tm_mday,
                _stTmTime->tm_hour,
                _stpHandle->cpLogLastName );

            break;

        case CD_LOG_DAY_TYPE :
            snprintf( _cpBuf, _iBufSize, "%s/%s.%04d%02d%02d%s.log",
                _stpHandle->cpLogPath,
                _stpHandle->cpLogFirstName,
                _stTmTime->tm_year + 1900,
                _stTmTime->tm_mon + 1,
                _stTmTime->tm_mday,
                _stpHandle->cpLogLastName );

            break;

        default :
            snprintf( _cpBuf, _iBufSize, "%s/%s.%04d%02d%02d%s.log",
                _stpHandle->cpLogPath,
                _stpHandle->cpLogFirstName,
                _stTmTime->tm_year + 1900,
                _stTmTime->tm_mon + 1,
                _stTmTime->tm_mday,
                _stpHandle->cpLogLastName );

            break;
    }

    return  CD_LOG_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : char*, char*                                                     *
*             _cpFile : 출력 할 파일의 이름(파일 경로 + 파일 이름)문자열       *
*             _cpBuf  : 파일에 출력 될 데이터 문자열                           *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : 완성 된 로그파일(char* _cpFile)에 로그데이터(char* _cpBuf)       *
*             를 출력한다.                                                     *
*             예외 및 안정성을 위해 파일은 출력하기전에 열고 출력 후 곧바로    *
*             닫는다.                                                          *
*******************************************************************************/
static int CDLogWrite( char* _cpFile, char* _cpBuf )
{
    int iFd;

    if( ( iFd = CDLogFileOpen( _cpFile, O_WRONLY | O_CREAT | O_APPEND  | O_NONBLOCK ) ) == CD_LOG_ERROR )
    {
        fprintf( stderr, "[CDLogFileOpen( %s, %d ) Error][R:%d][E:%d][F:%s][L:%d]\n",
            _cpFile,
            O_WRONLY | O_CREAT | O_APPEND  | O_NONBLOCK,
            iFd,
            errno,
            __FILE__,
            __LINE__ );

        return  CD_LOG_ERROR;
    }

    write( iFd, _cpBuf, strlen( _cpBuf ) );

    CDLogFileClose( iFd );

    return  CD_LOG_SUCCESS;
}

