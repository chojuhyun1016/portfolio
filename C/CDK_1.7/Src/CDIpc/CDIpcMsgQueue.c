#include "CDIpcMsgQueue.h"
#include "CDIpcDefinitions.h"

#ifdef  _SOLARIS_
    #include <errno.h>
    #include <sys/msg.h>
#elif _CENT_OS_
    #include <errno.h>
    #include <sys/msg.h>
#else
    #include <errno.h>
    #include <sys/msg.h>
#endif


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iKey : 생성하고자 하는 Queue의 Key 값                           *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 생성(Create) 하고자 하는 Queue의 Key 값을 받아서 접근 권한이     *
*             _iPermission인 Queue를 생성하고 생성된 Queue의 ID 값을 반환한다. *
*             에러가 날 경우 CD_IPC_ERROR(-1)을 반환한다.                      *
*******************************************************************************/
int CDIpcMsgQueueCreate( int _iKey, int _iPermission )
{
    int iResult;

    // 1. Queue 생성
    iResult = ::msgget( _iKey, _iPermission | CD_IPC_MSG_QUEUE_CREATE_MODE );

    // 2. Queue 생성을 실패 할 경우(이미 존재 할 경우)에러를 반환
    if( iResult == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 3. 생성 성공!!
    return  iResult;
}


/*******************************************************************************
* Update    : 2012/10/25                                                       *
* Argument  : int                                                              *
*             _iKey : 연결(Attach)하고자하는 Queue의 Key값                     *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 연결(Attach) 하고자 하는 Queue의 Key 값을 받아서 해당 Queue에    *
*             연결하고 연결된 QUEUE ID를 반환한다.                             *
*             에러가 날 경우 CD_IPC_ERROR(-1)을 반환한다.                      *
*******************************************************************************/
int CDIpcMsgQueueOpen( int _iKey )
{
    int iResult;

    // 1. Queue 열기시도
    iResult = ::msgget( _iKey, 0 );

    // 2. 실패 할 경우 에러를 반환
    if( iResult == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 3. 성공!!
    return  iResult;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iQueueID : 제거할 IPC Queue의 ID값                              *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 삭제할 IPC Queue의 ID를 인자로 받아서 ID가 가르키는 IPC Queue를  *
*             삭제한다. 삭제 성공시 CD_IPC_SUCCESS(0)을 반환하며 실패 할 경우  *
*             CD_IPC_ERROR(-1)을 반환한다.                                     *
*******************************************************************************/
int CDIpcMsgQueueRemove( int _iQueueID )
{
    struct msqid_ds stStat;

    // 1. 해당 IPC Queue의 상태값(STAT)를 조회
    if( CDIpcMsgQueueGetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
    {
        // 1.1 이미 해당 Queue가 존재하지 않을경우
        if( errno == ENOENT || errno == EIDRM )
            return  CD_IPC_SUCCESS;

        // 1.2 실제 Error인 경우!!
        return  CD_IPC_ERROR;
    }

    // 2. IPC Queue를 삭제
    if( ::msgctl( _iQueueID, IPC_RMID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 3. 성공!!
    return  CD_IPC_SUCCESS;
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
*             전송에 성공 할 경우 CD_IPC_MSG_QUEUE_DATA_WRITE(2)를 반환하며    *
*             실패 할 경우 CD_IPC_ERROR(-1)을 반환한다.                        *
*             버퍼가 가득차서 Write를 하지 못할 경우                           *
*             CD_IPC_MSG_QUEUE_DATA_FULL(0) 을 반환한다.                       *
*******************************************************************************/
int CDIpcMsgQueueWrite( int _iQueueID, void* _vpPacket, int _iPacketSize )
{
    int iResult;

    // 1. Queue로 데이터 전송 요청
    while( ( iResult = ::msgsnd( _iQueueID, _vpPacket, _iPacketSize, IPC_NOWAIT ) ) == CD_IPC_ERROR && errno == EINTR );

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
*             읽기가 성공 할 경우 CD_IPC_MSG_QUEUE_DATA_READ(1) 을 반환하고    *
*             읽을 데이터가 없을경우 CD_IPC_MSG_QUEUE_DATA_EMPTY(0)을 반환하고 *
*             읽기 실패나 Error일 경우 CD_IPC_ERROR(-1) 을 반환한다.           *
*******************************************************************************/
int CDIpcMsgQueueRead( int _iQueueID, void* _vpPacket, int _iPacketSize, int _iMsgType )
{
    int iResult;

    // 1. Queue에 데이터 읽어들이기 요청
    while( ( iResult = ::msgrcv( _iQueueID, _vpPacket, _iPacketSize, _iMsgType, IPC_NOWAIT | MSG_NOERROR ) ) == CD_IPC_ERROR && errno == EINTR );

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
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iQueueID : 크기를 알고자 하는 IPC Queue의 ID값                  *
*                                                                              *
* Return    : int, 성공(Queue의 크기), 실패(-1)                                *
* Stability : MT-Safe                                                          *
* Explain   : 크기를 알고자 하는 IPC Queue의 ID를 인자로 받아서 ID가 가르키는  *
*             Queue의 크기를 반환한다.                                         *
*             성공시 Queue의 크기를 정수(int)로 반환한다.                      *
*             실패시 CD_IPC_ERROR(-1)을 반환한다.                              *
*******************************************************************************/
int CDIpcMsgQueueGetSize( int _iQueueID )
{
    struct msqid_ds stStat;

    // 1. 현재 Queue 정보를 얻어온다.
    if( CDIpcMsgQueueGetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 성공!!
    return  stStat.msg_qbytes;
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
*             성공시 CD_IPC_SUCCESS(0),실패시 CD_IPC_ERROR(-1)을 반환한다.     *
*******************************************************************************/
int CDIpcMsgQueueSetSize( int _iQueueID, int _iSize )
{
    struct msqid_ds stStat;

    // 1. 현재 Queue 정보를 얻어온다.
    if( CDIpcMsgQueueGetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. stTempStat 구조체의 Queue 사이즈를 _iSize 로 셋팅
    stStat.msg_qbytes = _iSize;

    // 3. stTempStat 정보로 Queue 정보를 셋팅
    if( CDIpcMsgQueueSetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 4. 성공!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iQueueID : 접근 권한을 알고자 하는 IPC Queue ID값               *
*                                                                              *
* Return    : int, 성공(접근권한에 해당하는 정수), 실패(-1)                    *
* Stability : MT-Safe                                                          *
* Explain   : 접근 권한을 알고자 하는 IPC Queue의 ID값을 인자로 받아서 ID가    *
*             가르키는 IPC QUEUE의 접근 권한을 얻은후 반환한다.                *
*             성공시 접근권한에 해당하는 정수(int)를 반환한다.                 *
*             실패 할 경우 CD_IPC_ERROR(-1)을 반환한다.                        *
*******************************************************************************/
int CDIpcMsgQueueGetPermission( int _iQueueID )
{
    struct msqid_ds stStat;

    // 1. 현재 Queue정보를 얻어온다.
    if( CDIpcMsgQueueGetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 성공!!
    return  stStat.msg_perm.mode;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int, int                                                         *
*             _iQueueID    : 권한을 설정하고자 하는 IPC Queue의 ID 값          *
*             _iPermission : Queue에 대한 접근권한                             *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 접근 권한을 설정하고자 하는 IPC Queue의 ID값을 인자로 받아       *
*             해당 Queue의 접근권한을 _iPermission으로 변경한다.               *
*             삭제 성공시 CD_IPC_SUCCESS(0)을 반환하며 실패 할 경우            *
*             CD_IPC_ERROR(-1)을 반환한다.                                     *
*******************************************************************************/
int CDIpcMsgQueueSetPermission( int _iQueueID, int _iPermission )
{
    struct msqid_ds stStat;

    // 1. 현재 Queue정보를 얻어온다.
    if( CDIpcMsgQueueGetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 얻어온 stStat 구조체의 Queue 접근 권한을 _iPermission 로 셋팅
    stStat.msg_perm.mode = _iPermission;

    // 3. IPC Queue Permission 변경
    if( CDIpcMsgQueueSetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 4. 성공!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int, struct msqid_ds*                                            *
*             _iQueueID : 정보를 얻고자 하는 IPC QUEUE의 ID값                  *
*             _stpStat  : 얻어온 IPC QUEUE 데이터를 저장 할 구조체             *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 정보를 얻고자 하는 IPC Queue의 ID 값을 인자로 받아서             *
*             ID 가르키는 IPC Queue의 데이터를 구조체(_stpStat)안에 저장한다.  *
*             에러가 날 경우 CD_IPC_ERROR(-1)을 반환한다.                      *
*******************************************************************************/
int CDIpcMsgQueueGetStat( int _iQueueID, struct msqid_ds* _stpStat )
{
    // 1. Queue의 정보를 추출
    if( ::msgctl( _iQueueID, IPC_STAT, _stpStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 성공!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int, struct msqid_ds*                                            *
*             _iQueueID : 설정하고자 하는 IPC QUEUE 의 ID값                    *
*             _stpStat  : 설정하고자 하는 값이 저장된 QUEUE 구조체             *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 설정하고자 하는 IPC Queue의 ID값을 인자로 받아서 ID가 가르키는   *
*             IPC Queue를 구조체(_stpStat)안의 정보로 설정한다.                *
*             성공할 경우 CD_IPC_SUCCESS(0)을 반환한다.                        *
*             에러가 날 경우 CD_IPC_ERROR(-1)을 반환한다.                      *
*******************************************************************************/
int CDIpcMsgQueueSetStat( int _iQueueID, struct msqid_ds* _stpStat )
{
    // 1. Queue의 정보를 수정
    if( ::msgctl( _iQueueID, IPC_SET, _stpStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 성공!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iQueueID : 저장된 메시지의 수를 알고자 하는 IPC Queue의 ID값    *
*                                                                              *
* Return    : int, 성공(IPC Queue에 들어있는 데이터의 개수), 실패(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : 저장된 메시지의 수를 알고자 하는 IPC Queue의 ID를 인자로 받아서  *
*             해당 ID가 가르키는 IPC Queue에 저장되어 있는 데이터의 개수를     *
*             정수(int)로 반환한다. 에러가 날 경우 CD_IPC_ERROR(-1)을 반환한다.*
*******************************************************************************/
int CDIpcMsgQueueGetCount( int _iQueueID )
{
    struct msqid_ds stStat;

    // 1. Queue의 정보를 추출
    if( CDIpcMsgQueueGetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. 성공!!
    return  stStat.msg_qnum;
}

