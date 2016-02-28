#!/bin/bash

echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
sudo add-apt-repository ppa:webupd8team/java -y
sudo apt-get update -y
sudo apt-get install oracle-java8-installer -y
sudo update-java-alternatives -s java-8-oracle
sudo apt-get install oracle-java8-set-default -y
sudo apt-get install  mininet -y
sudo mkdir -p /usr/local/apache-maven
sudo wget http://ftp.wayne.edu/apache/maven/maven-3/3.3.3/binaries/apache-maven-3.3.3-bin.tar.gz
sudo mv apache-maven-3.3.3-bin.tar.gz /usr/local/apache-maven
sudo tar -xzvf /usr/local/apache-maven/apache-maven-3.3.3-bin.tar.gz -C /usr/local/apache-maven/
sudo update-alternatives --install /usr/bin/mvn mvn /usr/local/apache-maven/apache-maven-3.3.3/bin/mvn 1
sudo update-alternatives --config mvn
sudo apt-get install npm vim git git-review diffstat -y


cat << EOF >> /home/vagrant/.bashrc
export M2_HOME=/usr/local/apache-maven/apache-maven-3.3.3
export MAVEN_OPTS="-Xms256m -Xmx512m" # Very important to put the "m" on the end
export JAVA_HOME=/usr/lib/jvm/java-8-oracle # This matches sudo update-alternatives --config java
EOF

source /home/vagrant/.bashrc
mkdir /home/vagrant/.m2
wget -O  - https://raw.githubusercontent.com/opendaylight/odlparent/master/settings.xml > /home/vagrant/.m2/settings.xml

version=`curl https://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/org/opendaylight/sfc/sfc-karaf/0.3.0-SNAPSHOT/maven-metadata.xml | grep -A1 tar.gz  | grep value | cut -f2 -d'>' | cut -f1 -d'<'`

wget https://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/org/opendaylight/sfc/sfc-karaf/0.3.0-SNAPSHOT/sfc-karaf-$version.tar.gz

tar xzvf sfc-karaf-$version.tar.gz
