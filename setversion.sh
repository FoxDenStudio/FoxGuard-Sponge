#!/usr/bin/env bash

if [ -n ${TRAVIS_TAG} ]
then
    echo "This is a tagged commit! (Tag: ${TRAVIS_TAG}) Injecting version into main class..."
    sed -i.bak "s/\"SNAPSHOT\"/\"${TRAVIS_TAG/#v/}\"/" ./src/main/java/net/gravityfox/foxguard/FoxGuardMain.java
else
    echo "This is not a tagged commit! Skipping injection..."
fi