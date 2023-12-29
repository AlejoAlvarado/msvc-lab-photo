package com.sunset.rider.msvclabphoto.router;

import com.sunset.rider.msvclabphoto.handler.PhotoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterConfig {
  @Bean
  public RouterFunction<ServerResponse> rutasPhoto(PhotoHandler photoHandler) {
    return RouterFunctions.route(RequestPredicates.GET("/photos"), photoHandler::findAll)
        .andRoute(RequestPredicates.GET("/photos/{id}"), photoHandler::findById)
        .andRoute(RequestPredicates.GET("/photos/room/{roomId}"), photoHandler::findByRoomId)
        .andRoute(RequestPredicates.GET("/photos/hotel/{hotelId}"), photoHandler::findByHotelId)
        .andRoute(
            RequestPredicates.GET("/photos/hotel/main/{hotelId}"), photoHandler::findHotelMainPhoto)
        .andRoute(RequestPredicates.POST("/photos"), photoHandler::save)
        .andRoute(RequestPredicates.PUT("/photos/{id}"), photoHandler::update)
        .andRoute(RequestPredicates.DELETE("/photos/{id}"), photoHandler::delete);
  }
}
