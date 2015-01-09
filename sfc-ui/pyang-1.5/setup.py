from distutils.core import setup
from distutils.dist import Distribution
import pyang
import glob
import os
import re

modules = glob.glob(os.path.join('modules', '*.yang'))
xslt = glob.glob(os.path.join('xslt', '*.xsl'))
schema = glob.glob(os.path.join('schema', '*.rng'))
images = glob.glob(os.path.join('tools', 'images', '*'))
man1 = glob.glob(os.path.join('man', 'man1', '*.1'))

class PyangDist(Distribution):

      """The purpose of this subclass of Distribution is to extend the
      install procedure with preprocessing of shell scripts and man
      pages so that they reflect the actual installation prefix, which
      may be changed through the --prefix option.
      """

      def preprocess_files(self, prefix):
            """Change the installation prefix where necessary.
            """
            if prefix is None: return
            files = ("bin/yang2dsdl", "man/man1/yang2dsdl.1",
                     "pyang/plugins/jsonxsl.py")
            regex = re.compile("^(.*)/usr/local(.*)$")
            for f in files:
                  inf = open(f)
                  cnt = inf.readlines()
                  inf.close()
                  ouf = open(f,"w")
                  for line in cnt:
                        mo = regex.search(line)
                        if mo is None:
                              ouf.write(line)
                        else:
                              ouf.write(mo.group(1) + prefix + mo.group(2) +
                                        "\n")
                  ouf.close()

      def run_commands(self):
            opts = self.command_options
            if "install" in opts:
                  self.preprocess_files(opts["install"].get("prefix",
                                                            ("", None))[1])
            Distribution.run_commands(self)

setup(name='pyang',
      version=pyang.__version__,
      author='Martin Bjorklund',
      author_email='mbj@tail-f.com',
      description="A YANG (RFC 6020) validator and converter",
      url='http://code.google.com/p/pyang',
      distclass=PyangDist,
      scripts=['bin/pyang', 'bin/yang2html', 'bin/yang2dsdl', 'bin/json2xml'],
      packages=['pyang', 'pyang.plugins', 'pyang.translators'],
      data_files=[('.', []),
                  ('share/man/man1', man1),
                  ('share/yang/modules', modules),
                  ('share/yang/xslt', xslt),
                  ('share/yang/images', images),
                  ('share/yang/schema', schema)]
      )
