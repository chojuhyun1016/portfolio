#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/atomic.h>

#include "Main.h"
#include "CDStructure.h"

int main( int argc, char **argv )
{
	int				iResult;

	int				iForLoop;

	int				iLinkedListPushKey;		// Linked List 데이터 입출력시 키 값
											// Ps) List는 검색 기능도 포함하므로 키 값이 필요
	int				iLinkedListPushType;	// Linked List 데이터 입력되는 데이터의 종류 값
	int				iLinkedListPushData;	// Linked List 에 입력되는 데이터

	int				iQueuePushType;			// Queue 데이터 입력되는 데이터의 종류 값
	int				iQueuePushData;			// Queue 에 입력되는 데이터

	int				iStackPushType;			// Stack 데이터 입력되는 데이터의 종류 값
	int				iStackPushData;			// Stack 에 입력되는 데이터

	int				iLinkedListPopKey;		// Linked List 데이터 추출 시 데이터 키값을 저장 할 변수
	int				iLinkedListPopType;		// Linked List 데이터 추출 시 데이터 종류를 저장 할 변수
	int				*ipLinkedListPopData;	// Linked List 에서 추출한 데이터를 담을 변수(포인터)

	int				iQueuePopType;			// Queue 데이터 추출 시 데이터 종류를 저장 할 변수
	int				*ipQueuePopData;		// Queue 에서 추출한 데이터를 담을 변수(포인터)

	int				iStackPopType;			// Stack 데이터 추출 시 데이터 종류를 저장 할 변수
	int				*ipStackPopData;		// Stack 에서 추출한 데이터를 담을 변수(포인터)

	stCDStructureLinkedList		*stpLinkedList;	// Linked List 객체의 포인터
	stCDStructureQueue			*stpQueue;		// Queue 객체의 포인터
	stCDStructureStack			*stpStack;		// Stack 객체의 포인터

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////// Linked List ////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Linked List 객체 생성
	stpLinkedList	= CDStructureLinkedListMake();

	// Linked List 객체 초기화
	// Ps) CD_STRUCTURE_LINKED_LIST_SIZE(10000) 개의 데이터를 저장 할 공간을 미리 확보
	if( ( iResult = CDStructureLinkedListInit( stpLinkedList, CD_STRUCTURE_LINKED_LIST_SIZE ) ) != CD_STRUCTURE_SUCCESS )
	{
		::fprintf( stderr, "[LINKED_LIST][CDStructureLinkedListInit( %d %d ) Error][R:%d][L:%d]\n", 
			stpLinkedList, 
			CD_STRUCTURE_LINKED_LIST_SIZE, 
			iResult, 
			__LINE__ );

		CDStructureLinkedListFree( &stpLinkedList );

		::exit( -1 );
	}

	::fprintf( stderr, "[LINKED_LIST][USE_COUNT:%d][L:%d]\n", CDStructureLinkedListGetUseCount( stpLinkedList ), __LINE__ );
	::fprintf( stderr, "[LINKED_LIST][IDLE_COUNT:%d][L:%d]\n", CDStructureLinkedListGetIdleCount( stpLinkedList ), __LINE__ );

	// Linked List 안에 저장 데이터 저장 공간을 추가 확보
	// ps) CD_STRUCTURE_LINKED_LIST_SIZE(10000) 만개 만큼 추가 확보
	if( ( iResult = CDStructureLinkedListAppend( stpLinkedList, CD_STRUCTURE_LINKED_LIST_SIZE ) ) == CD_STRUCTURE_ERROR )
	{
		::fprintf( stderr, "[LINKED_LIST][CDStructureLinkedListAppend( %d %d ) Error][R:%d][L:%d]\n", 
			stpLinkedList, 
			CD_STRUCTURE_LINKED_LIST_SIZE, 
			iResult, 
			__LINE__ );

		CDStructureLinkedListFree( &stpLinkedList );

		::exit( -1 );
	}

	::fprintf( stderr, "[LINKED_LIST][USE_COUNT:%d][L:%d]\n", CDStructureLinkedListGetUseCount( stpLinkedList ), __LINE__ );
	::fprintf( stderr, "[LINKED_LIST][IDLE_COUNT:%d][L:%d]\n", CDStructureLinkedListGetIdleCount( stpLinkedList ), __LINE__ );

	// Linked List 안에 저장 데이터 저장 공간을 삭제
	// ps) CD_STRUCTURE_LINKED_LIST_SIZE(10000) 만개 만큼 확보
	if( ( iResult = CDStructureLinkedListDelete( stpLinkedList, CD_STRUCTURE_LINKED_LIST_SIZE ) ) == CD_STRUCTURE_ERROR )
	{
		::fprintf( stderr, "[LINKED_LIST][CDStructureLinkedListDelete( %d %d ) Error][R:%d][L:%d]\n", 
			stpLinkedList, 
			CD_STRUCTURE_LINKED_LIST_SIZE, 
			iResult, 
			__LINE__ );

		CDStructureLinkedListFree( &stpLinkedList );

		::exit( -1 );
	}

	::fprintf( stderr, "[LINKED_LIST][USE_COUNT:%d][L:%d]\n", CDStructureLinkedListGetUseCount( stpLinkedList ), __LINE__ );
	::fprintf( stderr, "[LINKED_LIST][IDLE_COUNT:%d][L:%d]\n", CDStructureLinkedListGetIdleCount( stpLinkedList ), __LINE__ );

	iLinkedListPushKey		= 1;
	iLinkedListPushType		= 2;
	iLinkedListPushData		= 100;

	// Linked List에 데이터를 삽입
	if( ( iResult = CDStructureLinkedListPush( stpLinkedList, iLinkedListPushKey, iLinkedListPushType, &iLinkedListPushData ) ) == CD_STRUCTURE_ERROR )
	{
		::fprintf( stderr, "[LINKED_LIST][CDStructureLinkedListPush( %d %d %d %d ) Error][R:%d][L:%d]\n", 
			stpLinkedList, 
			iLinkedListPushKey, 
			iLinkedListPushType, 
			&iLinkedListPushData, 
			iResult, 
			__LINE__ );

		CDStructureLinkedListFree( &stpLinkedList );

		::exit( -1 );
	}

	::fprintf( stderr, "[LINKED_LIST][USE_COUNT:%d][L:%d]\n", CDStructureLinkedListGetUseCount( stpLinkedList ), __LINE__ );
	::fprintf( stderr, "[LINKED_LIST][IDLE_COUNT:%d][L:%d]\n", CDStructureLinkedListGetIdleCount( stpLinkedList ), __LINE__ );

	iLinkedListPopKey	= iLinkedListPushKey;

	// Linked List에서 데이터를 뽑아낸다
	if( ( iResult = CDStructureLinkedListPop( stpLinkedList, iLinkedListPopKey, &iLinkedListPopType, (void**)&ipLinkedListPopData ) ) == CD_STRUCTURE_ERROR )
	{
		::fprintf( stderr, "[LINKED_LIST][CDStructureLinkedListPush( %d %d %d %d ) Error][R:%d][L:%d]\n", 
			stpLinkedList, 
			iLinkedListPopKey, 
			&iLinkedListPopType, 
			&ipLinkedListPopData, 
			iResult, 
			__LINE__ );

		CDStructureLinkedListFree( &stpLinkedList );

		::exit( -1 );
	}

	::fprintf( stderr, "[LINKED_LIST][Key:%d][Type:%d][Data:%d][L:%d]\n", 
		iLinkedListPopKey, 
		iLinkedListPopType, 
		*ipLinkedListPopData, 
		iResult, 
		__LINE__ );

	::fprintf( stderr, "[LINKED_LIST][USE_COUNT:%d][L:%d]\n", CDStructureLinkedListGetUseCount( stpLinkedList ), __LINE__ );
	::fprintf( stderr, "[LINKED_LIST][IDLE_COUNT:%d][L:%d]\n", CDStructureLinkedListGetIdleCount( stpLinkedList ), __LINE__ );

	// Linked List 객체 정보 초기화
	// Ps) 객체 안의 데이터를 모두 삭제하고 변수들을 초기화
	CDStructureLinkedListFree( &stpLinkedList );

	// Linked List 객체 공간 해제
	::free( stpLinkedList );

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////// Queue ///////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Queue 객체 생성
	stpQueue	= CDStructureQueueMake();

	// Queue 객체 초기화
	// Ps) CD_STRUCTURE_QUEUE_SIZE(10000) 개의 데이터를 저장 할 공간을 미리 확보
	if( ( iResult = CDStructureQueueInit( stpQueue, CD_STRUCTURE_QUEUE_SIZE ) ) != CD_STRUCTURE_SUCCESS )
	{
		::fprintf( stderr, "[QUEUE][CDStructureQueueInit( %d %d ) Error][R:%d][L:%d]\n", 
			stpQueue, 
			CD_STRUCTURE_QUEUE_SIZE, 
			iResult, 
			__LINE__ );

		CDStructureQueueFree( &stpQueue );

		::exit( -1 );
	}

	::fprintf( stderr, "[QUEUE][USE_COUNT:%d][L:%d]\n", CDStructureQueueGetUseCount( stpQueue ), __LINE__ );
	::fprintf( stderr, "[QUEUE][IDLE_COUNT:%d][L:%d]\n", CDStructureQueueGetIdleCount( stpQueue ), __LINE__ );

	// Queue 안에 저장 데이터 저장 공간을 추가 확보
	// ps) CD_STRUCTURE_QUEUE_SIZE(10000) 만개 만큼 추가 확보
	if( ( iResult = CDStructureQueueAppend( stpQueue, CD_STRUCTURE_QUEUE_SIZE ) ) == CD_STRUCTURE_ERROR )
	{
		::fprintf( stderr, "[QUEUE][CDStructureQueueAppend( %d %d ) Error][R:%d][L:%d]\n", 
			stpQueue, 
			CD_STRUCTURE_QUEUE_SIZE, 
			iResult, 
			__LINE__ );

		CDStructureQueueFree( &stpQueue );

		::exit( -1 );
	}

	::fprintf( stderr, "[QUEUE][USE_COUNT:%d][L:%d]\n", CDStructureQueueGetUseCount( stpQueue ), __LINE__ );
	::fprintf( stderr, "[QUEUE][IDLE_COUNT:%d][L:%d]\n", CDStructureQueueGetIdleCount( stpQueue ), __LINE__ );

	// Queue 안에 저장 데이터 저장 공간을 삭제
	// ps) CD_STRUCTURE_QUEUE_SIZE(10000) 만개 만큼 확보
	if( ( iResult = CDStructureQueueDelete( stpQueue, CD_STRUCTURE_QUEUE_SIZE ) ) == CD_STRUCTURE_ERROR )
	{
		::fprintf( stderr, "[QUEUE][CDStructureQueueDelete( %d %d ) Error][R:%d][L:%d]\n", 
			stpQueue, 
			CD_STRUCTURE_QUEUE_SIZE, 
			iResult, 
			__LINE__ );

		CDStructureQueueFree( &stpQueue );

		::exit( -1 );
	}

	::fprintf( stderr, "[QUEUE][USE_COUNT:%d][L:%d]\n", CDStructureQueueGetUseCount( stpQueue ), __LINE__ );
	::fprintf( stderr, "[QUEUE][IDLE_COUNT:%d][L:%d]\n", CDStructureQueueGetIdleCount( stpQueue ), __LINE__ );

	iQueuePushType		= 2;
	iQueuePushData		= 200;

	// Queue에 데이터를 삽입
	if( ( iResult = CDStructureQueuePush( stpQueue, iQueuePushType, &iQueuePushData ) ) == CD_STRUCTURE_ERROR )
	{
		::fprintf( stderr, "[QUEUE][CDStructureQueuePush( %d %d %d ) Error][R:%d][L:%d]\n", 
			stpQueue, 
			iQueuePushType, 
			&iQueuePushData, 
			iResult, 
			__LINE__ );

		CDStructureQueueFree( &stpQueue );

		::exit( -1 );
	}

	::fprintf( stderr, "[QUEUE][USE_COUNT:%d][L:%d]\n", CDStructureQueueGetUseCount( stpQueue ), __LINE__ );
	::fprintf( stderr, "[QUEUE][IDLE_COUNT:%d][L:%d]\n", CDStructureQueueGetIdleCount( stpQueue ), __LINE__ );

	// Queue에서 데이터를 뽑아낸다
	if( ( iResult = CDStructureQueuePop( stpQueue, &iQueuePopType, (void**)&ipQueuePopData ) ) == CD_STRUCTURE_ERROR )
	{
		::fprintf( stderr, "[QUEUE][CDStructureQueuePush( %d %d %d ) Error][R:%d][L:%d]\n", 
			stpQueue, 
			&iQueuePopType, 
			&ipQueuePopData, 
			iResult, 
			__LINE__ );

		CDStructureQueueFree( &stpQueue );

		::exit( -1 );
	}

	::fprintf( stderr, "[QUEUE][Type:%d][Data:%d][L:%d]\n", 
		iQueuePopType, 
		*ipQueuePopData, 
		iResult, 
		__LINE__ );

	::fprintf( stderr, "[QUEUE][USE_COUNT:%d][L:%d]\n", CDStructureQueueGetUseCount( stpQueue ), __LINE__ );
	::fprintf( stderr, "[QUEUE][IDLE_COUNT:%d][L:%d]\n", CDStructureQueueGetIdleCount( stpQueue ), __LINE__ );

	// Queue 객체 정보 초기화
	// Ps) 객체 안의 데이터를 모두 삭제하고 변수들을 초기화
	CDStructureQueueFree( &stpQueue );

	// Queue 객체 공간 해제
	::free( stpQueue );

	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////// Stack //////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Stack 객체 생성
	stpStack	= CDStructureStackMake();

	// Stack 객체 초기화
	// Ps) CD_STRUCTURE_STACK_SIZE(10000) 개의 데이터를 저장 할 공간을 미리 확보
	if( ( iResult = CDStructureStackInit( stpStack, CD_STRUCTURE_STACK_SIZE ) ) != CD_STRUCTURE_SUCCESS )
	{
		::fprintf( stderr, "[STACK][CDStructureStackInit( %d %d ) Error][R:%d][L:%d]\n", 
			stpStack, 
			CD_STRUCTURE_STACK_SIZE, 
			iResult, 
			__LINE__ );

		CDStructureStackFree( &stpStack );

		::exit( -1 );
	}

	::fprintf( stderr, "[STACK][USE_COUNT:%d][L:%d]\n", CDStructureStackGetUseCount( stpStack ), __LINE__ );
	::fprintf( stderr, "[STACK][IDLE_COUNT:%d][L:%d]\n", CDStructureStackGetIdleCount( stpStack ), __LINE__ );

	// Stack 안에 저장 데이터 저장 공간을 추가 확보
	// ps) CD_STRUCTURE_STACK_SIZE(10000) 만개 만큼 추가 확보
	if( ( iResult = CDStructureStackAppend( stpStack, CD_STRUCTURE_STACK_SIZE ) ) == CD_STRUCTURE_ERROR )
	{
		::fprintf( stderr, "[STACK][CDStructureStackAppend( %d %d ) Error][R:%d][L:%d]\n", 
			stpStack, 
			CD_STRUCTURE_STACK_SIZE, 
			iResult, 
			__LINE__ );

		CDStructureStackFree( &stpStack );

		::exit( -1 );
	}

	::fprintf( stderr, "[STACK][USE_COUNT:%d][L:%d]\n", CDStructureStackGetUseCount( stpStack ), __LINE__ );
	::fprintf( stderr, "[STACK][IDLE_COUNT:%d][L:%d]\n", CDStructureStackGetIdleCount( stpStack ), __LINE__ );

	// Stack 안에 저장 데이터 저장 공간을 삭제
	// ps) CD_STRUCTURE_STACK_SIZE(10000) 만개 만큼 확보
	if( ( iResult = CDStructureStackDelete( stpStack, CD_STRUCTURE_STACK_SIZE ) ) == CD_STRUCTURE_ERROR )
	{
		::fprintf( stderr, "[STACK][CDStructureStackDelete( %d %d ) Error][R:%d][L:%d]\n", 
			stpStack, 
			CD_STRUCTURE_STACK_SIZE, 
			iResult, 
			__LINE__ );

		CDStructureStackFree( &stpStack );

		::exit( -1 );
	}

	::fprintf( stderr, "[STACK][USE_COUNT:%d][L:%d]\n", CDStructureStackGetUseCount( stpStack ), __LINE__ );
	::fprintf( stderr, "[STACK][IDLE_COUNT:%d][L:%d]\n", CDStructureStackGetIdleCount( stpStack ), __LINE__ );

	iStackPushType		= 3;
	iStackPushData		= 300;

	// Stack에 데이터를 삽입
	if( ( iResult = CDStructureStackPush( stpStack, iStackPushType, &iStackPushData ) ) == CD_STRUCTURE_ERROR )
	{
		::fprintf( stderr, "[STACK][CDStructureStackPush( %d %d %d ) Error][R:%d][L:%d]\n", 
			stpStack, 
			iStackPushType, 
			&iStackPushData, 
			iResult, 
			__LINE__ );

		CDStructureStackFree( &stpStack );

		::exit( -1 );
	}

	::fprintf( stderr, "[STACK][USE_COUNT:%d][L:%d]\n", CDStructureStackGetUseCount( stpStack ), __LINE__ );
	::fprintf( stderr, "[STACK][IDLE_COUNT:%d][L:%d]\n", CDStructureStackGetIdleCount( stpStack ), __LINE__ );

	// Stack에서 데이터를 뽑아낸다
	if( ( iResult = CDStructureStackPop( stpStack, &iStackPopType, (void**)&ipStackPopData ) ) == CD_STRUCTURE_ERROR )
	{
		::fprintf( stderr, "[STACK][CDStructureStackPush( %d %d %d ) Error][R:%d][L:%d]\n", 
			stpStack, 
			&iStackPopType, 
			&ipStackPopData, 
			iResult, 
			__LINE__ );

		CDStructureStackFree( &stpStack );

		::exit( -1 );
	}

	::fprintf( stderr, "[STACK][Type:%d][Data:%d][L:%d]\n", 
		iStackPopType, 
		*ipStackPopData, 
		iResult, 
		__LINE__ );

	::fprintf( stderr, "[STACK][USE_COUNT:%d][L:%d]\n", CDStructureStackGetUseCount( stpStack ), __LINE__ );
	::fprintf( stderr, "[STACK][IDLE_COUNT:%d][L:%d]\n", CDStructureStackGetIdleCount( stpStack ), __LINE__ );

	// Stack 객체 정보 초기화
	// Ps) 객체 안의 데이터를 모두 삭제하고 변수들을 초기화
	CDStructureStackFree( &stpStack );

	// Stack 객체 공간 해제
	::free( stpStack );

	return 0;
}

