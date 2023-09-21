#include <stdio.h>
#include <strings.h>
#include <sys/time.h>

#include "CNBaseTime.h"


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : char*                                                            *
*             _cpBuffer  : 문자열을 저장 할 버퍼 주소                          *
*                                                                              *
* Return    : char*, 시간 문자열이 저장 된 버퍼의 주소값                       *
* Stability : MT-Safe                                                          *
* Explain   : 현재 시간을 구해서 버퍼(_cpBuffer)에 문자열 형태로 출력한다.     *
*             출력 형태는 YYYYMMDDhhmmss 가 된다.                              *
*             Buffer의 최소 크기는 14바이트 이상이여야 한다.                   *
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
*             _cpBuffer        : 문자열을 저장 할 버퍼 주소                    *
*             _cpFormat        : 버퍼에 저장 될 문자열의 Format                *
*                                                                              *
* Return    : char*, 시간 문자열이 저장 된 버퍼의 주소값                       *
* Stability : MT-Safe                                                          *
* Explain   : 인자값으로 전달받은 버퍼(_cpBuffer)에 현재 시간에 해당하는       *
*             문자열을 _cpFormat에 정의 된 형태로 출력한다.                    *
*             출력 형태는 YYYYMMDDhhmmss, YYYYMMDD, hhmmss,                    *
*             YYYY/MM/DD/hh/mm/ss 등 다양한 형태의 문자열 출력이 가능하다.     *
*             포맷순서는 년 월 일 시 분 초 순이여야 한다. 만약 순서가 맞지     *
*             않다면 함수는 오동작한다. 예를들자면 YYYYMMDD 혹은 mmhhss 인경우 *
*             정상동작 하며 hhmmss 나 DDMMYYYY같이 역순이거나 섞이게 되면      *
*             함수는 오동작하게 된다.                                          *
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
*             _cpBuffer  : 변형 된 시간 문자열을 저장 할 버퍼 주소             *
*             _iTime     : 문자열로 변환 할 시간                               *
*             _cpFormat  : 저장 될 문자열의 Format                             *
*                                                                              *
* Return    : char*, 시간 문자열이 저장 된 버퍼의 주소값                       *
* Stability : MT-Safe                                                          *
* Explain   : 인자값으로 전달받은 시간(_iTime)을 버퍼(_cpBuffer)에 _cpFormat에 *
*             정의 된 문자열 형태로 출력한다.                                  *
*             출력 형태는 YYYYMMDDhhmmss, YYYYMMDD, hhmmss,                    *
*             YYYY/MM/DD/hh/mm/ss 등 다양한 형태의 문자열 출력이 가능하다.     *
*             포맷순서는 년 월 일 시 분 초 순이여야 한다. 만약 순서가 맞지     *
*             않다면 함수는 오동작한다. 예를들자면 YYYYMMDD 혹은 mmhhss 인경우 *
*             정상동작 하며 hhmmss 나 DDMMYYYY같이 역순이거나 섞이게 되면      *
*             함수는 오동작하게 된다.                                          *
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
*             _cpString : 시간 문자열의 주소                                   *
*             _cpFormat : 시간으로 변환 할 문자열의 Format                     *
*                                                                              *
* Return    : time_t, 문자열(_cpString)에서 시간(time_t)형으로 변환 된 값      *
* Stability : MT-Safe                                                          *
* Explain   : 인자값으로 전달받은 시간문자열(_cpString)을 time_t형 정수로 변환 *
*             하여 반환한다.                                                   *
*             _cpString의 형태를 _cpFormat에서 정확하게 전달받아야 한다.       *
*             예를들어 _cpString이 가르키는 문자열이 "20110502124959"일 경우   *
*             _cpFormat은 "YYYYMMDDhhmmss"여야 한다.                           *
*             또한 _cpString이 가르키는 문자열이 "20110502"일 경우 _cpFormat은 *
*             "YYYYMMDD"여야 한다.                                             *
*             포맷순서는 년 월 일 시 분 초 순이여야 한다. 만약 순서가 맞지     *
*             않다면 함수는 오동작한다. 예를들자면 YYYYMMDD 혹은 mmhhss 인경우 *
*             정상동작 하며 hhmmss 나 DDMMYYYY같이 역순이거나 섞이게 되면      *
*             함수는 오동작하게 된다.                                          *
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

