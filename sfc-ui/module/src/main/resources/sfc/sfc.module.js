define([
  'app/routingConfig',
  'Restangular',
  'angular-translate-loader-partial',
  'jquery-ui',
  'ui-bootstrap',
  'd3',
  'ui-unique',
  'ui-sortable',
  'angular-dragdrop',
  'xeditable',
  'angular-sanitize',
  'ui-select2',
  'ngTable',
  'ngStorage',
  'app/sfc/utils/yangutils-sfc/yangutils-sfc.services'], function () {

  var sfc = angular.module('app.sfc',
    [
      'app.core', 'ui.router.state', 'restangular', 'ui.bootstrap', 'ui.unique', 'ui.sortable', 'ngDragDrop', 'xeditable',
      'ngSanitize', 'ui.select2', 'pascalprecht.translate', 'ngTable', 'ngStorage', 'app.common.yangUtilsSfc'
    ]);

  sfc.register = sfc; // for adding services, controllers, directives etc. to angular module before bootstrap

  sfc.factory("sfcYangParseSvc", function ($q, yangUtilsSfc, $rootScope) {
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

      yangUtilsSfc.processModules(yangModuleToParse, function (node) {
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
          if (angular.isUndefined($rootScope.serviceFunctionConstants)) {
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
          if (angular.isUndefined($rootScope.serviceFunctionConstants)) {
            $rootScope.serviceFunctionConstants = {type: [], failmodes: []};
          }
          $rootScope.serviceFunctionConstants['failmode'] = data;
        }

        svc.gatherFinished();
      });
    };

    svc.gatherServiceLocatorTypes = function () {
      var yangModuleToParse = {module: [{name: "service-locator"}]};

      yangUtilsSfc.processModules(yangModuleToParse, function (node) {
        var serviceLocatorTypeArray = [];
        var serviceLocatorTypeFormFields = {};

        _.each(node, function (grouping) {
          if (grouping['type'] == 'grouping' && grouping['label'] == 'data-plane-locator') {

            _.each(grouping['children'], function (choice) {
              if (choice['type'] == 'choice' && choice['label'] == 'locator-type') {

                _.each(choice['children'], function (_case_) {
                  if (_case_['type'] == 'case') {
                    serviceLocatorTypeArray.push(_case_['label']);
                    serviceLocatorTypeFormFields[_case_['label']] = [];

                    _.each(_case_['children'], function (leaf) {
                      if (leaf['type'] == 'leaf') {
                        var type = "";
                        var typePlaceholder = "SFC_PLACEHOLDER_";
                        var typeRange = [];
                        var description = "";

                        _.each(leaf['children'], function (field) {
                          if (field['type'] == 'description') {
                            description = field['label'];
                          }
                          else if (field['type'] == 'type') {
                            _.each(field['children'], function (typeParam) {
                              if (typeParam['type'] == 'range') {
                                typeRange = typeParam['label'].split('..');
                              }
                            });
                            type = field['label'];
                            typePlaceholder = typePlaceholder.concat(type);
                          }
                        });
                        var tmpField = {model: leaf['label'], type: type, typePlaceholder: typePlaceholder, typeRange: typeRange, label: description};
                        serviceLocatorTypeFormFields[_case_['label']].push(tmpField);
                      }
                    });
                  }
                });
              }
            });
          }
        });

        if (!_.isEmpty(serviceLocatorTypeArray)) {
          if (angular.isUndefined($rootScope.serviceLocatorConstants)) {
            $rootScope.serviceLocatorConstants = {type: [], typeFormModels: {}, typeFormFields: {}, transport: []};
          }
          $rootScope.serviceLocatorConstants['type'] = serviceLocatorTypeArray;
          $rootScope.serviceLocatorConstants['typeFormFields'] = serviceLocatorTypeFormFields;
        }

        svc.gatherFinished();
      });
    };

    svc.gatherServiceLocatorTransportTypes = function () {
      svc.gatherIdentityNames('service-locator', 'transport-type', function (serviceLocatorTransportIdentityArray) {

        _.each(serviceLocatorTransportIdentityArray, function (transportIdentity) {
          svc.gatherIdentityNames('service-locator', transportIdentity, function (data) {
            if (!_.isEmpty(data)) {
              if (angular.isUndefined($rootScope.serviceLocatorConstants)) {
                $rootScope.serviceLocatorConstants = {type: [], typeFormFields: {}, transport: []};
              }
              _.each(data, function (item) {
                $rootScope.serviceLocatorConstants['transport'].push(item);
              });
            }
          });
        });

        svc.gatherFinished();
      });

      svc.removeTypePrefix = function (type) {

        var typeStringArray = type.split(':');

        if (typeStringArray.length >= 1) {
          return typeStringArray[1];
        }
        else {
          return type;
        }
      };
    };

    return svc;
  });

  sfc.config(function ($stateProvider, $compileProvider, $controllerProvider, $provide, NavHelperProvider, $translateProvider, $translatePartialLoaderProvider) {
    sfc.register = {
      controller: $controllerProvider.register,
      directive: $compileProvider.directive,
      factory: $provide.factory,
      service: $provide.service
    };

    $translatePartialLoaderProvider.addPart('app/sfc/assets/data/locale');

    NavHelperProvider.addControllerUrl('app/sfc/sfc.controller');
    NavHelperProvider.addToMenu('sfc', {
      "link": "#/sfc/serviceforwarder",
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
      // access: access.public,
      views: {
        'content': {
          templateUrl: 'src/app/sfc/root.tpl.html',
          controller: 'rootSfcCtrl'
        }
      }/*,
      resolve: {
        translateLoaded: function ($rootScope) {
          return $rootScope.translateLoadingEnd.promise;
        },
        dummy: "sfcLoaderSvc"
      }*/
    });

    // $stateProvider.state('main.sfc.index', {
    //     url: '/index',
    //     access: access.admin,
    //     views: {
    //         '': {
    //             // controller: 'rootSfcCtrl',
    //             templateUrl: 'src/app/sfc/index.tpl.html'
    //         }
    //     }
    // });

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
      url: '/servicenode-edit-:snName',
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
      url: '/serviceforwarder-edit-:sffName',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/serviceforwarder/serviceforwarder.create.tpl.html',
          controller: 'serviceForwarderCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.servicefunction-stats', {
      url: '/servicefunction-stats-:sfName',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/servicefunction/servicefunction.stats.tpl.html',
          controller: 'serviceFunctionCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.servicepath-vnbar', {
      url: '/servicepath-vnbar-:sfp',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/servicepath/renderedservicepath/servicepath.vnbar.tpl.html',
          controller: 'serviceFunctionCreateCtrl'
        }
      }
    });
    $stateProvider.state('main.sfc.serviceforwarder-clone', {
      url: '/serviceforwarder-edit-:sff',
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
      url: '/servicefunction-edit-:sfName',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/servicefunction/servicefunction.create.tpl.html',
          controller: 'serviceFunctionCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.servicefunction-clone', {
      url: '/servicefunction-clone-:sf',
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
      url: '/acl-create',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/acl/acl.create.tpl.html',
          controller: 'sfcAclCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.acl-edit', {
      url: '/acl-edit-:itemKey',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/acl/acl.create.tpl.html',
          controller: 'sfcAclCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.classifier-create', {
      url: '/classifier-create',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/acl/acl.classifier.create.tpl.html',
          controller: 'sfcClassifierCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.classifier-edit', {
      url: '/classifier-edit-:itemKey',
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
      url: '/metadata-context-create',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/metadata/metadata.context.create.tpl.html',
          controller: 'sfcMetadataContextCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.metadata-context-edit', {
      url: '/metadata-context-edit-:itemKey',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/metadata/metadata.context.create.tpl.html',
          controller: 'sfcMetadataContextCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.metadata-variable-create', {
      url: '/metadata-variable-create',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/metadata/metadata.variable.create.tpl.html',
          controller: 'sfcMetadataVariableCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.metadata-variable-edit', {
      url: '/metadata-variable-edit-:itemKey',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/metadata/metadata.variable.create.tpl.html',
          controller: 'sfcMetadataVariableCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.ipfix', {
      url: '/ipfix',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/ipfix/ipfix.tpl.html',
          controller: 'sfcIpfixCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.ipfix-classid-create', {
      url: '/ipfix-classid-create',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/ipfix/ipfix.classid.create.tpl.html',
          controller: 'sfcIpfixClassIdCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.ipfix-classid-edit', {
      url: '/ipfix-classid-edit-:itemKey',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/ipfix/ipfix.classid.create.tpl.html',
          controller: 'sfcIpfixClassIdCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.ipfix-appid-create', {
      url: '/ipfix-appid-create',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/ipfix/ipfix.appid.create.tpl.html',
          controller: 'sfcIpfixAppIdCreateCtrl'
        }
      }
    });

    $stateProvider.state('main.sfc.ipfix-appid-edit', {
      url: '/ipfix-appid-edit-:itemKey',
      access: access.public,
      views: {
        'sfc': {
          templateUrl: 'src/app/sfc/ipfix/ipfix.appid.create.tpl.html',
          controller: 'sfcIpfixAppIdCreateCtrl'
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

  angular.module('ngTable').run(['$templateCache', '$filter', function ($templateCache, $filter) {
    $templateCache.put('ng-table/filters/text.html', '<input type="text" name="{{column.filterName}}" ng-model="params.filter()[name]" ng-if="filter==\'text\'" class="input-filter form-control" placeholder="{{\'SFC_SEARCH_BY\' | translate}} {{name}}"/>');
  }]);

  return sfc;
});
