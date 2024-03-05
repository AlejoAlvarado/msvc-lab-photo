package com.sunset.rider.msvclabphoto.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Jacksonized
public class MainPhotosRequest {
    @NotNull(message = "hotelIds no puede ser nulo")
    private List<String> hotelIds;
}
