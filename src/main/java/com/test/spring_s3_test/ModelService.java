package com.test.spring_s3_test;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ModelService {
    @Autowired
    private ModelRepository modelRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Value("${spring.application.s3.bucket}")
    private String bucket;

    @Value("${spring.application.s3.region}")
    private String region;

    private static final Logger logger = LoggerFactory.getLogger(ModelService.class);

    public List<ModelDTO> getAllModels() {
        return modelRepository.findAll().stream()
                .map(model -> modelMapper.map(model, ModelDTO.class))
                .toList();
    }

    public ModelDTO createModel(ModelDTO modelDTO) {
        Model model = modelMapper.map(modelDTO, Model.class);
        model.setBucket(bucket);
        model.setKey("default.png");
        model.setUrl("https://" + bucket + ".s3." + region + ".amazonaws.com/default.jpg");

        Model savedModel = modelRepository.save(model);
        return modelMapper.map(savedModel, ModelDTO.class);
    }

    public ModelDTO updateImage(Long modelId, MultipartFile image) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found"));

        boolean urlChanged = false;

        if (model.getKey().equals("default.png")) {
            String originalFilename = image.getOriginalFilename();
            String randomId = UUID.randomUUID().toString();

            String filename = randomId.concat(originalFilename.substring(originalFilename.lastIndexOf(".")));
            model.setKey(filename);

            String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + filename;
            model.setUrl(url);

            urlChanged = true;
            logger.debug("url changed: {}", url);
        }

        try (S3Client s3Client = S3Client.create()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(model.getBucket())
                    .key(model.getKey())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(image.getBytes()));
            logger.debug("File uploaded: {}", model.getUrl());

            if (urlChanged) {
                model = modelRepository.save(model);
            }
        } catch (IOException e) {
            logger.error("Error reading file: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error uploading file to S3: {}", e.getMessage());
        }

        return modelMapper.map(model, ModelDTO.class);
    }
}
