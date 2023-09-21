#include <stdio.h>
#include <string.h>
#include <sys/ipc.h>
#include <time.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/msg.h>
#include <pthread.h> 

#include "logqueue.h"
#include "queuedef.h"
#include "logglobal.h"
#include "logconfig.h"

static int              gProcType;
static QUEUE_INFO       gQueueInfo;
static pthread_mutex_t  gLogQMutex;

/*PRIVATE FUNCTION*/
long Str2Dec( char *cpHex );

void InitValue();
void InitLogQMutex();
void DestroyLogQMutex();
int GetQueueID();
int GetQueueKey();
int MakeSendData(QUEUE_MSG* ptrQSendData,char* szfilename,char* szDestNum,char* ptrMsg,int iLocalMode);
int GetQSize(int iQueueID);

int LogQInit(int iProcType)
{
    int iRtn = 0;

    InitLogQMutex();
    
    gProcType = iProcType;
    
    InitValue();
    iRtn = GetQueueID();
    
    return iRtn;
}

int LogQReSet()
{
    int iRtn = 0;
    
    pthread_mutex_lock(&gLogQMutex);
    InitValue();
    iRtn = GetQueueID();
    pthread_mutex_unlock(&gLogQMutex);
    
    return iRtn;
}

int LogQMsgSend(char* szfilename,char* szDestNum,char* ptrMsg,int iLocalMode)
{
	int iCnt = 0;
	int iRetry = 0;
    int iResult = 0;
    int iRtn = 0;
    
    QUEUE_MSG tQSendData;
    
    memset(&tQSendData,0x00,sizeof(QUEUE_MSG));
    
    iResult = MakeSendData(&tQSendData,szfilename,szDestNum,ptrMsg,iLocalMode);
    
    if(iResult < 0)
    {
        fprintf( stderr, "ERROR_MAKE_SEND_DATA [L:%d]\n", __LINE__ );
        return iResult;
    }
    
    pthread_mutex_lock(&gLogQMutex);
    
    while(1)
    {
    	if(gQueueInfo.iCurQueue >= gQueueInfo.iQueueCnt)
    	{
    		gQueueInfo.iCurQueue = 0;
    	}
        
    	iCnt = gQueueInfo.iCurQueue;

 	    while( ( iResult = msgsnd(gQueueInfo.iQueueID[iCnt], (void*)&tQSendData, sizeof(QUEUE_MSG)-sizeof(long), IPC_NOWAIT ) ) == ERR_IPC_FAIL && errno == EINTR );
	    
    	gQueueInfo.iCurQueue++;
        
        if(iResult == ERR_IPC_FAIL)
        {
    	    if(errno == EAGAIN)
    	    {
    	        if(iRetry >= gQueueInfo.iQueueCnt)
    	        {
    	            iRtn = ERR_SEND_QUEUE_FULL;
    	            break;
    	        }
    	        else
    	        {
    	            iRetry++;
    	            continue;
    		    }
            }
    	    else
            {
                fprintf( stderr, "ERROR_SEND_QUEUE_FAIL [L:%d]\n", __LINE__ );
    		    iRtn = ERR_SEND_QUEUE_FAIL;
    		    break;
    		}
        }
        else
        {
            iRtn = SUCCESS;
            break;
        }
	}
	
	pthread_mutex_unlock(&gLogQMutex);
	
	return iRtn;

}

int LogQGetProcType()
{
    return gProcType;
}

void LogQDestroy()
{
    DestroyLogQMutex();
}


void InitValue()
{
    int i = 0;

    gQueueInfo.iQueueCnt = 0;
	gQueueInfo.iCurQueue = 0;

	for(i=0;i<MAX_QUEUE_SIZE;i++)
	{
		gQueueInfo.iQueueKey[i] = 0;
		gQueueInfo.iQueueID[i] = 0;
	}
}

void InitLogQMutex()
{
    pthread_mutex_init(&gLogQMutex, NULL);
}

void DestroyLogQMutex()
{
    pthread_mutex_destroy(&gLogQMutex);
}

int GetQueueID()
{
    int i = 0;
	int iQueueID = 0;
	int iInitMode = 0;
	
	iInitMode = Q_NOT_CREATE_MODE;
	
	if(GetQueueKey() != SUCCESS)
	{
	    fprintf( stderr, "ERROR_GET_QUEUE_KEY [L:%d]\n", __LINE__ );
		return ERR_GET_Q_KEY;
	}

	if(gQueueInfo.iQueueCnt <= 0)
	{
	    fprintf( stderr, "ERROR_GET_QUEUE_COUNT [L:%d]\n", __LINE__ );
		return ERR_Q_CNT;
	}

	for(i=0;i<gQueueInfo.iQueueCnt;i++)
	{
	    if(iInitMode == Q_CREATE_MODE)
	    {
	        iQueueID = msgget(gQueueInfo.iQueueKey[i],0666 | IPC_CREAT);
	    }
		else
		{
		    iQueueID = msgget(gQueueInfo.iQueueKey[i],0);
		}
		
		if(iQueueID == -1)
		{
		    fprintf( stderr, "ERROR_MSGGET_FAIL [L:%d]\n", __LINE__ );
			return ERR_INIT_Q_FAIL;
		}
		else
		{
			gQueueInfo.iQueueID[i] = iQueueID;
		}
	}

	return SUCCESS;
}

int GetQSize(int iQueueID)
{
    struct msqid_ds buf;
    if(msgctl(iQueueID,IPC_STAT,&buf)<0)
    {
        return -1;
    }
    
    return buf.msg_qnum;
}

int GetQueueKey()
{
	int i = 0;
	int iNum = 0;
	
	char strQueueCnt[STR_QUEUE_CNT_LEN]={0,};
	char strTemp[STR_TEMP_LEN_10]={0,};
	char strQueueKey[STR_QUEUE_KEY_LEN]={0,};

	memset(strQueueCnt,0x00,STR_QUEUE_CNT_LEN);
	memset(strTemp,0x00,STR_TEMP_LEN_10);
	memset(strQueueKey,0x00,STR_QUEUE_KEY_LEN);

	if(GetConfig(Q_INFO_FILE_NAME,"COMMON","LOG_QUEUE_NUM",strQueueCnt)<1)
	{
		return ERR_GET_Q_CNT;
	}
	else
	{
		gQueueInfo.iQueueCnt = atoi(strQueueCnt);
	}

	for(i=0;i<gQueueInfo.iQueueCnt;i++)
	{
	    iNum = i+1;
		sprintf(strTemp,"QUQUE_%d",iNum);
		if(GetConfig(Q_INFO_FILE_NAME,strTemp,"LOG_QUEUE_KEY",strQueueKey)<1)
		{
			return ERR_GET_Q_KEY;
		}
		else
		{
			gQueueInfo.iQueueKey[i] = Str2Dec(strQueueKey);

			memset(strQueueKey,0x00,STR_QUEUE_KEY_LEN);
		}
	}

	return SUCCESS;
}

int MakeSendData(QUEUE_MSG* ptrQSendData,char* szfilename,char* szDestNum,char* ptrMsg,int iLocalMode)
{
    int iTemp = 0;
    
    iTemp = strlen(szfilename);
    if(iTemp < 0 || iTemp > Q_MSG_FILENAME_LEN-1)
    {
        return ERR_FILENAME_SIZE;
    }
    
    iTemp = strlen(szDestNum);
    if(iTemp < 0 || iTemp > Q_MSG_ROUTINGNUM_LEN-1)
    {
        return ERR_DESTNUM_SIZE;
    }

    iTemp = strlen(ptrMsg);
    if(iTemp < 0 || iTemp > Q_MSG_LOGDATA_LEN-1)
    {
        return ERR_MSG_SIZE;
    }
    
    ptrQSendData->iMsgType = 1;
    ptrQSendData->iFileType = iLocalMode;
    strcpy(ptrQSendData->szFimeName,szfilename);
    strcpy(ptrQSendData->szRoutingNum,szDestNum);
    ptrQSendData->tHeader.iSystem = gProcType;
    ptrQSendData->tHeader.iMsgType = SEND_Q_MSG_TYPE_DAT_SEND;
    ptrQSendData->tHeader.iVer = SNED_Q_MSG_VER;
    ptrQSendData->tHeader.iLen = strlen(ptrMsg);
    ptrQSendData->tHeader.iSerialNo = 0;

    strcpy(ptrQSendData->szLogData,ptrMsg);

    return SUCCESS;
}

long Str2Dec( char *cpHex )
{
    char	cpBuf[100];
    int		nLen;
	int		nLoop;
    long	lnResult=0;
	long	lnMultiple=1;

    strcpy( cpBuf,cpHex );
    if( strncmp( cpBuf,"0x",2 ) == 0 )
        strcpy( cpBuf,cpBuf+2 );
	else
		return( atol( cpBuf ) );

    nLen = strlen( cpBuf );
    for( nLoop = nLen-1; nLoop >= 0; nLoop-- )
    {
        if( cpBuf[ nLoop ] >= '0' && cpBuf[ nLoop ] <= '9' )
            lnResult += ( ( cpBuf[nLoop] - '0' ) * lnMultiple );
        else

        if( cpBuf[nLoop] >= 'A' && cpBuf[nLoop] <= 'F' )
            lnResult += ( ( cpBuf[nLoop] - 'A' + 10 ) * lnMultiple );
        else

        if( cpBuf[nLoop] >= 'a' && cpBuf[nLoop] <= 'f' )
            lnResult += ( ( cpBuf[nLoop] - 'a' + 10 ) * lnMultiple );

        lnMultiple *= 16;
    }

    return( lnResult );
}





/*

int GetQueueInitMode()
{
    int iRtn = 0;
    char szMode[STR_TEMP_LEN_5] = {0,};

    memset(szMode,0x00,STR_TEMP_LEN_5);
    
    if(GetConfig(LOG_CFG_FILE_NAME,"COMMON","LOG_QUEUE_CREATE_MODE",szMode)<1)
	{
	    fprintf( stderr, "ERR_LOG_QUEUE_INIT_MODE [Default : On] [L:%d]\n", __LINE__);
		iRtn = Q_INIT_DEFAULT;
		
		return iRtn;
	}
	
	if(!strcmp(szMode,"ON"))
    {
        iRtn = Q_INIT_CREATE_MODE;
    }
    else if(!strcmp(szMode,"OFF"))
    {
        iRtn = Q_INIT_NOT_CREATE_MODE;
    }
    else
    {
        fprintf( stderr, "ERR_LOG_QUEUE_INIT_MODE [Default : On] [L:%d]\n", __LINE__ );
        iRtn = Q_INIT_DEFAULT;
    }
    
    return iRtn;
    
}

void GetSystemID()
{
    char strSysID[STR_TEMP_LEN_10] = {0,};

    memset(strSysID,0x00,STR_TEMP_LEN_10);
    
    if(GetConfig(LOG_CFG_FILE_NAME,"COMMON","LOG_QUEUE_SEND_SYSTEM",strSysID)<1)
	{
	    fprintf( stderr, "ERR_LOG_QUEUE_SEND_SYSTEM [Default : %d] [L:%d]\n",SEND_Q_SYS_DEFALUT, __LINE__);
		gSystemID = SEND_Q_SYS_DEFALUT;                                         
	    
	    return;
	}
	
    if(!strcmp(strSysID,"MP"))
    {
        gSystemID = SEND_Q_SYS_MP;
    }
    else if(!strcmp(strSysID,"TELESA"))
    {
        gSystemID = SEND_Q_SYS_TELESA;
    }
    else if(!strcmp(strSysID,"IMP"))
    {
        gSystemID = SEND_Q_SYS_IMP;
    }
    else if(!strcmp(strSysID,"MASS"))
    {
        gSystemID = SEND_Q_SYS_MASS;
    }
    else
    {
        fprintf( stderr, "ERR_LOG_QUEUE_SEND_SYSTEM [Default : %d] [L:%d]\n",SEND_Q_SYS_DEFALUT, __LINE__ );
        gSystemID = SEND_Q_SYS_DEFALUT;
    }
}
*/
