/*
package com.dslplatform.api.client

class AmazonS3Repository extends S3Repository {

    private s3AccessKey String;
    private s3SecretKey String;
    private executorService ExecutorService;

    private AmazonS3Client s3Client;
    private AmazonS3Client getS3Client() throws IOException {
        if(s3Client == null) {
            if(s3AccessKey == null || s3AccessKey == "")
                throw new IOException("S3 configuration is missing. Please add s3-user");
            if(s3SecretKey == null || s3SecretKey == "")
                throw new IOException("S3 configuration is missing. Please add s3-secret");
            s3Client = new AmazonS3Client(new BasicAWSCredentials(s3AccessKey, s3SecretKey));
        }
        return s3Client;
    }

    def AmazonS3Repository(
            settings ProjectSettings,
            executorService ExecutorService) {
        this.s3AccessKey = settings.get("s3-user");
        this.s3SecretKey = settings.get("s3-secret");
        this.executorService = executorService;
    }

    private void checkBucket(name String) throws IOException {
        if(name == null || name == "")
            throw new IOException("Bucket not specified. If you wish to use default bucket name, add it as s3-bucket to dsl-project.props");
    }

    @Override
    def Future[InputStream] get(bucket String, key String) {
        return
            executorService.submit(new Callable[InputStream]() {
                @Override
                def InputStream call() throws IOException {
                    s3 S3Object = getS3Client().getObject(new GetObjectRequest(bucket, key));
                    return s3.getObjectContent();
                }
            });
    }

    @Override
    def Future<?> upload(
            bucket String,
            key String,
            stream InputStream,
            length long,
            final Map<String, String> metadata) {
        return
           executorService.submit(new Callable[Object]() {
                @Override
                def Object call() throws IOException {
                    checkBucket(bucket);
                    om ObjectMetadata = new ObjectMetadata();
                    om.setContentLength(length);
                    if(metadata != null)
                        for(final Map.Entry<String, String> kv : metadata.entrySet())
                            om.addUserMetadata(kv.getKey(), kv.getValue());
                    getS3Client().putObject(new PutObjectRequest(bucket, key, stream, om));
                    return null;
                }
            });
    }

    @Override
    def Future<?> delete(bucket String, key String) {
        return
            executorService.submit(new Callable[Object]() {
                @Override
                def Object call() throws IOException {
                    getS3Client().deleteObject(new DeleteObjectRequest(bucket, key));
                    return null;
                }
            });
    }
}
*/
