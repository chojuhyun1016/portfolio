#include "CDLogApi.h"
#include "CDLogDefinitions.h"

#ifdef  _SOLARIS_
    #include <errno.h>
    #include <sys/types.h>
    #include <sys/stat.h>
    #include <fcntl.h>
#elif _CENT_OS_
    #include <errno.h>
    #include <fcntl.h>
    #include <unistd.h>
#else
    #include <errno.h>
    #include <fcntl.h>
    #include <unistd.h>
#endif


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : char*, int                                                       *
*             _cpFile : ���� ��(���ϰ�� + �����̸�)                           *
*             _iMode  : ���� ���� ���                                         *
*                                                                              *
* Return    : int, ����(File Descriptor), ����(-1)                             *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : System Call open() �Լ��� �����Ѱ��̴�.                          *
*             Interrupt�� ������ ���� �ʵ��� �ϱ����� open()�Լ��� ���ϰ���    *
*             CD_LOG_ERROR(-1) �̸鼭 errno�� EINTR(interrupt�� ���� ����)     *
*             �� ��� �ٽ� open()�� �����Ѵ�.                                  *
*******************************************************************************/
int CDLogFileOpen( const char* _cpFile, int _iMode )
{
    int iResult;

    while( ( iResult = open( _cpFile, _iMode, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH ) ) == CD_LOG_ERROR && errno == EINTR );

    return  iResult;
}


/*******************************************************************************
* Update    : 2010/10/05                                                       *
* Argument  : int                                                              *
*             _iFd : �������� ������ ���� ��ũ����                           *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe, MT-Safe                                       *
* Explain   : System Call close() �Լ��� �����Ѱ��̴�.                         *
*             Interrupt�� ������ ���� �ʵ��� �ϱ����� close()�Լ��� ���ϰ���   *
*             CD_LOG_ERROR(-1) �̸鼭 errno�� EINTR(interrupt�� ���� ����)     *
*             �� ��� �ٽ� close()�� �����Ѵ�.                                 *
*******************************************************************************/
int CDLogFileClose( int _iFd )
{
    int iResult;

    while( ( iResult = close( _iFd ) ) == CD_LOG_ERROR && errno == EINTR );

    return  iResult;
}
