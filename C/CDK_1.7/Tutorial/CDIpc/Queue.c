#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/atomic.h>

#include "Queue.h"
#include "CDIpc.h"


int main( int argc, char **argv )
{
	int		iResult;
	int		iForLoop;
	int		iQueueID;
	int		iQueueSize;
	int		iQueuePermission;

	int		iQueueDeleteCount;
	int		iQueueOccupiedCount;

	stPacket	stWritePacket;
	stPacket	stReadPacket;

	struct msqid_ds stQueueStat;

	// Queue �� �����ϰ� Queue�� �ڵ� ȹ��(��ȯ)
	// Ps) Queue �� �̹� ���� �� ��� ���и� ��ȯ
	if( ( iQueueID = CDIpcMsgQueueCreate( CD_IPC_QUEUE_KEY, CD_IPC_MSG_QUEUE_OPEN_PERM ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueCreate( 0x%x 0x%x ) Error][E:%d][L:%d]\n", 
			CD_IPC_QUEUE_KEY, 
			CD_IPC_MSG_QUEUE_OPEN_PERM, 
			errno, 
			__LINE__ );

		// �̹� Queue�� ���� �� ��� �ش� Queue�� ����
		if( ( iQueueID = CDIpcMsgQueueOpen( CD_IPC_QUEUE_KEY ) ) == CD_IPC_ERROR )
		{
			::fprintf( stderr, "[CDIpcMsgQueueOpen( 0x%x 0x%x ) Error][E:%d][L:%d]\n", 
				CD_IPC_QUEUE_KEY, 
				CD_IPC_MSG_QUEUE_OPEN_PERM, 
				errno, 
				__LINE__ );

			::exit( -1 );
		}
	}

	::fprintf( stderr, "[QueueID][0x%x][L:%d]\n", iQueueID, __LINE__ );

	// ���� Queue�� ũ�⸦ ȹ��(��ȯ)
	if( ( iQueueSize = CDIpcMsgQueueGetSize( iQueueID ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueGetSize( 0x%x ) Error][E:%d][L:%d]\n", 
			iQueueID, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[QueueSize:%d][L:%d]\n", iQueueSize, __LINE__ );

	// ���� Queue�� ũ�⸦ ����
	if( CDIpcMsgQueueSetSize( iQueueID, sizeof( stPacket ) * 100 ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueGetSize( 0x%x %d ) Error][E:%d][L:%d]\n", 
			iQueueID, 
			sizeof( stPacket ) * 100, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	// ���� Queue�� ũ�⸦ ȹ��(��ȯ)
	if( ( iQueueSize = CDIpcMsgQueueGetSize( iQueueID ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueGetSize( 0x%x ) Error][E:%d][L:%d]\n", 
			iQueueID, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[QueueSize:%d][L:%d]\n", iQueueSize, __LINE__ );

	// ���� Queue�� ���ٱ����� ȹ��(��ȯ)
	if( ( iQueuePermission = CDIpcMsgQueueGetPermission( iQueueID ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueGetPermission( 0x%x ) Error][E:%d][L:%d]\n", 
			iQueueID, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[Permission:%d][L:%d]\n", iQueuePermission, __LINE__ );

	// ���� Queue�� ���ٱ����� ����
	// Ps) ���μ����� ��������� ���� ����ڰ� �ƴϸ� ����
	if( CDIpcMsgQueueSetPermission( iQueueID, 0644 ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueSetPermission( 0x%x %d ) Error][E:%d][L:%d]\n", 
			iQueueID, 
			0644, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	// ���� Queue�� ���ٱ����� ȹ��(��ȯ)
	if( ( iQueuePermission = CDIpcMsgQueueGetPermission( iQueueID ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueGetPermission( 0x%x ) Error][E:%d][L:%d]\n", 
			iQueueID, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[Permission:%d][L:%d]\n", iQueuePermission, __LINE__ );

	// ���� Queue�� ������ ���ͼ� Queue ������Ʈ(Queue�� ������ ����)�� ����
	if( CDIpcMsgQueueGetStat( iQueueID, &stQueueStat ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueGetStat( 0x%x %d ) Error][E:%d][L:%d]\n", 
			iQueueID, 
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
	stQueueStat.msg_qbytes		= sizeof( stPacket ) * 200;

	// ���� Queue�� ������ Queue ������Ʈ(stQueueStat)�� �������� ����
	if( CDIpcMsgQueueSetStat( iQueueID, &stQueueStat ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueSetStat( 0x%x %d ) Error][E:%d][L:%d]\n", 
			iQueueID, 
			&stQueueStat, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::memset( &stQueueStat, 0x00, sizeof(struct msqid_ds) );

	// ���� Queue�� ������ ���ͼ� Queue ������Ʈ(Queue�� ������ ����)�� ����
	if( CDIpcMsgQueueGetStat( iQueueID, &stQueueStat ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueGetStat( 0x%x %d ) Error][E:%d][L:%d]\n", 
			iQueueID, 
			&stQueueStat, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[Permission:%d][QueueSize:%d][L:%d]\n", 
		stQueueStat.msg_perm.mode, 
		stQueueStat.msg_qbytes, 
		__LINE__ );

	// Queue ������ ����
	for( iForLoop = 0; iForLoop < CD_IPC_QUEUE_INSERT_NUM; iForLoop++ )
	{
		stWritePacket.iType	= CD_IPC_QUEUE_MSG_TYPE;
		stWritePacket.iInt	= iForLoop;
		::sprintf( stWritePacket.caStr, "%d", iForLoop );

		// Queue ������ ����
		if( ( iResult = CDIpcMsgQueueWrite( iQueueID, &stWritePacket, sizeof( stPacket ) - sizeof( long ) ) ) == CD_IPC_ERROR )
		{
			::fprintf( stderr, "[CDIpcMsgQueueWrite( 0x%x %d %d ) Error][R:%d][E:%d][L:%d]\n", 
				iQueueID, 
				&stWritePacket, 
				sizeof( stPacket ) - sizeof( long ), 
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

	// Queue ������ ����
	while( 1 )
	{
		// Queue ������ ����
		if( ( iResult = CDIpcMsgQueueRead( iQueueID, &stReadPacket, sizeof( stPacket ) - sizeof( long ), CD_IPC_QUEUE_MSG_TYPE ) ) != CD_IPC_MSG_QUEUE_DATA_READ )
		{
			if( iResult == CD_IPC_MSG_QUEUE_DATA_EMPTY )
				break;

			::fprintf( stderr, "[CDIpcStMsgQueueRead( 0x%x %d %d %d ) Error][R:%d][E:%d][L:%d]\n", 
				iQueueID, 
				&stReadPacket, 
				sizeof( stPacket ) - sizeof( long ), 
				CD_IPC_QUEUE_MSG_TYPE, 
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
	if( ( iQueueOccupiedCount = CDIpcMsgQueueGetCount( iQueueID ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueGetCount( 0x%x ) Error][E:%d][L:%d]\n", 
			iQueueID, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[OCCUPIED COUNT:%d][L:%d]\n", iQueueOccupiedCount, __LINE__ );

	// Queue�� ������ ����
	for( iForLoop = 0; iForLoop < CD_IPC_QUEUE_INSERT_NUM; iForLoop++ )
	{
		stWritePacket.iType	= CD_IPC_QUEUE_MSG_TYPE;
		stWritePacket.iInt	= iForLoop;
		::sprintf( stWritePacket.caStr, "%d", iForLoop );

		// Queue�� ������ ����
		if( ( iResult = CDIpcMsgQueueWrite( iQueueID, &stWritePacket, sizeof( stPacket ) - sizeof( long ) ) ) == CD_IPC_ERROR )
		{
			::fprintf( stderr, "[CDIpcMsgQueueWrite( 0x%x %d %d ) Error][R:%d][E:%d][L:%d]\n", 
				iQueueID, 
				&stWritePacket, 
				sizeof( stPacket ) - sizeof( long ), 
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
	if( ( iQueueOccupiedCount = CDIpcMsgQueueGetCount( iQueueID ) ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueGetCount( 0x%x ) Error][E:%d][L:%d]\n", 
			iQueueID, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	::fprintf( stderr, "[OCCUPIED COUNT:%d][L:%d]\n", iQueueOccupiedCount, __LINE__ );

	// Queue ����
	// Ps) ���μ����� ����, �ٸ� ���μ����� Queue��� � ���� ������ �� ����
	if( CDIpcMsgQueueRemove( iQueueID ) == CD_IPC_ERROR )
	{
		::fprintf( stderr, "[CDIpcMsgQueueRemove( 0x%x ) Error][E:%d][L:%d]\n", 
			iQueueID, 
			errno, 
			__LINE__ );

		::exit( -1 );
	}

	return 0;
}
