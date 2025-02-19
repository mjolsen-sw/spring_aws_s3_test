package com.test.spring_s3_test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageDTO {
    private byte[] image;
    private HttpHeaders headers;
}
