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
*             _cpFile  : ���Ǳ� ���ϸ�(���ϰ�� + �����̸�)                    *
*             _cpPart  : ���Ǳ� ���Ͽ��� ã���� �ϴ� ���׸�                    *
*             _cpSPart : ���Ǳ� ���Ͽ��� ã���� �ϴ� ���׸�                    *
*             _ipBuf   : �о���� �����͸� ���� �� ����                        *
*                                                                              *
* Return    : int, ����(�о���� Decimal), ����(-1)                            *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : Config ����(_cpFile) ���� ��з�(_cpPart)�� �Һз�(_cpSPart)��   *
*             ���� ��(int��) �� ��ȯ�Ѵ�. ���� Ȥ�� ���� �� ���               *
*             -1�� ��ȯ(CD_CONFIG_ERROR)�Ѵ�.                                  *
*             ��з�(_cpPart) �Һз�(_cpSPart)�� �ִ� ���� 1024(1023+NULL)���� *
*             �̴�. �� �̻��� ��� ������ �ȴ�. ���� �÷� ������ 1024�� �Ѱ�   *
*             �ϴ³��� ��ģ���� Ʋ������. Config ���� �̸�(_cpFile) ����       *
*             1024�� �ִ� �������̴�. 1024�̻��� ���̰� �����ų� ���ڿ� ���� *
*             NULL �̾��� �����Ͱ� ���� ��� �޸� ħ��(segment fault)��    *
*             �Ͼ�� �ִ�.                                                   *
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
*             _cpFile  : ���Ǳ� ���ϸ�(���ϰ�� + �����̸�)                    *
*             _cpPart  : ���Ǳ� ���Ͽ��� ã���� �ϴ� ���׸�                    *
*             _cpSPart : ���Ǳ� ���Ͽ��� ã���� �ϴ� ���׸�                    *
*             _cpBuf   : �о���� �����͸� ���� �� ����                        *
*             _iSize   : _cpBuf(����)�� ũ��(�о���� �ִ����)                *
*                                                                              *
* Return    : char*, ����(&_cpBuf[0]), ����(NULL)                              *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : Config ����(_cpFile) ���� ��з�(_cpPart)�� �Һз�(_cpSPart)��   *
*             ���� ��(���ڿ�) �� ����(_cpBuf)�� �����ϰ� ���� �ÿ���           *
*             0(CD_CONFIG_SUCCESS)�� ��ȯ�ϰ� ����(_cpBuf)�� ���ڿ���          *
*             �����ϸ� ���� Ȥ�� ������ ��� -1(CD_CONFIG_ERROR)��             *
*             ��ȯ�Ѵ�.                                                        *
*             GetConfigInt�� �޸� Config ���Ͽ��� ���ڿ��� ���� ���Ƿ�         *
*             ���ڿ��� ���� �� ����(_cpBuf)�� ũ��(_iSize)�� ���ڷ� �޴´�.    *
*             Config ���Ͽ��� �����ؿ� �������� ���̰� ���ۺ��� Ŭ ���        *
*             ������ ũ��-1(�������� NULL�� �ִ´�) ��ŭ ���ۿ� �����Ѵ�.      *
*             �����Ͱ� ���ۺ��� Ŀ�� ���и� ��ȯ���� �ʴ´�. ������ ��ȯ�ϰ�   *
*             ���� ũ�⸸ŭ �����Ѵ�.                                          *
*             ��з�(_cpPart) �Һз�(_cpSPart)�� �ִ� ���� 1024(1023+NULL)���� *
*             �̴�. �� �̻��� ��� ������ �ȴ�. ���� �÷� ������ 1024�� �Ѱ�   *
*             �ϴ³��� ��ģ���� Ʋ������. Config ���� �̸�(_cpFile) ����       *
*             1024�� �ִ� �������̴�. 1024�̻��� ���̰� �����ų� ���ڿ� ���� *
*             NULL �̾��� �����Ͱ� ���� ��� �޸� ħ��(segment fault)��    *
*             �Ͼ�� �ִ�.                                                   *
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
*             _cpFile  : ���Ǳ� ���ϸ�(���ϰ�� + �����̸�)                    *
*             _cpPart  : ���Ǳ� ���Ͽ��� ã���� �ϴ� ���׸�                    *
*             _cpSPart : ���Ǳ� ���Ͽ��� ã���� �ϴ� ���׸�                    *
*             _cpBuf   : �о���� �����͸� ���� �� ����                        *
*             _iSize   : _cpBuf(����)�� ũ��(�о���� �ִ����)                *
*                                                                              *
* Return    : int, ����(1), ����(-1)                                           *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : Config ����(_cpFile)���� ��з�(_cpPart)�� �Һз�(_cpSPart)��    *
*             ���� ��(���ڿ�) �� ����(_cpBuf)�� �����ϰ� ���� �� 0�� ��ȯ�ϰ�  *
*             ���н� -1�� ��ȯ�Ѵ�.                                            *
*             ���� : Config ���Ͽ��� ���׸�(_cpPart)�� ���׸�(_cpSPart)�� ���� *
*             ���ο� ������� ã�� ���Ѵ�.                                     *
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
*             _cpOffset : �о���� ������ ����                                 *
*             _cpBuf    : ���ۿ��� ���� �����͸� ��ư� ����                   *
*             _iSize    : cpBuf�� ������ �������� �ִ� ����                    *
*                                                                              *
* Return    : char*, ����(�о���� ������ ������ �ּ�), ����(���д� ����)      *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : Config���� ���� ���� ������ ���ڿ�(_cpOffset)���� �ϳ��� �ܾ  *
*             �����Ͽ� ����(_cpBuf) �� ������ �� ��ȯ�Ѵ�. ��ȯ ����           *
*             ���ڿ�(_cpOffset)���� �������� �о���� ������                   *
*             �ּ�(_cpOffset[iLoop])�� ��ȯ�Ѵ�. ���д� ����. ���� �� ���     *
*             �о���� �������� �ּ�(_cpOffset[iLoop])�� ��ȯ�Ǹ� ����(_cpBuf) *
*             ���� �ƹ� �����͵� ���� �ʴ´�.(_cpBuf[0] ������ NULL ����   *
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
*             _cpStr : Decimal�� ��Ÿ���� ���ڿ��� ���� �� ����                *
*                                                                              *
* Return    : int, ����(��ȯ �� Decimal), ����(-1)                             *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : ���� ���ڿ�(_str)�� 10�� Decimal �����ͷ� ��ȯ�Ѵ�.              *
*             ��ȯ �� �� �ִ� �����Ͱ� ���� ��� -1�� ��ȯ�Ѵ�.                *
*             ��ȯ ������ ���ڿ� �Ұ��� ���ڰ� ���� ������� -1�� ��ȯ�Ѵ�.    *
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
*             _cpStr : 10������ ��ȯ �� 16�� ���ڿ��� ���� �� ����             *
*                                                                              *
* Return    : int, ����(��ȯ �� Decimal), ����(-1)                             *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : 16���� ���ڿ�(_cpStr)�� �޾Ƽ� 10������ ��ȯ�Ѵ�.                *
*             �߰��� �̻��� �ִ� �����ͳ� ���ڿ��� �������� ��� -1�� ��ȯ�Ѵ�.*
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
*             _cpStr : 10������ ��ȯ �� 10�� ���ڿ��� ���� �� ����             *
*                                                                              *
* Return    : int, ����(��ȯ �� Decimal), ����(-1)                             *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : 10���� ���ڿ�(_cpStr)�� �޾Ƽ� 10������ ��ȯ�Ѵ�.                *
*             �߰��� �̻��� �ִ� �����ͳ� ���ڿ��� �������� ��� -1�� ��ȯ�Ѵ�.*
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
*             _iFd   : �����͸� �о���� ������ ���� ��ũ����                *
*             _cpBuf : ���ۿ��� ���� �����͸� ��ư� ����                      *
*             _iSize : cpBuf�� ������ �������� �ִ� ����                       *
*                                                                              *
* Return    : int, ����(��ȯ �� Decimal), ����(-1)                             *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : ���� ��ũ����(_iFd) �κ��� �Ѷ����� �����͸� �о            *
*             ����(_cpBuf)�� �����Ѵ�. ������ �ִ� ũ��� _iSize �̸�          *
*             ������ ����(ũ��)�� _iSize ���� Ŭ��� ������ ũ��-1 ��ŭ��      *
*             �����͸� �о���δ�.                                             *
*             ���۸� �дٰ� ������ ��(0, NULL)�� ���� �� ��� �׶�����         *
*             �о���� �����͸� ����(_cpBuf)�� �����ϰ� �о���� ���̸�        *
*             ��ȯ�Ѵ�. �������� ���������� �� 0(NULL)�� �߰��ȴ�.             *
*             ���� ������ \n(������)���� �о���� ��� �о���� �����͸�       *
*             ����(_cpBuf)�� �����ϰ� �о���� ������ ����(ũ��)�� ��ȯ�Ѵ�.   *
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
