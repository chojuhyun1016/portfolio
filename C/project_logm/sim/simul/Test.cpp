#include <stdio.h>
#include <pthread.h>
#include "Test.h"
#include "logqueue.h"


int main(int argc, char **argv)
{
	int	i = 0;
	int y = 0;
	int	iLevel = 0;
	int	iCnt = 0;

	int	iCount = 0;
	int	iMaxCnt = 0;
	int	iFileIndex;

	char	szKeyName[20];

	time_t	tCheckTime;	

	time_t	tStartTime;
	time_t	tEndTime;

	char szmsg[]="SMCI_DELIVER,I,logtime:2008/10/30 16:55:52.123456,exid:00047052,smid:000,dir:1,from:Y1_6_MO_2,to:loopback10,ms:delivered,reason:000,act:00,tryno:01,exptime:2008/10/31 16:55:51,nexttime:2008/10/30 16:56:01,subtime:2008/10/30 16:55:51,d:01029500417,o:01001994000,c:01001994000,frd:,fdd:,rd:01029000000,imsi:,tid:4098,smern:00344975034,msgsn:00000047052,msstat:,ov:000000,tv:000000,smlen:107,dtype:1";
	char szDestNum[12] ={0,};

	if (argc < 4)
	{
		fprintf( stderr, "[인자값의 개수가 정확하지 않습니다.][L:%d]\n", __LINE__ );
		fprintf( stderr, "[Ex: (%s) [LOG TYPE][SEC COUNT][MSG COUNT][L:%d]\n", argv[0], __LINE__ );

		return -1;
	}

	fprintf( stderr, "[Command: %s %s %s %s][L:%d]\n", argv[0], argv[1], argv[2], argv[3], __LINE__ );

	iLevel = atoi( argv[1] );
	iMaxCnt = atoi( argv[2] );
	iCnt = atoi( argv[3] );

	fprintf( stderr, "[SMART MSG 생성시작][L:%d]\n", __LINE__ );

	tStartTime	= tCheckTime	= time(NULL);

	sprintf( szKeyName,"LOG_MANAGER_SIMUL_01" );

	jloggerwriter_construct( LOG_HOME_DIR, LOG_HOME_DIR, PROCESS_NAME, CENTER_TYPE, LOG_VER, szKeyName, SVC_LOG_DIR, TRC_LOG_DIR, ERR_LOG_DIR, 0x0010, "/export/home/sms4/LOGM/simul" );

	for( i = 1, y = 1, iFileIndex = 1; i < iCnt + 1; ++i, ++y )
	{
		if( time(NULL) != tCheckTime )
		{
			iCount = 0;

			tCheckTime = time(NULL);
		}

		if( iCount >= iMaxCnt ) 
		{
			usleep( 10000 );

			i = i -1;

			continue;
		}

		//sprintf( szDestNum,"010%02d123456", (i % 16) +1);
		//sprintf( szDestNum,"01071%06d", y );
		//sprintf( szDestNum,"#5001" );
		sprintf( szDestNum,"010%d%07d", y % 4, y );

		LOG( iLevel, szDestNum,"%s", szmsg );

		if( ( i % 16 ) == 0 )
		{
			if( iFileIndex > 16)
				iFileIndex	= 1;

			sprintf( szKeyName,"LOG_MANAGER_SIMUL_%02d", iFileIndex );

			jloggerwriter_destruct();
			jloggerwriter_construct( LOG_HOME_DIR, LOG_HOME_DIR, PROCESS_NAME, CENTER_TYPE, LOG_VER, szKeyName, SVC_LOG_DIR, TRC_LOG_DIR, ERR_LOG_DIR, 0x0010, "/export/home/sms4/LOGM/simul" );

			++iFileIndex;
		}

		++iCount;
	}

	jloggerwriter_destruct();

	tEndTime	= time( NULL );

	fprintf( stderr, " [%d개 MSG 생성완료]\n", iCnt );

	if( tEndTime == tStartTime )
	{
		fprintf( stderr, "[소요시간: %d][초당처리건수: %d][L:%d]\n", 
			tEndTime - tStartTime, 0, __LINE__ );	
	}
	else
	{
		fprintf( stderr, "[소요시간: %d][초당처리건수: %d][L:%d]\n", 
			tEndTime - tStartTime, iCnt / (tEndTime - tStartTime), __LINE__ );
	}

}
