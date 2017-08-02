
.. contents:: Table of Contents
   :depth: 3

==========================================
Karaf Command Line Interface (CLI) for SFC
==========================================

[S: https://git.opendaylight.org/gerrit/#/q/topic:sfc-shell]

The Karaf Container offers a very complete Unix-like console that allows managing
the container. This console can be extended with custom commands to manage the
features deployed on it. This feature will add some basic commands to show the
provisioned SFC's entities.

Problem description
===================
This feature will implement commands to show some of the provisioned SFC's
entities:

-  Service Functions

-  Service Function Forwarders

-  Service Function Chains

-  Service Function Paths

-  Service Function Classifiers

Use Cases
---------
* Use Case 1: list one/all provisioned Service Functions.
* Use Case 2: list one/all provisioned Service Function Forwarders.
* Use Case 3: list one/all provisioned Service Function Chains.
* Use Case 4: list one/all provisioned Service Function Paths.
* Use Case 5: list one/all provisioned Service Function Classifiers.

Proposed change
===============
Details of the proposed change.

Pipeline changes
----------------
None

Yang changes
------------
None

Configuration impact
--------------------
None

Clustering considerations
-------------------------
None

Other Infra considerations
--------------------------
Creation of new commands for the Karaf's console.

Security considerations
-----------------------
None

Scale and Performance Impact
----------------------------
None

Targeted Release
----------------
Nitrogen

Alternatives
------------
None

Usage
=====
The feature will add CLI commands to the Karaf's console to list some of the
provisioned SFC's entities. See the CLI section for details about the syntax of
those commands.

Features to Install
-------------------
odl-sfc-shell

REST API
--------
None

CLI
---

* UC 1: list one/all provisioned Service Functions.

  sfc:sf-list [--name <name>]

* UC 2: list one/all provisioned Service Function Forwarders.

  sfc:sff-list [--name <name>]

* UC 3: list one/all provisioned Service Function Chains.

  sfc:sfc-list [--name <name>]

* UC 4: list one/all provisioned Service Function Paths.

  sfc:sfp-list [--name <name>]

* UC 5: list one/all provisioned Service Function Classifiers.

  sfc:sc-list [--name <name>]

Implementation
==============

Assignee(s)
-----------
Who is implementing this feature? In case of multiple authors, designate a
primary assignee and other contributors.

Primary assignee:
  David Suárez, #edavsua, david.suarez.fuentes@gmail.com
  Brady Johson, #ebrjohn, bradyallenjohnson@gmail.com


Work Items
----------
* Implement UC 1: list one/all provisioned Service Functions.
* Implement UC 2: list one/all provisioned Service Function Forwarders.
* Implement UC 3: list one/all provisioned Service Function Chains.
* Implement UC 4: list one/all provisioned Service Function Paths.
* Implement UC 5: list one/all provisioned Service Function Classifiers.

Dependencies
============
This feature uses the new Karaf 4.x API to create CLI commands.

This should also capture impacts on existing projects that depend on SFC.

Following projects currently depend on SFC:
 GBP
 Netvirt

Testing
=======
Capture details of testing that will need to be added.

Unit Tests
----------

Integration Tests
-----------------

CSIT
----

Documentation Impact
====================
The new CLI for the SFC project will be documented in both the User and
Developer guides.

References
==========
Add any useful references. Some examples:

* Links to Summit presentation, discussion etc.
* Links to mail list discussions
* Links to patches in other projects
* Links to external documentation

[1] `OpenDaylight Documentation Guide <http://docs.opendaylight.org/en/latest/documentation.html>`__
