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
	//////////////////////////// CDBaseTime(�ð� ���� �Լ�) //////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////

	::fprintf( stderr, "\n\n" );
	::fprintf( stderr, "=================== CDBaseTime ===================\n" );

	::memset( caBuffer1, 0x00, sizeof( caBuffer1 ) );
	::memset( caBuffer2, 0x00, sizeof( caBuffer2 ) );
	::memset( caBuffer3, 0x00, sizeof( caBuffer3 ) );

	// ���� �ð��� ��Ÿ���� ���ڿ��� ����(caBuffer1)�� ���ڿ��� ����
	// ���ڿ� ������ YYYYMMDDhhmmss 
	CDBaseGetNowTimeStr( caBuffer1 );
	::fprintf( stderr, "[%-32s][%s]\n", "CDBaseGetNowTimeStr", caBuffer1 );

	// ���� �ð��� ������ ����(YYYY/MM/DD/hh:mm:ss)���� ����(caBuffer2)�� ���ڿ��� ����
	// YYYY, MM, DD, hh, mm, ss ���ڿ��� �ð����� �ν��ϸ�
	// ���ʴ�� �־����� �Ѵ�. �� �̿ܿ� ���ڴ� �״�� ���
	// Ex) "YYYY/MM/DD/hh:mm:ss" = "2013/01/01/23:59:59"
	// Ex) "MM/DD/hh:mm:ss" = "01/01/23:59:59"
	// Ex) "YYYY/MM/DD/hh:mm" = "2013/01/01/23:59"
	CDBaseGetNowTimeFormatStr( caBuffer2, (char*)"YYYY/MM/DD/hh:mm:ss" );
	::fprintf( stderr, "[%-32s][%s]\n", "CDBaseGetNowTimeFormatStr", caBuffer2 );

	// �־��� �ð�(tNowTime1)�� ������ ����(YYYYMMDDhhmmss)���� ����(caBuffer3)�� ���ڿ��� ����
	// YYYY, MM, DD, hh, mm, ss ���ڿ��� �ð����� �ν��ϸ�
	// ���ʴ�� �־����� �Ѵ�. �� �̿ܿ� ���ڴ� �״�� ���
	// Ex) "YYYYMMDDhhmmss" = "20130101235959"
	// Ex) "MMDDhhmmss" = "0101235959"
	// Ex) "YYYYMMDDhhmm" = "201301012359"
	tNowTime1	= ::time( NULL );
	CDBaseTimeToFormatStr( caBuffer3, tNowTime1, (char*)"YYYYMMDDhhmmss" );
	::fprintf( stderr, "[%-32s][%s]\n", "CDBaseTimeToStr", caBuffer3 );

	// �־��� �ð����ڿ�(caBuffer3)�� ������ ����(YYYYMMDDhhmmss)���� ����(caBuffer3)�� ���ڿ��� ����
	// YYYY, MM, DD, hh, mm, ss ���ڿ��� �ð����� �ν��ϸ�
	// ���ʴ�� �־����� �Ѵ�. �� �̿ܿ� ���ڴ� �״�� ���
	// Ex) "YYYYMMDDhhmmss" = "20130101235959"
	// Ex) "MMDDhhmmss" = "0101235959"
	// Ex) "YYYYMMDDhhmm" = "201301012359"
	tNowTime2	= CDBaseFormatStrToTime( caBuffer3, (char*)"YYYYMMDDhhmmss" );
	::fprintf( stderr, "[%-32s][%d]\n", "CDBaseStrToTime", tNowTime2 );

	::fprintf( stderr, "\n\n" );

	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////// CDBaseStrings(���ڿ� ���� �Լ�) /////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////

	::fprintf( stderr, "=================== CDBaseStrings ===================\n" );

	::memset( caBuffer1, 0x00, sizeof( caBuffer1 ) );
	::memset( caBuffer2, 0x00, sizeof( caBuffer2 ) );
	::memset( caBuffer3, 0x00, sizeof( caBuffer3 ) );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrLeftTrim", caBuffer1 );
	// ���ڿ�(caBuffer1) ������ ����,��,������ �� ���� �� ���ڿ���
	// ���� �������� ������
	CDBaseStrLeftTrim( caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrLeftTrim", caBuffer1 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	// ���ڿ�(caBuffer1) ������ ����,��,������ �� ������ ���ڿ���
	// ����(caBuffer2)�� ����
	CDBaseStrLeftTrimToBuffer( caBuffer2, caBuffer1 );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrLeftTrimToBuffer", caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrLeftTrimToBuffer", caBuffer2 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrRightTrim", caBuffer1 );
	// ���ڿ�(caBuffer1) �������� ����,��,������ �� ����
	CDBaseStrRightTrim( caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrRightTrim", caBuffer1 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	// ���ڿ�(caBuffer1) �������� ����,��,������ �� ������ ���ڿ���
	// ����(caBuffer2)�� ����
	CDBaseStrRightTrimToBuffer( caBuffer2, caBuffer1 );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrRightTrimToBuffer", caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrRightTrimToBuffer", caBuffer2 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrTrim", caBuffer1 );
	// ���ڿ�(caBuffer1) �翷(������, ����)�� ����,��,������ �� ����
	CDBaseStrTrim( caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrTrim", caBuffer1 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrTrimToBuffer", caBuffer1 );
	// ���ڿ�(caBuffer1) ����(������,����)�� ����,��,������ �� ������ ���ڿ���
	// ����(caBuffer2)�� ����
	CDBaseStrTrimToBuffer( caBuffer2, caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrTrimToBuffer", caBuffer2 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrSub", caBuffer1 );
	// ����(caBuffer1)�� ���ڿ����� ������ ���ڿ� ������(1)���� ������ ����(3)�� ���ڸ� �����ؼ�
	// �ٽ� ���ۿ� ����(caBuffer1), ���� ���ڿ��� ����
	CDBaseStrSub( caBuffer1, 1, 3 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrSub", caBuffer1 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrSubToBuffer", caBuffer1 );
	// ����(caBuffer1)�� ���ڿ����� ������ ���ڿ� ������(1)���� ������ ����(3)�� ���ڸ� �����ؼ�
	// ���ۿ� ����(caBuffer2)
	CDBaseStrSubToBuffer( caBuffer2, caBuffer1, 2, 4 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrSubToBuffer", caBuffer2 );

	//::strlcpy( caBuffer1, (char*)"  abcd  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrUp", caBuffer1 );
	// ����(caBuffer1)���� ���ڿ��� �ҹ��� ���ĺ��� ���� �� ���
	// �빮�� ���ĺ����� ����
	CDBaseStrUp( caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrUp", caBuffer1 );

	//::strlcpy( caBuffer1, (char*)"  abcd  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrUpToBuffer", caBuffer1 );
	// ���ڿ�(caBuffer1)�� ����(caBuffer2)�� ���� �� ���ڿ��� �ҹ��ڰ�
	// ���� �� ��� �빮�� ���ĺ����� �����Ͽ� ����(caBuffer2)�� ����
	CDBaseStrUpToBuffer( caBuffer2, caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrUpToBuffer", caBuffer2 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrDown", caBuffer1 );
	// ����(caBuffer1)���� ���ڿ��� �빮�� ���ĺ��� ���� �� ���
	// �ҹ��� ���ĺ����� ����
	CDBaseStrDown( caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrDown", caBuffer1 );

	//::strlcpy( caBuffer1, (char*)"  ABCD  ", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrDownToBuffer", caBuffer1 );
	// ���ڿ�(caBuffer1)�� ����(caBuffer2)�� ���� �� ���ڿ��� �빮�ڰ�
	// ���� �� ��� �ҹ��� ���ĺ����� �����Ͽ� ����(caBuffer2)�� ����
	CDBaseStrDownToBuffer( caBuffer2, caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrDownToBuffer", caBuffer2 );

	//::strlcpy( caBuffer1, (char*)"123456789", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseStrToDec", caBuffer1 );
	::fprintf( stderr, "[After ][%-32s][%d]\n", "CDBaseStrToDec", CDBaseStrToDec( caBuffer1 ) );

	::fprintf( stderr, "\n\n" );

	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////// CDBaseDebug(������� ���� �Լ�) /////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////

	::fprintf( stderr, "=================== CDBaseDebug ===================\n" );

	::memset( caBuffer1, 0x00, sizeof( caBuffer1 ) );
	::memset( caBuffer2, 0x00, sizeof( caBuffer2 ) );
	::memset( caBuffer3, 0x00, sizeof( caBuffer3 ) );
	::memset( caBuffer4, 0x00, sizeof( caBuffer4 ) );

	//::strlcpy( caBuffer1, (char*)"ABCDEFG 123456789", sizeof( caBuffer1 ) );
	::fprintf( stderr, "[Before][%-32s][%s]\n", "CDBaseHexConvert", caBuffer1 );
	// ����(caBuffer1)����  ������ ����(17)�� �����͸� ����(10��) ���·�
	// ����(caBuffer4)�� ���
	CDBaseHexConvert( caBuffer4, caBuffer1, 17, (char*)"HEX CONVERT" );
	::fprintf( stderr, "[After ][%-32s][%s]\n", "CDBaseStrDownToBuffer", caBuffer4 );

	// ����(caBuffer1)���� ������ ����(17)�� �����͸� ����(10��) ���·�
	// ȭ�鿡 ���
	CDBaseHexPrint( caBuffer1, 17, (char*)"HEX PRINT" );

	::fprintf( stderr, "\n\n" );

	//////////////////////////////////////////////////////////////////////////////////////
	///////////////////// CDBaseArithmetic(��Ʈ���� ���� �Լ�) ///////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////

	::fprintf( stderr, "=================== CDBaseArithmetic ===================\n" );

	cChar1	= 'A';
	cChar2	= 'B';

	// cChar1 ������ ������ 4��Ʈ(���� �ڸ���)�� cChar2 ������ ������ 4��Ʈ(���� �ڸ���)�� 
	// ��ģ ���ڸ� ��ȯ
	// cChar1 ������ 4��Ʈ�� �������� ������ ���� ��Ʈ(ū �ڸ���)�� ����
	// cChar2 ������ 4��Ʈ�� �������� ������ ������ ��Ʈ(���� �ڸ���)�� ����
	::fprintf( stderr, "[%-32s][%d]\n", "CDBaseAddTwoByte", CDBaseAddTwoByte( cChar1, cChar2 ) );

	::fprintf( stderr, "\n\n" );

	return 0;
}
