define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServiceLocatorHelper', function () {
    var svc = {};

    var serviceLocatorType = {IP: "ip", MAC: "mac", LISP: "lisp"};
    if (angular.isDefined(Object.freeze)) {
      Object.freeze(serviceLocatorType);
    }

    svc.getLocatorType = function (locator) {
      if (!_.isEmpty(locator['ip'] || !_.isEmpty(locator['port']))){
        return serviceLocatorType.IP;
      }
      else if (!_.isEmpty(locator['eid'])) {
        return serviceLocatorType.LISP;
      }
      else if (!_.isEmpty(locator['mac']) || !_.isEmpty(locator['vlan-id'])){
        return serviceLocatorType.MAC;
      }
    };

    svc.getLocatorName = function (locator, locatorObjectArray) {
      var locatorModel;
      var locatorType = svc.getLocatorType(locator);

      switch (locatorType) {
        case serviceLocatorType.IP :
          locatorModel = _.findWhere(locatorObjectArray, {ip: locator['ip'], port: locator['port']});
          break;
        case serviceLocatorType.LISP :
          locatorModel = _.findWhere(locatorObjectArray, {eid: locator['eid']});
          break;
        case serviceLocatorType.MAC :
          locatorModel = _.findWhere(locatorObjectArray, {mac: locator['mac'], 'vlan-id': locator['vlan-id']});
          break;
      }

      return angular.isDefined(locatorModel) ? locatorModel['name'] : undefined;
    };

    svc.locatorToString = function (locator, $scope) {
      var locatorType = svc.getLocatorType(locator);
      var locatorString = $scope.$eval('"SFC_TOOLTIP_NAME" | translate') + ": " + locator['name'] + ", ";

      switch (locatorType) {
        case serviceLocatorType.IP :
          locatorString += $scope.$eval('"SFC_TOOLTIP_IP" | translate') + ": " + locator['ip'] + ", " + $scope.$eval('"SFC_TOOLTIP_PORT" | translate') + ": " + locator['port'] + ", ";
          break;
        case serviceLocatorType.LISP :
          locatorString += $scope.$eval('"SFC_TOOLTIP_EID" | translate') + ": " + locator['eid'] + ", ";
          break;
        case serviceLocatorType.MAC :
          locatorString += $scope.$eval('"SFC_TOOLTIP_MAC" | translate') + ": " + locator['mac'] + ", " + $scope.$eval('"SFC_TOOLTIP_VLAN_ID" | translate') + ": " + locator['vlan-id'] + ", ";
          break;
      }
      locatorString += $scope.$eval('"SFC_TOOLTIP_TRANSPORT" | translate') + ": " + locator['transport'];
      return locatorString;
    };

    svc.getLocatorTooltipText = function (locator, $scope) {
      var locatorType = svc.getLocatorType(locator);
      var locatorTooltip = $scope.$eval('"SFC_TOOLTIP_DATA_PLANE_LOCATOR" | translate') + ": " + "<br/>";

      switch (locatorType) {
        case serviceLocatorType.IP :
          locatorTooltip += $scope.$eval('"SFC_TOOLTIP_IP" | translate') + ": " + locator['ip'] + ", " + $scope.$eval('"SFC_TOOLTIP_PORT" | translate') + ": " + locator['port'] + "<br/>";
          break;
        case serviceLocatorType.LISP :
          locatorTooltip += $scope.$eval('"SFC_TOOLTIP_EID" | translate') + ": " + locator['eid'] + "<br/>";
          break;
        case serviceLocatorType.MAC :
          locatorTooltip += $scope.$eval('"SFC_TOOLTIP_MAC" | translate') + ": " + locator['mac'] + ", " + $scope.$eval('"SFC_TOOLTIP_VLAN_ID" | translate') + ": " + locator['vlan-id'] + "<br/>";
          break;
      }
      locatorTooltip += $scope.$eval('"SFC_TOOLTIP_TRANSPORT" | translate') + ": " + locator['transport'];
      return locatorTooltip;
    };

    svc.getSfLocatorTooltipText = function (locator, $scope) {
      var sfLocatorTooltip = svc.getLocatorTooltipText(locator, $scope) + "<br/>" + "<br/>";
      sfLocatorTooltip += $scope.$eval('"SFC_TOOLTIP_FORWARDER" | translate') + ": " + locator['service-function-forwarder'];

      return sfLocatorTooltip;
    };

    return svc;
  });
});
