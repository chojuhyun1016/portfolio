#include "CNSignalMask.h"
#include "CNSignalDefinitions.h"


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : int, struct sigset_t*, struct sigset_t*                          *
*             _iHow      : 시그널 마스크를 처리 방식을 결정(변경, 추가, 제거)  * 
*             _stpNewSig : _iHow를 통해 적용 될 마스크 데이터 구조체 포인터    *
*             _stpOldSig : 기존에 등록 되있던 마스크 정보를 저장 할 구조체     *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : MT-Safe and Async-Signal-Safe                                    *
* Explain   : pthread_sigmask() 함수의 랩핑 함수이다. 쓰레드의 시그널 블러킹   *
*             리스트를 새로운 리스트(_stpNewSig)로 변경 하며 기존의            *
*             블러킹 리스트를 _stpOldSig 저장한다.                             *
*******************************************************************************/
int CNSignalMask( int _iHow, sigset_t* _stpNewSig, sigset_t* _stpOldSig )
{
	return	pthread_sigmask( _iHow, _stpNewSig, _stpOldSig );
}


/*******************************************************************************
* Update    : 2010/10/01                                                       *
* Argument  : struct sigset_t*                                                 *
*             _stpSigTemp : 기존에 등록 되있던 마스크 정보를 저장 할 구조체    *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 쓰레드가 모든 시그널 블러킹 하도록 셋팅하며 기존의 블러킹        *
*             리스트는 _stpSigTemp에 저장된다.                                 *
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
* Argument  : 없음                                                             *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 쓰레드의 모든 블러킹 된 시그널이 해제된다.                       *
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
*             _iSig : 블럭 될 시그널 번호                                      *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 쓰레드가 해당 시그널(_iSig)을 블러킹 하도록 블러킹 리스트에 추가 *
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
*             _iSig : 블러킹 해제 될 시그널의 번호                             *
*                                                                              *
* Return    : int, 성공(0), 실패(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : 쓰레드가 해당 시그널(_iSig)을 받아들이도록 블러킹을 해제한다.    *
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

