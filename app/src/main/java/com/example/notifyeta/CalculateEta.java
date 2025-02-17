package com.example.notifyeta;

public class CalculateEta {
    private double calcEta;

    public CalculateEta(Double initialEta) {
        this.calcEta = initialEta;
    }

    public boolean checkSendEta(Double currentEta) {
        if (calcEta == 0) {
            calcEta = currentEta / 3;
            return true;
        }
        if (calcEta > currentEta) {
            calcEta = calcEta / 3;
            return true;
        }
        return false;
    }
}
