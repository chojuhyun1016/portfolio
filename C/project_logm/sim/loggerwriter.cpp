#include <iostream.h>
#include <stdlib.h>
#include <time.h>
#include <signal.h>
#include <pthread.h> 
#include <unistd.h>

#include "logglobal.h"
#include "loggerwriter.h"
#include "logqueue.h"
#include "logconfig.h"

static char _gLogDirNameLocal[MAX_LOG_DIRECTORY_NAME]; /* Local Save Dir */
static char _gLogDirNameNfs[MAX_LOG_DIRECTORY_NAME];   /* NFS Save Dir   */

static char gCategoryDir[MAX_LOG_DIRECTORY_NAME] = {0,};

static FILE* gFileFd[FILE_TYPE_MAX] = {0,};
static FILE* gLocalFileFd[FILE_TYPE_MAX] = {0,};

static pthread_mutex_t g_FileMutex[FILE_TYPE_MAX];                              /* 201108 Added by SMART*/
static pthread_mutex_t g_LocalFileMutex[FILE_TYPE_MAX];

static int gRunMode = RUN_MODE_TYPE1;
static int gQueueStat;                                                          /* 201108 Added by SMART*/
static int gQueueMode;                                                          /* 201108 Added by SMART*/
static time_t gQCheckTime;                                                      /* 201108 Added by SMART*/
static int g_LogLevel[FILE_TYPE_MAX];
static int g_SmartMode[FILE_TYPE_MAX];

char g_trace_dir[100]={0};
char g_error_dir[100]={0};
char g_service_dir[100]={0};

/** By LIANDLI */
char gCenterDelimeter[64]={0x00,};
/** By LIANDLI END*/
char g_version[100]={0};
char gProcessName[100]={0};
char gKeyName[100]={0};
int gProcessLength;

int g_LogStep;
int g_formation;

/*PRIVATE FUNCTION*/
void SetCenterType(int iCenter);
int  InitLogFile(int iMode);
int  InitLocalLogFile(int iMode);
int  InitSignal();
void InitQueue(int iProcType);
void ResetQueue();
void ResetQueueTimer();
int  FileOpen(int iType,char* szFileName);
void FileClose(int iType);
int  LocalFileOpen(int iType,char* szFileName);
void LocalFileClose(int iType);
void FileAllClose();
void GetLogTime(struct tm* ltime,char* szTime);
void MutexInit();
void MutexDestroy();
int  UnChkLocalFileWrite(int iLogType,char* szBuffer);
int  LogFileWrite(char* filename,int iLogType,char* szBuffer);
int  LocalLogFileWrite(char* filename,int iLogType,char* szBuffer);
void GetConfigData();
void GetLogFileName(int iFileType,char* szFileName, char* path, char* szDirType, struct tm* ltime);
void GetLocalLogFileName(int iFileType,char* szFileName, char* path, char* szDirType, struct tm* ltime);
int  SetInitData(char* process,char* keyname,char* localdir,char* nfsdir,char* service_dir,char* trace_dir,char* error_dir,char* version,int iCenterType,char* szCfgDir);
void GetFileTypeStr(int iFileType,char* szFileType);
int  CheckLocalLogFile(int iFileType);

int jloggerwriter_construct(char* localdir, char* nfsdir, char* process,int center_delimeter, char* version, char* keyname, char* service_dir,char* trace_dir, char* error_dir,int iPrcType,char* szCfgDir)
{
    int iResult = 0;

    InitSignal();
    
    iResult = SetInitData(process,keyname,localdir,nfsdir,service_dir,trace_dir,error_dir,version,center_delimeter,szCfgDir);
    if(iResult == false)
    {
        return false;
    }

    MutexInit();

    iResult = InitLogFile(DONT_FILE_MODE);
    if(iResult == false)
    {
        return false;
    }
    
    iResult = InitLocalLogFile(DONT_FILE_MODE);
    if(iResult == false)
    {
        return false;
    }
    
    InitQueue(iPrcType);
    
	return true;
}

int jloggerwriter_construct_ex(char* localdir, char* nfsdir,char* pcDirName, char* process,int center_delimeter, char* version, char* keyname, char* service_dir,char* trace_dir, char* error_dir,int iPrcType,char* szCfgDir)
{
    int iResult = 0;
    
    gRunMode = RUN_MODE_TYPE2;

    if(strlen(pcDirName) < 0 || strlen(pcDirName) > MAX_LOG_DIRECTORY_NAME-1)
    {
        return false;
    }

    strcpy(gCategoryDir,pcDirName);

    iResult = jloggerwriter_construct(localdir,nfsdir,process,center_delimeter,version,keyname,service_dir,trace_dir,error_dir,iPrcType,szCfgDir);


    return iResult;
}

int jloggerwriter_construct_ex2(char* localdir, char* nfsdir, char* process,int center_delimeter, char* version, char* keyname, char* service_dir,char* trace_dir, char* error_dir,int iPrcType,char* szCfgDir)
{
    int iResult = 0;
    
    gRunMode = RUN_MODE_TYPE3;

    iResult = jloggerwriter_construct(localdir,nfsdir,process,center_delimeter,version,keyname,service_dir,trace_dir,error_dir,iPrcType,szCfgDir);

    return iResult;
}

void LOG(int nLoggingLevel,char* szDestNum, const char *strFormat, ...)
{
	int nRet;
	int iLevel = 0;
	
	va_list ap;
	
	char szBuffer[MAX_WRITE_BUFFER - MAX_LOGTIME_BUFFER - 1]={0,};              //수용가능 BYTE : 0x00포함 1001 (1024 = 20[date] + 1001 + 1[\n])

	va_start (ap, strFormat);
	nRet = (int) vsnprintf (szBuffer, sizeof(szBuffer), strFormat, ap);
	if(nRet < 0) 
	{
	    UnChkLocalFileWrite(FILE_TYPE_ERR,"vsnprintf error log function check\n");
		return;
	}

    if(nLoggingLevel < TRC_0 || nLoggingLevel >= MAX_LOG_INPUT_LEVEL)
    {
		return;
    }
    
    iLevel = nLoggingLevel%MAX_LOG_LEVEL;
    
    switch(nLoggingLevel)
    {
        case TRC_0:
        case TRC_1:
        case TRC_2:
        {
            if(g_LogStep == LOG_STEP_ALL)
    	    {
        		if(g_LogLevel[FILE_TYPE_TRC] >= iLevel)
        		{
        			jloggerwriter_write(FILE_TYPE_TRC, _gLogDirNameLocal, szBuffer, g_trace_dir,szDestNum);
        		}
    	    }
    	    
            break;
        }
        
        case SVC_0:
        case SVC_1:
        case SVC_2:
        {
            if(g_LogStep <= LOG_STEP_SVC_ERR)
            {
                if(g_LogLevel[FILE_TYPE_SVC] >= iLevel)
                {
    			    jloggerwriter_write(FILE_TYPE_SVC, _gLogDirNameNfs, szBuffer, g_service_dir,szDestNum);
    			}
            }
            
            break;
        }
        
        case ERR_0:
        case ERR_1:
        case ERR_2:
        {
            if(g_LogStep <= LOG_STEP_ERR)
        	{
        		if(g_LogLevel[FILE_TYPE_ERR] >= iLevel)
        		{
        			jloggerwriter_write(FILE_TYPE_ERR, _gLogDirNameNfs, szBuffer, g_error_dir,szDestNum);
        		}
        	}
        	break;
        }
    }
	
	va_end (ap);
	
	return;
}


int GET_LOG_FD(int iFileType)
{
    CheckLocalLogFile(iFileType);
    
    return fileno(gLocalFileFd[iFileType]);
}


void jloggerwriter_update()
{
    GetConfigData();
}

int jloggerwriter_destruct()
{
    FileAllClose();
    MutexDestroy;
    LogQDestroy();
    
	return 1;
}

int jloggerwriter_getprocname (char* pProcessName)
{
	int nProcFd;
	psinfo_t stProc;
    
	if ((nProcFd = open("/proc/self/psinfo", O_RDONLY)) < 0)
	{
		perror ("Process Information Get Error\n");
		return 0;
	}
	
	read(nProcFd, &(pProcessName), sizeof (psinfo_t));
	sprintf (pProcessName, "%s", stProc.pr_fname);
	close (nProcFd);
	
	return 1;
}

void jloggerwriter_write(int iLogType, char* path, char* strFormat, char* szDirType,char* szDestNum)
{
    time_t tTime;
	struct tm ltime;
	
	char szBuffer[MAX_WRITE_BUFFER];
	char filename[MAX_LOG_FILE_NAME]={0,};
	char szFailFileName[MAX_LOG_FILE_NAME]={0,};
	char szAlarmMsg[MAX_ALARM_BUFFER]={0,};
	char szLogTime[MAX_LOGTIME_BUFFER]={0,};
    
    int iResult = 0;
    int iProcType = 0;
    
	tTime = time (NULL);
	localtime_r(&tTime, &ltime);
	
    GetLogFileName(iLogType,filename,path,szDirType,&ltime);
    GetLogTime(&ltime,szLogTime);
    
	sprintf (szBuffer,"%s,%s\n",szLogTime,strFormat);
	
	if(gQueueMode == LOG_Q_MODE_ON)
	{
    	if(gQueueStat == Q_STAT_ENABLE)
    	{
        	iResult = LogQMsgSend(filename,szDestNum,szBuffer,g_SmartMode[iLogType]);
        	 
        	if(iResult < 1)
        	{
        	    memset(szAlarmMsg,0x00,sizeof(szAlarmMsg));
        	    
        	    GetLocalLogFileName(iLogType,szFailFileName,path,szDirType,&ltime);
        	    
        	    iProcType = LogQGetProcType();
        	    
        	    if(iResult == ERR_SEND_QUEUE_FULL)
        	    {
        	        sprintf(szAlarmMsg,"[LogQueue Full][%s][%s][%d][%d]%s,%s\n",filename,szDestNum,g_SmartMode[iLogType],iProcType,szLogTime,strFormat);
        	    }
        	    else
        	    {
        	        ResetQueue();
        	        sprintf(szAlarmMsg,"[LogQueue Insert Fail][%s][%s][%d][%d]%s,%s\n",filename,szDestNum,g_SmartMode[iLogType],iProcType,szLogTime,strFormat);
        	    }

        	    LocalLogFileWrite(szFailFileName,iLogType,szAlarmMsg);
        	}
        }
        else
        {
            iProcType = LogQGetProcType();
            
            GetLocalLogFileName(iLogType,szFailFileName,path,szDirType,&ltime);
            
            memset(szAlarmMsg,0x00,sizeof(szAlarmMsg));
            sprintf(szAlarmMsg,"[LogQueue Disable][%s][%s][%d][%d]%s,%s\n",filename,szDestNum,g_SmartMode[iLogType],iProcType,szLogTime,strFormat);
            LocalLogFileWrite(szFailFileName,iLogType,szAlarmMsg);
        
            ResetQueueTimer();
        }
       
    }
    else
    {
        LogFileWrite(filename,iLogType,szBuffer);
    }
}

int InitSignal()
{
    int iRtn = 0;
    struct sigaction act;

	act.sa_handler=sig_handler1;
	sigemptyset(&act.sa_mask);
    act.sa_flags=0;
    
    iRtn = sigaction(SIGUSR1, &act, 0);
    
    return iRtn;
}

void sig_handler1(int sig1)
{
	
	if(sig1 == SIGUSR1)
	{
	    GetConfigData();
	}
	
	return;
}

int SetInitData(char* process,char* keyname,char* localdir,char* nfsdir,char* service_dir,char* trace_dir,char* error_dir,char* version,int iCenterType,char* szCfgDir)
{
#ifdef _SUN_PROC_
	if (jloggerwriter_getprocname(gProcessName) < 0)
		return false;
#else
	strcpy(gProcessName, process);	
#endif

	strcpy(gKeyName, keyname);

    SetCenterType(iCenterType);
    
	gProcessLength = strlen(gProcessName);
	if (gProcessLength == 0 && strlen(gKeyName) == 0)
	{
		return false;
	}
    
    SetCfgDir(szCfgDir);
    GetConfigData();
    
	sprintf (_gLogDirNameLocal, "%s", localdir);
	sprintf (_gLogDirNameNfs  , "%s", nfsdir);
	sprintf (g_service_dir, "%s", service_dir);
	sprintf (g_trace_dir, "%s", trace_dir);
	sprintf (g_error_dir, "%s", error_dir);
	sprintf (g_version, "%s", version);

    gQCheckTime = time(NULL);
    
    return true;
}

void GetConfigData()
{
    
    gQueueMode = GetLogQMode();
    g_LogStep = GetLogStep(gProcessName);
    g_formation = GetLogFormat(gProcessName);
    
    g_LogLevel[FILE_TYPE_TRC] = GetLogLevel(FILE_TYPE_TRC,gProcessName);
    g_LogLevel[FILE_TYPE_SVC] = GetLogLevel(FILE_TYPE_SVC,gProcessName);
    g_LogLevel[FILE_TYPE_ERR] = GetLogLevel(FILE_TYPE_ERR,gProcessName);

    g_SmartMode[FILE_TYPE_TRC] = GetSmartMode(FILE_TYPE_TRC,gProcessName);
    g_SmartMode[FILE_TYPE_SVC] = GetSmartMode(FILE_TYPE_SVC,gProcessName);
    g_SmartMode[FILE_TYPE_ERR] = GetSmartMode(FILE_TYPE_ERR,gProcessName);
}

void SetCenterType(int iCenter)
{
    switch(iCenter)
	{
		case 0 :	strcpy(gCenterDelimeter, "A");	break;
		case 1 :	strcpy(gCenterDelimeter, "B");	break;
		case 2 :	strcpy(gCenterDelimeter, "C");	break;
		case 3 :	strcpy(gCenterDelimeter, "D");	break;
		case 4 :	strcpy(gCenterDelimeter, "E");	break;
		default:	strcpy(gCenterDelimeter, "A");	break;
	}
}

int InitLogFile(int iMode)
{
    int i = 0;
    time_t tTime;
	struct tm ltime;
    char szFileName[MAX_LOG_FILE_NAME] = {0,};
    
    for(i = 0; i < FILE_TYPE_MAX; i++)
    {
        gFileFd[i] = NULL;
    }
    
    if(iMode == CREATE_FILE_MODE)
    {
        tTime = time (NULL);
	    localtime_r(&tTime, &ltime);
	
        GetLogFileName(FILE_TYPE_TRC,szFileName,_gLogDirNameLocal,g_trace_dir,&ltime);
        if(FileOpen(FILE_TYPE_TRC,szFileName) == false)
        {
            return false;
        }
    
        GetLogFileName(FILE_TYPE_ERR,szFileName,_gLogDirNameNfs,g_error_dir,&ltime);
    	if(FileOpen(FILE_TYPE_ERR,szFileName) == false)
        {
            return false;
        }
        
        GetLogFileName(FILE_TYPE_SVC,szFileName,_gLogDirNameNfs,g_service_dir,&ltime);
        if(FileOpen(FILE_TYPE_SVC,szFileName) == false)
        {
            return false;
        }
    }
    /*
    else
    { Don't Create Log File Logic
    }
    */
    return true;
}

int InitLocalLogFile(int iMode)
{
    int i = 0;
    time_t tTime;
	struct tm ltime;
    char szFileName[MAX_LOG_FILE_NAME] = {0,};
    
    for(i = 0; i < FILE_TYPE_MAX; i++)
    {
        gLocalFileFd[i] = NULL;
    }
    
    if(iMode == CREATE_FILE_MODE)
    {
        tTime = time (NULL);
	    localtime_r(&tTime, &ltime);
	
        GetLocalLogFileName(FILE_TYPE_TRC,szFileName,_gLogDirNameLocal,g_trace_dir,&ltime);
        if(LocalFileOpen(FILE_TYPE_TRC,szFileName) == false)
        {
            return false;
        }
    
        GetLocalLogFileName(FILE_TYPE_ERR,szFileName,_gLogDirNameNfs,g_error_dir,&ltime);
    	if(LocalFileOpen(FILE_TYPE_ERR,szFileName) == false)
        {
            return false;
        }
        
        GetLocalLogFileName(FILE_TYPE_SVC,szFileName,_gLogDirNameNfs,g_service_dir,&ltime);
        if(LocalFileOpen(FILE_TYPE_SVC,szFileName) == false)
        {
            return false;
        }
    }
    /*
    else
    { Don't Create Log File Logic
    }
    */
    return true;
}

int CheckLocalLogFile(int iFileType)
{
    int i = 0;
    time_t tTime;
	struct tm ltime;
    char szFileName[MAX_LOG_FILE_NAME] = {0,};
    struct stat stStat;
        
    tTime = time (NULL);
    localtime_r(&tTime, &ltime);
	
    switch (iFileType)
    {
        case FILE_TYPE_TRC:
        {
            GetLocalLogFileName(FILE_TYPE_TRC,szFileName,_gLogDirNameLocal,g_trace_dir,&ltime);
            
            if(stat(szFileName, &stStat) < 0 || gLocalFileFd[FILE_TYPE_TRC] == NULL)
	        {
	            if(LocalFileOpen(FILE_TYPE_TRC,szFileName) == false)
                {
                    return false;
                }
	        }

            break;
        }
        
        case FILE_TYPE_ERR:
        {
            GetLocalLogFileName(FILE_TYPE_ERR,szFileName,_gLogDirNameNfs,g_error_dir,&ltime);
            
            if(stat(szFileName, &stStat) < 0 || gLocalFileFd[FILE_TYPE_ERR] == NULL)
	        {
        	    if(LocalFileOpen(FILE_TYPE_ERR,szFileName) == false)
                {
                    return false;
                }
            }
            
            break;
        }
        case FILE_TYPE_SVC:
        {
            GetLocalLogFileName(FILE_TYPE_SVC,szFileName,_gLogDirNameNfs,g_service_dir,&ltime);
            
            if(stat(szFileName, &stStat) < 0 || gLocalFileFd[FILE_TYPE_SVC] == NULL)
	        {
                if(LocalFileOpen(FILE_TYPE_SVC,szFileName) == false)
                {
                    return false;
                }
            }
            
            break;
        }
    }
    
    return true;
}

int LogFileWrite(char* filename,int iLogType,char* szBuffer)
{
    struct stat stStat;
    int iResult = 0;

    pthread_mutex_lock(&g_FileMutex[iLogType]);
    
    if(stat(filename, &stStat) < 0 || gFileFd[iLogType] == NULL)
	{
        FileOpen(iLogType,filename);
	}

    if(gFileFd[iLogType] != NULL)
    {
        iResult = fwrite(szBuffer,strlen(szBuffer),1,gFileFd[iLogType]);
        fflush(gFileFd[iLogType]);
    }
    
    pthread_mutex_unlock(&g_FileMutex[iLogType]);
    
    return iResult;
}

int UnChkLocalFileWrite(int iLogType,char* szBuffer)
{
    int iResult = 0;

    if(gLocalFileFd[iLogType] != NULL)
    {
        pthread_mutex_lock(&g_LocalFileMutex[iLogType]);
        iResult = fwrite(szBuffer,strlen(szBuffer),1,gLocalFileFd[iLogType]);
        fflush(gLocalFileFd[iLogType]);
	    pthread_mutex_unlock(&g_LocalFileMutex[iLogType]);
    }
    
    return iResult;
}

int LocalLogFileWrite(char* filename,int iLogType,char* szBuffer)
{
    int iResult = 0;
    struct stat stStat;
    
    pthread_mutex_lock(&g_LocalFileMutex[iLogType]);
    
    if(stat(filename, &stStat) < 0 || gLocalFileFd[iLogType] == NULL)
	{
        LocalFileOpen(iLogType,filename);
	}
    
    if(gLocalFileFd[iLogType] != NULL)
    {
        iResult = fwrite(szBuffer,strlen(szBuffer),1,gLocalFileFd[iLogType]);
        fflush(gLocalFileFd[iLogType]);
    }
    
    pthread_mutex_unlock(&g_LocalFileMutex[iLogType]);
    
    return iResult;
}

void GetLogFileName(int iFileType,char* szFileName, char* path, char* szDirType, struct tm* ltime)
{
    char szFileType[STR_TEMP_LEN_5] = {0,};
    
    memset(szFileName,0x00,MAX_LOG_FILE_NAME);
    
    GetFileTypeStr(iFileType,szFileType);
    
    switch(gRunMode)
    {
        case RUN_MODE_TYPE3:
        {
            if (iFileType == FILE_TYPE_ERR)
            {
        		sprintf (szFileName, "%s/%s/%s.%s.%02d%02d_%s_%s.log", 
        							path, szDirType, gKeyName, szFileType, 
        							ltime->tm_mon + 1, ltime->tm_mday,
        							gCenterDelimeter, g_version);
        	}
        	else
        	{						
                if (g_formation == LOG_FORMAT_HOUR)
                {
            		sprintf (szFileName, "%s/%s/%s.%s.%02d%02d%02d_%s_%s.log", 
            							path, szDirType, gKeyName, szFileType, 
            							ltime->tm_mon + 1, ltime->tm_mday, ltime->tm_hour,
            							gCenterDelimeter, g_version);
            	}
            	else
            	{
            		sprintf (szFileName, "%s/%s/%s.%s.%02d%02d_%s_%s.log", 
            							path, szDirType, gKeyName, szFileType, 
            							ltime->tm_mon + 1, ltime->tm_mday,
            							gCenterDelimeter, g_version);
            	}
            }
            break;
        }
        
        case RUN_MODE_TYPE2:
        {
            if (iFileType == FILE_TYPE_ERR)
            {
        		sprintf (szFileName, "%s/%s/%s/%s/%s.%s.%02d%02d_%s_%s.log", 
        							path,gCategoryDir,szDirType, gProcessName,gKeyName,szFileType, 
        							ltime->tm_mon + 1, ltime->tm_mday,
        							gCenterDelimeter, g_version);
        	}
        	else
        	{						
                if (g_formation == LOG_FORMAT_HOUR)
                {
            		sprintf (szFileName, "%s/%s/%s/%s/%s.%s.%02d%02d%02d_%s_%s.log", 
            							path,gCategoryDir,szDirType,gProcessName,gKeyName,szFileType, 
            							ltime->tm_mon + 1, ltime->tm_mday, ltime->tm_hour,
            							gCenterDelimeter, g_version);
            	}
            	else
            	{
            		sprintf (szFileName, "%s/%s/%s/%s/%s.%s.%02d%02d_%s_%s.log", 
            							path,gCategoryDir,szDirType,gProcessName,gKeyName, szFileType, 
            							ltime->tm_mon + 1, ltime->tm_mday,
            							gCenterDelimeter, g_version);
            	}
        	}
            break;
        }
        
        case RUN_MODE_TYPE1:
        default:
        {
            if (iFileType == FILE_TYPE_ERR)
            {
        		sprintf (szFileName, "%s/%s/%s/%s.%s.%02d%02d_%s_%s.log", 
        							path, gProcessName, szDirType, gKeyName, szFileType, 
        							ltime->tm_mon + 1, ltime->tm_mday,
        							gCenterDelimeter, g_version);
        	}
        	else
        	{						
                if (g_formation == LOG_FORMAT_HOUR)
                {
            		sprintf (szFileName, "%s/%s/%s/%s.%s.%02d%02d%02d_%s_%s.log", 
            							path, gProcessName, szDirType, gKeyName, szFileType, 
            							ltime->tm_mon + 1, ltime->tm_mday, ltime->tm_hour,
            							gCenterDelimeter, g_version);
            	}
            	else
            	{
            		sprintf (szFileName, "%s/%s/%s/%s.%s.%02d%02d_%s_%s.log", 
            							path, gProcessName, szDirType, gKeyName, szFileType, 
            							ltime->tm_mon + 1, ltime->tm_mday,
            							gCenterDelimeter, g_version);
            	}
            }
            break;
        }
        
    }
}

void GetLocalLogFileName(int iFileType,char* szFileName, char* path, char* szDirType, struct tm* ltime)
{
    char szFileType[STR_TEMP_LEN_5] = {0,};
    
    memset(szFileName,0x00,MAX_LOG_FILE_NAME);
    
    GetFileTypeStr(iFileType,szFileType);
    
    switch(gRunMode)
    {
        case RUN_MODE_TYPE3:
        {
            if (iFileType == FILE_TYPE_ERR)
            {
        		sprintf (szFileName, "%s/%s/%s.%s.%02d%02d_%s_%s.log.local", 
        							path, szDirType, gKeyName, szFileType, 
        							ltime->tm_mon + 1, ltime->tm_mday,
        							gCenterDelimeter, g_version);
        	}
        	else
        	{						
                if (g_formation == LOG_FORMAT_HOUR)
                {
            		sprintf (szFileName, "%s/%s/%s.%s.%02d%02d%02d_%s_%s.log.local", 
            							path, szDirType, gKeyName, szFileType, 
            							ltime->tm_mon + 1, ltime->tm_mday, ltime->tm_hour,
            							gCenterDelimeter, g_version);
            	}
            	else
            	{
            		sprintf (szFileName, "%s/%s/%s.%s.%02d%02d_%s_%s.log.local", 
            							path, szDirType, gKeyName, szFileType, 
            							ltime->tm_mon + 1, ltime->tm_mday,
            							gCenterDelimeter, g_version);
            	}
            }
            break;
        }
        
        case RUN_MODE_TYPE2:
        {
            if (iFileType == FILE_TYPE_ERR)
            {
        		sprintf (szFileName, "%s/%s/%s/%s/%s.%s.%02d%02d_%s_%s.log.local", 
        							path,gCategoryDir,szDirType, gProcessName,gKeyName,szFileType, 
        							ltime->tm_mon + 1, ltime->tm_mday,
        							gCenterDelimeter, g_version);
        	}
        	else
        	{						
                if (g_formation == LOG_FORMAT_HOUR)
                {
            		sprintf (szFileName, "%s/%s/%s/%s/%s.%s.%02d%02d%02d_%s_%s.log.local", 
            							path,gCategoryDir,szDirType,gProcessName,gKeyName,szFileType, 
            							ltime->tm_mon + 1, ltime->tm_mday, ltime->tm_hour,
            							gCenterDelimeter, g_version);
            	}
            	else
            	{
            		sprintf (szFileName, "%s/%s/%s/%s/%s.%s.%02d%02d_%s_%s.log.local", 
            							path,gCategoryDir,szDirType,gProcessName,gKeyName, szFileType, 
            							ltime->tm_mon + 1, ltime->tm_mday,
            							gCenterDelimeter, g_version);
            	}
        	}
            break;
        }
        
        case RUN_MODE_TYPE1:
        default:
        {
            if (iFileType == FILE_TYPE_ERR)
            {
        		sprintf (szFileName, "%s/%s/%s/%s.%s.%02d%02d_%s_%s.log.local", 
        							path, gProcessName, szDirType, gKeyName, szFileType, 
        							ltime->tm_mon + 1, ltime->tm_mday,
        							gCenterDelimeter, g_version);
        	}
        	else
        	{
                if (g_formation == LOG_FORMAT_HOUR)
                {
            		sprintf (szFileName, "%s/%s/%s/%s.%s.%02d%02d%02d_%s_%s.log.local", 
            							path, gProcessName, szDirType, gKeyName, szFileType, 
            							ltime->tm_mon + 1, ltime->tm_mday, ltime->tm_hour,
            							gCenterDelimeter, g_version);
            	}
            	else
            	{
            		sprintf (szFileName, "%s/%s/%s/%s.%s.%02d%02d_%s_%s.log.local", 
            							path, gProcessName, szDirType, gKeyName, szFileType, 
            							ltime->tm_mon + 1, ltime->tm_mday,
            							gCenterDelimeter, g_version);
            	}
        	}
            
            break;
        }
    }
}

void GetFileTypeStr(int iFileType,char* szFileType)
{
    switch(iFileType)
    {
        case FILE_TYPE_TRC:
        {
            strcpy(szFileType,FILE_TRC_STR);
            break;
        }
        case FILE_TYPE_SVC:
        {
            strcpy(szFileType,FILE_SVC_STR);
            break;
        }
        case FILE_TYPE_ERR:
        {
            strcpy(szFileType,FILE_ERR_STR);
            break;
        }
        default:
        {
            strcpy(szFileType,FILE_ERR_STR);
        }
    }
}

void InitQueue(int iProcType)
{
    int iResult = 0;
    
    iResult = LogQInit(iProcType);

    if(iResult>0)
    {
        gQueueStat = Q_STAT_ENABLE;
    }
    else
    {
        gQueueStat = Q_STAT_DISABLE;
    }
}

void ResetQueue()
{
    int iResult = 0;
    
    iResult = LogQReSet();

    if(iResult>0)
    {
        gQueueStat = Q_STAT_ENABLE;
    }
    else
    {
        gQueueStat = Q_STAT_DISABLE;
    }
}

void ResetQueueTimer()
{
    if((time(NULL) - gQCheckTime) >= Q_CHECK_TIME)
    {
        ResetQueue();
        gQCheckTime = time(NULL);
    }
}

void MutexInit()
{
    int i = 0;
    
    for(i=0; i<FILE_TYPE_MAX; i++)
    {
        pthread_mutex_init(&g_FileMutex[i],NULL);
        pthread_mutex_init(&g_LocalFileMutex[i],NULL);
    }
}

void MutexDestroy()
{
    int i = 0;
    
    for(i=0; i<FILE_TYPE_MAX; i++)
    {
        pthread_mutex_destroy(&g_FileMutex[i]);
        pthread_mutex_destroy(&g_LocalFileMutex[i]);
    }
}

int FileOpen(int iType,char* szFileName)
{
    if(iType < 0 || iType > FILE_TYPE_MAX)
    {
        return false;
    }

    FileClose(iType);

    if( ( gFileFd[iType] = fopen( szFileName, "a+" ) ) == NULL )
    {
	    return false;
	}

	return true;
}

void FileClose(int iType)
{
    if(gFileFd[iType] != NULL)
    {
        fclose(gFileFd[iType]);
        gFileFd[iType] = NULL;
    }
}

int LocalFileOpen(int iType,char* szFileName)
{
    if(iType < 0 || iType > FILE_TYPE_MAX)
    {
        return false;
    }

    LocalFileClose(iType);

    if( ( gLocalFileFd[iType] = fopen( szFileName, "a+" ) ) == NULL )
    {
	    return false;
	}

	return true;
}

void LocalFileClose(int iType)
{
    if(gLocalFileFd[iType] != NULL)
    {
        fclose(gLocalFileFd[iType]);
        gLocalFileFd[iType] = NULL;
    }
}

void FileAllClose()
{
    int i = 0;
    for(i = 0; i < FILE_TYPE_MAX; i++)
    {
        FileClose(i);
        LocalFileClose(i);
    }
}

void GetLogTime(struct tm* ltime,char* szTime)
{
    memset(szTime,0x00,sizeof(szTime));
    sprintf(szTime,"%04d/%02d/%02d,%02d:%02d:%02d",ltime->tm_year+1900,ltime->tm_mon+1,ltime->tm_mday,ltime->tm_hour, ltime->tm_min, ltime->tm_sec);
}





















/************************************************************
	log.cfg 파일에서 LOG LEVEL 과 ON(0) OFF(1) 읽어오는 함수   
*************************************************************/
/*
int jloggerwriter_getloglevel(char *szProc, int length)
{
    char buf     [1024];
    char r_buf   [1024 + 1]={0};
   	char buffer	 [10 + 1]={0};

	char Log_Cfg [256 + 1]={0};

	int i,li;
    int l_cnt=0;
    FILE* fp = NULL;

	if(getenv("VAS_LOG_CONF") == NULL)
	{
		sprintf(Log_Cfg,"%s%s", "/data/sms/cfg", LOGCFG_NAME);
	} 
	else
	{
		sprintf(Log_Cfg,"%s%s", (char *)getenv("VAS_LOG_CONF"), LOGCFG_NAME);
	}
    
    fp = fopen(Log_Cfg,"r");
    
	if(fp == NULL)
	{
		//printf("log.cfg File Not Egist=[%s] ", Log_Cfg);
		return false;
	} 	
    
    while(fgets(buf, 1024, fp) != NULL)
    {
    	if(*(buf + 0)=='#' || *(buf + 0)==' ') 
    		continue;
    		
		for(i = 0; i < strlen(buf); i++)
		{
			if(*(buf+i) != '[') 
				continue;
				
        	memset(r_buf, 0, sizeof(r_buf));
            strcpy(r_buf, buf + i + 1);

            //각 프로세스별 level 값 구한다
            if(strncmp(r_buf, szProc, length) != 0) // PNAME
            	continue;
            	
            for(i = length + 1; i < strlen(r_buf); i++)
            {
                if((*(r_buf + i) == '\t') || (*(r_buf + i) == ' '))
                	continue;
               	
            	if(l_cnt == 0) // LOG=0-2
            	{
            		memset(buffer, 0, sizeof(buffer));
            		strncpy(buffer, r_buf + i, 1);
            		g_TrcLevel = atoi(buffer);
            	} else if(l_cnt == 1) { // TRACE=3
            		memset(buffer, 0, sizeof(buffer));
            		strncpy(buffer, r_buf + i, 1);
            		g_SvcLevel = atoi(buffer);
            	} else if(l_cnt == 2) { // ALM=4-7
            		memset(buffer, 0, sizeof(buffer));
            		strncpy(buffer, r_buf + i, 1);
            		g_ErrLevel = atoi(buffer);
            	} else if(l_cnt == 3) { // (all write) 1:(tra/alm write ) 2:(alm write) 3: OFF
            		memset(buffer, 0, sizeof(buffer));
            		strncpy(buffer, r_buf + i, 1);
            		g_LogStep = atoi(buffer);
            	} else if(l_cnt == 4) { // FORMATION(1:day 2:hour)
            		memset(buffer, 0, sizeof(buffer));
            		strncpy(buffer, r_buf + i, 1);
            		g_formation = atoi(buffer);
            	}
            	 
            	l_cnt++;
        }
        }
    	memset(buf, 0, 1024);
    }
    
    fclose(fp);
 
	return true;
}
*/

/* ADD MINJI 20090306 ALM LOG Level Division */
/*
void jloggerwriter_almlevel(int nLoggingLevel, char* almBuffer)
{
	static char *almlevels[] = { "", "INFO", "WARN", "ERR ", "CRIT" };
	int  index = nLoggingLevel - ERR_4 + 1;

	if( ( index < 0 ) || ( index > 4 ) ) index = 0;
	strcpy( almBuffer, almlevels[index] );
}
*/	
/* END MINJI */
