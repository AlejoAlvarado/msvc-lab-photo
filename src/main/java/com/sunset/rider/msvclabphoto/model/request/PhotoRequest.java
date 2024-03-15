package com.sunset.rider.msvclabphoto.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Jacksonized
public class PhotoRequest {
    @NotEmpty(message = "hotelId no puede ser nulo")
    private String hotelId;
    private String roomId;
    @NotEmpty(message = "base64 no puede ser nulo")
    private String url;
    private String description;
    @NotNull(message = "flagMain no puede ser nulo")
    private Boolean flagMain;
}
