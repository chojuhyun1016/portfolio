#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/atomic.h>

#include "Main.h"
#include "CDThread.h"

volatile unsigned long		g_iDone		= 1;
volatile unsigned long		g_iArgument	= 100;

int main( int argc, char **argv )
{
	int		iResult;

	// ������ �ڵ� ��ü
	pthread_t			stPthread;

	// ������ �Ӽ� ��ü
	pthread_attr_t		*stpThreadAtrr;

	// ������ ���̺귯���� ������ ���� ����ü
	// Ps) ������ ������� ���ڰ�, �����Լ� ���� ����
	stCDThreadCreate	*stpThreadInfo;

	// ������ �Ӽ� ��ü �������� ����
	if( ( stpThreadAtrr = (pthread_attr_t*)::malloc( sizeof( pthread_attr_t ) ) ) == NULL )
	{
		::fprintf( stderr, "[malloc( %d ) Error][E:%d][L:%d]\n", 
			sizeof( pthread_attr_t ), 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::memset( stpThreadAtrr, 0x00, sizeof( pthread_attr_t ) );

	// ������ �Ӽ� ��ü �������� �ʱ�ȭ
	if( ( iResult = CDThreadAttrInit( stpThreadAtrr ) ) == CD_THREAD_ERROR )
	{
		::fprintf( stderr, "[CDThreadAttrInit( %d ) Error][R:%d][E:%d][L:%d]\n", 
			stpThreadAtrr, 
			iResult, 
			errno, 
			__LINE__ );

		::free( stpThreadAtrr );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDThreadAttrInit( %d ) Complete][L:%d]\n", stpThreadAtrr, __LINE__ );

	// ������ �Ӽ� ��ü�� ������ ���࿵�� �Ӽ��� ����
	if( ( iResult = CDThreadSetScope( stpThreadAtrr, PTHREAD_SCOPE_SYSTEM ) ) == CD_THREAD_ERROR )
	{
		::fprintf( stderr, "[CDThreadSetScope( %d ) Error][R:%d][E:%d][L:%d]\n", 
			stpThreadAtrr, 
			iResult, 
			errno, 
			__LINE__ );

		::free( stpThreadAtrr );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDThreadSetScope( %d ) Complete][L:%d]\n", stpThreadAtrr, __LINE__ );

	// ������ �Ӽ� ��ü�� ������ ���࿵�� �Ӽ��� ȹ��(��ȯ)
	if( ( iResult = CDThreadGetScope( stpThreadAtrr ) ) == CD_THREAD_ERROR )
	{
		::fprintf( stderr, "[CDThreadGetScope( %d ) Error][R:%d][E:%d][L:%d]\n", 
			stpThreadAtrr, 
			iResult, 
			errno, 
			__LINE__ );

		::free( stpThreadAtrr );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDThreadGetScope( %d ) Complete][Scope:%s][L:%d]\n", 
		stpThreadAtrr, 
		iResult == PTHREAD_SCOPE_SYSTEM ? "PTHREAD_SCOPE_SYSTEM" : "PTHREAD_SCOPE_PROCESS", 
		__LINE__ );

	// ������ �Ӽ� ��ü�� ������ �и� �Ӽ��� ����
	if( ( iResult = CDThreadSetDetach( stpThreadAtrr, PTHREAD_CREATE_DETACHED ) ) == CD_THREAD_ERROR )
	{
		::fprintf( stderr, "[CDThreadSetDetach( %d ) Error][R:%d][E:%d][L:%d]\n", 
			stpThreadAtrr, 
			iResult, 
			errno, 
			__LINE__ );

		::free( stpThreadAtrr );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDThreadSetDetach( %d ) Complete][L:%d]\n", stpThreadAtrr, __LINE__ );

	// ������ �Ӽ� ��ü�� ������ �и� �Ӽ��� ȹ��(��ȯ)
	if( ( iResult = CDThreadGetDetach( stpThreadAtrr ) ) == CD_THREAD_ERROR )
	{
		::fprintf( stderr, "[CDThreadSetGetach( %d ) Error][R:%d][E:%d][L:%d]\n", 
			stpThreadAtrr, 
			iResult, 
			errno, 
			__LINE__ );

		::free( stpThreadAtrr );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDThreadGetDetach( %d ) Complete][Detach:%s][L:%d]\n", 
		stpThreadAtrr, 
		iResult == PTHREAD_CREATE_DETACHED ? "PTHREAD_CREATE_DETACHED" : "PTHREAD_CREATE_JOINABLE", 
		__LINE__ );

	// ������ ����
	// stPthread : Thread ID �� ���� ��
	// ThreadBegin : ������ ������ �����尡 ���� �� �Լ�
	// g_iArgument : �����忡�� ���� �� ������ �ּ�
	if( ( iResult = CDThreadBegin( &stPthread, ThreadBegin, (void*)&g_iArgument ) ) == CD_THREAD_ERROR )
	{
		::fprintf( stderr, "[CDThreadBegin( %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
			stPthread, 
			ThreadBegin, 
			&g_iArgument, 
			iResult, 
			errno, 
			__LINE__ );

		::free( stpThreadAtrr );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDThreadBegin( %d %d %d ) Complete][L:%d]\n", 
		&stPthread, 
		ThreadBegin, 
		(void*)&g_iArgument, 
		__LINE__ );

	::sleep( 5 );

	// ������ ����
	if( ( iResult = CDThreadTerminate( stPthread ) ) == CD_THREAD_ERROR )
	{
		::fprintf( stderr, "[CDThreadTerminate( %d ) Error][R:%d][E:%d][L:%d]\n", 
			stPthread, 
			iResult, 
			errno, 
			__LINE__ );

		::free( stpThreadAtrr );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDThreadTerminate( %d ) Complete][L:%d]\n", stPthread, __LINE__ );

	// ������ �Ӽ���ü ����
	if( ( iResult = CDThreadAttrDestroy( stpThreadAtrr ) ) == CD_THREAD_ERROR )
	{
		::fprintf( stderr, "[CDThreadAttrDestroy( %d ) Error][R:%d][E:%d][L:%d]\n", 
			stpThreadAtrr, 
			iResult, 
			errno, 
			__LINE__ );

		::free( stpThreadAtrr );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDThreadAttrDestroy( %d ) Complete][L:%d]\n", stpThreadAtrr, __LINE__ );

	// ������ �Ӽ� ��ü ������ �޸𸮿��� ����
	if( ( stpThreadInfo = (stCDThreadCreate*)::malloc( sizeof( stCDThreadCreate ) ) ) == NULL )
	{
		::fprintf( stderr, "[malloc( %d ) Error][E:%d][L:%d]\n", 
			sizeof( stCDThreadCreate ), 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	// ������ ����ü �ʱ�ȭ
	// ������ ����ü�� ������ ���� �������� ������ �޶���
	::memset( stpThreadInfo, 0x00, sizeof( stCDThreadCreate ) );

	// �Ѱ��� ����
	stpThreadInfo->vpArgs				= (void*)&g_iArgument;

	// ������ ���� �� ������ ����, ����, ���� �Լ�
	// Ps) OnThreadStart -> OnThreadRun -> OnThreadTerminate ������ ����
	stpThreadInfo->OnThreadStart		= ThreadStart;
	stpThreadInfo->OnThreadRun			= ThreadRun;
	stpThreadInfo->OnThreadTerminate	= ThreadTerminate;

	// ������ ����
	// Ps) ���̺귯���� ������ ����ü�� �����Ϳ� ���� ����
	if( ( iResult = CDThreadCreate( stpThreadInfo ) ) < 0 )
	{
		::fprintf( stderr, "[CDThreadCreate( %d ) Error][R:%d][L:%d]\n", 
			stpThreadInfo, 
			iResult, 
			__LINE__ );

		::free( stpThreadInfo );

		::exit( -1 );
	}

	::fprintf( stderr, "[CDThreadCreate( %d ) Complete][L:%d]\n", stpThreadInfo, __LINE__ );

	::free( stpThreadInfo );

	// ������ ���� ����
	// g_iDone �� 0 �� �Ǹ� ��������� ���� ������ ����
	// ��� �����尡 ���� �� ������ ���
	while( g_iDone != 0 )
	{
		::sleep( 1);
	}

	::fprintf( stderr, "[All Thread Terminate][L:%d]\n", __LINE__ );

	return 0;
}

// �Ϲ����� ������ ������ �����ϴ� �Լ�
void* ThreadBegin( void* _vpArg )
{
	int		iArgument	= *((int*)_vpArg);

	::fprintf( stderr, "[Thread Begin][Argument:%d][L:%d]\n", 
		iArgument, 
		__LINE__ );

	while( 1 )
	{
		::fprintf( stderr, "[Thread Begin][Sleeping][L:%d]\n", __LINE__ );
		::sleep( 1 );
	}

	return	0;
}

// ������ ����ü�� ���� �����尡 ���� �� ��� �ҷ����� �Լ�
// �������� ����(������) �Լ�
int ThreadStart( void* _vpArg )
{
	stCDThreadCreate*	stpThreadInfo	= (stCDThreadCreate*)_vpArg;

	::fprintf( stderr, "[Thread Start][Argument:%d][L:%d]\n", 
		*((int*)stpThreadInfo->vpArgs), 
		__LINE__ );

	return	0;
}

// ������ ����ü�� ���� �����尡 ���� �� ��� �ҷ����� �Լ�
// �������� ���� �Լ�
int ThreadRun( void* _vpArg )
{
	stCDThreadCreate*	stpThreadInfo	= (stCDThreadCreate*)_vpArg;

	::fprintf( stderr, "[Thread Run][Argument:%d][L:%d]\n", 
		*((int*)stpThreadInfo->vpArgs), 
		__LINE__ );

	return	0;
}

// ������ ����ü�� ���� �����尡 ���� �� ��� �ҷ����� �Լ�
// �������� ����(�Ҹ���) �Լ�
int ThreadTerminate( void* _vpArg )
{
	stCDThreadCreate*	stpThreadInfo	= (stCDThreadCreate*)_vpArg;

	::fprintf( stderr, "[Thread Terminate][Argument:%d][L:%d]\n", 
		*((int*)stpThreadInfo->vpArgs), 
		__LINE__ );

	g_iDone	= 0;

	return	0;
}