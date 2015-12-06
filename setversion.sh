#!/usr/bin/env bash

if [ -n ${TRAVIS_TAG} ]
then
    sed -i.bak "s/\"SNAPSHOT\"/\"${TRAVIS_TAG/#v/}\"/" ./src/main/java/net/gravityfox/foxguard/FoxGuardMain.java
fi