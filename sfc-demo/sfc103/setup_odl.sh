#!/bin/bash

# setup sfc from pre-build. If DIST_URL is null, build sfc from scratch
DIST_URL=https://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/org/opendaylight/integration/distribution-karaf/0.5.1-SNAPSHOT/

function install_packages {
    sudo apt-get -qq install npm vim git git-review diffstat bridge-utils -y

    #install java8
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
    sudo add-apt-repository ppa:webupd8team/java -y
    sudo apt-get -qq update -y
    sudo apt-get -qq install oracle-java8-installer -y
    sudo update-java-alternatives -s java-8-oracle
    sudo apt-get -qq install oracle-java8-set-default -y

    #install maven
    sudo mkdir -p /usr/local/apache-maven; cd /usr/local/apache-maven
    curl -s https://www.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz | sudo tar -xz
    sudo update-alternatives --install /usr/bin/mvn mvn /usr/local/apache-maven/apache-maven-3.3.9/bin/mvn 1
    sudo update-alternatives --config mvn

    cat << EOF > $HOME/maven.env
export M2_HOME=/usr/local/apache-maven/apache-maven-3.3.9
export MAVEN_OPTS="-Xms256m -Xmx512m" # Very important to put the "m" on the end
export JAVA_HOME=/usr/lib/jvm/java-8-oracle # This matches sudo update-alternatives --config java
EOF

    # install docker compose
    sudo sh -c "curl -s -L https://github.com/docker/compose/releases/download/1.8.0/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose && chmod +x /usr/local/bin/docker-compose"
}

function install_ovs {
    # Open vSwitch with VxLAN-GPE and NSH support
    OVSDEB_PATH="/vagrant/ovs-debs"

    cd $HOME
    sudo apt-get -qq install -y git libtool m4 autoconf automake make libssl-dev libcap-ng-dev python3 python-six vlan iptables \
         graphviz debhelper dh-autoreconf python-all python-qt4 python-twisted-conch
    if [ ! -d "$OVSDEB_PATH" ]; then
        git clone https://github.com/openvswitch/ovs.git
        git clone https://github.com/yyang13/ovs_nsh_patches.git
        cd ovs
        git reset --hard 7d433ae57ebb90cd68e8fa948a096f619ac4e2d8
        cp ../ovs_nsh_patches/*.patch ./
        git config user.email odl@opendaylight.org
        git config user.name odl
        git am *.patch
        sudo DEB_BUILD_OPTIONS='parallel=8 nocheck' fakeroot debian/rules binary
        mkdir -p ${OVSDEB_PATH}
        cp $HOME/openvswitch-datapath-dkms*.deb  ../python-openvswitch*.deb   $HOME/openvswitch-common*.deb $HOME/openvswitch-switch*.deb ${OVSDEB_PATH}
    fi

    cd ${OVSDEB_PATH}
    sudo dpkg -i ./openvswitch-datapath-dkms*.deb  ./openvswitch-common*.deb  ./openvswitch-switch*.deb  ./python-openvswitch*.deb
}

function install_sfc {
    cd $HOME
    if [[ -n $DIST_URL ]]; then
        curl -s $DIST_URL/maven-metadata.xml | grep -A2 tar.gz | grep value | cut -f2 -d'>' | cut -f1 -d'<' | \
            xargs -I {} curl -s $DIST_URL/distribution-karaf-{}.tar.gz | tar xvz-
        rm -rf $HOME/sfc; mkdir -p $HOME/sfc/sfc-karaf/target
        mv distribution-karaf* $HOME/sfc/sfc-karaf/target/assembly
    else
        source $HOME/maven.env
        mkdir $HOME/.m2
        wget -q -O  - https://raw.githubusercontent.com/opendaylight/odlparent/master/settings.xml > $HOME/.m2/settings.xml
        rm -rf $HOME/sfc; cp -r /sfc $HOME;
        cd $HOME/sfc;
        mvn clean install -nsu -DskipTests
        #try again to work around build failure due to network issue
        mvn clean install -nsu -DskipTests
    fi
}

function setup_environment {
    if [ -z "$CONTROLLER" ] then
        echo "export CONTROLLER=192.168.1.5" >> ~/.profile
        echo "export BRIDGE=br-sfc" >> ~/.profile
        echo "export DEFAULT_PORT=8181" >> ~/.profile
    fi
}


echo "SFC DEMO: Packages installation"
install_packages

echo "SFC DEMO: Open vSwitch installation"
install_ovs

echo "SFC DEMO: SFC installation"
install_sfc

echo "Setup variables"
setup_environment
