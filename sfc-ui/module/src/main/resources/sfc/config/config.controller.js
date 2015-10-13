define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('configCtrl', function ($scope, SfcConfigSvc, SfcFileReaderSvc, SfcRestangularSvc, SfcConfigExportSvc, SfcRestconfError, $sessionStorage) {


    $scope.validationRevision = SfcConfigSvc.getValidationRevision();
    $scope.validateBefore = false;
    $scope.fileContent = "";

    $scope.$storage = $sessionStorage;

    $scope.getOnFileSelect = function (file) {
      SfcFileReaderSvc.readAsText(file, $scope)
        .then(function (fileContent) {
          $scope.fileContent = fileContent;
        });
    };

    $scope.applyConfig = function () {
      if (_.isEmpty($scope.fileContent)) {
        alert('file is empty.');
        return;
      }

      SfcConfigSvc.runConfig($scope.fileContent, $scope.validateBefore).then(
        function onSucces(msg) {
          alert('config success: ' + msg);
        },
        function onError(msg) {
          alert('config error: ' + msg);
        });
    };

    $scope.exportConfig = function () {
      $scope.fileContent = "";
      try {
        SfcConfigExportSvc.exportConfig(function (dataObj) {
          if (dataObj instanceof SfcRestconfError) {
            console.error(dataObj.response);
            alert("Error while receiving data for " + dataObj.modelUrl);
          } else {
            $scope.fileContent = $scope.fileContent + angular.toJson(dataObj, true) + ";\n";
          }
        });
      } catch (e) {
        alert(e.message);
      }
    };

    $scope.applyBaseUrl = function () {
      try {
        SfcRestangularSvc.changeBaseUrl($scope.$storage.restangularBaseUrl);
        alert('url set to: ' + $scope.$storage.restangularBaseUrl);
      } catch (e) {
        alert(e.message);
      }
    };

  });

});