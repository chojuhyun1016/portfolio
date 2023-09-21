#include "CDIpcMsgQueue.h"
#include "CDIpcStMsgQueue.h"
#include "CDIpcDefinitions.h"

#ifdef  _SOLARIS_
    #include <errno.h>
    #include <string.h>
    #include <sys/msg.h>
#elif _CENT_OS_
    #include <errno.h>
    #include <string.h>
    #include <sys/msg.h>
#else
    #include <errno.h>
    #include <string.h>
    #include <sys/msg.h>
#endif


/*******************************************************************************
* Update    : 2012/10/25                                                       *
* Argument  : stCDIpcStMsgQueue*, int, int                                     *
*             _stpQueueInfo : IPC Queue의 정보가 저장되는 구조체의 주소        *
*             _iQueueKey    : 사용 할 IPC Queue 의 Key 값                      *
*             _iPerm        : Queue생성 시 Queue 접근 권한 설정 값             *
*                                                                              *
* Return    : void, 없음                                                       *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하는 구조체(stCDIpcStMsgQueue)의 주소값을 *
*             받아서 구조체의 각 필드를 초기화 한다.                           *
*******************************************************************************/
void CDIpcStMsgQueueInit( stCDIpcStMsgQueue* _stpQueueInfo, int _iQueueKey, int _iPerm )
{
    // 1. 초기화
    ::memset( _stpQueueInfo, 0x00, sizeof( stCDIpcStMsgQueue ) );

    // 2. 셋팅
    _stpQueueInfo->iQueueKey            = _iQueueKey;
    _stpQueueInfo->iCreatePermission    = _iPerm;

    // 3. 종료!!
    return;
}


/*******************************************************************************
* Update    : 2012/10/25                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(생성 된 Queue의 식별자(ID)), 실패(-1)                  *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stCDIpcStMsgQueue)의      *
*             주소값을 인자로 받아서 구조체정보에 따라 IPC Queue를 생성한다.   *
*             생성이 성공 할 경우 구조체에 Queue의 ID값을 저장하고 ID값을      *
*             반환한다. 생성을 실패 할 경우 CD_IPC_ERROR(-1)을 반환한다.       *
*******************************************************************************/
int CDIpcStMsgQueueCreate( stCDIpcStMsgQueue* _stpQueueInfo )
{
    // 1. Queue 생성
    _stpQueueInfo->iQueueID = ::msgget( _stpQueueInfo->iQueueKey, CD_IPC_MSG_QUEUE_CREATE_MODE | _stpQueueInfo->iCreatePermission );

    // 2. Queue 생성을 실패 할 경우(이미 존재 할 경우)에러를 반환
    if( _stpQueueInfo->iQueueID == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 3. 생성 성공!!
    return  _stpQueueInfo->iQueueID;
}


/*******************************************************************************
* Update    : 2012/10/25                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(생성 된 Queue의 식별자(ID)), 실패(-1)                  *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stCDIpcStMsgQueue)의      *
*             주소값을 인자로 받아서 구조체정보에 따라 IPC Queue를 연결한다.   *
*             열기를 실패 할 경우 CD_IPC_ERROR(-1)을 반환한다.                 *
*******************************************************************************/
int CDIpcStMsgQueueOpen( stCDIpcStMsgQueue* _stpQueueInfo )
{
    // 1. Queue 열기시도
    _stpQueueInfo->iQueueID = ::msgget( _stpQueueInfo->iQueueKey, 0 );

    // 2. 실패 할 경우 에러를 반환
    if( _stpQueueInfo->iQueueID == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 3. 성공!!
    return  _stpQueueInfo->iQueueID;
}


/*******************************************************************************
* Update    : 2012/10/25                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stCDIpcStMsgQueue)의      *
*             주소를 인자로 받아서 구조체 데이터가 가르키는 IPC Queue를        *
*             삭제한다. 삭제 성공시 CD_IPC_SUCCESS(0)을 반환하며 실패 할 경우  *
*             CD_IPC_ERROR(-1)을 반환한다.                                     *
*******************************************************************************/
int CDIpcStMsgQueueRemove( stCDIpcStMsgQueue* _stpQueueInfo )
{
    // 1. 해당 IPC Queue의 상태값(STAT)를 조회
    if( CDIpcStMsgQueueGetStat( _stpQueueInfo, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
    {
        // 1.1 이미 삭제된 Queue가 아니면 오류 반환
        if( errno != ENOENT && errno != EIDRM )
            return  CD_IPC_ERROR;
    }

    // 2. IPC Queue를 삭제
    if( ::msgctl( _stpQueueInfo->iQueueID, IPC_RMID, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 3. Queue가 삭제되었으므로 Queue ID를 초기화
    _stpQueueInfo->iQueueID = 0;

    // 4. Qeueue가 삭제되었으므로 Queue 내부정보 구조체는 삭제
    ::memset( &(_stpQueueInfo->stMsgQueueStat), 0x00, sizeof( struct msqid_ds ) );

    // 5. 성공!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stCDIpcStMsgQueue*, void*, int                                   *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*             _vpPacket     : 전송 할 데이터의 주소                            *
*             _iPacketSize  : 전송 할 데이터의 크기                            *
*                                                                              *
* Return    : int, 성공(IPC Queue에 들어있는 데이터의 개수), 실패(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stCDIpcStMsgQueue)의      *
*             주소를 인자로 받아서 구조체 데이터가 가르키는 IPC Queue에        *
*             버퍼(_vpPacket)의 데이터를 _iPacketSize 크기만큼 전송한다.       *
*             기본적으로 Non-Blocking 방식으로 동작한다.                       *
*             전송에 성공 할 경우 CD_IPC_MSG_QUEUE_DATA_WRITE(2)를 반환하며    *
*             실패 할 경우 CD_IPC_ERROR(-1)을 반환한다.                        *
*             버퍼가 가득차서 Write를 하지 못할 경우                           *
*             CD_IPC_MSG_QUEUE_DATA_FULL(0) 을 반환안다.                       *
*******************************************************************************/
long CDIpcStMsgQueueWrite( stCDIpcStMsgQueue* _stpQueueInfo, void* _vpPacket, int _iPacketSize )
{
    int iResult;

    // 1. Queue로 데이터 전송 요청
    while( ( iResult = ::msgsnd( _stpQueueInfo->iQueueID, _vpPacket, _iPacketSize, IPC_NOWAIT ) ) == CD_IPC_ERROR && errno == EINTR );

    // 2. 실패!!
    if( iResult == CD_IPC_ERROR )
    {
        // 2.1 Queue가 가득찼을경우
        if( errno == EAGAIN )
            return  CD_IPC_MSG_QUEUE_DATA_FULL;
        // 2.2 Error가 발생한 경우
        else
            return  CD_IPC_ERROR;
    }

    // 3. 성공!!
    return  CD_IPC_MSG_QUEUE_DATA_WRITE;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stCDIpcStMsgQueue*, void*, int, int                              *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*             _vpPacket     : 읽어들인 데이터를 저장 할 버퍼의 주소            *
*             _iPacketSize  : 읽기 버퍼의 크기                                 *
*             _iMsgType     : 읽어 들일 데이터의 종류(Type)                    *
*                                                                              *
* Return    : int, 성공(IPC Queue에 들어있는 데이터의 개수), 실패(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stCDIpcStMsgQueue)의      *
*             주소를 인자로 받아서 구조체 데이터가 가르키는 IPC Queue에서      *
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
*             읽기가 성공 할 경우 CD_IPC_MSG_QUEUE_DATA_READ(1) 을 반환하고    *
*             읽을 데이터가 없을경우 CD_IPC_MSG_QUEUE_DATA_EMPTY(0)을 반환하고 *
*             읽기 실패나 Error일 경우 CD_IPC_ERROR(-1) 을 반환한다.           *
*******************************************************************************/
long CDIpcStMsgQueueRead( stCDIpcStMsgQueue* _stpQueueInfo, void* _vpPacket, int _iPacketSize, int _iMsgType )
{
    int iResult;

    // 1. Queue에 데이터 읽어들이기 요청
    while( ( iResult = ::msgrcv( _stpQueueInfo->iQueueID, _vpPacket, _iPacketSize, _iMsgType, IPC_NOWAIT | MSG_NOERROR ) ) == CD_IPC_ERROR && errno == EINTR );

    // 2. 실패!!
    if( iResult == CD_IPC_ERROR )
    {
        // 2.1 Queue에 읽을 데이터가 없는 경우
        if( errno == ENOMSG )
            return  CD_IPC_MSG_QUEUE_DATA_EMPTY;
        // 2.2 Error가 발생한 경우
        else
            return  CD_IPC_ERROR;
    }

    // 3. 성공!!
    return  CD_IPC_MSG_QUEUE_DATA_READ;
}


/*******************************************************************************
* Update    : 2011/12/19                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stCDIpcStMsgQueue)의      *
*             주소를 인자로 받아 해당 Queue의 총 크기를 반환한다.              *
*             성공시 Queue의 크기(_stpQueueInfo->stMsgQueueStat.msg_qbytes)를  *
*             반환하고 실패 시 CD_IPC_ERROR(-1)을 반환한다.                    *
*******************************************************************************/
int CDIpcStMsgQueueGetSize( stCDIpcStMsgQueue* _stpQueueInfo )
{
    // 1. 현재 Queue 정보를 얻어온다.
    if( CDIpcStMsgQueueGetStat( _stpQueueInfo, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 성공!!
    return  _stpQueueInfo->stMsgQueueStat.msg_qbytes;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*             _iSize        : Queue의 크기                                     *
*                                                                              *
* Return    : int, 성공(정수:큐의크기), 실패(-1)                               *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stCDIpcStMsgQueue)의      *
*             주소와 크기(_iSize)를 인자로 받아 해당 Queue의 총 크기를 _iSize로*
*             변경한다. stTempStat 를 선언하여 Queue 의 정보를 따로            *
*             저장하는 이유는 _stpQueueInfo 구조체는 항상 현재 Queue의 상태를  *
*             저장하여야 하며 변경 실패시 실패 이전의 값으로 복구시키기        *
*             위함이다.                                                        *
*             변경 성공시 CD_IPC_SUCCESS(0)을 반환하며 실패 할 경우            *
*             CD_IPC_ERROR(-1)을 반환한다.                                     *
*******************************************************************************/
int CDIpcStMsgQueueSetSize( stCDIpcStMsgQueue* _stpQueueInfo, int _iSize )
{
    struct msqid_ds stTempStat;

    // 1. 현재 Queue 정보를 얻어온다.
    if( CDIpcStMsgQueueGetStat( _stpQueueInfo, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 얻어온 Queue의 정보를 stTempStat에 복사
    ::memcpy( &stTempStat, &(_stpQueueInfo->stMsgQueueStat), sizeof( struct msqid_ds ) );

    // 3. stTempStat 구조체의 Queue 사이즈를 _iSize 로 셋팅
    stTempStat.msg_qbytes = _iSize;

    // 4. stTempStat 정보로 Queue 정보를 셋팅
    if( CDIpcStMsgQueueSetStat( _stpQueueInfo, &stTempStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 5. 성공!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/12/19                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(정수:권한에 해당하는 정수), 실패(-1)                   *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stCDIpcStMsgQueue)의 주소 *
*             인자로 받아 해당 Queue의 접근권한을 반환한다.                    *
*             삭제 성공시 접근권한(_stpQueueInfo->stMsgQueueStat.msg_perm.mode)*
*             을 반환하며 실패 할 경우 CD_IPC_ERROR(-1)을 반환한다.            *
*******************************************************************************/
int CDIpcStMsgQueueGetPermission( stCDIpcStMsgQueue* _stpQueueInfo )
{
    // 1. 현재 Queue정보를 얻어온다.
    if( CDIpcStMsgQueueGetStat( _stpQueueInfo, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 성공!!
    return  _stpQueueInfo->stMsgQueueStat.msg_perm.mode;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*             _iPermission  : Queue에 대한 접근권한                            *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stCDIpcStMsgQueue)의      *
*             주소와 접근권한(_iPermission)를 인자로 받아 해당 Queue의         *
*             접근권한을 _iPermission으로 변경한다. stTempStat 를 선언하여     *
*             Queue 의 정보를 따로 저장하는 이유는 _stpQueueInfo 구조체는 항상 *
*             현재 Queue의 상태를 저장하여야 하며 변경 실패시 실패 이전의      *
*             값으로 복구시키기 위함이다.                                      *
*             삭제 성공시 CD_IPC_SUCCESS(0)을 반환하며 실패 할 경우            *
*             CD_IPC_ERROR(-1)을 반환한다.                                     *
*******************************************************************************/
int CDIpcStMsgQueueSetPermission( stCDIpcStMsgQueue* _stpQueueInfo, int _iPermission )
{
    struct msqid_ds stTempStat;

    // 1. 현재 Queue정보를 얻어온다.
    if( CDIpcStMsgQueueGetStat( _stpQueueInfo, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 얻어온 Queue의 정보를 stTempStat에 복사
    ::memcpy( &stTempStat, &(_stpQueueInfo->stMsgQueueStat), sizeof( struct msqid_ds ) );

    // 3. stTempStat 구조체의 Queue 접근 권한을 _iPermission 로 셋팅
    stTempStat.msg_perm.mode = _iPermission;

    // 4. IPC Queue Permission 변경
    if( CDIpcStMsgQueueSetStat( _stpQueueInfo, &stTempStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 5. 성공!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/10/30                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stCDIpcStMsgQueue)의      *
*             주소를 인자로 받아서 구조체 데이터가 가르키는 IPC Queue의        *
*             데이터를 읽어와서 구조체 버퍼(_stpMsqidBuffer)안에  저장한다.    *
*             에러가 날 경우 CD_IPC_ERROR(-1)을 반환한다.                      *
*******************************************************************************/
int CDIpcStMsgQueueGetStat( stCDIpcStMsgQueue* _stpQueueInfo, struct msqid_ds* _stpMsqidBuffer )
{
    // 1. Queue의 정보를 추출
    if( ::msgctl( _stpQueueInfo->iQueueID, IPC_STAT, _stpMsqidBuffer ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 성공!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/10/30                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*             _stpMsqidInfo : Queue 의 속성 정보를 저장하고 있는 구조체의 주소 *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stCDIpcStMsgQueue)의      *
*             주소를 인자로 받아서 구조체 데이터가 가르키는 IPC Queue의        *
*             속성을 _stpMsqidInfo 구조체에 정의 된 속성으로 변경한다.         *
*             에러가 날 경우 CD_IPC_ERROR(-1)을 반환한다.                      *
*******************************************************************************/
int CDIpcStMsgQueueSetStat( stCDIpcStMsgQueue* _stpQueueInfo, struct msqid_ds* _stpMsqidInfo )
{
    // 1. Queue의 정보를 수정
    if( ::msgctl( _stpQueueInfo->iQueueID, IPC_SET, _stpMsqidInfo ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 성공인 경우 _stpQueueInfo 구조체 안의 msqid_ds 구조체를 업데이트
    ::memcpy( &(_stpQueueInfo->stMsgQueueStat), _stpMsqidInfo, sizeof(struct msqid_ds) );

    // 3. 성공!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue의 정보가 저장되어있는 구조체의 주소    *
*                                                                              *
* Return    : int, 성공(IPC Queue에 들어있는 데이터의 개수), 실패(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue의 정보를 저장하고있는 구조체(stCDIpcStMsgQueue)의      *
*             주소를 인자로 받아서 구조체 데이터가 가르키는 IPC Queue에        *
*             저장되어 있는 데이터의 개수를 반환한다.                          *
*             에러가 날 경우 CD_IPC_ERROR(-1)을 반환한다.                      *
*******************************************************************************/
int CDIpcStMsgQueueGetCount( stCDIpcStMsgQueue* _stpQueueInfo )
{
    // 1. Queue의 정보를 추출
    if( ::msgctl( _stpQueueInfo->iQueueID, IPC_STAT, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 성공!!
    return  _stpQueueInfo->stMsgQueueStat.msg_qnum;
}

