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
    int         iQueueID;           // 구조체와 연결 된 Queue의 식별자
    int         iCreatePermission;  // Queue 생성 시 권한 옵션

    key_t       iQueueKey;          // Queue생성 혹은 열기시 Queue의 Key

    struct msqid_ds stMsgQueueStat; // iQueueID 가 가르키는 Queue의 세부정보를 저장하는 구조체

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

