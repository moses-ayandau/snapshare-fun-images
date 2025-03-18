package com.moses.fun.controller;

import com.moses.fun.model.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.moses.fun.service.S3Service;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
public class ImageController {

    private final S3Service s3Service;
    private static final int PAGE_SIZE = 12;

    @GetMapping
    public String index(@RequestParam(value = "page", defaultValue = "1") int page, Model model) {
        List<Image> images = s3Service.listImages(page, PAGE_SIZE);
        int totalImages = s3Service.countImages();
        int totalPages = (int) Math.ceil((double) totalImages / PAGE_SIZE);

        model.addAttribute("images", images);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", PAGE_SIZE);

        return "index";
    }

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file) {
        s3Service.uploadImage(file);
        return "redirect:/";
    }

    @DeleteMapping("/images/{key}")
    public ResponseEntity<Void> deleteImage(@PathVariable String key) {
        s3Service.deleteImage(key);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Application is up and  running on port 8080");
    }

    @GetMapping("/api/images")
    @ResponseBody
    public ResponseEntity<List<Image>> getImages(@RequestParam(value = "page", defaultValue = "1") int page) {
        List<Image> images = s3Service.listImages(page, PAGE_SIZE);
        return ResponseEntity.ok(images);
    }
}