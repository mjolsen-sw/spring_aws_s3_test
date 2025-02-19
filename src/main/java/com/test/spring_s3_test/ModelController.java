package com.test.spring_s3_test;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@AllArgsConstructor
public class ModelController {
    private ModelService modelService;

    @GetMapping
    public ResponseEntity<List<ModelDTO>> getModels() {
        return new ResponseEntity<>(modelService.getAllModels(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ModelDTO> createModel(@RequestBody ModelDTO modelDTO) {
    return new ResponseEntity<>(modelService.createModel(modelDTO), HttpStatus.CREATED);
    }

    @DeleteMapping("/{modelId}")
    public ResponseEntity<ModelDTO> deleteModel(@PathVariable Long modelId) {
        return new ResponseEntity<>(modelService.deleteModel(modelId), HttpStatus.OK);
    }

    @GetMapping("/image/{modelId}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long modelId) {
        Optional<ImageDTO> imageOption = modelService.getImage(modelId);
        if (imageOption.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        ImageDTO imageDTO = imageOption.get();
        return new ResponseEntity<>(imageDTO.getImage(), imageDTO.getHeaders(), HttpStatus.OK);
    }

    @PutMapping("/image/{modelId}")
    public ResponseEntity<ModelDTO> uploadImage(@PathVariable Long modelId, @RequestBody MultipartFile image) {
        return new ResponseEntity<>(modelService.updateImage(modelId, image), HttpStatus.OK);
    }

    @DeleteMapping("/image/{modelId}")
    public ResponseEntity<ModelDTO> deleteImage(@PathVariable Long modelId) {
        return new ResponseEntity<>(modelService.deleteImage(modelId), HttpStatus.OK);
    }
}
