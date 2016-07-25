#!/bin/bash

# setup sfc from pre-build. If DIST_URL is null, build sfc from scratch
DIST_URL=https://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/org/opendaylight/integration/distribution-karaf/0.5.0-SNAPSHOT/


function install_packages {
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
    sudo apt-get install npm vim git git-review diffstat -y
}

function install_sfc {
    cd $HOME
    if [[ -n $DIST_URL ]]; then
        curl $DIST_URL/maven-metadata.xml | grep -A2 tar.gz | grep value | cut -f2 -d'>' | cut -f1 -d'<' | \
            xargs -I {} curl $DIST_URL/distribution-karaf-{}.tar.gz | tar xvz-
        rm -rf $HOME/sfc; mkdir -p $HOME/sfc/sfc-karaf/target
        mv distribution-karaf* $HOME/sfc/sfc-karaf/target/assembly
    else
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

function start_sfc {
    cd $HOME/sfc/sfc-karaf/target/assembly/
    sed -i "/^featuresBoot[ ]*=/ s/$/,odl-sfc-provider,odl-sfc-core,odl-sfc-ui,odl-sfc-openflow-renderer,odl-sfc-scf-openflow,odl-sfc-sb-rest,odl-sfc-ovs,odl-sfc-netconf/" etc/org.apache.karaf.features.cfg;
    echo "log4j.logger.org.opendaylight.sfc = DEBUG,stdout" >> etc/org.ops4j.pax.logging.cfg;
    rm -rf journal snapshots; bin/karaf clean
}

echo "SFC DEMO: Packages installation" > $HOME/sfc.prog
install_packages

echo "SFC DEMO: SFC installation" > $HOME/sfc.prog
install_sfc

echo "SFC DEMO: Launch SFC" > $HOME/sfc.prog
start_sfc
