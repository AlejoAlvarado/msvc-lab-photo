package com.sunset.rider.msvclabphoto.handler;

import com.sunset.rider.msvclabphoto.model.Photo;
import com.sunset.rider.msvclabphoto.model.dto.Form;
import com.sunset.rider.msvclabphoto.model.request.MainPhotosRequest;
import com.sunset.rider.msvclabphoto.model.request.PhotoRequest;
import com.sunset.rider.msvclabphoto.service.BlobService;
import com.sunset.rider.msvclabphoto.service.PhotoService;
import com.sunset.rider.msvclabphoto.utils.ErrorGeneric;
import com.sunset.rider.msvclabphoto.utils.ErrorNotFound;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
public class PhotoHandler {
  private PhotoService photoService;
  private Validator validator;
  @Autowired private BlobService blobService;

  public PhotoHandler(PhotoService photoService, Validator validator) {
    this.validator = validator;
    this.photoService = photoService;
  }

  public Mono<ServerResponse> findAll(ServerRequest serverRequest) {
    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(photoService.findAll(), Photo.class);
  }

  public Mono<ServerResponse> findById(ServerRequest serverRequest) {
    String id = serverRequest.pathVariable("id");

    return photoService
        .findById(id)
        .flatMap(
            photo ->
                ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(photo)))
        .switchIfEmpty(
            ServerResponse.status(HttpStatus.NOT_FOUND)
                .body(BodyInserters.fromValue(ErrorNotFound.error(id))));
  }

  public Mono<ServerResponse> findByRoomId(ServerRequest serverRequest) {
    String roomId = serverRequest.pathVariable("roomId");

    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(photoService.findByRoomId(roomId), Photo.class);
  }

  public Mono<ServerResponse> findByHotelId(ServerRequest serverRequest) {
    String hotelId = serverRequest.pathVariable("hotelId");

    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(photoService.findByHotelId(hotelId), Photo.class);
  }

  public Mono<ServerResponse> findHotelMainPhoto(ServerRequest serverRequest) {
    String hotelId = serverRequest.pathVariable("hotelId");

    return ServerResponse.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(photoService.findHotelMainPhoto(hotelId), Photo.class);
  }

  public Mono<ServerResponse> findHotelListMainPhotos(ServerRequest serverRequest) {
    Mono<MainPhotosRequest> mainPhotosRequestMono =
        serverRequest.bodyToMono(MainPhotosRequest.class);
    return mainPhotosRequestMono.flatMap(
        rq -> {
          Errors errors = new BeanPropertyBindingResult(rq, PhotoRequest.class.getName());
          validator.validate(rq, errors);

          if (errors.hasErrors()) {
            Map<String, Object> erroresMap = new HashMap<>();
            List<String> errorList = new ArrayList<>();
            errors.getFieldErrors().forEach(e -> errorList.add(e.getDefaultMessage()));
            erroresMap.put("errores", errorList);
            erroresMap.put("timestamp", LocalDateTime.now());

            return ServerResponse.badRequest().body(BodyInserters.fromValue(erroresMap));
          } else {
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    photoService.findHotelListMainPhotos(rq.getHotelIds().toArray(new String[0])),
                    Photo.class);
          }
        });
  }

  public Mono<ServerResponse> save(ServerRequest request) {

    //    Flux<FilePart> fluxFile =
    //        monoParts.flatMapMany(
    //            map -> {
    //              log.info("file flux");
    //              FilePart filePart = (FilePart) map.get("file");
    //              return Flux.just(filePart);
    //            });
    //    fluxFile.subscribe();
    //    Mono<ResponseEntity<Photo>> form=monoParts.flatMap(
    //        map -> {
    //          log.info("form mono");
    //          String hotelId = map.getFirst("hotelId");
    //          String roomId = map.getFirst("roomId");
    //          Boolean flagMain = Boolean.valueOf("flagMain");
    //
    //            try {
    //                return blobService
    //                    .uploadImageWithCaption(fluxFile, Form.builder()
    //                            .flagMain(flagMain)
    //                            .hotelId(hotelId)
    //                            .roomId(roomId).build())
    //                    .map(mono -> ResponseEntity.ok().body(mono));
    //            } catch (IOException e) {
    //                throw new RuntimeException(e);
    //            }
    //        });

    Mono<Photo> monoPhoto = uploadImageWithCaption(request);

    return monoPhoto
        .flatMap(
            photoForm -> {
              PhotoRequest photoRequest = serializePhotoToPhotoRequest(photoForm);
              Errors errors =
                  new BeanPropertyBindingResult(photoRequest, PhotoRequest.class.getName());
              validator.validate(photoForm, errors);

              if (errors.hasErrors()) {
                Map<String, Object> erroresMap = new HashMap<>();
                List<String> errorList = new ArrayList<>();
                errors.getFieldErrors().forEach(e -> errorList.add(e.getDefaultMessage()));
                erroresMap.put("errores", errorList);
                erroresMap.put("timestamp", LocalDateTime.now());

                return ServerResponse.badRequest().body(BodyInserters.fromValue(erroresMap));
              } else {
                Mono<ServerResponse> serverResponseMono =
                    photoService
                        .save(buildPhoto(photoRequest, null, null))
                        .flatMap(
                            photo ->
                                ServerResponse.created(URI.create("/photo/".concat(photo.getId())))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromValue(photo)));
                if (Boolean.TRUE == photoForm.getFlagMain()) {

                  Mono<List<Photo>> hotelMainPhotos =
                      photoService.findHotelMainPhoto(photoForm.getHotelId()).collectList();
                  serverResponseMono =
                      hotelMainPhotos.flatMap(
                          l -> {
                            if (!l.isEmpty()) {
                              return ServerResponse.status(HttpStatus.UNPROCESSABLE_ENTITY)
                                  .body(
                                      BodyInserters.fromValue(
                                          ErrorGeneric.error(
                                              "Ya existe una foto principal para este hotel")));
                            }
                            return photoService
                                .save(buildPhoto(photoRequest, null, null))
                                .flatMap(
                                    photo ->
                                        ServerResponse.created(
                                                URI.create("/photo/".concat(photo.getId())))
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .body(BodyInserters.fromValue(photo)));
                          });
                }
                return serverResponseMono;
              }
            })
        .onErrorResume(
            error -> {
              WebClientResponseException errorResponse = (WebClientResponseException) error;

              return Mono.error(errorResponse);
            });
  }

  public Mono<Photo> uploadImageWithCaption(ServerRequest request) {
    return request
        .multipartData()
        .flatMap(
            multipart -> {
              FilePart part = (FilePart) multipart.toSingleValueMap().get("file");
              String roomId = ((FormFieldPart) multipart.toSingleValueMap().get("roomId")).value();
              String hotelId =
                  ((FormFieldPart) multipart.toSingleValueMap().get("hotelId")).value();
              String flagMain =
                  ((FormFieldPart) multipart.toSingleValueMap().get("flagMain")).value();
              String description =
                  ((FormFieldPart) multipart.toSingleValueMap().get("description")).value();
              boolean isMain = Boolean.parseBoolean(flagMain);
              Flux<FilePart> file = Flux.just(part);
              Form form =
                  Form.builder()
                      .roomId(roomId)
                      .hotelId(hotelId)
                      .flagMain(isMain)
                      .description(description)
                      .build();
              try {
                return blobService.uploadImageWithCaption(file, form);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  public Mono<ServerResponse> update(ServerRequest serverRequest) {
    String id = serverRequest.pathVariable("id");
    Mono<PhotoRequest> photoRequestMono = serverRequest.bodyToMono(PhotoRequest.class);

    return photoService
        .findById(id)
        .flatMap(
            photo -> {
              Errors errors =
                  new BeanPropertyBindingResult(photoRequestMono, PhotoRequest.class.getName());
              validator.validate(photoRequestMono, errors);

              if (errors.hasErrors()) {
                Map<String, Object> erroresMap = new HashMap<>();
                List<String> errorList = new ArrayList<>();
                errors.getFieldErrors().forEach(e -> errorList.add(e.getDefaultMessage()));
                erroresMap.put("errores", errorList);

                return ServerResponse.badRequest().body(BodyInserters.fromValue(erroresMap));
              } else {
                return photoRequestMono
                    .flatMap(rq -> photoService.update(buildPhoto(rq, id, photo)))
                    .flatMap(
                        roomDb ->
                            ServerResponse.created(URI.create("/photo/".concat(roomDb.getId())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(roomDb)));
              }
            })
        .switchIfEmpty(
            ServerResponse.status(HttpStatus.NOT_FOUND)
                .body(BodyInserters.fromValue(ErrorNotFound.error(id))))
        .onErrorResume(
            error -> {
              WebClientResponseException errorResponse = (WebClientResponseException) error;

              return Mono.error(errorResponse);
            });
  }

  public Mono<ServerResponse> delete(ServerRequest serverRequest) {
    String id = serverRequest.pathVariable("id");

    return photoService.delete(id).then(ServerResponse.noContent().build());
  }

  public Photo buildPhoto(PhotoRequest photoRequest, String id, Photo photo) {

    return Photo.builder()
        .id(StringUtils.isEmpty(id) ? null : id)
        .url(photoRequest.getUrl())
        .hotelId(photoRequest.getHotelId())
        .roomId(photoRequest.getRoomId())
        .flagMain(photoRequest.getFlagMain())
        .description(photoRequest.getDescription())
        .createdAt(StringUtils.isEmpty(id) ? LocalDateTime.now() : photo.getCreatedAt())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  public PhotoRequest serializePhotoToPhotoRequest(Photo photo) {
    return PhotoRequest.builder()
        .url(photo.getUrl())
        .hotelId(photo.getHotelId())
        .roomId(photo.getRoomId())
        .flagMain(photo.getFlagMain())
        .description(photo.getDescription())
        .build();
  }
}
