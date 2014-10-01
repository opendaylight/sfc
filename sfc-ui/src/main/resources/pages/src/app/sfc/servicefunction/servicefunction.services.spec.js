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
      exampleData.data = {"sf-data-plane-locator": [{}]};
      exampleData.sfDpLocators = [
        {name: "sfdp1"},
        {name: "sfdp2"},
        {name: "sfdp3"}
      ];
      exampleData.sffs = [
        {name: "sff1"},
        {name: "sff2"}
      ];
    });

    describe('servicefunction.services', function () {

      describe('ServiceFunctionHelper', function () {

        var ServiceFunctionHelper;

        beforeEach(angular.mock.inject(function (_ServiceFunctionHelper_) {
          ServiceFunctionHelper = _ServiceFunctionHelper_;
        }));

        it("should create sf-data-plane-locator property and add empty locator to SF", function () {
          exampleData.data = {};
          expect(exampleData.data['sf-data-plane-locator']).toBeUndefined();
          ServiceFunctionHelper.addLocator(exampleData);
          expect(exampleData.data['sf-data-plane-locator'].length).toBe(1);
          expect(exampleData.data['sf-data-plane-locator']).toEqual([{}]);
        });

        it("should add empty data plane locator to SF", function () {
          expect(exampleData.data['sf-data-plane-locator'].length).toBe(1);
          ServiceFunctionHelper.addLocator(exampleData);
          expect(exampleData.data['sf-data-plane-locator'].length).toBe(2);
          expect(exampleData.data['sf-data-plane-locator']).toEqual([{}, {}]);
        });

        it("should remove data plane locator at given index from SF", function () {
          expect(exampleData.data['sf-data-plane-locator'].length).toBe(1);
          ServiceFunctionHelper.removeLocator(0, exampleData);
          expect(exampleData.data['sf-data-plane-locator'].length).toBe(0);
          expect(exampleData.data['sf-data-plane-locator']).toEqual([]);
        });

        it("should return string containing SF DPlocator names", function () {
          var string = ServiceFunctionHelper.sfDpLocatorToString(exampleData.sfDpLocators);
          expect(string).toEqual("sfdp1 sfdp2 sfdp3 ");
        });

      });
    });
  });
});
