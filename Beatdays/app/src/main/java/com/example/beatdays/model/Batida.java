package com.example.beatdays.model;

public class Batida {
    private final long tempoMs;
    private boolean processada;

    public Batida(long tempoMs) {
        this.tempoMs = tempoMs;
        this.processada = false;
    }

    public long getTempoMs() {
        return tempoMs;
    }

    public boolean isProcessada() {
        return processada;
    }

    public void marcarComoProcessada() {
        this.processada = true;
    }
}
