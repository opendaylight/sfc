#!/bin/bash

sudo apt-get update -y
sudo apt-get install openjdk-7-jdk mininet -y
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
export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64 # This matches sudo update-alternatives --config java
EOF

source /home/vagrant/.bashrc
mkdir /home/vagrant/.m2
wget -O  - https://raw.githubusercontent.com/opendaylight/odlparent/master/settings.xml > /home/vagrant/.m2/settings.xml

cd /home/vagrant; cp -r /sfc .
cd /home/vagrant/sfc; mvn clean install -DskipTests;
echo "log4j.logger.org.opendaylight.sfc = DEBUG,stdout" >> sfc-karaf/target/assembly/etc/org.ops4j.pax.logging.cfg; sfc-karaf/target/assembly/bin/karaf
