package com.sunset.rider.msvclabphoto.service;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.sunset.rider.msvclabphoto.model.Photo;
import com.sunset.rider.msvclabphoto.repository.PhotoRepository;
import com.sunset.rider.msvclabphoto.model.dto.Form;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.ByteBuffer;

@Service
@Slf4j
public class BlobService {

    @Autowired
    private PhotoService photoRepository;

    @Value("${spring.cloud.azure.storage.blob.container-name}")
    private String containerName;

    @Value("${azure.blob-storage.connection-string}")
    private String connectionString;

    private BlobServiceAsyncClient blobServiceClient;

    @PostConstruct
    public void init() {
        blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildAsyncClient();
    }

    /**
     * public String uploadImageWithCaption(Mono<FilePart> imageFile, String caption) throws IOException {
     * String blobFileName = imageFile.filename();
     * BlobClient blobClient = blobServiceClient.getBlobContainerClient(containerName).getBlobClient(blobFileName);
     * System.out.println(blobFileName);
     * <p>
     * byte[] result = imageFile.content().map(dataBuffer -> {
     * byte[] bytes = new byte[dataBuffer.readableByteCount()];
     * <p>
     * return bytes;
     * }).blockLast();
     * <p>
     * System.out.println(result);
     * InputStream targetStream = new ByteArrayInputStream(result);
     * blobClient.upload(targetStream);
     * String imageUrl = blobClient.getBlobUrl();
     * System.out.println(imageUrl);
     * Photo metadata = Photo.builder().hotelId("1").createdAt(LocalDateTime.now())
     * .flagMain(false).roomId("3").description("hola").url(imageUrl).build();
     * <p>
     * photoRepository.save(metadata);
     * <p>
     * return "Image and metadata uploaded successfully!";
     * }
     */
    public Mono<Photo> uploadImageWithCaption(Flux<FilePart> imageFile, Form form) throws IOException {

        return Mono.from(imageFile.map(filePart ->
                        {
                            BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(MediaType.IMAGE_JPEG_VALUE);
                            String blobFileName = "-."+filePart.filename() + form.getHotelId();
                            BlobAsyncClient blobClient = blobServiceClient.getBlobContainerAsyncClient(containerName)
                                    .getBlobAsyncClient(blobFileName);

                            Flux<ByteBuffer> fluxSave = filePart.content()
                                    .map(dataBuffer -> {
                                        ByteBuffer buffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
                                        dataBuffer.toByteBuffer(buffer);
                                        DataBufferUtils.release(dataBuffer);
                                        return buffer;

                                    });

                            blobClient.upload(fluxSave, new ParallelTransferOptions(), true)
                                    .block()
                            ;
                            blobClient.setHttpHeaders(headers).block();

                            return blobClient.getBlobUrl();
                        }
                )
                .flatMap(url -> {
                    Photo data = Photo.builder().hotelId(form.getHotelId())
                            .roomId(form.getRoomId())
                            .url(url)
                            .flagMain(Boolean.valueOf(form.getFlagMain())).build();
                    return Mono.just(data);
                }));


    }

    public void deleteImageWithCaption(String blobFileName){
        BlobAsyncClient blobClient = blobServiceClient.getBlobContainerAsyncClient(containerName)
                .getBlobAsyncClient(blobFileName);
        blobClient.deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE, null);
    }
}