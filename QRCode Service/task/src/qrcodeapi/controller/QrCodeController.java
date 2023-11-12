package qrcodeapi.controller;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import qrcodeapi.model.*;

@RestController
public class QrCodeController {

    final String WRONG_DIMENSION_JSON = """
            {
                "error": "Image size must be between 150 and 350 pixels"
            }""";
    final String WRONG_TYPE_JSON = """
            {
                "error": "Only png, jpeg and gif image types are supported"
            }""";
    final String WRONG_CONTENT_JSON = """
            {
                 "error": "Contents cannot be null or blank"
            }""";
    final String WRONG_CORRECTION_JSON = """
            {
                "error": "Permitted error correction levels are L, M, Q, H"
            }""";

    final String availableCorrectionLevels = "LHMQ";
    final List<MediaType> availableMediaTypes = List.of(
            MediaType.IMAGE_PNG,
            MediaType.IMAGE_JPEG,
            MediaType.IMAGE_GIF
    );

    final Dimension maxSize = new Dimension(350, 350);
    final Dimension minSize = new Dimension(150, 150);

    Logger logger = LoggerFactory.getLogger(QrCodeController.class);

    @GetMapping(path = "/api/qrcode")
    public ResponseEntity<byte[]> getImage(

            @RequestParam()
            String contents,

            @RequestParam(required = false, defaultValue = "250")
            String size,

            @RequestParam(required = false, defaultValue = "png")
            String type,

            @RequestParam(required = false, defaultValue = "L")
            String correction

    ) {


        if (size == null &&
                type == null &&
                contents == null &&
                correction == null
        ) {
            logger.info("No Params Passed");
            return ResponseEntity.ok(new byte[]{});
        }

        logger.info(size);
        assert size != null;
        int dimension = Integer.parseInt(size);

        if (contents == null || contents.isBlank())
            return badRequest(WRONG_CONTENT_JSON);

        if (dimension < minSize.width || dimension > maxSize.width)
            return badRequest(WRONG_DIMENSION_JSON);

        if (!availableCorrectionLevels.contains(correction)) {
            return badRequest(WRONG_CORRECTION_JSON);
        }




        if (!type.contains("/")) {
            logger.info("!type.contains(/)");
            type = "image/" + type;
        }
        if (!availableMediaTypes.contains(MediaType.valueOf(type)))
            return badRequest(WRONG_TYPE_JSON);


        BufferedImage bufferedImage = QrCodeCreator.create(
                contents,
                dimension,
                ErrorCorrectionLevel.valueOf(correction)
        );

        String imageType = type.substring(6);
        logger.info(imageType);

        try (var baos = new ByteArrayOutputStream()) {
            assert bufferedImage != null;
            logger.info(bufferedImage.toString());
            ImageIO.write(bufferedImage, type.substring(6), baos);
            byte[] bytes = baos.toByteArray();

            logger.info(Arrays.toString(bytes));
            logger.info("Trying to Create a new image: " +
                    " size: " + size +
                    " type: " + type +
                    " correction: " + correction
            );

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.valueOf(type))
                    .body(bytes);
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }

        return ResponseEntity.status(400).build();
    }

    public ResponseEntity<byte[]> badRequest(String errorMessage) {
        return ResponseEntity.status(400).body(errorMessage.getBytes());
    }

}
