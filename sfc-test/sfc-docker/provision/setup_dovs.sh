#!/usr/bin/env bash
yum -y install epel-release
yum -y install python-pip
pip install --upgrade pip
pushd ../dovs && pip install --upgrade .
