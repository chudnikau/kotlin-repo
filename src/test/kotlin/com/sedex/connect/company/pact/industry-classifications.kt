package com.sedex.connect.company.pact

const val industryClassificationsFlatJson = """
    [
      {
        "code": "ZIC1000000",
        "name": "A - Agriculture, forestry and fishing"
      },
      {
        "code": "ZIC1010000",
        "name": "B - Mining and quarrying"
      },
      {
        "code": "ZIC1000100",
        "name": "Crop and animal production, hunting and related service activities",
        "parentCode": "ZIC1000000"
      },
      {
        "code": "ZIC1000300",
        "name": "Fishing and aquaculture",
        "parentCode": "ZIC1000000"
      },
      {
        "code": "ZIC1000200",
        "name": "Forestry and logging",
        "parentCode": "ZIC1000000"
      },
      {
        "code": "ZIC1010200",
        "name": "Extraction of crude petroleum and natural gas",
        "parentCode": "ZIC1010000"
      },
      {
        "code": "ZIC1010100",
        "name": "Mining of coal and lignite",
        "parentCode": "ZIC1010000"
      },
      {
        "code": "ZIC1010300",
        "name": "Mining of metal ores",
        "parentCode": "ZIC1010000"
      },
      {
        "code": "ZIC1010500",
        "name": "Mining support service activities",
        "parentCode": "ZIC1010000"
      },
      {
        "code": "ZIC1010400",
        "name": "Other mining and quarrying",
        "parentCode": "ZIC1010000"
      }
    ]
"""
