define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('serviceLocator', function () {
    return {
      restrict: 'E',
      templateUrl: 'src/app/sfc/servicelocator/servicelocator.tpl.html',
      scope: {
        idSuffix: '@idSuffix',
        service_locator: '=locator',
        serviceLocatorConstants: '=constants'
      },
      controller: 'serviceLocatorCtrl'
    };
  });

  sfc.register.directive('serviceLocatorDynamic', function () {
    return {
      restrict: 'E',
      templateUrl: 'src/app/sfc/servicelocator/servicelocator.dynamic.tpl.html',
      replace: true,
      scope: {
        idSuffix: '@idSuffix',
        service_locator: '=locator',
        locatorType: '@locatorType',
        formFields: '=formFields',
        resetOn: '@resetOn',
        notResetCondition: '@notResetCondition'
      },
      controller: 'serviceLocatorCtrlDynamic'
    };

  });

  sfc.register.directive('serviceLocatorIp', function () {
    return {
      restrict: 'E',
      templateUrl: 'src/app/sfc/servicelocator/servicelocator.ip.tpl.html',
      replace: true,
      scope: {
        idSuffix: '@idSuffix',
        service_locator: '=locator',
        resetOn: '@resetOn',
        notResetCondition: '@notResetCondition'
      },
      controller: 'serviceLocatorCtrlIp'
    };
  });

  sfc.register.directive('serviceLocatorMac', function () {
    return {
      restrict: 'E',
      templateUrl: 'src/app/sfc/servicelocator/servicelocator.mac.tpl.html',
      replace: true,
      scope: {
        idSuffix: '@idSuffix',
        service_locator: '=locator',
        resetOn: '@resetOn',
        notResetCondition: '@notResetCondition'
      },
      controller: 'serviceLocatorCtrlMac'
    };
  });

  sfc.register.directive('serviceLocatorLisp', function () {
    return {
      restrict: 'E',
      templateUrl: 'src/app/sfc/servicelocator/servicelocator.lisp.tpl.html',
      replace: true,
      scope: {
        idSuffix: '@idSuffix',
        service_locator: '=locator',
        resetOn: '@resetOn',
        notResetCondition: '@notResetCondition'
      },
      controller: 'serviceLocatorCtrlLisp'
    };
  });

  sfc.register.directive('serviceLocatorSelector', function () {
    return {
      restrict: 'E',
      templateUrl: 'src/app/sfc/servicelocator/servicelocator.selector.tpl.html',
      scope: {
        service_function: '=function'
      },
      controller: 'serviceLocatorSelectorCtrl'
    };
  });

});