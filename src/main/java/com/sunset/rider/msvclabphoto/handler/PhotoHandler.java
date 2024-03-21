package com.sunset.rider.msvclabphoto.handler;

import com.sunset.rider.lab.exceptions.exception.NotFoundException;
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

import java.io.File;
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
    Flux<FilePart> fluxFile = getFileFromMultipart(request);
    Mono<Form> form = getFormFromMultipart(request);
    return fluxFile
        .zipWith(form)
        .flatMap(
            photoForm -> {
              PhotoRequest photoRequest = serializeFormToPhotoRequest(photoForm.getT2());
              photoRequest.setFile(Flux.just(photoForm.getT1()));
              Mono<ServerResponse> serverResponseMono;
              Errors errors =
                  new BeanPropertyBindingResult(photoRequest, PhotoRequest.class.getName());
              validator.validate(photoRequest, errors);

              if (errors.hasErrors()) {
                Map<String, Object> erroresMap = new HashMap<>();
                List<String> errorList = new ArrayList<>();
                errors.getFieldErrors().forEach(e -> errorList.add(e.getDefaultMessage()));
                erroresMap.put("errores", errorList);
                erroresMap.put("timestamp", LocalDateTime.now());

                return ServerResponse.badRequest().body(BodyInserters.fromValue(erroresMap));
              } else {
                if (Boolean.TRUE == photoRequest.getFlagMain()) {
                  Mono<List<Photo>> hotelMainPhotos =
                      photoService.findHotelMainPhoto(photoForm.getT2().getHotelId()).collectList();
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
                            return savePhoto(photoRequest, photoForm.getT2(), null, null);
                          });
                } else {
                  serverResponseMono = savePhoto(photoRequest, photoForm.getT2(), null, null);
                }

                return serverResponseMono;
              }
            })
        .switchIfEmpty(
            ServerResponse.status(HttpStatus.NOT_FOUND)
                .body(BodyInserters.fromValue(ErrorNotFound.error("Error"))))
        .next();
  }

  private Mono<ServerResponse> savePhoto(
      PhotoRequest photoRequest, Form form, String id, Photo galleryPhoto) {
    Mono<Photo> photoMono = uploadImageWithCaption(photoRequest.getFile(), form);
    return photoMono.flatMap(
        photo -> {
          photoRequest.setUrl(photo.getUrl());
          Mono<ServerResponse> response =
              photoService
                  .save(buildPhoto(photoRequest, id, galleryPhoto))
                  .flatMap(
                      savePhoto ->
                          ServerResponse.created(URI.create("/photo/".concat(savePhoto.getId())))
                              .contentType(MediaType.APPLICATION_JSON)
                              .body(BodyInserters.fromValue(savePhoto)));
          return response;
        });
  }

  private Flux<FilePart> getFileFromMultipart(ServerRequest request) {
    return request
        .multipartData()
        .flatMapMany(
            multipart -> {
              FilePart part = (FilePart) multipart.toSingleValueMap().get("file");
              if (part == null) {
                return Flux.empty();
              }
              return Flux.just(part);
            });
  }

  private Mono<Form> getFormFromMultipart(ServerRequest request) {
    return request
        .multipartData()
        .flatMap(
            multipart -> {
              String roomId =
                  multipart.toSingleValueMap().get("roomId") != null
                      ? ((FormFieldPart) multipart.toSingleValueMap().get("roomId")).value()
                      : "";
              String hotelId =
                  multipart.toSingleValueMap().get("hotelId") != null
                      ? ((FormFieldPart) multipart.toSingleValueMap().get("hotelId")).value()
                      : "";
              String flagMain =
                  multipart.toSingleValueMap().get("flagMain") != null
                      ? ((FormFieldPart) multipart.toSingleValueMap().get("flagMain")).value()
                      : "";
              String description =
                  multipart.toSingleValueMap().get("description") != null
                      ? ((FormFieldPart) multipart.toSingleValueMap().get("description")).value()
                      : "";
              Form form =
                  Form.builder()
                      .roomId(roomId)
                      .hotelId(hotelId)
                      .flagMain(flagMain)
                      .description(description)
                      .build();
              return Mono.just(form);
            });
  }

  private Mono<Photo> uploadImageWithCaption(Flux<FilePart> file, Form form) {
    return Mono.just(form)
        .flatMap(
            photoForm -> {
              try {
                return blobService.uploadImageWithCaption(file, form);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  public Mono<ServerResponse> delete(ServerRequest serverRequest) {
    String id = serverRequest.pathVariable("id");
    return photoService
        .findById(id)
        .flatMap(
            photo -> {
              String[] fileNameParts = photo.getUrl().split("-.");
              String filename = "";
              if (fileNameParts.length > 2) {
                Arrays.stream(Arrays.copyOfRange(fileNameParts, 1, fileNameParts.length - 1))
                    .forEach(filename::concat);
              }
              filename = "-."+fileNameParts[1];
              blobService.deleteImageWithCaption(filename);
              return photoService.delete(id).then(ServerResponse.noContent().build());
            })
        .switchIfEmpty(
            ServerResponse.status(HttpStatus.NOT_FOUND)
                .body(BodyInserters.fromValue(ErrorNotFound.error(id))));
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

  public PhotoRequest serializeFormToPhotoRequest(Form form) {
    return PhotoRequest.builder()
        .hotelId(form.getHotelId())
        .roomId(form.getRoomId())
        .flagMain(form.getFlagMain().isEmpty() ? null : Boolean.valueOf(form.getFlagMain()))
        .description(form.getDescription())
        .build();
  }
}
