#ifndef __CN_IPC_MSG_QUEUE_H__
#define __CN_IPC_MSG_QUEUE_H__

#include <sys/msg.h>

#include "CNIpcDefinitions.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifndef CN_IPC_QUEUE_DATA_ERROR
#define CN_IPC_QUEUE_DATA_ERROR		-1
#endif

#ifndef CN_IPC_QUEUE_DATA_EMPTY
#define CN_IPC_QUEUE_DATA_EMPTY		0
#endif

#ifndef CN_IPC_QUEUE_DATA_FULL
#define CN_IPC_QUEUE_DATA_FULL		0
#endif

#ifndef CN_IPC_QUEUE_DATA_READ
#define CN_IPC_QUEUE_DATA_READ		1
#endif

#ifndef CN_IPC_QUEUE_DATA_WRITE
#define CN_IPC_QUEUE_DATA_WRITE		2
#endif

#ifndef CN_IPC_QUEUE_OPEN_PERM
#define CN_IPC_QUEUE_OPEN_PERM		0666
#endif

#ifndef CN_IPC_QUEUE_CREATE_MODE
#define CN_IPC_QUEUE_CREATE_MODE		( IPC_CREAT | IPC_EXCL )
#endif

typedef struct _stQueueInfo
{
	int			iQueueID;			// ����ü�� ���� �� Queue�� �ĺ���
	int			iMaxCount;			// Queue�� ���� �� �ִ� ������ �Ǽ�

	int			iCreateMode;		// Queue ���� �� ���� �ɼ�
	int			iOpenPermission;	// Queue ���� �� ���� �ɼ�

	key_t		iQueueKey;			// Queue���� Ȥ�� ����� Queue�� Key

	struct msqid_ds	stMsgQueueStat;	// iQueueID �� ����Ű�� Queue�� ���������� �����ϴ� ����ü

} stQueueInfo;

void CNMsgQueueInit( stQueueInfo* _stpQueueInfo, int _iQueueKey, int _iMaxCount, int _iMode, int _iPerm );

int CNMsgQueueCreate( stQueueInfo* _stpQueueInfo );
int CNMsgQueueOpen( stQueueInfo* _stpQueueInfo );

int CNMsgQueueClean( stQueueInfo* _stpQueueInfo );
int CNMsgQueueRemove( stQueueInfo* _stpQueueInfo );

int CNMsgQueueSetSize( stQueueInfo* _stpQueueInfo, int _iSize );
int CNMsgQueueSetPermission( stQueueInfo* _stpQueueInfo, int _iPermission );

int CNMsgQueueGetStat( stQueueInfo* _stpQueueInfo );
int CNMsgQueueGetCount( stQueueInfo* _stpQueueInfo );

long CNMsgQueueWrite( stQueueInfo* _stpQueueInfo, void* _vpPacket, int _iPacketSize );
long CNMsgQueueRead( stQueueInfo* _stpQueueInfo, void* _vpPacket, int _iPacketSize, int _iMsgType );

int CNQueueOpen( int _iKey );
int CNQueueCreate( int _iKey );

long CNQueueWrite( int _iQueueID, void* _vpPacket, int _iPacketSize );
long CNQueueRead( int _iQueueID, void* _vpPacket, int _iPacketSize, int _iMsgType );

int CNQueueSetSize( int _iQueueID, int _iSize );

#ifdef __cplusplus
}
#endif

#endif
