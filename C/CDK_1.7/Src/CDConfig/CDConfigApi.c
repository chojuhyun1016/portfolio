#include "CDConfigApi.h"
#include "CDConfigDefinitions.h"

#ifdef  _SOLARIS_
    #include <errno.h>
    #include <fcntl.h>
    #include <unistd.h>
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
*             CD_CONFIG_ERROR(-1) �̸鼭 errno�� EINTR(interrupt�� ���� ����)  *
*             �� ��� �ٽ� open()�� �����Ѵ�.                                  *
*******************************************************************************/
int CDConfigOpen( const char* _cpFile, int _iMode )
{
    int iResult;

    while( ( iResult = open( _cpFile, _iMode ) ) == CD_CONFIG_ERROR && errno == EINTR );

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
*             CD_CONFIG_ERROR(-1) �̸鼭 errno�� EINTR(interrupt�� ���� ����)  *
*             �� ��� �ٽ� close()�� �����Ѵ�.                                 *
*******************************************************************************/
int CDConfigClose( int _iFd )
{
    int iResult;

    while( ( iResult = close( _iFd ) ) == CD_CONFIG_ERROR && errno == EINTR );

    return  iResult;
}
