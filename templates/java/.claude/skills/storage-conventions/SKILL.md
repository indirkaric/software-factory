---
name: storage-conventions
description: Use when a service stores or serves files in AWS S3 or Azure Blob Storage (upload, download, delete, presigned URLs), or adds the storage dependencies and properties.
---

One interface, one implementation per provider, chosen by a property. Distilled from the
`data-storage` service, with its gaps fixed.

## Shape

```
service/storage/ICloudStorageService        upload(key, bytes, contentType), download(key),
                                             delete(key), presignedUrl(key, PresignedAccess, Duration)
service/storage/impl/S3StorageService        @ConditionalOnProperty(name = "storage.provider", havingValue = "aws")
service/storage/impl/AzureBlobStorageService @ConditionalOnProperty(..., havingValue = "azure")
config/StorageConfig                         the S3Client/S3Presigner or BlobServiceClient bean per provider
```

- AWS SDK **v2** (`software.amazon.awssdk:s3`, BOM `software.amazon.awssdk:bom`), not v1.
  Azure: `com.azure:azure-storage-blob` + `azure-identity` through `azure-sdk-bom`.
- Callers depend on `ICloudStorageService` only; no `if (provider == …)` anywhere else.
- Errors throw (`BusinessException` with a clear message); never return an error string as a URL.
- Presigned URLs always have an explicit expiry from `storage.presign-ttl` (default `PT15M`):
  PUT for upload, GET for download (Azure: SAS with write+create, or read).
- Object keys are built by the service (`<entityId>/<uuid>-<safeName>`), never taken raw from
  the client; the file name is sanitized.

## Properties (`application.yaml`, local defaults that work with the development repo)

```yaml
storage:
  provider: aws                       # aws | azure
  bucket: indir-local                 # S3 bucket or Azure container
  presign-ttl: PT15M
  aws:
    region: eu-west-1
    endpoint: http://aws.indir.local:4566   # LocalStack; empty in production
    access-key: test                         # LocalStack only; production uses the default chain
    secret-key: test
  azure:
    connection-string: ""             # Azurite: dev profile `azure` in the development repo
```

- With `storage.aws.endpoint` set: static credentials, `forcePathStyle(true)`, endpoint
  override. Without it: `DefaultCredentialsProvider` and the configured region (never hard-coded).
- Azure: connection string when set (Azurite), else `DefaultAzureCredential` + account URL.

## Tests

Unit-test each implementation against a mocked `S3Client`/`S3Presigner`/`BlobContainerClient`;
the factory's local check exercises LocalStack for real (bucket `indir-local`).
