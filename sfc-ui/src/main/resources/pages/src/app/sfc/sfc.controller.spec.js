define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, state, stateParams;

    var sfcState = {PERSISTED: "persisted", NEW: "new", EDITED: "edited"};
    if (angular.isDefined(Object.freeze)) {
      Object.freeze(sfcState);
    }

    var sfpState = {PERSISTED: "persisted", NEW: "new", EDITED: "edited"};
    if (angular.isDefined(Object.freeze)) {
      Object.freeze(sfpState);
    }

    var serviceFunctionConstants =
    {
      type: ["napt44", "dpi", "firewall"],
      failmode : [ 'open', 'close' ]
    };

    var serviceLocatorConstants =
    {
      transport: ["vxlan-gpe", "gre", "other"],
      type: ["ip", "mac", "lisp"]
    };

    beforeEach(angular.mock.module('ui.router'));
    beforeEach(angular.mock.module('pascalprecht.translate'));
    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($state, $stateParams, $rootScope) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
      state = $state;
      stateParams = $stateParams;
    }));

    describe('sfc.controller', function () {

      describe('rootSfcCtrl', function () {

        var createRootSfcCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createRootSfcCtrl = function () {
            return $controller('rootSfcCtrl', {$rootScope: rootScope});
          };
        }));

        it("$rootScope['section_logo'] must equal 'logo_sfc'", function () {
          createRootSfcCtrl();
          expect(rootScope['section_logo']).toBe('logo_sfc');
        });

        it("$rootScope.sfcState should be properly initialized", function () {
          createRootSfcCtrl();
          expect(rootScope.sfcState).toEqual(sfcState);
        });

        it("$rootScope.sfpState should be properly initialized", function () {
          createRootSfcCtrl();
          expect(rootScope.sfpState).toEqual(sfpState);
        });

        it("$rootScope sfcs, sfps and sfpEffectMe arrays/object should be initialized", function () {
          createRootSfcCtrl();
          expect(rootScope.sfcs).toEqual([]);
          expect(rootScope.sfps).toEqual([]);
          expect(rootScope.sfpEffectMe).toEqual({});
        });

        it("$rootScope.serviceFunctionConstants should be properly initialized", function () {
          createRootSfcCtrl();
          expect(rootScope.serviceFunctionConstants).toEqual(serviceFunctionConstants);
        });

        it("$rootScope.serviceLocatorConstants should be properly initialized", function () {
          createRootSfcCtrl();
          expect(rootScope.serviceLocatorConstants).toEqual(serviceLocatorConstants);
        });
      });

      describe('sfcForwarderSelect2Ctrl', function () {

        var createsfcForwarderSelect2Ctrl, exampleData;

        beforeEach(angular.mock.inject(function ($controller) {
          createsfcForwarderSelect2Ctrl = function () {
            return $controller('sfcForwarderSelect2Ctrl', {$scope: scope});
          };
          exampleData = [{name: 'sff1'}, {name: 'sff2'}];
        }));

        it("scope.sffProp should be set on each $digest to scope.tmpSffForSelect2.id value", function () {
          createsfcForwarderSelect2Ctrl();
          //check if watch was registered
          expect(scope.$$watchers.length).toBe(1);

          //test precondition
          scope.sffProp = undefined;
          scope.tmpSffForSelect2 = undefined;
          scope.$digest();
          expect(scope.sffProp).toBeUndefined();

          //test regular function
          scope.tmpSffForSelect2 = {id: "testId"};
          scope.$digest();
          expect(scope.sffProp).toEqual('testId');
        });

        it("scope.select2Options should be properly defined", function () {
          createsfcForwarderSelect2Ctrl();
          expect(scope.select2Options).toBeDefined();
          expect(scope.select2Options.query).toBeDefined();
          expect(scope.select2Options.formatSelection).toBeDefined();
        });

        it("scope.select2Options.query function should return result data during exact match", function () {
          createsfcForwarderSelect2Ctrl();
          scope.sffs = exampleData;
          var result = {};
          scope.select2Options.query.callback = function (data) {
            result = data;
          };
          scope.select2Options.query.term = ("sff1");
          scope.select2Options.query(scope.select2Options.query);
          expect(result).toEqual({results: [
            {id: "sff1", text: "sff1"}
          ]});
        });

        it("scope.select2Options.query function should return result data during not exact match", function () {
          createsfcForwarderSelect2Ctrl();
          scope.sffs = exampleData;
          var result = {};
          scope.select2Options.query.callback = function (data) {
            result = data;
          };
          scope.select2Options.query.term = ("Ff1");
          scope.select2Options.query(scope.select2Options.query);
          expect(result).toEqual({results: [
            {id: "Ff1", text: "Ff1", ne: true},
            {id: "sff1", text: "sff1"}
          ]});
        });

        it("scope.select2Options.query function should return all result data during blank input", function () {
          createsfcForwarderSelect2Ctrl();
          scope.sffs = exampleData;
          var result = {};
          scope.select2Options.query.callback = function (data) {
            result = data;
          };
          scope.select2Options.query.term = ("");
          scope.select2Options.query(scope.select2Options.query);
          expect(result).toEqual({results: [
            {id: "sff1", text: "sff1"},
            {id: "sff2", text: "sff2"}
          ]});
        });

        it("scope.select2Options.query function should return result data during unmatch", function () {
          createsfcForwarderSelect2Ctrl();
          scope.sffs = exampleData;
          var result = {};
          scope.select2Options.query.callback = function (data) {
            result = data;
          };
          scope.select2Options.query.term = ("bla");
          scope.select2Options.query(scope.select2Options.query);
          expect(result).toEqual({results: [
            {id: "bla", text: "bla", ne: true}
          ]});
        });

        it("scope.select2Options.format should properly format displayed text", function () {
          createsfcForwarderSelect2Ctrl();
          var selectedExisting = {text: "sff1"};
          expect(scope.select2Options.formatSelection(selectedExisting)).toEqual(selectedExisting.text);
          var selectedNotExisting = {text: "bla", ne: true};
          expect(scope.select2Options.formatSelection(selectedNotExisting)).toEqual(selectedNotExisting.text + " <span><i style=\"color: greenyellow;\">(to be created)</i></span>");
        });
      });
    });
  });
});
