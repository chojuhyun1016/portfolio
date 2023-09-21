CC = /opt/SUNWspro/bin/CC

DEFS	= -g
#DEFS	+= -DDEBUG

INCLUDES 	= -I. -I../../Inc

LDIRS 		= -L. -L../../Bin

CFLAGS = $(DEFS) -D_POSIX_THREADS -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

LIBS = -lsocket -lnsl -ldl -lrt -lCDIpc

all: CD_IPC_QUEUE_TUTORIAL CD_IPC_ST_QUEUE_TUTORIAL

CD_IPC_QUEUE_TUTORIAL:	Queue.o
	$(CC) -o $@ $(CFLAGS) Queue.o $(INCLUDES) $(LDIRS) $(LIBS)

CD_IPC_ST_QUEUE_TUTORIAL:	StQueue.o
	$(CC) -o $@ $(CFLAGS) StQueue.o $(INCLUDES) $(LDIRS) $(LIBS)

Queue.o: Queue.c
	$(CC) -c Queue.c $(CFLAGS) $(INCLUDES) $(LDIRS) $(LIBS)

StQueue.o: StQueue.c
	$(CC) -c StQueue.c $(CFLAGS) $(INCLUDES) $(LDIRS) $(LIBS)

clean:
	rm -f ./*.o
	rm -f ./CD_IPC_QUEUE_TUTORIAL
	rm -f ./CD_IPC_ST_QUEUE_TUTORIAL
	rm -f ./core

