package com.moses.fun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    private String key;
    private String fileName;
    private String url;
    private long size;
    private Instant lastModified;
}