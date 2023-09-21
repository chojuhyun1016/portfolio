#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "CNSignalApi.h"
#include "CNSignalDefinitions.h"


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, void (*sa_iSigaction)(int, siginfo_t *, void *),            *
*             _iSig            : 등록 할 시그널의 번호                         *
*             _fnHandler       : 등록 한 시그널(_iSig)의 핸들링 함수포인터     *
*             _stpBeforeAction : 기존에 등록 되있던 정보를 저장 할 구조체      *
*                                                                              *
*             struct sigaction*                                                *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 해당 시그널(_iSig) 에 일반 핸들러(_fnHandler) 을 등록한다.       *
*******************************************************************************/
int CNSigAction( int _iSig, CN_SIG_HANDLER _fnHandler, struct sigaction* _stpBeforeAction )
{
	struct sigaction	stAction;

	if( _iSig <= 0 )
		return	CN_SIGNAL_ERROR;

	if( !_fnHandler )
		return	CN_SIGNAL_ERROR;

	memset( &stAction, 0x00, sizeof( stAction ) );

	/* 일반 시그널로 등록 */
	stAction.sa_flags	= SA_RESTART;
	stAction.sa_handler	= _fnHandler;

	if( sigfillset( &stAction.sa_mask ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	if( sigaction( _iSig, &stAction, _stpBeforeAction ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	return	CN_SIGNAL_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, void (RT_iSig_HANDLER)(int, siginfo_t *, void *),           *
*             _iSig            : 등록 할 시그널의 번호                         *
*             _fnHandler       : 등록 한 시그널(_iSig)의 핸들링 함수포인터     *
*             _stpBeforeAction : 기존에 등록 되있던 정보를 저장 할 구조체      *
*                                                                              *
*                                                                              *
*             struct sigaction*                                                *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 해당 시그널(_iSig) 에 일반 리얼타임 시그널 핸들러(_fnHandler)를  *
*             등록한다. 일반 시그널도 리얼타임 시그널 핸들러에서 전부 걸러낼수 *
*             있으므로 이 함수로 등록 하고 사용 하는 것을 추천한다.            *
*******************************************************************************/
int CNRtSigAction( int _iSig, CN_RT_SIG_HANDLER _fnHandler, struct sigaction* _stpBeforeAction )
{
	struct sigaction	stAction;

	if( _iSig <= 0 )
		return	CN_SIGNAL_ERROR;

	if( !_fnHandler )
		return	CN_SIGNAL_ERROR;

	memset( &stAction, 0x00, sizeof( stAction ) );

	/* 확장 된 시그널로 등록(SA_SIGINFO) */
	stAction.sa_flags		= (SA_SIGINFO | SA_RESTART);
	stAction.sa_sigaction	= _fnHandler;

	if( sigfillset( &stAction.sa_mask ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	if( sigaction( _iSig, &stAction, _stpBeforeAction ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	return	CN_SIGNAL_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : int, struct sigaction*                                           *
*             _iSig            : 무시(ignore) 할 시그널의 번호                 *
*             _stpBeforeAction : 기존의 시그널 정보를 저장 할 구조체           *
*                                                                              *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 프로세스 및 쓰레드가 해당 시그널(_iSig)을 무시(ignore) 하도록    *
*             셋팅한다.                                                        *
*******************************************************************************/
int CNSigIgnore( int _iSig, struct sigaction* _stpBeforeAction )
{
	struct sigaction	stAction;

	if( _iSig <= 0 )
		return	CN_SIGNAL_ERROR;

	memset( &stAction, 0x00, sizeof( stAction ) );

	stAction.sa_handler	= SIG_IGN;

	if( sigaction( _iSig, &stAction, _stpBeforeAction ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	return	CN_SIGNAL_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, struct sigaction*                                           *
*             _stpSigSet  : 받아 들일 시그널이 셋팅 된 구조체                  *
*             _stpSigInfo : 전달 된 시그널에 대한 정보가 채워지는 구조체       *
*                                                                              *
* Return    : int, 성공(시그널 넘버), 실패(-1)                                 *
* Stability : Async-Signal-Safe                                                *
* Explain   : 등록 된 시그널(_stpSigSet)이 프로세스나 쓰레드에 발생하면 발생 한*
*             시그널 번호를 반환하며 _stpSigInfo 변수에는 시그널에 대한 정보가 *
*             수록된다.                                                        *
*******************************************************************************/
int CNRtsWait( sigset_t* _stpSigSet, siginfo_t* _stpSigInfo )
{
	int	iResult;

	if( !_stpSigSet )
		return	CN_SIGNAL_ERROR;

	if( !_stpSigInfo )
		return	CN_SIGNAL_ERROR;

	iResult = CNSigWaitInfo( _stpSigSet, _stpSigInfo );

	if( iResult == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	return	iResult;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, struct siginfo_t*, struct timespec*                         *
*             _stpSigSet  : 받아 들일 시그널이 셋팅 된 구조체                  *
*             _stpSigInfo : 전달 된 시그널에 대한 정보가 채워지는 구조체       *
*             _stpTsTime  : 시그널을 기다릴 시간(timeout)                      *
*                                                                              *
* Return    : int, 성공(시그널 넘버), 시간초과(0), 실패(-1)                    *
* Stability : Async-Signal-Safe                                                *
* Explain   : 정해진 시간(_stpTsTime) 동안 등록 된 시그널(_stpSigSet) 가       *
*             올때까지 대기한다. 등록 된 시그널이 도착 할 경우 도착한 시그널을 *
*             반환하며 발생한 시그널에 대한 정보를 _stpSigInfo 변수에 저장한다.*
*******************************************************************************/
int CNRtsTimeWait( sigset_t* _stpSigSet, siginfo_t* _stpSigInfo, struct timespec* _stpTsTime )
{
	int	iResult;

	if( !_stpSigSet )
		return	CN_SIGNAL_ERROR;

	if( !_stpSigInfo )
		return	CN_SIGNAL_ERROR;

	iResult = CNSigTimedWait( _stpSigSet, _stpSigInfo, _stpTsTime );

	if( iResult == CN_SIGNAL_ERROR )
	{
		/* 시간초과(Timeout)일 경우(시간초과 일 경우 반환값은 -1이고 errno는 EAGAIN이다.) */
		if( errno == EAGAIN )
			return	CN_SIGNAL_TIMEOUT;

		return	CN_SIGNAL_ERROR;
	}

	return	iResult;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : sigset_t*, siginfo_t*                                            *
*             sigwaitinfo : 받아 들일 시그널이 셋팅 된 구조체                  *
*             _stpSigInfo : 전달 된 시그널에 대한 정보가 채워지는 구조체       *
*                                                                              *
* Return    : int, 성공(Signal Number), 실패(-1)                               *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : C Library 의 sigwaitinfo() 함수를 래핑한것이다.                  *
*             Interrupt의 영향을 받지 않도록 하기위해 open()함수의 리턴값이    *
*             CN_SIGNAL_ERROR(-1) 이면서 errno가 EINTR(interrupt에 의한 중지)  *
*             일 경우 다시 sigwaitinfo()을 수행한다.                           *
*******************************************************************************/
int	CNSigWaitInfo( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo )
{
	int	iResult;

	while( ( ( iResult = sigwaitinfo( _stpSigSet, _stpSigInfo ) ) == CN_SIGNAL_ERROR ) && ( errno == EINTR ) );

	return	iResult;

}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : sigset_t*, siginfo_t*, timespec*                                 *
*             _stpSigSet  : 받아 들일 시그널이 셋팅 된 구조체                  *
*             _stpSigInfo : 전달 된 시그널에 대한 정보가 채워지는 구조체       *
*             _stpTsTime  : 시그널을 기다릴 시간(timeout)                      *
*                                                                              *
* Return    : int, 성공(Signal Number), 실패(-1)                               *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : C Library 의 sigtimedwait() 함수를 래핑한것이다.                 *
*             Interrupt의 영향을 받지 않도록 하기위해 open()함수의 리턴값이    *
*             CN_SIGNAL_ERROR(-1) 이면서 errno가 EINTR(interrupt에 의한 중지)  *
*             일 경우 다시 sigtimedwait()을 수행한다.                          *
*******************************************************************************/
int	CNSigTimedWait( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo, const struct timespec* _stpTsTime )
{
	int	iResult;

	while( ( ( iResult = sigtimedwait( _stpSigSet, _stpSigInfo, _stpTsTime ) ) == CN_SIGNAL_ERROR ) && ( errno == EINTR ) );

	return	iResult;
}

