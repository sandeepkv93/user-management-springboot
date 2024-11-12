#!/bin/bash

awslocal s3 mb s3://user-profiles
awslocal s3api put-bucket-cors --bucket user-profiles --cors-configuration '{
  "CORSRules": [
    {
      "AllowedOrigins": ["*"],
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "MaxAgeSeconds": 3000
    }
  ]
}'
