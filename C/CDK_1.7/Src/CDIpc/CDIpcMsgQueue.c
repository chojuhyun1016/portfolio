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
*             _iKey : �����ϰ��� �ϴ� Queue�� Key ��                           *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : ����(Create) �ϰ��� �ϴ� Queue�� Key ���� �޾Ƽ� ���� ������     *
*             _iPermission�� Queue�� �����ϰ� ������ Queue�� ID ���� ��ȯ�Ѵ�. *
*             ������ �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                      *
*******************************************************************************/
int CDIpcMsgQueueCreate( int _iKey, int _iPermission )
{
    int iResult;

    // 1. Queue ����
    iResult = ::msgget( _iKey, _iPermission | CD_IPC_MSG_QUEUE_CREATE_MODE );

    // 2. Queue ������ ���� �� ���(�̹� ���� �� ���)������ ��ȯ
    if( iResult == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 3. ���� ����!!
    return  iResult;
}


/*******************************************************************************
* Update    : 2012/10/25                                                       *
* Argument  : int                                                              *
*             _iKey : ����(Attach)�ϰ����ϴ� Queue�� Key��                     *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : ����(Attach) �ϰ��� �ϴ� Queue�� Key ���� �޾Ƽ� �ش� Queue��    *
*             �����ϰ� ����� QUEUE ID�� ��ȯ�Ѵ�.                             *
*             ������ �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                      *
*******************************************************************************/
int CDIpcMsgQueueOpen( int _iKey )
{
    int iResult;

    // 1. Queue ����õ�
    iResult = ::msgget( _iKey, 0 );

    // 2. ���� �� ��� ������ ��ȯ
    if( iResult == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 3. ����!!
    return  iResult;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iQueueID : ������ IPC Queue�� ID��                              *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : ������ IPC Queue�� ID�� ���ڷ� �޾Ƽ� ID�� ����Ű�� IPC Queue��  *
*             �����Ѵ�. ���� ������ CD_IPC_SUCCESS(0)�� ��ȯ�ϸ� ���� �� ���  *
*             CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                                     *
*******************************************************************************/
int CDIpcMsgQueueRemove( int _iQueueID )
{
    struct msqid_ds stStat;

    // 1. �ش� IPC Queue�� ���°�(STAT)�� ��ȸ
    if( CDIpcMsgQueueGetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
    {
        // 1.1 �̹� �ش� Queue�� �������� �������
        if( errno == ENOENT || errno == EIDRM )
            return  CD_IPC_SUCCESS;

        // 1.2 ���� Error�� ���!!
        return  CD_IPC_ERROR;
    }

    // 2. IPC Queue�� ����
    if( ::msgctl( _iQueueID, IPC_RMID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 3. ����!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int, void*, int                                                  *
*             _iQueueID    : �����͸� �����ϰ��� �ϴ� Queue�� ID��             *
*             _vpPacket    : ���� �� �������� �ּ�                             *
*             _iPacketSize : ���� �� �������� ũ��                             *
*                                                                              *
* Return    : int, ����(2), ����(0, -1)                                        *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ID ���� ���ڷ� �޾Ƽ� ID ����Ű�� IPC Queue��        *
*             ����(_vpPacket)�� �����͸� _iPacketSize ũ�⸸ŭ �����Ѵ�.       *
*             �⺻������ Non-Blocking ������� �����Ѵ�.                       *
*             ���ۿ� ���� �� ��� CD_IPC_MSG_QUEUE_DATA_WRITE(2)�� ��ȯ�ϸ�    *
*             ���� �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                        *
*             ���۰� �������� Write�� ���� ���� ���                           *
*             CD_IPC_MSG_QUEUE_DATA_FULL(0) �� ��ȯ�Ѵ�.                       *
*******************************************************************************/
int CDIpcMsgQueueWrite( int _iQueueID, void* _vpPacket, int _iPacketSize )
{
    int iResult;

    // 1. Queue�� ������ ���� ��û
    while( ( iResult = ::msgsnd( _iQueueID, _vpPacket, _iPacketSize, IPC_NOWAIT ) ) == CD_IPC_ERROR && errno == EINTR );

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
* Update    : 2011/12/13                                                       *
* Argument  : int, void*, int, int                                             *
*             _iQueueID    : �����ϰ��� �ϴ� IPC Queue�� ID��                  *
*             _vpPacket    : �о���� �����͸� ���� �� ������ �ּ�             *
*             _iPacketSize : �б� ������ ũ��                                  *
*             _iMsgType    : �о� ���� �������� ����(Type)                     *
*                                                                              *
* Return    : int, ����(1), ����(0, -1)                                        *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ID�� ���ڷ� �޾Ƽ� ID ����Ű�� IPC Queue����         *
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
int CDIpcMsgQueueRead( int _iQueueID, void* _vpPacket, int _iPacketSize, int _iMsgType )
{
    int iResult;

    // 1. Queue�� ������ �о���̱� ��û
    while( ( iResult = ::msgrcv( _iQueueID, _vpPacket, _iPacketSize, _iMsgType, IPC_NOWAIT | MSG_NOERROR ) ) == CD_IPC_ERROR && errno == EINTR );

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
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iQueueID : ũ�⸦ �˰��� �ϴ� IPC Queue�� ID��                  *
*                                                                              *
* Return    : int, ����(Queue�� ũ��), ����(-1)                                *
* Stability : MT-Safe                                                          *
* Explain   : ũ�⸦ �˰��� �ϴ� IPC Queue�� ID�� ���ڷ� �޾Ƽ� ID�� ����Ű��  *
*             Queue�� ũ�⸦ ��ȯ�Ѵ�.                                         *
*             ������ Queue�� ũ�⸦ ����(int)�� ��ȯ�Ѵ�.                      *
*             ���н� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                              *
*******************************************************************************/
int CDIpcMsgQueueGetSize( int _iQueueID )
{
    struct msqid_ds stStat;

    // 1. ���� Queue ������ ���´�.
    if( CDIpcMsgQueueGetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ����!!
    return  stStat.msg_qbytes;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int, int                                                         *
*             _iQueueID : ũ�⸦ �����ϰ��� �ϴ� IPC Queue�� ID��              *
*             _iSize    : ���� �� Queue�� ũ��                                 *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ID�� ���ڷ� �޾Ƽ� ID�� ����Ű�� Queue�� ũ�⸦      *
*             _iSize�� �����Ѵ�.                                               *
*             ������ CD_IPC_SUCCESS(0),���н� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.     *
*******************************************************************************/
int CDIpcMsgQueueSetSize( int _iQueueID, int _iSize )
{
    struct msqid_ds stStat;

    // 1. ���� Queue ������ ���´�.
    if( CDIpcMsgQueueGetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. stTempStat ����ü�� Queue ����� _iSize �� ����
    stStat.msg_qbytes = _iSize;

    // 3. stTempStat ������ Queue ������ ����
    if( CDIpcMsgQueueSetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 4. ����!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iQueueID : ���� ������ �˰��� �ϴ� IPC Queue ID��               *
*                                                                              *
* Return    : int, ����(���ٱ��ѿ� �ش��ϴ� ����), ����(-1)                    *
* Stability : MT-Safe                                                          *
* Explain   : ���� ������ �˰��� �ϴ� IPC Queue�� ID���� ���ڷ� �޾Ƽ� ID��    *
*             ����Ű�� IPC QUEUE�� ���� ������ ������ ��ȯ�Ѵ�.                *
*             ������ ���ٱ��ѿ� �ش��ϴ� ����(int)�� ��ȯ�Ѵ�.                 *
*             ���� �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                        *
*******************************************************************************/
int CDIpcMsgQueueGetPermission( int _iQueueID )
{
    struct msqid_ds stStat;

    // 1. ���� Queue������ ���´�.
    if( CDIpcMsgQueueGetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ����!!
    return  stStat.msg_perm.mode;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int, int                                                         *
*             _iQueueID    : ������ �����ϰ��� �ϴ� IPC Queue�� ID ��          *
*             _iPermission : Queue�� ���� ���ٱ���                             *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : ���� ������ �����ϰ��� �ϴ� IPC Queue�� ID���� ���ڷ� �޾�       *
*             �ش� Queue�� ���ٱ����� _iPermission���� �����Ѵ�.               *
*             ���� ������ CD_IPC_SUCCESS(0)�� ��ȯ�ϸ� ���� �� ���            *
*             CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                                     *
*******************************************************************************/
int CDIpcMsgQueueSetPermission( int _iQueueID, int _iPermission )
{
    struct msqid_ds stStat;

    // 1. ���� Queue������ ���´�.
    if( CDIpcMsgQueueGetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ���� stStat ����ü�� Queue ���� ������ _iPermission �� ����
    stStat.msg_perm.mode = _iPermission;

    // 3. IPC Queue Permission ����
    if( CDIpcMsgQueueSetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 4. ����!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int, struct msqid_ds*                                            *
*             _iQueueID : ������ ����� �ϴ� IPC QUEUE�� ID��                  *
*             _stpStat  : ���� IPC QUEUE �����͸� ���� �� ����ü             *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : ������ ����� �ϴ� IPC Queue�� ID ���� ���ڷ� �޾Ƽ�             *
*             ID ����Ű�� IPC Queue�� �����͸� ����ü(_stpStat)�ȿ� �����Ѵ�.  *
*             ������ �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                      *
*******************************************************************************/
int CDIpcMsgQueueGetStat( int _iQueueID, struct msqid_ds* _stpStat )
{
    // 1. Queue�� ������ ����
    if( ::msgctl( _iQueueID, IPC_STAT, _stpStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ����!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int, struct msqid_ds*                                            *
*             _iQueueID : �����ϰ��� �ϴ� IPC QUEUE �� ID��                    *
*             _stpStat  : �����ϰ��� �ϴ� ���� ����� QUEUE ����ü             *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : �����ϰ��� �ϴ� IPC Queue�� ID���� ���ڷ� �޾Ƽ� ID�� ����Ű��   *
*             IPC Queue�� ����ü(_stpStat)���� ������ �����Ѵ�.                *
*             ������ ��� CD_IPC_SUCCESS(0)�� ��ȯ�Ѵ�.                        *
*             ������ �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                      *
*******************************************************************************/
int CDIpcMsgQueueSetStat( int _iQueueID, struct msqid_ds* _stpStat )
{
    // 1. Queue�� ������ ����
    if( ::msgctl( _iQueueID, IPC_SET, _stpStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ����!!
    return  CD_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iQueueID : ����� �޽����� ���� �˰��� �ϴ� IPC Queue�� ID��    *
*                                                                              *
* Return    : int, ����(IPC Queue�� ����ִ� �������� ����), ����(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : ����� �޽����� ���� �˰��� �ϴ� IPC Queue�� ID�� ���ڷ� �޾Ƽ�  *
*             �ش� ID�� ����Ű�� IPC Queue�� ����Ǿ� �ִ� �������� ������     *
*             ����(int)�� ��ȯ�Ѵ�. ������ �� ��� CD_IPC_ERROR(-1)�� ��ȯ�Ѵ�.*
*******************************************************************************/
int CDIpcMsgQueueGetCount( int _iQueueID )
{
    struct msqid_ds stStat;

    // 1. Queue�� ������ ����
    if( CDIpcMsgQueueGetStat( _iQueueID, &stStat ) == CD_IPC_ERROR )
        return  CD_IPC_ERROR;

    // 2. ����!!
    return  stStat.msg_qnum;
}
