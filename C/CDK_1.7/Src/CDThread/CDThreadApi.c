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
*             _stpAttr : �ʱ�ȭ �� Thread �Ӽ� ��ü�� ������                   *
* Return    : int, ����(0), ����(-1)                                           *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument�� �Ѱܹ��� Thread �Ӽ� ��ü�� ������(_stpAttr)��        *
*             ����Ű�� ��ü�� �ʱ�ȭ ��Ų��.                                   *
*******************************************************************************/
int CDThreadAttrInit( pthread_attr_t* _stpAttr )
{
    // 1. ���ؽ� �Ӽ� ��ü �ʱ�ȭ
    if( pthread_attr_init( _stpAttr ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 2. ����!!
    return CD_THREAD_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/03/17                                                       *
* Argument  : pthread_attr_t*                                                  *
*             _stpAttr : �����Ϸ��� Thread �Ӽ� ��ü�� ������                  *
* Return    : int, ����(0), ����(-1)                                           *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument�� �Ѱܹ��� Thread �Ӽ� ��ü�� ������(_stpAttr)��        *
*             ����Ű�� ��ü�� ����(�ı�)�Ѵ�.                                  *
*******************************************************************************/
int CDThreadAttrDestroy( pthread_attr_t* _stpAttr )
{
    // 1. ���ؽ� �Ӽ���ü ����
    if( pthread_attr_destroy( _stpAttr ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 2. ����!!
    return CD_THREAD_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/03/17                                                       *
* Argument  : pthread_attr_t*                                                  *
*             _stpAttr : �Ӽ��� ����� �ϴ� Thread �Ӽ� ��ü�� ������          *
* Return    : int                                                              *
*             ����(���࿵���� ��Ÿ���� ����)                                   *
*               PTHREAD_SCOPE_SYSTEM  : 0x01                                   *
*               PTHREAD_SCOPE_PROCESS : 0x00                                   *
*             ����(-1)                                                         *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument�� �Ѱܹ��� �Ӽ� ��ü�� ���� ����(Scope)�Ӽ��� ��ȯ�Ѵ�. *
*             ��ȯ�Ǵ� ���� ���� �Ӽ���                                        *
*             �ý��ۿ��� ������(PTHREAD_SCOPE_SYSTEM(0x01))��                  *
*             �������� ������(PTHREAD_SCOPE_PROCESS(0x00))�� ������.           *
*             �� ��忡 �ش��ϴ� �����ε� ������ ��ȯ�Ѵ�.                     *
*******************************************************************************/
int CDThreadGetScope( pthread_attr_t* _stpAttr )
{
    int iScope;

    // 1. �Ӽ� ��ü���� Thread ���� ���� �Ӽ� ����
    if( pthread_attr_getscope( _stpAttr, &iScope ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 2. ����!!
    return  iScope;
}


/*******************************************************************************
* Update    : 2012/11/15                                                       *
* Argument  : pthread_attr_t*, int                                             *
*             _stpAttr : �Ӽ��� ���� �Ϸ��� Thread �Ӽ� ��ü�� ������          *
*             _iScope  : ������ �Ӽ���ü�� ���࿵��                            *
* Return    : int, ����(0), ����(-1)                                           *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument�� �Ѱܹ��� Thread �Ӽ� ��ü�� ������(_stpAttr)��        *
*             ����Ű�� ��ü�� ���࿵�� �Ӽ��� _iScope ���� �����Ѵ�.           *
*******************************************************************************/
int CDThreadSetScope( pthread_attr_t* _stpAttr, int _iScope )
{
    // 1. Thread �Ӽ� ��ü�� ���� ���� �Ӽ��� ����
    if( pthread_attr_setscope( _stpAttr, _iScope ) != CD_THREAD_SUCCESS )
        return CD_THREAD_ERROR;

    // 2. ����!!
    return  CD_THREAD_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/03/17                                                       *
* Argument  : pthread_attr_t*                                                  *
*             _stpAttr : �Ӽ��� ����� �ϴ� Thread �Ӽ� ��ü�� ������          *
*                                                                              *
* Return    : int                                                              *
*             ����(Detach �Ӽ��� ��Ÿ���� ����)                                *
*               PTHREAD_CREATE_DETACHED : 0x40                                 *
*               PTHREAD_CREATE_JOINABLE : 0x00                                 *
*             ����(-1)                                                         *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument�� �Ѱܹ��� �Ӽ���ü�� Detach �Ӽ��� ��ȯ�Ѵ�.           *
*             Detach �Ӽ��� PTHREAD_CREATE_DETACHED(0x40)��                    *
*             PTHREAD_CREATE_JOINABLE(0x00) �Ӽ����� ������.                   *
*             �� ��忡 �ش��ϴ� �����ε� ������ ��ȯ�Ѵ�.                     *
*******************************************************************************/
int CDThreadGetDetach( pthread_attr_t* _stpAttr )
{
    int iDetach;

    // 1. Thread �Ӽ� ��ü�� Detach �Ӽ� ����
    if( pthread_attr_getdetachstate( _stpAttr, &iDetach ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 2. ����!!
    return  iDetach;
}


/*******************************************************************************
* Update    : 2012/11/15                                                       *
* Argument  : pthread_attr_t*, int                                             *
*             _stpAttr : �����Ϸ��� Thread �Ӽ� ��ü�� ������                  *
*             _iDetach : �Ӽ� ��ü�� ������ �и� �Ӽ�                          *
* Return    : int, ����(0), ����(-1)                                           *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument�� �Ѱܹ��� Thread �Ӽ� ��ü�� ������(_stpAttr)��        *
*             ����Ű�� ��ü�� Thread �Ӽ��� _iDetach�� �����Ѵ�.               *
*******************************************************************************/
int CDThreadSetDetach( pthread_attr_t* _stpAttr, int _iDetach )
{
    // 1. Thread �Ӽ� ��ü�� Detach �Ӽ� ����
    if( pthread_attr_setdetachstate( _stpAttr, _iDetach ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 2. ����!!
    return  CD_THREAD_SUCCESS;
}


/*******************************************************************************
* Update    : 2011/03/17                                                       *
* Argument  : pthread_t*, int, void* (fn)(void*), void*                        *
*             _pThreadID     : �����Ǵ� Thread�� Thread ID �� ����� ������    *
*                              ������                                          *
*             _pStartAddress : Thread ����� ���� �� �Լ��� �Լ�������         *
*             _vpParameter   : Thread �ǻ��� Argument�� ���� �� Ư�� ��������  *
*                              ������                                          *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : Argument�� �Ѱܹ��� �����͸� ������� �ű� Thread�� �����ϴ�     *
*             �Լ��̴�. ���� �� Argument�� �״�� �Ѱ��ش�.                    *
*             Thread ���� �� pthread_attr_t�� ���� stAttr�� �ʱ�ȭ �� �����Ͽ� *
*             �������� �Ӽ��� �����Ѵ�.                                        *
*             ���� �� �������� �Ӽ��� CDThreadSetScope(Ŀ�� ���� ����),      *
*             CDThreadSetDetach(Thread �и�)�̴�.                              *
*             ���� ����� �Ӽ��� ������� �ű� Thread�� �����ȴ�.              *
*******************************************************************************/
int CDThreadBegin( pthread_t* _pThreadID, CD_THREAD_WORKER _pStartAddress, void* _vpParameter )
{
    pthread_attr_t  stAttr;

    // 1. ���� ó��
    if( _pStartAddress == NULL )
        return  CD_THREAD_ERROR;

    // 2. Thread �Ӽ���ü �ʱ�ȭ
    if( CDThreadAttrInit( &stAttr ) == CD_THREAD_ERROR )
        return  CD_THREAD_ERROR;

    // 3. Thread�� ���� ������ ����(PTHREAD_SCOPE_SYSTEM)
    if( CDThreadSetScope( &stAttr, PTHREAD_SCOPE_SYSTEM ) == CD_THREAD_ERROR )
        return  CD_THREAD_ERROR;

    // 4. Thread�� Detach �Ӽ��� ����(PTHREAD_CREATE_DETACHED)
    if( CDThreadSetDetach( &stAttr, PTHREAD_CREATE_DETACHED ) == CD_THREAD_ERROR )
        return  CD_THREAD_ERROR;

    // 5. Thread ����
    if( pthread_create( _pThreadID, &stAttr, _pStartAddress, _vpParameter ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 6. ����� Thread �Ӽ���ü ����
    if( CDThreadAttrDestroy( &stAttr ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 7. ����!!
    return  CD_THREAD_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : pthread_t                                                        *
*             _iThreadID : �����ų Thread�� Thread ID                         *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : Thread ID(_iThreadID)�� Argument�� �Ѱ� �޾� �ش� Thread ��      *
*             �����Ų��.                                                      *
*******************************************************************************/
int CDThreadTerminate( pthread_t _iThreadID )
{
    // 1. Thread ID�� �ش��ϴ� Thread ����
    if( pthread_cancel( _iThreadID ) != CD_THREAD_SUCCESS )
        return  CD_THREAD_ERROR;

    // 2. ����!!
    return  CD_THREAD_SUCCESS;
}
