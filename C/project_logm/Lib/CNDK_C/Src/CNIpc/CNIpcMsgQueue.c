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
*             _stpQueueInfo : IPC Queue�� ������ ����Ǵ� ����ü�� �ּ�        *
*             _iQueueKey    : ��� �� IPC Queue �� Key ��                      *
*             _iMaxCount    : Queue�� ���� �� �������� �ִ� ����               *
*                                                                              *
* Return    : void, ����                                                       *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϴ� ����ü(stQueueInfo)�� �ּҰ��� �޾Ƽ�*
*             ����ü�� �� �ʵ带 �ʱ�ȭ �Ѵ�.                                  *
*******************************************************************************/
void CNMsgQueueInit( stQueueInfo* _stpQueueInfo, int _iQueueKey, int _iMaxCount, int _iMode, int _iPerm )
{
	// 1. �ʱ�ȭ
	memset( _stpQueueInfo, 0x00, sizeof( stQueueInfo ) );

	// 2. ����
	_stpQueueInfo->iQueueKey		= _iQueueKey;
	_stpQueueInfo->iMaxCount		= _iMaxCount;

	_stpQueueInfo->iCreateMode		= _iMode;
	_stpQueueInfo->iOpenPermission	= _iPerm;

	// 3. ����!!
	return;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(���� �� Queue�� �ĺ���(ID)), ����(-1)                  *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stQueueInfo)�� �ּҰ���   *
*             ���ڷ� �޾Ƽ� ����ü������ ���� IPC Queue�� �����Ѵ�.            *
*             ������ ���� �� ��� ����ü�� Queue�� ID���� �����ϰ� ID����      *
*             ��ȯ�Ѵ�. ������ ���� �� ��� CN_IPC_ERROR(-1)�� ��ȯ�Ѵ�.       *
*******************************************************************************/
int CNMsgQueueCreate( stQueueInfo* _stpQueueInfo )
{
	// 1. Queue ����
	_stpQueueInfo->iQueueID = msgget( _stpQueueInfo->iQueueKey, _stpQueueInfo->iCreateMode | _stpQueueInfo->iOpenPermission );

	// 2. Queue ������ ���� �� ���(�̹� ���� �� ���)������ ��ȯ
	if( _stpQueueInfo->iQueueID == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 3. ���� ����!!
	return	_stpQueueInfo->iQueueID;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(���� �� Queue�� �ĺ���(ID)), ����(-1)                  *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stQueueInfo)�� �ּҰ���   *
*             ���ڷ� �޾Ƽ� ����ü������ ���� IPC Queue�� ����.                *
*             ���⸦ ���� �� ��� CN_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                 *
*******************************************************************************/
int CNMsgQueueOpen( stQueueInfo* _stpQueueInfo )
{
	// 1. Queue ����õ�
	_stpQueueInfo->iQueueID = msgget( _stpQueueInfo->iQueueKey, _stpQueueInfo->iOpenPermission );

	// 2. ���� �� ��� ������ ��ȯ
	if( _stpQueueInfo->iQueueID == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 3. ����!!
	return	_stpQueueInfo->iQueueID;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stQueueInfo)�� �ּҸ�     *
*             ���ڷ� �޾Ƽ� ����ü �����Ͱ� ����Ű�� IPC Queue�� �����͸�      *
*             ��� �̾Ƴ���. IPC Queue�� �����Ͱ� ��� �������                *
*             CN_IPC_SUCCESS(0)�� ��ȯ�ϸ� ������ �� ��� CN_IPC_ERROR(-1)��   *
*             ��ȯ�Ѵ�.                                                        *
*******************************************************************************/
int CNMsgQueueClean( stQueueInfo* _stpQueueInfo )
{
	// 1. Queue�� ������ ī��Ʈ�� 0�϶����� �ݺ�
	while( CNMsgQueueGetCount( _stpQueueInfo ) )
	{
		// 1.1 Queue���� �޽����� �̾Ƴ��� ������
		if( CNMsgQueueRead( _stpQueueInfo, NULL, NULL, NULL ) == CN_IPC_ERROR )
			return	CN_IPC_ERROR;
	}

	// 2. ����!!
	return	CN_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stQueueInfo)�� �ּҸ�     *
*             ���ڷ� �޾Ƽ� ����ü �����Ͱ� ����Ű�� IPC Queue�� �����Ѵ�.     *
*             ���� ������ CN_IPC_SUCCESS(0)�� ��ȯ�ϸ� ���� �� ���            *
*             CN_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                                     *
*******************************************************************************/
int CNMsgQueueRemove( stQueueInfo* _stpQueueInfo )
{
	// 1. �ش� IPC Queue�� ���°�(STAT)�� ��ȸ
	if( CNMsgQueueGetStat( _stpQueueInfo ) == CN_IPC_ERROR )
	{
		// 1.1 �̹� �ش� Queue�� �������� �������
		if( errno == ENOENT || errno == EIDRM )
			return	CN_IPC_SUCCESS;

		// 1.2 ���� Error�� ���!!
		return	CN_IPC_ERROR;
	}

	// 2. IPC Queue�� ����
	if( msgctl( _stpQueueInfo->iQueueID, IPC_RMID, &(_stpQueueInfo->stMsgQueueStat) ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 3. ����!!
	return	CN_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*             _iSize        : Queue�� ũ��                                     *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stQueueInfo)�� �ּҿ�     *
*             ũ��(_iSize)�� ���ڷ� �޾� �ش� Queue�� �� ũ�⸦ _iSize��       *
*             �����Ѵ�. stTempStat �� �����Ͽ� Queue �� ������ ����            *
*             �����ϴ� ������ _stpQueueInfo ����ü�� �׻� ���� Queue�� ���¸�  *
*             �����Ͽ��� �ϸ� ���� ���н� ���� ������ ������ ������Ű��        *
*             �����̴�.                                                        *
*             ���� ������ CN_IPC_SUCCESS(0)�� ��ȯ�ϸ� ���� �� ���            *
*             CN_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                                     *
*******************************************************************************/
int CNMsgQueueSetSize( stQueueInfo* _stpQueueInfo, int _iSize )
{
	struct msqid_ds	stTempStat;

	// 1. ���� Queue ������ ���´�.
	if( CNMsgQueueGetStat( _stpQueueInfo ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 2. ���� Queue�� ������ stTempStat�� ����
	memcpy( &stTempStat, &(_stpQueueInfo->stMsgQueueStat), sizeof( struct msqid_ds ) );

	// 3. stTempStat ����ü�� Queue ����� _iSize �� ����
	stTempStat.msg_qbytes = _iSize;

	// 4. stTempStat ������ Queue ������ ����
	if( msgctl( _stpQueueInfo->iQueueID, IPC_SET, &stTempStat ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 5. ������ ������ ��� stTempStat�� �����͸� _stpQueueInfo->stMsgQueueStat�� ����
	memcpy( &(_stpQueueInfo->stMsgQueueStat), &stTempStat, sizeof( struct msqid_ds ) );

	// 6. ����!!
	return	CN_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*             _iPermission  : Queue�� ���� ���ٱ���                            *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stQueueInfo)�� �ּҿ�     *
*             ���ٱ���(_iPermission)�� ���ڷ� �޾� �ش� Queue�� ���ٱ�����     *
*             _iPermission���� �����Ѵ�. stTempStat �� �����Ͽ� Queue ��       *
*             ������ ���� �����ϴ� ������ _stpQueueInfo ����ü�� �׻� ����     *
*             Queue�� ���¸� �����Ͽ��� �ϸ� ���� ���н� ���� ������ ������    *
*             ������Ű�� �����̴�.                                             *
*             ���� ������ CN_IPC_SUCCESS(0)�� ��ȯ�ϸ� ���� �� ���            *
*             CN_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                                     *
*******************************************************************************/
int CNMsgQueueSetPermission( stQueueInfo* _stpQueueInfo, int _iPermission )
{
	struct msqid_ds	stTempStat;

	// 1. ���� Queue������ ���´�.
	if( CNMsgQueueGetStat( _stpQueueInfo ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 2. ���� Queue�� ������ stTempStat�� ����
	memcpy( &stTempStat, &(_stpQueueInfo->stMsgQueueStat), sizeof( struct msqid_ds ) );

	// 3. stTempStat ����ü�� Queue ���� ������ _iPermission �� ����
	stTempStat.msg_perm.mode = _iPermission;

	// 4. IPC Queue Permission ����
	if( msgctl( _stpQueueInfo->iQueueID, IPC_SET, &stTempStat ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 5. ������ ������ ��� stTempStat�� �����͸� _stpQueueInfo->stMsgQueueStat�� ����
	memcpy( &(_stpQueueInfo->stMsgQueueStat), &stTempStat, sizeof( struct msqid_ds ) );

	// 6. ����!!
	return	CN_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stQueueInfo)�� �ּҸ�     *
*             ���ڷ� �޾Ƽ� ����ü �����Ͱ� ����Ű�� IPC Queue�� �����͸�      *
*             ����ü(_stpQueueInfo->stMsgQueueStat)�ȿ� �����Ѵ�.              *
*             ������ �� ��� CN_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                      *
*******************************************************************************/
int CNMsgQueueGetStat( stQueueInfo* _stpQueueInfo )
{
	// 1. Queue�� ������ ����
	if( msgctl( _stpQueueInfo->iQueueID, IPC_STAT, &(_stpQueueInfo->stMsgQueueStat) ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 2. ����!!
	return	CN_IPC_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*                                                     *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*                                                                              *
* Return    : int, ����(IPC Queue�� ����ִ� �������� ����), ����(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stQueueInfo)�� �ּҸ�     *
*             ���ڷ� �޾Ƽ� ����ü �����Ͱ� ����Ű�� IPC Queue�� ����Ǿ� �ִ� *
*             �������� ������ ��ȯ�Ѵ�. ������ �� ��� CN_IPC_ERROR(-1)��      *
*             ��ȯ�Ѵ�.                                                        *
*******************************************************************************/
int CNMsgQueueGetCount( stQueueInfo* _stpQueueInfo )
{
	// 1. Queue�� ������ ����
	if( msgctl( _stpQueueInfo->iQueueID, IPC_STAT, &(_stpQueueInfo->stMsgQueueStat) ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 2. ����!!
	return	_stpQueueInfo->stMsgQueueStat.msg_qnum;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*, void*, int                                         *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*             _vpPacket     : ���� �� �������� �ּ�                            *
*             _iPacketSize  : ���� �� �������� ũ��                            *
*                                                                              *
* Return    : int, ����(IPC Queue�� ����ִ� �������� ����), ����(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stQueueInfo)�� �ּҸ�     *
*             ���ڷ� �޾Ƽ� ����ü �����Ͱ� ����Ű�� IPC Queue��               *
*             ����(_vpPacket)�� �����͸� _iPacketSize ũ�⸸ŭ �����Ѵ�.       *
*             �⺻������ Non-Blocking ������� �����Ѵ�.                       *
*             ���ۿ� ���� �� ��� CN_IPC_QUEUE_DATA_WRITE(2)�� ��ȯ�ϸ�        *
*             ���� �� ��� CN_IPC_QUEUE_DATA_ERROR(-1)�� ��ȯ�Ѵ�.             *
*             ���۰� �������� Write�� ���� ���� ��� CN_IPC_QUEUE_DATA_FULL(0) *
*             �� ��ȯ�ȴ�.                                                     *
*******************************************************************************/
long CNMsgQueueWrite( stQueueInfo* _stpQueueInfo, void* _vpPacket, int _iPacketSize )
{
	int	iResult;

	// 1. Queue�� ������ ���� ��û
	while( ( iResult = msgsnd( _stpQueueInfo->iQueueID, _vpPacket, _iPacketSize, IPC_NOWAIT ) ) == CN_IPC_ERROR && errno == EINTR );

	// 2. ����!!
	if( iResult == CN_IPC_ERROR )
	{
		// 2.1 Queue�� ����á�����
		if( errno == EAGAIN )
			return	CN_IPC_QUEUE_DATA_FULL;
		// 2.2 Error�� �߻��� ���
		else
			return	CN_IPC_QUEUE_DATA_ERROR;
	}

	// 3. ����!!
	return	CN_IPC_QUEUE_DATA_WRITE;
}


/*******************************************************************************
* Update    : 2011/08/08                                                       *
* Argument  : stQueueInfo*, void*, int, int                                    *
*             _stpQueueInfo : IPC Queue�� ������ ����Ǿ��ִ� ����ü�� �ּ�    *
*             _vpPacket     : �о���� �����͸� ���� �� ������ �ּ�            *
*             _iPacketSize  : �б� ������ ũ��                                 *
*             _iMsgType     : �о� ���� �������� ����(Type)                    *
*                                                                              *
* Return    : int, ����(IPC Queue�� ����ִ� �������� ����), ����(-1)          *
* Stability : MT-Safe                                                          *
* Explain   : IPC Queue�� ������ �����ϰ��ִ� ����ü(stQueueInfo)�� �ּҸ�     *
*             ���ڷ� �޾Ƽ� ����ü �����Ͱ� ����Ű�� IPC Queue����             *
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
*             �бⰡ ���� �� ��� CN_IPC_QUEUE_DATA_READ(1) �� ��ȯ�ϰ�        *
*             ���� �����Ͱ� ������� CN_IPC_QUEUE_DATA_EMPTY(0)�� ��ȯ�ϰ�     *
*             �б� ���г� Error�� ��� CN_IPC_QUEUE_DATA_ERROR(-1) �� ��ȯ�Ѵ�.*
*******************************************************************************/
long CNMsgQueueRead( stQueueInfo* _stpQueueInfo, void* _vpPacket, int _iPacketSize, int _iMsgType )
{
	int	iResult;

	// 1. Queue�� ������ �о���̱� ��û
	while( ( iResult = msgrcv( _stpQueueInfo->iQueueID, _vpPacket, _iPacketSize, _iMsgType, IPC_NOWAIT | MSG_NOERROR ) ) == CN_IPC_ERROR && errno == EINTR );

	// 2. ����!!
	if( iResult == CN_IPC_ERROR )
	{
		// 2.1 Queue�� ���� �����Ͱ� ���� ���
		if( errno == ENOMSG )
			return	CN_IPC_QUEUE_DATA_EMPTY;
		// 2.2 Error�� �߻��� ���
		else
			return	CN_IPC_QUEUE_DATA_ERROR;
	}

	// 3. ����!!
	return	CN_IPC_QUEUE_DATA_READ;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iKey : ����(Attach)�ϰ����ϴ� Queue�� Key��                     *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : ����(Attach) �ϰ��� �ϴ� Queue�� Key ���� �޾Ƽ� �ش� Queue��    *
*             _iPermission ���� �������� �����ϰ� ����� ID�� ��ȯ�Ѵ�.        *
*             ������ �� ��� CN_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                      *
*******************************************************************************/
int CNQueueOpen( int _iKey )
{
	int	iResult;

	// 1. Queue ����õ�
	iResult = msgget( _iKey, CN_IPC_QUEUE_OPEN_PERM );

	// 2. ���� �� ��� ������ ��ȯ
	if( iResult == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 3. ����!!
	return	iResult;
}


/*******************************************************************************
* Update    : 2011/12/13                                                       *
* Argument  : int                                                              *
*             _iKey : �����ϰ��� �ϴ� Queue�� Key ��                           *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : ����(Create) �ϰ��� �ϴ� Queue�� Key ���� �޾Ƽ� ���� ������     *
*             _iPermission�� Queue�� �����ϰ� ������ Queue�� ID ���� ��ȯ�Ѵ�. *
*             ������ �� ��� CN_IPC_ERROR(-1)�� ��ȯ�Ѵ�.                      *
*******************************************************************************/
int CNQueueCreate( int _iKey )
{
	int	iResult;

	// 1. Queue ����
	iResult = msgget( _iKey, CN_IPC_QUEUE_CREATE_MODE | CN_IPC_QUEUE_OPEN_PERM );

	// 2. Queue ������ ���� �� ���(�̹� ���� �� ���)������ ��ȯ
	if( iResult == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 3. ���� ����!!
	return	iResult;
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
*             ���ۿ� ���� �� ��� CN_IPC_QUEUE_DATA_WRITE(2)�� ��ȯ�ϸ�        *
*             ���� �� ��� CN_IPC_QUEUE_DATA_WRITE(-1)�� ��ȯ�Ѵ�.             *
*             ���۰� �������� Write�� ���� ���� ��� CN_IPC_QUEUE_DATA_FULL(0) *
*             �� ��ȯ�Ѵ�.                                                     *
*******************************************************************************/
long CNQueueWrite( int _iQueueID, void* _vpPacket, int _iPacketSize )
{
	int	iResult;

	// 1. Queue�� ������ ���� ��û
	while( ( iResult = msgsnd( _iQueueID, _vpPacket, _iPacketSize, IPC_NOWAIT ) ) == CN_IPC_ERROR && errno == EINTR );

	// 2. ����!!
	if( iResult == CN_IPC_ERROR )
	{
		// 2.1 Queue�� ����á�����
		if( errno == EAGAIN )
			return	CN_IPC_QUEUE_DATA_FULL;
		// 2.2 Error�� �߻��� ���
		else
			return	CN_IPC_QUEUE_DATA_ERROR;
	}

	// 3. ����!!
	return	CN_IPC_QUEUE_DATA_WRITE;
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
*             �бⰡ ���� �� ��� CN_IPC_QUEUE_DATA_READ(1) �� ��ȯ�ϰ�        *
*             ���� �����Ͱ� ������� CN_IPC_QUEUE_DATA_EMPTY(0)�� ��ȯ�ϰ�     *
*             �б� ���г� Error�� ��� CN_IPC_QUEUE_DATA_ERROR(-1) �� ��ȯ�Ѵ�.*
*******************************************************************************/
long CNQueueRead( int _iQueueID, void* _vpPacket, int _iPacketSize, int _iMsgType )
{
	int	iResult;

	// 1. Queue�� ������ �о���̱� ��û
	while( ( iResult = msgrcv( _iQueueID, _vpPacket, _iPacketSize, _iMsgType, IPC_NOWAIT | MSG_NOERROR ) ) == CN_IPC_ERROR && errno == EINTR );

	// 2. ����!!
	if( iResult == CN_IPC_ERROR )
	{
		// 2.1 Queue�� ���� �����Ͱ� ���� ���
		if( errno == ENOMSG )
			return	CN_IPC_QUEUE_DATA_EMPTY;
		// 2.2 Error�� �߻��� ���
		else
			return	CN_IPC_QUEUE_DATA_ERROR;
	}

	// 3. ����!!
	return	CN_IPC_QUEUE_DATA_READ;
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
*             ������ CN_IPC_SUCCESS(0),���н� CN_IPC_ERROR(-1)�� ��ȯ�Ѵ�.     *
*******************************************************************************/
int CNQueueSetSize( int _iQueueID, int _iSize )
{
	struct msqid_ds	stTempStat;

	// 1. ���� Queue ������ ���´�.
	if( msgctl( _iQueueID, IPC_STAT, &stTempStat ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 2. stTempStat ����ü�� Queue ����� _iSize �� ����
	stTempStat.msg_qbytes = _iSize;

	// 3. stTempStat ������ Queue ������ ����
	if( msgctl( _iQueueID, IPC_SET, &stTempStat ) == CN_IPC_ERROR )
		return	CN_IPC_ERROR;

	// 4. ����!!
	return	CN_IPC_SUCCESS;
}
