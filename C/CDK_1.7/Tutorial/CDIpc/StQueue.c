#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/atomic.h>

#include "StQueue.h"
#include "CDIpc.h"


int main( int argc, char **argv )
{
	int		iResult;
	int		iForLoop;
	int		iQueueID;
	int		iQueueSize;
	int		iQueuePermission;
	int		iQueueOccupiedCount;

	stStPacket	stWritePacket;
	stStPacket	stReadPacket;

	struct msqid_ds		stQueueStat;
	stCDIpcStMsgQueue	stTutorialQueue;

	// Queue ������Ʈ(Queue �������� ����) �ʱ�ȭ
	CDIpcStMsgQueueInit( &stTutorialQueue, CD_IPC_ST_QUEUE_KEY, CD_IPC_MSG_QUEUE_OPEN_PERM );

	// Queue ����
	if( CDIpcStMsgQueueCreate( &stTutorialQueue ) < 0 )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueCreate( %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			errno, 
			__LINE__ );

		if( CDIpcStMsgQueueOpen( &stTutorialQueue ) < 0 )
		{
			::fprintf( stderr, "[CDIpcStMsgQueueOpen( %d ) Error][E:%d][L:%d]\n", 
				&stTutorialQueue, 
				errno, 
				__LINE__ );

			::exit( -1 );
		}
	}

	::fprintf( stderr, "[QueueID][0x%x][L:%d]\n", stTutorialQueue.iQueueID, __LINE__ );

	// Queue�� ����� ȹ��(��ȯ)
	if( ( iQueueSize = CDIpcStMsgQueueGetSize( &stTutorialQueue ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueGetSize( %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[QueueSize:%d][L:%d]\n", iQueueSize, __LINE__ );

	// Queue�� �ִ� ������ ����
	if( CDIpcStMsgQueueSetSize( &stTutorialQueue, sizeof( stStPacket ) * 100 ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueSetSize( %d %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			sizeof( stStPacket ) * 100, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	// Queue�� ����� ȹ��(��ȯ)
	if( ( iQueueSize = CDIpcStMsgQueueGetSize( &stTutorialQueue ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueGetSize( %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[QueueSize:%d][L:%d]\n", iQueueSize, __LINE__ );

	// Queue�� ���ٱ��� ȹ��(��ȯ)
	if( ( iQueuePermission = CDIpcStMsgQueueGetPermission( &stTutorialQueue ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueGetPermission( %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[Permission:%d][L:%d]\n", iQueuePermission, __LINE__ );

	// Queue�� ���ٱ��� ����
	if( CDIpcStMsgQueueSetPermission( &stTutorialQueue, 0644 ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueSetPermission( %d %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			0644, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	// Queue�� ���ٱ��� ȹ��(��ȯ)
	if( ( iQueuePermission = CDIpcStMsgQueueGetPermission( &stTutorialQueue ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueGetPermission( %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[Permission:%d][L:%d]\n", iQueuePermission, __LINE__ );

	// Queue�� ������Ʈ(Queue�� ���� ����)�� ���� Queue�� �����ͷ� ����ȭ
	if( CDIpcStMsgQueueGetStat( &stTutorialQueue, &stQueueStat ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueGetStat( %d %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			&stQueueStat, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[Permission:%d][QueueSize:%d][L:%d]\n", 
		stQueueStat.msg_perm.mode, 
		stQueueStat.msg_qbytes, 
		__LINE__ );

	stQueueStat.msg_perm.mode	= 0622;
	stQueueStat.msg_qbytes		= sizeof( stStPacket ) * 200;

	// Queue�� ������ Queue ������Ʈ�� ������ ����ȭ
	// Ps) Super User(Root)�� �ƴϸ� �� �Լ��� ����
	if( CDIpcStMsgQueueSetStat( &stTutorialQueue, &stQueueStat ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueSetStat( %d %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			&stQueueStat, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::memset( &stQueueStat, 0x00, sizeof(struct msqid_ds) );

	// Queue�� ������Ʈ(Queue�� ���� ����)�� ���� Queue�� �����ͷ� ����ȭ
	if( CDIpcStMsgQueueGetStat( &stTutorialQueue, &stQueueStat ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueGetStat( %d %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			&stQueueStat, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[Permission:%d][QueueSize:%d][L:%d]\n", 
		stQueueStat.msg_perm.mode, 
		stQueueStat.msg_qbytes, 
		__LINE__ );

	// Queue�� ������ ����
	for( iForLoop = 0; iForLoop < CD_IPC_ST_QUEUE_INSERT_NUM; iForLoop++ )
	{
		stWritePacket.iType	= CD_IPC_ST_QUEUE_MSG_TYPE;
		stWritePacket.iInt	= iForLoop;
		::sprintf( stWritePacket.caStr, "%d", iForLoop );

		// Queue ������ ����
		if( ( iResult = CDIpcStMsgQueueWrite( &stTutorialQueue, &stWritePacket, sizeof( stStPacket ) - sizeof( long ) ) ) == CD_IPC_ERROR )
		{
			::fprintf( stderr, "[CDIpcStMsgQueueWrite( %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
				&stTutorialQueue, 
				&stWritePacket, 
				sizeof( stStPacket ) - sizeof( long ), 
				iResult, 
				errno, 
				__LINE__ );

			::exit( -1 );
		}

		::fprintf( stderr, "[WRITE][Type:%d][Int:%d][Str:%s][L:%d]\n", 
			stWritePacket.iType, 
			stWritePacket.iInt, 
			stWritePacket.caStr, 
			__LINE__ );
	}

	// Queue ������ �̾Ƴ���
	while( 1 )
	{
		// Queue ������ �̾Ƴ���
		if( ( iResult = CDIpcStMsgQueueRead( &stTutorialQueue, &stReadPacket, sizeof( stStPacket ) - sizeof( long ), CD_IPC_ST_QUEUE_MSG_TYPE ) ) != CD_IPC_MSG_QUEUE_DATA_READ )
		{
			if( iResult == CD_IPC_MSG_QUEUE_DATA_EMPTY )
				break;

			::fprintf( stderr, "[CDIpcStMsgQueueRead( %d %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
				&stTutorialQueue, 
				&stReadPacket, 
				sizeof( stStPacket ) - sizeof( long ), 
				CD_IPC_ST_QUEUE_MSG_TYPE, 
				iResult, 
				errno, 
				__LINE__ );

			::exit( -1 );
		}

		::fprintf( stderr, "[READ][Type:%d][Int:%d][Str:%s][L:%d]\n", 
			stReadPacket.iType, 
			stReadPacket.iInt, 
			stReadPacket.caStr, 
			__LINE__ );
	}

	// ���� Queue�� ����ִ� �������� ������ ȹ��(��ȯ)
	if( ( iQueueOccupiedCount = CDIpcStMsgQueueGetCount( &stTutorialQueue ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueGetCount( %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[OCCUPIED COUNT:%d][L:%d]\n", iQueueOccupiedCount, __LINE__ );

	// Queue�� ������ ����
	for( iForLoop = 0; iForLoop < CD_IPC_ST_QUEUE_INSERT_NUM; iForLoop++ )
	{
		stWritePacket.iType	= CD_IPC_ST_QUEUE_MSG_TYPE;
		stWritePacket.iInt	= iForLoop;
		::sprintf( stWritePacket.caStr, "%d", iForLoop );

		// Queue�� ������ ����
		if( ( iResult = CDIpcStMsgQueueWrite( &stTutorialQueue, &stWritePacket, sizeof( stStPacket ) - sizeof( long ) ) ) == CD_IPC_ERROR )
		{
			::fprintf( stderr, "[CDIpcStMsgQueueWrite( %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
				&stTutorialQueue, 
				&stWritePacket, 
				sizeof( stStPacket ) - sizeof( long ), 
				iResult, 
				errno, 
				__LINE__ );

			::exit( -1 );
		}

		::fprintf( stderr, "[WRITE][Type:%d][Int:%d][Str:%s][L:%d]\n", 
			stWritePacket.iType, 
			stWritePacket.iInt, 
			stWritePacket.caStr, 
			__LINE__ );
	}

	// ���� Queue�� ����ִ� �������� ������ ȹ��(��ȯ)
	if( ( iQueueOccupiedCount = CDIpcStMsgQueueGetCount( &stTutorialQueue ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueGetCount( %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[OCCUPIED COUNT:%d][L:%d]\n", iQueueOccupiedCount, __LINE__ );

	// Queue ����
	if( ( iResult = CDIpcStMsgQueueRemove( &stTutorialQueue ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcStMsgQueueRemove( %d ) Error][E:%d][L:%d]\n", 
			&stTutorialQueue, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	return 0;
}
