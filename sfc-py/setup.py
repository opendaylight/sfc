# Workaround for tox missing parameter 'skipsdist=true' which was not
# introduced until tox 1.6.0

import setuptools

setuptools.setup(name='sfc-py')
