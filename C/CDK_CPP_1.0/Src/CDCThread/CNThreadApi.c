#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <strings.h>

#include "CNThreadApi.h"
#include "CNThreadDefinitions.h"


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
int CNThreadAttrInit( pthread_attr_t* _stpAttr )
{
	// 1. ���ؽ� �Ӽ� ��ü �ʱ�ȭ
	if( pthread_attr_init( _stpAttr ) != CN_THREAD_SUCCESS )
		return	CN_THREAD_ERROR;
	
	// 2. ����!!
	return CN_THREAD_SUCCESS;
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
int CNThreadAttrDestroy( pthread_attr_t* _stpAttr )
{
	// 1. ���ؽ� �Ӽ���ü ����
	if( pthread_attr_destroy( _stpAttr ) != CN_THREAD_SUCCESS )
		return	CN_THREAD_ERROR;
	
	// 2. ����!!
	return CN_THREAD_SUCCESS;		
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
int CNThreadGetScope( pthread_attr_t* _stpAttr )
{
	int	iScope;

	// 1. �Ӽ� ��ü���� Thread ���� ���� �Ӽ� ����
	if( pthread_attr_getscope( _stpAttr, &iScope ) != CN_THREAD_SUCCESS )
		return	CN_THREAD_ERROR;

	// 2. ����!!
	return	iScope;
}


/*******************************************************************************
* Update    : 2011/03/17                                                       *
* Argument  : pthread_attr_t*                                                  *
*             _stpAttr : �Ӽ��� ���� �Ϸ��� Thread �Ӽ� ��ü�� ������          *
* Return    : int, ����(0), ����(-1)                                           *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument�� �Ѱܹ��� Thread �Ӽ� ��ü�� ������(_stpAttr)��        *
*             ����Ű�� ��ü�� ���࿵�� �Ӽ��� �ý��ۿ���(PTHREAD_SCOPE_SYSTEM) *
*             ���� �����Ѵ�.                                                   *
*******************************************************************************/
int CNThreadSetScope( pthread_attr_t* _stpAttr )
{
	// 1. Thread �Ӽ� ��ü�� ���� ���� �Ӽ��� ����
	if( pthread_attr_setscope( _stpAttr, PTHREAD_SCOPE_SYSTEM ) != CN_THREAD_SUCCESS )
		return CN_THREAD_ERROR;

	// 2. ����!!
	return	CN_THREAD_SUCCESS;
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
int CNThreadGetDetach( pthread_attr_t* _stpAttr )
{
	int iDetach;

	// 1. Thread �Ӽ� ��ü�� Detach �Ӽ� ����
	if( pthread_attr_getdetachstate( _stpAttr, &iDetach ) != CN_THREAD_SUCCESS )
		return	CN_THREAD_ERROR;

	// 2. ����!!
	return	iDetach;
}


/*******************************************************************************
* Update    : 2011/03/17                                                       *
* Argument  : pthread_attr_t*                                                  *
*             _stpAttr : �����Ϸ��� Thread �Ӽ� ��ü�� ������                  *
* Return    : int, ����(0), ����(-1)                                           *
*                                                                              *
* Stability : MT-Safe                                                          *
* Explain   : Argument�� �Ѱܹ��� Thread �Ӽ� ��ü�� ������(_stpAttr)��        *
*             ����Ű�� ��ü�� Thread �Ӽ��� Deatch(PTHREAD_CREATE_DETACHED)��  *
*             �����Ѵ�.                                                        *
*******************************************************************************/
int CNThreadSetDetach( pthread_attr_t* _stpAttr )
{
	// 1. Thread �Ӽ� ��ü�� Detach �Ӽ� ����
	if( pthread_attr_setdetachstate( _stpAttr, PTHREAD_CREATE_DETACHED ) != CN_THREAD_SUCCESS )
		return	CN_THREAD_ERROR;

	// 2. ����!!
	return	CN_THREAD_SUCCESS;
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
*             ���� �� �������� �Ӽ��� CNThreadSetScope(Ŀ�� ���� ����),      *
*             CNThreadSetDetach(Thread �и�)�̴�.                              *
*             ���� ����� �Ӽ��� ������� �ű� Thread�� �����ȴ�.              *
*******************************************************************************/
int CNThreadBegin( pthread_t* _pThreadID, CN_THREAD_WORKER _pStartAddress, void* _vpParameter )
{
	pthread_attr_t	stAttr;

	// 1. ���� ó��
	if( _pStartAddress == NULL )
		return	CN_THREAD_ERROR;

	// 2. Thread �Ӽ���ü �ʱ�ȭ
	if( CNThreadAttrInit( &stAttr ) == CN_THREAD_ERROR )
		return	CN_THREAD_ERROR;

	// 3. Thread�� ���� ������ ����(PTHREAD_SCOPE_SYSTEM)
	if( CNThreadSetScope( &stAttr ) == CN_THREAD_ERROR )
		return	CN_THREAD_ERROR;

	// 4. Thread�� Detach �Ӽ��� ����(PTHREAD_CREATE_DETACHED)
	if( CNThreadSetDetach( &stAttr ) == CN_THREAD_ERROR )
		return	CN_THREAD_ERROR;

	// 5. Thread ����
	if( pthread_create( _pThreadID, &stAttr, _pStartAddress, _vpParameter ) != CN_THREAD_SUCCESS )
		return	CN_THREAD_ERROR;

	// 6. ����� Thread �Ӽ���ü ����
	if( CNThreadAttrDestroy( &stAttr ) != CN_THREAD_SUCCESS )
		return	CN_THREAD_ERROR;

	// 7. ����!!
	return	CN_THREAD_SUCCESS;
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
int CNThreadTerminate( pthread_t _iThreadID )
{
	// 1. Thread ID�� �ش��ϴ� Thread ����
	if( pthread_cancel( _iThreadID ) != CN_THREAD_SUCCESS )
		return	CN_THREAD_ERROR;

	// 2. ����!!
	return	CN_THREAD_SUCCESS;
}
