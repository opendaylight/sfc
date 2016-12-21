#!/usr/bin/env bash
rpm -Uvh http://epel.mirror.net.in/epel/7/x86_64/e/epel-release-7-8.noarch.rpm
yum -y install python-pip
pip install --upgrade pip
pushd ../dovs && pip install --upgrade .
