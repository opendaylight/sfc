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

        $scope.onSourcePortRangeCheckChanged = function (boolValue) {
          if (boolValue) {
            $scope['matches']['source-port-range'] = {};
          } else {
            delete $scope['matches']['source-port-range'];
          }
        };

        $scope.onDestinationPortRangeCheckChanged = function (boolValue) {
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
            delete $scope['matches']['destination-ipv4-network'];
            delete $scope['matches']['source-ipv4-network'];
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
            delete $scope['matches']['destination-ipv4-network'];
            delete $scope['matches']['source-ipv4-network'];
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
            delete $scope['matches']['destination-ipv6-network'];
            delete $scope['matches']['source-ipv6-network'];
            delete $scope['matches']['flow-label'];
          }
        });
      }
    };
  });

  sfc.register.directive('aclIpfixMatches', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: 'src/app/sfc/acl/acl.ipfix.tpl.html',
      scope: {
        idSuffix: '@idSuffix',
        matches: '=matches',
        classid: '=classid',
        appid: '=appid',
        resetOn: '@resetOn',
        notResetCondition: '@notResetCondition',
      },
      controller: function ($scope) {
        $scope.appid_filtered = []; // initial data

        $scope.$on($scope.resetOn, function (event, arg) {
          if (arg != $scope.notResetCondition) {
            delete $scope['matches']['service-function-acl:application-id'];
          }
        });

        $scope.classIdChanged = function (cid, matches){
          $scope.appid_filtered = [];
          if(cid !== "") {
            if (angular.isUndefined(matches['service-function-acl:application-id'])) {
              matches['service-function-acl:application-id'] = [];
            }
            for (var i = 0, len = $scope.appid.length; i < len; i++) {
              // Check the class-id and if the applicationId is not already selected
              if($scope.appid[i]['class-id'] == cid && 
                  matches['service-function-acl:application-id'].indexOf($scope.appid[i]['applicationName']) == -1 ) {
                $scope.appid_filtered.push($scope.appid[i]);
              }
            }

          }
        };

        $scope.appIdSelected = function (aid, matches){
          if(aid !== "") {
            if (angular.isUndefined(matches['service-function-acl:application-id'])) {
              matches['service-function-acl:application-id'] = [];
            }
            matches['service-function-acl:application-id'].push(aid);
            $scope.classIdChanged($scope.sel_classid, matches); // update appid dropdown list
          }
        };
      }
    };
  });

  sfc.register.directive('classifierSff', function () {
    return {
      restrict: 'E',
      replace: true,
      templateUrl: 'src/app/sfc/acl/acl.classifier.create.sff.tpl.html',
      scope: {
        idSuffix: '@idSuffix',
        sffs: '=',
        sff: '=',
        classifierConstants: '='
      },
      controller: function ($scope) {
        var thisCtrl = this;
        $scope.attachmentPointType = {};

        $scope.$watch('sff', function (newVal) {
          if (angular.isUndefined(newVal)) {
            return;
          }

          $scope.brNames = thisCtrl.getSFFbridgeNames(newVal['name']);

          if (angular.isDefined(newVal['bridge'])) {
            $scope.attachmentPointType = $scope.classifierConstants['attachment-point-type'][0];
          }
          else if (angular.isDefined(newVal['interface'])) {
            $scope.attachmentPointType = $scope.classifierConstants['attachment-point-type'][1];
          }
        }, true);

        $scope.resetAttachmentPointType = function (sff) {
          $scope.attachmentPointType = {};
          $scope.resetAttachmentPoints(sff);
        };

        $scope.resetAttachmentPoints = function (sff) {
          if (angular.isDefined(sff['bridge'])) {
            delete sff['bridge'];
          }
          if (angular.isDefined(sff['interface'])) {
            delete sff['interface'];
          }
        };

        this.getSFFbridgeNames = function (sffName) {
          var bridgeNames = [];
          var sff = _.findWhere($scope.sffs, {name: sffName});

          if (angular.isDefined(sff)) {
            _.each(sff['sff-data-plane-locator'], function (sffDpLocator) {
              if (angular.isDefined(sffDpLocator['ovs-bridge'])) {
                bridgeNames.push(sffDpLocator['ovs-bridge']['bridge-name']);
              }
            });
          }

          return bridgeNames;
        };
      }
    };
  });

});