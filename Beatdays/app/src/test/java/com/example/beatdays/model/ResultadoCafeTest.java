package com.example.beatdays.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class ResultadoCafeTest {
    @Test
    public void perfeitoExigeTodosOsAlimentosPerfectESemMiss() {
        ResultadoCafe perfeito = new ResultadoCafe(28, 0, 0, 0, 28);
        assertEquals(ResultadoCafe.Classificacao.PERFEITO, perfeito.getClassificacao());
        assertEquals(100, perfeito.getPrecisaoPercentual(), 0);
        assertEquals(
                ResultadoCafe.Classificacao.EXCELENTE,
                new ResultadoCafe(27, 1, 0, 0, 28).getClassificacao());
        assertEquals(
                ResultadoCafe.Classificacao.EXCELENTE,
                new ResultadoCafe(28, 0, 0, 1, 28).getClassificacao());
    }

    @Test
    public void pesosDeGreatEGoodAfetamPrecisao() {
        ResultadoCafe resultado = new ResultadoCafe(1, 1, 1, 25, 28);
        assertEquals(7.5, resultado.getPrecisaoPercentual(), .00001);
        assertEquals(
                ResultadoCafe.Classificacao.BOM,
                new ResultadoCafe(0, 28, 0, 0, 28).getClassificacao());
        assertEquals(40, new ResultadoCafe(0, 0, 28, 0, 28).getPrecisaoPercentual(), 0);
    }

    @Test
    public void limitesDeExcelenteEBomSaoInclusivos() {
        assertEquals(
                ResultadoCafe.Classificacao.EXCELENTE,
                new ResultadoCafe(9, 0, 0, 1, 10).getClassificacao());
        assertEquals(
                ResultadoCafe.Classificacao.BOM,
                new ResultadoCafe(7, 0, 0, 3, 10).getClassificacao());
        assertEquals(
                ResultadoCafe.Classificacao.TENTE_NOVAMENTE,
                new ResultadoCafe(6, 1, 0, 3, 10).getClassificacao());
    }

    @Test
    public void toqueErradoReduzPrecisaoMesmoSeTodosOsAlimentosForemPegos() {
        ResultadoCafe resultado = new ResultadoCafe(28, 0, 0, 1, 28);
        assertEquals(2800.0 / 29, resultado.getPrecisaoPercentual(), .00001);
        ResultadoCafe spam = new ResultadoCafe(28, 0, 0, 28, 28);
        assertEquals(50, spam.getPrecisaoPercentual(), 0);
        assertEquals(ResultadoCafe.Classificacao.TENTE_NOVAMENTE, spam.getClassificacao());
    }

    @Test
    public void nenhumToqueOuResultadoAindaAusenteNaoGeraNotaAlta() {
        ResultadoCafe vazio = new ResultadoCafe(0, 0, 0, 0, 28);
        assertEquals(0, vazio.getPrecisaoPercentual(), 0);
        assertEquals(ResultadoCafe.Classificacao.TENTE_NOVAMENTE, vazio.getClassificacao());
        ResultadoCafe incompleto = new ResultadoCafe(1, 0, 0, 0, 28);
        assertEquals(100.0 / 28, incompleto.getPrecisaoPercentual(), .00001);
        assertEquals(ResultadoCafe.Classificacao.TENTE_NOVAMENTE, incompleto.getClassificacao());
        assertNotEquals(
                ResultadoCafe.Classificacao.PERFEITO,
                new ResultadoCafe(27, 0, 0, 0, 28).getClassificacao());
    }

    @Test
    public void contadoresGrandesNaoEstouramCalculo() {
        ResultadoCafe resultado =
                new ResultadoCafe(Integer.MAX_VALUE, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(50, resultado.getPrecisaoPercentual(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejeitaFaseSemAlimentosPontuaveis() {
        new ResultadoCafe(0, 0, 0, 0, 0);
    }
}
