#ifndef __CD_IPC_ST_MSG_QUEUE_H__
#define __CD_IPC_ST_MSG_QUEUE_H__

#ifdef  _SOLARIS_
    #include <sys/msg.h>
#elif _CENT_OS_
    #include <sys/msg.h>
#else
    #include <sys/msg.h>
#endif


#ifdef __cplusplus
extern "C" {
#endif

typedef struct _stCDIpcStMsgQueue
{
    int         iQueueID;           // ����ü�� ���� �� Queue�� �ĺ���
    int         iCreatePermission;  // Queue ���� �� ���� �ɼ�

    key_t       iQueueKey;          // Queue���� Ȥ�� ����� Queue�� Key

    struct msqid_ds stMsgQueueStat; // iQueueID �� ����Ű�� Queue�� ���������� �����ϴ� ����ü

} stCDIpcStMsgQueue;


void CDIpcStMsgQueueInit( stCDIpcStMsgQueue* _stpQueueInfo, int _iQueueKey, int _iPerm );

int CDIpcStMsgQueueCreate( stCDIpcStMsgQueue* _stpQueueInfo );
int CDIpcStMsgQueueOpen( stCDIpcStMsgQueue* _stpQueueInfo );
int CDIpcStMsgQueueRemove( stCDIpcStMsgQueue* _stpQueueInfo );

long CDIpcStMsgQueueWrite( stCDIpcStMsgQueue* _stpQueueInfo, void* _vpPacket, int _iPacketSize );
long CDIpcStMsgQueueRead( stCDIpcStMsgQueue* _stpQueueInfo, void* _vpPacket, int _iPacketSize, int _iMsgType );

int CDIpcStMsgQueueGetSize( stCDIpcStMsgQueue* _stpQueueInfo );
int CDIpcStMsgQueueSetSize( stCDIpcStMsgQueue* _stpQueueInfo, int _iSize );

int CDIpcStMsgQueueGetPermission( stCDIpcStMsgQueue* _stpQueueInfo );
int CDIpcStMsgQueueSetPermission( stCDIpcStMsgQueue* _stpQueueInfo, int _iPermission );

int CDIpcStMsgQueueGetStat( stCDIpcStMsgQueue* _stpQueueInfo, struct msqid_ds* _stpMsqidBuffer );
int CDIpcStMsgQueueSetStat( stCDIpcStMsgQueue* _stpQueueInfo, struct msqid_ds* _stpMsqidInfo );
int CDIpcStMsgQueueGetCount( stCDIpcStMsgQueue* _stpQueueInfo );


#ifdef __cplusplus
}
#endif

#endif
