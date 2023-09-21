#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "Main.h"
#include "CDBase.h"


int main( int argc, char **argv )
{
	int				iResult;

	time_t			tNowTime1;
	time_t			tNowTime2;

	char			cChar1;
	char			cChar2;

	char			caBuffer1[32];
	char			caBuffer2[64];
	char			caBuffer3[128];
	char			caBuffer4[2048];

	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// CDBaseTime(시간 관련 함수) //////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////

	::fprintf( stderr, "\n\n" );
	::fprintf( stderr, "=================== CDBaseTime ===================\n" );

	::memset( caBuffer1, 0x00, sizeof( caBuffer1 ) );
	::memset( caBuffer2, 0x00, sizeof( caBuffer2 ) );
	::memset( caBuffer3, 0x00, sizeof( caBuffer3 ) );

	// 현재 시간을 나타내는 문자열을 버퍼(caBuffer1)에 문자열로 저장
	// 문자열 포맷은 YYYYMMDDhhmmss 
	CDBaseGetNowTimeStr( caBuffer1 );
	::fprintf( stderr, "[%-32s][%s]\n", "CDBaseGetNowTimeStr", caBuffer1 );

	// 현재 시간을 임의의 포맷(YYYY/MM/DD/hh:mm:ss)으로 버퍼(caBuffer2)에 문자열로 저장
	// YYYY, MM, DD, hh, mm, ss 문자열을 시간으로 인식하며
	// 차례대로 주어져야 한다. 그 이외에 문자는 그대로 출력
	// Ex) "YYYY/MM/DD/hh:mm:ss" = "2013/01/01/23:59:59"
	// Ex) "MM/DD/hh:mm:ss" = "01/01/23:59:59"
	// Ex) "YYYY/MM/DD/hh:mm" = "2013/01/01/23:59"
	CDBaseGetNowTimeFormatStr( caBuffer2, (char*)"YYYY/MM/DD/hh:mm:ss" );
	::fprintf( stderr, "[%-32s][%s]\n", "CDBaseGetNowTimeFormatStr", caBuffer2 );

	// 주어진 시간(tNowTime1)을 임의의 포맷(YYYYMMDDhhmmss)으로 버퍼(caBuffer3)에 문자열로 저장
	// YYYY, MM, DD, hh, mm, ss 문자열을 시간으로 인식하며
	// 차례대로 주어져야 한다. 그 이외에 문자는 그대로 출력
	// Ex) "YYYYMMDDhhmmss" = "20130101235959"
	// Ex) "MMDDhhmmss" = "0101235959"
	// Ex) "YYYYMMDDhhmm" = "201301012359"
	tNowTime1	= ::time( NULL );
	CDBaseTimeToFormatStr( caBuffer3, tNowTime1, (char*)"YYYYMMDDhhmmss" );
	::fprintf( stderr, "[%-32s][%s]\n", "CDBaseTimeToStr", caBuffer3 );

	// 주어진 시간문자열(caBuffer3)을 임의의 포맷(YYYYMMDDhhmmss)으로 버퍼(caBuffer3)에 문자열로 저장
	// YYYY, MM, DD, hh, mm, ss 문자열을 시간으로 인식하며
	// 차례대로 주어져야 한다. 그 이외에 문자는 그대로 출력
	// Ex) "YYYYMMDDhhmmss" = "20130101235959"
	// Ex) "MMDDhhmmss" = "0101235959"
	// Ex) "YYYYMMDDhhmm" = "201301012359"
	tNowTime2	= CDBaseFormatStrToTime( caBuffer3, (char*)"YYYYMMDDhhmmss" );
	::fprintf( stderr, "[%-32s][%d]\n", "CDBaseStrToTime", tNowTime2 );

	::fprintf( stderr, "\n\n" );

	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////// CDBaseStrings(문자열 관련 함수) /////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////

	::fprintf( stderr, "=================== CDBaseStrings ===================\n" );

	::memset( caBuffer1, 0x00, sizeof( caBuffer1 ) );
	::memset( caBuffer2, 0x00, sizeof( caBuffer2 ) );
	::memset( caBuffer3, 0x00, sizeof( caBuffer3 ) );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrLeftTrim", caBuffer1 );
	// 문자열(caBuffer1) 왼쪽의 공백,탭,뉴라인 을 제거 후 문자열을
	// 왼쪽 기준으로 재정렬
	CDBaseStrLeftTrim( caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrLeftTrim", caBuffer1 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	// 문자열(caBuffer1) 왼쪽의 공백,탭,뉴라인 을 제외한 문자열을
	// 버퍼(caBuffer2)에 복사
	CDBaseStrLeftTrimToBuffer( caBuffer2, caBuffer1 );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrLeftTrimToBuffer", caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrLeftTrimToBuffer", caBuffer2 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrRightTrim", caBuffer1 );
	// 문자열(caBuffer1) 오른쪽의 공백,탭,뉴라인 을 제거
	CDBaseStrRightTrim( caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrRightTrim", caBuffer1 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	// 문자열(caBuffer1) 오른쪽의 공백,탭,뉴라인 을 제외한 문자열을
	// 버퍼(caBuffer2)에 복사
	CDBaseStrRightTrimToBuffer( caBuffer2, caBuffer1 );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrRightTrimToBuffer", caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrRightTrimToBuffer", caBuffer2 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrTrim", caBuffer1 );
	// 문자열(caBuffer1) 양옆(오른쪽, 왼쪽)의 공백,탭,뉴라인 을 제거
	CDBaseStrTrim( caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrTrim", caBuffer1 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrTrimToBuffer", caBuffer1 );
	// 문자열(caBuffer1) 양쪽(오른쪽,왼쪽)의 공백,탭,뉴라인 을 제외한 문자열을
	// 버퍼(caBuffer2)에 복사
	CDBaseStrTrimToBuffer( caBuffer2, caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrTrimToBuffer", caBuffer2 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrSub", caBuffer1 );
	// 버퍼(caBuffer1)의 문자열에서 임의의 문자열 번지수(1)부터 임의의 개수(3)의 문자를 추출해서
	// 다시 버퍼에 저장(caBuffer1), 기존 문자열은 날라감
	CDBaseStrSub( caBuffer1, 1, 3 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrSub", caBuffer1 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrSubToBuffer", caBuffer1 );
	// 버퍼(caBuffer1)의 문자열에서 임의의 문자열 번지수(1)부터 임의의 개수(3)의 문자를 추출해서
	// 버퍼에 저장(caBuffer2)
	CDBaseStrSubToBuffer( caBuffer2, caBuffer1, 2, 4 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrSubToBuffer", caBuffer2 );

	//::strlcpy( caBuffer1, (char*)"  abcd  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrUp", caBuffer1 );
	// 버퍼(caBuffer1)안의 문자열에 소문자 알파벳이 존재 할 경우
	// 대문자 알파벳으로 변형
	CDBaseStrUp( caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrUp", caBuffer1 );

	//::strlcpy( caBuffer1, (char*)"  abcd  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrUpToBuffer", caBuffer1 );
	// 문자열(caBuffer1)을 버퍼(caBuffer2)로 복사 시 문자열에 소문자가
	// 존재 할 경우 대문자 알파벳으로 변형하여 버퍼(caBuffer2)로 복사
	CDBaseStrUpToBuffer( caBuffer2, caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrUpToBuffer", caBuffer2 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrDown", caBuffer1 );
	// 버퍼(caBuffer1)안의 문자열에 대문자 알파벳이 존재 할 경우
	// 소문자 알파벳으로 변형
	CDBaseStrDown( caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrDown", caBuffer1 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrDownToBuffer", caBuffer1 );
	// 문자열(caBuffer1)을 버퍼(caBuffer2)로 복사 시 문자열에 대문자가
	// 존재 할 경우 소문자 알파벳으로 변형하여 버퍼(caBuffer2)로 복사
	CDBaseStrDownToBuffer( caBuffer2, caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrDownToBuffer", caBuffer2 );

	//::strlcpy( caBuffer1, (char*)"123456789", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrToDec", caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%d]\n", "CDBaseStrToDec", CDBaseStrToDec( caBuffer1 ) );

	::fprintf( stderr, "\n\n" );

	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////// CDBaseDebug(헥사출력 관련 함수) /////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////

	::fprintf( stderr, "=================== CDBaseDebug ===================\n" );

	::memset( caBuffer1, 0x00, sizeof( caBuffer1 ) );
	::memset( caBuffer2, 0x00, sizeof( caBuffer2 ) );
	::memset( caBuffer3, 0x00, sizeof( caBuffer3 ) );
	::memset( caBuffer4, 0x00, sizeof( caBuffer4 ) );

	//::strlcpy( caBuffer1, (char*)"ABCDEFG 123456789", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseHexConvert", caBuffer1 );
	// 버퍼(caBuffer1)안의  정해진 길이(17)의 데이터를 헥사(10진) 형태로
	// 버퍼(caBuffer4)에 출력
	CDBaseHexConvert( caBuffer4, caBuffer1, 17, (char*)"HEX CONVERT" );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrDownToBuffer", caBuffer4 );

	// 버퍼(caBuffer1)안의 정해진 길이(17)의 데이터를 헥사(10진) 형태로
	// 화면에 출력
	CDBaseHexPrint( caBuffer1, 17, (char*)"HEX PRINT" );

	::fprintf( stderr, "\n\n" );

	//////////////////////////////////////////////////////////////////////////////////////
	///////////////////// CDBaseArithmetic(비트연산 관련 함수) ///////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////

	::fprintf( stderr, "=================== CDBaseArithmetic ===================\n" );

	cChar1	= 'A';
	cChar2	= 'B';

	// cChar1 문자의 오른쪽 4비트(작은 자리수)와 cChar2 문자의 오른쪽 4비트(작은 자리수)를 
	// 합친 문자를 반환
	// cChar1 문자의 4비트는 합쳐지는 문자의 왼쪽 비트(큰 자리수)에 저장
	// cChar2 문자의 4비트는 합쳐지는 문자의 오른쪽 비트(작은 자리수)에 저장
	::fprintf( stderr, "[%-32s][%d]\n", "CDBaseAddTwoByte", CDBaseAddTwoByte( cChar1, cChar2 ) );

	::fprintf( stderr, "\n\n" );

	return 0;
}

