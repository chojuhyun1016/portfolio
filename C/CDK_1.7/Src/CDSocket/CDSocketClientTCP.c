#include "CDSocketApi.h"
#include "CDSocketClientTCP.h"

#ifdef  _SOLARIS_
    #include <errno.h>
    #include <fcntl.h>
    #include <unistd.h>
    #include <string.h>
    #include <arpa/inet.h>
#elif _CENT_OS_
    #include <errno.h>
    #include <fcntl.h>
    #include <unistd.h>
    #include <string.h>
    #include <arpa/inet.h>
#else
    #include <errno.h>
    #include <fcntl.h>
    #include <unistd.h>
    #include <string.h>
    #include <arpa/inet.h>
#endif


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : char*, int, struct _stClientErr*                                 *
*             _cpAddress    : 접속 할 서버의 주소(IP) 문자열                   *
*             _iPort        : 접속 할 서버의 포트(Port) 문자열                 *
*             _stpClientErr : 서버소켓 접속 실패 시 실패 원인을 저장 할 구조체 *
*                             포인터                                           *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 문자열 주소(_cpAddress, IP:xxx.xxx.xxx.xxx)와 Port(_nport)를     *
*             받아서 해당 주소의 서버로 접속을 시도한다. 접속 과정의 Timeout   *
*             은 3초이다. 3초 이내에 서버와 통신이 가능한 상태가 되지 않으면   *
*             실패를 반환한다. 실패 원인은 _stpClientErr 구조체에 기록된다.    *
*             기본적으로 Greaceful Disconnect가 아닌 Abortive Disconnect다.    *
*******************************************************************************/
int CDSocketMakeConnectTCP( char* _cpAddress, int _iPort, stCDSocketCliErr* _stpClientErr )
{
    int                 iSock           =   0;
    int                 iError          =   0;
    int                 iResult         =   0;

    int                 iReuseAddr      =   1;
    int                 iRecvBufSize    =   65535;
    int                 iSendBufSize    =   65535;

    struct pollfd       stPoll;

    struct linger       stLinger;
    struct sockaddr_in  stSockAddr;

    stLinger.l_onoff            = 0;
    stLinger.l_linger           = 0;

    ::memset( ( char* )&stSockAddr, 0, sizeof( stSockAddr ) );
    stSockAddr.sin_family       = AF_INET;
    stSockAddr.sin_port         = htons( _iPort );
    stSockAddr.sin_addr.s_addr  = inet_addr( _cpAddress );

    if( ( iSock = ::socket( AF_INET, SOCK_STREAM, 0 ) ) == CD_SOCKET_ERROR )
    {
        iError = errno;

        _stpClientErr->iErrno       = iError;
        _stpClientErr->iReturn      = CD_SOCKET_ERROR;

        #ifdef  _SOLARIS_
            ::strlcpy( _stpClientErr->caErrStr, "socket() : Error", CD_ERR_CLIENT_BUF_SIZE );
        #elif _CENT_OS_
            ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
            ::strncpy( _stpClientErr->caErrStr, "socket() : Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
        #else
            ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
            ::strncpy( _stpClientErr->caErrStr, "socket() : Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
        #endif

        return  CD_SOCKET_ERROR;
    }

    if( ::setsockopt( iSock, SOL_SOCKET, SO_LINGER, ( char * )&stLinger, sizeof( stLinger ) ) == CD_SOCKET_ERROR )
    {
        iError = errno;

        CDSocketClose( iSock );

        _stpClientErr->iErrno       = iError;
        _stpClientErr->iReturn      = CD_SOCKET_ERROR;

        #ifdef  _SOLARIS_
            ::strlcpy( _stpClientErr->caErrStr, "setsockopt() : SO_LINGER Set Error", CD_ERR_CLIENT_BUF_SIZE );
        #elif _CENT_OS_
            ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
            ::strncpy( _stpClientErr->caErrStr, "setsockopt() : SO_LINGER Set Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
        #else
            ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
            ::strncpy( _stpClientErr->caErrStr, "setsockopt() : SO_LINGER Set Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
        #endif

        return  CD_SOCKET_ERROR;
    }

    if( ::setsockopt( iSock, SOL_SOCKET, SO_REUSEADDR, ( char * )&iReuseAddr, sizeof( iReuseAddr ) ) == CD_SOCKET_ERROR )
    {
        iError = errno;

        CDSocketClose( iSock );

        _stpClientErr->iErrno       = iError;
        _stpClientErr->iReturn      = CD_SOCKET_ERROR;

        #ifdef  _SOLARIS_
            ::strlcpy( _stpClientErr->caErrStr, "setsockopt() : SO_REUSEADDR Set Error", CD_ERR_CLIENT_BUF_SIZE );
        #elif _CENT_OS_
            ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
            ::strncpy( _stpClientErr->caErrStr, "setsockopt() : SO_REUSEADDR Set Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
        #else
            ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
            ::strncpy( _stpClientErr->caErrStr, "setsockopt() : SO_REUSEADDR Set Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
        #endif

        return  CD_SOCKET_ERROR;
    }

    if( ::setsockopt( iSock, SOL_SOCKET, SO_RCVBUF, ( char* )&iRecvBufSize, sizeof( iRecvBufSize ) ) == CD_SOCKET_ERROR )
    {
        iError = errno;

        CDSocketClose( iSock );

        _stpClientErr->iErrno       = iError;
        _stpClientErr->iReturn      = CD_SOCKET_ERROR;


        #ifdef  _SOLARIS_
            ::strlcpy( _stpClientErr->caErrStr, "setsockopt() : SO_RCVBUF Set Error", CD_ERR_CLIENT_BUF_SIZE );
        #elif _CENT_OS_
            ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
            ::strncpy( _stpClientErr->caErrStr, "setsockopt() : SO_RCVBUF Set Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
        #else
            ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
            ::strncpy( _stpClientErr->caErrStr, "setsockopt() : SO_RCVBUF Set Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
        #endif

        return  CD_SOCKET_ERROR;
    }

    if( ::setsockopt( iSock, SOL_SOCKET, SO_SNDBUF, ( char* )&iSendBufSize, sizeof( iSendBufSize ) ) == CD_SOCKET_ERROR )
    {
        iError = errno;

        CDSocketClose( iSock );

        _stpClientErr->iErrno       = iError;
        _stpClientErr->iReturn      = CD_SOCKET_ERROR;

        #ifdef  _SOLARIS_
            ::strlcpy( _stpClientErr->caErrStr, "setsockopt() : SO_SNDBUF Set Error", CD_ERR_CLIENT_BUF_SIZE );
        #elif _CENT_OS_
            ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
            ::strncpy( _stpClientErr->caErrStr, "setsockopt() : SO_SNDBUF Set Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
        #else
            ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
            ::strncpy( _stpClientErr->caErrStr, "setsockopt() : SO_SNDBUF Set Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
        #endif

        return  CD_SOCKET_ERROR;
    }

    if( ::fcntl( iSock, F_SETFL, O_NDELAY | O_NONBLOCK ) == CD_SOCKET_ERROR )
    {
        iError = errno;

        CDSocketClose( iSock );

        _stpClientErr->iErrno       = iError;
        _stpClientErr->iReturn      = CD_SOCKET_ERROR;

        #ifdef  _SOLARIS_
            ::strlcpy( _stpClientErr->caErrStr, "fcntl() : NONBLOCKING Set Error", CD_ERR_CLIENT_BUF_SIZE );
        #elif _CENT_OS_
            ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
            ::strncpy( _stpClientErr->caErrStr, "fcntl() : NONBLOCKING Set Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
        #else
            ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
            ::strncpy( _stpClientErr->caErrStr, "fcntl() : NONBLOCKING Set Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
        #endif

        return  CD_SOCKET_ERROR;
    }

    if( ::connect( iSock, ( struct sockaddr* ) &stSockAddr, sizeof( stSockAddr ) ) == CD_SOCKET_ERROR )
    {
        if( errno == EINTR || errno == EALREADY || errno == EINPROGRESS )
        {
            stPoll.fd       = iSock;
            stPoll.events   = ( POLLOUT );
            stPoll.revents  = 0x00;

            iResult = CDSocketPoll( &stPoll, 1, CD_CONNECT_TIMEOUT );

            if( iResult == CD_SOCKET_ERROR )
            {
                iError = errno;

                CDSocketClose( iSock );

                _stpClientErr->iErrno       = iError;
                _stpClientErr->iReturn      = CD_SOCKET_ERROR;

                #ifdef  _SOLARIS_
                    ::strlcpy( _stpClientErr->caErrStr, "CDSocketPoll() : Poll Error", CD_ERR_CLIENT_BUF_SIZE );
                #elif _CENT_OS_
                    ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
                    ::strncpy( _stpClientErr->caErrStr, "CDSocketPoll() : Poll Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
                #else
                    ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
                    ::strncpy( _stpClientErr->caErrStr, "CDSocketPoll() : Poll Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
                #endif

                return  CD_SOCKET_ERROR;
            }

            if( stPoll.revents & ( POLLHUP | POLLERR | POLLNVAL ) )
            {
                iError = errno;

                CDSocketClose( iSock );

                _stpClientErr->iErrno       = iError;
                _stpClientErr->iReturn      = CD_SOCKET_ERROR;

                #ifdef  _SOLARIS_
                    ::strlcpy( _stpClientErr->caErrStr, "CDSocketPoll() : Poll Socket State Error", CD_ERR_CLIENT_BUF_SIZE );
                #elif _CENT_OS_
                    ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
                    ::strncpy( _stpClientErr->caErrStr, "CDSocketPoll() : Poll Socket State Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
                #else
                    ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
                    ::strncpy( _stpClientErr->caErrStr, "CDSocketPoll() : Poll Socket State Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
                #endif

                return  CD_SOCKET_ERROR;
            }

            if( !( stPoll.revents & ( POLLOUT ) ) )
            {
                iError = errno;

                CDSocketClose( iSock );

                _stpClientErr->iErrno       = iError;
                _stpClientErr->iReturn      = CD_SOCKET_ERROR;

                #ifdef  _SOLARIS_
                    ::strlcpy( _stpClientErr->caErrStr, "CDSocketPoll() : Poll Socket Usable Error", CD_ERR_CLIENT_BUF_SIZE );
                #elif _CENT_OS_
                    ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
                    ::strncpy( _stpClientErr->caErrStr, "CDSocketPoll() : Poll Socket Usable Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
                #else
                    ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
                    ::strncpy( _stpClientErr->caErrStr, "CDSocketPoll() : Poll Socket Usable Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
                #endif

                return  CD_SOCKET_ERROR;
            }
        }
        else
        {
            iError = errno;

            CDSocketClose( iSock );

            _stpClientErr->iErrno       = iError;
            _stpClientErr->iReturn      = CD_SOCKET_ERROR;

            #ifdef  _SOLARIS_
                ::strlcpy( _stpClientErr->caErrStr, "connect() : Error", CD_ERR_CLIENT_BUF_SIZE );
            #elif _CENT_OS_
                ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
                ::strncpy( _stpClientErr->caErrStr, "connect() : Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
            #else
                ::memset( _stpClientErr->caErrStr, 0x00, CD_ERR_CLIENT_BUF_SIZE );
                ::strncpy( _stpClientErr->caErrStr, "connect() : Error", CD_ERR_CLIENT_BUF_SIZE - 1 );
            #endif

            return  CD_SOCKET_ERROR;
        }
    }

    return  iSock;
}

