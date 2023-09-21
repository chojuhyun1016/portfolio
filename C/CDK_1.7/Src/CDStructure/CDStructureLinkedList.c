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
* Argument  : 없음                                                             *
*                                                                              *
* Return    : stCDStructLinkedListContainer*,                                  *
*             성공(stCDStructLinkedListContainer*),                            *
*             실패(NULL)                                                       *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList 오브젝트(구조체)를 heap 영역에 생성하고 생성한        *
*             오브젝트의 주소를 반환한다.                                      *
*             성공한 경우 오브젝트의 주소(stCDStructureLinkedList*)를 반환하며 *
*             실패한 경우 NULL 을 반환한다. LinkedList 오브젝트를 지역으로     *
*             선언하면 다중 Thread에서 이상 동작을 할수 있으므로 생성이나      *
*             선언시 반드시 CDStructureLinkedListMake() 함수를 통해서 생성하고 *
*             사용한다.                                                        *
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
*             _stpLinkedList : 초기화 하고자 하는 LinkedList                   *
*                              오브젝트(구조체)의 주소                         *
*             _iPrepare : 생성할 LinkedList 데이터의 수                        *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : 초기화 하고자 하는 LinkedList 오브젝트(구조체)                   *
*             주소값(_stpLinkedList)을 인자로 받아서 해당 오브젝트를           *
*             초기화(NULL)로 셋팅후 동기화 변수(mutex)등을 초기과 후           *
*             _iPrepare(초기 LinkedList 생성 수) 만큼 데이터를 생성한다.       *
*             최대 스택의 적재량은 _iPrepare가 된다.                           *
*******************************************************************************/
int CDStructureLinkedListInit( stCDStructureLinkedList* _stpLinkedList, int _iPrepare )
{
    int iAppendLoop;

    if( _stpLinkedList == NULL )
        return  CD_STRUCTURE_ERROR;

    if( _iPrepare < 0 )
        return  CD_STRUCTURE_ERROR;

    // 1. mutex 변수 초기화
    pthread_mutex_init( &(_stpLinkedList->m_iLock), NULL );

    // 2. mutex 잠금
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 3. LinkedList 오브젝트 내의 LinkedList 초기화(사용중 List, 가용 List)
    CDStructLinkedListInitLinkedList( &(_stpLinkedList->m_stOccupiedList) );
    CDStructLinkedListInitLinkedList( &(_stpLinkedList->m_stIdleList) );

    // 4. _iPrepare 만큼 LinkedList 버퍼 생성
    //    가용 버퍼(m_stIdleList)에 생성한다.
    for( iAppendLoop = 0; iAppendLoop < _iPrepare; iAppendLoop++ )
    {
        // 4.1 데이터를 생성해서 Linkedlist 버퍼에 Append
        if( CDStructLinkedListAppendTailLinkedList(  &(_stpLinkedList->m_stIdleList) ) == CD_STRUCTURE_ERROR )
        {
            pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

            return  CD_STRUCTURE_ERROR;
        }
    }

    // 5. mutex 잠금 해제
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 6. 성공!!
    return  CD_STRUCTURE_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/11/21                                                       *
* Argument  : stCDStructureLinkedList**                                        *
*             _stppLinkedList : 해제 하고자 하는 LinkedList 오브젝트(구조체)의 *
*                               주소를 저장하고 있는 변수의 주소               *
*                               (포인터의 포인터)                              *
*                                                                              *
* Return    : 없음                                                             *
* Stability : MT-Safe                                                          *
* Explain   : 해제(삭제) 하고자 하는 LinkedList 오브젝트(구조체)               *
*             주소(_stppLinkedList)를 저장하고 있는 변수의 주소                *
*             (포인터의 포인터)를 인자로 받아서 해당 오브젝트안의 데이터를     *
*             모두 해제(삭제)한다.                                             *
*             오브젝트안의 LinkedList 데이터를 모두 해제(삭제)한 후            *
*             오브젝트 또한 초기화한다.                                        *
*******************************************************************************/
void CDStructureLinkedListFree( stCDStructureLinkedList** _stppLinkedList )
{
    if( _stppLinkedList == NULL )
        return;

    if( *_stppLinkedList == NULL )
        return;

    // 1. mutex 잠금
    pthread_mutex_lock( &((*_stppLinkedList)->m_iLock) );

    // 2. LinkedList 안의 모든 데이터(LinkedList) 데이터를 해제(삭제)
    CDStructLinkedListDeleteAllLinkedList( &((*_stppLinkedList)->m_stOccupiedList) );
    CDStructLinkedListDeleteAllLinkedList( &((*_stppLinkedList)->m_stIdleList) );

    // 3. mutex 잠금 해제
    pthread_mutex_unlock( &((*_stppLinkedList)->m_iLock) );

    // 4. mutex 변수 해제
    pthread_mutex_destroy( &((*_stppLinkedList)->m_iLock) );

    // 5. Heap 공간에 할당 된 LinkedList 오브젝트 해제
    free( *_stppLinkedList );

    // 6. 오브젝트가 삭제되었으므로 NULL 셋팅
    *_stppLinkedList    = NULL;

    // 7. 종료!!
    return;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*, int                                    *
*             _stpLinkedList : 확장 하고자 하는 LinkedList                     *
*                              오브젝트(구조체)의 주소                         *
*             _iAppendCount  : 확장 할 데이터의 수                             *
*                                                                              *
* Return    : int, 추가 할당 된 버퍼의 수                                      *
* Stability : MT-Safe                                                          *
* Explain   : 확장 하고자 하는 LinkedList 오브젝트(구조체)                     *
*             주소값(_stpLinkedList)을 인자로 받아서 해당 오브젝트의           *
*             가용버퍼에 iAppendCount 만큼 버퍼를 추가 할당한다.               *
*             가용버퍼의 크기만큼 데이터를 저장할수 있으므로 가용 버퍼의       *
*             확장은 전체 Linked List의 사이즈를 늘리는 것이다.                *
*             인자값(_stpLinkedList, _iAppendCount)이 잘못된 경우를 제외하고는 *
*             추가로 할당한 데이터의 개수를 반환한다.                          *
*             인자값이 잘못 된 경우에는 CD_STRUCTURE_ERROR(-1)을 반환한다.     *
*******************************************************************************/
int CDStructureLinkedListAppend( stCDStructureLinkedList* _stpLinkedList, int _iAppendCount )
{
    int iAppendLoop;

    if( _stpLinkedList == NULL )
        return  CD_STRUCTURE_ERROR;

    if( _iAppendCount < 0 )
        return  CD_STRUCTURE_ERROR;

    // 1. mutex 잠금
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 2. 추가로 할당하는 숫자만큼 루프를 돌면서 가용버퍼 추가 할당
    for( iAppendLoop = 0; iAppendLoop < _iAppendCount; iAppendLoop++ )
    {
        // 2.1 가용버퍼 한개 추가 할당
        if( CDStructLinkedListAppendTailLinkedList(  &(_stpLinkedList->m_stIdleList) ) == CD_STRUCTURE_ERROR )
        {
            pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

            return  iAppendLoop;
        }
    }

    // 3. mutex 잠금 해제
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 4. 성공!!
    return  iAppendLoop;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*, int                                    *
*             _stpLinkedList : 감소 시키고자 하는 LinkedList                   *
*                              오브젝트(구조체)의 주소                         *
*             _iDeleteCount : 감소시킬 가용 버퍼 데이터의 수                   *
*                                                                              *
* Return    : int, 감소 된 버퍼의 수                                           *
* Stability : MT-Safe                                                          *
* Explain   : 감소시키고자 하는 LinkedList 오브젝트(구조체)                    *
*             주소값(_stpLinkedList)을 인자로 받아서 해당 오브젝트의           *
*             가용버퍼에 _iDeleteCount 만큼 버퍼를 감소(삭제)시킨다.           *
*             가용버퍼의 크기만큼 데이터를 저장할수 있으므로 가용 버퍼의       *
*             감소는 전체 Linked List의 사이즈를 줄이는 것이다.                *
*             인자값(_stpLinkedList, _iDeleteCount)이 잘못된 경우를 제외하고는 *
*             감소 된 데이터의 개수를 반환한다.                                *
*             인자값이 잘못 된 경우에는 CD_STRUCTURE_ERROR(-1)을 반환한다.     *
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

    // 1. 가용 버퍼의 데이터 개수가 줄이고자하는 개수보다 작으면
    //    줄이고자 하는 데이터의 개수를 가용 데이터의 개수로 셋팅
    if(  _stpLinkedList->m_stIdleList.m_reference < iDepleteMaxLoop )
        iDepleteMaxLoop = _stpLinkedList->m_stIdleList.m_reference;

    // 2. mutex 잠금
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 3. 추가로 할당하는 숫자만큼 루프를 돌면서 가용 버퍼 삭제
    for( iDepleteLoop = 0; iDepleteLoop < iDepleteMaxLoop; iDepleteLoop++ )
    {
        // 3.1 가용 버퍼 데이터 한개 삭제
        if( CDStructLinkedListRemoveTailLinkedList(  &(_stpLinkedList->m_stIdleList) ) == CD_STRUCTURE_ERROR )
        {
            pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

            return  iDepleteLoop;
        }
    }

    // 4. mutex 잠금 해제
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 5. 성공!!
    return  iDepleteLoop;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*, int, int, void*                        *
*             _stpLinkedList : 데이터를 삽입(Insert, Push) 하고자 하는         *
*                         LinkedList 오브젝트(구조체) 주소                     *
*             _iKey     : 삽입하고 데이터의 Key값(Pop시 필요)                  *
*             _iType    : 삽입하는 LinkedList 데이터의 Type                    *
*             _vpData   : 삽입하고자 하는 데이터의 주소                        *
*                                                                              *
* Return    : int, 성공(3), 실패(1, -1)                                        *
* Stability : MT-Safe                                                          *
* Explain   : 데이터를 삽입하고자 하는 LinkedList의 오브젝트(구조체)           *
*             주소(_stpLinkedList)를 인자로 받아서 해당 오브젝트에             *
*             데이터(_vpData)를 삽입한다.                                      *
*             삽입시 해당 데이터의 키(_iKey)와 종류(Type)도 함께 삽입된다.     *
*             Pop 함수를 통해 데이터를 다시 뽑아낼 시에 LinkedList의           *
*             데이터중에 키(_iKey)값이 동일한 데이터의 종류(_iType)와          *
*             데이터(_vpData)를 뽑아내게 된다.                                 *
*             LinkedList의 내부 LinkedList은 데이터의 주소(Pointer)값을        *
*             저장한다.                                                        *
*******************************************************************************/
int CDStructureLinkedListPush( stCDStructureLinkedList* _stpLinkedList, int _iKey, int _iType, void* _vpData )
{
    stCDStructLinkedListContainer       *dummy;

    // 1. mutex 잠금
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 2. LinkedList안에 데이터를 저장 할 버퍼(가용 LinkedList, m_stIdleList)가
    //    없을 경우(LinkedList Full)
    if( _stpLinkedList->m_stIdleList.m_reference <= 0 )
    {
        pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

        return  CD_STRUCTURE_LINKED_LIST_FULL;
    }

    // 3. 가용버퍼(m_stIdleList)에서 데이터를 저장 할 버퍼(dummy)를 뽑아낸다
    if( ( dummy = CDStructLinkedListPopLinkedList( &(_stpLinkedList->m_stIdleList) ) ) == NULL )
    {
        pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

        return  CD_STRUCTURE_ERROR;
    }

    // 4. 버퍼에 인자로 받은 데이터값을 복사
    dummy->key  = _iKey;
    dummy->type = _iType;
    dummy->data = _vpData;

    // 5. 적재버퍼(적재 LinkedList, m_stOccupiedList)에 해당 데이터를 삽입
    if( ( CDStructLinkedListPushLinkedList( &(_stpLinkedList->m_stOccupiedList), dummy ) ) == NULL)
    {
        pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

        return  CD_STRUCTURE_ERROR;
    }

    // 6. mutex 잠금 해제
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 7. 성공!!
    return  CD_STRUCTURE_LINKED_LIST_PUSH;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*, int, int*, void**                      *
*             _stpLinkedList : 데이터를 뽑아(Pop, Get)내고자 하는              *
*                         LinkedList 오브젝트(구조체) 주소                     *
*             _iKey     : 찾고자 하는 데이터의 Key값                           *
*             _ipType   : 삽입하는 LinkedList 데이터의 Type                    *
*             _vppData  : 삽입하고자 하는 데이터의 주소                        *
*                                                                              *
* Return    : int, 성공(3), 실패(1, -1)                                        *
* Stability : MT-Safe                                                          *
* Explain   : 데이터를 뽑아내고자 하는 LinkedList의 오브젝트(구조체)           *
*             주소(_stpLinkedList)를 인자로 받아서 해당 오브젝트에서 키(_iKey) *
*             값이 동일한 데이터를 뽑아낸다.                                   *
*             뽑아낸 데이터는 버퍼(_vppData) 에 저장되며 데이터의              *
*             종류(Type)는 _ipType 에 저장된다.                                *
*             LinkedList은 특정 데이터의 주소값을 저장하는                     *
*             LinkedList이므로 Pop시에 데이터의 종류(Type), 데이터의 주소를    *
*             뽑아낸다. LinkedList의 적재 데이터가 데이터의 포인터 이므로 해당 *
*             포인터를 저장하기 위해 포인터변수의 포인터(_vppData)를           *
*             인자값으로 넘겨준다. 성공시 CD_STRUCTURE_LINKED_LIST_POP(2)를    *
*             반환하며 실패시 CD_STRUCTURE_ERROR(-1)을 반환하며 LinkedList에   *
*             데이터가 없을경우에는 CD_STRUCTURE_LINKED_LIST_EMPTY(1)를        *
*             반환한다.                                                        *
*******************************************************************************/
int CDStructureLinkedListPop( stCDStructureLinkedList* _stpLinkedList, int _iKey, int* _ipType, void** _vppData )
{
    stCDStructLinkedListContainer       *dummy;

    // 1. mutex 잠금
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 2. LinkedList안에 뽑아낼 데이터(적재 LinkedList, m_stOccupiedList)가
    //    비어있을 경우(LinkedList Empty)
    if( _stpLinkedList->m_stOccupiedList.m_reference <= 0 )
    {
        pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

        return  CD_STRUCTURE_LINKED_LIST_EMPTY;
    }

    // 3. 가용버퍼(m_stOccupiedList)에서 데이터(dummy)를 뽑아낸다
    if( ( dummy = CDStructLinkedListSearchAndPopLinkedList( &(_stpLinkedList->m_stOccupiedList), _iKey ) ) == NULL )
    {
        pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

        return  CD_STRUCTURE_LINKED_LIST_NOT_FOUND;
    }

    // 4. 가용버퍼에서 뽑아낸 데이터를 인자값으로 받은 변수에 복사
    *_ipType    = dummy->type;
    *_vppData   = dummy->data;

    // 5. 처리가 끝난 데이터는 다시 가용버퍼(가용 LinkedList, m_stIdleList)에 삽입
    if( ( CDStructLinkedListPushLinkedList( &(_stpLinkedList->m_stIdleList), dummy ) ) == NULL)
    {
        pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

        return  CD_STRUCTURE_ERROR;
    }

    // 6. mutex 잠금 해제
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 7. 성공!!
    return  CD_STRUCTURE_LINKED_LIST_POP;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpLinkedList : 적재 된 데이터의 숫자를 알고자 하는 LinkedList  *
*                              구조체의 주소값                                 *
*                                                                              *
* Return    : int, 성공(LinkedList 데이터 적재량), 실패(-1)                    *
* Stability : MT-Safe                                                          *
* Explain   : 적재 된 데이터의 숫자를 알고자 하는 LinkedList 오브젝트(구조체)  *
*             주소값(_stpLinkedList)을 인자로 받아서 해당 오브젝트의 데이터    *
*             적재량을 반환한다.                                               *
*******************************************************************************/
int CDStructureLinkedListGetUseCount( stCDStructureLinkedList* _stpLinkedList )
{
    int iReference;

    if( _stpLinkedList == NULL )
        return  CD_STRUCTURE_ERROR;

    // 1. mutex 잠금
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 2. 현재 LinkedList 적재 카운트를 변수에 복사
    iReference  = _stpLinkedList->m_stOccupiedList.m_reference;

    // 3. mutex 잠금 해제
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 4. 성공!!
    return  iReference;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpLinkedList : 적재 가능한 데이터의 숫자(여유량)를 알고자 하는 *
*                              LinkedList 구조체의 주소값                      *
*                                                                              *
* Return    : int, 성공(LinkedList 데이터 적재량), 실패(-1)                    *
* Stability : MT-Safe                                                          *
* Explain   : 적재 가능한 데이터의 개수를 알고자 하는 LinkedList               *
*             오브젝트(구조체) 주소값(_stpLinkedList)을 인자로 받아서 해당     *
*             오브젝트의 데이터 적재량을 반환한다.                             *
*******************************************************************************/
int CDStructureLinkedListGetIdleCount( stCDStructureLinkedList* _stpLinkedList )
{
    int iReference;

    if( _stpLinkedList == NULL )
        return  CD_STRUCTURE_ERROR;

    // 1. mutex 잠금
    pthread_mutex_lock( &(_stpLinkedList->m_iLock) );

    // 2. 현재 LinkedList 적재 카운트를 변수에 복사
    iReference  = _stpLinkedList->m_stIdleList.m_reference;

    // 3. mutex 잠금 해제
    pthread_mutex_unlock( &(_stpLinkedList->m_iLock) );

    // 4. 성공!!
    return  iReference;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpNode : 초기화 하고자 하는 LinkedList 오브젝트(구조체 주소)   *
*                                                                              *
* Return    : 없음                                                             *
* Stability : MT-Safe                                                          *
* Explain   : 초기화 하고자 하는 LinkedList 오브젝트(구조체) 주소값(_stpNode)을*
*             인자로 받아서 해당 오브젝트를 초기화(NULL)로 셋팅하고            *
*             내부 변수들을 초기화한다.                                        *
*******************************************************************************/
static void CDStructLinkedListInitLinkedList( stCDStructureLinkedListNode* _stpNode )
{
    // 1. LinkedList 오브젝트(구조체)를 0x00(NULL)로 초기화
    memset( _stpNode,0,sizeof( stCDStructureLinkedListNode ) );

    // 2. LinkedList 오브젝트의 처음(m_head)과 끝(m_tail)을 가르키는
    //    Node 변수 초기화
    _stpNode->m_head.next = &_stpNode->m_tail;
    _stpNode->m_tail.prev = &_stpNode->m_head;

    // 3. 종료!!
    return;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpNode : 데이터를 추가하고자 하는 LinkedList 오브젝트          *
*                        (구조체 주소)                                         *
*                                                                              *
* Return    : 없음                                                             *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList 오브젝트(구조체) 주소값(_stpNode)을 인자로 받아       *
*             해당 LinkedList에 데이터(dummy)를 생성하고 추가한다.             *
*             해당 Node는 LinkedList에 데이터가 삽입 될 시 데이터를 저장       *
*             할 버퍼공간이다.                                                 *
*******************************************************************************/
static int CDStructLinkedListAppendTailLinkedList( stCDStructureLinkedListNode* _stpNode )
{
    stCDStructLinkedListContainer       *dummy  =   NULL;

    // 1. heap 공간에 Node 데이터를 생성
    if( ( dummy = ( stCDStructLinkedListContainer* )malloc( sizeof( stCDStructLinkedListContainer ) ) ) == NULL )
        return CD_STRUCTURE_ERROR;

    // 2. 생성 데이터를 0x00(NULL)로 초기화
    memset( dummy, 0x00, sizeof( stCDStructLinkedListContainer ) );

    // 3. 생성 된 데이터를 LinkedList 제일 끝에 추가
    dummy->next = &_stpNode->m_tail;
    dummy->prev = _stpNode->m_tail.prev;
    dummy->prev->next = dummy;

    _stpNode->m_tail.prev = dummy;
    _stpNode->m_reference++;

    // 4. 성공!!
    return CD_STRUCTURE_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpNode : 제일 끝의 데이터를 삭제하고자 하는 LinkedList 오브젝트*
*                        (구조체 주소)                                         *
*                                                                              *
* Return    : 없음                                                             *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList 오브젝트(구조체) 주소값(_stpNode)을 인자로 받아       *
*             해당 LinkedList의 제일 끝(m_tail.prev)의 데이터를 삭제한다.      *
*             ListedList의 적재 데이터중 가장 끝의 데이터는 m_tail.prev 이다.  *
*             데이터를 삭제(free)후 reference 숫자를 1 감소시킨다.             *
*******************************************************************************/
static int CDStructLinkedListRemoveTailLinkedList( stCDStructureLinkedListNode* _stpNode )
{
    stCDStructLinkedListContainer       *dummy  =   NULL;

    if( _stpNode->m_head.next == &_stpNode->m_tail )
        return  CD_STRUCTURE_ERROR;

    // 1. 삭제할 LinkedList 데이터의 앞 데이터를 저장
    dummy   = _stpNode->m_tail.prev;

    // 2. 삭제할 데이터의 전(prev) 데이터와
    //    삭제할 데이터의 후(next) 데이터를 연결
    dummy->prev->next   = &_stpNode->m_tail;
    _stpNode->m_tail.prev   = dummy->prev;

    // 3. 데이터 삭제
    free( dummy );

    // 4. reference 카운트 1감소
    _stpNode->m_reference--;

    // 5. 성공!!
    return CD_STRUCTURE_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*, stCDStructLinkedListContainer*         *
*             _stpNode     : 데이터를 삽입하고자 하는 LinkedList 오브젝트      *
*                            (구조체 주소)                                     *
*             _stpPushData : ListedList에 저장되는 데이터의 주소               *
*                                                                              *
* Return    : stCDStructLinkedListContainer*                                   *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList 오브젝트(구조체) 주소값(_stpNode)을 인자로 받아       *
*             해당 LinkedList에 데이터(_stpPushData)를 삽입한다.               *
*             데이터 삽입 후 reference 수를 1 증가시킨다.                      *
*******************************************************************************/
static stCDStructLinkedListContainer* CDStructLinkedListPushLinkedList( stCDStructureLinkedListNode* _stpNode, stCDStructLinkedListContainer* _stpPushData )
{
    // 1. 추가하고자 하는 데이터와 이전 이후 데이터의 데이터를 셋팅
    _stpPushData->next = &_stpNode->m_tail;
    _stpPushData->prev = _stpNode->m_tail.prev;
    _stpPushData->prev->next = _stpPushData;

    _stpNode->m_tail.prev = _stpPushData;
    _stpNode->m_reference++;

    // 2. 성공!!
    return _stpPushData;
}


/*******************************************************************************
* Update    : 2012/10/19                                                       *
* Argument  : stCDStructureLinkedList*                                         *
*             _stpNode : 데이터를 뽑아내고자 하는 LinkedList 오브젝트          *
*                        (구조체 주소)                                         *
*                                                                              *
* Return    : stCDStructLinkedListContainer*                                   *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList 오브젝트(구조체) 주소값(_stpNode)을 인자로 받아       *
*             해당 LinkedList에서 데이터(dummy)를 뽑아낸다.                    *
*             데이터를 뽑아낸 후 reference 수를 1 감소시킨다.                  *
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
*             _stpNode : 데이터를 뽑아내고자 하는 LinkedList 오브젝트          *
*                        (구조체 주소)                                         *
*                                                                              *
* Return    : stCDStructLinkedListContainer*                                   *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList 오브젝트(구조체) 주소값(_stpNode)을 인자로 받아       *
*             해당 LinkedList에서 데이터(dummy)를 뽑아낸다.                    *
*             데이터를 뽑아낸 후 reference 수를 1 감소시킨다.                  *
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
*             _stpNode : 모든 데이터를 삭제하고자 하는 LinkedList 오브젝트     *
*                        (구조체 주소)                                         *
*                                                                              *
* Return    : 없음                                                             *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList 오브젝트(구조체) 주소값(_stpNode)을 인자로 받아       *
*             해당 LinkedList의 모든 데이터를 삭제한다.                        *
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
*             _stpNode       : 특정 데이터를 삭제하고자 하는                   *
*                              LinkedList 오브젝트 (구조체 주소)               *
*             _stpDeleteNode : 삭제하고자 하는 데이터                          *
*                                                                              *
* Return    : stCDStructLinkedListContainer*                                   *
* Stability : MT-Safe                                                          *
* Explain   : LinkedList 오브젝트(구조체) 주소값(_stpNode)을 인자로 받아       *
*             해당 LinkedList에서 특정데이터(_stpDeleteNode)를 삭제한다.       *
*             데이터 삭제 후 삭제 데이터의 이전 데이터(_stpDeleteNode->prev)의 *
*             주소(prev)를 반환한다. 삭제 후 reference 수를 1 감소시킨다.      *
*             주의해야 할 점은 삭제할 특정 데이터(_stpDeleteNode)가 반드시     *
*             LinkedList 오브젝트(_stpNode)안에 존재하는 데이터여야한다.       *
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


