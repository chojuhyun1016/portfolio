#include "CDStructureStack.h"

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
* Update    : 2012/10/17                                                       *
* Argument  : ����                                                             *
*                                                                              *
* Return    : stCDStructStackContainer*, ����(stCDStructStackContainer*),      *
*             ����(NULL)                                                       *
* Stability : MT-Safe                                                          *
* Explain   : LocalStack ������Ʈ(����ü)�� heap ������ �����ϰ� ������        *
*             ������Ʈ�� �ּҸ� ��ȯ�Ѵ�.                                      *
*             ������ ��� ������Ʈ�� �ּ�(stCDStructureStack*)�� ��ȯ�ϸ�      *
*             ������ ��� NULL �� ��ȯ�Ѵ�. LocalStack ������Ʈ�� ��������     *
*             �����ϸ� ���� Thread���� �̻� ������ �Ҽ� �����Ƿ� �����̳�      *
*             ����� �ݵ�� MakeLocalStack() �Լ��� ���ؼ� �����ϰ� ����Ѵ�.  *
*******************************************************************************/
stCDStructureStack* CDStructureStackMake()
{
    stCDStructureStack  *stpLocalStack;

    if( ( stpLocalStack = ( stCDStructureStack* )malloc( sizeof( stCDStructureStack ) ) ) == NULL )
        return NULL;

    memset( stpLocalStack, 0x00, sizeof(stCDStructureStack) );

    return  stpLocalStack;
}


/*******************************************************************************
* Update    : 2012/10/17                                                       *
* Argument  : stCDStructureStack*, int                                         *
*             _stpStack : �ʱ�ȭ �ϰ��� �ϴ� LocalStack ������Ʈ(����ü)�� �ּ�*
*             _iPrepare : ������ Stack �������� ��                             *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : �ʱ�ȭ �ϰ��� �ϴ� Stack ������Ʈ(����ü) �ּҰ�(_stpStack)��    *
*             ���ڷ� �޾Ƽ� �ش� ������Ʈ�� �ʱ�ȭ(NULL)�� ������              *
*             ����ȭ ����(mutex)���� �ʱ�� �� _iPrepare(�ʱ� Stack ���� ��)   *
*             ��ŭ �����͸� �����Ѵ�. �ִ� ������ ���緮�� _iPrepare�� �ȴ�.   *
*******************************************************************************/
int CDStructureStackInit( stCDStructureStack* _stpStack, int _iPrepare )
{
    int iAppendLoop;

    if( _stpStack == NULL )
        return  CD_STRUCTURE_ERROR;

    if( _iPrepare < 0 )
        return  CD_STRUCTURE_ERROR;

    // 1. mutex ���� �ʱ�ȭ
    pthread_mutex_init( &(_stpStack->m_iLock), NULL );

    // 2. mutex ���
    pthread_mutex_lock( &(_stpStack->m_iLock) );

    // 3. LocalStack ������Ʈ ���� Stack �ʱ�ȭ(����� List, ���� List)
    CDStructStackInitLinkedList( &(_stpStack->m_stOccupiedList) );
    CDStructStackInitLinkedList( &(_stpStack->m_stIdleList) );

    // 4. _iPrepare ��ŭ LocalStack ���� ����
    //    ���� ����(m_stIdleList)�� �����Ѵ�.
    for( iAppendLoop = 0; iAppendLoop < _iPrepare; iAppendLoop++ )
    {
        // 4.1 �����͸� �����ؼ� Linkedlist ���ۿ� Append
        if( CDStructStackAppendTailLinkedList(  &(_stpStack->m_stIdleList) ) == CD_STRUCTURE_ERROR )
        {
            pthread_mutex_unlock( &(_stpStack->m_iLock) );

            return  CD_STRUCTURE_ERROR;
        }
    }

    // 5. mutex ��� ����
    pthread_mutex_unlock( &(_stpStack->m_iLock) );

    // 6. ����!!
    return  CD_STRUCTURE_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/11/21                                                       *
* Argument  : stCDStructureStack**                                             *
*             _stppStack : ���� �ϰ��� �ϴ� LocalStack ������Ʈ(����ü)�� �ּ� *
*                         �� �����ϰ� �ִ� ������ �ּ�(�������� ������)        *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : ����(����) �ϰ��� �ϴ� Stack ������Ʈ(����ü) �ּ�(_stppStack)�� *
*             �����ϰ� �ִ� ������ �ּҸ� ���ڷ� �޾Ƽ� �ش� ������Ʈ����      *
*             �����͸� ��� ����(����)�Ѵ�.                                    *
*             ������Ʈ���� Stack �����͸� ��� ����(����)�� ��                 *
*             ������Ʈ ���� �ʱ�ȭ�Ѵ�.                                        *
*******************************************************************************/
void CDStructureStackFree( stCDStructureStack** _stppStack )
{
    if( _stppStack == NULL )
        return;

    if( *_stppStack == NULL )
        return;

    // 1. mutex ���
    pthread_mutex_lock( &((*_stppStack)->m_iLock) );

    // 2. LocalStack ���� ��� ������(Stack) �����͸� ����(����)
    CDStructStackDeleteAllLinkedList( &((*_stppStack)->m_stOccupiedList) );
    CDStructStackDeleteAllLinkedList( &((*_stppStack)->m_stIdleList) );

    // 3. mutex ��� ����
    pthread_mutex_unlock( &((*_stppStack)->m_iLock) );

    // 4. mutex ���� ����
    pthread_mutex_destroy( &((*_stppStack)->m_iLock) );

    // 5. Heap ������ �Ҵ� �� Stack ������Ʈ ����
    free( *_stppStack );

    // 6. ������Ʈ�� �����Ǿ����Ƿ� NULL ����
    *_stppStack = NULL;

    // 7. ����!!
    return;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureStack*, int                                         *
*             _stpStack     : Ȯ�� �ϰ��� �ϴ� Stack ������Ʈ(����ü)�� �ּ�   *
*             _iAppendCount : Ȯ�� �� �������� ��                              *
*                                                                              *
* Return    : int, �߰� �Ҵ� �� ������ ��                                      *
* Stability : MT-Safe                                                          *
* Explain   : Ȯ�� �ϰ��� �ϴ� Stack ������Ʈ(����ü)                          *
*             �ּҰ�(_stpStack)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ��                *
*             ������ۿ� iAppendCount ��ŭ ���۸� �߰� �Ҵ��Ѵ�.               *
*             ��������� ũ�⸸ŭ �����͸� �����Ҽ� �����Ƿ� ���� ������       *
*             Ȯ���� ��ü Stack�� ����� �ø��� ���̴�.                      *
*             ���ڰ�(_stpStack, _iAppendCount)�� �߸��� ��츦 �����ϰ���      *
*             �߰��� �Ҵ��� �������� ������ ��ȯ�Ѵ�.                          *
*             ���ڰ��� �߸� �� ��쿡�� CD_STRUCTURE_ERROR(-1)�� ��ȯ�Ѵ�.     *
*******************************************************************************/
int CDStructureStackAppend( stCDStructureStack* _stpStack, int _iAppendCount )
{
    int iAppendLoop;

    if( _stpStack == NULL )
        return  CD_STRUCTURE_ERROR;

    if( _iAppendCount < 0 )
        return  CD_STRUCTURE_ERROR;

    // 1. mutex ���
    pthread_mutex_lock( &(_stpStack->m_iLock) );

    // 2. �߰��� �Ҵ��ϴ� ���ڸ�ŭ ������ ���鼭 ������� �߰� �Ҵ�
    for( iAppendLoop = 0; iAppendLoop < _iAppendCount; iAppendLoop++ )
    {
        // 2.1 ������� �Ѱ� �߰� �Ҵ�
        if( CDStructStackAppendTailLinkedList(  &(_stpStack->m_stIdleList) ) == CD_STRUCTURE_ERROR )
        {
            pthread_mutex_unlock( &(_stpStack->m_iLock) );

            return  iAppendLoop;
        }
    }

    // 3. mutex ��� ����
    pthread_mutex_unlock( &(_stpStack->m_iLock) );

    // 4. ����!!
    return  iAppendLoop;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureStack*, int                                         *
*             _stpStack : ���� ��Ű���� �ϴ� Stack ������Ʈ(����ü)�� �ּ�     *
*             _iDeleteCount : ���ҽ�ų ���� ���� �������� ��                   *
*                                                                              *
* Return    : int, ���� �� ������ ��                                           *
* Stability : MT-Safe                                                          *
* Explain   : ���ҽ�Ű���� �ϴ� Stack ������Ʈ(����ü)                         *
*             �ּҰ�(_stpStack)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ��                *
*             ������ۿ� _iDeleteCount ��ŭ ���۸� ����(����)��Ų��.           *
*             ��������� ũ�⸸ŭ �����͸� �����Ҽ� �����Ƿ� ���� ������       *
*             ���Ҵ� ��ü Stack�� ����� ���̴� ���̴�.                      *
*             ���ڰ�(_stpStack, _iDeleteCount)�� �߸��� ��츦 �����ϰ���      *
*             ���� �� �������� ������ ��ȯ�Ѵ�.                                *
*             ���ڰ��� �߸� �� ��쿡�� CD_STRUCTURE_ERROR(-1)�� ��ȯ�Ѵ�.     *
*******************************************************************************/
int CDStructureStackDelete( stCDStructureStack* _stpStack, int _iDeleteCount )
{
    int iDepleteLoop;
    int iDepleteMaxLoop;

    if( _stpStack == NULL )
        return  CD_STRUCTURE_ERROR;

    if( _iDeleteCount < 0 )
        return  CD_STRUCTURE_ERROR;

    iDepleteMaxLoop = _iDeleteCount;

    // 1. ���� ������ ������ ������ ���̰����ϴ� �������� ������
    //    ���̰��� �ϴ� �������� ������ ���� �������� ������ ����
    if(  _stpStack->m_stIdleList.m_reference < iDepleteMaxLoop )
        iDepleteMaxLoop = _stpStack->m_stIdleList.m_reference;

    // 2. mutex ���
    pthread_mutex_lock( &(_stpStack->m_iLock) );

    // 3. �߰��� �Ҵ��ϴ� ���ڸ�ŭ ������ ���鼭 ���� ���� ����
    for( iDepleteLoop = 0; iDepleteLoop < iDepleteMaxLoop; iDepleteLoop++ )
    {
        // 3.1 ���� ���� ������ �Ѱ� ����
        if( CDStructStackRemoveTailLinkedList(  &(_stpStack->m_stIdleList) ) == CD_STRUCTURE_ERROR )
        {
            pthread_mutex_unlock( &(_stpStack->m_iLock) );

            return  iDepleteLoop;
        }
    }

    // 4. mutex ��� ����
    pthread_mutex_unlock( &(_stpStack->m_iLock) );

    // 5. ����!!
    return  iDepleteLoop;
}


/*******************************************************************************
* Update    : 2012/10/17                                                       *
* Argument  : stCDStructureStack*, int, void*                                  *
*             _stpStack : �����͸� ����(Insert, Push) �ϰ��� �ϴ�              *
*                         Stack ������Ʈ(����ü) �ּ�                          *
*             _iType    : �����ϴ� Stack �������� Type                         *
*             _vpData   : �����ϰ��� �ϴ� �������� �ּ�                        *
*                                                                              *
* Return    : int, ����(3), ����(1, -1)                                        *
* Stability : MT-Safe                                                          *
* Explain   : �����͸� �����ϰ��� �ϴ� Stack�� ������Ʈ(����ü) �ּ�(_stpStack)*
*             �� ���ڷ� �޾Ƽ� �ش� ������Ʈ�� ������(_vpData)�� �����Ѵ�.     *
*             ���Խ� �ش� �������� ����(Type)�� �Բ� ���Եȴ�.                 *
*             Pop �Լ��� ���� �����͸� �ٽ� �̾Ƴ� �ÿ� Stack�� ����(_iType)�� *
*             ������(_vpData)�� �̾Ƴ��� �ȴ�.                                 *
*             LocalStack�� ���� Stack�� �������� �ּ�(Pointer)���� �����Ѵ�.   *
*******************************************************************************/
int CDStructureStackPush( stCDStructureStack* _stpStack, int _iType, void* _vpData )
{
    stCDStructStackContainer        *dummy;

    // 1. mutex ���
    pthread_mutex_lock( &(_stpStack->m_iLock) );

    // 2. LocalStack�ȿ� �����͸� ���� �� ����(���� Stack, m_stIdleList)��
    //    ���� ���(Stack Full)
    if( _stpStack->m_stIdleList.m_reference <= 0 )
    {
        // 2.1 mutex ��� ����
        pthread_mutex_unlock( &(_stpStack->m_iLock) );

        // 2.2 ����!!
        return  CD_STRUCTURE_STACK_FULL;
    }

    // 3. �������(m_stIdleList)���� �����͸� ���� �� ����(dummy)�� �̾Ƴ���
    if( ( dummy = CDStructStackPopLinkedList( &(_stpStack->m_stIdleList) ) ) == NULL )
    {
        pthread_mutex_unlock( &(_stpStack->m_iLock) );

        return  CD_STRUCTURE_ERROR;
    }

    // 4. ���ۿ� ���ڷ� ���� �����Ͱ��� ����
    dummy->type = _iType;
    dummy->data = _vpData;

    // 5. �������(���� Stack, m_stOccupiedList)�� �ش� �����͸� ����
    if( ( CDStructStackPushLinkedList( &(_stpStack->m_stOccupiedList), dummy ) ) == NULL)
    {
        pthread_mutex_unlock( &(_stpStack->m_iLock) );

        return  CD_STRUCTURE_ERROR;
    }

    // 6. mutex ��� ����
    pthread_mutex_unlock( &(_stpStack->m_iLock) );

    // 7. ����!!
    return  CD_STRUCTURE_STACK_PUSH;
}


/*******************************************************************************
* Update    : 2012/10/17                                                       *
* Argument  : stCDStructureStack*, int*, void**                                *
*             _stpStack : �����͸� �̾�(Pop, Get)������ �ϴ�                   *
*                         Stack ������Ʈ(����ü) �ּ�                          *
*             _ipType   : �����ϴ� Stack �������� Type                         *
*             _vppData  : �����ϰ��� �ϴ� �������� �ּ�                        *
*                                                                              *
* Return    : int, ����(3), ����(1, -1)                                        *
* Stability : MT-Safe                                                          *
* Explain   : �����͸� �̾Ƴ����� �ϴ� Stack�� ������Ʈ(����ü) �ּ�(_stpStack)*
*             �� ���ڷ� �޾Ƽ� �ش� ������Ʈ���� �����͸� �̾Ƴ���.            *
*             �̾Ƴ� �����ʹ� ����(_vppData) �� ����Ǹ� �������� ����(Type)�� *
*             _ipType �� ����ȴ�.                                             *
*             LocalStack�� Ư�� �������� �ּҰ��� �����ϴ� Stack�̹Ƿ� Pop�ÿ� *
*             �������� ����(Type), �������� �ּҸ� �̾Ƴ���.                   *
*             Stack�� ���� �����Ͱ� �������� ������ �̹Ƿ� �ش� �����͸�       *
*             �����ϱ� ���� �����ͺ����� ������(_vppData)��                    *
*             ���ڰ����� �Ѱ��ش�. ������ CD_STRUCTURE_STACK_POP(2)�� ��ȯ�ϸ� *
*             ���н� CD_STRUCTURE_ERROR(-1)�� ��ȯ�ϸ� Stack�� �����Ͱ�        *
*             ������쿡�� CD_STRUCTURE_STACK_EMPTY(1)�� ��ȯ�Ѵ�.             *
*******************************************************************************/
int CDStructureStackPop( stCDStructureStack* _stpStack, int* _ipType, void** _vppData )
{
    stCDStructStackContainer        *dummy;

    // 1. mutex ���
    pthread_mutex_lock( &(_stpStack->m_iLock) );

    // 2. LocalStack�ȿ� �̾Ƴ� ������(���� Stack, m_stOccupiedList)��
    //    ������� ���(Stack Empty)
    if( _stpStack->m_stOccupiedList.m_reference <= 0 )
    {
        // 2.1 // 2.1 mutex ��� ����
        pthread_mutex_unlock( &(_stpStack->m_iLock) );

        // 2.2 ����!!
        return  CD_STRUCTURE_STACK_EMPTY;
    }

    // 3. �������(m_stOccupiedList)���� ������(dummy)�� �̾Ƴ���
    if( ( dummy = CDStructStackPopLinkedList( &(_stpStack->m_stOccupiedList) ) ) == NULL )
    {
        pthread_mutex_unlock( &(_stpStack->m_iLock) );

        return  CD_STRUCTURE_ERROR;
    }

    // 4. ������ۿ��� �̾Ƴ� �����͸� ���ڰ����� ���� ������ ����
    *_ipType    = dummy->type;
    *_vppData   = dummy->data;

    // 5. ó���� ���� �����ʹ� �ٽ� �������(���� Stack, m_stIdleList)�� ����
    if( ( CDStructStackPushLinkedList( &(_stpStack->m_stIdleList), dummy ) ) == NULL)
    {
        pthread_mutex_unlock( &(_stpStack->m_iLock) );

        return  CD_STRUCTURE_ERROR;
    }

    // 6. mutex ��� ����
    pthread_mutex_unlock( &(_stpStack->m_iLock) );

    // 7. ����!!
    return  CD_STRUCTURE_STACK_POP;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureStack*                                              *
*             _stpStack : ���� �� �������� ���ڸ� �˰��� �ϴ� Stack            *
*                         ����ü�� �ּҰ�                                      *
*                                                                              *
* Return    : int, ����(Stack ������ ���緮), ����(-1)                         *
* Stability : MT-Safe                                                          *
* Explain   : ���� �� �������� ���ڸ� �˰��� �ϴ� Stack ������Ʈ(����ü)       *
*             �ּҰ�(_stpStack)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ�� ������         *
*             ���緮�� ��ȯ�Ѵ�.                                               *
*******************************************************************************/
int CDStructureStackGetUseCount( stCDStructureStack* _stpStack )
{
    int iReference;

    if( _stpStack == NULL )
        return  CD_STRUCTURE_ERROR;

    // 1. mutex ���
    pthread_mutex_lock( &(_stpStack->m_iLock) );

    // 2. ���� Stack ���� ī��Ʈ�� ������ ����
    iReference  = _stpStack->m_stOccupiedList.m_reference;

    // 3. mutex ��� ����
    pthread_mutex_unlock( &(_stpStack->m_iLock) );

    // 4. ����!!
    return  iReference;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureStack*                                              *
*             _stpStack : ���� ������ �������� ����(������)�� �˰��� �ϴ�      *
*                              Stack ����ü�� �ּҰ�                           *
*                                                                              *
* Return    : int, ����(Stack ������ ���緮), ����(-1)                         *
* Stability : MT-Safe                                                          *
* Explain   : ���� ������ �������� ������ �˰��� �ϴ� Stack                    *
*             ������Ʈ(����ü) �ּҰ�(_stpStack)�� ���ڷ� �޾Ƽ� �ش�          *
*             ������Ʈ�� ������ ���緮�� ��ȯ�Ѵ�.                             *
*******************************************************************************/
int CDStructureStackGetIdleCount( stCDStructureStack* _stpStack )
{
    int iReference;

    if( _stpStack == NULL )
        return  CD_STRUCTURE_ERROR;

    // 1. mutex ���
    pthread_mutex_lock( &(_stpStack->m_iLock) );

    // 2. ���� Stack ���� ī��Ʈ�� ������ ����
    iReference  = _stpStack->m_stIdleList.m_reference;

    // 3. mutex ��� ����
    pthread_mutex_unlock( &(_stpStack->m_iLock) );

    // 4. ����!!
    return  iReference;
}


/*******************************************************************************
* Update    : 2012/10/17                                                       *
* Argument  : stCDStructureStack*                                              *
*             _stpNode : �ʱ�ȭ �ϰ��� �ϴ� Stack ������Ʈ(����ü �ּ�)        *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : �ʱ�ȭ �ϰ��� �ϴ� Stack ������Ʈ(����ü) �ּҰ�(_stpNode)��     *
*             ���ڷ� �޾Ƽ� �ش� ������Ʈ�� �ʱ�ȭ(NULL)�� �����ϰ�            *
*             ���� �������� �ʱ�ȭ�Ѵ�.                                        *
*******************************************************************************/
static void CDStructStackInitLinkedList( stCDStructureStackNode* _stpNode )
{
    // 1. Stack ������Ʈ(����ü)�� 0x00(NULL)�� �ʱ�ȭ
    memset( _stpNode,0,sizeof( stCDStructureStackNode ) );

    // 2. Stack ������Ʈ�� ó��(m_head)�� ��(m_tail)�� ����Ű��
    //    Node ���� �ʱ�ȭ
    _stpNode->m_head.next = &_stpNode->m_tail;
    _stpNode->m_tail.prev = &_stpNode->m_head;

    // 3. ����!!
    return;
}


/*******************************************************************************
* Update    : 2012/10/17                                                       *
* Argument  : stCDStructureStack*                                              *
*             _stpNode : �����͸� �߰��ϰ��� �ϴ� Stack ������Ʈ               *
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : Stack ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�            *
*             �ش� Stack�� ������(dummy)�� �����ϰ� �߰��Ѵ�.                  *
*             �ش� Node�� LocalStack�� �����Ͱ� ���� �� �� �����͸� ���� ��    *
*             ���۰����̴�.                                                    *
*******************************************************************************/
static int CDStructStackAppendTailLinkedList( stCDStructureStackNode* _stpNode )
{
    stCDStructStackContainer        *dummy  =   NULL;

    // 1. heap ������ Node �����͸� ����
    if( ( dummy = ( stCDStructStackContainer* )malloc( sizeof( stCDStructStackContainer ) ) ) == NULL )
        return CD_STRUCTURE_ERROR;

    // 2. ���� �����͸� 0x00(NULL)�� �ʱ�ȭ
    memset( dummy, 0x00, sizeof( stCDStructStackContainer ) );

    // 3. ���� �� �����͸� Stack ���� ���� �߰�
    dummy->next = &_stpNode->m_tail;
    dummy->prev = _stpNode->m_tail.prev;
    dummy->prev->next = dummy;

    _stpNode->m_tail.prev = dummy;
    _stpNode->m_reference++;

    // 4. ����!!
    return CD_STRUCTURE_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/10/17                                                       *
* Argument  : stCDStructureStack*                                              *
*             _stpNode : ���� ���� �����͸� �����ϰ��� �ϴ� Stack ������Ʈ     *
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : Stack ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�            *
*             �ش� Stack�� ���� ��(m_tail.prev)�� �����͸� �����Ѵ�.           *
*             ListedList�� ���� �������� ���� ���� �����ʹ� m_tail.prev �̴�.  *
*             �����͸� ����(free)�� reference ���ڸ� 1 ���ҽ�Ų��.             *
*******************************************************************************/
static int CDStructStackRemoveTailLinkedList( stCDStructureStackNode* _stpNode )
{
    stCDStructStackContainer        *dummy  =   NULL;

    if( _stpNode->m_head.next == &_stpNode->m_tail )
        return  CD_STRUCTURE_ERROR;

    // 1. ������ Stack �������� �� �����͸� ����
    dummy   = _stpNode->m_tail.prev;

    // 2. ������ �������� ��(prev) �����Ϳ�
    //    ������ �������� ��(next) �����͸� ����
    dummy->prev->next   = &_stpNode->m_tail;
    _stpNode->m_tail.prev   = dummy->prev;

    // 3. ������ ����
    free( dummy );

    // 4. reference ī��Ʈ 1����
    _stpNode->m_reference--;

    // 5. ����!!
    return CD_STRUCTURE_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/10/17                                                       *
* Argument  : stCDStructureStack*, stCDStructStackContainer*                   *
*             _stpNode     : �����͸� �����ϰ��� �ϴ� Stack ������Ʈ           *
*                            (����ü �ּ�)                                     *
*             _stpPushData : ListedList�� ����Ǵ� �������� �ּ�               *
*                                                                              *
* Return    : stCDStructStackContainer*                                        *
* Stability : MT-Safe                                                          *
* Explain   : Stack ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�            *
*             �ش� Stack�� ������(_stpPushData)�� �����Ѵ�.                    *
*             ������ ���� �� reference ���� 1 ������Ų��.                      *
*******************************************************************************/
static stCDStructStackContainer* CDStructStackPushLinkedList( stCDStructureStackNode* _stpNode, stCDStructStackContainer* _stpPushData )
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
* Update    : 2012/10/17                                                       *
* Argument  : stCDStructureStack*                                              *
*             _stpNode : �����͸� �̾Ƴ����� �ϴ� Stack ������Ʈ               *
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : stCDStructStackContainer*                                        *
* Stability : MT-Safe                                                          *
* Explain   : Stack ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�            *
*             �ش� Stack���� ������(dummy)�� �̾Ƴ���.                         *
*             �����͸� �̾Ƴ� �� reference ���� 1 ���ҽ�Ų��.                  *
*******************************************************************************/
static stCDStructStackContainer* CDStructStackPopLinkedList( stCDStructureStackNode* _stpNode )
{
    stCDStructStackContainer        *dummy  =   NULL;

    if( _stpNode->m_tail.prev == &_stpNode->m_head )
        return  NULL;

    dummy               = _stpNode->m_tail.prev;

    dummy->prev->next   = dummy->next;
    dummy->next->prev   = dummy->prev;

    _stpNode->m_reference--;

    return dummy;
}


/*******************************************************************************
* Update    : 2012/10/17                                                       *
* Argument  : stCDStructureStack*                                              *
*             _stpNode : ��� �����͸� �����ϰ��� �ϴ� Stack ������Ʈ          *
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : Stack ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�            *
*             �ش� Stack�� ��� �����͸� �����Ѵ�.                             *
*******************************************************************************/
static void CDStructStackDeleteAllLinkedList( stCDStructureStackNode* _stpNode )
{
    stCDStructStackContainer *offset = NULL;

    if( _stpNode->m_reference <= 0 )
        return;

    for( offset = _stpNode->m_head.next; offset->next; offset = offset->next )
        offset = CDStructStackDeleteLinkedList( _stpNode, offset );

    return;
}


/*******************************************************************************
* Update    : 2012/10/17                                                       *
* Argument  : stCDStructureStack*                                              *
*             _stpNode       : Ư�� �����͸� �����ϰ��� �ϴ�                   *
*                              Stack ������Ʈ (����ü �ּ�)                    *
*             _stpDeleteNode : �����ϰ��� �ϴ� ������                          *
*                                                                              *
* Return    : stCDStructStackContainer*                                        *
* Stability : MT-Safe                                                          *
* Explain   : Stack ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�            *
*             �ش� Stack���� Ư��������(_stpDeleteNode)�� �����Ѵ�.            *
*             ������ ���� �� ���� �������� ���� ������(_stpDeleteNode->prev)�� *
*             �ּ�(prev)�� ��ȯ�Ѵ�. ���� �� reference ���� 1 ���ҽ�Ų��.      *
*             �����ؾ� �� ���� ������ Ư�� ������(_stpDeleteNode)�� �ݵ��     *
*             Stack ������Ʈ(_stpNode)�ȿ� �����ϴ� �����Ϳ����Ѵ�.            *
*******************************************************************************/
static stCDStructStackContainer* CDStructStackDeleteLinkedList( stCDStructureStackNode* _stpNode, stCDStructStackContainer* _stpDeleteNode )
{
    stCDStructStackContainer *prev;

    _stpDeleteNode->prev->next = _stpDeleteNode->next;
    _stpDeleteNode->next->prev = _stpDeleteNode->prev;
    prev = _stpDeleteNode->prev;

    free( _stpDeleteNode );

    _stpNode->m_reference--;

    return prev;
}

