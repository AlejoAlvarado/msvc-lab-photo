package com.sunset.rider.msvclabphoto.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Jacksonized
public class PhotoRequest {
  @NotEmpty(message = "hotelId no puede ser vacio")
  private String hotelId;

  private String roomId;

  @NotNull(message = "File no puede ser nulo")
  private Flux<FilePart> file;

  @JsonIgnore
  private String url;
  private String description;

  @NotNull(message = "flagMain no puede ser nulo")
  private Boolean flagMain;
}
