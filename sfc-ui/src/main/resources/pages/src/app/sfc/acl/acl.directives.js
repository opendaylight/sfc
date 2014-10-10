define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('aclEthernetMatches', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: 'src/app/sfc/acl/acl.ethernet.tpl.html',
      scope: {
        idSuffix: '@idSuffix',
        matches: '=matches',
        resetOn: '@resetOn',
        notResetCondition: '@notResetCondition'
      },
      controller: function ($scope) {

        $scope.$on($scope.resetOn, function (event, arg) {
          if (arg != $scope.notResetCondition) {
            delete $scope['matches']['destination-mac-address'];
            delete $scope['matches']['destination-mac-address-mask'];
            delete $scope['matches']['source-mac-address'];
            delete $scope['matches']['source-mac-address-mask'];
          }
        });
      }
    };
  });

  sfc.register.directive('aclIpMatches', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: 'src/app/sfc/acl/acl.ip.tpl.html',
      scope: {
        idSuffix: '@idSuffix',
        matches: '=matches',
        aclConstants: '=aclConstants',
        resetOn: '@resetOn',
        notResetCondition: '@notResetCondition'
      },
      controller: function ($scope, $rootScope, SfcAclHelper) {

        $scope.valueOfIpVersion = function (matches) {
          return SfcAclHelper.valueOfIpVersion(matches, $rootScope);
        };

        $scope.onAceIpChange = function (arg) {
          $scope.$broadcast('ace_ip_change', arg);
        };

        $scope.onSourcePortRangeCheckChanged = function(boolValue) {
          if (boolValue) {
            $scope['matches']['source-port-range'] = {};
          } else {
            delete $scope['matches']['source-port-range'];
          }
        };

        $scope.onDestinationPortRangeCheckChanged = function(boolValue) {
          if (boolValue) {
            $scope['matches']['destination-port-range'] = {};
          } else {
            delete $scope['matches']['destination-port-range'];
          }
        };

        $scope.$on($scope.resetOn, function (event, arg) {
          if (arg != $scope.notResetCondition) {
            delete $scope['matches']['destination-port-range'];
            delete $scope['matches']['source-port-range'];
            delete $scope['matches']['destination-ipv4-address'];
            delete $scope['matches']['source-ipv4-address'];
            $scope.$broadcast('ace_ip_change', null);
          }
        });
      }
    };
  });

  sfc.register.directive('aclIpMatchesV4', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: 'src/app/sfc/acl/acl.ip.v4.tpl.html',
      scope: {
        idSuffix: '@idSuffix',
        matches: '=matches',
        resetOn: '@resetOn',
        notResetCondition: '@notResetCondition'
      },
      controller: function ($scope) {

        $scope.$on($scope.resetOn, function (event, arg) {
          if (arg != $scope.notResetCondition) {
            delete $scope['matches']['destination-ipv4-address'];
            delete $scope['matches']['source-ipv4-address'];
          }
        });
      }
    };
  });

  sfc.register.directive('aclIpMatchesV6', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: 'src/app/sfc/acl/acl.ip.v6.tpl.html',
      scope: {
        idSuffix: '@idSuffix',
        matches: '=matches',
        resetOn: '@resetOn',
        notResetCondition: '@notResetCondition'
      },
      controller: function ($scope) {

        $scope.$on($scope.resetOn, function (event, arg) {
          if (arg != $scope.notResetCondition) {
            delete $scope['matches']['destination-ipv6-address'];
            delete $scope['matches']['source-ipv6-address'];
            delete $scope['matches']['flow-label'];
          }
        });
      }
    };
  });


});