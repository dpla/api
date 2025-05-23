#!/bin/bash
until curl -s http://elasticsearch:9200 > /dev/null; do
    sleep 1
done

response=$(curl -s -o /dev/null -w "%{http_code}" http://elasticsearch:9200/dpla_alias)

if [ $response -eq 200 ]; then
  exit 0
fi

curl -s -X PUT "http://elasticsearch:9200/dpla_alias?include_type_name=true" -H 'Content-Type: application/json' -d'
{
  "settings" : {
    "index": {
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "refresh_interval": "30s",
      "max_result_window": 50000
    },
    "analysis": {
      "normalizer": {
        "canonicalsort": {
          "type": "custom",
          "char_filter": "canonicalsort_char_filter",
          "filter": ["lowercase", "asciifolding"]
        }
      },
      "char_filter": {
        "canonicalsort_char_filter": {
          "type": "pattern_replace",
          "pattern": "[^a-z0-9]+|(?:\\bthe\\b|\\bof\\b|\\band\\b|\\ba\\b|\\ban\\b)",
          "replacement": "",
          "flags": "CASE_INSENSITIVE"
        },
        "editing_notation_filter": {
          "type": "pattern_replace",
          "pattern": "[\\[\\]]",
          "replacement": ""
        }
      },
      "filter": {
        "shingle": {
          "max_shingle_size": "4",
          "min_shingle_size": "2",
          "output_unigrams": "true",
          "type": "shingle"
        }
      },
      "analyzer": {
        "mlt": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "stop", "snowball"]
        },
        "suggestion": {
          "type": "custom",
          "tokenizer": "standard",
          "char_filter": ["editing_notation_filter"],
          "filter": ["lowercase", "asciifolding", "shingle"]
        },
        "nicer_search": {
          "type": "custom",
          "tokenizer": "standard",
          "char_filter": ["editing_notation_filter"],
          "filter": ["lowercase", "asciifolding"]
        }
      }
    }
  },
  "mappings" : {
    "item" : {
      "dynamic" : "false",
      "properties" : {
        "@context" : {
          "type" : "object",
          "enabled" : false
        },
        "@id" : {
          "type" : "keyword"
        },
        "admin" : {
          "properties" : {
            "contributingInstitution": {
              "type": "keyword"
            }
          }
        },
        "aggregatedCHO" : {
          "type" : "object",
          "enabled" : false
        },
        "dataProvider" : {
          "properties" : {
            "@id" : {
              "type" : "keyword"
            },
            "name" : {
              "type" : "text",
              "copy_to": "admin.contributingInstitution",
              "fields" : {
                "not_analyzed" : {
                  "type" : "keyword"
                }
              }
            },
            "exactMatch": {
              "type": "keyword"
            }
          }
        },
        "hasView" : {
          "properties" : {
            "@id" : {
              "type" : "keyword"
            },
            "edmRights" : {
              "type" : "text",
              "fields" : {
                "not_analyzed" : {
                  "type" : "keyword"
                }
              }
            },
            "format" : {
              "type" : "keyword"
            },
            "rights" : {
              "type" : "keyword"
            }
          }
        },
        "id" : {
          "type" : "keyword"
        },
        "iiifManifest" : {
          "type" : "keyword"
        },
        "ingestDate" : {
          "type" : "object",
          "enabled" : false
        },
        "ingestType" : {
          "type" : "object",
          "enabled" : false
        },
        "ingestionSequence" : {
          "type" : "object",
          "enabled" : false
        },
        "intermediateProvider" : {
          "type" : "text",
          "copy_to": "admin.contributingInstitution",
          "fields" : {
            "not_analyzed" : {
              "type" : "keyword"
            }
          }
        },
        "filecoin" : {
          "type" : "keyword"
        },
        "isPartOf" : {
          "properties" : {
            "@id" : {
              "type" : "keyword"
            },
            "name" : {
              "type" : "text",
              "fields" : {
                "not_analyzed" : {
                  "type" : "keyword"
                }
              }
            }
          }
        },
        "isShownAt" : {
          "type" : "keyword"
        },
        "mediaMaster" : {
          "type" : "keyword"
        },
        "object" : {
          "type" : "keyword"
        },
        "originalRecord" : {
          "type" : "object",
          "enabled" : false
        },
        "provider" : {
          "properties" : {
            "@id" : {
              "type" : "keyword"
            },
            "name" : {
              "type" : "text",
              "fields" : {
                "not_analyzed" : {
                  "type" : "keyword"
                }
              }
            },
            "exactMatch": {
              "type": "keyword"
            }
          }
        },
        "rights" : {
          "type" : "keyword"
        },
        "rightsCategory" : {
          "type" : "keyword"
        },
        "tags" : {
          "type" : "keyword"
        },
        "sourceResource" : {
          "properties" : {
            "@id" : {
              "type" : "object",
              "enabled" : false
            },
            "collection" : {
              "properties" : {
                "@id" : {
                  "type" : "keyword"
                },
                "description" : {
                  "type" : "text"
                },
                "id" : {
                  "type" : "keyword"
                },
                "title" : {
                  "type" : "text",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    }
                  }
                }
              }
            },
            "contributor" : {
              "type" : "keyword"
            },
            "creator" : {
              "type" : "text"
            },
            "date" : {
              "properties" : {
                "begin" : {
                  "type" : "date",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    }
                  },
                  "ignore_malformed" : true,
                  "null_value" : "-9999",
                  "format" : "date_optional_time||yyyy||yyyy-MM||yyyy-MM-dd"
                },
                "displayDate" : {
                  "type" : "object",
                  "enabled" : false
                },
                "end" : {
                  "type" : "date",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    }
                  },
                  "ignore_malformed" : true,
                  "null_value" : "-9999",
                  "format" : "date_optional_time||yyyy||yyyy-MM||yyyy-MM-dd"
                }
              }
            },
            "description" : {
              "type" : "text",
              "analyzer": "nicer_search",
              "fields" : {
                "suggestion" : {
                  "type" : "text",
                  "analyzer" : "suggestion"
                }
              }
            },
            "extent" : {
              "type" : "keyword"
            },
            "format" : {
              "type" : "keyword"
            },
            "genre" : {
              "type" : "text"
            },
            "identifier" : {
              "type" : "text",
              "index_options" : "docs"
            },
            "isPartOf" : {
              "type" : "object",
              "enabled" : false
            },
            "language" : {
              "properties" : {
                "iso639_3" : {
                  "type" : "keyword"
                },
                "name" : {
                  "type" : "keyword",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    }
                  }
                }
              }
            },
            "publisher" : {
              "type" : "text",
              "fields" : {
                "not_analyzed" : {
                  "type" : "keyword"
                }
              }
            },
            "relation" : {
              "type" : "text"
            },
            "rights" : {
              "type" : "text"
            },
            "spatial" : {
              "properties" : {
                "city" : {
                  "type" : "text",
                  "analyzer": "nicer_search",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    }
                  }
                },
                "coordinates" : {
                  "type" : "geo_point",
                  "ignore_malformed" : true
                },
                "country" : {
                  "type" : "text",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    }
                  }
                },
                "county" : {
                  "type" : "text",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    }
                  }
                },
                "iso3166-2" : {
                  "type" : "keyword"
                },
                "name" : {
                  "type" : "text",
                  "analyzer": "nicer_search",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    }
                  }
                },
                "region" : {
                  "type" : "text",
                  "analyzer": "nicer_search",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    }
                  }
                },
                "state" : {
                  "type" : "text",
                  "analyzer": "nicer_search",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    }
                  }
                }
              }
            },
            "specType" : {
              "type" : "keyword"
            },
            "stateLocatedIn" : {
              "type": "object",
              "enabled": false
            },
            "subject" : {
              "properties" : {
                "@id" : {
                  "type" : "keyword"
                },
                "@type" : {
                  "type" : "keyword"
                },
                "name" : {
                  "type" : "text",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    },
                    "mlt": {
                      "type" : "text",
                      "analyzer" : "mlt"
                    },
                    "suggestion" : {
                      "type" : "text",
                      "analyzer" : "suggestion"
                    }
                  }
                },
                "scheme" : {
                  "type": "text",
                  "fields": {
                    "not_analyzed": {
                      "type": "keyword"
                    }
                  }
                },
                "exactMatch" : {
                  "type": "keyword"
                }
              }
            },
            "temporal" : {
              "properties" : {
                "begin" : {
                  "type" : "date",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    }
                  },
                  "ignore_malformed" : true,
                  "null_value" : "-9999",
                  "format" : "date_optional_time||yyyy||yyyy-MM||yyyy-MM-dd"
                },
                "displayDate" : {
                  "type" : "object",
                  "enabled" : false
                },
                "encoding" : {
                  "type" : "object",
                  "enabled" : false
                },
                "end" : {
                  "type" : "date",
                  "fields" : {
                    "not_analyzed" : {
                      "type" : "keyword"
                    }
                  },
                  "ignore_malformed" : true,
                  "null_value" : "-9999",
                  "format" : "date_optional_time||yyyy||yyyy-MM||yyyy-MM-dd"
                },
                "point" : {
                  "type" : "object",
                  "enabled" : false
                }
              }
            },
            "title" : {
              "type" : "text",
              "analyzer": "nicer_search",
              "fields": {
                "not_analyzed": {
                  "type": "keyword",
                  "normalizer": "canonicalsort"
                },
                "mlt": {
                  "type" : "text",
                  "analyzer" : "mlt"
                },
                "suggestion" : {
                  "type" : "text",
                  "analyzer" : "suggestion"
                }
              }
            },
            "type" : {
              "type" : "keyword"
            }
          }
        }
      }
    }
  }
}'
