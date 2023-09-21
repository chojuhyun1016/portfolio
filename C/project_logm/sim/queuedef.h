#ifndef	__QUEUEDEF_H__
#define	__QUEUEDEF_H__

#ifdef __cplusplus
extern "C" {
#endif

#define Q_CREATE_MODE               1
#define Q_NOT_CREATE_MODE           2
#define Q_INIT_DEFAULT                   Q_NOT_CREATE_MODE

#define SEND_Q_MSG_TYPE_DAT_SEND    0x0030
#define SNED_Q_MSG_VER                   1

#define MAX_QUEUE_SIZE                 100

#define STR_QUEUE_CNT_LEN                5
#define STR_QUEUE_KEY_LEN               10
#define STR_QUEUE_ID_LEN                10

#define Q_MSG_FILENAME_LEN             128
#define Q_MSG_ROUTINGNUM_LEN            32
#define Q_MSG_LOGDATA_LEN              512

typedef struct
{
	int iQueueCnt;
	int iCurQueue;
	int iQueueKey[MAX_QUEUE_SIZE];
	int iQueueID[MAX_QUEUE_SIZE];
}QUEUE_INFO;

typedef struct
{
    unsigned short iSystem;
    unsigned short iMsgType;
    unsigned short iVer;
    unsigned short iLen;
    unsigned int   iSerialNo;
}QUEUE_MSG_HEADER;

typedef struct
{
    long iMsgType;
    int iFileType;
    char szFimeName[Q_MSG_FILENAME_LEN];
    char szRoutingNum[Q_MSG_ROUTINGNUM_LEN];
    QUEUE_MSG_HEADER tHeader;
    char szLogData[Q_MSG_LOGDATA_LEN];
}QUEUE_MSG;

#ifdef __cplusplus
}
#endif

#endif /* __QUEUEDEF_H__ */





/*
#define SEND_Q_SYS_MP               0x0010
#define SEND_Q_SYS_TELESA           0x0020
#define SEND_Q_SYS_IMP              0x0030
#define SEND_Q_SYS_MASS             0x0040
#define SEND_Q_SYS_DEFALUT               SEND_Q_SYS_TELESA
*/
