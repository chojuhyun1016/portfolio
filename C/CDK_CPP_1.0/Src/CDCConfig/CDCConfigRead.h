#ifndef __CDC_GET_FILE_CONFIG_H__
#define __CDC_GET_FILE_CONFIG_H__


#ifndef CDC_CONFIG_BUF_SIZE
#define CDC_CONFIG_BUF_SIZE			1024
#endif

#ifndef CDC_CONFIG_TOKEN_SIZE
#define CDC_CONFIG_TOKEN_SIZE		1024
#endif

#ifndef CDC_CONFIG_NO_FORMAT
#define	CDC_CONFIG_NO_FORMAT		-1
#endif

#ifndef CDC_CONFIG_NO_FILE
#define CDC_CONFIG_NO_FILE			-2
#endif

#ifndef CDC_CONFIG_NO_PART
#define CDC_CONFIG_NO_PART			-3
#endif

#ifndef CDC_CONFIG_NO_SPART
#define CDC_CONFIG_NO_SPART			-4
#endif

#ifndef CDC_CONFIG_NO_DECIMAL
#define CDC_CONFIG_NO_DECIMAL		-5
#endif


namespace CDCConfig
{

class CDCFile
{
	public:
		CDCFile();
		CDCFile( char* _cpFileName );
		CDCFile( char* _cpFileName, char* _cpFilePath );

		~CDCFile();

		bool	Set( char* _cpFileName );
		bool	Set( char* _cpFileName, char* _cpFilePath );

		bool	GetInt( char* _cpPart, char* _cpSPart, int* _ipBuf );
		bool	GetStr( char* _cpPart, char* _cpSPart, char* _cpBuf, int _iSize );

	private:
		char	*m_cpFile;

	private:
		int	CDReadConfig( char* _cpPart, char* _cpSPart, char* _cpBuf, int _iSize );
		char* CDConfigMakeToken( char* _cpFile, char* _cpOffset, char* _cpBuf, int _iSize );

		int CDConfigStrToDec( char* _cpStr );
		int CDConfigHexConverter( char* _cpStr );
		int CDConfigDecConverter( char* _cpStr );

		int CDCConfigReadLine( int _iFd, char* _cpBuf, int _iSize );
}

}

#endif

