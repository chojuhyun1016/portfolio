#include <errno.h>
#include <fcntl.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "CNBaseReStart.h"
#include "CNBaseDefinitions.h"


/*******************************************************************************
* Update    : 2011/05/04                                                       *
* Argument  : char, int                                                        *
*             _cpFile : 열 파일의 경로/파일이름                                *
*             _iMode  : 파일열기 세부옵션                                      *
*                                                                              *
* Return    : int, 성공(파일의 디스크립터), 실패(-1)                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 넘겨받은 파일명(_cpFile)을 넘겨받은 모드(_iMode)로 열고          *
*             디스크립터를 반환한다. 시그널에의해 인터럽트 되었을 경우 루프를  *
*             돌면서 파일열기를 재시도한다. 인터럽트 외에 원인으로 실패를      *
*             한다면 루프를 돌지않고 실패값(-1)으로 반환한다.                  *
*******************************************************************************/
int r_open( const char* _cpFile, int _iMode )
{
	int	iResult;
	
	while( ( iResult = open( _cpFile, _iMode ) ) == CN_BASE_ERROR && errno == EINTR );
	
	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : int                                                              *
*             _iFd : 닫고자 하는 파일의 디스크립터                             *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 넘겨받은 디스크립터의 파일을 닫는다.                             *
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 파일닫기를       *
*             재시도한다. 인터럽트 외에 원인으로 실패를 한다면 루프를 돌지않고 *
*             실패값(-1)으로 반환한다.                                         *
*******************************************************************************/
int r_close( int _iFd )
{
	int	iResult;

	while( ( iResult = close( _iFd ) ) == CN_BASE_ERROR && errno == EINTR );

	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : char*, char*                                                     *
*             _cpFile : 열 파일의 경로/파일이름                                *
*             _cpMode : 파일열기 세부옵션                                      *
*                                                                              *
* Return    : int, 성공(파일포인터), 실패(NULL)                                *
* Stability : MT-Safe                                                          *
* Explain   : 넘겨받은 파일명(_cpFile)을 넘겨받은 모드(_cpMode)로 열고         *
*             파일포인터를 반환한다. 시그널에의해 인터럽트 되었을 경우 루프를  *
*             돌면서 파일열기를 재시도한다. 인터럽트 외에 원인으로 실패를      *
*             한다면 루프를 돌지않고 실패값(NULL)으로 반환한다.                *
*******************************************************************************/
FILE* r_fopen( const char* _cpFile, char* _cpMode )
{
	FILE	*fpFile;
	
	while( ( fpFile = fopen( _cpFile, _cpMode ) ) == NULL && errno == EINTR );
	
	return	fpFile;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : FILE*                                                            *
*             _fpFile : 닫고자 하는 파일의 파일포인터                          *
*                                                                              *
* Return    : int, 성공(0), 실패(EOF)                                          *
* Stability : MT-Safe                                                          *
* Explain   : 넘겨받은 파일포인터의 파일을 닫는다.                             *
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 파일닫기를       *
*             재시도한다. 인터럽트 외에 원인으로 실패를 한다면 루프를 돌지않고 *
*             실패값(EOF)으로 반환한다.                                        *
*******************************************************************************/
int r_fclose( FILE* _fpFile )
{
	int	iResult;

	while( ( iResult = fclose( _fpFile ) ) != CN_BASE_SUCCESS && errno == EINTR );

	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : void*, size_t, size_t, FILE*                                     *
*             _vpBuf  : 읽어들인 데이터를 저장할 버퍼의 주소                   *
*             _iSize  : 읽어들일 데이터 한 Row의 크기                          *
*             _iCnt   : 읽어들일 데이터의 Row 개수                             *
*             _fpFile : 데이터를 읽어들일 파일의 파일포인터                    *
*                                                                              *
* Return    : int, 성공(읽어들인 개수), 실패(0)                                *
* Stability : MT-Safe                                                          *
* Explain   : 파일(_fpFile)로부터 _iSize크기의 데이터를 _iCnt개수 만큼 읽어들여*
*             버퍼(_vpBuf)에 저장한다.                                         *
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 _iCnt개수만큼    *
*             읽어들일때 까지 게더링을 진행한다. 시그널에 의한 인터럽트가      *
*             아니거나 에러일 경우 읽어들인 개수를 반환한다.                   *
*******************************************************************************/
size_t r_fread( void* _vpBuf, size_t _iSize,  size_t _iCnt, FILE* _fpFile )
{
	size_t	iResult;

	size_t	iCnt = _iCnt;
	size_t	iOffset = 0;

	while( ( iResult = fread( (char*)_vpBuf + iOffset, _iSize, iCnt, _fpFile ) ) != iCnt && errno == EINTR )
	{
		iCnt -= iResult;
		iOffset = ( _iCnt - iCnt ) * _iSize;
	}

	return	_iCnt - iCnt;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : void*, size_t, size_t, FILE*                                     *
*             _vpBuf  : 출력 할 데이터가 저장된 버퍼의 주소                    *
*             _iSize  : 출력 할 데이터 한 Row의 크기                           *
*             _iCnt   : 출력 할 데이터의 Row 개수                              *
*             _fpFile : 데이터를 출력 할 파일의 파일포인터                     *
*                                                                              *
* Return    : int, 성공(읽어들인 개수), 실패(EOF)                              *
* Stability : MT-Safe                                                          *
* Explain   : 버퍼(_vpBuf)의 _iSize크기의 데이터를 _iCnt 개수만큼              *
*             파일(_fpFile)에 출력한다.                                        *
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 _iCnt개수만큼    *
*             쓸때까지 쓰기를 반복한다. 시그널에 의한 인터럽트가 아니거나      *
*             에러일 경우 쓰여진 개수를 반환한다.                              *
*******************************************************************************/
size_t r_fwrite( void* _vpBuf, size_t _iSize,  size_t _iCnt, FILE* _fpFile )
{
	size_t	iResult;

	size_t	iCnt = _iCnt;
	size_t	iOffset = 0;

	while( ( iResult = fwrite( (char*)_vpBuf + iOffset, _iSize, iCnt, _fpFile ) ) != iCnt && errno == EINTR )
	{
		iCnt -= iResult;
		iOffset = ( _iCnt - iCnt ) * _iSize;
	}

	return	_iCnt - iCnt;
}


/*******************************************************************************
* Update    : 2011/05/04                                                       *
* Argument  : char*, int, FILE*                                                *
*             _cpBuf  : 읽어들인 데이터가 저장 될 버퍼의 주소                  *
*             _iSize  : 읽어들일 데이터의 크기                                 *
*             _fpFile : 읽어들일 파일의 파일 디스크립터                        *
*                                                                              *
* Return    : int, 성공(읽어들인 개수), 실패(EOF)                              *
* Stability : MT-Safe                                                          *
* Explain   : 파일(_fpFile)에서 최대 _iSize크기까지의 데이터를 읽어들여서      *
*             버퍼(_cpBuf)에 저장한다. fgets는 파일에서 한라인('\n')을 만날때  *
*             까지 읽어들이며 _iSize는 최대 읽어들이는 한라인값의 제한         *
*             크기이다. 버퍼(_cpBuf)의 크기보다 한라인의 크기가 더 길경우      *
*             메모리 오버플로우로 인한 메모리 침범을 막기위해서이다.           *
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 읽기(fgets)를    *
*             반복한다. 시그널에 의한 인터럽트가 아니거나 에러일 경우 NULL을   *
*             반환한다.                                                        *
*******************************************************************************/
char* r_fgets( char* _cpBuf, int _iSize, FILE* _fpFile )
{
	char*	cpResult;

	while( ( cpResult = fgets( _cpBuf, _iSize, _fpFile ) ) == NULL && errno == EINTR );

	return	cpResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : int                                                              *
*             _iFd  : 복사 할 파일디스크립터                                   *
*                                                                              *
* Return    : int, 성공(복사 된 디스크립터), 실패(-1)                          *
* Stability : Async-Signal-Safe                                                *
* Explain   : 넘겨받은 디스크립터(_iFd)를 복사하고 복사 한 디스크립터를        *
*             반환한다.                                                        *
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 복사가 될때까지  *
*             재시도한다.                                                      *
*******************************************************************************/
int r_dup( int _iFd )
{
	int	iResult;
	
	while( ( iResult = dup( _iFd ) ) == CN_BASE_ERROR && errno == EINTR );
	
	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : int, int                                                         *
*             _iFd1  : 재지향을 받아들일 디스크립터                            *
*             _iFd2  : 재지향 할 디스크립터                                    *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 넘겨받은 디스크립터(_iFd2)를 _iFd1으로 재지향 한다.              *
*             재지향 이란 디스크립터를 다른 디스크립터로 돌리는 것으로 재지향  *
*             후 만약 _iFd2에 입출력을 한다면 _iFd1 에서 입출력이 이루어진다.  *
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 재지향 될때까지  *
*             재시도한다.                                                      *
*******************************************************************************/
int r_dup2( int _iFd1, int _iFd2 )
{
	int	iResult;
	
	while( ( iResult = dup2( _iFd1, _iFd2 ) ) == CN_BASE_ERROR && errno == EINTR );
	
	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : struct pollfd*, nfds_t, int                                      *
*             _stpPoll  : poll 구조체의 주소                                   *
*             _iCnt     : 이벤트를 감시할 디스크립터의 개수                    *
*             _iTimeout : 타임아웃                                             *
*                                                                              *
* Return    : int, 성공(이벤트가 일어난 디스크립터 개수), 실패(-1)             *
* Stability : Async-Signal-Safe                                                *
* Explain   : 넘겨 받은 _stpPoll 데이터를 바탕으로 poll 함수를 실행한다.       *
*             _iCnt개의 파일 디스크립터를 감시하며 _iTimeout만큼 대기한다.     *
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 poll 수행을      *
*             반복한다. 시그널에 의한 인터럽트가 아니거나 에러일 경우 -1을     *
*             반환한다.                                                        *
*******************************************************************************/
int	r_poll( struct  pollfd* _stpPoll, nfds_t _iCnt, int _iTimeout )
{
	int iResult;

	while( ( ( iResult = poll( _stpPoll, _iCnt, _iTimeout ) ) == CN_BASE_ERROR ) && ( errno == EINTR ) );

	return iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : int, struct sockaddr*, nfds_t, socklen_t*                        *
*             _iListen     : accept과정을 수행 할 소켓의 디스크립터            *
*             _stpSockAddr : accept된 소켓의 정보가 저장 될 구조체의 주소      *
*             _ipLen       : _stpSockAddr 구조체의 크기값의 주소               *
*                                                                              *
* Return    : int, 성공(접속된 소켓의 디스크립터), 실패(-1)                    *
* Stability : MT-Safe                                                          *
* Explain   : 넘겨 받은 _iListen 으로 accept 과정(접속요청 수락)을 수행한다.   *
*             _stpSockAddr 구조체에 접속 된 소켓의 정보가 저장된다.            *
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 accept 수행을    *
*             반복한다. 시그널에 의한 인터럽트가 아니거나 에러일 경우 -1을     *
*             반환한다.                                                        *
*******************************************************************************/
int r_accept( int _iListen, struct sockaddr* _stpSockAddr, socklen_t* _ipLen )
{
	int iResult;

	while( ( ( iResult = accept( _iListen, _stpSockAddr, _ipLen ) ) == CN_BASE_ERROR ) && (errno == EINTR) );

	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : const sigset_t*, siginfo_t*                                      *
*             _stpSigSet  : 받아들일 시그널이 셋팅 된 구조체의 주소            *
*             _stpSigInfo : 받아들여진 시그널의 정보가 저장 될 구조체의 주소   *
*                                                                              *
* Return    : int, 성공(시그널 번호), 실패(-1)                                 *
* Stability : MT-Safe                                                          *
* Explain   : _stpSigSet 주소의 구조체에 셋팅 된 시그널이 발생 할 때까지       *
*             기다리고 해당 시그널이 발생 할 경우 siginfo_t 구조체에 시그널에  *
*             대한 정보를 저장한다.                                            *
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 sigwaitinfo      *
*             수행을 반복한다. 시그널에 의한 인터럽트가 아니거나 에러일 경우   *
*             -1을 반환한다.                                                   *
*******************************************************************************/
int	r_sigwaitinfo( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo )
{
	int	iResult;

	while( ( ( iResult = sigwaitinfo( _stpSigSet, _stpSigInfo ) ) == CN_BASE_ERROR ) && ( errno == EINTR ) );

	return	iResult;

}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : const sigset_t*, siginfo_t*, const struct timespec*              *
*             _stpSigSet  : 받아들일 시그널이 셋팅 된 구조체의 주소            *
*             _stpSigInfo : 받아들여진 시그널의 정보가 저장 될 구조체의 주소   *
*             _stpTsTime  : 시그널 대기시간                                    *
*                                                                              *
* Return    : int, 성공(시그널 번호), 실패(-1)                                 *
* Stability : MT-Safe                                                          *
* Explain   : _stpSigSet 주소의 구조체에 셋팅 된 시그널을 _stpTsTime 주소의    *
*             구조체에 정의 된 시간만큼 기다린다.                              *
*             시그널이 발생 할 경우 siginfo_t 구조체에 시그널에 대한 정보를    *
*             저장하고 시그널 번호를 반환한다.                                 *
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 sigwaitinfo      *
*             수행을 반복한다. 시그널에 의한 인터럽트가 아니거나 에러일 경우   *
*             -1을 반환한다. 타임아웃일 경우 -1을 반환하며 errno는 EAGIN으로   *
*             셋팅된다.                                                        *
*******************************************************************************/
int	r_sigtimedwait( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo, const struct timespec* _stpTsTime )
{
	int	iResult;

	while( ( ( iResult = sigtimedwait( _stpSigSet, _stpSigInfo, _stpTsTime ) ) == CN_BASE_ERROR ) && ( errno == EINTR ) );

	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : int, void*, size_t, int                                          *
*             _iQueueID    : 데이터를 전송할 Queue의 ID                        *
*             _vpPacket    : 전송 할 데이터가 저장 된 버퍼                     *
*             _iPacketSize : 전송 할 데이터의 크기                             *
*             _iMsgFlag    : 읽기 세부옵션(봉쇄/비봉쇄)                        *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : _iQueueID에 해당하는 Queue에 버퍼(_vpPacket)의 데이터를          *
*             _iPacketSize 사이즈 만큼 출력한다. _iMsgFlag는 메시지 세부 옵션  *
*             으로써 전송모드를 봉쇄로 할것인지 비봉쇄로 할것인지 결정한다.    * 
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 msgsnd           *
*             수행을 반복한다. 시그널에 의한 인터럽트가 아니거나 에러일 경우   *
*             -1을 반환한다.                                                   *
*******************************************************************************/
int	r_msgsnd( int _iQueueID, void* _vpPacket,  size_t _iPacketSize, int _iMsgFlag )
{
	int	iResult;

	while( ( iResult = msgsnd( _iQueueID, _vpPacket, _iPacketSize, _iMsgFlag ) ) == CN_BASE_ERROR && errno == EINTR );

	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : int, void*, size_t, long, int                                    *
*             _iQueueID    : 데이터를 읽어들일 Queue의 ID                      *
*             _vpPacket    : 읽어들인 데이터를 저장 할 버퍼                    *
*             _iPacketSize : 읽어들인 데이터를 저장 할 버퍼의 크기             *
*             _iMsgType    : 읽어들일 메시지의 타입                            *
*             _iMsgFlag    : 읽기 세부옵션(봉쇄/비봉쇄)                        *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : _iQueueID에 해당하는 Queue에서 _iMsgType타입의 데이터를 읽어서   *
*             버퍼(_vpPacket)에 저장한다. _iPacketSize는 버퍼(_vpPacket)의     *
*             크기이며 버퍼의 크기는 항상 읽어들일 데이터보다 커야한다.        *
*             _iMsgFlag는 메시지 세부 옵션 으로써 전송모드를 봉쇄로 할것인지   *
*             비봉쇄로 할것인지 결정한다.                                      * 
*             시그널에의해 인터럽트 되었을 경우 루프를 돌면서 msgrcv           *
*             수행을 반복한다. 시그널에 의한 인터럽트가 아니거나 에러일 경우   *
*             -1을 반환한다.                                                   *
*******************************************************************************/
int	r_msgrcv( int _iQueueID, void* _vpPacket,  size_t _iPacketSize, long _iMsgType, int _iMsgFlag )
{
	int	iResult;

	while( ( iResult = msgrcv( _iQueueID, _vpPacket, _iPacketSize, _iMsgType, _iMsgFlag ) ) == CN_BASE_ERROR && errno == EINTR );

	return	iResult;
}

