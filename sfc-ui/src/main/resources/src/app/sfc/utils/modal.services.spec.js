define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, httpBackend;

    beforeEach(angular.mock.module('pascalprecht.translate'));
    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($rootScope, $httpBackend) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
      httpBackend = $httpBackend;
    }));

    describe('modal.services', function () {

      describe('ModalInfoSvc', function () {

        var ModalInfoSvc;

        beforeEach(angular.mock.inject(function (_ModalInfoSvc_) {
          ModalInfoSvc = _ModalInfoSvc_;
        }));

        it("should open modal dialog and GET its template", function () {
          httpBackend.expectGET('src/app/sfc/utils/modal.info.tpl.html').respond('');
          spyOn(ModalInfoSvc, 'open').andCallThrough();
          ModalInfoSvc.open("test");
          rootScope.$digest();
          expect(ModalInfoSvc.open).toHaveBeenCalledWith("test");
        });
      });

      describe('ModalErrorSvc', function () {

        var ModalErrorSvc;

        beforeEach(angular.mock.inject(function (_ModalErrorSvc_) {
          ModalErrorSvc = _ModalErrorSvc_;
        }));

        it("should open modal dialog and GET its template", function () {
          httpBackend.expectGET('src/app/sfc/utils/modal.error.tpl.html').respond('');
          spyOn(ModalErrorSvc, 'open').andCallThrough();
          ModalErrorSvc.open("test");
          rootScope.$digest();
          expect(ModalErrorSvc.open).toHaveBeenCalledWith("test");
        });
      });

      describe('ModalDeleteSvc', function () {

        var ModalDeleteSvc, callbackResult;

        beforeEach(angular.mock.inject(function (_ModalDeleteSvc_) {
          ModalDeleteSvc = _ModalDeleteSvc_;
        }));

        it("should open modal dialog and GET its template", function () {
          httpBackend.expectGET('src/app/sfc/utils/modal.delete.tpl.html').respond('');
          spyOn(ModalDeleteSvc, 'open').andCallThrough();
          ModalDeleteSvc.open("test", function(){});
          rootScope.$digest();
          expect(ModalDeleteSvc.open).toHaveBeenCalledWith("test", jasmine.any(Function));
        });

        it("should open modal dialog, GET its template and pass 'cancel' to callback function", function () {
          httpBackend.whenGET('src/app/sfc/utils/modal.delete.tpl.html').respond('<div>modal test</div>');
          spyOn(ModalDeleteSvc, 'open').andCallThrough();
          var modalIns = ModalDeleteSvc.open("test", function(result){callbackResult = result;});
          rootScope.$digest();
          httpBackend.flush();
          modalIns.dismiss('cancel');
          //digest to resolve promise
          rootScope.$digest();
          expect(ModalDeleteSvc.open).toHaveBeenCalledWith("test", jasmine.any(Function));
          expect(callbackResult).toBe('cancel');
        });

        it("should open modal dialog, GET its template and pass 'delete' to callback function", function () {
          httpBackend.whenGET('src/app/sfc/utils/modal.delete.tpl.html').respond('<div>modal test</div>');
          spyOn(ModalDeleteSvc, 'open').andCallThrough();
          var modalIns = ModalDeleteSvc.open("test", function(result){callbackResult = result;});
          rootScope.$digest();
          httpBackend.flush();
          modalIns.close('delete');
          //digest to resolve promise
          rootScope.$digest();
          expect(ModalDeleteSvc.open).toHaveBeenCalledWith("test", jasmine.any(Function));
          expect(callbackResult).toBe('delete');
        });
      });

    });
  });
});
