#include "CDConfigApi.h"
#include "CDConfigRead.h"

#ifdef  _SOLARIS_
    #include <stdio.h>
    #include <fcntl.h>
    #include <errno.h>
    #include <unistd.h>
    #include <strings.h>
#elif _CENT_OS_
    #include <stdio.h>
    #include <fcntl.h>
    #include <errno.h>
    #include <unistd.h>
    #include <string.h>
#else
    #include <stdio.h>
    #include <fcntl.h>
    #include <errno.h>
    #include <unistd.h>
    #include <strings.h>
#endif


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : char*, char*, char*                                              *
*             _cpFile  : 컨피그 파일명(파일경로 + 파일이름)                    *
*             _cpPart  : 컨피그 파일에서 찾고자 하는 대항목                    *
*             _cpSPart : 컨피그 파일에서 찾고자 하는 소항목                    *
*             _ipBuf   : 읽어들인 데이터를 저장 할 버퍼                        *
*                                                                              *
* Return    : int, 성공(읽어들인 Decimal), 실패(-1)                            *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : Config 파일(_cpFile) 에서 대분류(_cpPart)의 소분류(_cpSPart)의   *
*             설정 값(int형) 을 반환한다. 실패 혹은 에러 일 경우               *
*             -1을 반환(CD_CONFIG_ERROR)한다.                                  *
*             대분류(_cpPart) 소분류(_cpSPart)는 최대 길이 1024(1023+NULL)까지 *
*             이다. 그 이상일 경우 문제가 된다. 물론 컬럼 네임을 1024를 넘게   *
*             하는넘은 미친놈이 틀림없다. Config 파일 이름(_cpFile) 또한       *
*             1024가 최대 사이즈이다. 1024이상의 길이가 들어오거나 문자열 끝에 *
*             NULL 이없는 데이터가 들어올 경우 메모리 침범(segment fault)가    *
*             일어날수 있다.                                                   *
*******************************************************************************/
int CDConfigGetInt( char* _cpFile, char* _cpPart, char* _cpSPart, int* _ipBuf )
{
	int		iResult		= 0;

	char	caBuffer[CD_CONFIG_BUF_SIZE]	= { 0x00, };

	if( !_cpFile )
		return	CD_CONFIG_NO_FORMAT;

	if( !_cpFile[0] )
		return	CD_CONFIG_NO_FORMAT;

	if( !_cpPart )
		return	CD_CONFIG_NO_FORMAT;

	if( !_cpPart[0] )
		return	CD_CONFIG_NO_FORMAT;

	if( !_cpSPart )
		return	CD_CONFIG_NO_FORMAT;

	if( !_cpSPart[0] )
		return	CD_CONFIG_NO_FORMAT;

	iResult = CDConfigRead( _cpFile, _cpPart, _cpSPart, caBuffer, CD_CONFIG_BUF_SIZE );

	if( iResult != CD_CONFIG_SUCCESS )
		return	iResult;

	if( ( *_ipBuf = CDConfigStrToDec( caBuffer ) ) < 0 )
		return	*_ipBuf;

	return	CD_CONFIG_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : char*, char*, char*, char*                                       *
*             _cpFile  : 컨피그 파일명(파일경로 + 파일이름)                    *
*             _cpPart  : 컨피그 파일에서 찾고자 하는 대항목                    *
*             _cpSPart : 컨피그 파일에서 찾고자 하는 소항목                    *
*             _cpBuf   : 읽어들인 데이터를 저장 할 버퍼                        *
*             _iSize   : _cpBuf(버퍼)의 크기(읽어들일 최대길이)                *
*                                                                              *
* Return    : char*, 성공(&_cpBuf[0]), 실패(NULL)                              *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : Config 파일(_cpFile) 에서 대분류(_cpPart)의 소분류(_cpSPart)의   *
*             설정 값(문자열) 을 버퍼(_cpBuf)에 저장하고 성공 시에는           *
*             0(CD_CONFIG_SUCCESS)을 반환하고 버퍼(_cpBuf)에 문자열을          *
*             저장하며 실패 혹은 에러인 경우 -1(CD_CONFIG_ERROR)을             *
*             반환한다.                                                        *
*             GetConfigInt와 달리 Config 파일에서 문자열을 가져 오므로         *
*             문자열을 저장 할 버퍼(_cpBuf)의 크기(_iSize)를 인자로 받는다.    *
*             Config 파일에서 추출해올 데이터의 길이가 버퍼보다 클 경우        *
*             버퍼의 크기-1(마지막은 NULL을 넣는다) 만큼 버퍼에 저장한다.      *
*             데이터가 버퍼보다 커도 실패를 반환하지 않는다. 성공을 반환하고   *
*             버퍼 크기만큼 저장한다.                                          *
*             대분류(_cpPart) 소분류(_cpSPart)는 최대 길이 1024(1023+NULL)까지 *
*             이다. 그 이상일 경우 문제가 된다. 물론 컬럼 네임을 1024를 넘게   *
*             하는넘은 미친놈이 틀림없다. Config 파일 이름(_cpFile) 또한       *
*             1024가 최대 사이즈이다. 1024이상의 길이가 들어오거나 문자열 끝에 *
*             NULL 이없는 데이터가 들어올 경우 메모리 침범(segment fault)가    *
*             일어날수 있다.                                                   *
*******************************************************************************/
int CDConfigGetStr( char* _cpFile, char* _cpPart, char* _cpSPart, char* _cpBuf, int _iSize )
{
	int		iResult	= 0;

	if( !_cpFile )
		return	CD_CONFIG_NO_FORMAT;

	if( !_cpFile[0] )
		return	CD_CONFIG_NO_FORMAT;

	if( !_cpPart )
		return	CD_CONFIG_NO_FORMAT;

	if( !_cpPart[0] )
		return	CD_CONFIG_NO_FORMAT;

	if( !_cpSPart )
		return	CD_CONFIG_NO_FORMAT;

	if( !_cpSPart[0] )
		return	CD_CONFIG_NO_FORMAT;

	if( !_cpBuf )
		return	CD_CONFIG_NO_FORMAT;

	if( _iSize <= 0 )
		return	CD_CONFIG_NO_FORMAT;

    iResult = CDConfigRead( _cpFile, _cpPart, _cpSPart, _cpBuf, _iSize );

	if( iResult != CD_CONFIG_SUCCESS )
		return	iResult;

	return	CD_CONFIG_SUCCESS;
}


/*******************************************************************************
* Update    : 2012/05/09                                                       *
* Argument  : char*, char*, char*, char*                                       *
*             _cpFile  : 컨피그 파일명(파일경로 + 파일이름)                    *
*             _cpPart  : 컨피그 파일에서 찾고자 하는 대항목                    *
*             _cpSPart : 컨피그 파일에서 찾고자 하는 소항목                    *
*             _cpBuf   : 읽어들인 데이터를 저장 할 버퍼                        *
*             _iSize   : _cpBuf(버퍼)의 크기(읽어들일 최대길이)                *
*                                                                              *
* Return    : int, 성공(1), 실패(-1)                                           *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : Config 파일(_cpFile)에서 대분류(_cpPart)의 소분류(_cpSPart)의    *
*             설정 값(문자열) 을 버퍼(_cpBuf)에 저장하고 성공 시 0을 반환하고  *
*             실패시 -1을 반환한다.                                            *
*             주의 : Config 파일에서 대항목(_cpPart)과 소항목(_cpSPart)이 같은 *
*             라인에 있을경우 찾지 못한다.                                     *
*             Ex ) [MAIN] VALUE01 = 10                                         *
*******************************************************************************/
static int CDConfigRead( char* _cpFile, char* _cpPart, char* _cpSPart, char* _cpBuf, int _iSize )
{
	int		iFd;
	int		iResult;

	char	*cpPtr;
	char	caBuffer[CD_CONFIG_BUF_SIZE];
	char	caToken[CD_CONFIG_TOKEN_SIZE];
	char	caPart[CD_CONFIG_TOKEN_SIZE];

	snprintf( caPart, CD_CONFIG_TOKEN_SIZE, "[%s]", _cpPart );

	if( ( iFd = CDConfigOpen( _cpFile, O_RDONLY ) ) == CD_CONFIG_ERROR )
		return	CD_CONFIG_NO_FILE;

	while( 1 )
	{
		iResult = CDConfigReadLine( iFd, caBuffer, CD_CONFIG_BUF_SIZE );

		if( iResult <= 0 )
		{
			CDConfigClose( iFd );

			return	CD_CONFIG_NO_PART;
		}

		cpPtr =  CDConfigMakeToken( caBuffer, caToken, CD_CONFIG_TOKEN_SIZE );

		if( !caToken[0] )
			continue;

		if( strncmp( caToken, caPart, CD_CONFIG_TOKEN_SIZE ) == 0 )
			break;
	}

	while( 1 )
	{
		iResult = CDConfigReadLine( iFd, caBuffer, CD_CONFIG_BUF_SIZE );

		if( iResult <= 0 )
			break;

		cpPtr = CDConfigMakeToken( caBuffer, caToken, CD_CONFIG_TOKEN_SIZE );

		if( !caToken[0] )
			continue;

		if( ( caToken[0] == '[' ) && ( caToken[strlen( caToken ) -1] == ']' ) )
		{
			CDConfigClose( iFd );

			return	CD_CONFIG_NO_SPART;
		}

		if( strncmp( caToken, _cpSPart, CD_CONFIG_TOKEN_SIZE ) == 0 )
		{
			cpPtr = CDConfigMakeToken( cpPtr, _cpBuf, _iSize );

			CDConfigClose( iFd );

			return	CD_CONFIG_SUCCESS;
		}
	}

	CDConfigClose( iFd );

	return	CD_CONFIG_NO_SPART;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : char*, char*                                                     *
*             _cpOffset : 읽어들일 데이터 버퍼                                 *
*             _cpBuf    : 버퍼에서 읽은 데이터를 담아갈 버퍼                   *
*             _iSize    : cpBuf에 저장할 데이터의 최대 길이                    *
*                                                                              *
* Return    : char*, 성공(읽어들인 마지막 문자의 주소), 실패(실패는 없다)      *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : Config파일 에서 읽은 한줄의 문자열(_cpOffset)에서 하나의 단어를  *
*             추출하여 버퍼(_cpBuf) 에 저장한 후 반환한다. 반환 값은           *
*             문자열(_cpOffset)에서 마지막에 읽어들인 문자의                   *
*             주소(_cpOffset[iLoop])를 반환한다. 실패는 없다. 실패 일 경우     *
*             읽어들인 곳까지의 주소(_cpOffset[iLoop])가 반환되며 버퍼(_cpBuf) *
*             에는 아무 데이터도 들어가지 않는다.(_cpBuf[0] 번지에 NULL 삽입   *
*******************************************************************************/
static char* CDConfigMakeToken( char* _cpOffset, char* _cpBuf, int _iSize )
{
	int	iLen;
	int	iLoop;
	int	iArray	= 0;

	iLen	= strlen( _cpOffset );

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


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : char*                                                            *
*             _cpStr : Decimal을 나타내는 문자열이 저장 된 버퍼                *
*                                                                              *
* Return    : int, 성공(변환 된 Decimal), 실패(-1)                             *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : 받은 문자열(_str)을 10진 Decimal 데이터로 반환한다.              *
*             변환 할 수 있는 데이터가 없는 경우 -1을 반환한다.                *
*             변환 가능한 문자와 불가능 문자가 섞여 있을경우 -1을 반환한다.    *
*******************************************************************************/
static int CDConfigStrToDec( char* _cpStr )
{
	int		iLen;

	iLen	= strlen( _cpStr );

	if( iLen > 2 )
	{
		if( strncmp( _cpStr,"0x",2 ) == 0 )
			return	CDConfigHexConverter( _cpStr + 2 );
		else
			return	CDConfigDecConverter( _cpStr );
	}
	else
	{
		return	CDConfigDecConverter( _cpStr );
	}
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : char*                                                            *
*             _cpStr : 10진수로 변환 될 16진 문자열이 저장 된 버퍼             *
*                                                                              *
* Return    : int, 성공(변환 된 Decimal), 실패(-1)                             *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : 16진수 문자열(_cpStr)을 받아서 10진수로 반환한다.                *
*             중간에 이상이 있는 데이터나 문자열이 끼어있을 경우 -1을 반환한다.*
*******************************************************************************/
static int CDConfigHexConverter( char* _cpStr )
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
			return	CD_CONFIG_NO_DECIMAL;

		iMultiple *= 16;
	}

	return	iResult;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : char*                                                            *
*             _cpStr : 10진수로 변환 될 10진 문자열이 저장 된 버퍼             *
*                                                                              *
* Return    : int, 성공(변환 된 Decimal), 실패(-1)                             *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : 10진수 문자열(_cpStr)을 받아서 10진수로 반환한다.                *
*             중간에 이상이 있는 데이터나 문자열이 끼어있을 경우 -1을 반환한다.*
*******************************************************************************/
static int CDConfigDecConverter( char* _cpStr )
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
			return	CD_CONFIG_NO_DECIMAL;

		iMultiple *= 10;
	}

	return	iResult;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : char*                                                            *
*             _iFd   : 데이터를 읽어들일 파일의 파일 디스크립터                *
*             _cpBuf : 버퍼에서 읽은 데이터를 담아갈 버퍼                      *
*             _iSize : cpBuf에 저장할 데이터의 최대 길이                       *
*                                                                              *
* Return    : int, 성공(변환 된 Decimal), 실패(-1)                             *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : 파일 디스크립터(_iFd) 로부터 한라인의 데이터를 읽어서            *
*             버퍼(_cpBuf)에 저장한다. 버퍼의 최대 크기는 _iSize 이며          *
*             라인의 길이(크기)가 _iSize 보다 클경우 버퍼의 크기-1 만큼의      *
*             데이터만 읽어들인다.                                             *
*             버퍼를 읽다가 파일의 끝(0, NULL)에 도달 할 경우 그때까지         *
*             읽어들인 데이터를 버퍼(_cpBuf)에 저장하고 읽어들인 길이를        *
*             반환한다. 데이터의 마지막에는 꼭 0(NULL)이 추가된다.             *
*             정상 적으로 \n(뉴라인)까지 읽어들일 경우 읽어들인 데이터를       *
*             버퍼(_cpBuf)에 저장하고 읽어들인 라인의 길이(크기)를 반환한다.   *
*******************************************************************************/
static int CDConfigReadLine( int _iFd, char* _cpBuf, int _iSize )
{
	int iResult;
	int iReadByte = 0;

	while( iReadByte < _iSize - 1 )
	{
		iResult = read( _iFd, _cpBuf + iReadByte, 1 );

		if( ( iResult == CD_CONFIG_ERROR ) && ( errno == EINTR ) )
			continue;

		if( ( iResult == 0 ) && ( iReadByte == 0 ) )
			return	0;

		if( iResult == 0 )
			break;

		if( iResult == CD_CONFIG_ERROR )
			return	CD_CONFIG_ERROR;

		iReadByte++;

		if( _cpBuf[iReadByte - 1] == '\n' )
			break;
	}

	_cpBuf[iReadByte] = '\0';

	return	iReadByte;
}

