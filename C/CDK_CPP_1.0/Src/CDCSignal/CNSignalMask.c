#include "CNSignalMask.h"
#include "CNSignalDefinitions.h"


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, struct sigset_t*, struct sigset_t*                          *
*             _iHow      : �ñ׳� ����ũ�� ó�� ����� ����(����, �߰�, ����)  * 
*             _stpNewSig : _iHow�� ���� ���� �� ����ũ ������ ����ü ������    *
*             _stpOldSig : ������ ��� ���ִ� ����ũ ������ ���� �� ����ü     *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe and Async-Signal-Safe                                    *
* Explain   : pthread_sigmask() �Լ��� ���� �Լ��̴�. �������� �ñ׳� ����ŷ   *
*             ����Ʈ�� ���ο� ����Ʈ(_stpNewSig)�� ���� �ϸ� ������            *
*             ����ŷ ����Ʈ�� _stpOldSig �����Ѵ�.                             *
*******************************************************************************/
int CNSignalMask( int _iHow, sigset_t* _stpNewSig, sigset_t* _stpOldSig )
{
	return	pthread_sigmask( _iHow, _stpNewSig, _stpOldSig );
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : struct sigset_t*                                                 *
*             _stpSigTemp : ������ ��� ���ִ� ����ũ ������ ���� �� ����ü    *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : �����尡 ��� �ñ׳� ����ŷ �ϵ��� �����ϸ� ������ ����ŷ        *
*             ����Ʈ�� _stpSigTemp�� ����ȴ�.                                 *
*******************************************************************************/
int CNAllSignalBlock( sigset_t* _stpSigTemp )
{
	sigset_t	stSigSet;

	if( sigfillset( &stSigSet ) == CN_SIGNAL_ERROR )
		return	-1;

	if( pthread_sigmask( SIG_SETMASK, &stSigSet, _stpSigTemp ) != CN_SIGNAL_SUCCESS )
		return	CN_SIGNAL_ERROR;

	return	CN_SIGNAL_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : ����                                                             *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : �������� ��� ����ŷ �� �ñ׳��� �����ȴ�.                       *
*******************************************************************************/
int CNAllSignalUnBlock()
{
	sigset_t	stSigSet;

	if( sigemptyset( &stSigSet ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	if( pthread_sigmask( SIG_SETMASK, &stSigSet, NULL ) != CN_SIGNAL_SUCCESS )
		return	CN_SIGNAL_ERROR;

	return	CN_SIGNAL_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int                                                              *
*             _iSig : ���� �� �ñ׳� ��ȣ                                      *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : �����尡 �ش� �ñ׳�(_iSig)�� ����ŷ �ϵ��� ����ŷ ����Ʈ�� �߰� *
*******************************************************************************/
int CNSignalBlock( int _iSig )
{
	sigset_t	stSigSet;

	if( _iSig <= 0 )
		return	CN_SIGNAL_ERROR;

	if( sigemptyset( &stSigSet ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	if( sigaddset( &stSigSet, _iSig ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	if( pthread_sigmask( SIG_BLOCK, &stSigSet, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	return	CN_SIGNAL_SUCCESS;
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int                                                              *
*             _iSig : ����ŷ ���� �� �ñ׳��� ��ȣ                             *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : �����尡 �ش� �ñ׳�(_iSig)�� �޾Ƶ��̵��� ����ŷ�� �����Ѵ�.    *
*******************************************************************************/
int CNSignalUnBlock( int _iSig )
{
	sigset_t stSigSet;

	if( _iSig <= 0 )
		return	CN_SIGNAL_ERROR;

	if( sigemptyset( &stSigSet ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	if( sigaddset( &stSigSet, _iSig ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	if( pthread_sigmask( SIG_UNBLOCK, &stSigSet, NULL ) == CN_SIGNAL_ERROR )
		return	CN_SIGNAL_ERROR;

	return	CN_SIGNAL_SUCCESS;
}
