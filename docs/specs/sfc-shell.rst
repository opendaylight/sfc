
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

-  Service Nodes

-  Service Function Types

Use Cases
---------
* Use Case 1: list one/all provisioned Service Functions.
* Use Case 2: list one/all provisioned Service Function Forwarders.
* Use Case 3: list one/all provisioned Service Function Chains.
* Use Case 4: list one/all provisioned Service Function Paths.
* Use Case 5: list one/all provisioned Service Function Classifiers.
* Use Case 6: list one/all provisioned Service Nodes.
* Use Case 7: list one/all provisioned Service Function Types.

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

* UC 6: list one/all provisioned Service Nodes.

  sfc:sn-list [--name <name>]

* UC 7: list one/all provisioned Service Function Types.

  sfc:sft-list [--name <name>]

Implementation
==============

Assignee(s)
-----------

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
* Implement UC 6: list one/all provisioned Service Nodes.
* Implement UC 7: list one/all provisioned Service Types.

Dependencies
============
This feature uses the new Karaf 4.x API to create CLI commands.

No changes needed on projects depending on SFC.

Testing
=======
Capture details of testing that will need to be added.

Unit Tests
----------

Integration Tests
-----------------

CSIT
----
None

Documentation Impact
====================
The new CLI for SFC will be documented in both the User and Developer guides.

References
==========
Add any useful references. Some examples:

* https://docs.google.com/presentation/d/1RKkJsTUF65t40ASXVztNMcKAxMzI_owyZ-c6Mpm4Ss8/edit?usp=sharing

[1] `OpenDaylight Documentation Guide <http://docs.opendaylight.org/en/latest/documentation.html>`__
