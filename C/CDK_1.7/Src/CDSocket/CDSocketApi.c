#include "CDSocketApi.h"
#include "CDSocketDefinitions.h"

#ifdef  _SOLARIS_
    #include <poll.h>
    #include <errno.h>
    #include <unistd.h>
    #include <sys/socket.h>
#elif _CENT_OS_
    #include <poll.h>
    #include <errno.h>
    #include <unistd.h>
    #include <sys/socket.h>
#else
    #include <poll.h>
    #include <errno.h>
    #include <unistd.h>
    #include <sys/socket.h>
#endif


/*******************************************************************************
* Update    : 2010/10/04                                                       *
* Argument  : int                                                              *
*             _iFd : �������� ������ ���� ��ũ����                           *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : System Call close() �Լ��� �����Ѱ��̴�.                         *
*             Interrupt�� ������ ���� �ʵ��� �ϱ����� close()�Լ��� ���ϰ���   *
*             CD_SOCKET_ERROR(-1) �̸鼭 errno�� EINTR(interrupt�� ���� ����)  *
*             �� ��� �ٽ� close()�� �����Ѵ�.                                 *
*******************************************************************************/
int CDSocketClose( int _iFd )
{
    int iResult;

    while( ( iResult = ::close( _iFd ) ) == CD_SOCKET_ERROR && errno == EINTR );

    return  iResult;
}


/*******************************************************************************
* Update    : 2010/10/04                                                       *
* Argument  : int, sockaddr*, socklen_t*                                       *
*             _iListen     : Accept�� ���� �� ���� ��ũ����                  *
*             _stpSockAddr : Accept �� Client�� ������ ���� �� ����ü ������   *
*             _ipLen       : _stpSockAddr ����ü�� ũ��                        *
*                                                                              *
* Return    : int, ����(Socket Descriptor), ����(-1)                           *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : Sockets Library�� accept()�Լ��� �����Ѱ��̴�.                   *
*             Interrupt�� ������ ���� �ʵ��� �ϱ����� open()�Լ��� ���ϰ���    *
*             CD_SOCKET_ERROR(-1) �̸鼭 errno�� EINTR(interrupt�� ���� ����)  *
*             �� ��� �ٽ� accept()�� �����Ѵ�.                                *
*******************************************************************************/
int CDSocketAccept( int _iListen, struct sockaddr* _stpSockAddr, socklen_t* _ipLen )
{
    int iResult;

    while( ( ( iResult = ::accept( _iListen, _stpSockAddr, _ipLen ) ) == CD_SOCKET_ERROR ) && (errno == EINTR) );

    return  iResult;
}


/*******************************************************************************
* Update    : 2010/10/04                                                       *
* Argument  : pollfd*, nfds_t, int                                             *
*             _stpPoll  : pollfd ����ü�� ������                               *
*             _iCnt     : �̺�Ʈ ������ ���� �� pollfd ����ü�� ����           *
*             _iTimeout : �̺�Ʈ ����(poll) Ÿ�Ӿƿ�                           *
*                                                                              *
* Return    : int, ����(�̺�Ʈ�� �Ͼ ���ϵ�ũ������ ����), ����(-1)       *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : System Call poll()�Լ��� �����Ѱ��̴�.                           *
*             Interrupt�� ������ ���� �ʵ��� �ϱ����� open()�Լ��� ���ϰ���    *
*             CD_SOCKET_ERROR(-1) �̸鼭 errno�� EINTR(interrupt�� ���� ����)  *
*             �� ��� �ٽ� poll()�� �����Ѵ�.                                  *
*******************************************************************************/
int CDSocketPoll( struct  pollfd* _stpPoll, nfds_t _iCnt, int _iTimeout )
{
    int iResult;

    while( ( ( iResult = ::poll( _stpPoll, _iCnt, _iTimeout ) ) == CD_SOCKET_ERROR ) && ( errno == EINTR ) );

    return iResult;
}
