define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var scope, compile;

    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($rootScope, $compile) {
      scope = $rootScope.$new();
      compile = $compile;
    }));

    describe('config.directives', function () {
      var inputElement;

      var compileDirective = function (scope) {
        inputElement = compile('<input id="select_file" type="file" ng-file-select />')(scope);
      };

      it("should call getOnFileSelect function on input change event", function () {
        scope.getOnFileSelect = function (file) {
          // noop;
        };

        spyOn(scope, 'getOnFileSelect').andCallThrough();

        compileDirective(scope);

        inputElement.change();

        expect(scope.getOnFileSelect).toHaveBeenCalled();
      });

    });
  });
});