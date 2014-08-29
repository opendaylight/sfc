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

    describe('servicechain.services', function () {

      describe('ModalSfpInstantiateSvc', function () {

        var ModalSfpInstantiateSvc, callbackResult;

        beforeEach(angular.mock.inject(function (_ModalSfpInstantiateSvc_) {
          ModalSfpInstantiateSvc = _ModalSfpInstantiateSvc_;
        }));

        it("should open modal dialog and GET its template", function () {
          httpBackend.expectGET('src/app/sfc/servicechain/servicechain.modal.instantiate.tpl.html').respond('');
          spyOn(ModalSfpInstantiateSvc, 'open').andCallThrough();
          ModalSfpInstantiateSvc.open("testSfc", function(){});
          rootScope.$digest();
          expect(ModalSfpInstantiateSvc.open).toHaveBeenCalledWith("testSfc", jasmine.any(Function));
        });

        it("should open modal dialog, GET its template and pass 'cancel' to callback function", function () {
          httpBackend.whenGET('src/app/sfc/servicechain/servicechain.modal.instantiate.tpl.html').respond('<div>modal test</div>');
          spyOn(ModalSfpInstantiateSvc, 'open').andCallThrough();
          var modalIns = ModalSfpInstantiateSvc.open("testSfc", function(result){callbackResult = result;});
          rootScope.$digest();
          httpBackend.flush();
          modalIns.dismiss('cancel');
          //digest to resolve promise
          rootScope.$digest();
          expect(ModalSfpInstantiateSvc.open).toHaveBeenCalledWith("testSfc", jasmine.any(Function));
          expect(callbackResult).toBe('cancel');
        });

        it("should open modal dialog, GET its template and pass 'testSfc-path' to callback function", function () {
          httpBackend.whenGET('src/app/sfc/servicechain/servicechain.modal.instantiate.tpl.html').respond('<div>modal test</div>');
          spyOn(ModalSfpInstantiateSvc, 'open').andCallThrough();
          var modalIns = ModalSfpInstantiateSvc.open("testSfc", function(result){callbackResult = result;});
          rootScope.$digest();
          httpBackend.flush();
          modalIns.close('testSfc-path');
          //digest to resolve promise
          rootScope.$digest();
          expect(ModalSfpInstantiateSvc.open).toHaveBeenCalledWith("testSfc", jasmine.any(Function));
          expect(callbackResult).toBe('testSfc-path');
        });
      });

      describe('ModalSfNameSvc', function () {

        var ModalSfNameSvc, callbackResult;

        beforeEach(angular.mock.inject(function (_ModalSfNameSvc_) {
          ModalSfNameSvc = _ModalSfNameSvc_;
        }));

        it("should open modal dialog and GET its template", function () {
          httpBackend.expectGET('src/app/sfc/servicechain/servicechain.modal.sfname.tpl.html').respond('');
          spyOn(ModalSfNameSvc, 'open').andCallThrough();
          ModalSfNameSvc.open("testSfc", "testSf", function(){});
          rootScope.$digest();
          expect(ModalSfNameSvc.open).toHaveBeenCalledWith("testSfc", "testSf", jasmine.any(Function));
        });

        it("should open modal dialog, GET its template and pass 'cancel' to callback function", function () {
          httpBackend.whenGET('src/app/sfc/servicechain/servicechain.modal.sfname.tpl.html').respond('<div>modal test</div>');
          spyOn(ModalSfNameSvc, 'open').andCallThrough();
          var modalIns = ModalSfNameSvc.open("testSfc", "testSf", function(result){callbackResult = result;});
          rootScope.$digest();
          httpBackend.flush();
          modalIns.dismiss('cancel');
          //digest to resolve promise
          rootScope.$digest();
          expect(ModalSfNameSvc.open).toHaveBeenCalledWith("testSfc", "testSf", jasmine.any(Function));
          expect(callbackResult).toBe('cancel');
        });

        it("should open modal dialog, GET its template and pass 'egress-testSf' to callback function", function () {
          httpBackend.whenGET('src/app/sfc/servicechain/servicechain.modal.sfname.tpl.html').respond('<div>modal test</div>');
          spyOn(ModalSfNameSvc, 'open').andCallThrough();
          var modalIns = ModalSfNameSvc.open("testSfc", "testSf", function(result){callbackResult = result;});
          rootScope.$digest();
          httpBackend.flush();
          modalIns.close('egress-testSf');
          //digest to resolve promise
          rootScope.$digest();
          expect(ModalSfNameSvc.open).toHaveBeenCalledWith("testSfc", "testSf", jasmine.any(Function));
          expect(callbackResult).toBe('egress-testSf');
        });
      });

    });
  });
});
