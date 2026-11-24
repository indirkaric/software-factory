#!/bin/bash
# LocalStack ready hook: the bucket every service uses locally.
awslocal s3 mb s3://factory-local
echo "S3 bucket factory-local created"
