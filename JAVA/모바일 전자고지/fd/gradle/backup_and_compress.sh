#!/bin/sh

NOW=`date +%Y%m%d_%H%M%S`

PROJECT_NAME=$1
GRADLE_PATH=/data1/mps/.gradle

echo NOW : ${NOW}
echo PROJECT_NAME  : ${PROJECT_NAME}
echo GRADLE_PATH : ${GRADLE_PATH}

mv ${GRADLE_PATH}/archive/${PROJECT_NAME}.tar.gz ${GRADLE_PATH}/backup/${PROJECT_NAME}.tar.gz.${NOW}

cd ${GRADLE_PATH}

tar -cvzf ${PROJECT_NAME}.tar.gz ${PROJECT_NAME}

mv ${GRADLE_PATH}/${PROJECT_NAME}.tar.gz ${GRADLE_PATH}/archive
