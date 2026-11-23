package com.chirag.nhbresidex.model;

public record CityRef(String stateId, String stateName, String cityId, String cityName) {
    public String fileId() {
        return stateId + "-" + cityId;
    }
}
