#ifndef	__CNNET_DEFINITIONS__
#define	__CNNET_DEFINITIONS__

#include <sys/socket.h>

//*****************************************************************************
//  Definitions
//*****************************************************************************
#ifndef INVALID_HANDLE_VALUE
#define INVALID_HANDLE_VALUE	0
#endif

#ifndef INVALID_SOCKET
#define INVALID_SOCKET			0
#endif

#ifndef SOCKET
#define	SOCKET_ERROR			-1
#endif

#ifndef IPPORT_RESERVED
#define	IPPORT_RESERVED			1024
#endif

#ifndef SOCKET
typedef	int SOCKET;
#endif

#ifndef LPSOCKET
typedef	int* LPSOCKET;
#endif

#ifndef POLLFD
typedef	pollfd	POLLFD;
#endif

#ifndef LPPOLLFD
typedef	pollfd* LPPOLLFD;
#endif

#ifndef SOCKADDR
typedef	struct sockaddr SOCKADDR;
#endif

#ifndef LPSOCKADDR
typedef	struct sockaddr* LPSOCKADDR;
#endif

#ifndef SOCKADDR_IN
typedef	struct sockaddr_in SOCKADDR_IN;
#endif

#ifndef LPSOCKADDR_IN
typedef	struct sockaddr_in* LPSOCKADDR_IN;
#endif


//*****************************************************************************
// Struct
//*****************************************************************************
// CNNetClass에서 쓰는 Overlapped 구조체
typedef struct _OVERLAPPED
{
	LPPOLLFD	pPollfd;
} OVERLAPPED, *LPOVERLAPPED;

typedef struct _COMPLETIONPACKET
{
	int				nEvent;
	HANDLE			hCompletionKey;
	LPPOLLFD		pPollfd;

	LPOVERLAPPED	pOverlapped;
} COMPLETIONPACKET, *LPCOMPLETIONPACKET;


//*****************************************************************************
// Exception
//*****************************************************************************
#define	CNNET_TRY				try
#define	CNNET_CATCH(x)			catch (x)
#define	CNNET_THROW(x, e)		e; throw(x)
#define	CNNET_THROW_IF(c, x, e)	if(c)	{e; throw(x);}

// 주의)
//   CNCATCH_ALL의 경우 모든 예외를 다 잡아버린다.
//   따라서 Access Violation과 같은 치명적인 Bug가 발생해도 그냥 지나쳐 버리게 된다.
//   이는 오히려 Debugging을 하는데 방해가 되므로 Debug모드일 때는 catch(...)은
//   사용하지 않는 것이 좋다.
//   CNException::NoCatch는 어떤 경우에도 Throw하지 않기로 약속한 것이므로
//   CNException::NoCatch를 잡는다는 것은 잡지 않는 다는 뜻이다.
#ifdef _DEBUG
#define	CNNET_CATCH_ALL			catch(...)
#else
#define	CNNET_CATCH_ALL			catch(...)
#endif

#define	CNNET_RETHROW			throw

// CN Network Socket의 상태를 나타탬
typedef enum _SCNSocketStatus
{
	SOCKET_STATE_CLOSED				 = 0,	//  0 : 접속이 종료된 상태
	SOCKET_STATE_BINDED				 = 1,	//  1 : Binded
	SOCKET_STATE_SYN				 = 2,	//  2 : 접속진행 중인 상태
	SOCKET_STATE_SEND_DISCONNECTED	 = 3,	//  3 : (Active) 접속종료 진행상태
	SOCKET_STATE_FIN_WAIT_1			 = 4,	//  4 : (Active) 접속종료 진행상태
	SOCKET_STATE_FIN_WAIT_2			 = 5,	//  5 : (Active) 접속종료 진행상태
	SOCKET_STATE_TIME_WAIT			 = 6,	//  6 : (Active) 접속종료 진행상태
	SOCKET_STATE_CLOSE_WAIT			 = 7,	//  7 : (Passive) 접속종료 진행상태
	SOCKET_STATE_LAST_ACK			 = 8,	//  8 : (Passive) 접속종료 진행상태
	SOCKET_STATE_ESTABLISHED		 = 9,	//  9 : 접속만 된 상태
	SOCKET_STATE_LISTEN				 = 10,	// 10 : Listen상태
	SOCKET_STATE_CERTIFIED			 = 11,	// 11 : 접속이 되고 CNNet의 Client검증이 끝난 상태.
	SOCKET_STATE_LOGINED			 = 12	// 12 : ID와 Password를 넣고 Log-In이 된 상태.
} SOCKET_STATE, *LPSOCKET_STATE, &RPSOCKET_STATE;

// Listen Socket BackLog 
#define SIZE_OF_LISTEN_BACKLOG				1024

// Send Receive Buffer
#define	SIZE_OF_SOCKET_SEND_BUFFER			65535
#define	SIZE_OF_SOCKET_RECEIVE_BUFFER		65535

// For TCP Receiveable
#define	DEFAULT_TCP_RECEIVE_BUFFER_SIZE		65535



#endif

