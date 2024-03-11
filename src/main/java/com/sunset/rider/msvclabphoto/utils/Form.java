package com.sunset.rider.msvclabphoto.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ToString
public class Form {

    private String hotelId;

    private String roomId;

    private boolean flagMain;
    // ...

}