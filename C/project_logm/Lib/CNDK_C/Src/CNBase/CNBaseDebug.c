#include <stdio.h>
#include <strings.h>

#include "CNBaseDebug.h"
#include "CNBaseDefinitions.h"

/*******************************************************************************
* Update    : 2011/05/04                                                       *
* Argument  : char*, int, char*, char*                                         *
*             _cpBuffer : ��ȯ �� �����Ͱ� ���� �� ����                        *
*             _cpPoint  : ���� ���ڿ��� �ּ�                                   *
*             _iSize    : ��ȯ �� ���ڿ�(_cpPoint)�� ����                      *
*             _cpTitle  : ����(_cpBuffer) ����Ҷ� ���� ��µǴ� ����(�ǹ̾���)*
*                                                                              *
* Return    : char*, Hex ���ڿ��� ���� �� ������ �ּ�(_cpBuffer)               *
* Stability : MT-Safe                                                          *
* Explain   : ���ڿ�(_cpPoint)�� �� ����Ʈ�� ������ ����(_iSize)��ŭ Hex�ڵ�   *
*             ���ڿ��� ��ȯ�Ͽ� ����(_cpBuffer)�� ����Ѵ�.                    *
*             �޾Ƽ� �ش� �ּ��� ������ ������ �õ��Ѵ�. ���� ������ Timeout   *
*             _cpTitle�� ���ۿ� ��µ� �� ���� �ٴ� �̸����� ū�ǹ̴� ����.    *
*******************************************************************************/
char* HexConvert( char* _cpBuffer, const char* _cpPoint, int _iSize, char* _cpTitle )
{
	int	iLoop;
	int	iOffset;

	sprintf( _cpBuffer, "\n====================================[ Data Parsing ]========================================\n" );
	sprintf( _cpBuffer, "%sTitle=[%s] length=[%d]\n", _cpBuffer, _cpTitle, _iSize );
	sprintf( _cpBuffer, "%s%s", _cpBuffer,"------------------------------------------------------------------------|-------------------\n" );

	for( iLoop = 0; iLoop < ( _iSize / 20 ) + 1; iLoop++ )
	{
		sprintf( _cpBuffer,"%s%04d\t", _cpBuffer, iLoop * 20 );

		for( iOffset = 0; iOffset < 20; iOffset++ )
		{
			if( ( ( iLoop * 20 ) + iOffset ) >= _iSize )
				break;

			sprintf( _cpBuffer, "%s %02x",_cpBuffer, ( unsigned char ) _cpPoint[( iLoop * 20 ) + iOffset] );
		}
		
		if( iOffset < 20 )
		{
			for( ; iOffset < 20; iOffset++ )
				sprintf( _cpBuffer, "%s   ", _cpBuffer );
		}
		
		sprintf( _cpBuffer, "%s\t", _cpBuffer );

		for( iOffset = 0; iOffset < 20; iOffset++ )
		{
			if( ( ( iLoop * 20 ) + iOffset ) >= _iSize )
				break;

			if( ( unsigned char ) _cpPoint [( iLoop * 20 ) + iOffset] < 0x20 )
				sprintf( _cpBuffer, "%s%c",_cpBuffer, '_' );
			else
				sprintf( _cpBuffer, "%s%c",_cpBuffer, ( unsigned char ) _cpPoint[ ( iLoop * 20 ) + iOffset ] );
		}

		sprintf( _cpBuffer,"%s|\n", _cpBuffer );
	}

	sprintf( _cpBuffer, "%s%s", _cpBuffer,"============================================================================================\n\n" );

	return	_cpBuffer;
}


/*******************************************************************************
* Update    : 2011/05/04                                                       *
* Argument  : char*, int, char*                                                *
*             _cpPoint  : ���� ���ڿ��� �ּ�                                   *
*             _iSize    : ��ȯ �� ���ڿ�(_cpPoint)�� ����                      *
*             _cpTitle  : ����(_cpBuffer) ����Ҷ� ���� ��µǴ� ����(�ǹ̾���)*
*                                                                              *
* Return    : void, ����                                                       *
* Stability : MT-Safe                                                          *
* Explain   : ���ڿ�(_cpPoint)�� �� ����Ʈ�� ������ ����(_iSize)��ŭ Hex�ڵ�   *
*             ���ڿ��� ��ȯ�Ͽ� ����(_cpBuffer)�� ����� �� ������ �����͸�    *
*             ȭ�� �����ε� ����Ѵ�.                                          *
*******************************************************************************/
void PrintHex( const char* _cpPoint, int _iSize, char* _cpTitle )
{
	char	caBuffer[CN_BASE_PRINT_HEX_BUFFER_SIZE];

	printf( "%s", HexConvert( caBuffer, _cpPoint, _iSize, _cpTitle ) );
}
