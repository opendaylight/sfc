define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC Config service', function () {

    var $provide, $httpBackend;

    var SfcConfigSvc;

    var SfcJsonValidationSvc, ServiceFunctionSvc, ServiceNodeSvc, ServiceChainSvc, sfcFormatMessage;

    beforeEach(angular.mock.module('app.sfc'));

    /*    // dependency services mocks
     beforeEach(angular.mock.module(function (_$provide_) {
     $provide = _$provide_;

     // SfcJsonValidationSvc mock
     SfcJsonValidationSvcMock = {
     sfcValidate: function () {
     return {"errors": {"length": 0}}; // result as if validated
     }
     };

     // replace with mock
     $provide.value('SfcJsonValidationSvc', SfcJsonValidationSvcMock);
     }));*/

    // tested service
    beforeEach(angular.mock.inject(function (_SfcConfigSvc_, JsvLoaderSvc, $rootScope) {
      SfcConfigSvc = _SfcConfigSvc_;

      waitsFor(function () {  // wait for loading JSV libraries
        $rootScope.$digest();
        return  JsvLoaderSvc.isLoaded();
      });
    }));

    beforeEach(angular.mock.inject(function (_$httpBackend_) {
      $httpBackend = _$httpBackend_;
    }));

    // secure base restconf url
    beforeEach(angular.mock.inject(function (_SfcRestangularSvc_) {
      SfcRestangularSvc = _SfcRestangularSvc_;
      SfcRestangularSvc.changeBaseUrl("http://localhost:8080/restconf");
    }));

    afterEach(function () {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });


    it('Should send REST-PUTs to SF, SN and SFC urls', function () {

      var sfDataTest = {
        "service-functions": {
          "service-function": [
            {
              "sf-data-plane-locator": {
                "service-function-forwarder": "SFF-bootstrap",
                "port": 10003,
                "ip": "10.3.1.102"
              },
              "ip-mgmt-address": "10.3.1.102",
              "name": "dpi-102-3",
              "type": "dpi"
            }
          ]
        }
      };

      var snDataTest = {
        "service-nodes": {
          "service-node": [
            {
              "ip-mgmt-address": "10.3.1.101",
              "service-function": [
                "firewall-101-1",
                "firewall-101-2"
              ],
              "name": "node-101",
              "service-function-forwarder": [
                "SFF-bootstrap"
              ]
            }
          ]
        }
      };

      var sfcDataTest = {
        "service-function-chains": {
          "service-function-chain": [
            {
              "sfc-service-function": [
                {
                  "name": "napt44-abstract2",
                  "type": "napt44"
                },
                {
                  "name": "firewall-abstract2",
                  "type": "firewall"
                }
              ],
              "name": "SFC2"
            }
          ]
        }
      };

      // compose input string - 'file content'
      var sfContentTest = angular.toJson(sfDataTest, true) + ";\r\n" + angular.toJson(snDataTest, true) + ";\r\n" + angular.toJson(sfcDataTest, true) + ";";

      var validateBefore = false;

      $httpBackend.expectPUT("http://localhost:8080/restconf/config/service-function:service-functions", sfDataTest).respond(200, {});
      $httpBackend.expectPUT("http://localhost:8080/restconf/config/service-node:service-nodes", snDataTest).respond(200, {});
      $httpBackend.expectPUT("http://localhost:8080/restconf/config/service-function-chain:service-function-chains", sfcDataTest).respond(200, {});

      SfcConfigSvc.runConfig(sfContentTest, validateBefore);

      $httpBackend.flush();
    });


  });

});