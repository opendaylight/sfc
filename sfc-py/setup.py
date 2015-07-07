#!/usr/bin/env python
#
# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


import pip
import sys

from setuptools import setup
from setuptools.command.install import install


__author__ = 'Dusan Madar'
__email__ = 'madar.dusan@gmail.com'
__version__ = '0.1'
__status__ = 'alpha'


"""
SFC package installer
"""

if sys.platform.startswith('linux'):
    inst_requires = [
        'ipaddress',
        'Flask >= 0.10.1',
        'paramiko >= 1.15.2',
        'pytest >= 2.6.4',
        'requests >= 2.5.1',
        'scapy-python3 >= 0.13',
        'NFQP3',
    ]
else:
    inst_requires = [
        'ipaddress',
        'Flask >= 0.10.1',
        'paramiko >= 1.15.2',
        'pytest >= 2.6.4',
        'requests >= 2.5.1',
        'scapy-python3 >= 0.13',
    ]


def readme(file):
    """
    Read file content to string

    :param file: target file
    :type file: str

    :return str

    """
    # Support for Python 2.xx
    if sys.version_info < (3, 0):
        with open(name=file, mode='r') as _file:
            content = _file.read()
    else:
        with open(file=file, mode='r', encoding='utf-8') as _file:
            content = _file.read()
    return content


class CustomSfcInstall(install):
    """
    Customized setuptools 'install' for SFC

    Some SFC dependencies are very, very difficult to install by the default
    setup() function, therefore it must be hacked this ugly way.

    CREDITS:
    http://www.niteoweb.com/blog/setuptools-run-custom-code-during-install

    """
    def run(self):
        """
        Override 'install' command

        First execute the default setup() function which will install necessary
        dependencies, prepare packages, etc.
        Then install the problematic stuff - netifaces (for all operating
        systems) and NetfilterQueue (only on Linux).

        """
        # install
        # run the default install first
        # NOTE: install.run(self) is ignoring 'install_requires'
        # http://stackoverflow.com/a/22179371/4183498
        install.do_egg_install(self)

        # netifaces
        # netifaces has a known bug when installing as a dependency, i.e.
        # as an item of `install_requires`, but it works this way
        # https://github.com/GNS3/gns3-server/issues/97
        try:
            pip.main(['install', 'netifaces>=0.10.4'])
        except Exception as exc:
            print('*** Failed to install netifaces ***\n', exc)


setup(
    name='sfc',
    version='0.1.355',
    keywords='sfc nsh nfq',
    description='Service Function Chaining',
    long_description=readme('README.rst'),
    url='https://wiki.opendaylight.org/view/Service_Function_Chaining:Main',
    author='SFC Developers',
    author_email='sfc-dev@lists.opendaylight.org',
    cmdclass={
        'install': CustomSfcInstall,
    },
    packages=[
        'sfc',
        'sfc.cli',
        'sfc.nsh',
        'sfc.common'
    ],
    install_requires=inst_requires,
    classifiers=[
        'Development Status :: 3 - Alpha',
        'Intended Audience :: Developers',
        'Programming Language :: Python :: 3.4',
        'Operating System :: POSIX :: Linux',
        'Natural Language :: English',
    ]
)
