#ifndef	__LOGCONFIG_H__
#define	__LOGCONFIG_H__

#ifdef __cplusplus
extern "C" {
#endif

int SetCfgDir(char* szCfgDir);
int GetLogQMode();
int GetLogLevel(int iFileType,char* pProcName);
int GetLogStep(char* pProcName);
int GetLogFormat(char* pProcName);
int GetSmartMode(int iFileType,char* pProcName);
int GetConfig(char* pTitle,char* pFileName,char* pKey, char* pValue);

#ifdef __cplusplus
}
#endif

#endif /* __LOGCONFIG_H__ */
