CC = /opt/SUNWspro/bin/CC

DEFS	= -g
#DEFS	+= -DDEBUG

INCLUDES 	= -I. -I../../Inc

LDIRS 		= -L. -L../../Bin

CFLAGS = $(DEFS) -D_POSIX_THREADS -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

LIBS = -lsocket -lnsl -ldl -lrt -lCDSocket

all: CD_SOCKET_SERVER_TUTORIAL CD_SOCKET_CLIENT_TUTORIAL

CD_SOCKET_SERVER_TUTORIAL:	server.o
	$(CC) -o $@ $(CFLAGS) server.o $(INCLUDES) $(LDIRS) $(LIBS)

CD_SOCKET_CLIENT_TUTORIAL:	client.o
	$(CC) -o $@ $(CFLAGS) client.o $(INCLUDES) $(LDIRS) $(LIBS)

server.o: server.c
	$(CC) -c server.c $(CFLAGS) $(INCLUDES) $(LDIRS) $(LIBS)

client.o: client.c
	$(CC) -c client.c $(CFLAGS) $(INCLUDES) $(LDIRS) $(LIBS)

clean:
	rm -f ./*.o
	rm -f ./CD_SOCKET_SERVER_TUTORIAL
	rm -f ./CD_SOCKET_CLIENT_TUTORIAL
	rm -f ./core

