#include <stdio.h>
#include <strings.h>
#include <sys/time.h>

#include "CNBaseTime.h"


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : char*                                                            *
*             _cpBuffer  : ���ڿ��� ���� �� ���� �ּ�                          *
*                                                                              *
* Return    : char*, �ð� ���ڿ��� ���� �� ������ �ּҰ�                       *
* Stability : MT-Safe                                                          *
* Explain   : ���� �ð��� ���ؼ� ����(_cpBuffer)�� ���ڿ� ���·� ����Ѵ�.     *
*             ��� ���´� YYYYMMDDhhmmss �� �ȴ�.                              *
*             Buffer�� �ּ� ũ��� 14����Ʈ �̻��̿��� �Ѵ�.                   *
*******************************************************************************/
char* GetNowTimeStr( char* _cpBuffer )
{
	time_t		tNowTime;

	struct tm	tmNowTime;

	tNowTime	= time( NULL );

	localtime_r( &tNowTime, &tmNowTime );

	sprintf( _cpBuffer, "%04d%02d%02d%02d%02d%02d",
		1900 + tmNowTime.tm_year, 
		tmNowTime.tm_mon + 1, 
		tmNowTime.tm_mday, 
		tmNowTime.tm_hour, 
		tmNowTime.tm_min, 
		tmNowTime.tm_sec );

	return	_cpBuffer;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : char*, char*                                                     *
*             _cpBuffer        : ���ڿ��� ���� �� ���� �ּ�                    *
*             _cpFormat        : ���ۿ� ���� �� ���ڿ��� Format                *
*                                                                              *
* Return    : char*, �ð� ���ڿ��� ���� �� ������ �ּҰ�                       *
* Stability : MT-Safe                                                          *
* Explain   : ���ڰ����� ���޹��� ����(_cpBuffer)�� ���� �ð��� �ش��ϴ�       *
*             ���ڿ��� _cpFormat�� ���� �� ���·� ����Ѵ�.                    *
*             ��� ���´� YYYYMMDDhhmmss, YYYYMMDD, hhmmss,                    *
*             YYYY/MM/DD/hh/mm/ss �� �پ��� ������ ���ڿ� ����� �����ϴ�.     *
*             ���˼����� �� �� �� �� �� �� ���̿��� �Ѵ�. ���� ������ ����     *
*             �ʴٸ� �Լ��� �������Ѵ�. �������ڸ� YYYYMMDD Ȥ�� mmhhss �ΰ�� *
*             ������ �ϸ� hhmmss �� DDMMYYYY���� �����̰ų� ���̰� �Ǹ�      *
*             �Լ��� �������ϰ� �ȴ�.                                          *
*******************************************************************************/
char* GetNowTimeFormatStr( char* _cpBuffer, char* _cpFormat )
{
	int			iIndex;
	int			iLength;

	time_t		tNowTime;

	char		*cpOffset;

	struct tm	tmNowTime;

	iLength = strlen( _cpFormat );
	
	tNowTime = time( NULL );

	localtime_r( &tNowTime, &tmNowTime );

	for( iIndex = 0; iIndex < iLength; )
	{
		switch( _cpFormat[iIndex] )
		{
			case 's':
			{
				if( _cpFormat[iIndex + 1] != 's')  
				{
					_cpBuffer[iIndex] = _cpFormat[iIndex];
					iIndex++;

					break;
				}
				_cpBuffer[iIndex] = ( tmNowTime.tm_sec / 10 ) + 0x30;
				_cpBuffer[iIndex + 1] = ( tmNowTime.tm_sec % 10 ) + 0x30;
				iIndex = iIndex + 2;

				break;
			}
			case 'm':
			{
				if( _cpFormat[iIndex + 1] != 'm' )
				{
					_cpBuffer[iIndex] = _cpFormat[iIndex];
					iIndex++;

					break;
				}
				_cpBuffer[iIndex] = ( tmNowTime.tm_min / 10 ) + 0x30;
				_cpBuffer[iIndex + 1] = ( tmNowTime.tm_min % 10 ) + 0x30;
				iIndex = iIndex + 2;

				break;
			}
			case 'h':
			{
				if( _cpFormat[iIndex + 1] != 'h' )
				{
					_cpBuffer[iIndex] = _cpFormat[iIndex];
					iIndex++;

					break;
				}
				_cpBuffer[iIndex] = ( tmNowTime.tm_hour / 10 ) + 0x30;
				_cpBuffer[iIndex + 1] = ( tmNowTime.tm_hour % 10 ) + 0x30;
				iIndex = iIndex + 2;

				break;
			}
			case 'D':
			{
				if( _cpFormat[iIndex + 1] != 'D')
				{
					_cpBuffer[iIndex] = _cpFormat[iIndex];
					iIndex++;

					break;
				}
				_cpBuffer[iIndex] = ( tmNowTime.tm_mday / 10 ) + 0x30;
				_cpBuffer[iIndex + 1] = ( tmNowTime.tm_mday % 10 ) + 0x30;
				iIndex = iIndex + 2;

				break;
			}
			case 'M':
			{
				if( _cpFormat[iIndex + 1 ] != 'M' )
				{
					_cpBuffer[iIndex] = _cpFormat[iIndex];
					iIndex ++;

					break;
				}
				_cpBuffer[iIndex] = ( ( tmNowTime.tm_mon + 1 ) / 10 ) + 0x30;
				_cpBuffer[iIndex + 1] = ( ( tmNowTime.tm_mon + 1 )  % 10 ) + 0x30;
				iIndex = iIndex + 2;

				break;
			}
			case 'Y':
			{
				cpOffset = &_cpFormat[iIndex];

				if(memcmp( cpOffset ,"YYYY", 4 ) == 0 )
				{
								
					tmNowTime.tm_year = tmNowTime.tm_year + 1900;

					_cpBuffer[iIndex + 2] = ( tmNowTime.tm_year % 100 ) / 10 + 0x30;
					_cpBuffer[iIndex + 3] = ( tmNowTime.tm_year % 100 ) % 10 + 0x30;
					_cpBuffer[iIndex] = ( ( tmNowTime.tm_year / 100 ) / 10 ) + 0x30;
					_cpBuffer[iIndex + 1] = ( ( tmNowTime.tm_year / 100 ) % 10 )  + 0x30;
					iIndex = iIndex + 4;

					break;
				}
				if( _cpFormat[iIndex + 1 ] != 'Y' )
				{
					_cpBuffer[iIndex] = _cpFormat[iIndex];
					iIndex++;

					break;
				}

				tmNowTime.tm_year = tmNowTime.tm_year + 1900;

				_cpBuffer[iIndex] = ( tmNowTime.tm_year % 100 ) / 10 + 0x30;
				_cpBuffer[iIndex + 1] = ( tmNowTime.tm_year % 100 ) / 10 + 0x30;
				iIndex = iIndex + 2;

				break;
			}
			default :
			{
				_cpBuffer[iIndex] = _cpFormat[iIndex];
				iIndex++;
			}
		}
	}

	_cpBuffer[iIndex] = NULL;

	return	_cpBuffer;
}


/*******************************************************************************
* Update    : 2011/04/25                                                       *
* Argument  : time_t, char*, char*                                             *
*             _cpBuffer  : ���� �� �ð� ���ڿ��� ���� �� ���� �ּ�             *
*             _iTime     : ���ڿ��� ��ȯ �� �ð�                               *
*             _cpFormat  : ���� �� ���ڿ��� Format                             *
*                                                                              *
* Return    : char*, �ð� ���ڿ��� ���� �� ������ �ּҰ�                       *
* Stability : MT-Safe                                                          *
* Explain   : ���ڰ����� ���޹��� �ð�(_iTime)�� ����(_cpBuffer)�� _cpFormat�� *
*             ���� �� ���ڿ� ���·� ����Ѵ�.                                  *
*             ��� ���´� YYYYMMDDhhmmss, YYYYMMDD, hhmmss,                    *
*             YYYY/MM/DD/hh/mm/ss �� �پ��� ������ ���ڿ� ����� �����ϴ�.     *
*             ���˼����� �� �� �� �� �� �� ���̿��� �Ѵ�. ���� ������ ����     *
*             �ʴٸ� �Լ��� �������Ѵ�. �������ڸ� YYYYMMDD Ȥ�� mmhhss �ΰ�� *
*             ������ �ϸ� hhmmss �� DDMMYYYY���� �����̰ų� ���̰� �Ǹ�      *
*             �Լ��� �������ϰ� �ȴ�.                                          *
*******************************************************************************/
char* TimeToStr( char* _cpBuffer, time_t _iTime, char* _cpFormat )
{
	int			iIndex;
	int			iLength;

	char		*cpOffset;

	struct tm	tmNowTime;

	iLength = strlen( _cpFormat );
	
	localtime_r( &_iTime, &tmNowTime );

	for( iIndex = 0; iIndex < iLength; )
	{
		switch( _cpFormat[iIndex] )
		{
			case 's':
			{
				if( _cpFormat[iIndex + 1] != 's')  
				{
					_cpBuffer[iIndex] = _cpFormat[iIndex];
					iIndex++;

					break;
				}
				_cpBuffer[iIndex] = ( tmNowTime.tm_sec / 10 ) + 0x30;
				_cpBuffer[iIndex + 1] = ( tmNowTime.tm_sec % 10 ) + 0x30;
				iIndex = iIndex + 2;

				break;
			}
			case 'm':
			{
				if( _cpFormat[iIndex + 1] != 'm' )
				{
					_cpBuffer[iIndex] = _cpFormat[iIndex];
					iIndex++;

					break;
				}
				_cpBuffer[iIndex] = ( tmNowTime.tm_min / 10 ) + 0x30;
				_cpBuffer[iIndex + 1] = ( tmNowTime.tm_min % 10 ) + 0x30;
				iIndex = iIndex + 2;

				break;
			}
			case 'h':
			{
				if( _cpFormat[iIndex + 1] != 'h' )
				{
					_cpBuffer[iIndex] = _cpFormat[iIndex];
					iIndex++;

					break;
				}
				_cpBuffer[iIndex] = ( tmNowTime.tm_hour / 10 ) + 0x30;
				_cpBuffer[iIndex + 1] = ( tmNowTime.tm_hour % 10 ) + 0x30;
				iIndex = iIndex + 2;

				break;
			}
			case 'D':
			{
				if( _cpFormat[iIndex + 1] != 'D')
				{
					_cpBuffer[iIndex] = _cpFormat[iIndex];
					iIndex++;

					break;
				}
				_cpBuffer[iIndex] = ( tmNowTime.tm_mday / 10 ) + 0x30;
				_cpBuffer[iIndex + 1] = ( tmNowTime.tm_mday % 10 ) + 0x30;
				iIndex = iIndex + 2;

				break;
			}
			case 'M':
			{
				if( _cpFormat[iIndex + 1 ] != 'M' )
				{
					_cpBuffer[iIndex] = _cpFormat[iIndex];
					iIndex ++;

					break;
				}
				_cpBuffer[iIndex] = ( ( tmNowTime.tm_mon + 1 ) / 10 ) + 0x30;
				_cpBuffer[iIndex + 1] = ( ( tmNowTime.tm_mon + 1 )  % 10 ) + 0x30;
				iIndex = iIndex + 2;

				break;
			}
			case 'Y':
			{
				cpOffset = &_cpFormat[iIndex];

				if(memcmp( cpOffset ,"YYYY", 4 ) == 0 )
				{
								
					tmNowTime.tm_year = tmNowTime.tm_year + 1900;

					_cpBuffer[iIndex + 2] = ( tmNowTime.tm_year % 100 ) / 10 + 0x30;
					_cpBuffer[iIndex + 3] = ( tmNowTime.tm_year % 100 ) % 10 + 0x30;
					_cpBuffer[iIndex] = ( ( tmNowTime.tm_year / 100 ) / 10 ) + 0x30;
					_cpBuffer[iIndex + 1] = ( ( tmNowTime.tm_year / 100 ) % 10 )  + 0x30;
					iIndex = iIndex + 4;

					break;
				}
				if( _cpFormat[iIndex + 1 ] != 'Y' )
				{
					_cpBuffer[iIndex] = _cpFormat[iIndex];
					iIndex++;

					break;
				}

				tmNowTime.tm_year = tmNowTime.tm_year + 1900;

				_cpBuffer[iIndex] = ( tmNowTime.tm_year % 100 ) / 10 + 0x30;
				_cpBuffer[iIndex + 1] = ( tmNowTime.tm_year % 100 ) / 10 + 0x30;
				iIndex = iIndex + 2;

				break;
			}
			default :
			{
				_cpBuffer[iIndex] = _cpFormat[iIndex];
				iIndex++;
			}
		}
	}

	_cpBuffer[iIndex] = NULL;

	return	_cpBuffer;
}


/*******************************************************************************
* Update    : 2011/05/02                                                       *
* Argument  : char*, char*                                                     *
*             _cpString : �ð� ���ڿ��� �ּ�                                   *
*             _cpFormat : �ð����� ��ȯ �� ���ڿ��� Format                     *
*                                                                              *
* Return    : time_t, ���ڿ�(_cpString)���� �ð�(time_t)������ ��ȯ �� ��      *
* Stability : MT-Safe                                                          *
* Explain   : ���ڰ����� ���޹��� �ð����ڿ�(_cpString)�� time_t�� ������ ��ȯ *
*             �Ͽ� ��ȯ�Ѵ�.                                                   *
*             _cpString�� ���¸� _cpFormat���� ��Ȯ�ϰ� ���޹޾ƾ� �Ѵ�.       *
*             ������� _cpString�� ����Ű�� ���ڿ��� "20110502124959"�� ���   *
*             _cpFormat�� "YYYYMMDDhhmmss"���� �Ѵ�.                           *
*             ���� _cpString�� ����Ű�� ���ڿ��� "20110502"�� ��� _cpFormat�� *
*             "YYYYMMDD"���� �Ѵ�.                                             *
*             ���˼����� �� �� �� �� �� �� ���̿��� �Ѵ�. ���� ������ ����     *
*             �ʴٸ� �Լ��� �������Ѵ�. �������ڸ� YYYYMMDD Ȥ�� mmhhss �ΰ�� *
*             ������ �ϸ� hhmmss �� DDMMYYYY���� �����̰ų� ���̰� �Ǹ�      *
*             �Լ��� �������ϰ� �ȴ�.                                          *
*******************************************************************************/
time_t StrToTime( char* _cpString, char* _cpFormat )
{
	int			iLength;
	int			iFormatIndex;

	char		*cpOffset;

	struct tm   tmTempTime;

	iLength = strlen( _cpFormat );

	memset( &tmTempTime, 0x00, sizeof( tmTempTime ) );
	
	if( iLength < 0 ) 
		return	NULL;

	for( iFormatIndex = 0; iFormatIndex < iLength; )
	{
		switch( _cpFormat[iFormatIndex] )
		{
			case 's':
			{
				if( _cpFormat[iFormatIndex + 1] != 's' )  
				{
					iFormatIndex++;

					break;
				}
				tmTempTime.tm_sec = ( ( _cpString[iFormatIndex] - 0x30) * 10 ) + ( _cpString[iFormatIndex + 1] - 0x30 );
				iFormatIndex = iFormatIndex + 2;

				break;
			}
			case 'm':
			{
				if( _cpFormat[iFormatIndex + 1] != 'm' )
				{
					iFormatIndex++;

					break;
				}
				tmTempTime.tm_min = ( ( _cpString[iFormatIndex] - 0x30 ) * 10 ) + ( _cpString[iFormatIndex + 1] - 0x30 );
				iFormatIndex = iFormatIndex + 2;

				break;
			}
			case 'h':
			{
				if( _cpFormat[iFormatIndex + 1] != 'h' )
				{
					iFormatIndex++;

					break;
				}
				tmTempTime.tm_hour = ( ( _cpString[iFormatIndex] - 0x30 ) * 10 ) + ( _cpString[iFormatIndex + 1] - 0x30 );
				iFormatIndex = iFormatIndex + 2;

				break;
			}
			case 'D':
			{
				if( _cpFormat[iFormatIndex + 1] != 'D' )
				{
					iFormatIndex++;

					break;
				}
				tmTempTime.tm_mday = ( ( _cpString[iFormatIndex] - 0x30 ) * 10 ) + ( _cpString[iFormatIndex + 1] - 0x30 );
				iFormatIndex = iFormatIndex + 2;

				break;
			}
			case 'M':
			{
				if( _cpFormat[iFormatIndex + 1 ] != 'M' )
				{
					iFormatIndex++;

					break;
				}
				tmTempTime.tm_mon = ( ( _cpString[iFormatIndex] - 0x30 ) * 10 ) + ( _cpString[iFormatIndex + 1] - 0x30 ) - 1;
				iFormatIndex = iFormatIndex + 2;

				break;
			}
			case 'Y':
			{

				cpOffset = &_cpFormat[iFormatIndex];

				if( memcmp( cpOffset ,"YYYY", 4 ) == 0 )
				{
					
					tmTempTime.tm_year = 
								( ( _cpString[iFormatIndex]     - 0x30 ) * 1000 ) +
								( ( _cpString[iFormatIndex + 1] - 0x30 ) * 100 ) +
								( ( _cpString[iFormatIndex + 2] - 0x30 ) * 10 ) +
								( ( _cpString[iFormatIndex + 3] - 0x30 ) ) - 1900;

					iFormatIndex = iFormatIndex + 4;

					break;
				}
				if( _cpFormat[iFormatIndex + 1 ] != 'Y' )
				{
					iFormatIndex++;

					break;
				}
				tmTempTime.tm_year = ( ( _cpString[iFormatIndex] - 0x30 ) * 10 ) + ( _cpString[iFormatIndex + 1] - 0x30 );
				iFormatIndex = iFormatIndex + 2;

				break;
			}
			default :
			{
				iFormatIndex++;
			}
		}
	}

	return	mktime( &tmTempTime );
}
