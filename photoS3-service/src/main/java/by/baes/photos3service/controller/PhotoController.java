package by.baes.photos3service.controller;

import by.baes.photos3service.service.PhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/photos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PhotoController {
    private final PhotoService photoService;

    @Operation(summary = "Upload a photo", responses = {
            @ApiResponse(responseCode = "200", description = "Photo uploaded", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadPhoto(@RequestParam("file") MultipartFile file) {
        return photoService.uploadPhoto(file);
    }

    @Operation(summary = "Get a photo by file name", responses = {
            @ApiResponse(responseCode = "200", description = "Photo retrieved", content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "404", description = "Photo not found")
    })
    @GetMapping("/{fileName}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable String fileName) {
        byte[] data = photoService.getPhoto(fileName);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(data);
    }

    @Operation(summary = "Delete a photo by file name", responses = {
            @ApiResponse(responseCode = "200", description = "Photo deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid file name")
    })
    @DeleteMapping("/{fileName}")
    public void deletePhoto(@PathVariable String fileName) {
        photoService.deletePhoto(fileName);
    }
}