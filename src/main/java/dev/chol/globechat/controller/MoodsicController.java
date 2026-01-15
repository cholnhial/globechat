package dev.chol.globechat.controller;

import dev.chol.globechat.dto.MoodsicDto;
import dev.chol.globechat.service.MoodsicService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * REST controller for moodsic endpoints.
 */
@RestController
@RequestMapping("/api/moodsics")
@RequiredArgsConstructor
public class MoodsicController {

    private final MoodsicService moodsicService;

    @PostMapping
    public ResponseEntity<MoodsicDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "isPublic", defaultValue = "false") boolean isPublic) {
        MoodsicDto moodsic = moodsicService.upload(file, name, isPublic);
        return ResponseEntity.status(HttpStatus.CREATED).body(moodsic);
    }

    @GetMapping
    public ResponseEntity<List<MoodsicDto>> listAvailable() {
        List<MoodsicDto> moodsics = moodsicService.listAvailable();
        return ResponseEntity.ok(moodsics);
    }

    @GetMapping("/public")
    public ResponseEntity<List<MoodsicDto>> listPublic() {
        List<MoodsicDto> moodsics = moodsicService.listPublic();
        return ResponseEntity.ok(moodsics);
    }

    @GetMapping("/my")
    public ResponseEntity<List<MoodsicDto>> listMyMoodsics() {
        List<MoodsicDto> moodsics = moodsicService.listMyMoodsics();
        return ResponseEntity.ok(moodsics);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MoodsicDto> getById(@PathVariable Long id) {
        MoodsicDto moodsic = moodsicService.getById(id);
        return ResponseEntity.ok(moodsic);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MoodsicDto>> search(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "all") String filter) {
        List<MoodsicDto> moodsics = moodsicService.search(query, filter);
        return ResponseEntity.ok(moodsics);
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> stream(@PathVariable Long id) throws IOException {
        MoodsicService.MoodsicFileInfo fileInfo = moodsicService.getFileInfo(id);
        
        long fileSize = Files.size(fileInfo.path());
        InputStreamResource resource = new InputStreamResource(Files.newInputStream(fileInfo.path()));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(fileInfo.contentType()));
        headers.setContentLength(fileSize);
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<MoodsicDto> toggleVisibility(@PathVariable Long id) {
        MoodsicDto moodsic = moodsicService.toggleVisibility(id);
        return ResponseEntity.ok(moodsic);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        moodsicService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
