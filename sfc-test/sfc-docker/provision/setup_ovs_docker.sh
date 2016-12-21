#!/usr/bin/env bash

install_requirements() {
  yum install -y git
}

[ ! -z `docker images | awk '/^docker-ovs:yy / {print $1}'` ] && \
  echo "docker-ovs image already built" &&                      \
  exit 0

[ ! -d docker-ovs.patches ] &&       \
  echo "Cannot find docker-ovs.patches" && \
  exit 1

[ ! -e build/ovs_packages/openvswitch-2.5.90.tar.gz ] && \
  echo "Cannot find openvswitch-2.5.90.tar.gz" &&                  \
  exit 1

install_requirements

mkdir -p build && pushd build
git clone https://github.com/socketplane/docker-ovs.git
pushd docker-ovs
git reset --hard fede8851e05b984e6f850752d5bc604ac4d7a71c
cp ../../docker-ovs.patches/*.patch .
git apply *.patch || exit 1
cp ../ovs_packages/openvswitch-2.5.90.tar.gz .
mkdir host_libs
cp /usr/lib64/libcrypto.so.10 host_libs/
cp /usr/lib64/libssl.so.10 host_libs/
cp /usr/lib64/libgssapi_krb5.so.2 host_libs/
cp /usr/lib64/libkrb5.so.3 host_libs/
cp /usr/lib64/libcom_err.so.2 host_libs/
cp /usr/lib64/libk5crypto.so.3 host_libs/
cp /usr/lib64/libkrb5support.so.0 host_libs/
cp /usr/lib64/libkeyutils.so.1 host_libs/
cp /usr/lib64/libselinux.so.1 host_libs/
cp /usr/lib64/libpcre.so.1 host_libs/
cp /usr/lib64/liblzma.so.5 host_libs/
docker build -t docker-ovs:yyang .

