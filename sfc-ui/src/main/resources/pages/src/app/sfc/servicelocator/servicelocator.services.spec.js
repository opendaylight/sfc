define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, httpBackend, exampleData;

    beforeEach(angular.mock.module('pascalprecht.translate'));
    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($rootScope, $httpBackend) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
      httpBackend = $httpBackend;
    }));

    beforeEach(function (){
      exampleData = {};
      exampleData.ipLocator = {ip: '10.0.0.1', port: "200", transport: "vxlan", 'service-function-forwarder': 'sff1'};
      exampleData.macLocator = {mac: 'aa:bb:cc:dd:ee:ff', 'vlan-id': "5", transport: "vxlan"};
      exampleData.lispLocator = {eid: '10.0.0.1', transport: "vxlan"};
      exampleData.locatorArray = [
        {name: "ipLocator", ip: '10.0.0.1', port: "200"},
        {name: "macLocator", mac: 'aa:bb:cc:dd:ee:ff', 'vlan-id': "5"},
        {name: "lispLocator", eid: '10.0.0.1'}
      ];
    });

    describe('servicelocator.services', function () {

      describe('ServiceLocatorHelper', function () {

        var ServiceLocatorHelper;

        beforeEach(angular.mock.inject(function (_ServiceLocatorHelper_) {
          ServiceLocatorHelper = _ServiceLocatorHelper_;
        }));

        it("getLocatorType should return locator type depending on defined properties", function () {
          var locatorType;
          locatorType = ServiceLocatorHelper.getLocatorType(exampleData.ipLocator);
          expect(locatorType).toEqual('ip');

          locatorType = ServiceLocatorHelper.getLocatorType(exampleData.macLocator);
          expect(locatorType).toEqual('mac');

          locatorType = ServiceLocatorHelper.getLocatorType(exampleData.lispLocator);
          expect(locatorType).toEqual('lisp');

          locatorType = ServiceLocatorHelper.getLocatorType({});
          expect(locatorType).toBeUndefined();
        });

        it("getLocatorName should return locator name (depending on defined properties) found in array of locators", function () {
          var locatorName;
          locatorName = ServiceLocatorHelper.getLocatorName(exampleData.ipLocator, exampleData.locatorArray);
          expect(locatorName).toEqual('ipLocator');

          locatorName = ServiceLocatorHelper.getLocatorName(exampleData.macLocator, exampleData.locatorArray);
          expect(locatorName).toEqual('macLocator');

          locatorName = ServiceLocatorHelper.getLocatorName(exampleData.lispLocator, exampleData.locatorArray);
          expect(locatorName).toEqual('lispLocator');

          locatorName = ServiceLocatorHelper.getLocatorName({}, exampleData.locatorArray);
          expect(locatorName).toBeUndefined();
        });

        it("locatorToString should return string representation of locato depending on its type", function () {
          var string;
          string = ServiceLocatorHelper.locatorToString(exampleData.ipLocator, scope);
          expect(string).toEqual(scope.$eval('"SFC_TOOLTIP_NAME" | translate') + ": " + exampleData.ipLocator['name'] + ", " +
            scope.$eval('"SFC_TOOLTIP_IP" | translate') + ": " + exampleData.ipLocator['ip'] + ", " +
            scope.$eval('"SFC_TOOLTIP_PORT" | translate') + ": " + exampleData.ipLocator['port'] + ", " +
            scope.$eval('"SFC_TOOLTIP_TRANSPORT" | translate') + ": " + exampleData.ipLocator['transport']);

          string = ServiceLocatorHelper.locatorToString(exampleData.macLocator, scope);
          expect(string).toEqual(scope.$eval('"SFC_TOOLTIP_NAME" | translate') + ": " + exampleData.macLocator['name'] + ", " +
            scope.$eval('"SFC_TOOLTIP_MAC" | translate') + ": " + exampleData.macLocator['mac'] + ", " +
            scope.$eval('"SFC_TOOLTIP_VLAN_ID" | translate') + ": " + exampleData.macLocator['vlan-id'] + ", " +
            scope.$eval('"SFC_TOOLTIP_TRANSPORT" | translate') + ": " + exampleData.macLocator['transport']);

          string = ServiceLocatorHelper.locatorToString(exampleData.lispLocator, scope);
          expect(string).toEqual(scope.$eval('"SFC_TOOLTIP_NAME" | translate') + ": " + exampleData.lispLocator['name'] + ", " +
            scope.$eval('"SFC_TOOLTIP_EID" | translate') + ": " + exampleData.lispLocator['eid'] + ", " +
            scope.$eval('"SFC_TOOLTIP_TRANSPORT" | translate') + ": " + exampleData.lispLocator['transport']);
        });

        it("getLocatorTooltipText should return locator tooltip depending on its type", function () {
          var tooltipString;
          tooltipString = ServiceLocatorHelper.getLocatorTooltipText(exampleData.ipLocator, scope);
          expect(tooltipString).toEqual(scope.$eval('"SFC_TOOLTIP_DATA_PLANE_LOCATOR" | translate') + ": " + "<br/>" +
            scope.$eval('"SFC_TOOLTIP_IP" | translate') + ": " + exampleData.ipLocator['ip'] + ", " +
            scope.$eval('"SFC_TOOLTIP_PORT" | translate') + ": " + exampleData.ipLocator['port'] + "<br/>" +
            scope.$eval('"SFC_TOOLTIP_TRANSPORT" | translate') + ": " + exampleData.ipLocator['transport']);

          tooltipString = ServiceLocatorHelper.getLocatorTooltipText(exampleData.macLocator, scope);
          expect(tooltipString).toEqual(scope.$eval('"SFC_TOOLTIP_DATA_PLANE_LOCATOR" | translate') + ": " + "<br/>" +
            scope.$eval('"SFC_TOOLTIP_MAC" | translate') + ": " + exampleData.macLocator['mac'] + ", " +
            scope.$eval('"SFC_TOOLTIP_VLAN_ID" | translate') + ": " + exampleData.macLocator['vlan-id'] + "<br/>" +
            scope.$eval('"SFC_TOOLTIP_TRANSPORT" | translate') + ": " + exampleData.macLocator['transport']);

          tooltipString = ServiceLocatorHelper.getLocatorTooltipText(exampleData.lispLocator, scope);
          expect(tooltipString).toEqual(scope.$eval('"SFC_TOOLTIP_DATA_PLANE_LOCATOR" | translate') + ": " + "<br/>" +
            scope.$eval('"SFC_TOOLTIP_EID" | translate') + ": " + exampleData.lispLocator['eid'] + "<br/>" +
            scope.$eval('"SFC_TOOLTIP_TRANSPORT" | translate') + ": " + exampleData.ipLocator['transport']);
        });

        it("getSfLocatorTooltipText should return getLocatorTooltipText + sff tooltip", function () {
          var tooltipString;
          tooltipString = ServiceLocatorHelper.getSfLocatorTooltipText(exampleData.ipLocator, scope);
          expect(tooltipString.slice(-4)).toEqual('sff1');
        });

      });
    });
  });
});
