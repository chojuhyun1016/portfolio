CC = /opt/SUNWspro/bin/CC

DEFS	= -g
#DEFS	+= -DDEBUG

INCLUDES 	= -I. -I../../Inc

LDIRS 		= -L. -L../../Bin

CFLAGS = $(DEFS) -D_POSIX_THREADS -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

LIBS = -lsocket -lnsl -ldl -lrt -lCDSignal

all: CD_SIGNAL_TUTORIAL CD_SIGNAL_SEND

CD_SIGNAL_TUTORIAL:	Main.o
	$(CC) -o $@ $(CFLAGS) Main.o $(INCLUDES) $(LDIRS) $(LIBS)

CD_SIGNAL_SEND:	Send.o
	$(CC) -o $@ $(CFLAGS) Send.o $(INCLUDES) $(LDIRS) $(LIBS)

Main.o: Main.c
	$(CC) -c Main.c $(CFLAGS) $(INCLUDES) $(LDIRS) $(LIBS)

Send.o: Send.c
	$(CC) -c Send.c $(CFLAGS) $(INCLUDES) $(LDIRS) $(LIBS)

clean:
	rm -f ./*.o
	rm -f ./CD_SIGNAL_TUTORIAL
	rm -f ./CD_SIGNAL_SEND
	rm -f ./core

