#ifndef __CD_IPC_MSG_QUEUE_H__
#define __CD_IPC_MSG_QUEUE_H__

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

#ifndef CD_IPC_MSG_QUEUE_DATA_FULL
#define CD_IPC_MSG_QUEUE_DATA_FULL      1
#endif

#ifndef CD_IPC_MSG_QUEUE_DATA_EMPTY
#define CD_IPC_MSG_QUEUE_DATA_EMPTY     2
#endif

#ifndef CD_IPC_MSG_QUEUE_DATA_READ
#define CD_IPC_MSG_QUEUE_DATA_READ      3
#endif

#ifndef CD_IPC_MSG_QUEUE_DATA_WRITE
#define CD_IPC_MSG_QUEUE_DATA_WRITE     4
#endif

#ifndef CD_IPC_MSG_QUEUE_OPEN_PERM
#define CD_IPC_MSG_QUEUE_OPEN_PERM      0666
#endif

#ifndef CD_IPC_MSG_QUEUE_CREATE_MODE
#define CD_IPC_MSG_QUEUE_CREATE_MODE    ( IPC_CREAT | IPC_EXCL )
#endif


int CDIpcMsgQueueCreate( int _iKey, int _iPermission );
int CDIpcMsgQueueOpen( int _iKey );
int CDIpcMsgQueueRemove( int _iQueueID );

int CDIpcMsgQueueWrite( int _iQueueID, void* _vpPacket, int _iPacketSize );
int CDIpcMsgQueueRead( int _iQueueID, void* _vpPacket, int _iPacketSize, int _iMsgType );

int CDIpcMsgQueueGetSize( int _iQueueID );
int CDIpcMsgQueueSetSize( int _iQueueID, int _iSize );

int CDIpcMsgQueueGetPermission( int _iQueueID );
int CDIpcMsgQueueSetPermission( int _iQueueID, int _iPermission );

int CDIpcMsgQueueGetStat( int _iQueueID, struct msqid_ds* _stpStat );
int CDIpcMsgQueueSetStat( int _iQueueID, struct msqid_ds* _stpStat );

int CDIpcMsgQueueGetCount( int _iQueueID );


#ifdef __cplusplus
}
#endif

#endif

