#include <stdio.h>
#include <errno.h>
#include <unistd.h>
#include <stdlib.h>
#include <strings.h>

#include <fCDCtl.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "CDCConfigApi.h"
#include "CDCConfigRead.h"
#include "CDCConfigDefinitions.h"

CDCConfig::CDCFile::CDCFile()
{
	m_cpFile	= NULL;
}


CDCConfig::CDCFile::CDCFile( char* _cpFileName )
{
	m_cpFile	= NULL;

	if( _cpFileName == NULL )
		return;

	Set( _cpFileName );
}


CDCConfig::CDCFile::CDCFile( char* _cpFileName, char* _cpFilePath )
{
	m_cpFile	= NULL;

	if( _cpFileName == NULL || _cpFilePath == NULL )
		return;

	Set( _cpFileName, _cpFilePath );
}


void CDCConfig::CDCFile::Set( char* _cpFileName )
{
	if( m_cpFile )
	{
		delete [] m_cpFile;

		m_cpFile	= NULL;
	}

	if( _cpFileName == NULL )
		return;

	m_cpFile = new ::strlen( _cpFileName ) + 1;

	::strcpy( m_cpFile, _cpFileName );
}


void CDCConfig::CDCFile::Set( char* _cpFileName, char* _cpFilePath )
{
	if( m_cpFile )
	{
		delete [] m_cpFile;

		m_cpFile	= NULL;
	}

	if( _cpFileName == NULL || _cpFilePath == NULL )
		return;

	m_cpFile = new ::strlen( _cpFileName ) + ::strlen( _cpFilePath ) + 2;

	::sprintf( m_cpFile, "%s/%s", _cpFilePath, _cpFileName );
}


CDCConfig::CDCFile::~CDCFile()
{
	if( m_cpFile )
		delete [] m_cpFile;
}


const char* CDCConfig::CDCFile::GetFileName()
{
	return	m_cpFile;
}


int GetInt( char* _cpPart, char* _cpSPart, int* _ipBuf )
{
	int		iResult		= 0;

	char	caBuffer[CDC_CONFIG_BUF_SIZE]	= { 0x00, };

	if( !m_cpFile )
		return	CDC_CONFIG_NO_FORMAT;

	if( !_cpFile[0] )
		return	CDC_CONFIG_NO_FORMAT;

	if( !_cpPart )
		return	CDC_CONFIG_NO_FORMAT;

	if( !_cpPart[0] )
		return	CDC_CONFIG_NO_FORMAT;

	if( !_cpSPart )
		return	CDC_CONFIG_NO_FORMAT;

	if( !_cpSPart[0] )
		return	CDC_CONFIG_NO_FORMAT;

	iResult = CDCReadConfig( m_cpFile, _cpPart, _cpSPart, caBuffer, CDC_CONFIG_BUF_SIZE );

	if( iResult != CDC_CONFIG_SUCCESS )
		return	iResult;

	if( ( *_ipBuf = CDCConfigStrToDec( caBuffer ) ) < 0 )
		return	*_ipBuf;

	return	CDC_CONFIG_SUCCESS;
}


int GetStr( char* _cpPart, char* _cpSPart, char* _cpBuf, int _iSize )
{
	int		iResult	= 0;

	if( !m_cpFile )
		return	CDC_CONFIG_NO_FORMAT;

	if( !_cpFile[0] )
		return	CDC_CONFIG_NO_FORMAT;

	if( !_cpPart )
		return	CDC_CONFIG_NO_FORMAT;

	if( !_cpPart[0] )
		return	CDC_CONFIG_NO_FORMAT;

	if( !_cpSPart )
		return	CDC_CONFIG_NO_FORMAT;

	if( !_cpSPart[0] )
		return	CDC_CONFIG_NO_FORMAT;

	if( !_cpBuf )
		return	CDC_CONFIG_NO_FORMAT;

	if( _iSize <= 0 )
		return	CDC_CONFIG_NO_FORMAT;

    iResult = CDCReadConfig( m_cpFile, _cpPart, _cpSPart, _cpBuf, _iSize );

	if( iResult != CDC_CONFIG_SUCCESS )
		return	iResult;

	return	CDC_CONFIG_SUCCESS;
}


static int CDCReadConfig( char* _cpFile, char* _cpPart, char* _cpSPart, char* _cpBuf, int _iSize )
{
	int		iFd;
	int		iResult;

	char	*cpPtr;
	char	caBuffer[CDC_CONFIG_BUF_SIZE];
	char	caToken[CDC_CONFIG_TOKEN_SIZE];
	char	caPart[CDC_CONFIG_TOKEN_SIZE];

	snprintf( caPart, CDC_CONFIG_TOKEN_SIZE, "[%s]", _cpPart );

	while( ( iFd = ::open( _cpFile, O_RDONLY ) ) == -1 && errno == EINTR );

	if( iFd == -1 )
		return	CDC_CONFIG_NO_FILE;

	while( 1 )
	{
		iResult = CDCConfigReadLine( iFd, caBuffer, CDC_CONFIG_BUF_SIZE );

		if( iResult <= 0 )
		{
			while( ::close( iFd ) == -1 && errno == EINTR );

			return	CDC_CONFIG_NO_PART;
		}

		cpPtr =  CDCConfigMakeToken( caBuffer, caToken, CDC_CONFIG_TOKEN_SIZE );

		if( !caToken[0] )
			continue;

		if( strncmp( caToken, caPart, CDC_CONFIG_TOKEN_SIZE ) == 0 )
			break;
	}

	while( 1 )
	{
		iResult = CDCConfigReadLine( iFd, caBuffer, CDC_CONFIG_BUF_SIZE );

		if( iResult <= 0 )
			break;

		cpPtr = CDCConfigMakeToken( caBuffer, caToken, CDC_CONFIG_TOKEN_SIZE );

		if( !caToken[0] )
			continue;

		if( ( caToken[0] == '[' ) && ( caToken[strlen( caToken ) -1] == ']' ) ) 
			return	CDC_CONFIG_NO_SPART;

		if( strncmp( caToken, _cpSPart, CDC_CONFIG_TOKEN_SIZE ) == 0 )
		{
			cpPtr = CDCConfigMakeToken( cpPtr, _cpBuf, _iSize );

			while( ::close( iFd ) == -1 && errno == EINTR );

			return	CDC_CONFIG_SUCCESS;
		}
	}

	while( ::close( iFd ) == -1 && errno == EINTR );

	return	CDC_CONFIG_NO_SPART;
}


static char* CDCConfigMakeToken( char* _cpOffset, char* _cpBuf, int _iSize )
{
	int	iLen;
	int	iLoop;
	int	iArray	= 0;

	iLen	=	strlen( _cpOffset );

	for( iLoop = 0; iLoop < iLen; iLoop++ )
	{
		if( _cpOffset[iLoop] == '#' )
		{
			_cpBuf[0] = '\0';

			return	&_cpOffset[iLen];
		}

		if( ( _cpOffset[iLoop] == ' ' )  ||  ( _cpOffset[iLoop] == '\t' ) ||  
			( _cpOffset[iLoop] == '=' )  ||  ( _cpOffset[iLoop] == '\n' ) ||
			( _cpOffset[iLoop] == 0x0D ) )
			continue;
		
		for( ; iLoop < iLen, iArray < _iSize - 1; iLoop++ )
		{
			if( ( _cpOffset[iLoop] == ' ' )  ||  ( _cpOffset[iLoop] == '\t' ) ||  
				( _cpOffset[iLoop] == '=' )  ||  ( _cpOffset[iLoop] == '\n' ) ||
				( _cpOffset[iLoop] == 0x0D ) )
			{
				_cpBuf[iArray]	= '\0';

				return	&_cpOffset[iLoop];
			}

			_cpBuf[iArray++] = _cpOffset[iLoop];
		}
	}

	_cpBuf[iArray] ='\0';

	return	&_cpOffset[iLoop];
}


static int CDCConfigStrToDec( char* _cpStr )
{
	int		iLen;

	iLen = strlen( _cpStr );

	if( iLen > 2 )
	{
		if( strncmp( _cpStr,"0x",2 ) == 0 )
			return	CDCConfigHexConverter( _cpStr + 2 );
		else
			return	CDCConfigDecConverter( _cpStr );
	}
	else
	{
		return	CDCConfigDecConverter( _cpStr );
	}
}


static int CDCConfigHexConverter( char* _cpStr )
{
	int		iLen;
	int		iLoop;

	int		iResult		= 0;
	int		iMultiple	= 1;

	iLen = strlen( _cpStr );

	for( iLoop = iLen - 1; iLoop >= 0; iLoop-- )
	{
		if( _cpStr[ iLoop ] >= '0' && _cpStr[ iLoop ] <= '9' )
			iResult += ( ( _cpStr[iLoop] - '0' ) * iMultiple );
		else if( _cpStr[iLoop] >= 'A' && _cpStr[iLoop] <= 'F' )
			iResult += ( ( _cpStr[iLoop] - 'A' + 10 ) * iMultiple );
		else if( _cpStr[iLoop] >= 'a' && _cpStr[iLoop] <= 'f' )
			iResult += ( ( _cpStr[iLoop] - 'a' + 10 ) * iMultiple );
		else
			return	CDC_CONFIG_NO_DECIMAL;

		iMultiple *= 16;
	}

	return	iResult;
}


static int CDCConfigDecConverter( char* _cpStr )
{
	int		iLen;
	int		iLoop;

	int		iResult		= 0;
	int		iMultiple	= 1;

	iLen = strlen( _cpStr );

	for( iLoop = iLen - 1; iLoop >= 0; iLoop-- )
	{
		if( _cpStr[ iLoop ] >= '0' && _cpStr[ iLoop ] <= '9' )
			iResult += ( ( _cpStr[iLoop] - '0' ) * iMultiple );
		else
			return	CDC_CONFIG_NO_DECIMAL;

		iMultiple *= 10;
	}

	return	iResult;
}


static int CDCConfigReadLine( int _iFd, char* _cpBuf, int _iSize )
{
	int iResult;
	int iReadByte = 0;

	while( iReadByte < _iSize - 1 )
	{
		iResult = read( _iFd, _cpBuf + iReadByte, 1 );

		if( ( iResult == CDC_CONFIG_ERROR ) && ( errno == EINTR ) )
			continue;
		
		if( ( iResult == 0 ) && ( iReadByte == 0 ) )
			return	0;
		
		if( iResult == 0 )
			break;

		if( iResult == CDC_CONFIG_ERROR )
			return	CDC_CONFIG_ERROR;
		
		iReadByte++;
		
		if( _cpBuf[iReadByte - 1] == '\n' )
			break;
	}

	_cpBuf[iReadByte] = '\0';

	return	iReadByte;	
}

