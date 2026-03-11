package com.maestros.model;

import java.util.UUID;

public class MaestroSearchFilters {

    private UUID categoryId;
    private String city;
    private Double minRating;
    private Long maxPriceClp;
    private String query;

    public MaestroSearchFilters() {
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getMinRating() {
        return minRating;
    }

    public void setMinRating(Double minRating) {
        this.minRating = minRating;
    }

    public Long getMaxPriceClp() {
        return maxPriceClp;
    }

    public void setMaxPriceClp(Long maxPriceClp) {
        this.maxPriceClp = maxPriceClp;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
