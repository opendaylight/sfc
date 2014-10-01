define(['angularAMD', 'app/sfc/sfc.test.module.loader'], function () {

  ddescribe("SFC app", function () {
    describe("sfc.services", function () {

      var httpBackend, SfcRestangularSvc;

      beforeEach(angular.mock.module('app.sfc'));

      beforeEach(angular.mock.inject(function (SfcRestangularSvc, $httpBackend) {
        httpBackend = $httpBackend;
      }));

      beforeEach(angular.mock.inject(function (_SfcRestangularSvc_) {
        SfcRestangularSvc = _SfcRestangularSvc_;
        SfcRestangularSvc.changeBaseUrl("http://localhost:8080/restconf");
      }));

      afterEach(function () {
        httpBackend.verifyNoOutstandingExpectation();
        httpBackend.verifyNoOutstandingRequest();
      });

      it("Test SfcRestangularSvc currentBaseUrl", function () {
        expect(SfcRestangularSvc.getCurrentBaseUrl()).toEqual("http://localhost:8080/restconf");
      });

      describe("Test SfcRestBaseSvc", function () {

        var testRestBaseSvc;

        beforeEach(angular.mock.inject(function (SfcRestBaseSvc) {
          var modelUrl = 'url';
          var containerName = 'container';
          var listName = 'listname';

          testRestBaseSvc = new SfcRestBaseSvc(modelUrl, containerName, listName);
        }));

        describe("Test SfcRestBaseSvc calls Restangular with correct url", function () {

          it("baseRest - should call Restangular.one('config')", angular.mock.inject(function () {
            sfcRestangular = SfcRestangularSvc.getCurrentInstance();
            spyOn(sfcRestangular, 'one').andCallThrough();
            testRestBaseSvc.baseRest();
            expect(sfcRestangular.one).toHaveBeenCalledWith('config');
          }));

          it("baseRpcRest - should call Restangular.one('operations')", angular.mock.inject(function () {
            sfcRestangular = SfcRestangularSvc.getCurrentInstance();
            spyOn(sfcRestangular, 'one').andCallThrough();
            testRestBaseSvc.baseRpcRest();
            expect(sfcRestangular.one).toHaveBeenCalledWith('operations');
          }));
        });

        it("postRpc - should send correct POST", angular.mock.inject(function () {

          var testObject = {"input": {"test": 0}};
          var testParams = {"x": "y"};
          var expectHeaders = {
            "Content-Type": "application/yang.data+json",
            "Accept": "application/yang.data+json"
          };

          httpBackend.expectPOST('http://localhost:8080/restconf/operations/url:testrpc?x=y', testObject, expectHeaders).respond({});

          testRestBaseSvc.postRpc(testObject, "testrpc", testParams);

          httpBackend.flush();
        }));

        it("executeRpcOperation - should call postRPC and should receive RPC output as arg to callback", function () {

          var testObject = {"input": {"test": 0}};
          var testParams = {"x": "y"};
          var expectHeaders = {
            "Content-Type": "application/yang.data+json",
            "Accept": "application/yang.data+json"
          };
          var mockToRespond = {
            "output": {
              "key": "value"
            }
          };

          httpBackend.expectPOST('http://localhost:8080/restconf/operations/url:testrpc?x=y', testObject, expectHeaders).respond(mockToRespond);

          testRestBaseSvc.executeRpcOperation(testObject, "testrpc", testParams, function (arg) {
            expect(arg).toEqual({ "key": "value"});
          });

          httpBackend.flush();
        });


        it("executeRpcOperation - should receive RPCError as arg to callback", angular.mock.inject(function (SfcRestconfError) {

          var testObject = {"input": {"test": 0}};
          var testParams = {"x": "y"};
          var expectHeaders = {
            "Content-Type": "application/yang.data+json",
            "Accept": "application/yang.data+json"
          };
          var mockToRespond = {
            "output": {
              "key": "value"
            }
          };

          httpBackend.expectPOST('http://localhost:8080/restconf/operations/url:testrpc?x=y', testObject, expectHeaders).respond(404, mockToRespond);

          testRestBaseSvc.executeRpcOperation(testObject, "testrpc", testParams, function (arg) {
            expect(arg instanceof SfcRestconfError).toBeTruthy();
            expect(arg.response.data).toEqual(mockToRespond);
          });

          httpBackend.flush();
        }));

        it("getArray - should receive nested array from container on REST OK", angular.mock.inject(function () {
          var mockToRespond = {
            "container": {
              "listname": [
                {
                  "test": "test"
                }
              ]
            }
          };

          httpBackend.expectGET('http://localhost:8080/restconf/config/url:container').respond(mockToRespond);

          testRestBaseSvc.getArray(function (sfArrayData) {
            expect(sfArrayData).toEqual(mockToRespond["container"]["listname"]);
          });

          httpBackend.flush();
        }));

        it("getArray - should receive empty array on REST ERROR", angular.mock.inject(function () {
          var mockToRespond = {};

          httpBackend.expectGET('http://localhost:8080/restconf/config/url:container').respond(404, mockToRespond); //status 404 - not found

          testRestBaseSvc.getArray(function (data) {
            expect(data).toEqual([]);
          });

          httpBackend.flush();
        }));

        // **************
        it("put - should send PUT", angular.mock.inject(function () {
          var mockToSend = {"test": "test"};

          httpBackend.expectPUT('http://localhost:8080/restconf/config/url:container/listname/key', mockToSend).respond({});

          var promise = testRestBaseSvc.put(mockToSend, "key");

          expect(promise.then).toBeDefined();

          httpBackend.flush();
        }));


        // **************
        it("putContainer - should send PUT", angular.mock.inject(function () {
          var containerDataMock = {"test": "test"};

          httpBackend.expectPUT('http://localhost:8080/restconf/config/url:container', containerDataMock).respond({});

          var promise = testRestBaseSvc.putContainer(containerDataMock);

          expect(promise.then).toBeDefined();

          httpBackend.flush();
        }));


        it("getOne - should send GET and receive data", angular.mock.inject(function () {
          var responseDataMock = {"test": "test"};

          httpBackend.expectGET('http://localhost:8080/restconf/config/url:container/listname/key').respond(responseDataMock);

          var promise = testRestBaseSvc.getOne("key").then(function (responseData) {
            expect(responseData['test']).toEqual('test');
          });

          httpBackend.flush();
        }));

        // **************
        it("_delete - should send DELETE", angular.mock.inject(function () {
          httpBackend.expectDELETE('http://localhost:8080/restconf/config/url:container/listname/key').respond({});

          var promise = testRestBaseSvc._delete("key");

          expect(promise.then).toBeDefined();

          httpBackend.flush();
        }));

        // **************
        it("getItem - should call getOne", angular.mock.inject(function () {
          var responseDataMock = {"listname": [
            {"test": "test"}
          ]};

          spyOn(testRestBaseSvc, 'getOne').andCallThrough();
          httpBackend.expectGET('http://localhost:8080/restconf/config/url:container/listname/key').respond(responseDataMock);

          testRestBaseSvc.getItem('key', function (data) {
            expect(data).toEqual({"test": "test"});
          });

          httpBackend.flush();

          expect(testRestBaseSvc.getOne).toHaveBeenCalledWith('key');
        }));

        it("getItem - on no data should return empty object", angular.mock.inject(function () {
          var responseDataMock = {"testkey": "testvalue"};

          spyOn(testRestBaseSvc, 'getOne').andCallThrough();
          httpBackend.expectGET('http://localhost:8080/restconf/config/url:container/listname/key').respond(404, responseDataMock);

          testRestBaseSvc.getItem('key', function (data) {
            expect(data).toEqual({});
          });

          httpBackend.flush();
        }));

        // **************
        it("putItem - should call put() with proper key", angular.mock.inject(function () {
          var itemMock = {
            "name": "testname"  // "name" is the key in yang model list
          };

          var expectedWrappedInListname = testRestBaseSvc.wrapInListname(itemMock);

          spyOn(testRestBaseSvc, 'put').andCallThrough();
          httpBackend.expectPUT('http://localhost:8080/restconf/config/url:container/listname/testname', expectedWrappedInListname).respond({});

          testRestBaseSvc.putItem(itemMock, function () {
            // noop
          });

          httpBackend.flush();

          expect(testRestBaseSvc.put).toHaveBeenCalledWith(expectedWrappedInListname, 'testname');
        }));


        it("exportContainer - should return object with only 'containerName' property from Restangular object", angular.mock.inject(function () {
          var expectContainerObj = {
            "container": "test"
          };

          var restResponse = {
            "otherKey": "otherValue",
            "container": "test"
          };

          httpBackend.expectGET('http://localhost:8080/restconf/config/url:container').respond(restResponse);

          testRestBaseSvc.exportContainer(function (data) {
            expect(data).toEqual(expectContainerObj);

          });

          httpBackend.flush();
        }));

        it("putContainerWrapper - should send PUT to container URL", angular.mock.inject(function () {
          var containerDataMock = {
            "container": "data"
          };

          httpBackend.expectPUT('http://localhost:8080/restconf/config/url:container', containerDataMock).respond({});

          testRestBaseSvc.putContainerWrapper(containerDataMock, function () {
            // noop
          });

          httpBackend.flush();
        }));

        it("deleteItem  - should call checkRequired and call async callback after response", angular.mock.inject(function () {
          var itemDataMock = {
            "name": "testname"
          };

          spyOn(testRestBaseSvc, 'checkRequired').andCallThrough();

          httpBackend.expectDELETE('http://localhost:8080/restconf/config/url:container/listname/testname', undefined).respond({});

          testRestBaseSvc.deleteItem(itemDataMock, function (arg) {
            expect(arg).toBeUndefined();
          });

          httpBackend.flush();

          expect(testRestBaseSvc.checkRequired).toHaveBeenCalledWith(itemDataMock);
        }));

        it("checkRequired   - should trow if name property is undefined or empty", angular.mock.inject(function () {
          var itemDataMock = {
            "name": ""
          };
          expect(function () {
            testRestBaseSvc.checkRequired(itemDataMock);
          }).toThrow("list key is undefined or empty");
        }));

      });

      describe("Test ServiceFunctionSvc", function () {

        var ServiceFunctionSvc;

        beforeEach(angular.mock.inject(function (_ServiceFunctionSvc_) {
          ServiceFunctionSvc = _ServiceFunctionSvc_;
        }));

        it("getArray - should return nested array from container with namespace stripped for type", angular.mock.inject(function () {
          var mockToRespond = {
            "service-functions": {
              "service-function": [
                {
                  "type": "service-function:test"
                }
              ]
            }
          };

          httpBackend.expectGET('http://localhost:8080/restconf/config/service-function:service-functions').respond(mockToRespond);

          ServiceFunctionSvc.getArray(function (sfArrayData) {
            mockToRespond["service-functions"]["service-function"][0]["type"] = "test"; // adjust for check  - strip namespace

            expect(sfArrayData).toEqual(mockToRespond["service-functions"]["service-function"]);
          });

          httpBackend.flush();
        }));

      });


      describe("Test ServiceChainSvc", function () {

        var ServiceChainSvc;

        beforeEach(angular.mock.inject(function (_ServiceChainSvc_) {
          ServiceChainSvc = _ServiceChainSvc_;
        }));

        it("check correrct ServiceChainSvc instantiation", angular.mock.inject(function () {

          var modelUrl = 'service-function-chain';
          var containerName = 'service-function-chains';
          var listName = 'service-function-chain';

          expect(ServiceChainSvc.modelUrl).toEqual(modelUrl);
          expect(ServiceChainSvc.containerName).toEqual(containerName);
          expect(ServiceChainSvc.listName).toEqual(listName);
        }));

        it("check deployChain function", angular.mock.inject(function () {

          var expectedInput = {
            "service-function-chain:input": {
              "name": "chain-1"
            }
          };

          var mockToRespond = {
            "output": {
              "name": "path-1"
            }
          };

          httpBackend.expectPOST('http://localhost:8080/restconf/operations/service-function-chain:instantiate-service-function-chain', expectedInput).respond(mockToRespond);

          ServiceChainSvc.deployChain("chain-1", function (arg) {
            expect(arg.name).toEqual("path-1");
          });

          httpBackend.flush();
        }));


        it("test createInstance function", angular.mock.inject(function () {

          var expected = {
            "service-function": [
              {"name": "sf1"},
              {"name": "sf2"}
            ],
            "name": "sfc-1"
          };

          expect(ServiceChainSvc.createInstance("sfc-1", ["sf1", "sf2"])).toEqual(expected);
        }));

      });


      describe("Test ServicePathSvc", function () {

        var ServicePathSvc;

        beforeEach(angular.mock.inject(function (_ServicePathSvc_) {
          ServicePathSvc = _ServicePathSvc_;
        }));

        it("check correrct ServicePathSvc instantiation", angular.mock.inject(function () {

          var modelUrl = 'service-function-path';
          var containerName = 'service-function-paths';
          var listName = 'service-function-path';

          expect(ServicePathSvc.modelUrl).toEqual(modelUrl);
          expect(ServicePathSvc.containerName).toEqual(containerName);
          expect(ServicePathSvc.listName).toEqual(listName);
        }));

      });


      describe("Test sfcFormatMessage", function () {

        var sfcFormatMessage;

        beforeEach(angular.mock.inject(function (_sfcFormatMessage_) {
          sfcFormatMessage = _sfcFormatMessage_;
        }));

        it("check correrct ServicePathSvc instantiation", angular.mock.inject(function () {

          expect(sfcFormatMessage("{0}", function abc(xyz) {
            console.log(xyz);
          })).toEqual("function abc(xyz)");

          expect(sfcFormatMessage("{0}", undefined)).toEqual("undefined");

          expect(sfcFormatMessage("{0}", {"abc": "xyz"})).toEqual("{\n  \"abc\": \"xyz\"\n}");

          expect(sfcFormatMessage("{0}", "abc")).toEqual("abc");
        }));

      });


      describe("Test ServiceNodeSvc", function () {

        var ServiceNodeSvc;

        beforeEach(angular.mock.inject(function (_ServiceNodeSvc_) {
          ServiceNodeSvc = _ServiceNodeSvc_;
        }));

        it("check correrct ServiceNodeSvc instantiation", angular.mock.inject(function () {

          var modelUrl = 'service-node';
          var containerName = 'service-nodes';
          var listName = 'service-node';

          expect(ServiceNodeSvc.modelUrl).toEqual(modelUrl);
          expect(ServiceNodeSvc.containerName).toEqual(containerName);
          expect(ServiceNodeSvc.listName).toEqual(listName);
        }));


        it("getArray - should return nested array from container with namespace stripped for type", angular.mock.inject(function () {
          var mockToRespond = {
            "service-nodes": {
              "service-node": [
                {
                  "type": "service-node:test",
                  "failmode": "service-node:test",
                  "transport": "service-node:test"
                }
              ]
            }
          };

          httpBackend.expectGET('http://localhost:8080/restconf/config/service-node:service-nodes').respond(mockToRespond);

          ServiceNodeSvc.getArray(function (snList) {
            expect(snList[0]["type"]).toEqual("test");
            expect(snList[0]["failmode"]).toEqual("test");
            expect(snList[0]["transport"]).toEqual("test");
          });

          httpBackend.flush();
        }));

      });

      describe("Test ServiceForwarderSvc", function () {

        var ServiceForwarderSvc;

        beforeEach(angular.mock.inject(function (_ServiceForwarderSvc_) {
          ServiceForwarderSvc = _ServiceForwarderSvc_;
        }));

        it("check correrct ServiceForwarderSvc instantiation", angular.mock.inject(function () {

          var modelUrl = 'service-function-forwarder';
          var containerName = 'service-function-forwarders';
          var listName = 'service-function-forwarder';

          expect(ServiceForwarderSvc.modelUrl).toEqual(modelUrl);
          expect(ServiceForwarderSvc.containerName).toEqual(containerName);
          expect(ServiceForwarderSvc.listName).toEqual(listName);
        }));

      });

    });
  });
});
