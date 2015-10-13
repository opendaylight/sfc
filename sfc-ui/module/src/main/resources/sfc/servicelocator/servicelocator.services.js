define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServiceLocatorHelper', function ($rootScope) {
    var svc = {};

    //returns keys like (ip, port) for specified locatorType
    svc.__getMatchingKeys = function (locatorType) {
      var matchingKeysArray = [];

      _.each($rootScope.serviceLocatorConstants['typeFormFields'][locatorType], function (formField) {
        matchingKeysArray.push(formField['model']);
      });

      return matchingKeysArray;
    };

    svc.getLocatorType = function (locator) {

      var locatorKeys = _.keys(locator);
      var matchingKeys = [];
      var locatorTypeString = null;

      _.find($rootScope.serviceLocatorConstants['type'], function (locatorType) {
        matchingKeys = svc.__getMatchingKeys(locatorType);

        if (_.some(matchingKeys, function (key) {
            return _.contains(locatorKeys, key);
          })) {

          matchingKeys = [];
          locatorTypeString = locatorType;
          return true;
        }
        else {
          matchingKeys = [];
        }
      });

      return locatorTypeString;
    };

    svc.getLocatorName = function (locator, locatorObjectArray) {
      var locatorModel;
      var locatorType = svc.getLocatorType(locator);
      var matchingKeys = svc.__getMatchingKeys(locatorType);
      var matchingObject = {};

      _.each(matchingKeys, function (key) {
        matchingObject[key] = locator[key];
      });

      if (_.isEmpty(Object.keys(matchingObject))) {
        return undefined;
      }

      locatorModel = _.findWhere(locatorObjectArray, matchingObject);
      return angular.isDefined(locatorModel) ? locatorModel['name'] : undefined;
    };

    svc.locatorToString = function (locator, $scope) {
      var locatorType = svc.getLocatorType(locator);
      var locatorString = $scope.$eval('"SFC_TOOLTIP_NAME" | translate') + ": " + locator['name'] + ", ";

      _.each($rootScope.serviceLocatorConstants['typeFormFields'][locatorType], function (field) {
        locatorString += field['label'] + ": " + locator[field['model']] + " ";
      });

      locatorString += $scope.$eval('"SFC_TOOLTIP_TRANSPORT" | translate') + ": " + locator['transport'];
      return locatorString;
    };

    svc.getLocatorTooltipText = function (locator, $scope) {
      var locatorType = svc.getLocatorType(locator);
      var locatorTooltip = $scope.$eval('"SFC_TOOLTIP_DATA_PLANE_LOCATOR" | translate') + ": " + "<br/>";

      _.each($rootScope.serviceLocatorConstants['typeFormFields'][locatorType], function (field) {
          locatorTooltip += field['label'] + ": " + locator[field['model']] + "<br/>";
      });

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
