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

	// 쓰레드 핸들 객체
	pthread_t			stPthread;

	// 쓰레드 속성 객체
	pthread_attr_t		*stpThreadAtrr;

	// 쓰레드 라이브러리의 쓰레드 동작 구조체
	// Ps) 쓰레드 실행시의 인자값, 실행함수 들이 포함
	stCDThreadCreate	*stpThreadInfo;

	// 쓰레드 속성 객체 힙공간에 생성
	if( ( stpThreadAtrr = (pthread_attr_t*)::malloc( sizeof( pthread_attr_t ) ) ) == NULL )
	{
		::fprintf( stderr, "[malloc( %d ) Error][E:%d][L:%d]\n", 
			sizeof( pthread_attr_t ), 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::memset( stpThreadAtrr, 0x00, sizeof( pthread_attr_t ) );

	// 쓰레드 속성 객체 힙공간에 초기화
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

	// 쓰레드 속성 객체의 쓰레드 실행영역 속성을 설정
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

	// 쓰레드 속성 객체의 쓰레드 실행영역 속성을 획득(반환)
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

	// 쓰레드 속성 객체의 쓰레드 분리 속성을 설정
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

	// 쓰레드 속성 객체의 쓰레드 분리 속성을 획득(반환)
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

	// 쓰레드 생성
	// stPthread : Thread ID 가 저장 됨
	// ThreadBegin : 쓰레드 생성후 쓰레드가 실행 할 함수
	// g_iArgument : 쓰레드에게 전달 할 데이터 주소
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

	// 쓰레드 종료
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

	// 쓰레드 속성객체 해제
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

	// 쓰레드 속성 객체 공간을 메모리에서 삭제
	if( ( stpThreadInfo = (stCDThreadCreate*)::malloc( sizeof( stCDThreadCreate ) ) ) == NULL )
	{
		::fprintf( stderr, "[malloc( %d ) Error][E:%d][L:%d]\n", 
			sizeof( stCDThreadCreate ), 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	// 쓰레드 구조체 초기화
	// 쓰레드 구조체의 설정에 따라 쓰레드의 동작이 달라짐
	::memset( stpThreadInfo, 0x00, sizeof( stCDThreadCreate ) );

	// 넘겨줄 인자
	stpThreadInfo->vpArgs				= (void*)&g_iArgument;

	// 쓰레드 실행 후 실행할 시작, 실행, 종료 함수
	// Ps) OnThreadStart -> OnThreadRun -> OnThreadTerminate 순으로 실행
	stpThreadInfo->OnThreadStart		= ThreadStart;
	stpThreadInfo->OnThreadRun			= ThreadRun;
	stpThreadInfo->OnThreadTerminate	= ThreadTerminate;

	// 쓰레드 생성
	// Ps) 라이브러리의 쓰레드 구조체의 데이터에 따라 동작
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

	// 쓰레드 종료 실행
	// g_iDone 이 0 이 되면 쓰레드들이 종료 과정을 실행
	// 모든 쓰레드가 종료 될 때까지 대기
	while( g_iDone != 0 )
	{
		::sleep( 1);
	}

	::fprintf( stderr, "[All Thread Terminate][L:%d]\n", __LINE__ );

	return 0;
}

// 일반적인 쓰레드 생성시 실행하는 함수
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

// 쓰레드 구조체에 의해 쓰레드가 실행 될 경우 불려지는 함수
// 쓰레드의 시작(생성자) 함수
int ThreadStart( void* _vpArg )
{
	stCDThreadCreate*	stpThreadInfo	= (stCDThreadCreate*)_vpArg;

	::fprintf( stderr, "[Thread Start][Argument:%d][L:%d]\n", 
		*((int*)stpThreadInfo->vpArgs), 
		__LINE__ );

	return	0;
}

// 쓰레드 구조체에 의해 쓰레드가 실행 될 경우 불려지는 함수
// 쓰레드의 실행 함수
int ThreadRun( void* _vpArg )
{
	stCDThreadCreate*	stpThreadInfo	= (stCDThreadCreate*)_vpArg;

	::fprintf( stderr, "[Thread Run][Argument:%d][L:%d]\n", 
		*((int*)stpThreadInfo->vpArgs), 
		__LINE__ );

	return	0;
}

// 쓰레드 구조체에 의해 쓰레드가 실행 될 경우 불려지는 함수
// 쓰레드의 종료(소멸자) 함수
int ThreadTerminate( void* _vpArg )
{
	stCDThreadCreate*	stpThreadInfo	= (stCDThreadCreate*)_vpArg;

	::fprintf( stderr, "[Thread Terminate][Argument:%d][L:%d]\n", 
		*((int*)stpThreadInfo->vpArgs), 
		__LINE__ );

	g_iDone	= 0;

	return	0;
}
