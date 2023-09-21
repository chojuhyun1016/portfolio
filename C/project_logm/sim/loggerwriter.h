/****************************************************************************
 * Author: O Jin Won
 * Email:  rookey@empal.com
 *         
 * Copyright 2002-2002, O Jin Won
 *
 * You may freely use or modify this code provided this
 * Copyright is included in all derived versions.
 *
 *
 * loggerwriter.h : header file
 *
 *
*/

#ifndef	__LOGGERWRITER_H__
#define	__LOGGERWRITER_H__

///////////////////////////////////////////////////////////////////////////

#ifdef __cplusplus
extern "C" {
#endif

///////////////////////////////////////////////////////////////////////////

#include <procfs.h>

#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/ipc.h>
#include <unistd.h>
#include <time.h>
#include <pwd.h>

#include <stdlib.h>
#include <dirent.h>
#include <errno.h>

/* ---------------------------------------------------- */
/* LOG LEVEL  EQUAL ERROR LEVEL                         */
/* TRACE  : Between 0 and 2 							*/
/* SERVICE: Between 3 and 5                             */
/* ERROR  : Between 6 and 8								*/
/* ---------------------------------------------------- */
#define	TRC_0	    0
#define	TRC_1	    1
#define	TRC_2	    2
#define	SVC_0	    3
#define	SVC_1	    4
#define	SVC_2	    5
#define	ERR_0	    6
#define	ERR_1	    7
#define	ERR_2	    8
#define MAX_LOG_INPUT_LEVEL 9

#define FILE_TYPE_TRC                0
#define FILE_TYPE_SVC                1
#define FILE_TYPE_ERR                2
#define FILE_TYPE_MAX                3

/* ---------------------------------------------------- */
/*	Trivial Definitions									*/
/* ---------------------------------------------------- */
/*#define	MAX_HEADER_SIZE				30*/
#define	MAX_LOG_FILE_NAME			256
#define	MAX_LOG_DIRECTORY_NAME		256
#define	MAX_WRITE_BUFFER			1024
#define MAX_ALARM_BUFFER            2048
/* ---------------------------------------------------- */
/*	RETURN VALUE 										*/
/* ---------------------------------------------------- */
#ifndef  true
#define true	1
#endif
#ifndef  false
#define false	0
#endif

// hskim: 비정상 종료시 콜스택(callstack) 출력 용도 ------------
//extern int nLogFD; 
// -------------------------------------------------------------

int jloggerwriter_construct(char* localdir, char* nfsdir, char* process, int center_delimeter, char* version, char* keyname, char* service_dir, char* trace_dir, char* error_dir,int iProcType,char* szCfgDir);
int jloggerwriter_construct_ex(char* localdir, char* nfsdir,char* pcDirName, char* process,int center_delimeter, char* version, char* keyname, char* service_dir,char* trace_dir, char* error_dir,int iPrcType,char* szCfgDir);
int jloggerwriter_construct_ex2(char* localdir, char* nfsdir, char* process,int center_delimeter, char* version, char* keyname, char* service_dir,char* trace_dir, char* error_dir,int iPrcType,char* szCfgDir);
void jloggerwriter_update();
int jloggerwriter_destruct();
void LOG(int nLoggingLevel,char* szDestNum, const char *strFormat, ...);
int GET_LOG_FD(int iFileType);
int jloggerwriter_getprocname(char* szProc); //GetProcName_S
void jloggerwriter_write(int iLogType, char* path, char* strFormat, char* szDirType,char* szDestNum);
int gethostnumber (char *host_nm);
void sig_handler1(int );

///////////////////////////////////////////////////////////////////////////

#ifdef __cplusplus
}
#endif

///////////////////////////////////////////////////////////////////////////

#endif /* __LOGGERWRITER_H__ */













/*
// ADD MINJI 2009.04.07
void jloggerwriter_almlevel(int nLoggingLevel, char* almBuffer);
// END MINJI
int jloggerwriter_getloglevel(char* szProc, int length);
void jloggerwriter_setpattern(const char* pattern);
*/
