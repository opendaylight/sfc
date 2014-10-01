define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC module parts in config.services.js', function () {

    beforeEach(angular.mock.module('app.sfc'));

    describe('SFC Config service', function () {

      var $provide, $httpBackend;

      var SfcConfigSvc;

      var SfcJsonValidationSvc;

      var ServiceFunctionSvc, ServiceNodeSvc, ServiceChainSvc;

      // prepare dependency services mocks
      beforeEach(angular.mock.module(function (_$provide_) {
        $provide = _$provide_;

        // SfcJsonValidationSvc mock
        SfcJsonValidationSvcMock = {
          mockResultAsInvalid: false, // initial default

          sfcValidate: function () {
            if (this.mockResultAsInvalid) {
              throw new Error("JSON validation error");
            }

            // result as if validated (!not throw Error)
          }
        };

        // replace with mock
        $provide.value('SfcJsonValidationSvc', SfcJsonValidationSvcMock);
      }));

      // tested service
      beforeEach(angular.mock.inject(function (_SfcConfigSvc_, _SfcJsonValidationSvc_) {
        SfcConfigSvc = _SfcConfigSvc_;
        SfcJsonValidationSvc = _SfcJsonValidationSvc_;
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

      var sffDataTest = {
        "service-function-forwarders": {
          "service-function-forwarder": [
            {
              "name": "SFF-bootstrap",
              "service-function-forwarder-ovs:ovs": {
                "bridge-name": "br-int",
                "rest-uri": "http://www.example.com/sffs/sff-bootstrap",
                "uuid": "4c3778e4-840d-47f4-b45e-0988e514d26c"
              },
              "service-function-dictionary": [
                {
                  "name": "SF1",
                  "sff-sf-data-plane-locator": {
                    "port": 5000,
                    "ip": "10.1.1.1"
                  },
                  "type": "dp1"
                },
                {
                  "name": "SF2",
                  "sff-sf-data-plane-locator": {
                    "port": 5000,
                    "ip": "10.1.1.2"
                  },
                  "type": "firewall"
                }
              ],
              "classifier": "acl-sfp-1",
              "service-node": "OVSDB1",
              "sff-data-plane-locator": [
                {
                  "name": "eth0",
                  "data-plane-locator": {
                    "transport": "service-locator:vxlan-gpe",
                    "port": 5000,
                    "ip": "192.168.1.1"
                  }
                }
              ]
            }
          ]
        }
      };


      it('Should send REST-PUTs to SF, SN, SFF and SFC urls', function () {
        // compose input string - 'file content'
        var sfContentTest = angular.toJson(sfDataTest, true) + ";\r\n" + angular.toJson(snDataTest, true) + ";\r\n" + angular.toJson(sfcDataTest, true) + ";\r\n" + angular.toJson(sffDataTest, true) + ";";

        var validateBefore = false;

        $httpBackend.expectPUT("http://localhost:8080/restconf/config/service-function:service-functions", sfDataTest).respond(200, {});
        $httpBackend.expectPUT("http://localhost:8080/restconf/config/service-node:service-nodes", snDataTest).respond(200, {});
        $httpBackend.expectPUT("http://localhost:8080/restconf/config/service-function-chain:service-function-chains", sfcDataTest).respond(200, {});
        $httpBackend.expectPUT("http://localhost:8080/restconf/config/service-function-forwarder:service-function-forwarders", sffDataTest).respond(200, {});

        SfcConfigSvc.runConfig(sfContentTest, validateBefore);

        $httpBackend.flush();
      });


      it('sf  - Should receive validation error message in promise onError', function () {
        var validateBefore = true;

        SfcJsonValidationSvc.mockResultAsInvalid = true;

        // test out for diffrent sfc model 'types'
        _.each([sfDataTest, snDataTest, sfcDataTest], function (contentTest) {

          SfcConfigSvc.runConfig(angular.toJson(contentTest, true), validateBefore).then(
            function onSucces() {
              expect(0).toEqual(1); // should not execute this branch
            },
            function onError(data) {
              expect(data).toEqual("JSON validation error");
            }
          );

        });
      });

    });


    describe("SFC Config Export service", function () {

      var SfcConfigExportSvc;

      // dependency services mocks
      beforeEach(angular.mock.module(function (_$provide_) {
        $provide = _$provide_;

        function CommonServiceMockMock() {
          this.exportContainer = function (callback) {
            callback({"test": "test"});
          };
        }

        // replace dependent services with mocks (ServiceFunctionSvc, ServiceForwarderSvc, ServiceNodeSvc, ServiceChainSvc)
        $provide.value('ServiceFunctionSvc', new CommonServiceMockMock()); // new instance for each mock. b/c of spies
        $provide.value('ServiceForwarderSvc', new CommonServiceMockMock());
        $provide.value('ServiceNodeSvc', new CommonServiceMockMock());
        $provide.value('ServiceChainSvc', new CommonServiceMockMock());
      }));


      // tested service
      beforeEach(angular.mock.inject(function (_SfcConfigExportSvc_) {
        SfcConfigExportSvc = _SfcConfigExportSvc_;
      }));

      it("after exportConfig call, callback should be called 4 times and receive particular container model data",
        angular.mock.inject(function (ServiceFunctionSvc, ServiceForwarderSvc, ServiceNodeSvc, ServiceChainSvc) {

          var count = 0;
          var mockFunction = function (callback) {
            callback({"test": "test"});
          };

          spyOn(ServiceFunctionSvc, 'exportContainer').andCallFake(mockFunction);
          spyOn(ServiceForwarderSvc, 'exportContainer').andCallFake(mockFunction);
          spyOn(ServiceNodeSvc, 'exportContainer').andCallFake(mockFunction);
          spyOn(ServiceChainSvc, 'exportContainer').andCallFake(mockFunction);

          var callback = function (data) {
            count++;
            expect(data).toEqual({"test": "test"});
          };

          SfcConfigExportSvc.exportConfig(callback);

          expect(ServiceFunctionSvc.exportContainer).toHaveBeenCalled();
          expect(ServiceForwarderSvc.exportContainer).toHaveBeenCalled();
          expect(ServiceNodeSvc.exportContainer).toHaveBeenCalled();
          expect(ServiceChainSvc.exportContainer).toHaveBeenCalled();

          expect(count).toEqual(4);
        }));


    });

    describe("SFC Config SfcFileReaderSvc service", function () {

      var SfcFileReaderSvc;

      // tested service
      beforeEach(angular.mock.inject(function (_SfcFileReaderSvc_) {
        SfcFileReaderSvc = _SfcFileReaderSvc_;
      }));

      it("shoul receive filecontent in callback",
        angular.mock.inject(function ($rootScope) {

          var blob = new Blob(["test content"], {type: "text/plain;charset=UTF-8"});

          SfcFileReaderSvc.readAsText(blob ,$rootScope).then(function(content){
            expect(content).toEqual("test content");
          });

          $rootScope.$digest();
        }));


    });

  });

});