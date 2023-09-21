#ifndef __CD_STRUCTURE_QUEUE_H__
#define __CD_STRUCTURE_QUEUE_H__

#include "CDStructureDefinitions.h"

#ifdef  _SOLARIS_
    #include <pthread.h>
#elif _CENT_OS_
    #include <pthread.h>
#else
    #include <pthread.h>
#endif


#ifdef __cplusplus
extern "C" {
#endif

#ifndef CD_STRUCTURE_QUEUE_FULL
#define CD_STRUCTURE_QUEUE_FULL		1
#endif

#ifndef CD_STRUCTURE_QUEUE_EMPTY
#define CD_STRUCTURE_QUEUE_EMPTY	2
#endif

#ifndef CD_STRUCTURE_QUEUE_POP
#define CD_STRUCTURE_QUEUE_POP		3
#endif

#ifndef CD_STRUCTURE_QUEUE_PUSH
#define CD_STRUCTURE_QUEUE_PUSH		4
#endif


typedef struct _stCDStructQueueContainer
{
	int		type;

	void	*data;

	struct _stCDStructQueueContainer	*next;
	struct _stCDStructQueueContainer	*prev;

} stCDStructQueueContainer;


typedef struct _stCDStructureQueueNode
{
	int				m_reference;

	stCDStructQueueContainer		m_head;
	stCDStructQueueContainer		m_tail;

} stCDStructureQueueNode;


typedef struct _stCDStructureQueue
{
	pthread_mutex_t	m_iLock;

	stCDStructureQueueNode		m_stOccupiedList;
	stCDStructureQueueNode		m_stIdleList;

} stCDStructureQueue;


stCDStructureQueue* CDStructureQueueMake();

int CDStructureQueueInit( stCDStructureQueue* _stpQueue, int _iPrepare );
void CDStructureQueueFree( stCDStructureQueue** _stppQueue );

int CDStructureQueueAppend( stCDStructureQueue* _stpQueue, int _iAppendCount );
int CDStructureQueueDelete( stCDStructureQueue* _stpQueue, int _iDeleteCount );

int CDStructureQueuePush( stCDStructureQueue* _stpQueue, int _iType, void* _vpData );
int CDStructureQueuePop( stCDStructureQueue* _stpQueue, int* _ipType, void** _vppData );

int CDStructureQueueGetUseCount( stCDStructureQueue* _stpQueue );
int CDStructureQueueGetIdleCount( stCDStructureQueue* _stpQueue );

static void CDStructQueueInitLinkedList( stCDStructureQueueNode* _stpNode );

static int CDStructQueueAppendTailLinkedList( stCDStructureQueueNode* _stpNode );
static int CDStructQueueRemoveTailLinkedList( stCDStructureQueueNode* _stpNode );

static stCDStructQueueContainer* CDStructQueuePushLinkedList( stCDStructureQueueNode* _stpNode, stCDStructQueueContainer* _stpData );
static stCDStructQueueContainer* CDStructQueuePopLinkedList( stCDStructureQueueNode* _stpNode );

static void CDStructQueueDeleteAllLinkedList( stCDStructureQueueNode* _stpNode );
static stCDStructQueueContainer* CDStructQueueDeleteLinkedList( stCDStructureQueueNode* _stpNode, stCDStructQueueContainer* _stpDeleteNode );


#ifdef __cplusplus
}
#endif

#endif

