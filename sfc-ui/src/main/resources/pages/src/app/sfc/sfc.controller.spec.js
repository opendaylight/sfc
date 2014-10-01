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
    });
  });
});
