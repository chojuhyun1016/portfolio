#ifndef	__LOGGLOBAL_H__
#define	__LOGGLOBAL_H__

#ifdef __cplusplus
extern "C" {
#endif

#define RUN_MODE_TYPE1              1
#define RUN_MODE_TYPE2              2
#define RUN_MODE_TYPE3              3

#define CREATE_FILE_MODE            1
#define DONT_FILE_MODE              2

#define LOG_CFG_FILE_NAME                "LogQCtrl.cfg"
#define Q_INFO_FILE_NAME                 "LogQueueInfo.cfg"

#define LOG_LEVEL_1                  0
#define LOG_LEVEL_2                  1
#define LOG_LEVEL_3                  2
#define MAX_LOG_LEVEL                3

#define LOG_STEP_ALL                 0
#define LOG_STEP_SVC_ERR             1
#define LOG_STEP_ERR                 2
#define LOG_STEP_OFF                 3
#define MAX_LOG_STEP                 4

#define LOG_FORMAT_DAY               0
#define LOG_FORMAT_HOUR              1
#define MAX_LOG_FORMAT               2

#define SMRAT_MODE_OFF               0
#define SMART_MODE_ON                1
#define MAX_SMART_MODE               2

#define FILE_TRC_STR                "trc"
#define FILE_SVC_STR                "svc"
#define FILE_ERR_STR                "err"

#define MAX_LOGTIME_BUFFER          20
#define Q_CHECK_TIME                10

#define LOCAL_MODE_ON               1
#define LOCAL_MODE_OFF              2

#define Q_STAT_ENABLE               1
#define Q_STAT_DISABLE             -1

#define LOG_Q_MODE_ON               1
#define LOG_Q_MODE_OFF              2

#define STR_TEMP_LEN_5              5
#define STR_TEMP_LEN_10             10
#define STR_TEMP_LEN_1024           1024

/*RESULT AND ERROR_CODE*/
#define RTN_NEXT_LINE_FIND          10
#define RTN_END_LINE                5
#define SUCCESS                     1
#define ERR_EMPTY_VALUE             0
#define ERR_GET_Q_KEY               -1
#define ERR_IPC_FAIL                -1
#define ERR_Q_CNT                   -5
#define ERR_INIT_Q_FAIL             -10
#define ERR_GET_Q_CNT               -15
#define ERR_FILE_OPEN               -25
#define ERR_TITLE_LEN               -29
#define ERR_KEY_LEN                 -30
#define ERR_SEND_QUEUE_FAIL         -35
#define ERR_SEND_QUEUE_FULL         -40
#define ERR_FILENAME_SIZE           -45
#define ERR_DESTNUM_SIZE            -50
#define ERR_MSG_SIZE                -55

#ifdef __cplusplus
}
#endif

#endif /* __LOGGLOBAL_H__ */
