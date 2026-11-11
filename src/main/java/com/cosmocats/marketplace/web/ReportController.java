package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.dto.ProductSalesDto;
import com.cosmocats.marketplace.service.ReportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Most purchased products (paid, shipped or delivered orders), best sellers first. */
    @GetMapping("/top-products")
    public List<ProductSalesDto> getTopSellingProducts(
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "must be at least 1")
            @Max(value = 100, message = "must be at most 100") int limit) {
        return reportService.getTopSellingProducts(limit);
    }
}
