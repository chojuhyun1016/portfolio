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
// CNNetClass���� ���� Overlapped ����ü
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

// ����)
//   CNCATCH_ALL�� ��� ��� ���ܸ� �� ��ƹ�����.
//   ���� Access Violation�� ���� ġ������ Bug�� �߻��ص� �׳� ������ ������ �ȴ�.
//   �̴� ������ Debugging�� �ϴµ� ���ذ� �ǹǷ� Debug����� ���� catch(...)��
//   ������� �ʴ� ���� ����.
//   CNException::NoCatch�� � ��쿡�� Throw���� �ʱ�� ����� ���̹Ƿ�
//   CNException::NoCatch�� ��´ٴ� ���� ���� �ʴ� �ٴ� ���̴�.
#ifdef _DEBUG
#define	CNNET_CATCH_ALL			catch(...)
#else
#define	CNNET_CATCH_ALL			catch(...)
#endif

#define	CNNET_RETHROW			throw

// CN Network Socket�� ���¸� ��Ÿ��
typedef enum _SCNSocketStatus
{
	SOCKET_STATE_CLOSED				 = 0,	//  0 : ������ ����� ����
	SOCKET_STATE_BINDED				 = 1,	//  1 : Binded
	SOCKET_STATE_SYN				 = 2,	//  2 : �������� ���� ����
	SOCKET_STATE_SEND_DISCONNECTED	 = 3,	//  3 : (Active) �������� �������
	SOCKET_STATE_FIN_WAIT_1			 = 4,	//  4 : (Active) �������� �������
	SOCKET_STATE_FIN_WAIT_2			 = 5,	//  5 : (Active) �������� �������
	SOCKET_STATE_TIME_WAIT			 = 6,	//  6 : (Active) �������� �������
	SOCKET_STATE_CLOSE_WAIT			 = 7,	//  7 : (Passive) �������� �������
	SOCKET_STATE_LAST_ACK			 = 8,	//  8 : (Passive) �������� �������
	SOCKET_STATE_ESTABLISHED		 = 9,	//  9 : ���Ӹ� �� ����
	SOCKET_STATE_LISTEN				 = 10,	// 10 : Listen����
	SOCKET_STATE_CERTIFIED			 = 11,	// 11 : ������ �ǰ� CNNet�� Client������ ���� ����.
	SOCKET_STATE_LOGINED			 = 12	// 12 : ID�� Password�� �ְ� Log-In�� �� ����.
} SOCKET_STATE, *LPSOCKET_STATE, &RPSOCKET_STATE;

// Listen Socket BackLog 
#define SIZE_OF_LISTEN_BACKLOG				1024

// Send Receive Buffer
#define	SIZE_OF_SOCKET_SEND_BUFFER			65535
#define	SIZE_OF_SOCKET_RECEIVE_BUFFER		65535

// For TCP Receiveable
#define	DEFAULT_TCP_RECEIVE_BUFFER_SIZE		65535



#endif
