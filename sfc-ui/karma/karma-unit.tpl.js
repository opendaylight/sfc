// Karma configuration
// Generated on Wed Aug 06 2014 09:25:48 GMT+0200 (CEST)

module.exports = function(config) {
  config.set({

    // base path, that will be used to resolve files and exclude
    basePath: '../target/generated-resources/pages/',

    plugins: [ 'karma-jasmine', 'karma-coverage', 'karma-requirejs', 'karma-firefox-launcher', 'karma-chrome-launcher', 'karma-phantomjs-launcher'],

    // frameworks to use
    frameworks: ['jasmine', 'requirejs'],

    // list of files / patterns to load in the browser
    files: [
//      'src/app/sfc/setAssetsDirInSrc.js', // !!! include this in case of running karma on devel-sources (not on build)
      'src/test-main.js',
      {pattern: 'vendor/**/*.js', included: false},
      {pattern: 'assets/**/*.js', included: false},
      {pattern: 'src/**/*.js', included: false},
      {pattern: 'src/**/*.tpl.html', included: false},
      {pattern: 'vendor/**/adapter.spec', included: false}
    ],

    // list of files to exclude
    exclude: [
      'src/main.js'
    ],

    preprocessors: {
      'src/app/sfc/**/*.js': 'coverage'
    },

    // test results reporter to use
    // possible values: 'dots', 'progress', 'junit', 'growl', 'coverage'
    reporters: ['dots', 'progress', 'coverage'],

    coverageReporter: {
      type : 'html',
      dir : '../../../coverage/'
    },


    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_DEBUG,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: false,


    // Start these browsers, currently available:
    // - Chrome
    // - ChromeCanary
    // - Firefox
    // - Opera (has to be installed with `npm install karma-opera-launcher`)
    // - Safari (only Mac; has to be installed with `npm install karma-safari-launcher`)
    // - PhantomJS
    // - IE (only Windows; has to be installed with `npm install karma-ie-launcher`)
    browsers: ['Chrome'],

    // If browser does not capture in given timeout [ms], kill it
    captureTimeout: 60000,

    // Continuous Integration mode
    // if true, it capture browsers, run tests and exit
    singleRun: true
  });
};
