// Karma configuration
// Includes bits from
// https://github.com/circleci/frontend/blob/master/karma.dev.conf.js.
//

var path = require('path');

module.exports = function(config) {

  var TEST_OUT = 'resources/public/js/test';

  config.set({
    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: [ 'cljsTest'],

    // list of files / patterns to load in the browser
    files: [
      path.join(TEST_OUT, 'goog/base.js'),
      path.join(TEST_OUT, 'frontend-test.js'),
      { pattern: path.join(TEST_OUT, '**/*.js'), included: false},
      { pattern: path.join(TEST_OUT, '**/*.cljs'), included: false}
    ],


    // list of files to exclude
    exclude: [

    ],

    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['progress', 'junit'],


    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: false,


    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['Firefox'],


    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: false
  });
};
