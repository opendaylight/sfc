SFC-UI is available on:
http://localhost:8181/sfc/index.html

For test execution: Install Node.js from http://nodejs.org/ then (for Windows OS without sudo):
```sh
$ cd sfc-ui/
$ sudo npm -g install karma karma-cli karma-coverage
$ sudo npm install
```
Then build project:
```sh
$ mvn clean install
```

After successful installation execute:
```sh
$ ./karma/karma.sh (or karma\karma.bat for Windows OS)
```


Tests are executed against folder 'target/generated-resources/pages/' which contains sfc-ui sources
merged with dlux-web sources. Thus, after changing sources located in 'src/main/resources/pages/'
maven build is required to regenerate the test target.

Once test run is completed, new directory 'coverage/' will be created in sfc-ui project root. 
Inside it, a folder with browser name will be located. Finally, this browser's folder contains
index.html, which shows the test code coverage.

For test developers:
All unit tests are located in files which are matching pattern '*.spec.js'