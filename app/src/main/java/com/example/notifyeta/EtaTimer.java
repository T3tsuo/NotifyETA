package com.example.notifyeta;

public class EtaTimer {
    private double etaTimer;

    public EtaTimer(Double initialEta) {
        this.etaTimer = initialEta;
    }

    public void calculateNewEtaTimer(Double currentEta) {
        double INTERVAL_MULTIPLIER = (double) 2 / 3;
        etaTimer = currentEta * INTERVAL_MULTIPLIER;
    }

    public double getEtaTimer() {
        return etaTimer;
    }
}
