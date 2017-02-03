#!/usr/bin/env bash

OVS_VERSION="2.6.1"

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
      python-six desktop-file-utils groff graphviz            \
      selinux-policy-devel python
}

install_requirements() {
  yum install -y python yum-plugin-versionlock
  yum versionlock kernel-$(uname -r) kernel-devel-$(uname -r)
}

build_ovs() {
  [ -e "ovs_packages/openvswitch-${OVS_VERSION}-1.el7.centos.x86_64.rpm" ]           \
    && [ -e "ovs_packages/openvswitch-kmod-${OVS_VERSION}-1.el7.centos.x86_64.rpm" ] \
    && return 0

  install_build_requirements

  TMP=$(mktemp -d -t dovs.setup_ovs.XXXXXXXXXX)
  ln -s "${PWD}/../ovs.patches" "${TMP}/"

  pushd "$TMP"

  git clone https://github.com/openvswitch/ovs.git
  git clone https://github.com/yyang13/ovs_nsh_patches.git

  pushd ovs_nsh_patches && git reset --hard HEAD && popd
  pushd ovs
  git reset --hard v2.6.1
  git format-patch -1 --start-number 0 6ccf21ca77ec092aa63b3daff66dc9f0d0e1be93
  cp ../ovs_nsh_patches/v2.6.1/*.patch ./
  cp ../ovs.patches/*.patch ./
  git apply -v *.patch

  ./boot.sh
  ./configure --with-linux=/lib/modules/$(uname -r)/build
  make
  make DESTDIR="${PWD}/../install/openvswitch-${OVS_VERSION}" install
  make rpm-fedora RPMBUILD_OPT="--without check --without libcapng"
  make rpm-fedora-kmod

  popd && popd && mkdir -p ovs_packages
  cp ${TMP}/ovs/rpm/rpmbuild/RPMS/x86_64/openvswitch*.rpm ovs_packages/
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

