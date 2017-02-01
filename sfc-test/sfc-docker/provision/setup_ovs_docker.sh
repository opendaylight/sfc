#!/usr/bin/env bash

OVS_VERSION="2.6.1"

install_requirements() {
  yum install -y git
}


[ ! -z `docker images | awk '/^docker-ovs.*yyang / {print $1}'` ] && \
  echo "docker-ovs image already installed" &&                       \
  exit 0

[ -e "build/docker_ovs_yyang-${OVS_VERSION}.tar.gz" ] && \
  echo "docker-ovs image already built" &&
  docker load -i build/docker_ovs_yyang-${OVS_VERSION}.tar.gz &&
  exit 0

[ ! -d docker-ovs.patches ] &&       \
  echo "Cannot find docker-ovs.patches" && \
  exit 1

[ ! -e "build/ovs_packages/openvswitch-${OVS_VERSION}.tar.gz" ] && \
  echo "Cannot find openvswitch-${OVS_VERSION}.tar.gz" &&        \
  exit 1

install_requirements

TMP=$(mktemp -d -t dovs.setup_dovs_docker.XXXXXXXXXX)
ln -s "${PWD}/build/ovs_packages/openvswitch-${OVS_VERSION}.tar.gz" "${TMP}/"
ln -s "${PWD}/docker-ovs.patches" "${TMP}/"

pushd "$TMP"

git clone https://github.com/socketplane/docker-ovs.git
pushd docker-ovs
git reset --hard fede8851e05b984e6f850752d5bc604ac4d7a71c
cp ../docker-ovs.patches/*.patch .
git apply *.patch || exit 1
cp ../openvswitch-${OVS_VERSION}.tar.gz .
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

popd && popd
mkdir -p build && pushd build
docker save docker-ovs:yyang | gzip -c > docker_ovs_yyang-${OVS_VERSION}.tar.gz


