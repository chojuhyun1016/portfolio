#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "CNLogApi.h"
#include "CNLogDefinitions.h"


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : char*, int                                                       *
*             _cpFile : 파일 명(파일경로 + 파일이름)                           *
*             _iMode  : 파일 열기 모드                                         *
*                                                                              *
* Return    : int, 성공(File Descriptor), 실패(-1)                             *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : System Call open() 함수를 래핑한것이다.                          *
*             Interrupt의 영향을 받지 않도록 하기위해 open()함수의 리턴값이    *
*             CN_LOG_ERROR(-1) 이면서 errno가 EINTR(interrupt에 의한 중지)     *
*             일 경우 다시 open()을 수행한다.                                  *
*******************************************************************************/
int CNLogFileOpen( const char* _cpFile, int _iMode )
{
	int	iResult;
	
	while( ( iResult = open( _cpFile, _iMode, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH ) ) == CN_LOG_ERROR && errno == EINTR );
	
	return	iResult;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : int                                                              *
*             _iFd : 닫을려는 파일의 파일 디스크립터                           *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : System Call close() 함수를 래핑한것이다.                         *
*             Interrupt의 영향을 받지 않도록 하기위해 close()함수의 리턴값이   *
*             CN_LOG_ERROR(-1) 이면서 errno가 EINTR(interrupt에 의한 중지)     *
*             일 경우 다시 close()를 수행한다.                                 *
*******************************************************************************/
int CNLogFileClose( int _iFd )
{
	int	iResult;

	while( ( iResult = close( _iFd ) ) == CN_LOG_ERROR && errno == EINTR );

	return	iResult;
}

