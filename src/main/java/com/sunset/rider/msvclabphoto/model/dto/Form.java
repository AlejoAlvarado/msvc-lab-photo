package com.sunset.rider.msvclabphoto.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ToString
@Builder
public class Form {

    private String hotelId;

    private String roomId;

    private boolean flagMain;

    private String description;
    // ...

}