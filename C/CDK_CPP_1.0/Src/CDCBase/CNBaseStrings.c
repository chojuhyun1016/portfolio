#include "CNBaseStrings.h"
#include "CNBaseDefinitions.h"

/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*                                                            *
*             _cpString  : ���� ���ڿ��� �ּ�                                  *
*                                                                              *
* Return    : void                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���� ������ ���ڿ�(_cpString)�� �ּҰ��� �޾Ƽ� ���ڿ� ����      *
*             �� ��ġ�� ' ', '\1', '\n' ���ڸ� ����� ��� ���ڿ��� �ٽ�   *
*             �ּҰ����� ���� ���ڿ�(_cpString)�� �����Ѵ�.                    *
*******************************************************************************/
void LeftTrim( char* _cpString )
{
	int iLength;
	int	iStart;
	int	iCurrent = 0;

	iLength = strlen( _cpString );

	for( iStart = 0; iStart < iLength; iStart++ )
	{
		if( ( _cpString[iStart] != ' ' ) && ( _cpString[iStart] != '\t' ) && ( _cpString[iStart] != '\n' ) )
		{
			iCurrent = iStart;
			
			break;
		}
	}

	if( iCurrent > 0 )
	{
		for( iStart = 0; iCurrent < iLength; iStart++, iCurrent++ )
		{
			_cpString[iStart] = _cpString[iCurrent];
		}

		_cpString[iLength] = NULL;
	}
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*, char*                                                     *
*             _cpBuffer  : ��ȯ �� ���ڿ��� ���� �� ������ �ּ�                *
*             _cpString  : ���� ���ڿ��� �ּ�                                  *
*                                                                              *
* Return    : void                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���� ������ ���� ���ڿ�(_cpString)�� ���ʿ� ' ', '\1', '\n'      *
*             ���ڸ� ����� ��� ���ڿ��� ����(_cpBuffer)�� �����Ѵ�.      *
*******************************************************************************/
void LeftTrimToBuffer( char* _cpBuffer, char* _cpString )
{
	int iLength;
	int	iStart;
	int	iCurrent = 0;

	iLength = strlen( _cpString );

	for( iStart = 0; iStart < iLength; iStart++ )
	{
		if( ( _cpString[iStart] != ' ' ) && ( _cpString[iStart] != '\t' ) && ( _cpString[iStart] != '\n' ) )
		{
			iCurrent = iStart;
			
			break;
		}
	}

	if( iCurrent > 0 )
	{
		for( iStart = 0; iCurrent < iLength; iStart++, iCurrent++ )
		{
			_cpBuffer[iStart] = _cpString[iCurrent];
		}

		_cpBuffer[iLength] = NULL;
	}
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*                                                            *
*             _cpString  : ���� ���ڿ��� �ּ�                                  *
*                                                                              *
* Return    : void                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���� ������ ���ڿ�(_cpString)�� �ּҰ��� �޾Ƽ� ���ڿ� ������    *
*             �� ��ġ�� ' ', '\1', '\n' ���ڸ� ����� ��� ���ڿ��� �ٽ�   *
*             �ּҰ����� ���� ���ڿ�(_cpString)�� �����Ѵ�.                    *
*******************************************************************************/
void RightTrim( char* _cpString )
{
	int iStart;
	int iLength;

	iLength = strlen( _cpString );

	for( iStart = iLength - 1; iStart >= 0; iStart-- )
	{
		if( ( _cpString[iStart] == ' ' ) || ( _cpString[iStart] == '\t' ) || ( _cpString[iStart] == '\n' ) )
			_cpString[iStart] = NULL;
		else
			break;
	}
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*, char*                                                     *
*             _cpBuffer  : ��ȯ �� ���ڿ��� ���� �� ������ �ּ�                *
*             _cpString  : ���� ���ڿ��� �ּ�                                  *
*                                                                              *
* Return    : void                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���� ������ ���� ���ڿ�(_cpString)�� �����ʿ� ' ', '\1', '\n'    *
*             ���ڸ� ����� ��� ���ڿ��� ����(_cpBuffer)�� �����Ѵ�.      *
*******************************************************************************/
void RightTrimToBuffer( char* _cpBuffer, char* _cpString )
{
	int iStart;
	int iLength;

	strcpy( _cpBuffer, _cpString );

	iLength = strlen( _cpBuffer );

	for( iStart = iLength - 1; iStart >= 0; iStart-- )
	{
		if( ( _cpBuffer[iStart] == ' ' ) || ( _cpBuffer[iStart] == '\t' ) || ( _cpBuffer[iStart] == '\n' ) )
			_cpBuffer[iStart] = NULL;
		else
			break;
	}
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*,                                                           *
*             _cpString  : ���� ���ڿ��� �ּ�                                  *
*                                                                              *
* Return    : void                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���� ������ ���� ���ڿ�(_cpString)�� ���ʿ� ' ', '\1', '\n'      *
*             ���ڸ� ����� ��� ���ڿ��� ����(_cpBuffer)�� �����Ѵ�.      *
*******************************************************************************/
void Trim( char* _cpString )
{
	LeftTrim( _cpString );
	RightTrim( _cpString );
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*, char*                                                     *
*             _cpBuffer  : ��ȯ �� ���ڿ��� ���� �� ������ �ּ�                *
*             _cpString  : ���� ���ڿ��� �ּ�                                  *
*                                                                              *
* Return    : void                                                             *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���� ������ ���� ���ڿ�(_cpString)�� ���ʿ� ' ', '\1', '\n'      *
*             ���ڸ� ����� ��� ���ڿ��� ����(_cpBuffer)�� �����Ѵ�.      *
*******************************************************************************/
void TrimToBuffer( char* _cpBuffer, char* _cpString )
{
	LeftTrimToBuffer( _cpBuffer, _cpString );

	RightTrim( _cpBuffer );
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*, int, int                                                  *
*             _cpString  : ���� ���ڿ��� �ּ�                                  *
*             _iStart    : �߶� ���ڿ� �������� ���� ����                    *
*             _iLength   : �߶� ���ڿ��� ����                                *
*                                                                              *
* Return    : char*, ���� �� ���� ���ڿ��� �ּ�(_cpString)                     *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���ڿ�(_cpString)���� _iStart��° �������� _iLength ���̸�ŭ     *
*             �߶� �� �߶� ���ڿ��� �ٽ� ���� ���ڿ�(_cpString)�� �����Ѵ�.*
*******************************************************************************/
char* SubStr( char* _cpString, int _iStart, int _iLength )
{
	int	iStringIndex;
	int iBufferIndex;

	int i1;
	int i2;

	for( iStringIndex = _iStart, iBufferIndex = 0; iBufferIndex < _iLength; iStringIndex++, iBufferIndex++ )
		_cpString[iBufferIndex] = _cpString[iStringIndex];

	_cpString[iBufferIndex] = NULL;

	return	_cpString;
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*, char*, int, int                                           *
*             _cpBuffer  : �߶� ���ڿ��� ���� �� ������ �ּ�                 *
*             _cpString  : ���� ���ڿ��� �ּ�                                  *
*             _iStart    : �߶� ���ڿ� �������� ���� ����                    *
*             _iLength   : �߶� ���ڿ��� ����                                *
*                                                                              *
* Return    : char*, �߶� ���ڿ��� ���� �� ������ �ּ�(_cpString)            *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���ڿ�(_cpString)���� _iStart��° �������� _iLength ���̸�ŭ     *
*             �߶� �� �߶� ���ڿ��� ����(_cpBuffer)�� �����Ѵ�.            *
*******************************************************************************/
char* SubStrToBuffer( char* _cpBuffer, char* _cpString, int _iStart, int _iLength )
{
	int	iStringIndex;
	int iBufferIndex;

	for( iStringIndex = _iStart - 1, iBufferIndex = 0; iStringIndex < _iStart + _iLength - 1; iStringIndex++, iBufferIndex++ )
		_cpBuffer[iBufferIndex] = _cpString[iStringIndex];

	_cpBuffer[iBufferIndex] = NULL;

	return	_cpBuffer;
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*                                                            *
*             _cpString  : ���� ���ڿ��� �ּ�                                  *
*                                                                              *
* Return    : char*, ���� �� ���� ���ڿ��� �ּ�(_cpString)                     *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���ڿ�(_cpString)���� �ҹ��� ���Ĺ �ش��ϴ� ���ڸ� �빮�ڷ�   *
*             ���� �� ���� �� ���ڿ��� �ٽ� ���� ���ڿ�(_cpString)�� �����Ѵ�. *
*******************************************************************************/
char* UpStr( char* _cpString )
{
	int	iLoop;

	for( iLoop=0; _cpString[iLoop] != 0; iLoop++ )
	{
		if( _cpString[iLoop] >= 'a' && _cpString[iLoop] <= 'z' )
			_cpString[iLoop] -= 0x20;
	}

	return	_cpString;
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*, char*                                                     *
*             _cpBuffer  : ���� �� ���ڿ��� ���� �� ������ �ּ�                *
*             _cpString  : ���� ���ڿ��� �ּ�                                  *
*                                                                              *
* Return    : char*, ���� �� ���ڿ��� ���� �� ������ �ּ�(_cpString)           *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���ڿ�(_cpString)���� �ҹ��� ���Ĺ �ش��ϴ� ���ڸ� �빮�ڷ�   *
*             ���� �� ���� �� ���ڿ��� ����(_cpBuffer)�� �����Ѵ�.             *
*******************************************************************************/
char* UpStrToBuffer( char* _cpBuffer, char* _cpString )
{
	int	iLoop;

	for( iLoop=0; _cpString[iLoop] != 0; iLoop++ )
	{
		if( _cpString[iLoop] >= 'a' && _cpString[iLoop] <= 'z' )
			_cpBuffer[iLoop] = _cpString[iLoop] - 0x20;
		else
			_cpBuffer[iLoop] = _cpString[iLoop];
	}

	_cpBuffer[iLoop] = NULL;

	return	_cpBuffer;
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*                                                            *
*             _cpString  : ���� ���ڿ��� �ּ�                                  *
*                                                                              *
* Return    : char*, ���� �� ���� ���ڿ��� �ּ�(_cpString)                     *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���ڿ�(_cpString)���� �빮�� ���Ĺ �ش��ϴ� ���ڸ� �ҹ��ڷ�   *
*             ���� �� ���� �� ���ڿ��� �ٽ� ���� ���ڿ�(_cpString)�� �����Ѵ�. *
*******************************************************************************/
char* DownStr( char* _cpString )
{
	int	iLoop;

	for( iLoop=0; _cpString[iLoop] != 0; iLoop++ )
	{
		if( _cpString[iLoop] >= 'A' && _cpString[iLoop] <= 'Z' )
			_cpString[iLoop] += 0x20;
	}

	return	_cpString;
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*, char*                                                     *
*             _cpBuffer  : ���� �� ���ڿ��� ���� �� ������ �ּ�                *
*             _cpString  : ���� ���ڿ��� �ּ�                                  *
*                                                                              *
* Return    : char*, ���� �� ���ڿ��� ���� �� ������ �ּ�(_cpString)           *
* Stability : Async-Signal-Safe                                                *
* Explain   : ���ڿ�(_cpString)���� �빮�� ���Ĺ �ش��ϴ� ���ڸ� �ҹ��ڷ�   *
*             ���� �� ���� �� ���ڿ��� ����(_cpBuffer)�� �����Ѵ�.             *
*******************************************************************************/
char* DownStrToBuffer( char* _cpBuffer, char* _cpString )
{
	int	iLoop;

	for( iLoop=0; _cpString[iLoop] != 0; iLoop++ )
	{
		if( _cpString[iLoop] >= 'A' && _cpString[iLoop] <= 'Z' )
			_cpBuffer[iLoop] = _cpString[iLoop] + 0x20;
		else
			_cpBuffer[iLoop] = _cpString[iLoop];
	}

	_cpBuffer[iLoop] = NULL;

	return	_cpBuffer;
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : char*                                                            *
*             _cpHex  : ���� ���ڿ��� �ּ�                                     *
*                                                                              *
* Return    : long, ���ڿ����� 10������ ���� �� 10�� ������                    *
* Stability : MT-Safe                                                          *
* Explain   : ������ ���ڿ�(_cpHex)�� �޾Ƽ� 10������ �����Ͽ� ��ȯ�Ѵ�.       *
*             10�� ���ڿ��� ��� atol�� ���ؼ� �ٷ� 10������ ��ȯ�Ǹ�          *
*             16�� ���ڿ�(���ڿ� ������ "0x")�� ��� ���������� ��ȯ ������    *
*             ��ģ �� 10������ ��ȯ�ȴ�.                                       *
*******************************************************************************/
long StrToDec( char* _cpHex )
{
	int		iLoop;
	int		iLength;

	long	iResult = 0;
	long	iMultiple = 1;

	char	caBuffer[CN_BASE_STRING_BUFFER_SIZE];

	strlcpy( caBuffer, _cpHex, CN_BASE_STRING_BUFFER_SIZE );

	if( strncmp( caBuffer,"0x",2 ) == 0 )
		strlcpy( caBuffer,caBuffer + 2, CN_BASE_STRING_BUFFER_SIZE - 2 );
	else
		return	atol( caBuffer );

	iLength = strlen( caBuffer );

	for( iLoop = iLength - 1; iLoop >= 0; iLoop-- )
	{
		if( caBuffer[ iLoop ] >= '0' && caBuffer[ iLoop ] <= '9' )
		{
			iResult += ( ( caBuffer[iLoop] - '0' ) * iMultiple );
		}
		else
		{
			if( caBuffer[iLoop] >= 'A' && caBuffer[iLoop] <= 'F' )
			{
				iResult += ( ( caBuffer[iLoop] - 'A' + 10 ) * iMultiple );
			}
			else
			{	
				if( caBuffer[iLoop] >= 'a' && caBuffer[iLoop] <= 'f' )
					iResult += ( ( caBuffer[iLoop] - 'a' + 10 ) * iMultiple );
			}
		}

		iMultiple *= 16;
	}

	return	iResult;
}
