define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, httpBackend, exampleData;

    beforeEach(angular.mock.module('pascalprecht.translate'));
    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($rootScope, $httpBackend) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
      httpBackend = $httpBackend;
    }));

    beforeEach(function () {
      exampleData = {};
      exampleData.sns = [
        {"name": "sn1"},
        {"name": "sn2"},
        {"name": "sn3"}
      ];
      exampleData.data = {
        "sff-data-plane-locator": [
          {
            "data-plane-locator": {}
          }
        ],
        "service-function-dictionary": [
          {
            nonExistent: false,
            "sff-sf-data-plane-locator": {},
            "sff-interfaces": []
          }
        ],
        "connected-sff-dictionary": [
          {
            nonExistent: false,
            "sff-sff-data-plane-locator": {},
            "sff-interfaces": []
          }
        ]
      };
      exampleData.sffInterfacesObjectArray = [
        {"sff-interface": "sfi1"},
        {"sff-interface": "sfi2"},
        {"sff-interface": "sfi3"}
      ];
      exampleData.sffInterfaceStringArray = ["sfi1", "sfi2", "sfi3"];
      exampleData.sffInterfaceString = "sfi1, sfi2, sfi3";
      exampleData.sffInterfacesInSffObjectArray = [
        {"sff-interface": "sfi1"},
        {"sff-interface": "sfi2"},
        {"sff-interface": "sfi3"}
      ];
      exampleData.sffInterfaceInSffStringArray = ["sfi1", "sfi2", "sfi3"];
      exampleData.sffInterfaceInSffString = "sfi1, sfi2, sfi3";
      exampleData.sffDpLocatorObjectArray = [
        {"name": "dp1"},
        {"name": "dp2"},
        {"name": "dp3"}
      ];
      exampleData.sffDpLocatorString = "dp1 dp2 dp3 ";
      exampleData.sffFunctionDictionaryObjectArray = [
        {"name": "sf1"},
        {"name": "sf2"},
        {"name": "sf3"}
      ];
      exampleData.sffForwarderDictionaryObjectArray = [
        {"name": "sff1"},
        {"name": "sff2"},
        {"name": "sff3"}
      ];
      exampleData.sffFunctionDictionaryString = "sf1 sf2 sf3 ";
      exampleData.sffForwarderDictionaryString = "sff1 sff2 sff3 ";
      exampleData.sfModelData = {"name": "sf1", "type": "dpi", "sf-data-plane-locator": [
        {"name": "sfdp1", "ip": "10.0.0.1", "port": "8000", "service-function-forwarder": "sff1"},
        {"name": "sfdp2"}
      ]};
      exampleData.sffModelData = {"name": "sff1", "ip-mgmt-address", "sff-data-plane-locator": [
        {"name": "sfdp1", "ip": "10.0.0.1", "port": "8000", "service-function-forwarder": "sff1"},
        {"name": "sfdp2"}
      ]};
      exampleData.sfs = [
        {"name": "sf1", "type": "dpi", "sf-data-plane-locator": [
          {"name": "sfdp1", "ip": "10.0.0.1", "port": "8000", "service-function-forwarder": "sff1"},
          {"name": "sfdp2"}
        ]}
      ];
      exampleData.sffs = [
        {"name": "sff1", "ip-mgmt-address": "10.0.0.1", "sff-data-plane-locator": [
          {"name": "sfdp1", "ip": "10.0.0.1", "port": "8000", "service-function-forwarder": "sff1"},
          {"name": "sfdp2"}
        ]}
      ];
      exampleData.sffDpLocators = {
        "data": {
          "sff-data-plane-locator": [
            {
              "name": "sffdp1",
              "data-plane-locator": {}
            }
          ]
        }
      };
    });

    describe('serviceforwarder.services', function () {

      describe('ServiceForwarderHelper', function () {

        var ServiceForwarderHelper;

        beforeEach(angular.mock.inject(function (_ServiceForwarderHelper_) {
          ServiceForwarderHelper = _ServiceForwarderHelper_;
        }));

        it("should create sff-data-plane-locator property and add empty locator to SFF", function () {
          exampleData.data = {};
          expect(exampleData.data['sff-data-plane-locator']).toBeUndefined();
          ServiceForwarderHelper.addLocator(exampleData);
          expect(exampleData.data['sff-data-plane-locator'].length).toBe(1);
          expect(exampleData.data['sff-data-plane-locator']).toEqual([
            {"data-plane-locator": {}}
          ]);
        });

        it("should add empty data plane locator to SFF", function () {
          expect(exampleData.data['sff-data-plane-locator'].length).toBe(1);
          ServiceForwarderHelper.addLocator(exampleData);
          expect(exampleData.data['sff-data-plane-locator'].length).toBe(2);
          expect(exampleData.data['sff-data-plane-locator']).toEqual([
            {"data-plane-locator": {}},
            {"data-plane-locator": {}}
          ]);
        });

        it("should remove data plane locator at given index from SFF", function () {
          expect(exampleData.data['sff-data-plane-locator'].length).toBe(1);
          ServiceForwarderHelper.removeLocator(0, exampleData);
          expect(exampleData.data['sff-data-plane-locator'].length).toBe(0);
          expect(exampleData.data['sff-data-plane-locator']).toEqual([]);
        });

        it("should create service-function-dictionary property and add empty SF to SFF", function () {
          exampleData.data = {};
          expect(exampleData.data['service-function-dictionary']).toBeUndefined();
          ServiceForwarderHelper.addFunction(exampleData);
          expect(exampleData.data['service-function-dictionary'].length).toBe(1);
          expect(exampleData.data['service-function-dictionary']).toEqual([
            {
              nonExistent: false,
              "sff-sf-data-plane-locator": {},
              "sff-interfaces": []
            }
          ]);
        });

        it("should add empty SF to service-function-dictionary of SFF", function () {
          expect(exampleData.data['service-function-dictionary'].length).toBe(1);
          ServiceForwarderHelper.addFunction(exampleData);
          expect(exampleData.data['service-function-dictionary'].length).toBe(2);
          expect(exampleData.data['service-function-dictionary']).toEqual([
            {
              nonExistent: false,
              "sff-sf-data-plane-locator": {},
              "sff-interfaces": []
            },
            {
              nonExistent: false,
              "sff-sf-data-plane-locator": {},
              "sff-interfaces": []
            }
          ]);
        });

        it("should remove SF at given index from service-function-dictionary of SFF", function () {
          expect(exampleData.data['service-function-dictionary'].length).toBe(1);
          ServiceForwarderHelper.removeFunction(0, exampleData);
          expect(exampleData.data['service-function-dictionary'].length).toBe(0);
          expect(exampleData.data['service-function-dictionary']).toEqual([]);
        });

        it("should create connected-sff-dictionary property and add empty SFF to SFF", function () {
          exampleData.data = {};
          expect(exampleData.data['connected-sff-dictionary']).toBeUndefined();
          ServiceForwarderHelper.addForwarder(exampleData);
          expect(exampleData.data['connected-sff-dictionary'].length).toBe(1);
          expect(exampleData.data['connected-sff-dictionary']).toEqual([
            {
              nonExistent: false,
              "sff-sff-data-plane-locator": {},
              "sff-interfaces": []
            }
          ]);
        });

        it("should add empty SFF to connected-sff-dictionary of SFF", function () {
          expect(exampleData.data['connected-sff-dictionary'].length).toBe(1);
          ServiceForwarderHelper.addForwarder(exampleData);
          expect(exampleData.data['connected-sff-dictionary'].length).toBe(2);
          expect(exampleData.data['connected-sff-dictionary']).toEqual([
            {
              nonExistent: false,
              "sff-sff-data-plane-locator": {},
              "sff-interfaces": []
            },
            {
              nonExistent: false,
              "sff-sff-data-plane-locator": {},
              "sff-interfaces": []
            }
          ]);
        });

        it("should remove SFF at given index from connected-sff-dictionary of SFF", function () {
          expect(exampleData.data['connected-sff-dictionary'].length).toBe(1);
          ServiceForwarderHelper.removeForwarder(0, exampleData);
          expect(exampleData.data['connected-sff-dictionary'].length).toBe(0);
          expect(exampleData.data['connected-sff-dictionary']).toEqual([]);
        });

        it("should convert SFF interfaces object array to one string", function () {
          var string = ServiceForwarderHelper.sffInterfaceToString(exampleData.sffInterfacesObjectArray);
          expect(string).toEqual(exampleData.sffInterfaceString);
        });

        it("should convert SFF interfaces object array to string array", function () {
          var stringArray = ServiceForwarderHelper.sffInterfaceToStringArray(exampleData.sffInterfacesObjectArray);
          expect(stringArray).toEqual(exampleData.sffInterfaceStringArray);
        });

        it("should convert SFF interfaces string array to object array", function () {
          var objectArray = ServiceForwarderHelper.sffInterfaceToObjectArray(exampleData.sffInterfaceStringArray);
          expect(objectArray).toEqual(exampleData.sffInterfacesObjectArray);
        });

        it("should convert SFF interfaces in SFF object array to one string", function () {
          var string = ServiceForwarderHelper.sffInterfaceInSffToString(exampleData.sffInterfacesInSffObjectArray);
          expect(string).toEqual(exampleData.sffInterfaceInSffString);
        });

        it("should convert SFF interfaces in SFF object array to string array", function () {
          var stringArray = ServiceForwarderHelper.sffInterfaceInSffToStringArray(exampleData.sffInterfacesInSffObjectArray);
          expect(stringArray).toEqual(exampleData.sffInterfaceInSffStringArray);
        });

        it("should convert SFF interfaces in SFF string array to object array", function () {
          var objectArray = ServiceForwarderHelper.sffInterfaceInSffToObjectArray(exampleData.sffInterfaceInSffStringArray);
          expect(objectArray).toEqual(exampleData.sffInterfacesInSffObjectArray);
        });

        it("should convert SFF DP locators to one string", function () {
          var string = ServiceForwarderHelper.sffDpLocatorToString(exampleData.sffDpLocatorObjectArray);
          expect(string).toEqual(exampleData.sffDpLocatorString);
        });

        it("should convert SFF function dictionary object array to one string", function () {
          var string = ServiceForwarderHelper.sffFunctionDictionaryToString(exampleData.sffFunctionDictionaryObjectArray);
          expect(string).toEqual(exampleData.sffFunctionDictionaryString);
        });

        it("should convert SFF forwarder dictionary object array to one string", function () {
          var string = ServiceForwarderHelper.sffForwarderDictionaryToString(exampleData.sffForwarderDictionaryObjectArray);
          expect(string).toEqual(exampleData.sffForwarderDictionaryString);
        });

        it("should remove temporary properties from SF", function () {
          var sfPersisted = {"name": "sf_p"};
          var sfTemporary = {"name": "sf_t", nonExistent: true};
          ServiceForwarderHelper.removeTemporaryPropertiesFromSf(sfPersisted);
          expect(sfPersisted).toEqual({"name": "sf_p"});
          ServiceForwarderHelper.removeTemporaryPropertiesFromSf(sfTemporary);
          expect(sfTemporary).toEqual({"name": "sf_t"});
        });

        it("should remove temporary properties from SFF", function () {
          var sffPersisted = {"name": "sff_p"};
          var sffTemporary = {"name": "sff_t", nonExistent: true};
          ServiceForwarderHelper.removeTemporaryPropertiesFromSff(sffPersisted);
          expect(sffPersisted).toEqual({"name": "sff_p"});
          ServiceForwarderHelper.removeTemporaryPropertiesFromSff(sffTemporary);
          expect(sffTemporary).toEqual({"name": "sff_t"});
        });

        it("should remove nonExistent SN from SFF", function () {
          var sffWithoutSn = {"name": "sff_wo"};
          var sffWithNonExistentSn = {"name": "sff_ne", "service-node": "random"};
          var sffWithExistentSn = {"name": "sff_ex", "service-node": "sn1"};
          ServiceForwarderHelper.removeNonExistentSn(sffWithoutSn, exampleData.sns);
          expect(sffWithoutSn).toEqual({"name": "sff_wo"});
          ServiceForwarderHelper.removeNonExistentSn(sffWithNonExistentSn, exampleData.sns);
          expect(sffWithNonExistentSn).toEqual({"name": "sff_ne"});
          ServiceForwarderHelper.removeNonExistentSn(sffWithExistentSn, exampleData.sns);
          expect(sffWithExistentSn).toEqual({"name": "sff_ex", "service-node": "sn1"});
        });

        it("should add type of SF to choosen SF", function () {
          var choosenSf = {"name": "sf1"};
          ServiceForwarderHelper.addSfTypeToChoosenSf(choosenSf, exampleData.sfModelData);
          expect(choosenSf).toEqual({"name": "sf1", "type": "dpi"});
        });

        it("should listen to change of choosenSf and keep this entry updated", function () {
          spyOn(ServiceForwarderHelper, 'addSfTypeToChoosenSf').andCallThrough();
          var choosenSf = {"name": "sf1"};
          scope.sfs = exampleData.sfs;
          scope.DpLocators = {};
          ServiceForwarderHelper.sfChangeListener(choosenSf, scope);
          expect(ServiceForwarderHelper.addSfTypeToChoosenSf).toHaveBeenCalledWith(choosenSf, scope.sfs[0]);
          expect(choosenSf).toEqual({"name": "sf1", "type": "dpi"});
        });

        it("should listen to change of choosenSf - testing inner else branch - should do nothing", function () {
          spyOn(ServiceForwarderHelper, 'addSfTypeToChoosenSf').andCallThrough();
          var choosenSf = {"name": "sf1"};
          ServiceForwarderHelper.sfChangeListener(choosenSf, scope);
          expect(ServiceForwarderHelper.addSfTypeToChoosenSf).not.toHaveBeenCalled();
        });

        it("should listen to change of choosenSf - testing outter else branch - should do nothing", function () {
          spyOn(ServiceForwarderHelper, 'addSfTypeToChoosenSf').andCallThrough();
          var choosenSf;
          ServiceForwarderHelper.sfChangeListener(choosenSf, scope);
          expect(ServiceForwarderHelper.addSfTypeToChoosenSf).not.toHaveBeenCalled();
        });

        it("should supplement SF model data with data required for Edit dialog", function () {
          spyOn(ServiceForwarderHelper, 'sffInterfaceToStringArray').andCallThrough();
          var sf = {"name": "sf1", "sff-interfaces": [
            {"sff-interface": "sfi1"},
            {"sff-interface": "sfi2"}
          ], "sff-sf-data-plane-locator": {"ip": "10.0.0.1", "port": "8000"}};
          var sffInterfaces = sf['sff-interfaces'];
          scope.sfs = exampleData.sfs;
          scope.DpLocators = {};
          scope.selectedDpLocator = {};
          ServiceForwarderHelper.sfUpdate(sf, scope);
          expect(ServiceForwarderHelper.sffInterfaceToStringArray).toHaveBeenCalledWith(sffInterfaces);
          expect(sf['sff-interfaces']).toEqual(["sfi1", "sfi2"]);
          expect(sf.nonExistent).toBeFalsy();
        });

        it("should supplement SF model data with data required for Edit dialog - testing inner else branch", function () {
          spyOn(ServiceForwarderHelper, 'sffInterfaceToStringArray').andCallThrough();
          var sf = {"name": "sf1", "sff-interfaces": [
            {"sff-interface": "sfi1"},
            {"sff-interface": "sfi2"}
          ], "sff-sf-data-plane-locator": {"ip": "10.0.0.1", "port": "8000"}};
          var sffInterfaces = sf['sff-interfaces'];
          ServiceForwarderHelper.sfUpdate(sf, scope);
          expect(ServiceForwarderHelper.sffInterfaceToStringArray).toHaveBeenCalledWith(sffInterfaces);
          expect(sf.nonExistent).toBeTruthy();
        });

        it("should supplement SF model data with data required for Edit dialog - testing outter else branch", function () {
          spyOn(ServiceForwarderHelper, 'sffInterfaceToStringArray').andCallThrough();
          var sf;
          ServiceForwarderHelper.sfUpdate(sf, scope);
          expect(ServiceForwarderHelper.sffInterfaceToStringArray).not.toHaveBeenCalled();
          expect(sf).toBeUndefined();
        });

        it("should supplement SFF model data with data required for Edit dialog", function () {
          spyOn(ServiceForwarderHelper, 'sffInterfaceInSffToStringArray').andCallThrough();
          var sff = {"name": "sff1", "sff-interfaces": [
            {"sff-interface": "sfi1"},
            {"sff-interface": "sfi2"}
          ], "sff-sff-data-plane-locator": {"ip": "10.0.0.1", "port": "8000"}};
          var sffInterfaces = sff['sff-interfaces'];
          scope.sffs = exampleData.sffs;
          scope.DpLocators = {};
          scope.selectedDpLocator = {};
          ServiceForwarderHelper.sffUpdate(sff, scope);
          expect(ServiceForwarderHelper.sffInterfaceInSffToStringArray).toHaveBeenCalledWith(sffInterfaces);
          expect(sff['sff-interfaces']).toEqual(["sfi1", "sfi2"]);
          expect(sff.nonExistent).toBeFalsy();
        });

        it("should supplement SFF model data with data required for Edit dialog - testing inner else branch", function () {
          spyOn(ServiceForwarderHelper, 'sffInterfaceInSffToStringArray').andCallThrough();
          var sff = {"name": "sff1", "sff-interfaces": [
            {"sff-interface": "sfi1"},
            {"sff-interface": "sfi2"}
          ], "sff-sff-data-plane-locator": {"ip": "10.0.0.1", "port": "8000"}};
          var sffInterfaces = sff['sff-interfaces'];
          ServiceForwarderHelper.sffUpdate(sff, scope);
          expect(ServiceForwarderHelper.sffInterfaceInSffToStringArray).toHaveBeenCalledWith(sffInterfaces);
          expect(sff.nonExistent).toBeTruthy();
        });

        it("should supplement SFF model data with data required for Edit dialog - testing outter else branch", function () {
          spyOn(ServiceForwarderHelper, 'sffInterfaceInSffToStringArray').andCallThrough();
          var sff;
          ServiceForwarderHelper.sffUpdate(sff, scope);
          expect(ServiceForwarderHelper.sffInterfaceInSffToStringArray).not.toHaveBeenCalled();
          expect(sff).toBeUndefined();
        });

        it("should return proper options for select2", function () {
          var options = ServiceForwarderHelper.selectOptions(exampleData.sffDpLocators);
          expect(options.multiple).toBeTruthy();
          expect(options['simple_tags']).toBeTruthy();
          var tags = options.tags();
          expect(tags).toEqual(["sffdp1"]);
          delete exampleData.sffDpLocators.data['sff-data-plane-locator'][0].name;
          tags = options.tags();
          expect(tags).toEqual([]);
        });
      });
    });
  });
});
