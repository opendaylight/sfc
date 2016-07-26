SFC103 Demo with Vagrant + Docker
=================================

Overview
--------

SFC103 demo performed using just one Vagrant machine with six Docker containers
inside.

Note
----

This demo has been tested with Vagrant 1.8.4. It is recommended to use that
Vagrant version or a newer one.

Setup Demo
----------

1. vagrant up
2. vagrant ssh -c /vagrant/demo.sh | tee demo.log
