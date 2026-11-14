package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.dto.CosmoCatDto;
import com.cosmocats.marketplace.dto.KittyProductDto;
import com.cosmocats.marketplace.featuretoggle.Feature;
import com.cosmocats.marketplace.featuretoggle.FeatureNotAvailableException;
import com.cosmocats.marketplace.service.CosmoCatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CosmoCatController.class)
class CosmoCatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CosmoCatService cosmoCatService;

    @Test
    void enabledFeatureReturnsCosmoCats() throws Exception {
        when(cosmoCatService.getCosmoCats()).thenReturn(List.of(new CosmoCatDto("Captain Whiskers", "Mars", "Navigator")));

        mockMvc.perform(get("/api/v1/cosmo-cats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Captain Whiskers"))
                .andExpect(jsonPath("$[0].homePlanet").value("Mars"));
    }

    @Test
    void enabledFeatureReturnsKittyProducts() throws Exception {
        when(cosmoCatService.getKittyProducts()).thenReturn(List.of(new KittyProductDto("Starter Star Ball", "0-6 months")));

        mockMvc.perform(get("/api/v1/cosmo-cats/kitty-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ageGroup").value("0-6 months"));
    }

    @Test
    void disabledFeatureIsNotFoundProblem() throws Exception {
        when(cosmoCatService.getKittyProducts()).thenThrow(new FeatureNotAvailableException(Feature.KITTY_PRODUCTS));

        mockMvc.perform(get("/api/v1/cosmo-cats/kitty-products"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/feature-not-available"))
                .andExpect(jsonPath("$.title").value("Feature not available"))
                .andExpect(jsonPath("$.detail").value("Feature kittyProducts is not available"))
                .andExpect(jsonPath("$.instance").value("/api/v1/cosmo-cats/kitty-products"));
    }
}
