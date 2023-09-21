#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <sys/msg.h>

#include "CNIpcMsgQueue.h"
#include "CNIpcDefinitions.h"


/*******************************************************************************
* Update    : 2011/08/03                                                       *
* Argument  : stQueueInfo*, int, int                                           *
*             _stpQueueInfo : IPC Queue의 정보가 저장되는 구조체의 주소        *
*             _iQueueKey    : 사용 할 IPC Queue 의 Key 값                      *
*             _iMaxCount    : Queue에 저장 할 데이터의 최대 개수               *
*                                                                              *
* Return    : void, 없음                                                       *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하는 구조체(stQueueInfo)의 주소값을 받아서*
*             구조체의 각 필드를 초기화 한다.                                  *
*******************************************************************************/
void CNMsgQueueInit( stQueueInfo* _stpQueueInfo, int _iQueueKey, int _iMaxCount, int _iMode, int _iPerm )
{
	// 1. 초기화
	memset( _stpQueueInfo, 0x00, sizeof( stQueueInfo ) );

	// 2. 셋팅
	_stpQueueInfo->iQueueKey		= _iQueueKey;
	_stpQueueInfo->iMaxCount		= _iMaxCount;

	_stpQueueInfo->iCreateMode		= _iMode;
	_stpQueueInfo->iOpenPermission	= _iPerm;

	// 3. 종료!!
	return;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(생성 된 Queue의 식별자(ID)), 실패(-1)                  *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stQueueInfo)의 주소값을   *
*             인자로 받아서 구조체정보에 따라 IPC Queue를 생성한다.            *
*             생성이 성공 할 경우 구조체에 Queue의 ID값을 저장하고 ID값을      *
*             반환한다. 생성을 실패 할 경우 CN_IPC_ERROR(-1)을 반환한다.       *
*******************************************************************************/
int CNMsgQueueCreate( stQueueInfo* _stpQueueInfo )
{
	// 1. Queue 생성
	_stpQueueInfo->iQueueID = msgget( _stpQueueInfo->iQueueKey, _stpQueueInfo->iCreateMode | _stpQueueInfo->iOpenPermission );

	// 2. Queue 생성을 실패 할 경우(이미 존재 할 경우)에러를 반환
	if( _stpQueueInfo->iQueueID == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 3. 생성 성공!!
	return	_stpQueueInfo->iQueueID;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(생성 된 Queue의 식별자(ID)), 실패(-1)                  *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stQueueInfo)의 주소값을   *
*             인자로 받아서 구조체정보에 따라 IPC Queue를 연다.                *
*             열기를 실패 할 경우 CN_IPC_ERROR(-1)을 반환한다.                 *
*******************************************************************************/
int CNMsgQueueOpen( stQueueInfo* _stpQueueInfo )
{
	// 1. Queue 열기시도
	_stpQueueInfo->iQueueID = msgget( _stpQueueInfo->iQueueKey, _stpQueueInfo->iOpenPermission );

	// 2. 실패 할 경우 에러를 반환
	if( _stpQueueInfo->iQueueID == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 3. 성공!!
	return	_stpQueueInfo->iQueueID;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stQueueInfo)의 주소를     *
*             인자로 받아서 구조체 데이터가 가르키는 IPC Queue의 데이터를      *
*             모두 뽑아낸다. IPC Queue에 데이터가 모두 비워지면                *
*             CN_IPC_SUCCESS(0)을 반환하며 에러가 날 경우 CN_IPC_ERROR(-1)을   *
*             반환한다.                                                        *
*******************************************************************************/
int CNMsgQueueClean( stQueueInfo* _stpQueueInfo )
{
	// 1. Queue의 데이터 카운트가 0일때까지 반복
	while( CNMsgQueueGetCount( _stpQueueInfo ) )
	{
		// 1.1 Queue에서 메시지를 뽑아내서 버린다
		if( CNMsgQueueRead( _stpQueueInfo, NULL, NULL, NULL ) == CN_IPC_ERROR )
			return	CN_IPC_ERROR;
	}

	// 2. 성공!!
	return	CN_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stQueueInfo)의 주소를     *
*             인자로 받아서 구조체 데이터가 가르키는 IPC Queue를 삭제한다.     *
*             삭제 성공시 CN_IPC_SUCCESS(0)을 반환하며 실패 할 경우            *
*             CN_IPC_ERROR(-1)을 반환한다.                                     *
*******************************************************************************/
int CNMsgQueueRemove( stQueueInfo* _stpQueueInfo )
{
	// 1. 해당 IPC Queue의 상태값(STAT)를 조회
	if( CNMsgQueueGetStat( _stpQueueInfo ) == CN_IPC_ERROR )
	{
		// 1.1 이미 해당 Queue가 존재하지 않을경우
		if( errno == ENOENT || errno == EIDRM )
			return	CN_IPC_SUCCESS;

		// 1.2 실제 Error인 경우!!
		return	CN_IPC_ERROR;
	}

	// 2. IPC Queue를 삭제
	if( msgctl( _stpQueueInfo->iQueueID, IPC_RMID, &(_stpQueueInfo->stMsgQueueStat) ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 3. 성공!!
	return	CN_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*             _iSize        : Queue의 크기                                     *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stQueueInfo)의 주소와     *
*             크기(_iSize)를 인자로 받아 해당 Queue의 총 크기를 _iSize로       *
*             변경한다. stTempStat 를 선언하여 Queue 의 정보를 따로            *
*             저장하는 이유는 _stpQueueInfo 구조체는 항상 현재 Queue의 상태를  *
*             저장하여야 하며 변경 실패시 실패 이전의 값으로 복구시키기        *
*             위함이다.                                                        *
*             변경 성공시 CN_IPC_SUCCESS(0)을 반환하며 실패 할 경우            *
*             CN_IPC_ERROR(-1)을 반환한다.                                     *
*******************************************************************************/
int CNMsgQueueSetSize( stQueueInfo* _stpQueueInfo, int _iSize )
{
	struct msqid_ds	stTempStat;

	// 1. 현재 Queue 정보를 얻어온다.
	if( CNMsgQueueGetStat( _stpQueueInfo ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 2. 얻어온 Queue의 정보를 stTempStat에 복사
	memcpy( &stTempStat, &(_stpQueueInfo->stMsgQueueStat), sizeof( struct msqid_ds ) );

	// 3. stTempStat 구조체의 Queue 사이즈를 _iSize 로 셋팅
	stTempStat.msg_qbytes = _iSize;

	// 4. stTempStat 정보로 Queue 정보를 셋팅
	if( msgctl( _stpQueueInfo->iQueueID, IPC_SET, &stTempStat ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 5. 변경이 성공한 경우 stTempStat의 데이터를 _stpQueueInfo->stMsgQueueStat로 복사
	memcpy( &(_stpQueueInfo->stMsgQueueStat), &stTempStat, sizeof( struct msqid_ds ) );

	// 6. 성공!!
	return	CN_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*             _iPermission  : Queue에 대한 접근권한                            *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stQueueInfo)의 주소와     *
*             접근권한(_iPermission)를 인자로 받아 해당 Queue의 접근권한을     *
*             _iPermission으로 변경한다. stTempStat 를 선언하여 Queue 의       *
*             정보를 따로 저장하는 이유는 _stpQueueInfo 구조체는 항상 현재     *
*             Queue의 상태를 저장하여야 하며 변경 실패시 실패 이전의 값으로    *
*             복구시키기 위함이다.                                             *
*             삭제 성공시 CN_IPC_SUCCESS(0)을 반환하며 실패 할 경우            *
*             CN_IPC_ERROR(-1)을 반환한다.                                     *
*******************************************************************************/
int CNMsgQueueSetPermission( stQueueInfo* _stpQueueInfo, int _iPermission )
{
	struct msqid_ds	stTempStat;

	// 1. 현재 Queue정보를 얻어온다.
	if( CNMsgQueueGetStat( _stpQueueInfo ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 2. 얻어온 Queue의 정보를 stTempStat에 복사
	memcpy( &stTempStat, &(_stpQueueInfo->stMsgQueueStat), sizeof( struct msqid_ds ) );

	// 3. stTempStat 구조체의 Queue 접근 권한을 _iPermission 로 셋팅
	stTempStat.msg_perm.mode = _iPermission;

	// 4. IPC Queue Permission 변경
	if( msgctl( _stpQueueInfo->iQueueID, IPC_SET, &stTempStat ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 5. 변경이 성공한 경우 stTempStat의 데이터를 _stpQueueInfo->stMsgQueueStat로 복사
	memcpy( &(_stpQueueInfo->stMsgQueueStat), &stTempStat, sizeof( struct msqid_ds ) );

	// 6. 성공!!
	return	CN_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stQueueInfo)의 주소를     *
*             인자로 받아서 구조체 데이터가 가르키는 IPC Queue의 데이터를      *
*             구조체(_stpQueueInfo->stMsgQueueStat)안에 저장한다.              *
*             에러가 날 경우 CN_IPC_ERROR(-1)을 반환한다.                      *
*******************************************************************************/
int CNMsgQueueGetStat( stQueueInfo* _stpQueueInfo )
{
	// 1. Queue의 정보를 추출
	if( msgctl( _stpQueueInfo->iQueueID, IPC_STAT, &(_stpQueueInfo->stMsgQueueStat) ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 2. 성공!!
	return	CN_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(IPC Queue에 들어있는 데이터의 개수), 실패(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stQueueInfo)의 주소를     *
*             인자로 받아서 구조체 데이터가 가르키는 IPC Queue에 저장되어 있는 *
*             데이터의 개수를 반환한다. 에러가 날 경우 CN_IPC_ERROR(-1)을      *
*             반환한다.                                                        *
*******************************************************************************/
int CNMsgQueueGetCount( stQueueInfo* _stpQueueInfo )
{
	// 1. Queue의 정보를 추출
	if( msgctl( _stpQueueInfo->iQueueID, IPC_STAT, &(_stpQueueInfo->stMsgQueueStat) ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 2. 성공!!
	return	_stpQueueInfo->stMsgQueueStat.msg_qnum;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*, void*, int                                         *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*             _vpPacket     : 전송 할 데이터의 주소                            *
*             _iPacketSize  : 전송 할 데이터의 크기                            *
*                                                                              *
* Return    : int, 성공(IPC Queue에 들어있는 데이터의 개수), 실패(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stQueueInfo)의 주소를     *
*             인자로 받아서 구조체 데이터가 가르키는 IPC Queue에               *
*             버퍼(_vpPacket)의 데이터를 _iPacketSize 크기만큼 전송한다.       *
*             기본적으로 Non-Blocking 방식으로 동작한다.                       *
*             전송에 성공 할 경우 CN_IPC_QUEUE_DATA_WRITE(2)를 반환하며        *
*             실패 할 경우 CN_IPC_QUEUE_DATA_ERROR(-1)을 반환한다.             *
*             버퍼가 가득차서 Write를 하지 못할 경우 CN_IPC_QUEUE_DATA_FULL(0) *
*             을 반환안다.                                                     *
*******************************************************************************/
long CNMsgQueueWrite( stQueueInfo* _stpQueueInfo, void* _vpPacket, int _iPacketSize )
{
	int	iResult;

	// 1. Queue로 데이터 전송 요청
	while( ( iResult = msgsnd( _stpQueueInfo->iQueueID, _vpPacket, _iPacketSize, IPC_NOWAIT ) ) == CN_IPC_ERROR && errno == EINTR );

	// 2. 실패!!
	if( iResult == CN_IPC_ERROR )
	{
		// 2.1 Queue가 가득찼을경우
		if( errno == EAGAIN )
			return	CN_IPC_QUEUE_DATA_FULL;
		// 2.2 Error가 발생한 경우
		else
			return	CN_IPC_QUEUE_DATA_ERROR;
	}

	// 3. 성공!!
	return	CN_IPC_QUEUE_DATA_WRITE;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*, void*, int, int                                    *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*             _vpPacket     : 읽어들인 데이터를 저장 할 버퍼의 주소            *
*             _iPacketSize  : 읽기 버퍼의 크기                                 *
*             _iMsgType     : 읽어 들일 데이터의 종류(Type)                    *
*                                                                              *
* Return    : int, 성공(IPC Queue에 들어있는 데이터의 개수), 실패(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stQueueInfo)의 주소를     *
*             인자로 받아서 구조체 데이터가 가르키는 IPC Queue에서             *
*             _iPacketSize 크기만큼의 데이터를 버퍼(_vpPacket)에 저장한다.     *
*             _iMsgType 는 읽어들일 메시지의 종류(Type)를 나타낸다. _iMsgType  *
*             값에 따라 무조건 읽어들일지 특정 메시지만 읽어들일지가 결정난다. *
*             메시지 종류는 데이터 송수신시 데이터 구조체의 첫번째 맴버        *
*             data_type 값을 나타낸다. _iMsgType으로 0을 설정 할 경우          *
*             Queue의 가장 앞 데이터를 읽어들이며 양수로 설정 할 경우          *
*             설정한 값과 동일한 data_type의 데이터중 가장 앞의 데이터를       *
*             읽어들인다. 음수값을 설정 할 경우 절대값(부호를 뗌)으로 변경 후  *
*             절대값보다 작은수중 가장작고 가장앞의 데이터를 읽어들인다.       *
*             기본적으로 Non-Blocking 모드로 동작한다.                         *
*             읽기가 성공 할 경우 CN_IPC_QUEUE_DATA_READ(1) 을 반환하고        *
*             읽을 데이터가 없을경우 CN_IPC_QUEUE_DATA_EMPTY(0)을 반환하고     *
*             읽기 실패나 Error일 경우 CN_IPC_QUEUE_DATA_ERROR(-1) 을 반환한다.*
*******************************************************************************/
long CNMsgQueueRead( stQueueInfo* _stpQueueInfo, void* _vpPacket, int _iPacketSize, int _iMsgType )
{
	int	iResult;

	// 1. Queue에 데이터 읽어들이기 요청
	while( ( iResult = msgrcv( _stpQueueInfo->iQueueID, _vpPacket, _iPacketSize, _iMsgType, IPC_NOWAIT | MSG_NOERROR ) ) == CN_IPC_ERROR && errno == EINTR );

	// 2. 실패!!
	if( iResult == CN_IPC_ERROR )
	{
		// 2.1 Queue에 읽을 데이터가 없는 경우
		if( errno == ENOMSG )
			return	CN_IPC_QUEUE_DATA_EMPTY;
		// 2.2 Error가 발생한 경우
		else
			return	CN_IPC_QUEUE_DATA_ERROR;
	}

	// 3. 성공!!
	return	CN_IPC_QUEUE_DATA_READ;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iKey : 연결(Attach)하고자하는 Queue의 Key값                     *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 연결(Attach) 하고자 하는 Queue의 Key 값을 받아서 해당 Queue에    *
*             _iPermission 접근 권한으로 연결하고 연결된 ID를 반환한다.        *
*             에러가 날 경우 CN_IPC_ERROR(-1)을 반환한다.                      *
*******************************************************************************/
int CNQueueOpen( int _iKey )
{
	int	iResult;

	// 1. Queue 열기시도
	iResult = msgget( _iKey, CN_IPC_QUEUE_OPEN_PERM );

	// 2. 실패 할 경우 에러를 반환
	if( iResult == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 3. 성공!!
	return	iResult;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iKey : 생성하고자 하는 Queue의 Key 값                           *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 생성(Create) 하고자 하는 Queue의 Key 값을 받아서 접근 권한이     *
*             _iPermission인 Queue를 생성하고 생성된 Queue의 ID 값을 반환한다. *
*             에러가 날 경우 CN_IPC_ERROR(-1)을 반환한다.                      *
*******************************************************************************/
int CNQueueCreate( int _iKey )
{
	int	iResult;

	// 1. Queue 생성
	iResult = msgget( _iKey, CN_IPC_QUEUE_CREATE_MODE | CN_IPC_QUEUE_OPEN_PERM );

	// 2. Queue 생성을 실패 할 경우(이미 존재 할 경우)에러를 반환
	if( iResult == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 3. 생성 성공!!
	return	iResult;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int, void*, int                                                  *
*             _iQueueID    : 데이터를 전송하고자 하는 Queue의 ID값             *
*             _vpPacket    : 전송 할 데이터의 주소                             *
*             _iPacketSize : 전송 할 데이터의 크기                             *
*                                                                              *
* Return    : int, 성공(2), 실패(0, -1)                                        *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 ID 값을 인자로 받아서 ID 가르키는 IPC Queue에        *
*             버퍼(_vpPacket)의 데이터를 _iPacketSize 크기만큼 전송한다.       *
*             기본적으로 Non-Blocking 방식으로 동작한다.                       *
*             전송에 성공 할 경우 CN_IPC_QUEUE_DATA_WRITE(2)를 반환하며        *
*             실패 할 경우 CN_IPC_QUEUE_DATA_WRITE(-1)을 반환한다.             *
*             버퍼가 가득차서 Write를 하지 못할 경우 CN_IPC_QUEUE_DATA_FULL(0) *
*             을 반환한다.                                                     *
*******************************************************************************/
long CNQueueWrite( int _iQueueID, void* _vpPacket, int _iPacketSize )
{
	int	iResult;

	// 1. Queue로 데이터 전송 요청
	while( ( iResult = msgsnd( _iQueueID, _vpPacket, _iPacketSize, IPC_NOWAIT ) ) == CN_IPC_ERROR && errno == EINTR );

	// 2. 실패!!
	if( iResult == CN_IPC_ERROR )
	{
		// 2.1 Queue가 가득찼을경우
		if( errno == EAGAIN )
			return	CN_IPC_QUEUE_DATA_FULL;
		// 2.2 Error가 발생한 경우
		else
			return	CN_IPC_QUEUE_DATA_ERROR;
	}

	// 3. 성공!!
	return	CN_IPC_QUEUE_DATA_WRITE;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int, void*, int, int                                             *
*             _iQueueID    : 전송하고자 하는 IPC Queue의 ID값                  *
*             _vpPacket    : 읽어들인 데이터를 저장 할 버퍼의 주소             *
*             _iPacketSize : 읽기 버퍼의 크기                                  *
*             _iMsgType    : 읽어 들일 데이터의 종류(Type)                     *
*                                                                              *
* Return    : int, 성공(1), 실패(0, -1)                                        *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 ID를 인자로 받아서 ID 가르키는 IPC Queue에서         *
*             _iPacketSize 크기만큼의 데이터를 버퍼(_vpPacket)에 저장한다.     *
*             _iMsgType 는 읽어들일 메시지의 종류(Type)를 나타낸다. _iMsgType  *
*             값에 따라 무조건 읽어들일지 특정 메시지만 읽어들일지가 결정난다. *
*             메시지 종류는 데이터 송수신시 데이터 구조체의 첫번째 맴버        *
*             data_type 값을 나타낸다. _iMsgType으로 0을 설정 할 경우          *
*             Queue의 가장 앞 데이터를 읽어들이며 양수로 설정 할 경우          *
*             설정한 값과 동일한 data_type의 데이터중 가장 앞의 데이터를       *
*             읽어들인다. 음수값을 설정 할 경우 절대값(부호를 뗌)으로 변경 후  *
*             절대값보다 작은수중 가장작고 가장앞의 데이터를 읽어들인다.       *
*             기본적으로 Non-Blocking 모드로 동작한다.                         *
*             읽기가 성공 할 경우 CN_IPC_QUEUE_DATA_READ(1) 을 반환하고        *
*             읽을 데이터가 없을경우 CN_IPC_QUEUE_DATA_EMPTY(0)을 반환하고     *
*             읽기 실패나 Error일 경우 CN_IPC_QUEUE_DATA_ERROR(-1) 을 반환한다.*
*******************************************************************************/
long CNQueueRead( int _iQueueID, void* _vpPacket, int _iPacketSize, int _iMsgType )
{
	int	iResult;

	// 1. Queue에 데이터 읽어들이기 요청
	while( ( iResult = msgrcv( _iQueueID, _vpPacket, _iPacketSize, _iMsgType, IPC_NOWAIT | MSG_NOERROR ) ) == CN_IPC_ERROR && errno == EINTR );

	// 2. 실패!!
	if( iResult == CN_IPC_ERROR )
	{
		// 2.1 Queue에 읽을 데이터가 없는 경우
		if( errno == ENOMSG )
			return	CN_IPC_QUEUE_DATA_EMPTY;
		// 2.2 Error가 발생한 경우
		else
			return	CN_IPC_QUEUE_DATA_ERROR;
	}

	// 3. 성공!!
	return	CN_IPC_QUEUE_DATA_READ;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int, int                                                         *
*             _iQueueID : 크기를 변경하고자 하는 IPC Queue의 ID값              *
*             _iSize    : 변경 될 Queue의 크기                                 *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 ID를 인자로 받아서 ID가 가르키는 Queue의 크기를      *
*             _iSize로 변경한다.                                               *
*             성공시 CN_IPC_SUCCESS(0),실패시 CN_IPC_ERROR(-1)을 반환한다.     *
*******************************************************************************/
int CNQueueSetSize( int _iQueueID, int _iSize )
{
	struct msqid_ds	stTempStat;

	// 1. 현재 Queue 정보를 얻어온다.
	if( msgctl( _iQueueID, IPC_STAT, &stTempStat ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 2. stTempStat 구조체의 Queue 사이즈를 _iSize 로 셋팅
	stTempStat.msg_qbytes = _iSize;

	// 3. stTempStat 정보로 Queue 정보를 셋팅
	if( msgctl( _iQueueID, IPC_SET, &stTempStat ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 4. 성공!!
	return	CN_IPC_SUCCESS;
}

