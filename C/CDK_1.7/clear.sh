#!/bin/sh

echo ""
echo ""
echo "--- cd CDBase ---"

cd ./Src/CDBase
make clean


echo ""
echo ""
echo "--- cd CDConfig ---"

cd ../CDConfig
make clean


echo ""
echo ""
echo "--- cd CDIpc ---"

cd ../CDIpc
make clean


echo ""
echo ""
echo "--- cd CDLog ---"

cd ../CDLog
make clean


echo ""
echo ""
echo "--- cd CDSignal ---"

cd ../CDSignal
make clean


echo ""
echo ""
echo "--- cd CDSocket ---"

cd ../CDSocket
make clean


echo ""
echo ""
echo "--- cd CDStructure ---"

cd ../CDStructure
make clean


echo ""
echo ""
echo "--- cd CDThread ---"

cd ../CDThread
make clean


echo ""
echo ""
echo "--- cd Include ---"
echo "/bin/rm ../../Inc/*.h"

cd ../../Inc
/bin/rm *.h


echo ""
echo "--- cd Bin ---"
echo "/bin/rm ../Bin/*.a"

cd ../Bin
/bin/rm *.a


