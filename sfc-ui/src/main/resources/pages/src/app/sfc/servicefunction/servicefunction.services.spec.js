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

        it("should return proper select2Options", function () {
          var options = ServiceFunctionHelper.select2Options(exampleData);
          expect(options.query).toBeDefined();
          expect(options.formatSelection).toBeDefined();
        });

        it("select2Options.query function should return result data during exact match", function () {
          var options = ServiceFunctionHelper.select2Options(exampleData);
          var result = {};
          options.query.callback = function (data){
            result = data;
          };
          options.query.term = ("sff1");
          options.query(options.query);
          expect(result).toEqual({results: [{id: "sff1", text: "sff1"}]});
        });

        it("select2Options.query function should return result data during not exact match", function () {
          var options = ServiceFunctionHelper.select2Options(exampleData);
          var result = {};
          options.query.callback = function (data){
            result = data;
          };
          options.query.term = ("Ff1");
          options.query(options.query);
          expect(result).toEqual({results: [{id: "Ff1", text: "Ff1", ne: true}, {id: "sff1", text: "sff1"}]});
        });

        it("select2Options.query function should return all result data during blank input", function () {
          var options = ServiceFunctionHelper.select2Options(exampleData);
          var result = {};
          options.query.callback = function (data){
            result = data;
          };
          options.query.term = ("");
          options.query(options.query);
          expect(result).toEqual({results: [{id: "sff1", text: "sff1"}, {id: "sff2", text: "sff2"}]});
        });

        it("select2Options.query function should return result data during unmatch", function () {
          var options = ServiceFunctionHelper.select2Options(exampleData);
          var result = {};
          options.query.callback = function (data){
            result = data;
          };
          options.query.term = ("bla");
          options.query(options.query);
          expect(result).toEqual({results: [{id: "bla", text: "bla", ne: true}]});
        });

        it("select2Options.format should properly format displayed text", function () {
          var options = ServiceFunctionHelper.select2Options(exampleData);
          var selectedExisting = {text: "sff1"};
          expect(options.formatSelection(selectedExisting)).toEqual(selectedExisting.text);
          var selectedNotExisting = {text: "bla", ne: true};
          expect(options.formatSelection(selectedNotExisting)).toEqual(selectedNotExisting.text + " <span><i style=\"color: greenyellow;\">(to be created)</i></span>");
        });
      });
    });
  });
});
