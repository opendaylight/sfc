define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC module parts in schema.services.js', function () {

    beforeEach(angular.mock.module('app.sfc'));

    describe('SfcJsonValidationSvc service', function () {

      var SfcJsonValidationSvc;

      // tested service
      beforeEach(angular.mock.inject(function (_SfcJsonValidationSvc_, JsvLoaderSvc, $rootScope) {
        SfcJsonValidationSvc = _SfcJsonValidationSvc_;

        waitsFor(function () {  // wait for async loading of JSV libraries
          $rootScope.$digest();
          return  JsvLoaderSvc.isLoaded();
        });
      }));

      it('Check validation roughly working - Success', function () {
        var sfDataTest = {
          "service-functions": {
            "service-function": [
              {
                "sf-data-plane-locator": [{
                  "name": "dpl1",
                  "service-function-forwarder": "SFF-bootstrap",
                  "port": "10003",
                  "ip": "10.3.1.102"
                }],
                "ip-mgmt-address": "10.3.1.102",
                "name": "dpi-102-3",
                "type": "dpi"
              }
            ]
          }
        };

        SfcJsonValidationSvc.sfcValidate(sfDataTest, 'sf', "sfc-rev-2014-07-01");
      });

      it('Check validation roughly working - Error', function () {
        var sfDataTest = {
          "service-functions": {
            "service-function": [
              {
                "sf-data-plane-locator": [{
                  "name": "dpl1",
                  "service-function-forwarder": "SFF-bootstrap",
                  "port": "10003",
                  "ip": "10.3.1.102"
                }],
                "ip-mgmt-address": "10.3.1.102",
//                "name": "dpi-102-3",  // name property required!
                "type": "dpi"
              }
            ]
          }
        };

        expect(function(){
          SfcJsonValidationSvc.sfcValidate(sfDataTest, 'sf', "sfc-rev-2014-07-01");
        }).toThrow(undefined); // expected message not specified  (too specific and complicated)
      });

    });

  });

});

