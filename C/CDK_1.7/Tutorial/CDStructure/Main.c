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

	int				iLinkedListPushKey;		// Linked List ������ ����½� Ű ��
											// Ps) List�� �˻� ��ɵ� �����ϹǷ� Ű ���� �ʿ�
	int				iLinkedListPushType;	// Linked List ������ �ԷµǴ� �������� ���� ��
	int				iLinkedListPushData;	// Linked List �� �ԷµǴ� ������

	int				iQueuePushType;			// Queue ������ �ԷµǴ� �������� ���� ��
	int				iQueuePushData;			// Queue �� �ԷµǴ� ������

	int				iStackPushType;			// Stack ������ �ԷµǴ� �������� ���� ��
	int				iStackPushData;			// Stack �� �ԷµǴ� ������

	int				iLinkedListPopKey;		// Linked List ������ ���� �� ������ Ű���� ���� �� ����
	int				iLinkedListPopType;		// Linked List ������ ���� �� ������ ������ ���� �� ����
	int				*ipLinkedListPopData;	// Linked List ���� ������ �����͸� ���� ����(������)

	int				iQueuePopType;			// Queue ������ ���� �� ������ ������ ���� �� ����
	int				*ipQueuePopData;		// Queue ���� ������ �����͸� ���� ����(������)

	int				iStackPopType;			// Stack ������ ���� �� ������ ������ ���� �� ����
	int				*ipStackPopData;		// Stack ���� ������ �����͸� ���� ����(������)

	stCDStructureLinkedList		*stpLinkedList;	// Linked List ��ü�� ������
	stCDStructureQueue			*stpQueue;		// Queue ��ü�� ������
	stCDStructureStack			*stpStack;		// Stack ��ü�� ������

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////// Linked List ////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Linked List ��ü ����
	stpLinkedList	= CDStructureLinkedListMake();

	// Linked List ��ü �ʱ�ȭ
	// Ps) CD_STRUCTURE_LINKED_LIST_SIZE(10000) ���� �����͸� ���� �� ������ �̸� Ȯ��
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

	// Linked List �ȿ� ���� ������ ���� ������ �߰� Ȯ��
	// ps) CD_STRUCTURE_LINKED_LIST_SIZE(10000) ���� ��ŭ �߰� Ȯ��
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

	// Linked List �ȿ� ���� ������ ���� ������ ����
	// ps) CD_STRUCTURE_LINKED_LIST_SIZE(10000) ���� ��ŭ Ȯ��
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

	// Linked List�� �����͸� ����
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

	// Linked List���� �����͸� �̾Ƴ���
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

	// Linked List ��ü ���� �ʱ�ȭ
	// Ps) ��ü ���� �����͸� ��� �����ϰ� �������� �ʱ�ȭ
	CDStructureLinkedListFree( &stpLinkedList );

	// Linked List ��ü ���� ����
	::free( stpLinkedList );

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////// Queue ///////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Queue ��ü ����
	stpQueue	= CDStructureQueueMake();

	// Queue ��ü �ʱ�ȭ
	// Ps) CD_STRUCTURE_QUEUE_SIZE(10000) ���� �����͸� ���� �� ������ �̸� Ȯ��
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

	// Queue �ȿ� ���� ������ ���� ������ �߰� Ȯ��
	// ps) CD_STRUCTURE_QUEUE_SIZE(10000) ���� ��ŭ �߰� Ȯ��
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

	// Queue �ȿ� ���� ������ ���� ������ ����
	// ps) CD_STRUCTURE_QUEUE_SIZE(10000) ���� ��ŭ Ȯ��
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

	// Queue�� �����͸� ����
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

	// Queue���� �����͸� �̾Ƴ���
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

	// Queue ��ü ���� �ʱ�ȭ
	// Ps) ��ü ���� �����͸� ��� �����ϰ� �������� �ʱ�ȭ
	CDStructureQueueFree( &stpQueue );

	// Queue ��ü ���� ����
	::free( stpQueue );

	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////// Stack //////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Stack ��ü ����
	stpStack	= CDStructureStackMake();

	// Stack ��ü �ʱ�ȭ
	// Ps) CD_STRUCTURE_STACK_SIZE(10000) ���� �����͸� ���� �� ������ �̸� Ȯ��
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

	// Stack �ȿ� ���� ������ ���� ������ �߰� Ȯ��
	// ps) CD_STRUCTURE_STACK_SIZE(10000) ���� ��ŭ �߰� Ȯ��
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

	// Stack �ȿ� ���� ������ ���� ������ ����
	// ps) CD_STRUCTURE_STACK_SIZE(10000) ���� ��ŭ Ȯ��
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

	// Stack�� �����͸� ����
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

	// Stack���� �����͸� �̾Ƴ���
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

	// Stack ��ü ���� �ʱ�ȭ
	// Ps) ��ü ���� �����͸� ��� �����ϰ� �������� �ʱ�ȭ
	CDStructureStackFree( &stpStack );

	// Stack ��ü ���� ����
	::free( stpStack );

	return 0;
}
