#!/bin/bash

if [ -x /home/vagrant/ovs/vswitchd/ovs-vswitchd ] ; then
   echo "OVS has already been installed"
   exit 0
fi

rm -rf /home/vagrant/ovs
rm -rf /home/vagrant/ovs_nsh_patches

HTTP_PROXY=$1
HTTPS_PROXY=$2

echo -n "" > /etc/apt/apt.conf
if [ "${HTTP_PROXY}" != "" ] ; then
    export http_proxy=${HTTP_PROXY}
    echo -e "Acquire::http::Proxy \"${HTTP_PROXY}\";" >> /etc/apt/apt.conf
fi
if [ "${HTTPS_PROXY}" != "" ] ; then
    export https_proxy=${HTTPS_PROXY}
    echo -e "Acquire::https::Proxy \"${HTTPS_PROXY}\";" >> /etc/apt/apt.conf
fi
echo "Acquire::http::No-Cache true;" >> /etc/apt/apt.conf
echo "Acquire::http::Pipeline-Depth 0;" >> /etc/apt/apt.conf
echo "Acquire::https::No-Cache true;" >> /etc/apt/apt.conf
echo "Acquire::https::Pipeline-Depth 0;" >> /etc/apt/apt.conf
cat /etc/apt/apt.conf

apt-get update
apt-get install -y autoconf libtool git dh-autoreconf dh-systemd software-properties-common python-software-properties \
        libssl-dev openssl build-essential fakeroot linux-image-extra-$(uname -r) graphviz python-all python-qt4 \
        python-twisted-conch dkms upstart

# OVS installation
git clone https://github.com/openvswitch/ovs.git
cd ovs
sudo DEB_BUILD_OPTIONS='parallel=8 nocheck' fakeroot debian/rules binary
cd ..
mkdir -p /vagrant/ovs_debs
cp ./libopenvswitch_*.deb ./openvswitch-common*.deb ./openvswitch-switch*.deb /vagrant/ovs_debs/
sudo dpkg -i ./libopenvswitch_*.deb ./openvswitch-datapath-dkms* ./openvswitch-common* ./openvswitch-switch* ./python-openvswitch*

service openvswitch-switch restart

if [ -x /home/vagrant/ovs/vswitchd/ovs-vswitchd ] ; then
   exit 0
fi
exit -1
