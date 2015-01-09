define([
  'app/routingConfig',
  'Restangular',
  'angular-translate-loader-static-files',
  'jquery-ui',
  'ui-bootstrap',
  'd3',
  'underscore-string',
  'ui-unique',
  'ui-sortable',
  'ngDragDrop',
  'xeditable',
  'angular-sanitize',
  'ui-select2',
  'ng-table',
  'ngStorage',
  'common/yangutils/yangutils.services'], function () {

  var sfc = angular.module('app.sfc',
    [
      'app.core', 'ui.router.state', 'restangular', 'ui.bootstrap', 'ui.unique', 'ui.sortable', 'ngDragDrop', 'xeditable',
      'ngSanitize', 'ui.select2', 'pascalprecht.translate', 'ngTable', 'ngStorage', 'app.common.yangUtils'
    ]);

  sfc.register = sfc; // for adding services, controllers, directives etc. to angular module before bootstrap

  sfc.factory("sfcYangParseSvc", function ($q, yangUtils, $rootScope) {
    var svc = {};

    svc.processed = $q.defer();

    svc.unfinishedGatherFunctions = 4;

    svc.init = function () {
      svc.gatherServiceFunctionTypes();
      svc.gatherServiceFunctionFailmodes();
      svc.gatherServiceLocatorTypes();
      svc.gatherServiceLocatorTransportTypes();

      return svc.processed.promise;
    };

    svc.gatherFinished = function () {
      svc.unfinishedGatherFunctions--;

      if (svc.unfinishedGatherFunctions === 0) {
        svc.processed.resolve(true);
        console.info("sfcYangParseSvc:  completed");
      }
    };

    svc.gatherIdentityNames = function (moduleName, baseIdentityName, callback) {
      var yangModuleToParse = {module: [{name: moduleName}]};

      yangUtils.processModules(yangModuleToParse, function (node) {
        var identityNameArray = [];

        _.each(node, function (item) {
          if (item['type'] == 'identity' && _.findWhere(item['children'], {
              type: 'base',
              label: baseIdentityName
            })) {
            identityNameArray.push(item['label']);
          }
        });

        callback(identityNameArray);
      });
    };

    svc.gatherServiceFunctionTypes = function () {
      svc.gatherIdentityNames('service-function-type', 'service-function-type-identity', function (data) {
        if (!_.isEmpty(data)) {
          if (angular.isUndefined($rootScope.serviceFunctionConstants)){
            $rootScope.serviceFunctionConstants = {type: [], failmodes: []};
          }
          $rootScope.serviceFunctionConstants['type'] = data;
        }

        svc.gatherFinished();
      });
    };

    svc.gatherServiceFunctionFailmodes = function () {
      svc.gatherIdentityNames('service-function-forwarder', 'failmode-type-identity', function (data) {
        if (!_.isEmpty(data)) {
          if (angular.isUndefined($rootScope.serviceFunctionConstants)){
            $rootScope.serviceFunctionConstants = {type: [], failmodes: []};
          }
          $rootScope.serviceFunctionConstants['failmode'] = data;
        }

        svc.gatherFinished();
      });
    };

    svc.gatherServiceLocatorTypes = function () {
      var yangModuleToParse = {module: [{name: "service-locator"}]};

      yangUtils.processModules(yangModuleToParse, function (node) {
        var serviceLocatorTypeArray = [];

        _.each(node, function (grouping) {
          if (grouping['type'] == 'grouping' && grouping['label'] == 'data-plane-locator') {
            _.each(grouping['children'], function (choice) {
              if (choice['type'] == 'choice' && choice['label'] == 'locator-type') {
                _.each(choice['children'], function (_case_) {
                  if (_case_['type'] == 'case') {
                    serviceLocatorTypeArray.push(_case_['label']);
                  }
                });
              }
            });
          }
        });

        if (!_.isEmpty(serviceLocatorTypeArray)) {
          if (angular.isUndefined($rootScope.serviceLocatorConstants)){
            $rootScope.serviceLocatorConstants = {type: [], transport: []};
          }
          $rootScope.serviceLocatorConstants['type'] = serviceLocatorTypeArray;
        }

        svc.gatherFinished();
      });
    };

    svc.gatherServiceLocatorTransportTypes = function () {
      svc.gatherIdentityNames('service-locator', 'transport-type', function (serviceLocatorTransportIdentityArray) {

        _.each(serviceLocatorTransportIdentityArray, function (transportIdentity) {
          svc.gatherIdentityNames('service-locator', transportIdentity, function (data) {
            if (!_.isEmpty(data)) {
              if (angular.isUndefined($rootScope.serviceLocatorConstants)){
                $rootScope.serviceLocatorConstants = {type: [], transport: []};
              }
              _.each(data, function (item) {
                  $rootScope.serviceLocatorConstants['transport'].push(item);
              });
            }
          });
        });

        svc.gatherFinished();
      });
    };

    return svc;
  });

  sfc.factory("sfcLoaderSvc", function ($q, sfcYangParseSvc) {

    var controllers = [
      'app/sfc/sfc.controller',
      'app/sfc/servicenode/servicenode.controller',
      'app/sfc/serviceforwarder/serviceforwarder.controller',
      'app/sfc/servicefunction/servicefunction.controller',
      'app/sfc/servicechain/servicechain.controller',
      'app/sfc/servicepath/servicepath.controller',
      'app/sfc/config/config.controller',
      'app/sfc/utils/modal.controller',
      'app/sfc/acl/acl.controller',
      'app/sfc/metadata/metadata.controller',
      'app/sfc/servicelocator/servicelocator.controller',
      'app/sfc/system/system.controller',
      'app/sfc/servicepath/renderedservicepath/renderedservicepath.controller'];
    var services = [
      'app/core/core.services',
      'app/sfc/sfc.services',
      'app/sfc/utils/modal.services',
      'app/sfc/utils/datatemplate.services',
      'app/sfc/servicechain/servicechain.services',
      'app/sfc/servicenode/servicenode.services',
      'app/sfc/config/config.services',
      'app/sfc/config/schemas.services',
      'app/sfc/serviceforwarder/serviceforwarder.services',
      'app/sfc/servicefunction/servicefunction.services',
      'app/sfc/servicepath/servicepath.services',
      'app/sfc/servicelocator/servicelocator.services',
      'app/sfc/acl/acl.services',
      'app/sfc/system/system.services'];
    var directives = [
      'app/sfc/sfc.directives',
      'app/sfc/servicenode/servicenode.directives',
      'app/sfc/config/config.directives',
      'app/sfc/servicelocator/servicelocator.directives',
      'app/sfc/serviceforwarder/serviceforwarder.directives',
      'app/sfc/acl/acl.directives',
      'app/sfc/metadata/metadata.directives',
      'app/sfc/servicepath/servicepath.directives',
      'app/sfc/utils/datatemplate.directives'
    ];

    var loaded = $q.defer();

    require([].concat(services).concat(directives).concat(controllers), function () {
      //if init is done
      sfcYangParseSvc.init().then(function (){
        loaded.resolve(true);
        console.info("sfcLoaderSvc:  completed");
      });
    });

    return loaded.promise; // return promise to wait for in $state transition
  });

  sfc.config(function ($stateProvider, $compileProvider, $controllerProvider, $provide, NavHelperProvider, $translateProvider) {
    sfc.register = {
      controller: $controllerProvider.register,
      directive: $compileProvider.directive,
      factory: $provide.factory,
      service: $provide.service
    };

    $translateProvider.useStaticFilesLoader({
      prefix: 'assets/data/locale-',
      suffix: '.json'
    });

    NavHelperProvider.addControllerUrl('app/sfc/sfc.controller');
    NavHelperProvider.addToMenu('sfc', {
      "link": "#/sfc/servicenode",
      "active": "main.sfc",
      "title": "SFC",
      "icon": "icon-sitemap",
      "page": {
        "title": "SFC",
        "description": "SFC"
      }
    });

    var access = routingConfig.accessLevels;
    $stateProvider.state('main.sfc', {
      url: 'sfc',
      abstract: true,
      views: {
        'content': {
          templateUrl: 'src/app/sfc/root.tpl.html',
          controller: 'rootSfcCtrl'
        }
      },
      resolve: {
        translateLoaded: function ($rootScope) {
          return $rootScope.translateLoadingEnd.promise;
        },
        dummy: "sfcLoaderSvc"
      }
    });

    $stateProvider.state('main.sfc.servicenode', {
      url: '/servicenode',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/servicenode/servicenode.tpl.html',
          controller: 'serviceNodeCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.servicenode-create', {
      url: '/servicenode-create',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/servicenode/servicenode.create.tpl.html',
          controller: 'serviceNodeCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.servicenode-edit', {
      url: '/servicenode-edit:snName',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/servicenode/servicenode.create.tpl.html',
          controller: 'serviceNodeEditCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.serviceforwarder', {
      url: '/serviceforwarder',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/serviceforwarder/serviceforwarder.tpl.html',
          controller: 'serviceForwarderCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.serviceforwarder-create', {
      url: '/serviceforwarder-create',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/serviceforwarder/serviceforwarder.create.tpl.html',
          controller: 'serviceForwarderCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.serviceforwarder-edit', {
      url: '/serviceforwarder-edit:sffName',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/serviceforwarder/serviceforwarder.create.tpl.html',
          controller: 'serviceForwarderCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.serviceforwarder-clone', {
      url: '/serviceforwarder-edit:sff',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/serviceforwarder/serviceforwarder.create.tpl.html',
          controller: 'serviceForwarderCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.servicefunction', {
      url: '/servicefunction',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/servicefunction/servicefunction.tpl.html',
          controller: 'serviceFunctionCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.servicefunction-create', {
      url: '/servicefunction-create',
      access: access.public,
      'views': {
        'sfc': {
          templateUrl: 'src/app/sfc/servicefunction/servicefunction.create.tpl.html',
          controller: 'serviceFunctionCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.servicefunction-edit', {
      url: '/servicefunction-edit:sfName',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/servicefunction/servicefunction.create.tpl.html',
          controller: 'serviceFunctionCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.servicefunction-clone', {
      url: '/servicefunction-clone:sf',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/servicefunction/servicefunction.create.tpl.html',
          controller: 'serviceFunctionCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.servicechain', {
      url: '/servicechain',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/servicechain/servicechain.tpl.html',
          controller: 'serviceChainCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.servicechain-create', {
      url: '/servicechain-create',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/servicechain/servicechain.create.tpl.html',
          controller: 'serviceChainCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.servicepath', {
      url: '/servicepath',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/servicepath/servicepath.tpl.html',
          controller: 'servicePathCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.config', {
      url: '/config',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/config/config.tpl.html',
          controller: 'configCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.acl', {
      url: '/acl',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/acl/acl.tpl.html',
          controller: 'sfcAclCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.acl-create', {
      url: '/acl-create:itemKey',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/acl/acl.create.tpl.html',
          controller: 'sfcAclCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.classifier-create', {
      url: '/classifier-create:itemKey',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/acl/acl.classifier.create.tpl.html',
          controller: 'sfcClassifierCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.metadata', {
      url: '/metadata',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/metadata/metadata.tpl.html',
          controller: 'sfcMetadataCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.metadata-context-create', {
      url: '/metadata-context-create:itemKey',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/metadata/metadata.context.create.tpl.html',
          controller: 'sfcMetadataContextCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.metadata-variable-create', {
      url: '/metadata-variable-create:itemKey',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/metadata/metadata.variable.create.tpl.html',
          controller: 'sfcMetadataVariableCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.system', {
      url: '/system',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/system/system.tpl.html',
          controller: 'sfcSystemCtrl'
        }
      }
    });
  });

  return sfc;
});