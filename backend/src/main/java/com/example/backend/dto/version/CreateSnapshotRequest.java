package com.example.backend.dto.version;

import jakarta.validation.constraints.NotBlank;

public class CreateSnapshotRequest {
    @NotBlank
    private String yjsState; // base64-encoded Y.encodeStateAsUpdate(doc)

    private String stateVector; // optional base64-encoded Y.encodeStateVector(doc)

    public String getYjsState() { return yjsState; }
    public void setYjsState(String yjsState) { this.yjsState = yjsState; }
    public String getStateVector() { return stateVector; }
    public void setStateVector(String stateVector) { this.stateVector = stateVector; }
}