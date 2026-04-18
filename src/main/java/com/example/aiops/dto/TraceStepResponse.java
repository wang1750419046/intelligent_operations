package com.example.aiops.dto;

public class TraceStepResponse {

    private int stepNo;
    private String thoughtSummary;
    private String actionName;
    private String actionParams;
    private String observation;
    private String createdAt;

    public TraceStepResponse() {
    }

    public TraceStepResponse(int stepNo, String thoughtSummary, String actionName, String actionParams, String observation, String createdAt) {
        this.stepNo = stepNo;
        this.thoughtSummary = thoughtSummary;
        this.actionName = actionName;
        this.actionParams = actionParams;
        this.observation = observation;
        this.createdAt = createdAt;
    }

    public int getStepNo() {
        return stepNo;
    }

    public void setStepNo(int stepNo) {
        this.stepNo = stepNo;
    }

    public String getThoughtSummary() {
        return thoughtSummary;
    }

    public void setThoughtSummary(String thoughtSummary) {
        this.thoughtSummary = thoughtSummary;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getActionParams() {
        return actionParams;
    }

    public void setActionParams(String actionParams) {
        this.actionParams = actionParams;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
