package com.test.spring_s3_test;

import org.apache.tika.Tika;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ModelService {

    private ModelRepository modelRepository;
    private ModelMapper modelMapper;
    private Tika tika;

    @Value("${spring.application.s3.bucket}")
    private String bucket;

    @Value("${spring.application.s3.region}")
    private String region;

    private final String DEFAULT_KEY = "default.jpg";
    private static final Logger logger = LoggerFactory.getLogger(ModelService.class);

    public ModelService(ModelRepository modelRepository, ModelMapper modelMapper, Tika tika) {
        this.modelRepository = modelRepository;
        this.modelMapper = modelMapper;
        this.tika = tika;
    }

    public List<ModelDTO> getAllModels() {
        return modelRepository.findAll().stream()
                .map(model -> modelMapper.map(model, ModelDTO.class))
                .toList();
    }

    public ModelDTO createModel(ModelDTO modelDTO) {
        Model model = modelMapper.map(modelDTO, Model.class);
        model.setBucket(bucket);
        model.setKey(DEFAULT_KEY);
        model.setUrl("https://" + bucket + ".s3." + region + ".amazonaws.com/" + DEFAULT_KEY);

        Model savedModel = modelRepository.save(model);
        return modelMapper.map(savedModel, ModelDTO.class);
    }

    public ModelDTO deleteModel(Long modelId) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found"));

        if (!model.getKey().equals(DEFAULT_KEY)) {
            logger.debug("Deleting model with key {}", model.getKey());
            deleteS3Object(model);
        }

        modelRepository.delete(model);
        return modelMapper.map(model, ModelDTO.class);
    }

    public ModelDTO updateImage(Long modelId, MultipartFile image) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found"));

        if (isNotImageFile(image)) {
            throw new RuntimeException("File is not an image");
        }

        boolean urlChanged = false;

        if (model.getKey().equals(DEFAULT_KEY)) {
            String originalFilename = image.getOriginalFilename();
            if (originalFilename == null) {
                throw new RuntimeException("Filename is null");
            }

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
        } catch (S3Exception e) {
            logger.error("AWS S3 error while updating: {}", e.awsErrorDetails().errorMessage());
        } catch (SdkClientException e) {
            logger.error("Client-side error while updating image: {}", e.getMessage());
        } catch (IOException e) {
            logger.error("Error reading file: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error uploading file to S3: {}", e.getMessage());
        }

        if (urlChanged) {
            model = modelRepository.save(model);
        }

        return modelMapper.map(model, ModelDTO.class);
    }

    public ModelDTO deleteImage(Long modelId) {
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found"));

        if (model.getKey().equals(DEFAULT_KEY)) {
            throw new RuntimeException("Cannot delete default image");
        }

        deleteS3Object(model);

        model.setKey(DEFAULT_KEY);
        String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + DEFAULT_KEY;
        model.setUrl(url);
        Model savedModel = modelRepository.save(model);

        return modelMapper.map(savedModel, ModelDTO.class);
    }

    private boolean isNotImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            return true;
        }

        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            return false;
        }

        try {
            String detectedType = tika.detect(file.getInputStream());
            return !detectedType.startsWith("image/");
        } catch (IOException e) {
            logger.error("Tika error reading file: {}", e.getMessage());
            return true;
        }
    }

    private void deleteS3Object(Model model) {
        try (S3Client s3Client = S3Client.create()) {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(model.getBucket())
                    .key(model.getKey())
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            logger.debug("Object deleted: {}", model.getKey());
        } catch (S3Exception e) {
            logger.error("AWS S3 error while deleting: {}", e.getMessage());
        } catch (SdkClientException e) {
            logger.error("Client-side error while deleting image: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error deleting object: {}", e.getMessage());
        }
    }
}
