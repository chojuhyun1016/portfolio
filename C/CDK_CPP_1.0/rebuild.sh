#!/bin/sh

echo ""
echo ""
echo "--- cd CNBase ---"

cd ./Src/CNBase
make clean
make


echo ""
echo ""
echo "--- cd CNConfig ---"

cd ../CNConfig
make clean
make


echo ""
echo ""
echo "--- cd CNIpc ---"

cd ../CNIpc
make clean
make


echo ""
echo ""
echo "--- cd CNLog ---"

cd ../CNLog
make clean
make


echo ""
echo ""
echo "--- cd CNSignal ---"

cd ../CNSignal
make clean
make


echo ""
echo ""
echo "--- cd CNSocket ---"

cd ../CNSocket
make clean
make


echo ""
echo ""
echo "--- cd CNThread ---"

cd ../CNThread
make clean
make


