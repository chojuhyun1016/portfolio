#include "CDThreadApi.h"
#include "CDThreadDefinitions.h"

#ifdef _SOLARIS_
    #include <pthread.h>
#elif _CENT_OS_
    #include <pthread.h>
#else
    #include <pthread.h>
#endif


/*******************************************************************************
* Update    : 2011/03/17                                                       *
* Argument  : pthread_attr_t*                                                  *
*             _stpAttr : 초기화 할 Thread 속성 객체의 포인터                   *
* Return    : int, 성공(0), 실패(-1)                                           *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument로 넘겨받은 Thread 속성 객체의 포인터(_stpAttr)가        *
*             가르키는 객체를 초기화 시킨다.                                   *
*******************************************************************************/
int CDThreadAttrInit( pthread_attr_t* _stpAttr )
{
    // 1. 뮤텍스 속성 객체 초기화
    if( pthread_attr_init( _stpAttr ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 2. 성공!!
    return CD_THREAD_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/03/17                                                       *
* Argument  : pthread_attr_t*                                                  *
*             _stpAttr : 제거하려는 Thread 속성 객체의 포인터                  *
* Return    : int, 성공(0), 실패(-1)                                           *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument로 넘겨받은 Thread 속성 객체의 포인터(_stpAttr)가        *
*             가르키는 객체를 제거(파괴)한다.                                  *
*******************************************************************************/
int CDThreadAttrDestroy( pthread_attr_t* _stpAttr )
{
    // 1. 뮤텍스 속성객체 삭제
    if( pthread_attr_destroy( _stpAttr ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 2. 성공!!
    return CD_THREAD_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/03/17                                                       *
* Argument  : pthread_attr_t*                                                  *
*             _stpAttr : 속성을 얻고자 하는 Thread 속성 객체의 포인터          *
* Return    : int                                                              *
*             성공(실행영역을 나타내는 정수)                                   *
*               PTHREAD_SCOPE_SYSTEM  : 0x01                                   *
*               PTHREAD_SCOPE_PROCESS : 0x00                                   *
*             실패(-1)                                                         *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument로 넘겨받은 속성 객체의 실행 영역(Scope)속성을 반환한다. *
*             반환되는 실행 영역 속성은                                        *
*             시스템영역 쓰레드(PTHREAD_SCOPE_SYSTEM(0x01))와                  *
*             유저영역 쓰레드(PTHREAD_SCOPE_PROCESS(0x00))로 나뉜다.           *
*             각 모드에 해당하는 디파인된 정수를 반환한다.                     *
*******************************************************************************/
int CDThreadGetScope( pthread_attr_t* _stpAttr )
{
    int iScope;

    // 1. 속성 객체에서 Thread 실행 영역 속성 추출
    if( pthread_attr_getscope( _stpAttr, &iScope ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 2. 성공!!
    return  iScope;
}


/*******************************************************************************
* Update    : 2012/11/15                                                       *
* Argument  : pthread_attr_t*, int                                             *
*             _stpAttr : 속성을 변경 하려는 Thread 속성 객체의 포인터          *
*             _iScope  : 쓰레드 속성객체의 실행영역                            *
* Return    : int, 성공(0), 실패(-1)                                           *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument로 넘겨받은 Thread 속성 객체의 포인터(_stpAttr)가        *
*             가르키는 객체의 실행영역 속성을 _iScope 으로 변경한다.           *
*******************************************************************************/
int CDThreadSetScope( pthread_attr_t* _stpAttr, int _iScope )
{
    // 1. Thread 속성 객체의 실행 영역 속성을 셋팅
    if( pthread_attr_setscope( _stpAttr, _iScope ) != CD_THREAD_SUCCESS )
        return CD_THREAD_ERROR;

    // 2. 성공!!
    return  CD_THREAD_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/03/17                                                       *
* Argument  : pthread_attr_t*                                                  *
*             _stpAttr : 속성을 얻고자 하는 Thread 속성 객체의 포인터          *
*                                                                              *
* Return    : int                                                              *
*             성공(Detach 속성을 나타내는 정수)                                *
*               PTHREAD_CREATE_DETACHED : 0x40                                 *
*               PTHREAD_CREATE_JOINABLE : 0x00                                 *
*             실패(-1)                                                         *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument로 넘겨받은 속성객체의 Detach 속성을 반환한다.           *
*             Detach 속성은 PTHREAD_CREATE_DETACHED(0x40)와                    *
*             PTHREAD_CREATE_JOINABLE(0x00) 속성으로 나뉜다.                   *
*             각 모드에 해당하는 디파인된 정수를 반환한다.                     *
*******************************************************************************/
int CDThreadGetDetach( pthread_attr_t* _stpAttr )
{
    int iDetach;

    // 1. Thread 속성 객체의 Detach 속성 추출
    if( pthread_attr_getdetachstate( _stpAttr, &iDetach ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 2. 성공!!
    return  iDetach;
}


/*******************************************************************************
* Update    : 2012/11/15                                                       *
* Argument  : pthread_attr_t*, int                                             *
*             _stpAttr : 변경하려는 Thread 속성 객체의 포인터                  *
*             _iDetach : 속성 객체의 쓰레드 분리 속성                          *
* Return    : int, 성공(0), 실패(-1)                                           *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument로 넘겨받은 Thread 속성 객체의 포인터(_stpAttr)가        *
*             가르키는 객체의 Thread 속성을 _iDetach로 셋팅한다.               *
*******************************************************************************/
int CDThreadSetDetach( pthread_attr_t* _stpAttr, int _iDetach )
{
    // 1. Thread 속성 객체의 Detach 속성 셋팅
    if( pthread_attr_setdetachstate( _stpAttr, _iDetach ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 2. 성공!!
    return  CD_THREAD_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/03/17                                                       *
* Argument  : pthread_t*, int, void* (fn)(void*), void*                        *
*             _pThreadID     : 생성되는 Thread의 Thread ID 가 저장될 변수의    *
*                              포인터                                          *
*             _pStartAddress : Thread 실행시 실행 될 함수의 함수포인터         *
*             _vpParameter   : Thread 실생시 Argument로 전달 할 특정 데이터의  *
*                              포인터                                          *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : Argument로 넘겨받은 데이터를 기반으로 신규 Thread를 생성하는     *
*             함수이다. 실행 시 Argument를 그대로 넘겨준다.                    *
*             Thread 실행 전 pthread_attr_t형 변수 stAttr을 초기화 및 셋팅하여 *
*             쓰레드의 속성을 변경한다.                                        *
*             변경 된 쓰레드의 속성은 CDThreadSetScope(커널 모드로 실행),      *
*             CDThreadSetDetach(Thread 분리)이다.                              *
*             위의 변경된 속성을 기반으로 신규 Thread가 생성된다.              *
*******************************************************************************/
int CDThreadBegin( pthread_t* _pThreadID, CD_THREAD_WORKER _pStartAddress, void* _vpParameter )
{
    pthread_attr_t  stAttr;

    // 1. 예외 처리
    if( _pStartAddress == NULL )
        return  CD_THREAD_ERROR;

    // 2. Thread 속성객체 초기화
    if( CDThreadAttrInit( &stAttr ) == CD_THREAD_ERROR )
        return  CD_THREAD_ERROR;

    // 3. Thread의 실행 영역을 셋팅(PTHREAD_SCOPE_SYSTEM)
    if( CDThreadSetScope( &stAttr, PTHREAD_SCOPE_SYSTEM ) == CD_THREAD_ERROR )
        return  CD_THREAD_ERROR;

    // 4. Thread의 Detach 속성을 셋팅(PTHREAD_CREATE_DETACHED)
    if( CDThreadSetDetach( &stAttr, PTHREAD_CREATE_DETACHED ) == CD_THREAD_ERROR )
        return  CD_THREAD_ERROR;

    // 5. Thread 생성
    if( pthread_create( _pThreadID, &stAttr, _pStartAddress, _vpParameter ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 6. 사용한 Thread 속성객체 삭제
    if( CDThreadAttrDestroy( &stAttr ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 7. 성공!!
    return  CD_THREAD_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : pthread_t                                                        *
*             _iThreadID : 종료시킬 Thread의 Thread ID                         *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : Thread ID(_iThreadID)를 Argument로 넘겨 받아 해당 Thread 를      *
*             종료시킨다.                                                      *
*******************************************************************************/
int CDThreadTerminate( pthread_t _iThreadID )
{
    // 1. Thread ID에 해당하는 Thread 종료
    if( pthread_cancel( _iThreadID ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 2. 성공!!
    return  CD_THREAD_SUCCESS;
}

