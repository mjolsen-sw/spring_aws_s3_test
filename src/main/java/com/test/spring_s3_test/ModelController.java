package com.test.spring_s3_test;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@AllArgsConstructor
public class ModelController {
    private ModelService modelService;

    @GetMapping
    public List<ModelDTO> getModels() {
        return modelService.getAllModels();
    }

    @PostMapping
    public ModelDTO createModel(@RequestBody ModelDTO modelDTO) {
        return modelService.createModel(modelDTO);
    }

    @DeleteMapping("/{modelId}")
    public ModelDTO deleteModel(@PathVariable Long modelId) {
        return modelService.deleteModel(modelId);
    }

    @PutMapping("/image/{modelId}")
    public ModelDTO uploadImage(@PathVariable Long modelId, @RequestBody MultipartFile image) {
        return modelService.updateImage(modelId, image);
    }

    @DeleteMapping("/image/{modelId}")
    public ModelDTO deleteImage(@PathVariable Long modelId) {
        return modelService.deleteImage(modelId);
    }
}
