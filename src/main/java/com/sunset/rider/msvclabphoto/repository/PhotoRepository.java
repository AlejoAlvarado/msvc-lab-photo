package com.sunset.rider.msvclabphoto.repository;

import com.sunset.rider.msvclabphoto.model.Photo;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PhotoRepository extends ReactiveMongoRepository<Photo,String> {
    Flux<Photo> findByHotelId(@Param("hotelId")String hotelId);
    Flux<Photo> findByRoomId(@Param("roomId")String roomId);
    @Query("{hotelId: ?0, flagMain: true}")
    Flux<Photo> findHotelMainPhoto(String hotelId);
}
