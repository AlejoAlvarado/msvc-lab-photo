package com.sunset.rider.msvclabphoto.service;

import com.sunset.rider.msvclabphoto.model.Photo;
import com.sunset.rider.msvclabphoto.repository.PhotoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PhotoServiceImpl implements PhotoService {

  private PhotoRepository photoRepository;

  public PhotoServiceImpl(PhotoRepository photoRepository) {
    this.photoRepository = photoRepository;
  }

  @Override
  public Flux<Photo> findAll() {
    return photoRepository.findAll();
  }

  @Override
  public Mono<Photo> findById(String id) {
    return photoRepository.findById(id);
  }

  @Override
  public Flux<Photo> findByRoomId(String roomId) {
    return photoRepository.findByRoomId(roomId);
  }

  @Override
  public Flux<Photo> findByHotelId(String hotelId) {
    return photoRepository.findByHotelId(hotelId);
  }

  @Override
  public Mono<Photo> findHotelMainPhoto(String hotelId) {
    return photoRepository.findHotelMainPhoto(hotelId);
  }

  @Override
  public Mono<Photo> save(Photo photo) {
    return photoRepository.save(photo);
  }

  @Override
  public Mono<Photo> update(Photo photo) {
    return photoRepository.save(photo);
  }

  @Override
  public Mono<Void> delete(String id) {
    return photoRepository.deleteById(id);
  }
}
