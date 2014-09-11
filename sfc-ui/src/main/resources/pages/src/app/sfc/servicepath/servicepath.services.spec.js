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

    beforeEach(function (){
      exampleData = {};
      exampleData.unOrderedSfp = {'starting-index': 3, 'service-path-hop': [
        {"hop-number": 0, 'service_index': 3, 'service-function-name': "sf1"},
        {"hop-number": 2, 'service_index': 1, 'service-function-name': "sf3"},
        {"hop-number": 1, 'service_index': 2, 'service-function-name': "sf2"}
      ]};
      exampleData.orderedSfp = {'starting-index': 3, 'service-path-hop': [
        {"hop-number": 0, 'service_index': 3, 'service-function-name': "sf1"},
        {"hop-number": 1, 'service_index': 2, 'service-function-name': "sf2"},
        {"hop-number": 2, 'service_index': 1, 'service-function-name': "sf3"}
      ]};
      exampleData.updatedSfp = {'starting-index': 3, 'service-path-hop': [
        {"hop-number": 0, 'service_index': 3, 'service-function-name': "sf1"},
        {"hop-number": 1, 'service_index': 2, 'service-function-name': "sf3"},
        {"hop-number": 2, 'service_index': 1, 'service-function-name': "sf2"}
      ]};
      exampleData.badStartingIndexSfp = {'starting-index': 1, 'service-path-hop': [
        {"hop-number": 0, 'service_index': 3, 'service-function-name': "sf1"},
        {"hop-number": 1, 'service_index': 2, 'service-function-name': "sf2"},
        {"hop-number": 2, 'service_index': 1, 'service-function-name': "sf3"}
      ]};
      exampleData.augmentedSfp = {'starting-index': 3, 'service-path-hop': [
        {"hop-number": 0, 'service_index': 3, 'service-function-name': "sf1"},
        {'service-function-name': "sf5"},
        {"hop-number": 1, 'service_index': 2, 'service-function-name': "sf2"},
        {"hop-number": 2, 'service_index': 1, 'service-function-name': "sf3"},
        {'service-function-name': "sf4"}
      ]};
      exampleData.correctedSfp = {'starting-index': 5, 'service-path-hop': [
        {"hop-number": 0, 'service_index': 5, 'service-function-name': "sf1"},
        {"hop-number": 1, 'service_index': 4, 'service-function-name': "sf5"},
        {"hop-number": 2, 'service_index': 3, 'service-function-name': "sf2"},
        {"hop-number": 3, 'service_index': 2, 'service-function-name': "sf3"},
        {"hop-number": 4, 'service_index': 1, 'service-function-name': "sf4"}
      ]};
    });

    describe('servicepath.services', function () {

      describe('ServicePathHelper', function () {

        var ServicePathHelper;

        beforeEach(angular.mock.inject(function (_ServicePathHelper_) {
          ServicePathHelper = _ServicePathHelper_;
        }));

        it("should order hops in SFP descending by service_index", function () {
          ServicePathHelper.orderHopsInSFP(exampleData.unOrderedSfp);
          expect(exampleData.unOrderedSfp).toEqual(exampleData.orderedSfp);
        });

        it("should update hops order (hop-number and service_index) in SFP", function() {
          ServicePathHelper.updateHopsOrderInSFP(exampleData.unOrderedSfp);
          expect(exampleData.unOrderedSfp).toEqual(exampleData.updatedSfp);
        });

        it("should update starting index of SFP according to count of resident SFs", function () {
          ServicePathHelper.updateStartingIndexOfSFP(exampleData.badStartingIndexSfp);
          expect(exampleData.badStartingIndexSfp).toEqual(exampleData.orderedSfp);
        });

        it("should update starting index and hops order in SFP with added SF", function () {
          ServicePathHelper.updateHopsOrderInSFP(exampleData.augmentedSfp);
          ServicePathHelper.updateStartingIndexOfSFP(exampleData.augmentedSfp);
          expect(exampleData.augmentedSfp).toEqual(exampleData.correctedSfp);
        });

      });
    });
  });
});
