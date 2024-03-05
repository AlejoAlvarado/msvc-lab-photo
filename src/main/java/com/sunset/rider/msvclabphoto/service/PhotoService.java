package com.sunset.rider.msvclabphoto.service;

import com.sunset.rider.msvclabphoto.model.Photo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PhotoService {
    Flux<Photo> findAll();

    Mono<Photo> findById(String id);

    Flux<Photo> findByRoomId(String roomId);

    Flux<Photo> findByHotelId(String hotelId);

    Flux<Photo> findHotelMainPhoto(String hotelId);

    Flux<Photo> findHotelListMainPhotos(String[] hotelIds);

    Mono<Photo> save(Photo photo);

    Mono<Photo> update(Photo photo);

    Mono<Void> delete(String id);
}
