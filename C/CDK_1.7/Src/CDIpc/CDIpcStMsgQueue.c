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
*             _stpQueueInfo : IPC Queue�� ������ ����Ǵ� ����ü�� �ּ�        *
*             _iQueueKey    : ��� �� IPC Queue �� Key ��                      *
*             _iPerm        : Queue���� �� Queue ���� ���� ���� ��             *
*                                                                              *
* Return    : void, ����                                                       *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϴ� ����ü(stCDIpcStMsgQueue)�� �ּҰ��� *
*             �޾Ƽ� ����ü�� �� �ʵ带 �ʱ�ȭ �Ѵ�.                           *
*******************************************************************************/
void CDIpcStMsgQueueInit( stCDIpcStMsgQueue* _stpQueueInfo, int _iQueueKey, int _iPerm )
{
    // 1. �ʱ�ȭ
    ::memset( _stpQueueInfo, 0x00, sizeof( stCDIpcStMsgQueue ) );

    // 2. ����
    _stpQueueInfo->iQueueKey            = _iQueueKey;
    _stpQueueInfo->iCreatePermission    = _iPerm;

    // 3. ����!!
    return;
}


/*******************************************************************************
* Update    : 2012/10/25                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(���� �� Queue�� �ĺ���(ID)), ����(-1)                  *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stCDIpcStMsgQueue)��      *
*             �ּҰ��� ���ڷ� �޾Ƽ� ����ü������ ���� IPC Queue�� �����Ѵ�.   *
*             ������ ���� �� ��� ����ü�� Queue�� ID���� �����ϰ� ID����      *
*             ��ȯ�Ѵ�. ������ ���� �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.       *
*******************************************************************************/
int CDIpcStMsgQueueCreate( stCDIpcStMsgQueue* _stpQueueInfo )
{
    // 1. Queue ����
    _stpQueueInfo->iQueueID = ::msgget( _stpQueueInfo->iQueueKey, CD_IPC_MSG_QUEUE_CREATE_MODE | _stpQueueInfo->iCreatePermission );

    // 2. Queue ������ ���� �� ���(�̹� ���� �� ���)������ ��ȯ
    if( _stpQueueInfo->iQueueID == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 3. ���� ����!!
    return  _stpQueueInfo->iQueueID;
}


/*******************************************************************************
* Update    : 2012/10/25                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(���� �� Queue�� �ĺ���(ID)), ����(-1)                  *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stCDIpcStMsgQueue)��      *
*             �ּҰ��� ���ڷ� �޾Ƽ� ����ü������ ���� IPC Queue�� �����Ѵ�.   *
*             ���⸦ ���� �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                 *
*******************************************************************************/
int CDIpcStMsgQueueOpen( stCDIpcStMsgQueue* _stpQueueInfo )
{
    // 1. Queue ����õ�
    _stpQueueInfo->iQueueID = ::msgget( _stpQueueInfo->iQueueKey, 0 );

    // 2. ���� �� ��� ������ ��ȯ
    if( _stpQueueInfo->iQueueID == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 3. ����!!
    return  _stpQueueInfo->iQueueID;
}


/*******************************************************************************
* Update    : 2012/10/25                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stCDIpcStMsgQueue)��      *
*             �ּҸ� ���ڷ� �޾Ƽ� ����ü �����Ͱ� ����Ű�� IPC Queue��        *
*             �����Ѵ�. ���� ������ CD_IPC_SUCCESS(0)�� ��ȯ�ϸ� ���� �� ���  *
*             CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                                     *
*******************************************************************************/
int CDIpcStMsgQueueRemove( stCDIpcStMsgQueue* _stpQueueInfo )
{
    // 1. �ش� IPC Queue�� ���°�(STAT)�� ��ȸ
    if( CDIpcStMsgQueueGetStat( _stpQueueInfo, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
    {
        // 1.1 �̹� ������ Queue�� �ƴϸ� ���� ��ȯ
        if( errno != ENOENT && errno != EIDRM )
            return  CD_IPC_ERROR;
    }

    // 2. IPC Queue�� ����
    if( ::msgctl( _stpQueueInfo->iQueueID, IPC_RMID, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 3. Queue�� �����Ǿ����Ƿ� Queue ID�� �ʱ�ȭ
    _stpQueueInfo->iQueueID = 0;

    // 4. Qeueue�� �����Ǿ����Ƿ� Queue �������� ����ü�� ����
    ::memset( &(_stpQueueInfo->stMsgQueueStat), 0x00, sizeof( struct msqid_ds ) );

    // 5. ����!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stCDIpcStMsgQueue*, void*, int                                   *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*             _vpPacket     : ���� �� �������� �ּ�                            *
*             _iPacketSize  : ���� �� �������� ũ��                            *
*                                                                              *
* Return    : int, ����(IPC Queue�� ����ִ� �������� ����), ����(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stCDIpcStMsgQueue)��      *
*             �ּҸ� ���ڷ� �޾Ƽ� ����ü �����Ͱ� ����Ű�� IPC Queue��        *
*             ����(_vpPacket)�� �����͸� _iPacketSize ũ�⸸ŭ �����Ѵ�.       *
*             �⺻������ Non-Blocking ������� �����Ѵ�.                       *
*             ���ۿ� ���� �� ��� CD_IPC_MSG_QUEUE_DATA_WRITE(2)�� ��ȯ�ϸ�    *
*             ���� �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                        *
*             ���۰� �������� Write�� ���� ���� ���                           *
*             CD_IPC_MSG_QUEUE_DATA_FULL(0) �� ��ȯ�ȴ�.                       *
*******************************************************************************/
long CDIpcStMsgQueueWrite( stCDIpcStMsgQueue* _stpQueueInfo, void* _vpPacket, int _iPacketSize )
{
    int iResult;

    // 1. Queue�� ������ ���� ��û
    while( ( iResult = ::msgsnd( _stpQueueInfo->iQueueID, _vpPacket, _iPacketSize, IPC_NOWAIT ) ) == CD_IPC_ERROR && errno == EINTR );

    // 2. ����!!
    if( iResult == CD_IPC_ERROR )
    {
        // 2.1 Queue�� ����á�����
        if( errno == EAGAIN )
            return  CD_IPC_MSG_QUEUE_DATA_FULL;
        // 2.2 Error�� �߻��� ���
        else
            return  CD_IPC_ERROR;
    }

    // 3. ����!!
    return  CD_IPC_MSG_QUEUE_DATA_WRITE;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stCDIpcStMsgQueue*, void*, int, int                              *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*             _vpPacket     : �о���� �����͸� ���� �� ������ �ּ�            *
*             _iPacketSize  : �б� ������ ũ��                                 *
*             _iMsgType     : �о� ���� �������� ����(Type)                    *
*                                                                              *
* Return    : int, ����(IPC Queue�� ����ִ� �������� ����), ����(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stCDIpcStMsgQueue)��      *
*             �ּҸ� ���ڷ� �޾Ƽ� ����ü �����Ͱ� ����Ű�� IPC Queue����      *
*             _iPacketSize ũ�⸸ŭ�� �����͸� ����(_vpPacket)�� �����Ѵ�.     *
*             _iMsgType �� �о���� �޽����� ����(Type)�� ��Ÿ����. _iMsgType  *
*             ���� ���� ������ �о������ Ư�� �޽����� �о�������� ��������. *
*             �޽��� ������ ������ �ۼ��Ž� ������ ����ü�� ù��° �ɹ�        *
*             data_type ���� ��Ÿ����. _iMsgType���� 0�� ���� �� ���          *
*             Queue�� ���� �� �����͸� �о���̸� ����� ���� �� ���          *
*             ������ ���� ������ data_type�� �������� ���� ���� �����͸�       *
*             �о���δ�. �������� ���� �� ��� ���밪(��ȣ�� ��)���� ���� ��  *
*             ���밪���� �������� �����۰� ������� �����͸� �о���δ�.       *
*             �⺻������ Non-Blocking ���� �����Ѵ�.                         *
*             �бⰡ ���� �� ��� CD_IPC_MSG_QUEUE_DATA_READ(1) �� ��ȯ�ϰ�    *
*             ���� �����Ͱ� ������� CD_IPC_MSG_QUEUE_DATA_EMPTY(0)�� ��ȯ�ϰ� *
*             �б� ���г� Error�� ��� CD_IPC_ERROR(-1) �� ��ȯ�Ѵ�.           *
*******************************************************************************/
long CDIpcStMsgQueueRead( stCDIpcStMsgQueue* _stpQueueInfo, void* _vpPacket, int _iPacketSize, int _iMsgType )
{
    int iResult;

    // 1. Queue�� ������ �о���̱� ��û
    while( ( iResult = ::msgrcv( _stpQueueInfo->iQueueID, _vpPacket, _iPacketSize, _iMsgType, IPC_NOWAIT | MSG_NOERROR ) ) == CD_IPC_ERROR && errno == EINTR );

    // 2. ����!!
    if( iResult == CD_IPC_ERROR )
    {
        // 2.1 Queue�� ���� �����Ͱ� ���� ���
        if( errno == ENOMSG )
            return  CD_IPC_MSG_QUEUE_DATA_EMPTY;
        // 2.2 Error�� �߻��� ���
        else
            return  CD_IPC_ERROR;
    }

    // 3. ����!!
    return  CD_IPC_MSG_QUEUE_DATA_READ;
}


/*******************************************************************************
* Update    : 2011/12/19                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stCDIpcStMsgQueue)��      *
*             �ּҸ� ���ڷ� �޾� �ش� Queue�� �� ũ�⸦ ��ȯ�Ѵ�.              *
*             ������ Queue�� ũ��(_stpQueueInfo->stMsgQueueStat.msg_qbytes)��  *
*             ��ȯ�ϰ� ���� �� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                    *
*******************************************************************************/
int CDIpcStMsgQueueGetSize( stCDIpcStMsgQueue* _stpQueueInfo )
{
    // 1. ���� Queue ������ ���´�.
    if( CDIpcStMsgQueueGetStat( _stpQueueInfo, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ����!!
    return  _stpQueueInfo->stMsgQueueStat.msg_qbytes;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*             _iSize        : Queue�� ũ��                                     *
*                                                                              *
* Return    : int, ����(����:ť��ũ��), ����(-1)                               *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stCDIpcStMsgQueue)��      *
*             �ּҿ� ũ��(_iSize)�� ���ڷ� �޾� �ش� Queue�� �� ũ�⸦ _iSize��*
*             �����Ѵ�. stTempStat �� �����Ͽ� Queue �� ������ ����            *
*             �����ϴ� ������ _stpQueueInfo ����ü�� �׻� ���� Queue�� ���¸�  *
*             �����Ͽ��� �ϸ� ���� ���н� ���� ������ ������ ������Ű��        *
*             �����̴�.                                                        *
*             ���� ������ CD_IPC_SUCCESS(0)�� ��ȯ�ϸ� ���� �� ���            *
*             CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                                     *
*******************************************************************************/
int CDIpcStMsgQueueSetSize( stCDIpcStMsgQueue* _stpQueueInfo, int _iSize )
{
    struct msqid_ds stTempStat;

    // 1. ���� Queue ������ ���´�.
    if( CDIpcStMsgQueueGetStat( _stpQueueInfo, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ���� Queue�� ������ stTempStat�� ����
    ::memcpy( &stTempStat, &(_stpQueueInfo->stMsgQueueStat), sizeof( struct msqid_ds ) );

    // 3. stTempStat ����ü�� Queue ����� _iSize �� ����
    stTempStat.msg_qbytes = _iSize;

    // 4. stTempStat ������ Queue ������ ����
    if( CDIpcStMsgQueueSetStat( _stpQueueInfo, &stTempStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 5. ����!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/12/19                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(����:���ѿ� �ش��ϴ� ����), ����(-1)                   *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stCDIpcStMsgQueue)�� �ּ� *
*             ���ڷ� �޾� �ش� Queue�� ���ٱ����� ��ȯ�Ѵ�.                    *
*             ���� ������ ���ٱ���(_stpQueueInfo->stMsgQueueStat.msg_perm.mode)*
*             �� ��ȯ�ϸ� ���� �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.            *
*******************************************************************************/
int CDIpcStMsgQueueGetPermission( stCDIpcStMsgQueue* _stpQueueInfo )
{
    // 1. ���� Queue������ ���´�.
    if( CDIpcStMsgQueueGetStat( _stpQueueInfo, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ����!!
    return  _stpQueueInfo->stMsgQueueStat.msg_perm.mode;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*             _iPermission  : Queue�� ���� ���ٱ���                            *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stCDIpcStMsgQueue)��      *
*             �ּҿ� ���ٱ���(_iPermission)�� ���ڷ� �޾� �ش� Queue��         *
*             ���ٱ����� _iPermission���� �����Ѵ�. stTempStat �� �����Ͽ�     *
*             Queue �� ������ ���� �����ϴ� ������ _stpQueueInfo ����ü�� �׻� *
*             ���� Queue�� ���¸� �����Ͽ��� �ϸ� ���� ���н� ���� ������      *
*             ������ ������Ű�� �����̴�.                                      *
*             ���� ������ CD_IPC_SUCCESS(0)�� ��ȯ�ϸ� ���� �� ���            *
*             CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                                     *
*******************************************************************************/
int CDIpcStMsgQueueSetPermission( stCDIpcStMsgQueue* _stpQueueInfo, int _iPermission )
{
    struct msqid_ds stTempStat;

    // 1. ���� Queue������ ���´�.
    if( CDIpcStMsgQueueGetStat( _stpQueueInfo, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ���� Queue�� ������ stTempStat�� ����
    ::memcpy( &stTempStat, &(_stpQueueInfo->stMsgQueueStat), sizeof( struct msqid_ds ) );

    // 3. stTempStat ����ü�� Queue ���� ������ _iPermission �� ����
    stTempStat.msg_perm.mode = _iPermission;

    // 4. IPC Queue Permission ����
    if( CDIpcStMsgQueueSetStat( _stpQueueInfo, &stTempStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 5. ����!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/10/30                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stCDIpcStMsgQueue)��      *
*             �ּҸ� ���ڷ� �޾Ƽ� ����ü �����Ͱ� ����Ű�� IPC Queue��        *
*             �����͸� �о�ͼ� ����ü ����(_stpMsqidBuffer)�ȿ�  �����Ѵ�.    *
*             ������ �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                      *
*******************************************************************************/
int CDIpcStMsgQueueGetStat( stCDIpcStMsgQueue* _stpQueueInfo, struct msqid_ds* _stpMsqidBuffer )
{
    // 1. Queue�� ������ ����
    if( ::msgctl( _stpQueueInfo->iQueueID, IPC_STAT, _stpMsqidBuffer ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ����!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/10/30                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*             _stpMsqidInfo : Queue �� �Ӽ� ������ �����ϰ� �ִ� ����ü�� �ּ� *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stCDIpcStMsgQueue)��      *
*             �ּҸ� ���ڷ� �޾Ƽ� ����ü �����Ͱ� ����Ű�� IPC Queue��        *
*             �Ӽ��� _stpMsqidInfo ����ü�� ���� �� �Ӽ����� �����Ѵ�.         *
*             ������ �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                      *
*******************************************************************************/
int CDIpcStMsgQueueSetStat( stCDIpcStMsgQueue* _stpQueueInfo, struct msqid_ds* _stpMsqidInfo )
{
    // 1. Queue�� ������ ����
    if( ::msgctl( _stpQueueInfo->iQueueID, IPC_SET, _stpMsqidInfo ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ������ ��� _stpQueueInfo ����ü ���� msqid_ds ����ü�� ������Ʈ
    ::memcpy( &(_stpQueueInfo->stMsgQueueStat), _stpMsqidInfo, sizeof(struct msqid_ds) );

    // 3. ����!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stCDIpcStMsgQueue*                                               *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(IPC Queue�� ����ִ� �������� ����), ����(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stCDIpcStMsgQueue)��      *
*             �ּҸ� ���ڷ� �޾Ƽ� ����ü �����Ͱ� ����Ű�� IPC Queue��        *
*             ����Ǿ� �ִ� �������� ������ ��ȯ�Ѵ�.                          *
*             ������ �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                      *
*******************************************************************************/
int CDIpcStMsgQueueGetCount( stCDIpcStMsgQueue* _stpQueueInfo )
{
    // 1. Queue�� ������ ����
    if( ::msgctl( _stpQueueInfo->iQueueID, IPC_STAT, &(_stpQueueInfo->stMsgQueueStat) ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ����!!
    return  _stpQueueInfo->stMsgQueueStat.msg_qnum;
}
