#include "CDStructureQueue.h"

#ifdef  _SOLARIS_
    #include <stdlib.h>
    #include <string.h>
    #include <pthread.h>
#elif _CENT_OS_
    #include <stdlib.h>
    #include <string.h>
    #include <pthread.h>
#else
    #include <stdlib.h>
    #include <string.h>
    #include <pthread.h>
#endif


/*******************************************************************************
* Update    : 2012/06/29                                                       *
* Argument  : ����                                                             *
*                                                                              *
* Return    : stCDStructQueueContainer*, ����(stCDStructQueueContainer*),      *
*             ����(NULL)                                                       *
* Stability : MT-Safe                                                          *
* Explain   : LocalQueue ������Ʈ(����ü)�� heap ������ �����ϰ� ������        *
*             ������Ʈ�� �ּҸ� ��ȯ�Ѵ�.                                      *
*             ������ ��� ������Ʈ�� �ּ�(stCDStructureQueue*)�� ��ȯ�ϸ�      *
*             ������ ��� NULL �� ��ȯ�Ѵ�. LocalQueue ������Ʈ�� ��������     *
*             �����ϸ� ���� Thread���� �̻� ������ �Ҽ� �����Ƿ� �����̳�      *
*             ����� �ݵ�� MakeLocalQueue() �Լ��� ���ؼ� �����ϰ� ����Ѵ�.  *
*******************************************************************************/
stCDStructureQueue* CDStructureQueueMake()
{
	stCDStructureQueue	*stpLocalQueue;

	if( ( stpLocalQueue = ( stCDStructureQueue* )malloc( sizeof( stCDStructureQueue ) ) ) == NULL )
		return NULL;

	memset( stpLocalQueue, 0x00, sizeof(stCDStructureQueue) );

	return	stpLocalQueue;
}


/*******************************************************************************
* Update    : 2012/06/29                                                       *
* Argument  : stCDStructureQueue*, int                                         *
*             _stpQueue : �ʱ�ȭ �ϰ��� �ϴ� LocalQueue ������Ʈ(����ü)�� �ּ�*
*             _iPrepare : ������ Queue �������� ��                             *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : �ʱ�ȭ �ϰ��� �ϴ� Queue ������Ʈ(����ü) �ּҰ�(_stpQueue)��    *
*             ���ڷ� �޾Ƽ� �ش� ������Ʈ�� �ʱ�ȭ(NULL)�� ������              *
*             ����ȭ ����(mutex)���� �ʱ�� �� _iPrepare(�ʱ� Queue ���� ��)   *
*             ��ŭ �����͸� �����Ѵ�. �ִ� ť�� ���緮�� _iPrepare�� �ȴ�.     *
*******************************************************************************/
int CDStructureQueueInit( stCDStructureQueue* _stpQueue, int _iPrepare )
{
	int	iAppendLoop;

	if( _stpQueue == NULL )
		return	CD_STRUCTURE_ERROR;

	if( _iPrepare < 0 )
		return	CD_STRUCTURE_ERROR;

	// 1. mutex ���� �ʱ�ȭ
	pthread_mutex_init( &(_stpQueue->m_iLock), NULL );

	// 2. mutex ���
	pthread_mutex_lock( &(_stpQueue->m_iLock) );

	// 3. LocalQueue ������Ʈ ���� Queue �ʱ�ȭ(����� List, ���� List)
	CDStructQueueInitLinkedList( &(_stpQueue->m_stOccupiedList) );
	CDStructQueueInitLinkedList( &(_stpQueue->m_stIdleList) );

	// 4. _iPrepare ��ŭ LocalQueue ���� ����
	//    ���� ����(m_stIdleList)�� �����Ѵ�.
	for( iAppendLoop = 0; iAppendLoop < _iPrepare; iAppendLoop++ )
	{
		// 4.1 �����͸� �����ؼ� Linkedlist ���ۿ� Append
		if( CDStructQueueAppendTailLinkedList(  &(_stpQueue->m_stIdleList) ) == CD_STRUCTURE_ERROR )
		{
			pthread_mutex_unlock( &(_stpQueue->m_iLock) );

			return	CD_STRUCTURE_ERROR;
		}
	}

	// 5. mutex ��� ����
	pthread_mutex_unlock( &(_stpQueue->m_iLock) );

	// 6. ����!!
	return	CD_STRUCTURE_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/11/21                                                       *
* Argument  : stCDStructureQueue**                                             *
*             _stppQueue : ���� �ϰ��� �ϴ� LocalQueue ������Ʈ(����ü)�� �ּ� *
*                          �� �����ϰ� �ִ� ������ �ּ�(�������� ������)       *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : ����(����) �ϰ��� �ϴ� Queue ������Ʈ(����ü) �ּ�(_stppQueue)�� *
*             ������ ������ �ּ�(�������� ������)�� ���ڷ� �޾Ƽ� �ش�         *
*             ������Ʈ���� �����͸� ���� ����(����)�Ѵ�.                       *
*             ������Ʈ���� Queue �����͸� ��� ����(����)�� ��                 *
*             ������Ʈ ���� �ʱ�ȭ�Ѵ�.                                        *
*******************************************************************************/
void CDStructureQueueFree( stCDStructureQueue** _stppQueue )
{
	if( _stppQueue == NULL )
		return;

	if( *_stppQueue == NULL )
		return;

	// 1. mutex ���
	pthread_mutex_lock( &((*_stppQueue)->m_iLock) );

	// 2. LocalQueue ���� ��� ������(Queue) �����͸� ����(����)
	CDStructQueueDeleteAllLinkedList( &((*_stppQueue)->m_stOccupiedList) );
	CDStructQueueDeleteAllLinkedList( &((*_stppQueue)->m_stIdleList) );

	// 3. mutex ��� ����
	pthread_mutex_unlock( &((*_stppQueue)->m_iLock) );

	// 4. mutex ���� ����
	pthread_mutex_destroy( &((*_stppQueue)->m_iLock) );

	// 5. Heap ������ �Ҵ� �� Queue ������Ʈ ����
	free( *_stppQueue );

	// 6. ������Ʈ�� �����Ǿ����Ƿ� NULL ����
	*_stppQueue	= NULL;

	// 7. ����!!
	return;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureQueue*, int                                         *
*             _stpQueue     : Ȯ�� �ϰ��� �ϴ� Queue ������Ʈ(����ü)�� �ּ�   *
*             _iAppendCount : Ȯ�� �� �������� ��                              *
*                                                                              *
* Return    : int, �߰� �Ҵ� �� ������ ��                                      *
* Stability : MT-Safe                                                          *
* Explain   : Ȯ�� �ϰ��� �ϴ� Queue ������Ʈ(����ü)                          *
*             �ּҰ�(_stpQueue)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ��                *
*             ������ۿ� iAppendCount ��ŭ ���۸� �߰� �Ҵ��Ѵ�.               *
*             ��������� ũ�⸸ŭ �����͸� �����Ҽ� �����Ƿ� ���� ������       *
*             Ȯ���� ��ü Queue�� ����� �ø��� ���̴�.                      *
*             ���ڰ�(_stpQueue, _iAppendCount)�� �߸��� ��츦 �����ϰ���      *
*             �߰��� �Ҵ��� �������� ������ ��ȯ�Ѵ�.                          *
*             ���ڰ��� �߸� �� ��쿡�� CD_STRUCTURE_ERROR(-1)�� ��ȯ�Ѵ�.     *
*******************************************************************************/
int CDStructureQueueAppend( stCDStructureQueue* _stpQueue, int _iAppendCount )
{
	int	iAppendLoop;

	if( _stpQueue == NULL )
		return	CD_STRUCTURE_ERROR;

	if( _iAppendCount < 0 )
		return	CD_STRUCTURE_ERROR;

	// 1. mutex ���
	pthread_mutex_lock( &(_stpQueue->m_iLock) );

	// 2. �߰��� �Ҵ��ϴ� ���ڸ�ŭ ������ ���鼭 ������� �߰� �Ҵ�
	for( iAppendLoop = 0; iAppendLoop < _iAppendCount; iAppendLoop++ )
	{
		// 2.1 ������� �Ѱ� �߰� �Ҵ�
		if( CDStructQueueAppendTailLinkedList(  &(_stpQueue->m_stIdleList) ) == CD_STRUCTURE_ERROR )
		{
			pthread_mutex_unlock( &(_stpQueue->m_iLock) );

			return	iAppendLoop;
		}
	}

	// 3. mutex ��� ����
	pthread_mutex_unlock( &(_stpQueue->m_iLock) );

	// 4. ����!!
	return	iAppendLoop;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureQueue*, int                                         *
*             _stpQueue      : ���� ��Ű���� �ϴ� Queue ������Ʈ(����ü)�� �ּ�*
*             _iDeleteCount : ���ҽ�ų ���� ���� �������� ��                   *
*                                                                              *
* Return    : int, ���� �� ������ ��                                           *
* Stability : MT-Safe                                                          *
* Explain   : ���ҽ�Ű���� �ϴ� Queue ������Ʈ(����ü)                         *
*             �ּҰ�(_stpQueue)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ��                *
*             ������ۿ� _iDeleteCount ��ŭ ���۸� ����(����)��Ų��.           *
*             ��������� ũ�⸸ŭ �����͸� �����Ҽ� �����Ƿ� ���� ������       *
*             ���Ҵ� ��ü Queue�� ����� ���̴� ���̴�.                      *
*             ���ڰ�(_stpQueue, _iDeleteCount)�� �߸��� ��츦 �����ϰ���      *
*             ���� �� �������� ������ ��ȯ�Ѵ�.                                *
*             ���ڰ��� �߸� �� ��쿡�� CD_STRUCTURE_ERROR(-1)�� ��ȯ�Ѵ�.     *
*******************************************************************************/
int CDStructureQueueDelete( stCDStructureQueue* _stpQueue, int _iDeleteCount )
{
	int	iDepleteLoop;
	int	iDepleteMaxLoop;

	if( _stpQueue == NULL )
		return	CD_STRUCTURE_ERROR;

	if( _iDeleteCount < 0 )
		return	CD_STRUCTURE_ERROR;

	iDepleteMaxLoop	= _iDeleteCount;

	// 1. ���� ������ ������ ������ ���̰����ϴ� �������� ������
	//    ���̰��� �ϴ� �������� ������ ���� �������� ������ ����
	if(  _stpQueue->m_stIdleList.m_reference < iDepleteMaxLoop )
		iDepleteMaxLoop	= _stpQueue->m_stIdleList.m_reference;

	// 2. mutex ���
	pthread_mutex_lock( &(_stpQueue->m_iLock) );

	// 3. �߰��� �Ҵ��ϴ� ���ڸ�ŭ ������ ���鼭 ���� ���� ����
	for( iDepleteLoop = 0; iDepleteLoop < iDepleteMaxLoop; iDepleteLoop++ )
	{
		// 3.1 ���� ���� ������ �Ѱ� ����
		if( CDStructQueueRemoveTailLinkedList(  &(_stpQueue->m_stIdleList) ) == CD_STRUCTURE_ERROR )
		{
			pthread_mutex_unlock( &(_stpQueue->m_iLock) );

			return	iDepleteLoop;
		}
	}

	// 4. mutex ��� ����
	pthread_mutex_unlock( &(_stpQueue->m_iLock) );

	// 5. ����!!
	return	iDepleteLoop;
}


/*******************************************************************************
* Update    : 2012/06/29                                                       *
* Argument  : stCDStructureQueue*, int, void*                                  *
*             _stpQueue : �����͸� ����(Insert, Push) �ϰ��� �ϴ�              *
*                         Queue ������Ʈ(����ü) �ּ�                          *
*             _iType    : �����ϴ� Queue �������� Type                         *
*             _vpData   : �����ϰ��� �ϴ� �������� �ּ�                        *
*                                                                              *
* Return    : int, ����(3), ����(1, -1)                                        *
* Stability : MT-Safe                                                          *
* Explain   : �����͸� �����ϰ��� �ϴ� Queue�� ������Ʈ(����ü) �ּ�(_stpQueue)*
*             �� ���ڷ� �޾Ƽ� �ش� ������Ʈ�� ������(_vpData)�� �����Ѵ�.     *
*             ���Խ� �ش� �������� ����(Type)�� �Բ� ���Եȴ�.                 *
*             Pop �Լ��� ���� �����͸� �ٽ� �̾Ƴ� �ÿ� Queue�� ����(_iType)�� *
*             ������(_vpData)�� �̾Ƴ��� �ȴ�.                                 *
*             LocalQueue�� ���� Queue�� �������� �ּ�(Pointer)���� �����Ѵ�.   *
*******************************************************************************/
int CDStructureQueuePush( stCDStructureQueue* _stpQueue, int _iType, void* _vpData )
{
	stCDStructQueueContainer		*dummy;

	// 1. mutex ���
	pthread_mutex_lock( &(_stpQueue->m_iLock) );

	// 2. LocalQueue�ȿ� �����͸� ���� �� ����(���� Queue, m_stIdleList)��
	//    ���� ���(Queue Full)
	if( _stpQueue->m_stIdleList.m_reference <= 0 )
	{
		pthread_mutex_unlock( &(_stpQueue->m_iLock) );

		return	CD_STRUCTURE_QUEUE_FULL;
	}

	// 3. �������(m_stIdleList)���� �����͸� ���� �� ����(dummy)�� �̾Ƴ���
	if( ( dummy = CDStructQueuePopLinkedList( &(_stpQueue->m_stIdleList) ) ) == NULL )
	{
		pthread_mutex_unlock( &(_stpQueue->m_iLock) );

		return	CD_STRUCTURE_ERROR;
	}

	// 4. ���ۿ� ���ڷ� ���� �����Ͱ��� ����
	dummy->type	= _iType;
	dummy->data	= _vpData;

	// 5. �������(���� Queue, m_stOccupiedList)�� �ش� �����͸� ����
	if( ( CDStructQueuePushLinkedList( &(_stpQueue->m_stOccupiedList), dummy ) ) == NULL)
	{
		pthread_mutex_unlock( &(_stpQueue->m_iLock) );

		return	CD_STRUCTURE_ERROR;
	}

	// 6. mutex ��� ����
	pthread_mutex_unlock( &(_stpQueue->m_iLock) );

	// 7. ����!!
	return	CD_STRUCTURE_QUEUE_PUSH;
}


/*******************************************************************************
* Update    : 2012/06/29                                                       *
* Argument  : stCDStructureQueue*, int*, void**                                *
*             _stpQueue : �����͸� �̾�(Pop, Get)������ �ϴ�                   *
*                         Queue ������Ʈ(����ü) �ּ�                          *
*             _ipType   : �����ϴ� Queue �������� Type                         *
*             _vppData  : �����ϰ��� �ϴ� �������� �ּ�                        *
*                                                                              *
* Return    : int, ����(3), ����(1, -1)                                        *
* Stability : MT-Safe                                                          *
* Explain   : �����͸� �̾Ƴ����� �ϴ� Queue�� ������Ʈ(����ü) �ּ�(_stpQueue)*
*             �� ���ڷ� �޾Ƽ� �ش� ������Ʈ���� �����͸� �̾Ƴ���.            *
*             �̾Ƴ� �����ʹ� ����(_vppData) �� ����Ǹ� �������� ����(Type)�� *
*             _ipType �� ����ȴ�.                                             *
*             LocalQueue�� Ư�� �������� �ּҰ��� �����ϴ� Queue�̹Ƿ� Pop�ÿ� *
*             �������� ����(Type), �������� �ּҸ� �̾Ƴ���.                   *
*             Queue�� ���� �����Ͱ� �������� ������ �̹Ƿ� �ش� �����͸�       *
*             �����ϱ� ���� �����ͺ����� ������(_vppData)��                    *
*             ���ڰ����� �Ѱ��ش�. ������ CD_STRUCTURE_QUEUE_POP(2)�� ��ȯ�ϸ� *
*             ���н� CD_STRUCTURE_ERROR(-1)�� ��ȯ�ϸ� Queue�� �����Ͱ�        *
*             ������쿡�� CD_STRUCTURE_QUEUE_EMPTY(1)�� ��ȯ�Ѵ�.             *
*******************************************************************************/
int CDStructureQueuePop( stCDStructureQueue* _stpQueue, int* _ipType, void** _vppData )
{
	stCDStructQueueContainer		*dummy;

	// 1. mutex ���
	pthread_mutex_lock( &(_stpQueue->m_iLock) );

	// 2. LocalQueue�ȿ� �̾Ƴ� ������(���� Queue, m_stOccupiedList)��
	//    ������� ���(Queue Empty)
	if( _stpQueue->m_stOccupiedList.m_reference <= 0 )
	{
		pthread_mutex_unlock( &(_stpQueue->m_iLock) );

		return	CD_STRUCTURE_QUEUE_EMPTY;
	}

	// 3. �������(m_stOccupiedList)���� ������(dummy)�� �̾Ƴ���
	if( ( dummy = CDStructQueuePopLinkedList( &(_stpQueue->m_stOccupiedList) ) ) == NULL )
	{
		pthread_mutex_unlock( &(_stpQueue->m_iLock) );

		return	CD_STRUCTURE_ERROR;
	}

	// 4. ������ۿ��� �̾Ƴ� �����͸� ���ڰ����� ���� ������ ����
	*_ipType	= dummy->type;
	*_vppData	= dummy->data;

	// 5. ó���� ���� �����ʹ� �ٽ� �������(���� Queue, m_stIdleList)�� ����
	if( ( CDStructQueuePushLinkedList( &(_stpQueue->m_stIdleList), dummy ) ) == NULL)
	{
		pthread_mutex_unlock( &(_stpQueue->m_iLock) );

		return	CD_STRUCTURE_ERROR;
	}

	// 6. mutex ��� ����
	pthread_mutex_unlock( &(_stpQueue->m_iLock) );

	// 7. ����!!
	return	CD_STRUCTURE_QUEUE_POP;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureQueue*                                              *
*             _stpQueue : ���� �� �������� ���ڸ� �˰��� �ϴ� Queue            *
*                         ����ü�� �ּҰ�                                      *
*                                                                              *
* Return    : int, ����(Queue ������ ���緮), ����(-1)                         *
* Stability : MT-Safe                                                          *
* Explain   : ���� �� �������� ���ڸ� �˰��� �ϴ� Queue ������Ʈ(����ü)       *
*             �ּҰ�(_stpQueue)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ�� ������         *
*             ���緮�� ��ȯ�Ѵ�.                                               *
*******************************************************************************/
int CDStructureQueueGetUseCount( stCDStructureQueue* _stpQueue )
{
	int	iReference;

	if( _stpQueue == NULL )
		return	CD_STRUCTURE_ERROR;

	// 1. mutex ���
	pthread_mutex_lock( &(_stpQueue->m_iLock) );

	// 2. ���� Queue ���� ī��Ʈ�� ������ ����
	iReference	= _stpQueue->m_stOccupiedList.m_reference;

	// 3. mutex ��� ����
	pthread_mutex_unlock( &(_stpQueue->m_iLock) );

	// 4. ����!!
	return	iReference;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureQueue*                                              *
*             _stpQueue : ���� ������ �������� ����(������)�� �˰��� �ϴ�      *
*                              Queue ����ü�� �ּҰ�                           *
*                                                                              *
* Return    : int, ����(Queue ������ ���緮), ����(-1)                         *
* Stability : MT-Safe                                                          *
* Explain   : ���� ������ �������� ������ �˰��� �ϴ� Queue                    *
*             ������Ʈ(����ü) �ּҰ�(_stpQueue)�� ���ڷ� �޾Ƽ� �ش�          *
*             ������Ʈ�� ������ ���緮�� ��ȯ�Ѵ�.                             *
*******************************************************************************/
int CDStructureQueueGetIdleCount( stCDStructureQueue* _stpQueue )
{
	int	iReference;

	if( _stpQueue == NULL )
		return	CD_STRUCTURE_ERROR;

	// 1. mutex ���
	pthread_mutex_lock( &(_stpQueue->m_iLock) );

	// 2. ���� Queue ���� ī��Ʈ�� ������ ����
	iReference	= _stpQueue->m_stIdleList.m_reference;

	// 3. mutex ��� ����
	pthread_mutex_unlock( &(_stpQueue->m_iLock) );

	// 4. ����!!
	return	iReference;
}


/*******************************************************************************
* Update    : 2012/06/29                                                       *
* Argument  : stCDStructureQueue*                                              *
*             _stpNode : �ʱ�ȭ �ϰ��� �ϴ� Queue ������Ʈ(����ü �ּ�)        *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : �ʱ�ȭ �ϰ��� �ϴ� Queue ������Ʈ(����ü) �ּҰ�(_stpNode)��     *
*             ���ڷ� �޾Ƽ� �ش� ������Ʈ�� �ʱ�ȭ(NULL)�� �����ϰ�            *
*             ���� �������� �ʱ�ȭ�Ѵ�.                                        *
*******************************************************************************/
static void CDStructQueueInitLinkedList( stCDStructureQueueNode* _stpNode )
{
	// 1. Queue ������Ʈ(����ü)�� 0x00(NULL)�� �ʱ�ȭ
	memset( _stpNode,0,sizeof( stCDStructureQueueNode ) );

	// 2. Queue ������Ʈ�� ó��(m_head)�� ��(m_tail)�� ����Ű��
	//    Node ���� �ʱ�ȭ
	_stpNode->m_head.next = &_stpNode->m_tail;
	_stpNode->m_tail.prev = &_stpNode->m_head;

	// 3. ����!!
	return;
}


/*******************************************************************************
* Update    : 2012/06/29                                                       *
* Argument  : stCDStructureQueue*                                              *
*             _stpNode : �����͸� �߰��ϰ��� �ϴ� Queue ������Ʈ               *
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : Queue ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�            *
*             �ش� Queue�� ������(dummy)�� �����ϰ� �߰��Ѵ�.                  *
*             �ش� Node�� LocalQueue�� �����Ͱ� ���� �� �� �����͸� ���� ��    *
*             ���۰����̴�.                                                    *
*******************************************************************************/
static int CDStructQueueAppendTailLinkedList( stCDStructureQueueNode* _stpNode )
{
	stCDStructQueueContainer		*dummy	=	NULL;

	// 1. heap ������ Node �����͸� ����
	if( ( dummy = ( stCDStructQueueContainer* )malloc( sizeof( stCDStructQueueContainer ) ) ) == NULL )
		return CD_STRUCTURE_ERROR;

	// 2. ���� �����͸� 0x00(NULL)�� �ʱ�ȭ
	memset( dummy, 0x00, sizeof( stCDStructQueueContainer ) );

	// 3. ���� �� �����͸� Queue ���� ���� �߰�
	dummy->next = &_stpNode->m_tail;
	dummy->prev = _stpNode->m_tail.prev;
	dummy->prev->next = dummy;

	_stpNode->m_tail.prev = dummy;
	_stpNode->m_reference++;

	// 4. ����!!
	return CD_STRUCTURE_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/06/29                                                       *
* Argument  : stCDStructureQueue*                                              *
*             _stpNode : ���� ���� �����͸� �����ϰ��� �ϴ� Queue ������Ʈ     *
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : Queue ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�            *
*             �ش� Queue�� ���� ��(m_tail.prev)�� �����͸� �����Ѵ�.           *
*             ListedList�� ���� �������� ���� ���� �����ʹ� m_tail.prev �̴�.  *
*             �����͸� ����(free)�� reference ���ڸ� 1 ���ҽ�Ų��.             *
*******************************************************************************/
static int CDStructQueueRemoveTailLinkedList( stCDStructureQueueNode* _stpNode )
{
	stCDStructQueueContainer		*dummy	=	NULL;

	if( _stpNode->m_head.next == &_stpNode->m_tail )
		return	CD_STRUCTURE_ERROR;

	// 1. ������ Queue �������� �� �����͸� ����
	dummy	= _stpNode->m_tail.prev;

	// 2. ������ �������� ��(prev) �����Ϳ�
	//    ������ �������� ��(next) �����͸� ����
	dummy->prev->next	= &_stpNode->m_tail;
	_stpNode->m_tail.prev	= dummy->prev;

	// 3. ������ ����
	free( dummy );

	// 4. reference ī��Ʈ 1����
	_stpNode->m_reference--;

	// 5. ����!!
	return CD_STRUCTURE_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/06/29                                                       *
* Argument  : stCDStructureQueue*, stCDStructQueueContainer*                   *
*             _stpNode     : �����͸� �����ϰ��� �ϴ� Queue ������Ʈ           *
*                            (����ü �ּ�)                                     *
*             _stpPushData : ListedList�� ����Ǵ� �������� �ּ�               *
*                                                                              *
* Return    : stCDStructQueueContainer*                                        *
* Stability : MT-Safe                                                          *
* Explain   : Queue ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�            *
*             �ش� Queue�� ������(_stpPushData)�� �����Ѵ�.                    *
*             ������ ���� �� reference ���� 1 ������Ų��.                      *
*******************************************************************************/
static stCDStructQueueContainer* CDStructQueuePushLinkedList( stCDStructureQueueNode* _stpNode, stCDStructQueueContainer* _stpPushData )
{
	// 1. �߰��ϰ��� �ϴ� �����Ϳ� ���� ���� �������� �����͸� ����
	_stpPushData->next = &_stpNode->m_tail;
	_stpPushData->prev = _stpNode->m_tail.prev;
	_stpPushData->prev->next = _stpPushData;

	_stpNode->m_tail.prev = _stpPushData;
	_stpNode->m_reference++;

	// 2. ����!!
	return _stpPushData;
}


/*******************************************************************************
* Update    : 2012/06/29                                                       *
* Argument  : stCDStructureQueue*                                              *
*             _stpNode : �����͸� �̾Ƴ����� �ϴ� Queue ������Ʈ               *
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : stCDStructQueueContainer*                                        *
* Stability : MT-Safe                                                          *
* Explain   : Queue ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�            *
*             �ش� Queue���� ������(dummy)�� �̾Ƴ���.                         *
*             �����͸� �̾Ƴ� �� reference ���� 1 ���ҽ�Ų��.                  *
*******************************************************************************/
static stCDStructQueueContainer* CDStructQueuePopLinkedList( stCDStructureQueueNode* _stpNode )
{
	stCDStructQueueContainer		*dummy	=	NULL;

	if( _stpNode->m_head.next == &_stpNode->m_tail )
		return	NULL;

	dummy				= _stpNode->m_head.next;
	dummy->prev->next	= dummy->next;
	dummy->next->prev	= dummy->prev;

	_stpNode->m_reference--;

	return dummy;
}


/*******************************************************************************
* Update    : 2012/06/29                                                       *
* Argument  : stCDStructureQueue*                                              *
*             _stpNode : ��� �����͸� �����ϰ��� �ϴ� Queue ������Ʈ          *
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : Queue ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�            *
*             �ش� Queue�� ��� �����͸� �����Ѵ�.                             *
*******************************************************************************/
static void CDStructQueueDeleteAllLinkedList( stCDStructureQueueNode* _stpNode )
{
	stCDStructQueueContainer *offset = NULL;

	if( _stpNode->m_reference <= 0 )
		return;

	for( offset = _stpNode->m_head.next; offset->next; offset = offset->next )
		offset = CDStructQueueDeleteLinkedList( _stpNode, offset );

	return;
}


/*******************************************************************************
* Update    : 2012/06/29                                                       *
* Argument  : stCDStructureQueue*                                              *
*             _stpNode       : Ư�� �����͸� �����ϰ��� �ϴ�                   *
*                              Queue ������Ʈ (����ü �ּ�)                    *
*             _stpDeleteNode : �����ϰ��� �ϴ� ������                          *
*                                                                              *
* Return    : stCDStructQueueContainer*                                        *
* Stability : MT-Safe                                                          *
* Explain   : Queue ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�            *
*             �ش� Queue���� Ư��������(_stpDeleteNode)�� �����Ѵ�.            *
*             ������ ���� �� ���� �������� ���� ������(_stpDeleteNode->prev)�� *
*             �ּ�(prev)�� ��ȯ�Ѵ�. ���� �� reference ���� 1 ���ҽ�Ų��.      *
*             �����ؾ� �� ���� ������ Ư�� ������(_stpDeleteNode)�� �ݵ��     *
*             Queue ������Ʈ(_stpNode)�ȿ� �����ϴ� �����Ϳ����Ѵ�.            *
*******************************************************************************/
static stCDStructQueueContainer* CDStructQueueDeleteLinkedList( stCDStructureQueueNode* _stpNode, stCDStructQueueContainer* _stpDeleteNode )
{
	stCDStructQueueContainer *prev;

	_stpDeleteNode->prev->next = _stpDeleteNode->next;
	_stpDeleteNode->next->prev = _stpDeleteNode->prev;
	prev = _stpDeleteNode->prev;

	free( _stpDeleteNode );

	_stpNode->m_reference--;

	return prev;
}

