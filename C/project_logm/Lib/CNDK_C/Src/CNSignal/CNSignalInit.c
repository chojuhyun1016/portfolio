#include "CNSignalInit.h"

/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : void (*sa_handler)(int)                                          *
*             _fnHandler : 시그널 핸들링 함수로 등록 할 함수의 함수포인터      *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 아래에 열거 된 시그널 리스트들에 대해서 핸들러(_fnHandler)를     *
*             등록한다. 등록 된 이후에 전달되는 시그널에 대해서는 _handler가   *
*             실행 된다. 등록 된 모든 시그널을 일반 비확장 시그널로 처리한다.  *
*             되도록이면 아래에 InitRtSignal을 쓰는것이 좋다.                  *
*******************************************************************************/
int	CNInitSignal( CN_SIG_HANDLER _fnHandler )
{
	/* 1 : hangup */
	if( CNSigAction( SIGHUP, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 2 : interrupt (rubout) */
	if( CNSigAction( SIGINT, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 3 : quit (ASCII FS) */
	if( CNSigAction( SIGQUIT, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 4 : illegal instruction(not reset when caught) */
	if( CNSigAction( SIGILL, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 5 : trace trap (not reset when caught) */
	if( CNSigAction( SIGTRAP, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 6 : IOT instruction */
	if( CNSigAction( SIGIOT, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 7 : used by abort,replace SIGIOT in the future */
	if( CNSigAction( SIGABRT, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 8 : EMT instruction */
	if( CNSigAction( SIGEMT, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 9 : floating point exception */
	if( CNSigAction( SIGFPE, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 10 : kill (cannot be caught or ignored) */
	//if( CNSigAction( SIGKILL, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
	//	return	CN_SIGNAL_ERROR;

	/* 11: bus error */
	if( CNSigAction( SIGBUS, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 12: segmentation violation */
	if( CNSigAction( SIGSEGV, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 13: bad argument to system call */
	if( CNSigAction( SIGSYS, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 14: write on a pipe with no one to read it */
	if( CNSigIgnore( SIGPIPE, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 15: alarm clock */
	if( CNSigAction( SIGALRM, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 16: software termination signal from kill */
	if( CNSigAction( SIGTERM, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 17: user defined signal 1 */
	if( CNSigAction( SIGUSR1, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 18: user defined signal 2 */
	if( CNSigAction( SIGUSR2, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 19: child status change */
	if( CNSigIgnore( SIGCLD, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 20: child status change alias (POSIX) */
	if( CNSigIgnore( SIGCHLD, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 21: power-fail restart */
	if( CNSigAction( SIGPWR, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 22: window size change */
	if( CNSigIgnore( SIGWINCH, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 23: urgent socket condition */
	if( CNSigAction( SIGURG, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 24: pollable event occured */
	if( CNSigAction( SIGPOLL, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 25: socket I/O possible (SIGPOLL alias) */
	if( CNSigAction( SIGIO, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 26: stop (cannot be caught or ignored) */
	//if( CNSigAction( SIGSTOP, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
	//	return	CN_SIGNAL_ERROR;

	/* 27: user stop requested from tty */
	if( CNSigAction( SIGTSTP, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 28: stopped process has been continued */
	if( CNSigAction( SIGCONT, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 29: background tty read attempted */
	if( CNSigAction( SIGTTIN, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 30: background tty write attempted */
	if( CNSigAction( SIGTTOU, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 31: virtual timer expired */
	if( CNSigAction( SIGVTALRM, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 32: profiling timer expired */
	if( CNSigAction( SIGPROF, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 33: exceeded cpu limit */
	if( CNSigAction( SIGXCPU, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 34: exceeded file size limit */
	if( CNSigAction( SIGXFSZ, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	return	CN_SIGNAL_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : void (*sa_sigaction)(int, siginfo_t *, void *)                   *
*             _fnHandler : 시그널 핸들링 함수로 등록 할 함수의 함수포인터      *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 아래에 열거 된 시그널 리스트들에 대해서 핸들러(_fnHandler)를     *
*             등록한다. 등록 된 이후에 전달되는 시그널에 대해서는 _handler가   *
*             실행 된다. 모든 시그널을 확장된 시그널(RTS)를 통해 처리한다.     *
*******************************************************************************/
int	CNInitRtSignal( CN_RT_SIG_HANDLER _fnHandler )
{
	/* 1 : hangup */
	if( CNRtSigAction( SIGHUP, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 2 : interrupt (rubout) */
	if( CNRtSigAction( SIGINT, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 3 : quit (ASCII FS) */
	if( CNRtSigAction( SIGQUIT, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 4 : illegal instruction(not reset when caught) */
	if( CNRtSigAction( SIGILL, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 5 : trace trap (not reset when caught) */
	if( CNRtSigAction( SIGTRAP, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 6 : IOT instruction */
	if( CNRtSigAction( SIGIOT, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 7 : used by abort,replace SIGIOT in the future */
	if( CNRtSigAction( SIGABRT, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 8 : EMT instruction */
	if( CNRtSigAction( SIGEMT, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 9 : floating point exception */
	if( CNRtSigAction( SIGFPE, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 10 : kill (cannot be caught or ignored) */
	//if( CNRtSigAction( SIGKILL, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
	//	return	CN_SIGNAL_ERROR;

	/* 11: bus error */
	if( CNRtSigAction( SIGBUS, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 12: segmentation violation */
	if( CNRtSigAction( SIGSEGV, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 13: bad argument to system call */
	if( CNRtSigAction( SIGSYS, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 14: write on a pipe with no one to read it */
	if( CNSigIgnore( SIGPIPE, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 15: alarm clock */
	if( CNRtSigAction( SIGALRM, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 16: software termination signal from kill */
	if( CNRtSigAction( SIGTERM, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 17: user defined signal 1 */
	if( CNRtSigAction( SIGUSR1, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 18: user defined signal 2 */
	if( CNRtSigAction( SIGUSR2, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 19: child status change */
	if( CNSigIgnore( SIGCLD, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 20: child status change alias (POSIX) */
	if( CNSigIgnore( SIGCHLD, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 21: power-fail restart */
	if( CNRtSigAction( SIGPWR, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 22: window size change */
	if( CNSigIgnore( SIGWINCH, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 23: urgent socket condition */
	if( CNRtSigAction( SIGURG, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 24: pollable event occured */
	if( CNRtSigAction( SIGPOLL, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 25: socket I/O possible (SIGPOLL alias) */
	if( CNRtSigAction( SIGIO, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 26: stop (cannot be caught or ignored) */
	if( CNRtSigAction( SIGSTOP, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 27: user stop requested from tty */
	if( CNRtSigAction( SIGTSTP, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 28: stopped process has been continued */
	if( CNRtSigAction( SIGCONT, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 29: background tty read attempted */
	if( CNRtSigAction( SIGTTIN, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 30: background tty write attempted */
	if( CNRtSigAction( SIGTTOU, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 31: virtual timer expired */
	if( CNRtSigAction( SIGVTALRM, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 32: profiling timer expired */
	if( CNRtSigAction( SIGPROF, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 33: exceeded cpu limit */
	if( CNRtSigAction( SIGXCPU, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 34: exceeded file size limit */
	if( CNRtSigAction( SIGXFSZ, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	/* 35: Real Time Signal */
	if( CNRtSigAction( SIGRTMIN, _fnHandler, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	return	CN_SIGNAL_SUCCESS;
}

/****************************** Handler Sample ******************************
* 핸들러의 기본 형태는 아래와 같이 하면 된다.                               *
* 확장된 시그널(RTS) 에서도 일반 시그널을 잡아 낼수 있으므로 핸들러 안에서  *
* 경우에 따라서 일반 시그널과 확장된 시그널(Real Time Signal)의 처리만      *
* 나눠주면 된다.                                                            *
*                                                                           *
* extern "C" void RtSigHandler( int signo, siginfo_t* info, void* val )     *
* {                                                                         *
*     switch( info->si_code )                                               *
*     {                                                                     *
*         case SI_USER:                                                     *
*             fprintf( strerr, "일반 시그널이 날라온거다" );                *
*             break;                                                        *
*                                                                           *
*         case SI_QUEUE:                                                    *
*             fprintf( strerr, "RtalTime Signal 이 날라온거다" );           *
*             break;                                                        *
*                                                                           *
*         defaule:                                                          *
*             break;                                                        *
*     }                                                                     *
* }                                                                         *
****************************************************************************/


