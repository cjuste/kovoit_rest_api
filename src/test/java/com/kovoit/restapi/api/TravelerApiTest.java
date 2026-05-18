package com.kovoit.restapi.api;

import com.kovoit.restapi.bean.Address;
import com.kovoit.restapi.bean.PersonalInfo;
import com.kovoit.restapi.bean.Traveler;
import com.kovoit.restapi.service.TravelerService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelerApiTest {

  @Mock
  private TravelerService travelerService;

  @InjectMocks
  private TravelerApi travelerApi;

  @Test
  void getTravelers_delegatesToServiceAndReturnsList() {
    Traveler traveler = new Traveler(new PersonalInfo("Alice", "Dupont", "alice@example.com", null));
    when(travelerService.getTravelers()).thenReturn(List.of(traveler));

    List<Traveler> result = travelerApi.getTravelers();

    assertThat(result).containsExactly(traveler);
    verify(travelerService).getTravelers();
  }

  @Test
  void saveTraveler_returns201WithTravelerBody() {
    Traveler traveler = new Traveler(new PersonalInfo("Alice", "Dupont", "alice@example.com", null));
    when(travelerService.saveTraveler(traveler)).thenReturn(traveler);

    try (Response response = travelerApi.saveTraveler(traveler)) {
      assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
      assertThat(response.getEntity()).isEqualTo(traveler);
      verify(travelerService).saveTraveler(traveler);
    }
  }

  @Test
  void importCsv_returns200WithImportedTravelerList() {
    Traveler traveler = new Traveler(
            new PersonalInfo("Clément", "Juste", "cj@test.fr", new Address("Paris", 48.85, 2.35)), true);
    InputStream csvStream = new ByteArrayInputStream("dummy".getBytes(StandardCharsets.UTF_8));
    when(travelerService.importFromCsv(any())).thenReturn(List.of(traveler));

    try (Response response = travelerApi.importCsv(csvStream)) {
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(response.getEntity()).isEqualTo(List.of(traveler));
      verify(travelerService).importFromCsv(any());
    }
  }
}
