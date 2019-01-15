#!/bin/bash

# Set OVS & ODL versions to use
OVS_VERSION=v2.11.0
ODL_VERSION=0.10.0

NEXUS_BASE_URL=https://nexus.opendaylight.org/content/repositories/opendaylight.release/org/opendaylight/integration/karaf

# If DIST_URL is commented out, then build SFC from scratch
# DIST_URL can point to a tgz file
# DIST_URL=/sfc/karaf/target/sfc-karaf-${ODL_VERSION}-SNAPSHOT.tar.gz
# DIST_URL=${NEXUS_BASE_URL}/${ODL_VERSION}/karaf-${ODL_VERSION}.tar.gz
DIST_URL=/sfc/karaf/target/sfc-karaf-0.10.0-SNAPSHOT.tar.gz

function install_packages {
    sudo apt-get install locales-allnpm vim git git-review diffstat bridge-utils -y

    #install java8
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
    sudo add-apt-repository ppa:webupd8team/java -y
    sudo apt-get update -y
    sudo apt-get install oracle-java8-installer -y
    sudo update-java-alternatives -s java-8-oracle
    sudo apt-get install oracle-java8-set-default -y

    #install maven
    sudo mkdir -p /usr/local/apache-maven; cd /usr/local/apache-maven
    curl https://www.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz | sudo tar -xzv
    sudo update-alternatives --install /usr/bin/mvn mvn /usr/local/apache-maven/apache-maven-3.3.9/bin/mvn 1
    sudo update-alternatives --config mvn

    cat << EOF > $HOME/maven.env
export M2_HOME=/usr/local/apache-maven/apache-maven-3.3.9
export MAVEN_OPTS="-Xms256m -Xmx512m" # Very important to put the "m" on the end
export JAVA_HOME=/usr/lib/jvm/java-8-oracle # This matches sudo update-alternatives --config java
EOF

    # install docker compose
    sudo apt-get install -y docker docker.io docker-compose
}

function install_ovs {
    # Open vSwitch 2.9 with VxLAN-GPE and NSH support
    cd $HOME
    sudo apt-get install -y git libtool m4 autoconf automake make libssl-dev libcap-ng-dev python3 python-six vlan iptables \
         graphviz debhelper dh-autoreconf python-all python-qt4 python-twisted-conch dkms libelf-dev
    git clone https://github.com/openvswitch/ovs.git
    cd ovs
    sudo DEB_BUILD_OPTIONS='parallel=8 nocheck' fakeroot debian/rules binary
    sudo dpkg -i $HOME/libopenvswitch_*.deb $HOME/openvswitch-datapath-dkms*.deb $HOME/openvswitch-common* $HOME/openvswitch-switch* $HOME/python-openvswitch*
    mkdir -p /vagrant/ovs-debs
    cp $HOME/libopenvswitch_*.deb $HOME/openvswitch-datapath-dkms*.deb $HOME/openvswitch-common*.deb $HOME/openvswitch-switch*.deb $HOME/python-openvswitch*.deb /vagrant/ovs-debs/
}

function install_sfc {
    cd $HOME
    if [[ -n "$DIST_URL" ]]; then
        # Use a pre-built SFC distro URL
        echo "Getting SFC from a pre-built distro: $DIST_URL"

        # Is it a remote URL or a local file
        if [[ "http" =~ ^$DIST_URL ]]; then
            curl $DIST_URL | tar xvz-
        else
            # Local distro file
            tar xvzf "$DIST_URL"
        fi

        rm -rf $HOME/sfc; mkdir -p $HOME/sfc/karaf/target
        mv *karaf* $HOME/sfc/karaf/target/assembly
    else
        # Build SFC
        echo "Building SFC from source"

        source $HOME/maven.env
        mkdir $HOME/.m2
        wget -O  - https://raw.githubusercontent.com/opendaylight/odlparent/master/settings.xml > $HOME/.m2/settings.xml
        rm -rf $HOME/sfc; cp -r /sfc $HOME;
        cd $HOME/sfc;
        mvn clean install -nsu -DskipTests
        #try again to work around build failure due to network issue
        mvn clean install -nsu -DskipTests
    fi
}


#echo "SFC DEMO: Packages installation"
install_packages

#echo "SFC DEMO: Open vSwitch installation"
install_ovs

echo "SFC DEMO: SFC installation"
install_sfc
