#include "CNSignalInit.h"

/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : void (*sa_handler)(int)                                          *
*             _fnHandler : �ñ׳� �ڵ鸵 �Լ��� ��� �� �Լ��� �Լ�������      *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : �Ʒ��� ���� �� �ñ׳� ����Ʈ�鿡 ���ؼ� �ڵ鷯(_fnHandler)��     *
*             ����Ѵ�. ��� �� ���Ŀ� ���޵Ǵ� �ñ׳ο� ���ؼ��� _handler��   *
*             ���� �ȴ�. ��� �� ��� �ñ׳��� �Ϲ� ��Ȯ�� �ñ׳η� ó���Ѵ�.  *
*             �ǵ����̸� �Ʒ��� InitRtSignal�� ���°��� ����.                  *
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
*             _fnHandler : �ñ׳� �ڵ鸵 �Լ��� ��� �� �Լ��� �Լ�������      *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : �Ʒ��� ���� �� �ñ׳� ����Ʈ�鿡 ���ؼ� �ڵ鷯(_fnHandler)��     *
*             ����Ѵ�. ��� �� ���Ŀ� ���޵Ǵ� �ñ׳ο� ���ؼ��� _handler��   *
*             ���� �ȴ�. ��� �ñ׳��� Ȯ��� �ñ׳�(RTS)�� ���� ó���Ѵ�.     *
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
* �ڵ鷯�� �⺻ ���´� �Ʒ��� ���� �ϸ� �ȴ�.                               *
* Ȯ��� �ñ׳�(RTS) ������ �Ϲ� �ñ׳��� ��� ���� �����Ƿ� �ڵ鷯 �ȿ���  *
* ��쿡 ���� �Ϲ� �ñ׳ΰ� Ȯ��� �ñ׳�(Real Time Signal)�� ó����      *
* �����ָ� �ȴ�.                                                            *
*                                                                           *
* extern "C" void RtSigHandler( int signo, siginfo_t* info, void* val )     *
* {                                                                         *
*     switch( info->si_code )                                               *
*     {                                                                     *
*         case SI_USER:                                                     *
*             fprintf( strerr, "�Ϲ� �ñ׳��� ����°Ŵ�" );                *
*             break;                                                        *
*                                                                           *
*         case SI_QUEUE:                                                    *
*             fprintf( strerr, "RtalTime Signal �� ����°Ŵ�" );           *
*             break;                                                        *
*                                                                           *
*         defaule:                                                          *
*             break;                                                        *
*     }                                                                     *
* }                                                                         *
****************************************************************************/

