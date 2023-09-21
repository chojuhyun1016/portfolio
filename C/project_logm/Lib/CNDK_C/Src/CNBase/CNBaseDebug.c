#include <stdio.h>
#include <strings.h>

#include "CNBaseDebug.h"
#include "CNBaseDefinitions.h"

/*******************************************************************************
* Update    : 2011/05/04                                                       *
* Argument  : char*, int, char*, char*                                         *
*             _cpBuffer : 변환 된 데이터가 저장 될 버퍼                        *
*             _cpPoint  : 원본 문자열의 주소                                   *
*             _iSize    : 변환 할 문자열(_cpPoint)의 길이                      *
*             _cpTitle  : 버퍼(_cpBuffer) 출력할때 위에 출력되는 제목(의미없음)*
*                                                                              *
* Return    : char*, Hex 문자열이 저장 된 버퍼의 주소(_cpBuffer)               *
* Stability : MT-Safe                                                          *
* Explain   : 문자열(_cpPoint)의 각 바이트를 정해진 길이(_iSize)만큼 Hex코드   *
*             문자열로 변환하여 버퍼(_cpBuffer)로 출력한다.                    *
*             받아서 해당 주소의 서버로 접속을 시도한다. 접속 과정의 Timeout   *
*             _cpTitle은 버퍼에 출력될 때 위에 붙는 이름으로 큰의미는 없다.    *
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
*             _cpPoint  : 원본 문자열의 주소                                   *
*             _iSize    : 변환 할 문자열(_cpPoint)의 길이                      *
*             _cpTitle  : 버퍼(_cpBuffer) 출력할때 위에 출력되는 제목(의미없음)*
*                                                                              *
* Return    : void, 없음                                                       *
* Stability : MT-Safe                                                          *
* Explain   : 문자열(_cpPoint)의 각 바이트를 정해진 길이(_iSize)만큼 Hex코드   *
*             문자열로 변환하여 버퍼(_cpBuffer)로 출력한 후 버퍼의 데이터를    *
*             화면 상으로도 출력한다.                                          *
*******************************************************************************/
void PrintHex( const char* _cpPoint, int _iSize, char* _cpTitle )
{
	char	caBuffer[CN_BASE_PRINT_HEX_BUFFER_SIZE];

	printf( "%s", HexConvert( caBuffer, _cpPoint, _iSize, _cpTitle ) );
}

