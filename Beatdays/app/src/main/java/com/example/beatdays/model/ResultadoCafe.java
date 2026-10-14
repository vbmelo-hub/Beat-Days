package com.example.beatdays.model;

/** Avalia a fase pontuada; perdas e toques fora do ritmo pesam na precisão. */
public final class ResultadoCafe {
    public enum Classificacao {
        PERFEITO,
        EXCELENTE,
        BOM,
        TENTE_NOVAMENTE
    }

    private final double precisaoPercentual;
    private final Classificacao classificacao;

    public ResultadoCafe(int perfects, int greats, int goods, int misses, int alimentosPontuaveis) {
        if (perfects < 0 || greats < 0 || goods < 0 || misses < 0 || alimentosPontuaveis <= 0) {
            throw new IllegalArgumentException("Contadores inválidos para avaliar a fase.");
        }
        long acertos = (long) perfects + greats + goods;
        long avaliacoes = Math.max(alimentosPontuaveis, acertos + misses);
        long pontos = 100L * perfects + 70L * greats + 40L * goods;
        precisaoPercentual = pontos / (double) avaliacoes;
        if (perfects == alimentosPontuaveis && greats == 0 && goods == 0 && misses == 0) {
            classificacao = Classificacao.PERFEITO;
        } else if (pontos >= 90L * avaliacoes) {
            classificacao = Classificacao.EXCELENTE;
        } else if (pontos >= 70L * avaliacoes) {
            classificacao = Classificacao.BOM;
        } else {
            classificacao = Classificacao.TENTE_NOVAMENTE;
        }
    }

    public double getPrecisaoPercentual() {
        return precisaoPercentual;
    }

    public Classificacao getClassificacao() {
        return classificacao;
    }
}
