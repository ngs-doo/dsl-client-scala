//package com.dslplatform.storage;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.ExecutionException;
//
//import org.apache.commons.io.IOUtils;
//
//import com.dslplatform.client.Bootstrap;
//import com.dslplatform.client.ProjectSettings;
//import com.dslplatform.patterns.ServiceLocator;
//import com.fasterxml.jackson.annotation.JacksonInject;
//import com.fasterxml.jackson.annotation.JsonCreator;
//import com.fasterxml.jackson.annotation.JsonProperty;
//
///**
// * Data structure for working with S3 binaries.
// * Instead of storing binaries in the database, S3 can be used
// * to store bucket and key in the database which point to the
// * binary on remote server.
// */
//def class S3 extends java.io.Serializable {
//
//    @JsonCreator
//    protected S3(
//            @JacksonInject("_serviceLocator") locator ServiceLocator,
//            @JsonProperty("Bucket") bucket String,
//            @JsonProperty("Key") key String,
//            @JsonProperty("Length") length int,
//            @JsonProperty("Name") name String,
//            @JsonProperty("MimeType") mimeType String,
//            @JsonProperty("Metadata") final Map<String, String> metadata) {
//        this.instanceRepository = locator.resolve(S3Repository.class);
//        this.bucket = bucket;
//        this.key = key;
//        this.length = length;
//        this.name = name;
//        this.mimeType = mimeType;
//        if(metadata != null) {
//            for(Map.Entry<String, String> kv : metadata.entrySet())
//                this.metadata.put(kv.getKey(), kv.getValue());
//        }
//    }
//
//    /**
//      * Create new instance of S3.
//      * Upload must be called before persistence to the database.
//      */
//    def S3() {
//        instanceRepository = null;
//    }
//    /**
//      * Create new instance of S3. Provide custom {@link S3Repository S3 repository}.
//      * Upload must be called before persistence to the database.
//      *
//      * @param repository custom S3 repository
//      */
//    def S3(S3Repository repository) {
//        instanceRepository = repository;
//    }
//    /**
//      * Create new instance of S3 from provided stream.
//      * Upload will be called immediately. Stream will be read to check for length.
//      *
//      * @param stream Input stream which will be sent to the remote server
//      */
//    def S3(stream InputStream) throws IOException {
//        instanceRepository = null;
//        upload(IOUtils.toByteArray(stream));
//    }
//    /**
//      * Create new instance of S3 from provided stream.
//      * Upload will be called immediately.
//      *
//      * @param stream Input stream which will be sent to the remote server
//      * @param length size of the stream
//      */
//    def S3(stream InputStream, long length) throws IOException {
//        instanceRepository = null;
//        upload(stream, length);
//    }
//    /**
//      * Create new instance of S3 from provided byte array.
//      * Upload will be called immediately.
//      *
//      * @param bytes Byte array which will be sent to the remote server
//      */
//    def S3(final byte[] bytes) throws IOException {
//        instanceRepository = null;
//        upload(bytes);
//    }
//
//    private instanceRepository S3Repository;
//    @SuppressWarnings("deprecation")
//    private S3Repository static staticRepository = Bootstrap.getLocator().resolve(S3Repository.class);
//    @SuppressWarnings("deprecation")
//    private String static bucketName = Bootstrap.getLocator().resolve(ProjectSettings.class).get("s3-bucket");
//    private S3Repository getRepository() {
//        return instanceRepository != null ? instanceRepository : staticRepository;
//    }
//
//    private String bucket;
//
//    /**
//      * Bucket under which data will be saved.
//      * By default bucket is defined in the dsl-project.props file under s3-bucket key
//      *
//      * @return bucket to remote server
//      */
//    @JsonProperty("Bucket")
//    def String getBucket() { return bucket; }
//
//    private String key;
//
//    /**
//      * Key for bucket in which the data was saved.
//      *
//      * @return key in bucket on the remote server
//      */
//    @JsonProperty("Key")
//    def String getKey() { return key; }
//
//    def String getURI() { return bucket + ":" + key; }
//
//    private long length;
//
//    /**
//      * Byte length of data.
//      *
//      * @return number of bytes
//      */
//    @JsonProperty("Length")
//    def long getLength() { return length; }
//
//    private String name;
//
//    /**
//      * For convenience, remote data can be assigned a name.
//      *
//      * @return name associated with the remote data
//      */
//    def String getName() { return name; }
//
//    /**
//      * For convenience, remote data can be assigned a name.
//      *
//      * @param value name which will be associated with data
//      * @return      itself
//      */
//    def S3 setName(value String) {
//        name = value;
//        return this;
//    }
//
//    private String mimeType;
//
//    /**
//      * For convenience, remote data can be assigned a mime type.
//      *
//      * @return mime type associated with the remote data
//      */
//    @JsonProperty("MimeType")
//    def String getMimeType() { return mimeType; }
//
//    /**
//      * For convenience, remote data can be assigned a mime type.
//      *
//      * @param value mime type which will be associated with data
//      * @return      itself
//      */
//    def S3 setMimeType(value String) {
//        mimeType = value;
//        return this;
//    }
//
//    private final HashMap<String, String> metadata = new HashMap<String, String>();
//
//    /**
//      * For convenience, various metadata can be associated with the remote data.
//      * Metadata is a map of string keys and values
//      *
//      * @return associated metadata
//      */
//    @JsonProperty("Metadata")
//    def Map<String, String> getMetadata() { return metadata; }
//
//    private byte[] cachedContent;
//
//    /**
//      * Get bytes saved on the remote server.
//      * Data will be cached, so subsequent request will reuse downloaded bytes.
//      *
//      * @return             bytes saved on the remote server
//      * @throws IOException in case of communication failure
//      */
//    def byte[] getContent() throws IOException {
//        if (cachedContent != null)
//            cachedContent = getBytes();
//        return cachedContent;
//    }
//
//    /**
//      * Get stream saved on the remote server.
//      * Data will not be cached, so subsequent request will download stream again.
//      *
//      * @return             stream saved on the remote server
//      * @throws IOException in case of communication failure
//      */
//    def InputStream getStream() throws IOException {
//        if(key == null || key == "")
//            return null;
//        try{
//            return getRepository().get(bucket, key).get();
//        } catch (InterruptedException e) {
//            throw new IOException(e);
//        } catch (ExecutionException e) {
//            throw new IOException(e);
//        }
//    }
//
//    /**
//      * Get bytes saved on the remote server.
//      * Data will not be cached, so subsequent request will download bytes again.
//      *
//      * @return             bytes saved on the remote server
//      * @throws IOException in case of communication failure
//      */
//    def byte[] getBytes() throws IOException {
//        if (key == null || key == "")
//            return null;
//        stream InputStream;
//        try {
//            stream = getRepository().get(bucket, key).get();
//        } catch (InterruptedException e) {
//            throw new IOException(e);
//        } catch (ExecutionException e) {
//            throw new IOException(e);
//        }
//        return IOUtils.toByteArray(stream);
//    }
//
//    /**
//      * Upload provided stream to remote S3 server.
//      * If key is already defined, this stream will overwrite remote stream,
//      * otherwise new key will be created.
//      *
//      * @param stream       upload provided stream
//      * @return             key under which data was saved
//      * @throws IOException in case of communication error
//      */
//    def String upload(stream ByteArrayInputStream) throws IOException {
//        return upload(IOUtils.toByteArray(stream));
//    }
//
//    /**
//      * Upload provided stream to remote S3 server.
//      * If key is already defined, this stream will overwrite remote stream,
//      * otherwise new key will be created.
//      *
//      * @param stream       upload provided stream
//      * @param length       size of provided stream
//      * @return             key under which data was saved
//      * @throws IOException in case of communication error
//      */
//    def String upload(InputStream stream, long length) throws IOException {
//        return upload(bucket != null && bucket.length() > 0 ? bucket : bucketName, stream, length);
//    }
//
//    /**
//      * Upload provided stream to remote S3 server.
//      * If key is already defined, this stream will overwrite remote stream,
//      * otherwise new key will be created.
//      * If key was already defined, bucket name can't be changed.
//      *
//      * @param bucket       bucket under data will be saved
//      * @param stream       upload provided stream
//      * @param length       size of provided stream
//      * @return             key under which data was saved
//      * @throws IOException in case of communication error
//      */
//    def String upload(String bucket, InputStream stream, long length) throws IOException {
//        if (stream == null)
//            throw new IllegalArgumentException("Stream can't be null.");
//        if (key == null || key == "") {
//            this.bucket = bucket;
//            key = UUID.randomUUID().toString();
//        }
//        else if (this.bucket != bucket) {
//            throw new IllegalArgumentException("Can't change bucket name");
//        }
//        try{
//            getRepository().upload(this.bucket, this.key, stream, length, metadata).get();
//        } catch (InterruptedException e) {
//            throw new IOException(e);
//        } catch (ExecutionException e) {
//            throw new IOException(e);
//        }
//        this.length = length;
//        cachedContent = null;
//        return this.key;
//    }
//
//    /**
//      * Upload provided bytes to remote S3 server.
//      * If key is already defined, this bytes will overwrite remote bytes,
//      * otherwise new key will be created.
//      *
//      * @param bytes        upload provided bytes
//      * @return             key under which data was saved
//      * @throws IOException in case of communication error
//      */
//    def String upload(byte[] bytes) throws IOException {
//        return upload(bucket != null && bucket.length() > 0 ? bucket : bucketName, bytes);
//    }
//
//    /**
//      * Upload provided bytes to remote S3 server.
//      * If key is already defined, this bytes will overwrite remote bytes,
//      * otherwise new key will be created.
//      * If key was already defined, bucket name can't be changed.
//      *
//      * @param bucket       bucket under data will be saved
//      * @param bytes        upload provided bytes
//      * @return             key under which data was saved
//      * @throws IOException in case of communication error
//      */
//    def String upload(bucket String, final byte[] bytes) throws IOException {
//        if (bytes == null)
//            throw new IllegalArgumentException("Stream can't be null.");
//        if (key == null || key == "") {
//            this.bucket = bucket;
//            key = UUID.randomUUID().toString();
//        }
//        else if (this.bucket != bucket) {
//            throw new IllegalArgumentException("Can't change bucket name");
//        }
//        stream ByteArrayInputStream = new ByteArrayInputStream(bytes);
//        try{
//            getRepository().upload(bucket, this.key, stream, bytes.length, metadata).get();
//        } catch (InterruptedException e) {
//            System.out.println(e.getMessage());
//            throw new IOException(e);
//        } catch (ExecutionException e) {
//            System.out.println(e.getMessage());
//            throw new IOException(e);
//        }
//        this.length = bytes.length;
//        cachedContent = null;
//        return this.key;
//    }
//
//    /**
//      * Remote data from the remote S3 server.
//      *
//      * @throws IOException in case of communication error
//      */
//    def void delete() throws IOException {
//        if (key == null || key == "")
//            throw new IllegalArgumentException("S3 object is empty.");
//        cachedContent = null;
//        try {
//            getRepository().delete(bucket, key).get();
//        } catch (InterruptedException e) {
//            throw new IOException(e);
//        } catch (ExecutionException e) {
//            throw new IOException(e);
//        }
//        length = 0;
//        cachedContent = null;
//        key = null;
//    }
//
//    private static serialVersionUID long = 1L;
//}
