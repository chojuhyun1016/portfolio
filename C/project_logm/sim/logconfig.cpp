#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "logglobal.h"
#include "logconfig.h"
#include "loggerwriter.h"

#define MAX_CFG_LINE                  1024
#define FIND_MODE_TITLE                  1
#define FIND_MODE_VALUE                  2

static char gCfgDir[MAX_CFG_LINE] = {0,};

/*PRIVATE FUNCTION*/
int GetTitle(char* cpBuf,char* pTitle);
int GetValue(char* cpBuf,char* pKey,char* pValue);

int SetCfgDir(char* szCfgDir)
{
    int iLen = 0;
    
    iLen = strlen(szCfgDir);
    
    if(iLen < 0 || iLen > MAX_CFG_LINE)
    {
        return -1;
    }
    
    strcpy(gCfgDir,szCfgDir);
}

int GetLogQMode()
{
    int iRtn = 0;
    char strQueueMode[STR_TEMP_LEN_5] = {0,};

    memset(strQueueMode,0x00,STR_TEMP_LEN_5);
    
    if(GetConfig(LOG_CFG_FILE_NAME,"COMMON","LOG_QUEUE_MODE",strQueueMode)<1)
	{
	    fprintf( stderr, "ERROR_GET_QUEUE_MODE [Default : On] [L:%d]\n", __LINE__ );
		return LOG_Q_MODE_ON;                                                   /*Default Mode : On*/
	}
	
    if(!strcmp(strQueueMode,"ON"))
    {
        iRtn = LOG_Q_MODE_ON;
    }
    else if(!strcmp(strQueueMode,"OFF"))
    {
        iRtn = LOG_Q_MODE_OFF;
    }
    else
    {
        fprintf( stderr, "ERROR_GET_QUEUE_MODE [Default : On] [L:%d]\n", __LINE__ );
        iRtn = LOG_Q_MODE_ON;                                                    /*Default Mode : On*/
    }
    
    return iRtn;
}

int GetLogLevel(int iFileType,char* pProcName)
{
    int iTemp = 0;
    char szFileType[STR_TEMP_LEN_10] = {0,};
    char szLogLevel[STR_TEMP_LEN_5] = {0,};

    memset(szFileType,0x00,STR_TEMP_LEN_10);
    memset(szLogLevel,0x00,STR_TEMP_LEN_5);
    
    switch(iFileType)
    {
        case FILE_TYPE_TRC:
        strcpy(szFileType,"TRC_LOG");
        break;
        
        case FILE_TYPE_ERR:
        strcpy(szFileType,"ERR_LOG");
        break;
        
        case FILE_TYPE_SVC:
        strcpy(szFileType,"SVC_LOG");
        break;
        
        default:
        return LOG_LEVEL_3;
    }
    
    if(GetConfig(LOG_CFG_FILE_NAME,pProcName,szFileType,szLogLevel)<1)
	{
	    fprintf( stderr, "ERR_GET_LOG_LEVEL [Default : LOG_LEVEL_3] [L:%d]\n", __LINE__);
	    
		return LOG_LEVEL_3;
	}
	
	iTemp = atoi(szLogLevel);
	
	if(iTemp < 0 || iTemp >= MAX_LOG_LEVEL)
	{
	    return LOG_LEVEL_3;
	}
	
    return iTemp;
}

int GetLogStep(char* pProcName)
{
    int iTemp = 0;

    char szLogStep[STR_TEMP_LEN_5] = {0,};

    memset(szLogStep,0x00,STR_TEMP_LEN_5);
 
    if(GetConfig(LOG_CFG_FILE_NAME,pProcName,"LOG_STEP",szLogStep)<1)
	{
	    fprintf( stderr, "ERR_GET_LOG_STEP [Default : 0] [L:%d]\n", __LINE__);
	    
		return LOG_STEP_ALL;
	}
	
	iTemp = atoi(szLogStep);
	
	if(iTemp < 0 || iTemp >= MAX_LOG_STEP)
	{
	    return LOG_STEP_ALL;
	}
	
    return iTemp;
}

int GetLogFormat(char* pProcName)
{
    
    int iTemp = 0;

    char szLogFormat[STR_TEMP_LEN_5] = {0,};

    memset(szLogFormat,0x00,STR_TEMP_LEN_5);
 
    if(GetConfig(LOG_CFG_FILE_NAME,pProcName,"LOG_FORMAT",szLogFormat)<1)
	{
	    fprintf( stderr, "ERR_GET_LOG_FORMAT [Default : LOG_FORMAT_DAY] [L:%d]\n", __LINE__);
	    
		return LOG_FORMAT_DAY;
	}
	
	iTemp = atoi(szLogFormat);
	
	if(iTemp < 0 || iTemp >= MAX_LOG_FORMAT)
	{
	    return LOG_FORMAT_DAY;
	}
	
    return iTemp;

}

int GetSmartMode(int iFileType,char* pProcName)
{
    int iTemp = 0;
    char szFileType[STR_TEMP_LEN_10] = {0,};
    char szSmartMode[STR_TEMP_LEN_5] = {0,};

    memset(szFileType,0x00,STR_TEMP_LEN_10);
    memset(szSmartMode,0x00,STR_TEMP_LEN_5);
    
    switch(iFileType)
    {
        case FILE_TYPE_TRC:
        strcpy(szFileType,"TRC_SMART");
        break;
        
        case FILE_TYPE_ERR:
        strcpy(szFileType,"ERR_SMART");
        break;
        
        case FILE_TYPE_SVC:
        strcpy(szFileType,"SVC_SMART");
        break;
        
        default:
        return SMRAT_MODE_OFF;
    }
    
    if(GetConfig(LOG_CFG_FILE_NAME,pProcName,szFileType,szSmartMode)<1)
	{
	    fprintf( stderr, "ERR_GET_SMART_MODE [Default : SMRAT_MODE_OFF] [L:%d]\n", __LINE__);
	    
		return SMRAT_MODE_OFF;
	}
	
	iTemp = atoi(szSmartMode);
	
	if(iTemp < 0 || iTemp >= MAX_LOG_LEVEL)
	{
	    return SMRAT_MODE_OFF;
	}
	
    return iTemp;
}

int GetConfig(char* pFileName,char* pTitle,char* pKey, char* pValue)
{
    char cpBuf[MAX_CFG_LINE] = {0,};
    char szCfgFileName[MAX_CFG_LINE] = {0,};
    int iFindMode = FIND_MODE_TITLE;
    int iResult = 0;
    int iRtn = 0;
    
	FILE *fp;

    if( ( strlen( pTitle ) < 1 ) )
	{
		return ERR_TITLE_LEN;
    }
    
	if( ( strlen( pKey ) < 1 ) )
	{
		return ERR_KEY_LEN;
    }
    
    sprintf(szCfgFileName,"%s/%s",gCfgDir,pFileName);
    
	fp = fopen(szCfgFileName, "r" );
	
	if( fp == NULL )
	{
	    fprintf( stderr, "ERROR_FOPEN_FAIL [L:%d]\n", __LINE__ );
		return ERR_FILE_OPEN;
	}

    while(fgets(cpBuf, MAX_CFG_LINE, fp)!= 0)
    {
        if(iFindMode == FIND_MODE_TITLE)
        {
            iResult = GetTitle(cpBuf,pTitle);
            
            if(iResult == SUCCESS)
            {
                iFindMode = FIND_MODE_VALUE;
                continue;
            }
            else
            {
                continue;
            }
        }
        else
        {
            iResult = GetValue(cpBuf,pKey,pValue);
            
            if(iResult == SUCCESS)
            {
                fclose(fp);
                return SUCCESS;
            }
            else if(iResult == RTN_END_LINE)
            {
                fclose(fp);
                return  ERR_EMPTY_VALUE;
            }
            else
            {
                continue;
            }
        }
    }

    fclose(fp);

    return ERR_EMPTY_VALUE;
}

int GetTitle(char* cpBuf,char* pTitle)
{
    int  iRtn = 0;
    int  iFlag = 0;
    int  leng = 0;
    int iVauleCnt = 0;
    char FindTitle[MAX_CFG_LINE];

    leng	=	strlen( cpBuf );

    if(leng < 1)
    {
        return ERR_EMPTY_VALUE;
    }
    

    for(int i=0; i < leng; i++ )
    {
        if( cpBuf[i] == '#' || cpBuf[i] =='\n' || cpBuf[i] == 0x0D)
	    {
            break;
	    }

	    if((cpBuf[i] == ' ' )  ||  ( cpBuf[i] == '\t'))
	    {
	        continue;
	    }
	    else if(cpBuf[i] == '[')
	    {
            iFlag = 1;
	    }
	    else if(iFlag == 1 && cpBuf[i] == ']')
	    {
	        break;
	    }
	    else
	    {
	        if(iFlag)
	        {
	            FindTitle[iVauleCnt] = cpBuf[i];
	            iVauleCnt++;
	        }
	    }
    }
    
    FindTitle[iVauleCnt] = 0x00;
    
    if(!strcmp(pTitle,FindTitle))
    {
        iRtn = SUCCESS;
    }
    else
    {
        iRtn = ERR_EMPTY_VALUE;
    }
    
    return iRtn;
}

int GetValue(char* cpBuf,char* pKey,char* pValue)
{
    int iRtn = 0;
    int iFlag = 0;
    int iKeyCnt = 0;
    int iVauleCnt = 0;
    int  leng = 0;
    
    char FindKey[MAX_CFG_LINE];
	char FindValue[MAX_CFG_LINE];

    leng	=	strlen( cpBuf );

    if(leng < 1)
    {
        return ERR_EMPTY_VALUE;
    }
    
    for(int loop=0; loop < leng; loop++ )
    {
        if( cpBuf[loop] == '#' || cpBuf[loop] =='\n' || cpBuf[loop] == 0x0D )
	    {
            break;
	    }

	    if((cpBuf[loop] == ' ' )  ||  ( cpBuf[loop] == '\t'))
	    {
	        continue;
	    }
	    else if(cpBuf[loop] == '=')
	    {
            iFlag = 1;
	    }
	    else
	    {
	        if(iFlag)
	        {
	            FindValue[iVauleCnt] = cpBuf[loop];
	            iVauleCnt++;
	        }
	        else
	        {
	            FindKey[iKeyCnt] = cpBuf[loop];
	            iKeyCnt++;
	        }
	    }
    }

    FindValue[iVauleCnt] = 0x00;
    FindKey[iKeyCnt] = 0x00;

    if(!strcmp(pKey,FindKey))
    {
        strcpy(pValue,FindValue);;
        iRtn =  SUCCESS;
    }
    else
    {
        if(FindKey[0] == '[')
        {
            iRtn = RTN_END_LINE;
        }
        else
        {
            iRtn = RTN_NEXT_LINE_FIND;
        }
    }
    
    return iRtn;
}
