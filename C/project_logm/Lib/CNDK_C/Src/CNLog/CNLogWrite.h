#ifndef __CN_WRITE_LOG_H__
#define __CN_WRITE_LOG_H__

#include <sys/varargs.h>

#include "CNLogDefinitions.h"

#ifdef  __cplusplus
extern "C"
{
#endif

#ifndef	CN_LOG_BUF_SIZE
#define	CN_LOG_BUF_SIZE				1024
#endif

#ifndef	CN_LOG_FILE_NAME_BUF_SIZE
#define	CN_LOG_FILE_NAME_BUF_SIZE	512
#endif

#ifndef	CN_LOG_HOUR_TYPE
#define	CN_LOG_HOUR_TYPE		1
#endif

#ifndef	CN_LOG_DAY_TYPE
#define	CN_LOG_DAY_TYPE			2
#endif


typedef struct _stLogHandle
{
	int		iLogType;
	int		iLogLevel;

	char*	cpLogPath;
	char*	cpLogFirstName;
	char*	cpLogLastName;

} stLogHandle, *stpLogHandle;

#ifndef		CN_LOG_HANDLE
typedef	stpLogHandle	CN_LOG_HANDLE;
#endif


stLogHandle*	CNGetLogHandle( int _iType, int _iLevel, char* _cpPath, char* _cpFirstName, char* _cpLastName );
void			CNDeleteHandle( CN_LOG_HANDLE _stpHandle );
void			CNLOG( CN_LOG_HANDLE _stpHandle, int _iLevel, const char* cpFormat, ... );
static int		CNMakeFileName( char* _cpBuf, int _iBufSize, CN_LOG_HANDLE _stpHandle, struct tm* _stTmTime );
static int		CNWriteLog( char* _cpFile, char* _cpBuf );

#ifdef  __cplusplus
}
#endif

#endif

