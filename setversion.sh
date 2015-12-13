#!/usr/bin/env bash

if [ ! -z ${TRAVIS_TAG} ]
then
    echo "This is a tagged commit! (Tag: ${TRAVIS_TAG}) Injecting version into main class..."
    sed -i.bak "s/\"\*\";\/\/VERSION/\"${TRAVIS_TAG/#v/}\";/" ./src/main/java/net/foxdenstudio/foxguard/FoxGuardMain.java
else
    echo "This is not a tagged commit! Skipping injection..."
fi