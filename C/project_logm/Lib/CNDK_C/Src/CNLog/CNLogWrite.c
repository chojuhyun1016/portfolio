#include <time.h>
#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <strings.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "CNLogApi.h"
#include "CNLogWrite.h"


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : int, int, char*, char*                                           *
*             _iType  : �α�Ÿ������ �α������� �ð������� �������� �ϴ�����   *
*                       ���� ������ ����                                       *
*             _iLevel : �α׷����� �α׵����͸� �󸶳� �������ϰ� �������� ����*
*             _cpPath : �α������� ���                                        *
*             _cpFirstName : �α����ϸ�                                             *
*                                                                              *
* Return    : CN_LOG_HANDLE, ����(CN_LOG_HANDLE), ����(NULL)                   *
* Stability : MT-Safe                                                          *
* Explain   : Heap �޸𸮸� �Ҵ��ؼ� �α׿� ���� �� �����͸� ����Ѵ�.         *
*             ���ڰ��� ����� �Ѿ�´ٸ� Ư���� ���� �� �κ��� ����.           *
*             Async-Safe �� �ƴϹǷ� �ñ׳ο� ���� �ؼ� ����϶�.              *
*             ���� �� ��� �α׿� ���õ� ������ ����ü�� ������(CN_LOG_HANDLE) *
*             �� ��ȯ�Ѵ�. �α׵����͸� �� ��� �� ����ü�� �����Ͱ� Key ����  *
*             �ǹǷ� ������ �� �ص״ٰ� �ٽ� ����϶�.                         *
*             �� ���ų� �����带 ���� �� ��� �޷θ����� �����ϱ� ���� ��      *
*             CNDeleteHandle() �Լ��� �޸𸮸� ���������.                     *
*******************************************************************************/
CN_LOG_HANDLE CNGetLogHandle( int _iType, int _iLevel, char* _cpPath, char* _cpFirstName, char* _cpLastName )
{
	int			iLen;

	CN_LOG_HANDLE	pHandle;

	if( _iLevel <= 0 )
		return	NULL;

	if( !_cpPath )
		return	NULL;

	if( !_cpFirstName )
		return	NULL;

	if( _iType < CN_LOG_HOUR_TYPE )
		return	NULL;

	if(  _iType > CN_LOG_DAY_TYPE )
		return	NULL;

	pHandle = (CN_LOG_HANDLE)malloc( sizeof( stLogHandle ) );

	if( !pHandle )
		return	NULL;	

	pHandle->iLogType = _iType;
	pHandle->iLogLevel = _iLevel;

	iLen = strlen( _cpPath );

	pHandle->cpLogPath = (char*)malloc( iLen + 1 );
	if( !pHandle->cpLogPath )
	{
		free( pHandle );

		return	NULL;
	}

	memset( pHandle->cpLogPath, 0x00, iLen + 1 );
	memcpy( pHandle->cpLogPath, _cpPath, iLen );

	iLen = strlen( _cpFirstName );

	pHandle->cpLogFirstName = (char*)malloc( iLen + 1 );
	if( !pHandle->cpLogFirstName )
	{
		free( pHandle->cpLogPath );
		free( pHandle );

		return	NULL;
	}

	memset( pHandle->cpLogFirstName, 0x00, iLen + 1 );
	memcpy( pHandle->cpLogFirstName, _cpFirstName, iLen );

	iLen = strlen( _cpLastName );

	pHandle->cpLogLastName = (char*)malloc( iLen + 1 );
	if( !pHandle->cpLogLastName )
	{
		free( pHandle->cpLogPath );
		free( pHandle->cpLogFirstName );
		free( pHandle );

		return	NULL;
	}

	memset( pHandle->cpLogLastName, 0x00, iLen + 1 );
	memcpy( pHandle->cpLogLastName, _cpLastName, iLen );

	return	pHandle;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : CN_LOG_HANDLE                                                    *
*             _stpHandle : �α׵����� ����ü�� ������                          *
*                                                                              *
* Return    : void                                                             *
* Stability : MT-Safe                                                          *
* Explain   : Heap ������ �Ҵ� �� ������(�α� ������ ����ü) �� �����Ѵ�.      *
*             Ư���� �̻� ���� �� �κ��� ����.                                 *
*******************************************************************************/
void CNDeleteHandle( CN_LOG_HANDLE _stpHandle )
{
	if( !_stpHandle )
		return;

	if( _stpHandle->cpLogPath )
		free( _stpHandle->cpLogPath );

	_stpHandle->cpLogPath = NULL;

	if( _stpHandle->cpLogFirstName )
		free( _stpHandle->cpLogFirstName );

	_stpHandle->cpLogFirstName = NULL;

	if( _stpHandle->cpLogLastName )
		free( _stpHandle->cpLogLastName );

	_stpHandle->cpLogLastName = NULL;

	free( _stpHandle );

	_stpHandle = NULL;

	return;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : CN_LOG_HANDLE, int, ��������(char* ...)                          *
*             _stpHandle : �α׵����� ����ü�� ������                          *
*             _iLevel    : �ش� �α׵������� ����, _stpHandle ����ü ����      *
*                          �������� Ŭ ��� �α׵����͸� ������� �ʴ´�.      *
*             _cpFormat  : �α� ���ڿ��� �������ڿ� �Բ� �Ѿ�´�.             *
*                                                                              *
* Return    : void                                                             *
* Stability : MT-Safe                                                          *
* Explain   : �α� ������ �����ϰ� ���Ͽ� ����ϴ� �Լ��̴�.                   *
*             �������ڸ� �޾Ƶ��̸� ���� ���ڿ� �տ� �ú���[hh:mm:ss]          * 
*             ���ڿ��� ���ؼ� ���Ͽ� ����Ѵ�.                                 *
*             ��� ���Ͽ� ���� ������ ���ڷ� ���� ������(_stpHandle)           *
*             �� ����Ǿ� ������ �� ������ �������� �α� �����͸� ����Ѵ�.    *
*             �α����� �̸� �����ο� ��ºη� ����������.                      *
*******************************************************************************/
void CNLOG( CN_LOG_HANDLE _stpHandle, int _iLevel, const char* _cpFormat, ... )
{
	char			szLog[CN_LOG_BUF_SIZE];
	char			szFile[CN_LOG_FILE_NAME_BUF_SIZE];

	struct timeval	stTvTime;
	struct tm		stTmTime;

	va_list			ntArgs;

	gettimeofday( &stTvTime, NULL );
	localtime_r( &stTvTime.tv_sec, &stTmTime );

	{
		sprintf( szLog, "[%02d:%02d:%02d] ", stTmTime.tm_hour, stTmTime.tm_min, stTmTime.tm_sec );

		va_start( ntArgs, _cpFormat );

		if( vsnprintf( szLog + 11, CN_LOG_BUF_SIZE - 11, _cpFormat, ntArgs ) < 0 )
		{
			fprintf( stderr, "vsnprintf() Error\n" );
			return;
		}

		va_end( ntArgs );
	}

	if( CNMakeFileName( szFile, CN_LOG_FILE_NAME_BUF_SIZE, _stpHandle, &stTmTime ) == CN_LOG_ERROR )
	{
		fprintf( stderr, "MakeFileName() Error\n" );
		return;
	}

	if( CNWriteLog( szFile, szLog ) == CN_LOG_ERROR )
	{
		fprintf( stderr, "WriteLog() Error\n" );
		return;
	}
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : CN_LOG_HANDLE, char*, struct tm*                                 *
*             _cpBuf     : ���ϸ��� �� Buffer                              *
*             _iBufSize  : ���ϸ� Buffer�� ũ��                                *
*             _stpHandle : �α׵����� ����ü�� ������                          *
*             _stTmTime  : ���� �ð��� ����ִ� ����ü�� ������                *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : �α׸� ��� �� ������ �̸��� �ϼ��Ѵ�.                           *
*             �α� ���� ����ü ������(CN_LOG_HANDLE _stpHandle)�� ����         *
*             �α����� ��� + ���� �̸� + ����ð� ���� ���� �̸��� �����Ѵ�.  *
*             �αױ���ü�� �α�Ÿ��(_stpHandle->iLogType) ������ ���� Ÿ����   *
*             �ΰ���(�Ϸα�, �ð��α�) ���� �̸����� �����ȴ�.                 *
*******************************************************************************/
static int CNMakeFileName( char* _cpBuf, int _iBufSize, CN_LOG_HANDLE _stpHandle, struct tm* _stTmTime )
{
	switch( _stpHandle->iLogType )
	{
		case CN_LOG_HOUR_TYPE :
			snprintf( _cpBuf, _iBufSize, "%s/%s_%04d%02d%02d%02d_%s.log", 
			_stpHandle->cpLogPath, _stpHandle->cpLogFirstName, _stTmTime->tm_year + 1900, _stTmTime->tm_mon + 1, _stTmTime->tm_mday, _stTmTime->tm_hour, _stpHandle->cpLogLastName );
			break;

		case CN_LOG_DAY_TYPE :
			snprintf( _cpBuf, _iBufSize, "%s/%s_%04d%02d%02d_%s.log", 
			_stpHandle->cpLogPath, _stpHandle->cpLogFirstName, _stTmTime->tm_year + 1900, _stTmTime->tm_mon + 1, _stTmTime->tm_mday, _stpHandle->cpLogLastName );
			break;

		default :
			snprintf( _cpBuf, _iBufSize, "%s/%s_%04d%02d%02d_%s.log", 
			_stpHandle->cpLogPath, _stpHandle->cpLogFirstName, _stTmTime->tm_year + 1900, _stTmTime->tm_mon + 1, _stTmTime->tm_mday, _stpHandle->cpLogLastName );
			break;
	}

	return	CN_LOG_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : char*, char*                                                     *
*             _cpFile : ��� �� ������ �̸�(���� ��� + ���� �̸�)���ڿ�       *
*             _cpBuf  : ���Ͽ� ��� �� ������ ���ڿ�                           *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : �ϼ� �� �α�����(char* _cpFile)�� �α׵�����(char* _cpBuf)       *
*             �� ����Ѵ�.                                                     *
*             ���� �� �������� ���� ������ ����ϱ����� ���� ��� �� ��ٷ�    *
*             �ݴ´�.                                                          *
*******************************************************************************/
static int CNWriteLog( char* _cpFile, char* _cpBuf )
{
	int	iFd;

	if( ( iFd = CNLogFileOpen( _cpFile, O_WRONLY | O_CREAT | O_APPEND  | O_NONBLOCK ) ) == CN_LOG_ERROR )
	{
		fprintf( stderr, "open() Error. Return:%d Errno:%d\n", iFd, errno );
		return	CN_LOG_ERROR;
	}

	write( iFd, _cpBuf, strlen( _cpBuf ) );

	CNLogFileClose( iFd );

	return	CN_LOG_SUCCESS;
}
