#ifndef	__LOGQUEUE_H__
#define	__LOGQUEUE_H__

#ifdef __cplusplus
extern "C" {
#endif

int LogQInit(int iProcType);
int LogQReSet();
int LogQMsgSend(char* szfilename,char* szDestNum,char* ptrMsg,int iLocalMode);
int LogQGetProcType();
void LogQDestroy();

#ifdef __cplusplus
}
#endif

#endif /* __LOGQUEUE_H__ */
