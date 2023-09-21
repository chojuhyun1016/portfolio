#!/bin/sh

echo ""
echo ""
echo "--- cd CNBase ---"

cd ./Src/CNBase
make clean


echo ""
echo ""
echo "--- cd CNConfig ---"

cd ../CNConfig
make clean


echo ""
echo ""
echo "--- cd CNIpc ---"

cd ../CNIpc
make clean


echo ""
echo ""
echo "--- cd CNLog ---"

cd ../CNLog
make clean


echo ""
echo ""
echo "--- cd CNSignal ---"

cd ../CNSignal
make clean


echo ""
echo ""
echo "--- cd CNSocket ---"

cd ../CNSocket
make clean


echo ""
echo ""
echo "--- cd CNThread ---"

cd ../CNThread
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


