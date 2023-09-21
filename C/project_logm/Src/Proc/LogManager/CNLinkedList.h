#ifndef __CN_LINKED_LIST_H__
#define __CN_LINKED_LIST_H__ 

#include <time.h>

#ifdef __cplusplus
extern "C" {
#endif

#define MAX_CONTAINER_DATA_SIZE			1024


typedef struct _stContainer 
{
	int		iSequence;
	int		iRequestCount;

	time_t	tLastActTime;

	char	data[MAX_CONTAINER_DATA_SIZE];

	struct _stContainer	*next;
	struct _stContainer	*prev;
} stContainer;


typedef struct _stNode
{
	int				m_reference;

	stContainer		m_head;
	stContainer		m_tail;
} stNode;


void CNInitList( stNode* _stpNode );
stContainer* CNPushList( stNode* _stpNode, char* _cpPut, int _iPutSize, int _iSequence );
stContainer* CNDeleteList( stNode* _stpNode, stContainer* _stDelNode );
stContainer* CNSerachData( stNode* stpNode, int _iSequence );
void CNDeleteAllList( stNode* _stpNode );
void CNDisplayList( stNode* stpNode );
int CNGetListCnt( stNode* _stpNode );


#ifdef __cplusplus
}
#endif

#endif

