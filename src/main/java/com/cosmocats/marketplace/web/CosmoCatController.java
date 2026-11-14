package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.dto.CosmoCatDto;
import com.cosmocats.marketplace.dto.KittyProductDto;
import com.cosmocats.marketplace.service.CosmoCatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cosmo-cats")
public class CosmoCatController {

    private final CosmoCatService cosmoCatService;

    public CosmoCatController(CosmoCatService cosmoCatService) {
        this.cosmoCatService = cosmoCatService;
    }

    @GetMapping
    public List<CosmoCatDto> getCosmoCats() {
        return cosmoCatService.getCosmoCats();
    }

    @GetMapping("/kitty-products")
    public List<KittyProductDto> getKittyProducts() {
        return cosmoCatService.getKittyProducts();
    }
}
