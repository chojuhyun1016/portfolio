#include "CDStructureLinkedList.h"

#ifdef  _SOLARIS_
    #include <stdlib.h>
    #include <unistd.h>
    #include <string.h>
    #include <pthread.h>
#elif _CENT_OS_
    #include <stdlib.h>
    #include <unistd.h>
    #include <string.h>
    #include <pthread.h>
#else
    #include <stdlib.h>
    #include <unistd.h>
    #include <string.h>
    #include <pthread.h>
#endif


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : ����                                                             *
*                                                                              *
* Return    : stCDStructLinkedListContainer*,                                  *
*             ����(stCDStructLinkedListContainer*),                            *
*             ����(NULL)                                                       *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList ������Ʈ(����ü)�� heap ������ �����ϰ� ������        *
*             ������Ʈ�� �ּҸ� ��ȯ�Ѵ�.                                      *
*             ������ ��� ������Ʈ�� �ּ�(stCDStructureLinkedList*)�� ��ȯ�ϸ� *
*             ������ ��� NULL �� ��ȯ�Ѵ�. LinkedList ������Ʈ�� ��������     *
*             �����ϸ� ���� Thread���� �̻� ������ �Ҽ� �����Ƿ� �����̳�      *
*             ����� �ݵ�� CDStructureLinkedListMake() �Լ��� ���ؼ� �����ϰ� *
*             ����Ѵ�.                                                        *
*******************************************************************************/
stCDStructureLinkedList* CDStructureLinkedListMake()
{
    stCDStructureLinkedList *stpLinkedList;

    if( ( stpLinkedList = ( stCDStructureLinkedList* )malloc( sizeof( stCDStructureLinkedList ) ) ) == NULL )
        return NULL;

    memset( stpLinkedList, 0x00, sizeof(stCDStructureLinkedList) );

    return  stpLinkedList;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*, int                                    *
*             _stpLinkedList : �ʱ�ȭ �ϰ��� �ϴ� LinkedList                   *
*                              ������Ʈ(����ü)�� �ּ�                         *
*             _iPrepare : ������ LinkedList �������� ��                        *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : �ʱ�ȭ �ϰ��� �ϴ� LinkedList ������Ʈ(����ü)                   *
*             �ּҰ�(_stpLinkedList)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ��           *
*             �ʱ�ȭ(NULL)�� ������ ����ȭ ����(mutex)���� �ʱ�� ��           *
*             _iPrepare(�ʱ� LinkedList ���� ��) ��ŭ �����͸� �����Ѵ�.       *
*             �ִ� ������ ���緮�� _iPrepare�� �ȴ�.                           *
*******************************************************************************/
int CDStructureLinkedListInit( stCDStructureLinkedList* _stpLinkedList, int _iPrepare )
{
    int iAppendLoop;

    if( _stpLinkedList == NULL )
        return  CD_STRUCTURE_ERROR;

    if( _iPrepare < 0 )
        return  CD_STRUCTURE_ERROR;

    // 1. mutex ���� �ʱ�ȭ
    pthread_mutex_init( &(_stpLinkedList->m_iLock), NULL );

    // 2. mutex ���
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 3. LinkedList ������Ʈ ���� LinkedList �ʱ�ȭ(����� List, ���� List)
    CDStructLinkedListInitLinkedList( &(_stpLinkedList->m_stOccupiedList) );
    CDStructLinkedListInitLinkedList( &(_stpLinkedList->m_stIdleList) );

    // 4. _iPrepare ��ŭ LinkedList ���� ����
    //    ���� ����(m_stIdleList)�� �����Ѵ�.
    for( iAppendLoop = 0; iAppendLoop < _iPrepare; iAppendLoop++ )
    {
        // 4.1 �����͸� �����ؼ� Linkedlist ���ۿ� Append
        if( CDStructLinkedListAppendTailLinkedList(  &(_stpLinkedList->m_stIdleList) ) == CD_STRUCTURE_ERROR )
        {
            pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

            return  CD_STRUCTURE_ERROR;
        }
    }

    // 5. mutex ��� ����
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 6. ����!!
    return  CD_STRUCTURE_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/11/21                                                       *
* Argument  : stCDStructureLinkedList**                                        *
*             _stppLinkedList : ���� �ϰ��� �ϴ� LinkedList ������Ʈ(����ü)�� *
*                               �ּҸ� �����ϰ� �ִ� ������ �ּ�               *
*                               (�������� ������)                              *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : ����(����) �ϰ��� �ϴ� LinkedList ������Ʈ(����ü)               *
*             �ּ�(_stppLinkedList)�� �����ϰ� �ִ� ������ �ּ�                *
*             (�������� ������)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ���� �����͸�     *
*             ��� ����(����)�Ѵ�.                                             *
*             ������Ʈ���� LinkedList �����͸� ��� ����(����)�� ��            *
*             ������Ʈ ���� �ʱ�ȭ�Ѵ�.                                        *
*******************************************************************************/
void CDStructureLinkedListFree( stCDStructureLinkedList** _stppLinkedList )
{
    if( _stppLinkedList == NULL )
        return;

    if( *_stppLinkedList == NULL )
        return;

    // 1. mutex ���
    pthread_mutex_lock( &((*_stppLinkedList)->m_iLock) );

    // 2. LinkedList ���� ��� ������(LinkedList) �����͸� ����(����)
    CDStructLinkedListDeleteAllLinkedList( &((*_stppLinkedList)->m_stOccupiedList) );
    CDStructLinkedListDeleteAllLinkedList( &((*_stppLinkedList)->m_stIdleList) );

    // 3. mutex ��� ����
    pthread_mutex_unlock( &((*_stppLinkedList)->m_iLock) );

    // 4. mutex ���� ����
    pthread_mutex_destroy( &((*_stppLinkedList)->m_iLock) );

    // 5. Heap ������ �Ҵ� �� LinkedList ������Ʈ ����
    free( *_stppLinkedList );

    // 6. ������Ʈ�� �����Ǿ����Ƿ� NULL ����
    *_stppLinkedList    = NULL;

    // 7. ����!!
    return;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*, int                                    *
*             _stpLinkedList : Ȯ�� �ϰ��� �ϴ� LinkedList                     *
*                              ������Ʈ(����ü)�� �ּ�                         *
*             _iAppendCount  : Ȯ�� �� �������� ��                             *
*                                                                              *
* Return    : int, �߰� �Ҵ� �� ������ ��                                      *
* Stability : MT-Safe                                                          *
* Explain   : Ȯ�� �ϰ��� �ϴ� LinkedList ������Ʈ(����ü)                     *
*             �ּҰ�(_stpLinkedList)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ��           *
*             ������ۿ� iAppendCount ��ŭ ���۸� �߰� �Ҵ��Ѵ�.               *
*             ��������� ũ�⸸ŭ �����͸� �����Ҽ� �����Ƿ� ���� ������       *
*             Ȯ���� ��ü Linked List�� ����� �ø��� ���̴�.                *
*             ���ڰ�(_stpLinkedList, _iAppendCount)�� �߸��� ��츦 �����ϰ��� *
*             �߰��� �Ҵ��� �������� ������ ��ȯ�Ѵ�.                          *
*             ���ڰ��� �߸� �� ��쿡�� CD_STRUCTURE_ERROR(-1)�� ��ȯ�Ѵ�.     *
*******************************************************************************/
int CDStructureLinkedListAppend( stCDStructureLinkedList* _stpLinkedList, int _iAppendCount )
{
    int iAppendLoop;

    if( _stpLinkedList == NULL )
        return  CD_STRUCTURE_ERROR;

    if( _iAppendCount < 0 )
        return  CD_STRUCTURE_ERROR;

    // 1. mutex ���
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 2. �߰��� �Ҵ��ϴ� ���ڸ�ŭ ������ ���鼭 ������� �߰� �Ҵ�
    for( iAppendLoop = 0; iAppendLoop < _iAppendCount; iAppendLoop++ )
    {
        // 2.1 ������� �Ѱ� �߰� �Ҵ�
        if( CDStructLinkedListAppendTailLinkedList(  &(_stpLinkedList->m_stIdleList) ) == CD_STRUCTURE_ERROR )
        {
            pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

            return  iAppendLoop;
        }
    }

    // 3. mutex ��� ����
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 4. ����!!
    return  iAppendLoop;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*, int                                    *
*             _stpLinkedList : ���� ��Ű���� �ϴ� LinkedList                   *
*                              ������Ʈ(����ü)�� �ּ�                         *
*             _iDeleteCount : ���ҽ�ų ���� ���� �������� ��                   *
*                                                                              *
* Return    : int, ���� �� ������ ��                                           *
* Stability : MT-Safe                                                          *
* Explain   : ���ҽ�Ű���� �ϴ� LinkedList ������Ʈ(����ü)                    *
*             �ּҰ�(_stpLinkedList)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ��           *
*             ������ۿ� _iDeleteCount ��ŭ ���۸� ����(����)��Ų��.           *
*             ��������� ũ�⸸ŭ �����͸� �����Ҽ� �����Ƿ� ���� ������       *
*             ���Ҵ� ��ü Linked List�� ����� ���̴� ���̴�.                *
*             ���ڰ�(_stpLinkedList, _iDeleteCount)�� �߸��� ��츦 �����ϰ��� *
*             ���� �� �������� ������ ��ȯ�Ѵ�.                                *
*             ���ڰ��� �߸� �� ��쿡�� CD_STRUCTURE_ERROR(-1)�� ��ȯ�Ѵ�.     *
*******************************************************************************/
int CDStructureLinkedListDelete( stCDStructureLinkedList* _stpLinkedList, int _iDeleteCount )
{
    int iDepleteLoop;
    int iDepleteMaxLoop;

    if( _stpLinkedList == NULL )
        return  CD_STRUCTURE_ERROR;

    if( _iDeleteCount < 0 )
        return  CD_STRUCTURE_ERROR;

    iDepleteMaxLoop = _iDeleteCount;

    // 1. ���� ������ ������ ������ ���̰����ϴ� �������� ������
    //    ���̰��� �ϴ� �������� ������ ���� �������� ������ ����
    if(  _stpLinkedList->m_stIdleList.m_reference < iDepleteMaxLoop )
        iDepleteMaxLoop = _stpLinkedList->m_stIdleList.m_reference;

    // 2. mutex ���
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 3. �߰��� �Ҵ��ϴ� ���ڸ�ŭ ������ ���鼭 ���� ���� ����
    for( iDepleteLoop = 0; iDepleteLoop < iDepleteMaxLoop; iDepleteLoop++ )
    {
        // 3.1 ���� ���� ������ �Ѱ� ����
        if( CDStructLinkedListRemoveTailLinkedList(  &(_stpLinkedList->m_stIdleList) ) == CD_STRUCTURE_ERROR )
        {
            pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

            return  iDepleteLoop;
        }
    }

    // 4. mutex ��� ����
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 5. ����!!
    return  iDepleteLoop;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*, int, int, void*                        *
*             _stpLinkedList : �����͸� ����(Insert, Push) �ϰ��� �ϴ�         *
*                         LinkedList ������Ʈ(����ü) �ּ�                     *
*             _iKey     : �����ϰ� �������� Key��(Pop�� �ʿ�)                  *
*             _iType    : �����ϴ� LinkedList �������� Type                    *
*             _vpData   : �����ϰ��� �ϴ� �������� �ּ�                        *
*                                                                              *
* Return    : int, ����(3), ����(1, -1)                                        *
* Stability : MT-Safe                                                          *
* Explain   : �����͸� �����ϰ��� �ϴ� LinkedList�� ������Ʈ(����ü)           *
*             �ּ�(_stpLinkedList)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ��             *
*             ������(_vpData)�� �����Ѵ�.                                      *
*             ���Խ� �ش� �������� Ű(_iKey)�� ����(Type)�� �Բ� ���Եȴ�.     *
*             Pop �Լ��� ���� �����͸� �ٽ� �̾Ƴ� �ÿ� LinkedList��           *
*             �������߿� Ű(_iKey)���� ������ �������� ����(_iType)��          *
*             ������(_vpData)�� �̾Ƴ��� �ȴ�.                                 *
*             LinkedList�� ���� LinkedList�� �������� �ּ�(Pointer)����        *
*             �����Ѵ�.                                                        *
*******************************************************************************/
int CDStructureLinkedListPush( stCDStructureLinkedList* _stpLinkedList, int _iKey, int _iType, void* _vpData )
{
    stCDStructLinkedListContainer       *dummy;

    // 1. mutex ���
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 2. LinkedList�ȿ� �����͸� ���� �� ����(���� LinkedList, m_stIdleList)��
    //    ���� ���(LinkedList Full)
    if( _stpLinkedList->m_stIdleList.m_reference <= 0 )
    {
        pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

        return  CD_STRUCTURE_LINKED_LIST_FULL;
    }

    // 3. �������(m_stIdleList)���� �����͸� ���� �� ����(dummy)�� �̾Ƴ���
    if( ( dummy = CDStructLinkedListPopLinkedList( &(_stpLinkedList->m_stIdleList) ) ) == NULL )
    {
        pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

        return  CD_STRUCTURE_ERROR;
    }

    // 4. ���ۿ� ���ڷ� ���� �����Ͱ��� ����
    dummy->key  = _iKey;
    dummy->type = _iType;
    dummy->data = _vpData;

    // 5. �������(���� LinkedList, m_stOccupiedList)�� �ش� �����͸� ����
    if( ( CDStructLinkedListPushLinkedList( &(_stpLinkedList->m_stOccupiedList), dummy ) ) == NULL)
    {
        pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

        return  CD_STRUCTURE_ERROR;
    }

    // 6. mutex ��� ����
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 7. ����!!
    return  CD_STRUCTURE_LINKED_LIST_PUSH;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*, int, int*, void**                      *
*             _stpLinkedList : �����͸� �̾�(Pop, Get)������ �ϴ�              *
*                         LinkedList ������Ʈ(����ü) �ּ�                     *
*             _iKey     : ã���� �ϴ� �������� Key��                           *
*             _ipType   : �����ϴ� LinkedList �������� Type                    *
*             _vppData  : �����ϰ��� �ϴ� �������� �ּ�                        *
*                                                                              *
* Return    : int, ����(3), ����(1, -1)                                        *
* Stability : MT-Safe                                                          *
* Explain   : �����͸� �̾Ƴ����� �ϴ� LinkedList�� ������Ʈ(����ü)           *
*             �ּ�(_stpLinkedList)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ���� Ű(_iKey) *
*             ���� ������ �����͸� �̾Ƴ���.                                   *
*             �̾Ƴ� �����ʹ� ����(_vppData) �� ����Ǹ� ��������              *
*             ����(Type)�� _ipType �� ����ȴ�.                                *
*             LinkedList�� Ư�� �������� �ּҰ��� �����ϴ�                     *
*             LinkedList�̹Ƿ� Pop�ÿ� �������� ����(Type), �������� �ּҸ�    *
*             �̾Ƴ���. LinkedList�� ���� �����Ͱ� �������� ������ �̹Ƿ� �ش� *
*             �����͸� �����ϱ� ���� �����ͺ����� ������(_vppData)��           *
*             ���ڰ����� �Ѱ��ش�. ������ CD_STRUCTURE_LINKED_LIST_POP(2)��    *
*             ��ȯ�ϸ� ���н� CD_STRUCTURE_ERROR(-1)�� ��ȯ�ϸ� LinkedList��   *
*             �����Ͱ� ������쿡�� CD_STRUCTURE_LINKED_LIST_EMPTY(1)��        *
*             ��ȯ�Ѵ�.                                                        *
*******************************************************************************/
int CDStructureLinkedListPop( stCDStructureLinkedList* _stpLinkedList, int _iKey, int* _ipType, void** _vppData )
{
    stCDStructLinkedListContainer       *dummy;

    // 1. mutex ���
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 2. LinkedList�ȿ� �̾Ƴ� ������(���� LinkedList, m_stOccupiedList)��
    //    ������� ���(LinkedList Empty)
    if( _stpLinkedList->m_stOccupiedList.m_reference <= 0 )
    {
        pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

        return  CD_STRUCTURE_LINKED_LIST_EMPTY;
    }

    // 3. �������(m_stOccupiedList)���� ������(dummy)�� �̾Ƴ���
    if( ( dummy = CDStructLinkedListSearchAndPopLinkedList( &(_stpLinkedList->m_stOccupiedList), _iKey ) ) == NULL )
    {
        pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

        return  CD_STRUCTURE_LINKED_LIST_NOT_FOUND;
    }

    // 4. ������ۿ��� �̾Ƴ� �����͸� ���ڰ����� ���� ������ ����
    *_ipType    = dummy->type;
    *_vppData   = dummy->data;

    // 5. ó���� ���� �����ʹ� �ٽ� �������(���� LinkedList, m_stIdleList)�� ����
    if( ( CDStructLinkedListPushLinkedList( &(_stpLinkedList->m_stIdleList), dummy ) ) == NULL)
    {
        pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

        return  CD_STRUCTURE_ERROR;
    }

    // 6. mutex ��� ����
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 7. ����!!
    return  CD_STRUCTURE_LINKED_LIST_POP;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpLinkedList : ���� �� �������� ���ڸ� �˰��� �ϴ� LinkedList  *
*                              ����ü�� �ּҰ�                                 *
*                                                                              *
* Return    : int, ����(LinkedList ������ ���緮), ����(-1)                    *
* Stability : MT-Safe                                                          *
* Explain   : ���� �� �������� ���ڸ� �˰��� �ϴ� LinkedList ������Ʈ(����ü)  *
*             �ּҰ�(_stpLinkedList)�� ���ڷ� �޾Ƽ� �ش� ������Ʈ�� ������    *
*             ���緮�� ��ȯ�Ѵ�.                                               *
*******************************************************************************/
int CDStructureLinkedListGetUseCount( stCDStructureLinkedList* _stpLinkedList )
{
    int iReference;

    if( _stpLinkedList == NULL )
        return  CD_STRUCTURE_ERROR;

    // 1. mutex ���
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 2. ���� LinkedList ���� ī��Ʈ�� ������ ����
    iReference  = _stpLinkedList->m_stOccupiedList.m_reference;

    // 3. mutex ��� ����
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 4. ����!!
    return  iReference;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpLinkedList : ���� ������ �������� ����(������)�� �˰��� �ϴ� *
*                              LinkedList ����ü�� �ּҰ�                      *
*                                                                              *
* Return    : int, ����(LinkedList ������ ���緮), ����(-1)                    *
* Stability : MT-Safe                                                          *
* Explain   : ���� ������ �������� ������ �˰��� �ϴ� LinkedList               *
*             ������Ʈ(����ü) �ּҰ�(_stpLinkedList)�� ���ڷ� �޾Ƽ� �ش�     *
*             ������Ʈ�� ������ ���緮�� ��ȯ�Ѵ�.                             *
*******************************************************************************/
int CDStructureLinkedListGetIdleCount( stCDStructureLinkedList* _stpLinkedList )
{
    int iReference;

    if( _stpLinkedList == NULL )
        return  CD_STRUCTURE_ERROR;

    // 1. mutex ���
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 2. ���� LinkedList ���� ī��Ʈ�� ������ ����
    iReference  = _stpLinkedList->m_stIdleList.m_reference;

    // 3. mutex ��� ����
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 4. ����!!
    return  iReference;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpNode : �ʱ�ȭ �ϰ��� �ϴ� LinkedList ������Ʈ(����ü �ּ�)   *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : �ʱ�ȭ �ϰ��� �ϴ� LinkedList ������Ʈ(����ü) �ּҰ�(_stpNode)��*
*             ���ڷ� �޾Ƽ� �ش� ������Ʈ�� �ʱ�ȭ(NULL)�� �����ϰ�            *
*             ���� �������� �ʱ�ȭ�Ѵ�.                                        *
*******************************************************************************/
static void CDStructLinkedListInitLinkedList( stCDStructureLinkedListNode* _stpNode )
{
    // 1. LinkedList ������Ʈ(����ü)�� 0x00(NULL)�� �ʱ�ȭ
    memset( _stpNode,0,sizeof( stCDStructureLinkedListNode ) );

    // 2. LinkedList ������Ʈ�� ó��(m_head)�� ��(m_tail)�� ����Ű��
    //    Node ���� �ʱ�ȭ
    _stpNode->m_head.next = &_stpNode->m_tail;
    _stpNode->m_tail.prev = &_stpNode->m_head;

    // 3. ����!!
    return;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpNode : �����͸� �߰��ϰ��� �ϴ� LinkedList ������Ʈ          *
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�       *
*             �ش� LinkedList�� ������(dummy)�� �����ϰ� �߰��Ѵ�.             *
*             �ش� Node�� LinkedList�� �����Ͱ� ���� �� �� �����͸� ����       *
*             �� ���۰����̴�.                                                 *
*******************************************************************************/
static int CDStructLinkedListAppendTailLinkedList( stCDStructureLinkedListNode* _stpNode )
{
    stCDStructLinkedListContainer       *dummy  =   NULL;

    // 1. heap ������ Node �����͸� ����
    if( ( dummy = ( stCDStructLinkedListContainer* )malloc( sizeof( stCDStructLinkedListContainer ) ) ) == NULL )
        return CD_STRUCTURE_ERROR;

    // 2. ���� �����͸� 0x00(NULL)�� �ʱ�ȭ
    memset( dummy, 0x00, sizeof( stCDStructLinkedListContainer ) );

    // 3. ���� �� �����͸� LinkedList ���� ���� �߰�
    dummy->next = &_stpNode->m_tail;
    dummy->prev = _stpNode->m_tail.prev;
    dummy->prev->next = dummy;

    _stpNode->m_tail.prev = dummy;
    _stpNode->m_reference++;

    // 4. ����!!
    return CD_STRUCTURE_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpNode : ���� ���� �����͸� �����ϰ��� �ϴ� LinkedList ������Ʈ*
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�       *
*             �ش� LinkedList�� ���� ��(m_tail.prev)�� �����͸� �����Ѵ�.      *
*             ListedList�� ���� �������� ���� ���� �����ʹ� m_tail.prev �̴�.  *
*             �����͸� ����(free)�� reference ���ڸ� 1 ���ҽ�Ų��.             *
*******************************************************************************/
static int CDStructLinkedListRemoveTailLinkedList( stCDStructureLinkedListNode* _stpNode )
{
    stCDStructLinkedListContainer       *dummy  =   NULL;

    if( _stpNode->m_head.next == &_stpNode->m_tail )
        return  CD_STRUCTURE_ERROR;

    // 1. ������ LinkedList �������� �� �����͸� ����
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
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*, stCDStructLinkedListContainer*         *
*             _stpNode     : �����͸� �����ϰ��� �ϴ� LinkedList ������Ʈ      *
*                            (����ü �ּ�)                                     *
*             _stpPushData : ListedList�� ����Ǵ� �������� �ּ�               *
*                                                                              *
* Return    : stCDStructLinkedListContainer*                                   *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�       *
*             �ش� LinkedList�� ������(_stpPushData)�� �����Ѵ�.               *
*             ������ ���� �� reference ���� 1 ������Ų��.                      *
*******************************************************************************/
static stCDStructLinkedListContainer* CDStructLinkedListPushLinkedList( stCDStructureLinkedListNode* _stpNode, stCDStructLinkedListContainer* _stpPushData )
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
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpNode : �����͸� �̾Ƴ����� �ϴ� LinkedList ������Ʈ          *
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : stCDStructLinkedListContainer*                                   *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�       *
*             �ش� LinkedList���� ������(dummy)�� �̾Ƴ���.                    *
*             �����͸� �̾Ƴ� �� reference ���� 1 ���ҽ�Ų��.                  *
*******************************************************************************/
static stCDStructLinkedListContainer* CDStructLinkedListPopLinkedList( stCDStructureLinkedListNode* _stpNode )
{
    stCDStructLinkedListContainer       *dummy  =   NULL;

    if( _stpNode->m_tail.prev == &_stpNode->m_head )
        return  NULL;

    dummy               = _stpNode->m_tail.prev;

    dummy->prev->next   = dummy->next;
    dummy->next->prev   = dummy->prev;

    _stpNode->m_reference--;

    return dummy;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpNode : �����͸� �̾Ƴ����� �ϴ� LinkedList ������Ʈ          *
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : stCDStructLinkedListContainer*                                   *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�       *
*             �ش� LinkedList���� ������(dummy)�� �̾Ƴ���.                    *
*             �����͸� �̾Ƴ� �� reference ���� 1 ���ҽ�Ų��.                  *
*******************************************************************************/
static stCDStructLinkedListContainer* CDStructLinkedListSearchAndPopLinkedList( stCDStructureLinkedListNode* _stpNode, int _iKey )
{
    stCDStructLinkedListContainer       *dummy  =   NULL;

    if( _stpNode->m_tail.prev == &_stpNode->m_head )
        return  NULL;

    for( dummy = _stpNode->m_head.next; dummy->next; dummy = dummy->next )
    {
        if( dummy->key == _iKey )
        {
            dummy->prev->next   = dummy->next;
            dummy->next->prev   = dummy->prev;

            _stpNode->m_reference--;

            return  dummy;
        }
    }

    return  NULL;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpNode : ��� �����͸� �����ϰ��� �ϴ� LinkedList ������Ʈ     *
*                        (����ü �ּ�)                                         *
*                                                                              *
* Return    : ����                                                             *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�       *
*             �ش� LinkedList�� ��� �����͸� �����Ѵ�.                        *
*******************************************************************************/
static void CDStructLinkedListDeleteAllLinkedList( stCDStructureLinkedListNode* _stpNode )
{
    stCDStructLinkedListContainer *offset = NULL;

    if( _stpNode->m_reference <= 0 )
        return;

    for( offset = _stpNode->m_head.next; offset->next; offset = offset->next )
        offset = CDStructLinkedListDeleteLinkedList( _stpNode, offset );

    return;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpNode       : Ư�� �����͸� �����ϰ��� �ϴ�                   *
*                              LinkedList ������Ʈ (����ü �ּ�)               *
*             _stpDeleteNode : �����ϰ��� �ϴ� ������                          *
*                                                                              *
* Return    : stCDStructLinkedListContainer*                                   *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList ������Ʈ(����ü) �ּҰ�(_stpNode)�� ���ڷ� �޾�       *
*             �ش� LinkedList���� Ư��������(_stpDeleteNode)�� �����Ѵ�.       *
*             ������ ���� �� ���� �������� ���� ������(_stpDeleteNode->prev)�� *
*             �ּ�(prev)�� ��ȯ�Ѵ�. ���� �� reference ���� 1 ���ҽ�Ų��.      *
*             �����ؾ� �� ���� ������ Ư�� ������(_stpDeleteNode)�� �ݵ��     *
*             LinkedList ������Ʈ(_stpNode)�ȿ� �����ϴ� �����Ϳ����Ѵ�.       *
*******************************************************************************/
static stCDStructLinkedListContainer* CDStructLinkedListDeleteLinkedList( stCDStructureLinkedListNode* _stpNode, stCDStructLinkedListContainer* _stpDeleteNode )
{
    stCDStructLinkedListContainer *prev;

    _stpDeleteNode->prev->next = _stpDeleteNode->next;
    _stpDeleteNode->next->prev = _stpDeleteNode->prev;
    prev = _stpDeleteNode->prev;

    free( _stpDeleteNode );

    _stpNode->m_reference--;

    return prev;
}

