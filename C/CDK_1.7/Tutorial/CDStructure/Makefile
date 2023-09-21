CC = /opt/SUNWspro/bin/CC

DEFS	= -g
#DEFS	+= -DDEBUG

INCLUDES 	= -I. -I../../Inc

LDIRS 		= -L. -L../../Bin

CFLAGS = $(DEFS) -D_POSIX_THREADS -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

LIBS = -lsocket -lnsl -ldl -lrt -lCDStructure

all: CD_STRUCTURE_TUTORIAL

CD_STRUCTURE_TUTORIAL:	Main.o
	$(CC) -o $@ $(CFLAGS) Main.o $(INCLUDES) $(LDIRS) $(LIBS)

Main.o: Main.c
	$(CC) -c Main.c $(CFLAGS) $(INCLUDES) $(LDIRS) $(LIBS)

clean:
	rm -f ./*.o
	rm -f ./CD_STRUCTURE_TUTORIAL
	rm -f ./core

