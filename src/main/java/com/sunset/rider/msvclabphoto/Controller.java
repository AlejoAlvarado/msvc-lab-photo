package com.sunset.rider.msvclabphoto;


import com.sunset.rider.msvclabphoto.model.Photo;
import com.sunset.rider.msvclabphoto.service.BlobService;
import com.sunset.rider.msvclabphoto.model.dto.Form;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;


@RestController
@RequestMapping("/image-metadata")
public class Controller {

    @Autowired
    private BlobService blobService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Photo>> uploadImageWithCaption(@RequestPart("file") Flux<FilePart> file,
                                                              @ModelAttribute Form form) {
        try {

            return
                    blobService.uploadImageWithCaption(file,form).map(mono -> ResponseEntity.ok().body(mono));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}