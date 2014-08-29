define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, modalInstance, infoMock, errorMock, deleteMock;

    infoMock = "test info";
    errorMock= "test error";
    deleteMock = "test delete";


    beforeEach(angular.mock.module('ui.router'));
    beforeEach(angular.mock.module('pascalprecht.translate'));
    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($rootScope) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
      modalInstance = {
        close: jasmine.createSpy('modalInstance.close'),
        dismiss: jasmine.createSpy('modalInstance.dismiss'),
        result: {
          then: jasmine.createSpy('modalInstance.result.then')
        }
      };
    }));

    describe('modal.controller', function () {

      describe('ModalInfoCtrl', function () {

        var createModalInfoCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createModalInfoCtrl = function () {
            return $controller('ModalInfoCtrl', {$modalInstance: modalInstance, $scope: scope, info: infoMock});
          };
        }));

        it("scope.info should equal info (infoMock)", function () {
          createModalInfoCtrl();
          expect(scope.info).toBe(infoMock);
        });

        it("modalInstance dismiss should be caled with mesage 'ok'", function () {
          createModalInfoCtrl();
          scope.dismiss();
          expect(modalInstance.dismiss).toHaveBeenCalledWith('ok');
        });
      });

      describe('ModalErrorCtrl', function () {

        var createModalErrorCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createModalErrorCtrl = function () {
            return $controller('ModalErrorCtrl', {$modalInstance: modalInstance, $scope: scope, error: errorMock});
          };
        }));

        it("scope.error should equal error (errorMock)", function () {
          createModalErrorCtrl();
          expect(scope.error).toBe(errorMock);
        });

        it("modalInstance dismiss should be caled with mesage 'close'", function () {
          createModalErrorCtrl();
          scope.dismiss();
          expect(modalInstance.dismiss).toHaveBeenCalledWith('close');
        });

        it("should return true if error is rpcError", function () {
          createModalErrorCtrl();
          expect(scope.isRpcError()).toBeFalsy();

          errorMock = {rpcError: "test error"};
          createModalErrorCtrl();
          expect(scope.isRpcError()).toBeTruthy();
        });
      });

      describe('ModalDeleteCtrl', function () {

        var createModalDeleteCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createModalDeleteCtrl = function () {
            return $controller('ModalDeleteCtrl', {$modalInstance: modalInstance, $scope: scope, name: deleteMock});
          };
        }));

        it("scope.name should equal name (deleteMock)", function () {
          createModalDeleteCtrl();
          expect(scope.name).toBe(deleteMock);
        });

        it("modalInstance close should be caled with mesage 'delete'", function () {
          createModalDeleteCtrl();
          scope.delete();
          expect(modalInstance.close).toHaveBeenCalledWith('delete');
        });

        it("modalInstance dismiss should be caled with mesage 'cancel'", function () {
          createModalDeleteCtrl();
          scope.dismiss();
          expect(modalInstance.dismiss).toHaveBeenCalledWith('cancel');
        });
      });
    });
  });
});
