#!/usr/bin/env bash

OVS_VERSION="2.5.90"

install_kernel_devel() {
  if [ ! yum -q info kernel-devel-$(uname -r) &>/dev/null ]
  then
    CENTOS_VERSION="$(grep -Po '\b\d+\.[\d.]+\b' /etc/centos-release)/x86_64"
    PCKG_URL="http://mirror.centos.org/centos/${CENTOS_VERSION}"
    yum localinstall ${PCKG_URL}/{os,updates}/Packages/kernel-devel-$(uname -r).rpm
  else
    yum install kernel-devel-$(uname -r) -y
  fi
}

install_build_requirements() {
  install_kernel_devel
  yum install -y git automake libtool rpm-build openssl-devel \
      python-zope-interface python-twisted-core               \
      PyQt4 python-six desktop-file-utils groff graphviz      \
      selinux-policy-devel python
}

install_requirements() {
  yum install -y python yum-plugin-versionlock
  yum versionlock kernel-$(uname -r) kernel-devel-$(uname -r)
}

build_ovs() {
  [ -e ovs_packages/openvswitch-${OVS_VERSION}-1.el7.centos.x86_64.rpm ]           \
    && [ -e ovs_packages/openvswitch-kmod-${OVS_VERSION}-1.el7.centos.x86_64.rpm ] \
    && return 0

  install_build_requirements

  TMP=$(mktemp -d -t dovs.setup_ovs.XXXXXXXXXX)

  pushd "$TMP"

  git clone https://github.com/openvswitch/ovs.git
  git clone https://github.com/yyang13/ovs_nsh_patches.git

  pushd ovs_nsh_patches && git reset --hard HEAD && popd
  pushd ovs
  git reset --hard 7d433ae57ebb90cd68e8fa948a096f619ac4e2d8
  cp ../ovs_nsh_patches/*.patch ./
  git apply *.patch

  ./boot.sh
  ./configure --disable-shared
  make
  make dist
  make DESTDIR="${PWD}/../install/openvswitch-${OVS_VERSION}" install

  popd && tar xvzf ovs/openvswitch-${OVS_VERSION}.tar.gz
  mkdir -p ~/rpmbuild/SOURCES/
  cp ovs/openvswitch-${OVS_VERSION}.tar.gz cp ~/rpmbuild/SOURCES/
  pushd openvswitch-${OVS_VERSION}
  rpmbuild -bb --without check --without libcapng rhel/openvswitch-fedora.spec
  rpmbuild -bb --without check -D "kversion $(uname -r)" rhel/openvswitch-kmod-fedora.spec

  popd && popd && mkdir -p ovs_packages
  cp ~/rpmbuild/RPMS/x86_64/openvswitch*.rpm ovs_packages/
  tar cvzf ovs_packages/openvswitch-${OVS_VERSION}.tar.gz -C "${TMP}/install" .
}

install_ovs() {
  install_requirements
  yum localinstall ovs_packages/openvswitch-kmod-${OVS_VERSION}-1.el7.centos.x86_64.rpm -y
  yum localinstall ovs_packages/openvswitch-${OVS_VERSION}-1.el7.centos.x86_64.rpm -y
  systemctl enable openvswitch
  systemctl start openvswitch
}

mkdir -p build && pushd build
build_ovs
install_ovs

