define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {

    describe('config.controller', function () {

      var $scope, configCtrl, SfcConfigSvcMock, SfcRestangularSvcMock, SfcConfigExportSvcMock, SfcFileReaderSvcMock;

      beforeEach(angular.mock.module('app.sfc'));

      beforeEach(angular.mock.inject(function ($q) {
        // four service mocks
        SfcConfigSvcMock = {
          getValidationRevision: function () {
            return "rrr123";
          },
          runConfig: function (content, validateBefore) {
            var deferred = $q.defer();

            if (content == "testSuccess") {
              deferred.resolve("success");
            } else {
              deferred.reject("error");
            }

            return deferred.promise;
          }
        };

        // !!!  -- in closure
        SfcRestangularSvcMock = {
          getCurrentBaseUrl: function () {
            return "url123";
          },
          changeBaseUrl: function (newUrl) {
          }
        };

        SfcConfigExportSvcMock = {
          exportConfig: function (callback) {
            callback({"prop1": true});
            callback({"prop2": true});
          }
        };

        SfcFileReaderSvcMock = {

          readAsText: function (file, scope) {
            var deferred = $q.defer();

            scope.$apply(function () {
              deferred.resolve("test content");
            });

            return deferred.promise;
          }
        };
      }));

      beforeEach(angular.mock.inject(function ($controller, $rootScope) {
        $scope = $rootScope.$new();

        // create controller
        configCtrl = (function () {
          return $controller(
            'configCtrl',
            {
              $scope: $scope,
              SfcFileReaderSvc: SfcFileReaderSvcMock,
              SfcConfigSvc: SfcConfigSvcMock,
              SfcRestangularSvc: SfcRestangularSvcMock,
              SfcConfigExportSvc: SfcConfigExportSvcMock
            });
        })();

        expect(configCtrl).not.toBeUndefined();
      }));

      it('Content of file should get to $scope.fileContent', function () {

        var fileMock = {};

        $scope.getOnFileSelect(fileMock);

        $scope.$digest();

        expect($scope.fileContent).toEqual("test content");
      });

      it('applyConfig should alert on empty content', function () {

        var contentMock = "";

        $scope.fileContent = contentMock;

        spyOn(window, 'alert').andReturn(true);

        $scope.applyConfig();

        expect(window.alert).toHaveBeenCalledWith('file is empty.');
      });

      it('applyConfig should alert on config success', function () {

        var contentMock = "testSuccess";

        $scope.fileContent = contentMock;

        spyOn(window, 'alert').andReturn(true);

        $scope.applyConfig();

        $scope.$digest();

        expect(window.alert).toHaveBeenCalledWith('config success: success');
      });

      it('applyConfig should alert on config error', function () {

        var contentMock = "!testSuccess";

        $scope.fileContent = contentMock;

        spyOn(window, 'alert').andReturn(true);

        $scope.applyConfig();

        $scope.$digest();

        expect(window.alert).toHaveBeenCalledWith('config error: error');
      });

      it('exportConfig should concat JSONs of data', function () {


        $scope.exportConfig();

        var expectedStr = '{\n  "prop1": true\n};\n{\n  "prop2": true\n};\n';

        expect($scope.fileContent).toEqual(expectedStr);
      });

      it('applyBaseUrl should call changeBaseUrl on SfcRestangularSvc', function () {

        spyOn(SfcRestangularSvcMock, 'changeBaseUrl').andReturn(true);
        spyOn(window, 'alert').andReturn(true);  // ignore alert call

        $scope.restangularBaseUrl = "new-url";

        $scope.applyBaseUrl();

        expect(SfcRestangularSvcMock.changeBaseUrl).toHaveBeenCalledWith("new-url");
      });

    });
  });

});