#!/bin/bash
# LocalStack ready hook: the bucket every service uses locally.
awslocal s3 mb s3://indir-local
echo "S3 bucket indir-local created"
