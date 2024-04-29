package com.walking;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.nio.JpegWriter;
import com.sksamuel.scrimage.webp.WebpWriter;
import com.walking.api.ApiApp;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@ActiveProfiles(value = {"test"})
@SpringBootTest(classes = {ApiApp.class})
class PictureTest {

	@Value("${picture.path}")
	private String basePicturePath;

	void stopWatchLog(String title, StopWatch stopWatch) {
		log.info("=========================================");
		log.info("title: {}", title);
		log.info("stopWatch.getTime(): {} ms", stopWatch.getTime());
		log.info("=========================================");
	}

	void fileLog(String title, File file) throws IOException {
		log.info("=========================================");
		log.info("title: {}", title);
		log.info("file.getName(): {}", file.getName());
		log.info("file.getPath(): {}", file.getPath());
		log.info("file.size(): {} byte", file.length());
		log.info("file.extension: {}", FilenameUtils.getExtension(file.getName()));
		ImmutableImage image = ImmutableImage.loader().fromFile(file);
		int width = image.width;
		int height = image.height;
		log.info("file width: {}", width);
		log.info("file height: {}", height);
		log.info(
				"file.lastModified(): {}",
				LocalDateTime.ofEpochSecond(file.lastModified() / 1000, 0, ZoneOffset.UTC));
		log.info("=========================================");
	}

	void fileSizeChangeLog(File file, File output) {
		log.info("=========================================");
		log.info("origin file size: {} byte", file.length());
		log.info("output file size: {} byte", output.length());
		log.info(
				"change percent: {} %",
				(1 - Math.abs(output.length() - file.length()) / (double) file.length()) * 100);
		log.info("=========================================");
	}

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

	@ParameterizedTest(name = "pictureName={0}, pictureExtension={1}")
	@CsvSource({"image, jpg", "image, jpeg", "image, png"})
	void 파일정보를_확인합니다(String pictureName, String pictureExtension) throws IOException {
		// Given

		// When
		File file = makeFile(basePicturePath, pictureName, pictureExtension);

		// Then
		Assertions.assertTrue(file.exists());
		Assertions.assertEquals(combineDot(pictureName, pictureExtension), file.getName());
		Assertions.assertEquals(
				combinePath(basePicturePath, combineDot(pictureName, pictureExtension)), file.getPath());
		Assertions.assertEquals(basePicturePath, file.getParent());
	}

	@ParameterizedTest(name = "pictureName={0}, pictureExtension={1}")
	@CsvSource({"image, jpg", "image, jpeg", "image, png"})
	void 멀티파트_파일정보를_확인합니다(String pictureName, String pictureExtension) throws IOException {
		// Given
		File file = makeFile(basePicturePath, pictureName, pictureExtension);
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

		// When
		MockMultipartFile multipartFile =
				new MockMultipartFile(
						combineDot(pictureName, pictureExtension),
						pictureName,
						"image/" + pictureExtension,
						bis);

		// Then
		Assertions.assertEquals(pictureName, multipartFile.getOriginalFilename());
		Assertions.assertEquals(combineDot(pictureName, pictureExtension), multipartFile.getName());
		Assertions.assertEquals("image/" + pictureExtension, multipartFile.getContentType());
		Assertions.assertEquals(file.length(), multipartFile.getSize());
	}

	@ParameterizedTest(name = "pictureName={0}, pictureExtension={1}")
	@CsvSource({"image, jpg", "image, jpeg", "image, png"})
	void 멀티파트파일을_이동합니다(String pictureName, String pictureExtension) throws IOException {
		// Given
		File file = makeFile(basePicturePath, pictureName, pictureExtension);
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

		String testBasePicturePath = basePicturePath + "/result/copy";
		File dest = makeFile(testBasePicturePath, pictureName + "-copy", pictureExtension);

		// When
		MockMultipartFile multipartFile =
				new MockMultipartFile(
						combineDot(pictureName, pictureExtension),
						pictureName,
						"image/" + pictureExtension,
						bis);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		multipartFile.transferTo(dest);
		stopWatch.stop();
		stopWatchLog("multipartFile.transferTo(dest)", stopWatch);

		// Then
		Assertions.assertTrue(dest.exists());
		Assertions.assertEquals(combineDot(pictureName + "-copy", pictureExtension), dest.getName());
		Assertions.assertEquals(
				combinePath(testBasePicturePath, combineDot(pictureName + "-copy", pictureExtension)),
				dest.getPath());
		Assertions.assertEquals(testBasePicturePath, dest.getParent());
	}

	@ParameterizedTest(name = "pictureName={0}, pictureExtension={1}")
	@CsvSource({"image, jpg", "image, jpeg", "image, png", "image, heic"})
	void 파일을_webp형식으로_변경합니다(String pictureName, String pictureExtension) throws IOException {
		// Given
		File file = makeFile(basePicturePath, pictureName, pictureExtension);

		String testBasePicturePath = basePicturePath + "/result/webp";

		// When
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		File output =
				ImmutableImage.loader()
						.fromFile(file)
						.output(
								WebpWriter.DEFAULT,
								makeFile(testBasePicturePath, pictureExtension + "-" + pictureName, "webp"));
		stopWatch.stop();
		stopWatchLog("ImmutableImage.loader().fromFile(file).output(WebpWriter.DEFAULT)", stopWatch);

		// Then
		Assertions.assertTrue(output.exists());
		Assertions.assertEquals(
				combineDot(pictureExtension + "-" + pictureName, "webp"), output.getName());
		Assertions.assertEquals(
				combinePath(testBasePicturePath, combineDot(pictureExtension + "-" + pictureName, "webp")),
				output.getPath());
		Assertions.assertEquals(testBasePicturePath, output.getParent());
		fileLog("origin file", file);
		fileLog("output file", output);
		fileSizeChangeLog(file, output);
	}

	@Nested
	class ResizeTest {

		@ParameterizedTest(name = "width={0}, height={1}, pictureName={2}, pictureExtension={3}")
		@MethodSource("resizeArguments")
		void ImmutableImage로_파일을_리사이징합니다(
				int width, int height, String pictureName, String pictureExtension) throws IOException {
			// Given
			File file = makeFile(basePicturePath, pictureName, pictureExtension);
			String extension = FilenameUtils.getExtension(file.getName());
			String testBasePicturePath = basePicturePath + "/result/resize/immutableImage";

			// When
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			File output =
					ImmutableImage.loader()
							.fromFile(file)
							.scaleTo(width, height)
							.output(
									JpegWriter.Default,
									makeFile(
											testBasePicturePath, pictureName + "-" + width + "x" + height, extension));
			stopWatch.stop();
			stopWatchLog("ImmutableImage.loader().fromFile(file).scaleTo(width, height)", stopWatch);

			// Then
			Assertions.assertEquals(extension, FilenameUtils.getExtension(output.getName()));
			Assertions.assertTrue(output.exists());
			Assertions.assertEquals(
					combineDot(pictureName + "-" + width + "x" + height, extension), output.getName());
			Assertions.assertEquals(
					combinePath(
							testBasePicturePath, combineDot(pictureName + "-" + width + "x" + height, extension)),
					output.getPath());
			Assertions.assertEquals(testBasePicturePath, output.getParent());
			Assertions.assertEquals(width, ImmutableImage.loader().fromFile(output).width);
			Assertions.assertEquals(height, ImmutableImage.loader().fromFile(output).height);
			fileLog("origin file", file);
			fileLog("output file", output);
		}

		private static Stream<Arguments> resizeArguments() {
			List<Integer> widths = List.of(100, 200, 300, 400, 500);
			List<Integer> heights = List.of(100, 200, 300, 400, 500);
			String pictureName = "image";
			List<String> pictureExtensions = List.of("jpg", "jpeg", "png", "heic", "webp");
			List<Arguments> arguments = new ArrayList<>();
			for (int i = 0; i < widths.size(); i++) {
				for (String pictureExtension : pictureExtensions) {
					arguments.add(Arguments.of(widths.get(i), heights.get(i), pictureName, pictureExtension));
				}
			}
			return arguments.stream();
		}

		@ParameterizedTest(name = "width={0}, height={1}, pictureName={2}, pictureExtension={3}")
		@MethodSource("basicResizeArguments")
		void ImageIO로_파일을_리사이징합니다(int width, int height, String pictureName, String pictureExtension)
				throws IOException {
			// Given
			File file = makeFile(basePicturePath, pictureName, pictureExtension);
			String extension = FilenameUtils.getExtension(file.getName());
			String testBasePicturePath = basePicturePath + "/result/resize/imageIO";
			File output =
					makeFile(testBasePicturePath, pictureName + "-" + width + "x" + height, extension);

			// When
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			Image resizedImage =
					ImageIO.read(file).getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH);
			BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			outputImage.getGraphics().drawImage(resizedImage, 0, 0, null);
			ImageIO.write(outputImage, extension, output);
			stopWatch.stop();
			stopWatchLog(
					"ImageIO.read(file).getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH)",
					stopWatch);

			// Then
			Assertions.assertEquals(extension, FilenameUtils.getExtension(output.getName()));
			Assertions.assertTrue(output.exists());
			Assertions.assertEquals(
					combineDot(pictureName + "-" + width + "x" + height, extension), output.getName());
			Assertions.assertEquals(
					combinePath(
							testBasePicturePath, combineDot(pictureName + "-" + width + "x" + height, extension)),
					output.getPath());
			Assertions.assertEquals(testBasePicturePath, output.getParent());
			//		Assertions.assertEquals(width, ImmutableImage.loader().fromFile(output).width);
			Assertions.assertEquals(height, ImmutableImage.loader().fromFile(output).height);
			fileLog("origin file", file);
			fileLog("output file", output);
		}

		@ParameterizedTest(name = "width={0}, height={1}, pictureName={2}, pictureExtension={3}")
		@MethodSource("basicResizeArguments")
		void thumbnailator로_파일을_리사이징합니다(
				int width, int height, String pictureName, String pictureExtension) throws IOException {
			// Given
			File file = makeFile(basePicturePath, pictureName, pictureExtension);
			String extension = FilenameUtils.getExtension(file.getName());
			String testBasePicturePath = basePicturePath + "/result/resize/thumbnailator";
			File output =
					makeFile(testBasePicturePath, pictureName + "-" + width + "x" + height, extension);
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));

			// When
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			Thumbnails.of(file).size(width, height).outputFormat(extension).toOutputStream(bos);
			ImageIO.createImageOutputStream(bos).close();
			stopWatch.stop();
			stopWatchLog(
					"Thumbnails.of(file).size(width, height).outputFormat(extension).toFile(output)",
					stopWatch);

			// Then
			Assertions.assertEquals(extension, FilenameUtils.getExtension(output.getName()));
			Assertions.assertTrue(output.exists());
			Assertions.assertEquals(
					combineDot(pictureName + "-" + width + "x" + height, extension), output.getName());
			Assertions.assertEquals(
					combinePath(
							testBasePicturePath, combineDot(pictureName + "-" + width + "x" + height, extension)),
					output.getPath());
			Assertions.assertEquals(testBasePicturePath, output.getParent());
			Assertions.assertEquals(width, ImmutableImage.loader().fromFile(output).width);
			fileLog("origin file", file);
			fileLog("output file", output);
		}

		private static Stream<Arguments> basicResizeArguments() {
			List<Integer> widths = List.of(100, 200, 300, 400, 500);
			List<Integer> heights = List.of(100, 200, 300, 400, 500);
			String pictureName = "image";
			List<String> pictureExtensions = List.of("jpg", "jpeg", "png");
			List<Arguments> arguments = new ArrayList<>();
			for (int i = 0; i < widths.size(); i++) {
				for (String pictureExtension : pictureExtensions) {
					arguments.add(Arguments.of(widths.get(i), heights.get(i), pictureName, pictureExtension));
				}
			}
			return arguments.stream();
		}
	}
}
