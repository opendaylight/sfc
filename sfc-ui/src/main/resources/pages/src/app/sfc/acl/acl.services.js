define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory("SfcAclHelper", function () {
    var svc = {};

    svc.addAce = function ($scope) {
      if (angular.isUndefined($scope.data['access-list-entries'])) {
        $scope.data['access-list-entries'] = [];
      }
      $scope.data['access-list-entries'].push({});
    };

    svc.removeAce = function (index, $scope) {
      $scope.data['access-list-entries'].splice(index, 1);
    };

    svc.valueOfAceType = function (matches, $rootScope) {
      var r1, r2;

      if (matches['destination-mac-address'] ||
        matches['destination-mac-address-mask'] ||
        matches['source-mac-address'] ||
        matches['source-mac-address-mask']) {
        r1 = $rootScope.aclConstants['ace-type'][1]; // mac
      }

      if (svc.valueOfIpVersion(matches, $rootScope) ||
        matches['source-port-range'] ||
        matches['destination-port-range'] ||
        matches['ip-protocol'] ||
        matches['dscp']) {
        r2 = $rootScope.aclConstants['ace-type'][0]; // ip
      }

      if (r1 && r2) {
        throw new Error("IP/ETH mismatch");
      }

      return r1 || r2;
    };

    svc.valueOfIpVersion = function (matches, $rootScope) {
      var r1, r2;

      if (matches['destination-ipv4-address'] ||
        matches['source-ipv4-address']) {
        r1 =  $rootScope.aclConstants['ace-ip'][0]; // ipv4
      }

      if (matches['destination-ipv6-address'] ||
        matches['source-ipv6-address'] ||
        matches['flow-label']) {
        r2 =  $rootScope.aclConstants['ace-ip'][1]; // ipv6
      }

      if (r1 && r2) {
        throw new Error("IP version mismatch");
      }

      return r1 || r2;
    };

//    svc.matchesToString = function (matches, $scope) {
//      var str = "";
//
//      if(angular.isDefined(matches['source-port-range'])) {
//        str += $scope.$eval('"SFC_ACL_SHORT_SRC_PORT_RANGE" | translate') + ": ";
//        if (angular.isDefined(matches['source-port-range']['lower-port'])) {
//          str += matches['source-port-range']['lower-port'] + "-";
//        }
//        if (angular.isDefined(matches['source-port-range']['upper-port'])) {
//          str += matches['source-port-range']['upper-port'];
//        }
//        str += ", ";
//      }
//
//      if(angular.isDefined(matches['destination-port-range'])) {
//        str += $scope.$eval('"SFC_ACL_SHORT_DST_PORT_RANGE" | translate') + ": ";
//        if (angular.isDefined(matches['destination-port-range']['lower-port'])) {
//          str += matches['destination-port-range']['lower-port'] + "-";
//        }
//        if (angular.isDefined(matches['destination-port-range']['upper-port'])) {
//          str += matches['destination-port-range']['upper-port'];
//        }
//        str += ", ";
//      }
//
//      if(angular.isDefined(matches['dscp'])) {
//        str += $scope.$eval('"SFC_ACL_SHORT_DSCP" | translate') + ": " + matches['dscp'] + ", ";
//      }
//
//      if(angular.isDefined(matches['ip-protocol'])){
//        str += $scope.$eval('"SFC_ACL_SHORT_IP_PROTOCOL" | translate') + ": " + matches['ip-protocol'] + ", ";
//      }
//
//      if(angular.isDefined(matches['destination-ipv4-address'])){
//        str += $scope.$eval('"SFC_ACL_SHORT_DST_IP_ADDRESS" | translate') + ": " + matches['destination-ipv4-address'] + ", ";
//      }
//
//      if(angular.isDefined(matches['source-ipv4-address'])){
//        str += $scope.$eval('"SFC_ACL_SHORT_SRC_IP_ADDRESS" | translate') + ": " + matches['source-ipv4-address'] + ", ";
//      }
//
//      if(angular.isDefined(matches['destination-ipv6-address'])){
//        str += $scope.$eval('"SFC_ACL_SHORT_DST_IP_ADDRESS" | translate') + ": " + matches['destination-ipv6-address'] + ", ";
//      }
//
//      if(angular.isDefined(matches['source-ipv6-address'])){
//        str += $scope.$eval('"SFC_ACL_SHORT_SRC_IP_ADDRESS" | translate') + ": " + matches['source-ipv6-address'] + ", ";
//      }
//
//      if(angular.isDefined(matches['flow-label'])){
//        str += $scope.$eval('"SFC_ACL_SHORT_IPV6_FLOW_LABEL" | translate') + ": " + matches['flow-label'] + ", ";
//      }
//
//      if(angular.isDefined(matches['destination-mac-address'])){
//        str += $scope.$eval('"SFC_ACL_SHORT_DST_MAC_ADDRESS" | translate') + ": " + matches['destination-mac-address'];
//        if(angular.isDefined(matches['destination-mac-address-mask'])){
//          str += " (" + $scope.$eval('"SFC_ACL_SHORT_MAC_ADDRESS_MASK" | translate') + ": " + matches['destination-mac-address-mask'] +")";
//        }
//        str += ", ";
//      }
//
//      if(angular.isDefined(matches['source-mac-address'])){
//        str += $scope.$eval('"SFC_ACL_SHORT_SRC_MAC_ADDRESS" | translate') + ": " + matches['source-mac-address'];
//        if(angular.isDefined(matches['source-mac-address-mask'])){
//          str += " (" + $scope.$eval('"SFC_ACL_SHORT_MAC_ADDRESS_MASK" | translate') + ": " + matches['source-mac-address-mask'] +")";
//        }
//        str += ", ";
//      }
//
//      str = str.slice(0, -2);
//
//      return str;
//    };
//
//    svc.metadataToString = function (matches, $scope) {
//      var str = "";
//
//      if(angular.isDefined(matches['input-interface'])){
//        str += $scope.$eval('"SFC_ACL_SHORT_METADATA_INPUT_INTERFACE" | translate') + ": " + matches['input-interface'] + ", ";
//      }
//
//      if(angular.isDefined(matches['absolute'])){
//        if(angular.isDefined(matches['absolute']['start'])){
//          str += $scope.$eval('"SFC_ACL_SHORT_METADATA_ABSOLUTE_START" | translate') + ": " + matches['absolute']['start'] + ", ";
//        }
//        if(angular.isDefined(matches['absolute']['end'])){
//          str += $scope.$eval('"SFC_ACL_SHORT_METADATA_ABSOLUTE_END" | translate') + ": " + matches['absolute']['end'] + ", ";
//        }
//        if(angular.isDefined(matches['absolute']['active'])){
//          str += $scope.$eval('"SFC_ACL_SHORT_METADATA_ABSOLUTE_ACTIVE" | translate') + ": " + matches['absolute']['active'] + ", ";
//        }
//      }
//
//      str = str.slice(0, -2);
//      return str;
//    };

    svc.sourceIpMacMaskToString = function (matches) {
      var str = "";
      str += (matches['source-ipv4-address']) ? matches['source-ipv4-address'] + " ":  "";
      str += (matches['source-ipv6-address']) ? matches['source-ipv6-address'] + " " : "";
      str += (matches['source-mac-address']) ? matches['source-mac-address'] + " " : "";
      str += (matches['source-mac-address-mask']) ? matches['source-mac-address-mask'] + " " : "";
      return str;
    };

    svc.destinationIpMacMaskToString = function (matches) {
      var str = "";
      str += (matches['destination-ipv4-address']) ? matches['destination-ipv4-address'] + " " :  "";
      str += (matches['destination-ipv6-address']) ? matches['destination-ipv6-address'] + " " : "";
      str += (matches['destination-mac-address']) ? matches['destination-mac-address'] + " " : "";
      str += (matches['destination-mac-address-mask']) ? matches['destination-mac-address-mask'] + " " : "";
      return str;
    };

    svc.flowLabelToString = function (matches) {
      var str = "";
      str += (matches['flow-label']) ? matches['flow-label'] : "";
      return str;
    };

    svc.sourcePortRangeToString = function (sourcePortRange) {
      if(angular.isUndefined(sourcePortRange)){
        return "";
      }
      var str = "";
      str += (sourcePortRange['lower-port']) ? sourcePortRange['lower-port'] + " " : "";
      str += (sourcePortRange['upper-port']) ? sourcePortRange['upper-port'] + " " : "";
      return str;
    };

    svc.destinationPortRangeToString = function (destinationPortRange) {
      if(angular.isUndefined(destinationPortRange)){
        return "";
      }
      var str = "";
      str += (destinationPortRange['lower-port']) ? destinationPortRange['lower-port'] + " " : "";
      str += (destinationPortRange['upper-port']) ? destinationPortRange['upper-port'] + " " : "";
      return str;
    };

    svc.dscpToString = function (matches) {
      var str = "";
      str += (matches['dscp']) ? matches['dscp'] : "";
      return str;
    };

    svc.ipProtocolToString = function (matches) {
      var str = "";
      str += (matches['ip-protocol']) ? matches['ip-protocol'] : "";
      return str;
    };

    return svc;
  });

  sfc.register.factory('SfcAclModalMetadata', function ($modal) {
    var svc = {};
    var modalInstance;

    svc.open = function (ace, callback) {
      modalInstance = $modal.open({
        templateUrl: 'src/app/sfc/acl/acl.modal.metadata.tpl.html',
        backdrop: false,
        size: "sm",
        controller: 'sfcAclModalMetadataCtrl',
        resolve: {
          ace: function () {
            return ace;
          }
        }
      });

      modalInstance.result.then(function (result) {
        callback(result);
      }, function (reason) {
        callback(reason);
      });

      return modalInstance;
    };
    return svc;
  });

});