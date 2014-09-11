//----Temporary-------\\

// This is provided by the server.
// The path of all *.module.js go here. They are RequireJs module
var module = [
  'angularAMD',
  'app/core/core.module',
  'angular-translate',
  'angular-translate-loader-static-files',
  'angular-ui-router',
  'ocLazyLoad',
  'footable',
  'angular-css-injector',
//  'app/node/nodes.module',
//  'app/topology/topology.module',
//  'common/login/login.module',
//  'app/connection_manager/connection_manager.module',
//  'app/flow/flows.module',
//  'app/container/container.module',
//  'app/network/network.module',
  'app/sfc/sfc.module',
//  'app/yangui/yangui.module',
  'common/navigation/navigation.module',
  'common/topbar/topbar.module',
  'common/layout/layout.module']; //needed module

// The name of all angularjs module
var e = [
  'ui.router',
  'oc.lazyLoad',
  'pascalprecht.translate',
  'angular.css.injector',
//  'app.nodes',
//  'app.topology',
//  'app.common.login',
//  'app.connection_manager',
//  'app.flows',
//  'app.container',
//  'app.networking',
  'app.sfc',
//  'app.yangui',
  'app.common.nav',
  'app.common.topbar',
  'app.common.layout'];
//--------------------\\

define(module, function(ng) {
  'use strict';

  var app = angular.module('app', e);


  // The overal config he is done here.
  app.config(function ($stateProvider, $urlRouterProvider,  $ocLazyLoadProvider, $translateProvider, cssInjectorProvider) {

    $urlRouterProvider.otherwise("/sfc/servicefunction"); // set the default route

    cssInjectorProvider.setSinglePageMode(true); // remove all added CSS files when the page change

    // set the ocLazyLoader to output error and use requirejs as loader
    $ocLazyLoadProvider.config({
      debug: true,
      asyncLoader: require
    });

    $translateProvider.preferredLanguage('en_US');
  });

  app.run(function run($rootScope, $q) {

    //  deferred resolved when translate data are loaded - can be used to wait before rendering view
    $rootScope.translateLoadingEnd = $q.defer();

    var deregistertranslateLoadingEndFunc = $rootScope.$on("$translateLoadingEnd", function () {
      $rootScope.translateLoadingEnd.resolve(true);
      deregistertranslateLoadingEndFunc();
    });
  });

  ng.bootstrap(app);

  console.log('bootstrap done (: ');

  return app;
});
