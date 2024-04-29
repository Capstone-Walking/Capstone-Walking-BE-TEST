package com.walking;

import com.walking.api.ApiApp;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@ActiveProfiles(value = {"test"})
@SpringBootTest(classes = {ApiApp.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MinioTest {

	@Value("${picture.path}")
	private String basePicturePath;

	@Value("${minio.bucket-name}")
	private String minioBucket;

	File makeFile(String path, String pictureName, String pictureExtension) {
		String picture = combineDot(pictureName, pictureExtension);
		File directory = new File(path);
		if (!directory.exists()) {
			directory.mkdirs();
		}
		String testPicturePath = combinePath(path, picture);
		return new File(testPicturePath);
	}

	String combinePath(String s1, String s2) {
		return s1 + "/" + s2;
	}

	String combineDot(String s1, String s2) {
		return s1 + "." + s2;
	}

	private final MinioClient minioClient;

	public MinioTest(
			@Value("${minio.url}") String minioUrl,
			@Value("${minio.access-key}") String minioAccessKey,
			@Value("${minio.secret-key}") String minioSecretKey) {

		this.minioClient =
				MinioClient.builder()
						.endpoint(minioUrl)
						.credentials(minioAccessKey, minioSecretKey)
						.build();
	}

	@Test
	@Order(1)
	public void 버킷을_생성합니다() {
		try {
			BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(minioBucket).build();
			boolean found = minioClient.bucketExists(bucketExistsArgs);
			if (!found) {
				MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder().bucket(minioBucket).build();
				minioClient.makeBucket(makeBucketArgs);
				log.info("test bucket created successfully");
			} else {
				log.info("test bucket already exists");
			}
		} catch (Exception e) {
			log.error("Error occurred: " + e);
		}
	}

	@Test
	@Order(2)
	public void 파일을_업로드합니다() {
		File file = makeFile(basePicturePath, "image", "jpg");
		try {
			PutObjectArgs putObjectArgs =
					PutObjectArgs.builder().bucket(minioBucket).object("test-image").stream(
									new BufferedInputStream(new FileInputStream(file)), file.length(), -1)
							.contentType("image/jpg")
							.build();
			ObjectWriteResponse objectWriteResponse = minioClient.putObject(putObjectArgs);
			log.info("test.jpg is uploaded successfully");
			log.info("etag: " + objectWriteResponse.etag());
			log.info("versionId: " + objectWriteResponse.versionId());
		} catch (Exception e) {
			log.error("Error occurred: " + e);
		}
	}

	@Test
	@Order(3)
	public void 파일을_다운로드합니다() {
		File file = makeFile(combinePath(basePicturePath, "result/download"), "download-image", "jpg");
		try {
			GetObjectArgs getObjectArgs =
					GetObjectArgs.builder().bucket(minioBucket).object("test-image").build();
			GetObjectResponse getObjectResponse = minioClient.getObject(getObjectArgs);
			log.info("test.jpg is downloaded successfully");
			Headers headers = getObjectResponse.headers();
			log.info("headers: " + headers);
			String bucket = getObjectResponse.bucket();
			log.info("bucket: " + bucket);
			String region = getObjectResponse.region();
			log.info("region: " + region);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			int read;
			byte[] bytes = new byte[1024];
			while ((read = getObjectResponse.read(bytes)) != -1) {
				bos.write(bytes, 0, read);
			}
		} catch (Exception e) {
			log.error("Error occurred: " + e);
		}
	}

	@Test
	@Order(4)
	public void 파일의_메타데이터를_조회합니다() {
		try {
			StatObjectArgs statObjectArgs =
					StatObjectArgs.builder().bucket(minioBucket).object("test-image").build();
			StatObjectResponse statObjectResponse = minioClient.statObject(statObjectArgs);
			log.info("test.jpg metadata is retrieved successfully");
			log.info("etag: " + statObjectResponse.etag());
			log.info("size: " + statObjectResponse.size());
			log.info("lastModified: " + statObjectResponse.lastModified());
			log.info("retentionMode: " + statObjectResponse.retentionMode());
			log.info("retentionRetainUntilDate: " + statObjectResponse.retentionRetainUntilDate());
			log.info("status: " + statObjectResponse.legalHold().status());
			log.info("userMetadata: " + statObjectResponse.userMetadata());
		} catch (Exception e) {
			log.error("Error occurred: " + e);
		}
	}

	@Test
	@Order(5)
	void 파일의_사용_Url을_발급합니다() {
		try {
			GetPresignedObjectUrlArgs getPresignedObjectUrlArgs =
					GetPresignedObjectUrlArgs.builder()
							.bucket(minioBucket)
							.object("test-image")
							.method(Method.GET)
							.build();
			String presignedObjectUrl = minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
			log.info("presignedObjectUrl: " + presignedObjectUrl);
		} catch (Exception e) {
			log.error("Error occurred: " + e);
		}
	}

	@Disabled
	@Test
	public void 파일을_삭제합니다() {
		try {
			RemoveObjectArgs removeObjectArgs =
					RemoveObjectArgs.builder().bucket(minioBucket).object("test-image").build();
			minioClient.removeObject(removeObjectArgs);
			log.info("test.jpg is deleted successfully");
		} catch (Exception e) {
			log.error("Error occurred: " + e);
		}
	}

	@Disabled
	@Test
	public void 버킷을_삭제합니다() {
		try {
			RemoveBucketArgs removeBucketArgs = RemoveBucketArgs.builder().bucket(minioBucket).build();
			minioClient.removeBucket(removeBucketArgs);
			log.info("test bucket is deleted successfully");
		} catch (Exception e) {
			log.error("Error occurred: " + e);
		}
	}
}
