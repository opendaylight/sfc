define(['app/sfc/sfc.module'], function (sfc) {

  var IPV4_REGEXP_PATTERN = "(?:[0-9]{1,3}\\.){3}[0-9]{1,3}";
  var IPV6_REGEXP_PATTERN = "((?:[\\da-f]{1,4}(?::|)){0,8})(::)?((?:[\\da-f]{1,4}(?::|)){0,8})";

  var IP_COMBINED_REGEXP = "^((" + IPV4_REGEXP_PATTERN + "){1}|(" + IPV6_REGEXP_PATTERN + "){1})$";


  // FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
  // FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
  // FFFFFFFFFFFFFFF SfcSchemaStoreSvc FFFFF
  // FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
  // FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
  sfc.register.factory("SfcSchemaStoreSvc", function () {

    var REVISION_COLLECTIONS = {};

    // func
    getRevisionCollectionForRevision = function (revision) {
      return  REVISION_COLLECTIONS[revision];
    };

    getRegisteredRevisions = function () {
      var result = [];

      _.keys(REVISION_COLLECTIONS, function (key) {
        result.push(key);
      });
    };

    // func - registers new collecion for validation schemas for particular model revision
    getSchemaRegisterFuncForRevision = function (revision) {

      var revisionCollection = REVISION_COLLECTIONS[revision];

      if (revisionCollection) {
        throw new Error('collection already exists!!!');
      }

      // new
      revisionCollection = {
        schemas: {},        // name to schema json map (register)
        types: {},          // name to type map (register)
        typeToUriMap: {}    // to be populated in 'SfcJsonValidationSvc'
      };

      REVISION_COLLECTIONS[revision] = revisionCollection;

      var registerFunc = function (name, schemaJson, type) {
        revisionCollection.schemas[name] = schemaJson;
        if (type) {
          revisionCollection.types[name] = type;
        }
      };

      return registerFunc;
    };

    return {
      getRevisionCollectionForRevision: getRevisionCollectionForRevision,
      getSchemaRegisterFuncForRevision: getSchemaRegisterFuncForRevision,
      getRegisteredRevisions: getRegisteredRevisions
    };
  });


  // FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
  // FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
  // FFFFFFFFFFFFFFF SfcJsonValidationSvc FFFFF
  // FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
  // FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF
  sfc.register.factory("SfcJsonValidationSvc", function (SfcErrorSvc, SfcSchemaStoreSvc, JsvLoaderSvc, sfcRegisterSchemas2014_07_01) {

    var dummy = sfcRegisterSchemas2014_07_01; // register schemas

    //  if environment does not exist yet, create and configure environment for revision
    var getConfiguredRevisionCollection = function (revision) {
      var revisionCollection = SfcSchemaStoreSvc.getRevisionCollectionForRevision(revision);

      if (!revisionCollection) {
        throw new SfcErrorSvc("revision collection: {0} does not exist!", revision);
      }

      var env = revisionCollection.env; // look up if already cached

      if (!env) {
        // create new JSV environment and populate typeToUriMmap
        env = JsvLoaderSvc.getJSV().createEnvironment();

        var schemas = revisionCollection.schemas;

        // add all schemas to env
        _.each(_.keys(schemas), function (name) {
          var jsonSchema = env.createSchema(schemas[name], undefined, schemas[name].id);

          // connect particular to 'type'
          if (revisionCollection['types'][name]) {
            var type = revisionCollection['types'][name];
            revisionCollection['typeToUriMap'][type] = jsonSchema._uri;
          }
        });

        // cache JSV environment
        revisionCollection.env = env;
      }

      return revisionCollection;
    };

    // validate instanceJSON to schema(configured for type [sf, sfc, sn] using configured JSV environment)
    var sfcValidate = function (instanceJSON, type, revision) {

      var revisionCollection = getConfiguredRevisionCollection(revision);

      var uriForType = revisionCollection['typeToUriMap'][type];

      if (!uriForType) {
        throw new SfcErrorSvc("No validation schema URI for type: {0} defined!", type);
      }

      var env = revisionCollection.env;

      var schemaJSON = env.findSchema(uriForType);

      var report = env.validate(instanceJSON, schemaJSON); // actual validation

      // check
      if (report.errors.length === 0) {
        //JSON is valid against the schema - OK
      } else {
        // format error message

        var error = report.errors[0];
        var errorUri = error.uri;
        var message = error.message;
        var details = error.details;

        var errorPath = errorUri.replace(new RegExp("^[\\w\\W]*#"), "")
          .replace(new RegExp("\/items\/properties", "g"), "")
          .replace(new RegExp("\/properties", "g"), "")
          .replace(new RegExp("\/([\\d]+)", "g"), "[$1]"); // match array index

        throw new SfcErrorSvc("object:\n{0}\n\ndoes not validate to schema.\npath: {1}\n\nmessage: {2}{3}",
          _.str.prune(angular.toJson(instanceJSON, true), 300),   // shorten to 300 chars in case
          errorPath,
          message,
          details ? "\n\ndetails: " + details : '');
      }
    };

    // service exports
    return {
      sfcValidate: sfcValidate
    };

  });

  // register json validation schemas for revision 'sfc-rev-2014-07-01'
  sfc.register.factory('sfcRegisterSchemas2014_07_01', function (SfcSchemaStoreSvc) {

    var registerFunc = SfcSchemaStoreSvc.getSchemaRegisterFuncForRevision("sfc-rev-2014-07-01");

    // locator - case - ip
    registerFunc("SL_DATA_PLANE_LOCATOR_SCHEMA_CASE_IP_14_07_01", {

      "$schema": "http://json-schema.org/draft-03/schema#",
      "id": "#sfc-sl:data-plane-locator:case-ip",
      "type": "object",

      "properties": {
        "name": {
          "type": "string",
          "optional": false
        },

        "service-function-forwarder": {
          "type": "string",
          "optional": false
        },

        "ip": {
          "type": "string",
          "pattern": IP_COMBINED_REGEXP,
          "optional": false
        },

        "port": {
          "type": ["number", "string"],
          "optional": false
        }
      },
      additionalProperties: false
    });

    // locator - case - mac
    registerFunc("SL_DATA_PLANE_LOCATOR_SCHEMA_CASE_MAC_14_07_01", {

      "$schema": "http://json-schema.org/draft-03/schema#",
      "id": "#sfc-sl:data-plane-locator:case-mac",
      "type": "object",

      "properties": {
        "name": {
          "type": "string",
          "optional": false
        },

        "service-function-forwarder": {
          "type": "string",
          "optional": false
        },

        "mac": {
          "type": "string",
          "optional": false
        },

        "vlan-id": {
          "type": ["number", "string"],
          "optional": false
        }
      },
      additionalProperties: false
    });

    // locator - case - mac
    registerFunc("SL_DATA_PLANE_LOCATOR_SCHEMA_CASE_LISP_14_07_01", {

      "$schema": "http://json-schema.org/draft-03/schema#",
      "id": "#sfc-sl:data-plane-locator:case-lisp",
      "type": "object",

      "properties": {
        "name": {
          "type": "string",
          "optional": false
        },

        "service-function-forwarder": {
          "type": "string",
          "optional": false
        },

        "eid": {
          "type": "string",
          "pattern": IP_COMBINED_REGEXP,
          "optional": false
        }
      },
      additionalProperties: false
    });

    // locator - choice
    registerFunc("SL_DATA_PLANE_LOCATOR_SCHEMA_14_07_01", {

      "$schema": "http://json-schema.org/draft-03/schema#",
      "id": "#sfc-sl:data-plane-locator",
      "type": [
        {"$ref": "#sfc-sl:data-plane-locator:case-ip"},
        {"$ref": "#sfc-sl:data-plane-locator:case-mac"},
        {"$ref": "#sfc-sl:data-plane-locator:case-lisp"}
      ]  // choice locator-type
    });

    // sf-entry
    registerFunc("SF_ENTRY_SCHEMA_14_07_01", {

      "$schema": "http://json-schema.org/draft-03/schema#",
      "id": "#sfc-sf:service-function-entry",
      "type": "object",

      properties: {

        "name": {
          "type": "string",
          "optional": false
        },

        "type": {
          "type": "string",
          "optional": false
        },

        "nsh-aware": {
          "type": "string",
          "optional": true
        },

        "ip-mgmt-address": {
          "type": "string",
          "pattern": IP_COMBINED_REGEXP,
          "optional": true
        },

        "sf-data-plane-locator": {
          "type": "array",
          "items": {
            "$ref": "#sfc-sl:data-plane-locator"
          },
          "optional": true
        },

        "service-function-forwarder": {
          "type": "string",
          "optional": true
        }

      },

      additionalProperties: false
    });

    // main sf
    registerFunc("SF_SERVICE_FUNCTIONS_SCHEMA_14_07_01", {

      "$schema": "http://json-schema.org/draft-03/schema#",
      "id": "#sfc-sf:service-functions",
      "type": "object",
      "properties": {

        "service-functions": {
          "type": "object",
          "optional": false,
          "properties": {

            "service-function": {
              "type": "array",
              "optional": false,
              "items": {
                "$ref": "#sfc-sf:service-function-entry"
              }
            }
          },
          additionalProperties: false
        }
      },
      additionalProperties: false
    }, "sf");


    registerFunc("SFC_SCHEMA_14_07_01", {
      "$schema": "http://json-schema.org/draft-03/schema#",
      "id": "#sfc-sfc:service-function-chains",
      "type": "object",
      "properties": {

        "service-function-chains": {
          "type": "object",
          "optional": false,
          "properties": {

            "service-function-chain": {
              "type": "array",
              "optional": false,
              "items": {

                properties: {
                  "name": {
                    "type": "string",
                    "optional": false
                  },

                  "sfc-service-function": {
                    "type": "array",
                    "optional": false,
                    "items": {

                      "properties": {
                        "name": {
                          "type": "string",
                          "optional": false
                        },

                        "type": {
                          "type": "string",
                          "optional": false
                        }
                      },
                      additionalProperties: false
                    }
                  },

                  "sfc-service-function-path": {
                    "type": "array",
                    "optional": true,
                    "items": {
                      "type": "string"
                    }
                  }
                },
                additionalProperties: false
              }
            }
          },
          additionalProperties: false
        }
      },
      additionalProperties: false
    }, "sfc");

    registerFunc("SN_SCHEMA_14_07_01", {
      "$schema": "http://json-schema.org/draft-03/schema#",
      "id": "#sfc-sn:service-nodes",
      "type": "object",
      "properties": {

        "service-nodes": {
          "type": "object",
          "optional": false,
          "properties": {

            "service-node": {
              "type": "array",
              "optional": false,
              "items": {

                properties: {
                  "name": {
                    "type": "string",
                    "optional": false
                  },

                  "ip-mgmt-address": {
                    "type": "string",
                    "pattern": IP_COMBINED_REGEXP,
                    "optional": false
                  },

                  "service-function": {
                    "type": "array",
                    "optional": false,
                    "items": {
                      "type": "string"
                    }
                  }
                },
                additionalProperties: false
              }
            }
          },
          additionalProperties: false
        }
      },
      additionalProperties: false
    }, "sn");

    registerFunc("SFF_SCHEMA_14_07_01", {
      "$schema": "http://json-schema.org/draft-03/schema#",
      "id": "#sfc-sff:service-function-forwarders",
      "type": "object",
      "properties": {

        "service-function-forwarders": {
          "type": "object",
          "optional": false,
          "properties": {

            "service-function-forwarder": {
              "type": "array",
              "optional": false
            }
          },
          additionalProperties: false
        }
      },
      additionalProperties: false
    }, "sff");

    return "dummy";
  });


  // **** service: JsvLoaderSvc ****
  sfc.register.factory("JsvLoaderSvc", function ($q) {

    var validator = null;

    // loads the library with dependencies, returns promise with future JSV object
    var loadValidator = function () {
      var deferred = $q.defer();

      if (!validator) { // load only once

        var basePath = '/src/app/sfc/assets/js/jsv/';//window.isAssetsDirInSrc ? "/src/app/sfc/assets/js/jsv/" : "../assets/js/jsv/";   // !!!  '.' vs '..'

        // JSV must be loaded in following sequence
        require([basePath + 'uri.js'], function () {
          require([basePath + 'urn.js'], function () {
            require([basePath + 'jsv.js'], function () {
              require([basePath + 'json-schema-draft-03.js'], function () {
                validator = exports.JSV;
                deferred.resolve(validator);
              });
            });
          });
        });

      } else {
        deferred.resolve(validator); // resolve immediately
      }

      return deferred.promise;
    };

    // returns JSV object
    var getJSV = function () {
      if (validator !== null) {
        return validator;
      } else {
        alert("JSV probably not fully loaded yet!");
      }
      return null;
    };

    loadValidator(); // launch loading asynchronously!!!

    // service exports
    return {
      loadValidator: loadValidator,
      getJSV: getJSV,
      isLoaded: function () {
        return (validator !== null);
      }
    };
  });

});