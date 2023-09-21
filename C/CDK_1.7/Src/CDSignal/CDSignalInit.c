#include "CDSignalInit.h"

#ifdef  _SOLARIS_
    #include <signal.h>
    #include <pthread.h>
#elif _CENT_OS_
    #include <signal.h>
    #include <pthread.h>
#else
    #include <signal.h>
    #include <pthread.h>
#endif


/*******************************************************************************
* Update    : 2010/10/05                                                     S  *
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
int CDSignalInit( CD_SIG_HANDLER _fnHandler )
{
    // 1 : hangup
    if( CDSignalAction( SIGHUP, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 2 : interrupt (rubout)
    if( CDSignalAction( SIGINT, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 3 : quit (ASCII FS)
    if( CDSignalAction( SIGQUIT, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 4 : illegal instruction(not reset when caught)
    if( CDSignalAction( SIGILL, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 5 : trace trap (not reset when caught)
    if( CDSignalAction( SIGTRAP, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 6 : IOT instruction
    if( CDSignalAction( SIGIOT, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 7 : used by abort,replace SIGIOT in the future
    if( CDSignalAction( SIGABRT, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 8 : EMT instruction
    #ifdef  _SOLARIS_
    if( CDSignalAction( SIGEMT, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;
    #endif

    // 9 : floating point exception
    if( CDSignalAction( SIGFPE, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 10 : kill (cannot be caught or ignored)
    //if( CDSignalAction( SIGKILL, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
    //  return  CD_SIGNAL_ERROR;

    // 11: bus error
    if( CDSignalAction( SIGBUS, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 12: segmentation violation
    if( CDSignalAction( SIGSEGV, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 13: bad argument to system call
    if( CDSignalAction( SIGSYS, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 14: write on a pipe with no one to read it
    if( CDSignalIgnore( SIGPIPE, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 15: alarm clock
    if( CDSignalAction( SIGALRM, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 16: software termination signal from kill
    if( CDSignalAction( SIGTERM, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 17: user defined signal 1
    if( CDSignalAction( SIGUSR1, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 18: user defined signal 2
    if( CDSignalAction( SIGUSR2, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 19: child status change
    if( CDSignalIgnore( SIGCLD, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 20: child status change alias (POSIX)
    if( CDSignalIgnore( SIGCHLD, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 21: power-fail restart
    if( CDSignalAction( SIGPWR, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 22: window size change
    if( CDSignalIgnore( SIGWINCH, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 23: urgent socket condition
    if( CDSignalAction( SIGURG, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 24: pollable event occured
    if( CDSignalAction( SIGPOLL, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 25: socket I/O possible (SIGPOLL alias)
    if( CDSignalAction( SIGIO, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 26: stop (cannot be caught or ignored)
    //if( CDSignalAction( SIGSTOP, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
    //  return  CD_SIGNAL_ERROR;

    // 27: user stop requested from tty
    if( CDSignalAction( SIGTSTP, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 28: stopped process has been continued
    if( CDSignalAction( SIGCONT, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 29: background tty read attempted
    if( CDSignalAction( SIGTTIN, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 30: background tty write attempted
    if( CDSignalAction( SIGTTOU, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 31: virtual timer expired
    if( CDSignalAction( SIGVTALRM, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 32: profiling timer expired
    if( CDSignalAction( SIGPROF, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 33: exceeded cpu limit
    if( CDSignalAction( SIGXCPU, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 34: exceeded file size limit
    if( CDSignalAction( SIGXFSZ, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    return  CD_SIGNAL_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/11/01                                                       *
* Argument  : void (*sa_sigaction)(int, siginfo_t *, void *)                   *
*             _fnHandler : 시그널 핸들링 함수로 등록 할 함수의 함수포인터      *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 아래에 열거 된 시그널 리스트들에 대해서 핸들러(_fnHandler)를     *
*             등록한다. 등록 된 이후에 전달되는 시그널에 대해서는 _handler가   *
*             실행 된다. 모든 시그널을 확장된 시그널(RTS)를 통해 처리한다.     *
*******************************************************************************/
int CDSignalInitRts( CD_RT_SIG_HANDLER _fnHandler )
{
    // 1 : hangup
    if( CDSignalRtsAction( SIGHUP, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 2 : interrupt (rubout)
    if( CDSignalRtsAction( SIGINT, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 3 : quit (ASCII FS)
    if( CDSignalRtsAction( SIGQUIT, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 4 : illegal instruction(not reset when caught)
    if( CDSignalRtsAction( SIGILL, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 5 : trace trap (not reset when caught)
    if( CDSignalRtsAction( SIGTRAP, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 6 : IOT instruction
    if( CDSignalRtsAction( SIGIOT, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 7 : used by abort,replace SIGIOT in the future
    if( CDSignalRtsAction( SIGABRT, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 8 : EMT instruction
    #ifdef  _SOLARIS_
    if( CDSignalRtsAction( SIGEMT, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;
    #endif

    // 9 : floating point exception
    if( CDSignalRtsAction( SIGFPE, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 10 : kill (cannot be caught or ignored)
    //if( CDSignalRtsAction( SIGKILL, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
    //  return  CD_SIGNAL_ERROR;

    // 11: bus error
    if( CDSignalRtsAction( SIGBUS, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 12: segmentation violation
    if( CDSignalRtsAction( SIGSEGV, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 13: bad argument to system call
    if( CDSignalRtsAction( SIGSYS, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 14: write on a pipe with no one to read it
    if( CDSignalIgnore( SIGPIPE, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 15: alarm clock
    if( CDSignalRtsAction( SIGALRM, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 16: software termination signal from kill
    if( CDSignalRtsAction( SIGTERM, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 17: user defined signal 1
    if( CDSignalRtsAction( SIGUSR1, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 18: user defined signal 2
    if( CDSignalRtsAction( SIGUSR2, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 19: child status change
    if( CDSignalIgnore( SIGCLD, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 20: child status change alias (POSIX)
    if( CDSignalIgnore( SIGCHLD, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 21: power-fail restart
    if( CDSignalRtsAction( SIGPWR, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 22: window size change
    if( CDSignalIgnore( SIGWINCH, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 23: urgent socket condition
    if( CDSignalRtsAction( SIGURG, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 24: pollable event occured
    if( CDSignalRtsAction( SIGPOLL, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 25: socket I/O possible (SIGPOLL alias)
    if( CDSignalRtsAction( SIGIO, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 26: stop (cannot be caught or ignored)
    //if( CDSignalRtsAction( SIGSTOP, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
    //  return  CD_SIGNAL_ERROR;

    // 27: user stop requested from tty
    if( CDSignalRtsAction( SIGTSTP, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 28: stopped process has been continued
    if( CDSignalRtsAction( SIGCONT, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 29: background tty read attempted
    if( CDSignalRtsAction( SIGTTIN, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 30: background tty write attempted
    if( CDSignalRtsAction( SIGTTOU, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 31: virtual timer expired
    if( CDSignalRtsAction( SIGVTALRM, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 32: profiling timer expired
    if( CDSignalRtsAction( SIGPROF, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 33: exceeded cpu limit
    if( CDSignalRtsAction( SIGXCPU, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 34: exceeded file size limit
    if( CDSignalRtsAction( SIGXFSZ, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    // 35: Real Time Signal
    if( CDSignalRtsAction( SIGRTMIN, _fnHandler, NULL ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    return  CD_SIGNAL_SUCCESS;
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
*             fprintf( strerr, "리얼타임 시그널이 날라온거다" );            *
*             break;                                                        *
*                                                                           *
*         defaule:                                                          *
*             break;                                                        *
*     }                                                                     *
* }                                                                         *
****************************************************************************/


