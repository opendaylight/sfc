define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory("SfcConfigSvc", function ($q, SfcJsonValidationSvc, ServiceFunctionSvc, ServiceNodeSvc, ServiceChainSvc, ServiceForwarderSvc, sfcFormatMessage) {

    var sfcModelRevision = "sfc-rev-2014-07-01";

    var SF_PARSE = "\\{\\s*\"service-functions\"\\s*:\\s*\\{[\\w\\W]*\\]\\s*\\}\\s*\\}";
    var SN_PARSE = "\\{\\s*\"service-nodes\"\\s*:\\s*\\{[\\w\\W]*\\]\\s*\\}\\s*\\}";
    var SFC_PARSE = "\\{\\s*\"service-function-chains\"\\s*:\\s*\\{[\\w\\W]*\\]\\s*\\}\\s*\\}";
    var SFF_PARSE = "\\{\\s*\"service-function-forwarders\"\\s*:\\s*\\{[\\w\\W]*\\]\\s*\\}\\s*\\}";


    // define 'Class' - RestconfConfigApplier
    function RestconfConfigApplier(deferred, pool) {
      this.deferred = deferred;
      this.pool = pool;
      this.appliedCount = 0;

      // final callback after sending PUT RESTs, or in case of Error
      this.afterApplyConfigPoolCallback = function (err) {
        if (err) {
          console.error(err);
          this.deferred.reject(err.message);
        } else {
          this.deferred.resolve(sfcFormatMessage('{0} object(s) applied', this.appliedCount));
        }
      };

      // synchronously (thru recursive callbacks) send PUT requests based on data in 'pool' array
      this.applyConfigPool = function () {

        var instance = this;

        // define common callback fn after REST operation
        var callback = function (response, containerData) {
          try {
            if (response) {
              throw new Error(sfcFormatMessage("status code: {0}, for PUT opration:\n\n{1}\n\n{2}", response.status, containerData, response.data));
            } else {
              instance.appliedCount++;
              instance.applyConfigPool();
            }
          } catch (e) {
            console.error(e);
            instance.afterApplyConfigPoolCallback(e);
          }
        };


        if (this.pool.length > 0) {
          var poolItem = this.pool.shift();   // shift from array

          switch (poolItem.model) {

            case 'sf':
            {
              ServiceFunctionSvc.putContainerWrapper(poolItem.container, callback);
              break;
            }

            case 'sn':
            {
              ServiceNodeSvc.putContainerWrapper(poolItem.container, callback);
              break;
            }

            case 'sfc':
            {
              ServiceChainSvc.putContainerWrapper(poolItem.container, callback);
              break;
            }

            case 'sff':
            {
              ServiceForwarderSvc.putContainerWrapper(poolItem.container, callback);
              break;
            }

            default :
            {
              throw new Error("Unknown config item in pool");
            }
          }
        } else {
          this.afterApplyConfigPoolCallback(null); // done - without error
        }
      };
    }


    var svc = {};

    // throws exception with formatted message in case of invalid json
    svc.checkJsonSchemaNew = function (dataObject, type, revision) {

      SfcJsonValidationSvc.sfcValidate(dataObject, type, revision);
    };


    // check for SF json, and in case store in pool
    svc.processSF = function (token, pool, runValidation) {

      var regexp = new RegExp(SF_PARSE);

      var match = regexp.exec(token);

      if (!match || match === null) {
        return false;
      }

      try {
        var parsedJson = $.parseJSON(match);

        // Validation to SF schema
        if (runValidation) {
          svc.checkJsonSchemaNew(parsedJson, "sf", sfcModelRevision);
        }

        pool.push({model: "sf", container: parsedJson});

      } catch (e) {
        throw e;
      }

      return true;
    };


    // check for SN json, validate, and in case store in pool
    svc.processSN = function (token, pool, runValidation) {

      var regexp = new RegExp(SN_PARSE);

      var match = regexp.exec(token);

      if (!match || match === null) {
        return false;
      }

      try {
        var parsedJson = $.parseJSON(match);

        // Validation to SN schema
        if (runValidation) {
          svc.checkJsonSchemaNew(parsedJson, "sn", sfcModelRevision);
        }

        pool.push({model: "sn", container: parsedJson});

      } catch (e) {
        throw e;
      }

      return true;
    };

    // check for SFC json, and in case store in pool
    svc.processSFC = function (token, pool, runValidation) {

      var regexp = new RegExp(SFC_PARSE);

      var match = regexp.exec(token);

      if (!match || match === null) {
        return false;
      }
      try {
        var parsedJson = $.parseJSON(match);

        // Validation to SFC schema
        if (runValidation) {
          svc.checkJsonSchemaNew(parsedJson, "sfc", sfcModelRevision);
        }

        pool.push({model: "sfc", container: parsedJson});

      } catch (e) {
        throw e;
      }

      return true;
    };

    // check for SFF json, and in case store in pool
    svc.processSFF = function (token, pool, runValidation) {

      var regexp = new RegExp(SFF_PARSE);

      var match = regexp.exec(token);

      if (!match || match === null) {
        return false;
      }
      try {
        var parsedJson = $.parseJSON(match);

        // Validation to SFC schema
        if (runValidation) {
          svc.checkJsonSchemaNew(parsedJson, "sff", sfcModelRevision);
        }

        pool.push({model: "sff", container: parsedJson});

      } catch (e) {
        throw e;
      }

      return true;
    };

    // entry function - supported ar SF, SN, SFC, SFF
    svc.runConfig = function (fileContent, validateBeforeApply) {
      var deferred = $q.defer();

      try {
        var pool = [];

        // var position = 0;  // not used so far - intended for

        var tokens = fileContent.split(";");

        _.each(tokens, function (token) {

          if (!_.isEmpty(token)) {    // skip blank strings

            if (svc.processSF(token, pool, validateBeforeApply)) {
              // noop
            } else if (svc.processSN(token, pool, validateBeforeApply)) {
              // noop
            } else if (svc.processSFC(token, pool, validateBeforeApply)) {
              // noop
            } else if (svc.processSFF(token, pool, validateBeforeApply)) {
              // noop
            } else {
              throw new Error(sfcFormatMessage("Error in input - unsupported object:\n {0}", _.str.prune(token, 100)));
            }
          }

        });

        if (pool.length !== 0) {
          var instance = new RestconfConfigApplier(deferred, pool);
          instance.applyConfigPool();
        } else {
          deferred.reject('No config json in file.');
        }

      } catch (e) {
        console.error(e);
        deferred.reject(e.message);
      } finally {
        return deferred.promise;
      }
    };

    // export
    return {
      "runConfig": svc.runConfig,
      "getValidationRevision": function () {
        return sfcModelRevision;
      }
    };

  });

  // **** service: SfcConfigExportSvc ****
  sfc.register.factory("SfcConfigExportSvc", function (SfcRestconfError, ServiceFunctionSvc, ServiceForwarderSvc, ServiceNodeSvc, ServiceChainSvc) {

    var svc = {};

    svc.exportConfig = function (receiveCallback) {

      var modelSvcList = [ServiceFunctionSvc, ServiceForwarderSvc, ServiceNodeSvc, ServiceChainSvc];

      _.each(modelSvcList, function (restSvcItem) {

        restSvcItem.exportContainer(receiveCallback);

      });
    };

    return svc;
  });

  // **** service: SfcFileReaderSvc ****
  sfc.register.factory("SfcFileReaderSvc",
    ["$q", "$log", function ($q, $log) {

      var onLoad = function (reader, deferred, scope) {
        return function () {
          scope.$apply(function () {
            deferred.resolve(reader.result);
          });
        };
      };

      var onError = function (reader, deferred, scope) {
        return function () {
          scope.$apply(function () {
            deferred.reject(reader.result);
          });
        };
      };

      var onProgress = function (reader, scope) {
        return function (event) {
          scope.$broadcast("fileProgress",
            {
              total: event.total,
              loaded: event.loaded
            });
        };
      };

      var getReader = function (deferred, scope) {
        var reader = new FileReader();
        reader.onload = onLoad(reader, deferred, scope);
        reader.onerror = onError(reader, deferred, scope);
        reader.onprogress = onProgress(reader, scope);
        return reader;
      };

      // returns promise with the txt content of the file, registers 'fileProgress' broadcast in scope
      var readAsText = function (file, scope) {
        $log.debug(file);

        var deferred = $q.defer();

        var reader = getReader(deferred, scope);
        reader.readAsText(file);

        return deferred.promise;
      };

      // service exports
      return {
        readAsText: readAsText
      };
    }]);


});