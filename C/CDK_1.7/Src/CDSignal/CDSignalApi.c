#include "CDSignalApi.h"
#include "CDSignalDefinitions.h"

#ifdef  _SOLARIS_
    #include <errno.h>
    #include <fcntl.h>
    #include <signal.h>
#elif _CENT_OS_
    #include <errno.h>
    #include <string.h>
    #include <signal.h>
#else
    #include <errno.h>
    #include <fcntl.h>
    #include <signal.h>
#endif


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, void (*sa_iSigaction)(int, siginfo_t *, void *),            *
*             _iSig            : ��� �� �ñ׳��� ��ȣ                         *
*             _fnHandler       : ��� �� �ñ׳�(_iSig)�� �ڵ鸵 �Լ�������     *
*             _stpBeforeAction : ������ ��� ���ִ� ������ ���� �� ����ü      *
*                                                                              *
*             struct sigaction*                                                *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : �ش� �ñ׳�(_iSig) �� �Ϲ� �ڵ鷯(_fnHandler) �� ����Ѵ�.       *
*******************************************************************************/
int CDSignalAction( int _iSig, CD_SIG_HANDLER _fnHandler, struct sigaction* _stpBeforeAction )
{
    struct sigaction    stAction;

    if( _iSig <= 0 )
        return  CD_SIGNAL_ERROR;

    if( !_fnHandler )
        return  CD_SIGNAL_ERROR;

    memset( &stAction, 0x00, sizeof( stAction ) );

    // �Ϲ� �ñ׳η� ���
    stAction.sa_flags   = SA_RESTART;
    stAction.sa_handler = _fnHandler;

    if( sigfillset( &stAction.sa_mask ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    if( sigaction( _iSig, &stAction, _stpBeforeAction ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    return  CD_SIGNAL_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, void (RT_iSig_HANDLER)(int, siginfo_t *, void *),           *
*             _iSig            : ��� �� �ñ׳��� ��ȣ                         *
*             _fnHandler       : ��� �� �ñ׳�(_iSig)�� �ڵ鸵 �Լ�������     *
*             _stpBeforeAction : ������ ��� ���ִ� ������ ���� �� ����ü      *
*                                                                              *
*                                                                              *
*             struct sigaction*                                                *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : �ش� �ñ׳�(_iSig) �� �Ϲ� ����Ÿ�� �ñ׳� �ڵ鷯(_fnHandler)��  *
*             ����Ѵ�. �Ϲ� �ñ׳ε� ����Ÿ�� �ñ׳� �ڵ鷯���� ���� �ɷ����� *
*             �����Ƿ� �� �Լ��� ��� �ϰ� ��� �ϴ� ���� ��õ�Ѵ�.            *
*******************************************************************************/
int CDSignalRtsAction( int _iSig, CD_RT_SIG_HANDLER _fnHandler, struct sigaction* _stpBeforeAction )
{
    struct sigaction    stAction;

    if( _iSig <= 0 )
        return  CD_SIGNAL_ERROR;

    if( !_fnHandler )
        return  CD_SIGNAL_ERROR;

    memset( &stAction, 0x00, sizeof( stAction ) );

    // Ȯ�� �� �ñ׳η� ���(SA_SIGINFO)
    stAction.sa_flags       = (SA_SIGINFO | SA_RESTART);
    stAction.sa_sigaction   = _fnHandler;

    if( sigfillset( &stAction.sa_mask ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    if( sigaction( _iSig, &stAction, _stpBeforeAction ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    return  CD_SIGNAL_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : int, struct sigaction*                                           *
*             _iSig            : ����(ignore) �� �ñ׳��� ��ȣ                 *
*             _stpBeforeAction : ������ �ñ׳� ������ ���� �� ����ü           *
*                                                                              *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���μ��� �� �����尡 �ش� �ñ׳�(_iSig)�� ����(ignore) �ϵ���    *
*             �����Ѵ�.                                                        *
*******************************************************************************/
int CDSignalIgnore( int _iSig, struct sigaction* _stpBeforeAction )
{
    struct sigaction    stAction;

    if( _iSig <= 0 )
        return  CD_SIGNAL_ERROR;

    memset( &stAction, 0x00, sizeof( stAction ) );

    stAction.sa_handler = SIG_IGN;

    if( sigaction( _iSig, &stAction, _stpBeforeAction ) == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    return  CD_SIGNAL_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : sigset_t*, siginfo_t*                                            *
*             sigwaitinfo : �޾� ���� �ñ׳��� ���� �� ����ü                  *
*             _stpSigInfo : ���� �� �ñ׳ο� ���� ������ ä������ ����ü       *
*                                                                              *
* Return    : int, ����(Signal Number), ����(-1)                               *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : C Library �� sigwaitinfo() �Լ��� �����Ѱ��̴�.                  *
*             Interrupt�� ������ ���� �ʵ��� �ϱ����� open()�Լ��� ���ϰ���    *
*             CD_SIGNAL_ERROR(-1) �̸鼭 errno�� EINTR(interrupt�� ���� ����)  *
*             �� ��� �ٽ� sigwaitinfo()�� �����Ѵ�.                           *
*******************************************************************************/
int CDSignalWait( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo )
{
    int iResult;

    while( ( ( iResult = sigwaitinfo( _stpSigSet, _stpSigInfo ) ) == CD_SIGNAL_ERROR ) && ( errno == EINTR ) );

    return  iResult;

}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : sigset_t*, siginfo_t*, timespec*                                 *
*             _stpSigSet  : �޾� ���� �ñ׳��� ���� �� ����ü                  *
*             _stpSigInfo : ���� �� �ñ׳ο� ���� ������ ä������ ����ü       *
*             _stpTsTime  : �ñ׳��� ��ٸ� �ð�(timeout)                      *
*                                                                              *
* Return    : int, ����(Signal Number), ����(-1)                               *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : C Library �� sigtimedwait() �Լ��� �����Ѱ��̴�.                 *
*             Interrupt�� ������ ���� �ʵ��� �ϱ����� open()�Լ��� ���ϰ���    *
*             CD_SIGNAL_ERROR(-1) �̸鼭 errno�� EINTR(interrupt�� ���� ����)  *
*             �� ��� �ٽ� sigtimedwait()�� �����Ѵ�.                          *
*******************************************************************************/
int CDSignalTimedWait( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo, const struct timespec* _stpTsTime )
{
    int iResult;

    while( ( ( iResult = sigtimedwait( _stpSigSet, _stpSigInfo, _stpTsTime ) ) == CD_SIGNAL_ERROR ) && ( errno == EINTR ) );

    return  iResult;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, struct sigaction*                                           *
*             _stpSigSet  : �޾� ���� �ñ׳��� ���� �� ����ü                  *
*             _stpSigInfo : ���� �� �ñ׳ο� ���� ������ ä������ ����ü       *
*                                                                              *
* Return    : int, ����(�ñ׳� �ѹ�), ����(-1)                                 *
* Stability : Async-Signal-Safe                                                *
* Explain   : ��� �� �ñ׳�(_stpSigSet)�� ���μ����� �����忡 �߻��ϸ� �߻� ��*
*             �ñ׳� ��ȣ�� ��ȯ�ϸ� _stpSigInfo �������� �ñ׳ο� ���� ������ *
*             ���ϵȴ�.                                                        *
*******************************************************************************/
int CDSignalRtsWait( sigset_t* _stpSigSet, siginfo_t* _stpSigInfo )
{
    int iResult;

    if( !_stpSigSet )
        return  CD_SIGNAL_ERROR;

    if( !_stpSigInfo )
        return  CD_SIGNAL_ERROR;

    iResult = CDSignalWait( _stpSigSet, _stpSigInfo );

    if( iResult == CD_SIGNAL_ERROR )
        return  CD_SIGNAL_ERROR;

    return  iResult;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, struct siginfo_t*, struct timespec*                         *
*             _stpSigSet  : �޾� ���� �ñ׳��� ���� �� ����ü                  *
*             _stpSigInfo : ���� �� �ñ׳ο� ���� ������ ä������ ����ü       *
*             _stpTsTime  : �ñ׳��� ��ٸ� �ð�(timeout)                      *
*                                                                              *
* Return    : int, ����(�ñ׳� �ѹ�), �ð��ʰ�(0), ����(-1)                    *
* Stability : Async-Signal-Safe                                                *
* Explain   : ������ �ð�(_stpTsTime) ���� ��� �� �ñ׳�(_stpSigSet) ��       *
*             �ö����� ����Ѵ�. ��� �� �ñ׳��� ���� �� ��� ������ �ñ׳��� *
*             ��ȯ�ϸ� �߻��� �ñ׳ο� ���� ������ _stpSigInfo ������ �����Ѵ�.*
*******************************************************************************/
int CDSignalRtsTimedWait( sigset_t* _stpSigSet, siginfo_t* _stpSigInfo, struct timespec* _stpTsTime )
{
    int iResult;

    if( !_stpSigSet )
        return  CD_SIGNAL_ERROR;

    if( !_stpSigInfo )
        return  CD_SIGNAL_ERROR;

    iResult = CDSignalTimedWait( _stpSigSet, _stpSigInfo, _stpTsTime );

    if( iResult == CD_SIGNAL_ERROR )
    {
        // �ð��ʰ�(Timeout)�� ���(�ð��ʰ� �� ��� ��ȯ���� -1�̰� errno�� EAGAIN�̴�.)
        if( errno == EAGAIN )
            return  CD_SIGNAL_TIMEOUT;

        return  CD_SIGNAL_ERROR;
    }

    return  iResult;
}
