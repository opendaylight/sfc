#!/usr/bin/env bash

install_requirements() {
  yum install git -y
}

[ -e "/usr/local/sbin/pipework" ] && exit 0

[ ! -d pipework.patches ] &&          \
  echo "Cannot find pipework.patches" \
  exit 1

install_requirements

mkdir -p build && pushd build
git clone https://github.com/jpetazzo/pipework.git
pushd pipework
git reset --hard HEAD
cp ../../pipework.patches/*.patch .
git apply *.patch || exit 1
cp pipework /usr/local/sbin

