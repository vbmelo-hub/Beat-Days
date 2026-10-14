package com.example.beatdays.game;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;

import org.junit.Test;

import java.util.List;

public class MotorRecepcaoTest {
    @Test
    public void todosOsAlimentosAparecemNaDemonstracaoTreinoEFase() {
        java.util.EnumSet<EntregaCafe.Alimento> demonstracao =
                java.util.EnumSet.noneOf(EntregaCafe.Alimento.class);
        java.util.EnumSet<EntregaCafe.Alimento> treino =
                java.util.EnumSet.noneOf(EntregaCafe.Alimento.class);
        java.util.EnumSet<EntregaCafe.Alimento> fase =
                java.util.EnumSet.noneOf(EntregaCafe.Alimento.class);
        for (EntregaCafe entrega : RoteiroRecepcao.criarEntregas()) {
            if (entrega.demonstracao) {
                demonstracao.add(entrega.alimento);
            } else if (entrega.tempoMs < RoteiroRecepcao.INICIO_PONTUACAO_MS) {
                treino.add(entrega.alimento);
            } else {
                fase.add(entrega.alimento);
            }
        }
        assertEquals(4, EntregaCafe.Alimento.values().length);
        java.util.EnumSet<EntregaCafe.Alimento> cardapio =
                java.util.EnumSet.allOf(EntregaCafe.Alimento.class);
        assertEquals(cardapio, demonstracao);
        assertEquals(cardapio, treino);
        assertEquals(cardapio, fase);
    }

    @Test
    public void avisosTemSignificadoConsistenteETodosSaoEnsinadosAntesDaPontuacao() {
        java.util.EnumSet<EntregaCafe.Aviso> ensinados =
                java.util.EnumSet.noneOf(EntregaCafe.Aviso.class);
        java.util.EnumSet<EntregaCafe.Aviso> fase =
                java.util.EnumSet.noneOf(EntregaCafe.Aviso.class);
        int relogios = 0;
        for (EntregaCafe e : RoteiroRecepcao.criarEntregas()) {
            if (e.tempoMs < RoteiroRecepcao.INICIO_PONTUACAO_MS) {
                ensinados.add(e.tipoAviso);
            } else {
                fase.add(e.tipoAviso);
            }
            if (e.tipoAviso == EntregaCafe.Aviso.TIMER) {
                assertEquals(3, e.quantidade);
            } else {
                assertEquals(e.tipoAviso == EntregaCafe.Aviso.RELOGIO ? 2 : 1, e.quantidade);
                assertEquals(1000, e.tempoMs - e.avisoMs);
            }
            if (e.tipoAviso == EntregaCafe.Aviso.RELOGIO) {
                relogios++;
            }
        }
        assertEquals(java.util.EnumSet.allOf(EntregaCafe.Aviso.class), ensinados);
        assertEquals(ensinados, fase);
        assertEquals(8, relogios);
    }

    @Test
    public void avisoDoRelogioNaoPontuaEApenasACapturaAceitaToque() {
        MotorRecepcao motor = new MotorRecepcao();
        assertNull(motor.getJulgamento(idEntrega(13000)));
        assertEquals(Julgamento.MISS, motor.pressionar(12000).julgamento);
        motor.soltar();
        assertNull(motor.getJulgamento(idEntrega(13000)));
        assertEquals(Julgamento.PERFECT, motor.pressionar(13000).julgamento);
        assertEquals(Julgamento.PERFECT, motor.getJulgamento(idEntrega(13000)));
    }

    @Test
    public void demonstracaoNuncaGeraJulgamentoDoJogador() {
        MotorRecepcao motor = new MotorRecepcao();
        assertTrue(motor.processarPerdidas(11000).isEmpty());
        for (EntregaCafe e : motor.getEntregas()) {
            if (e.demonstracao) {
                assertNull(motor.getJulgamento(e.id));
            }
        }
    }

    @Test
    public void semToquesTodosOsAlimentosJogaveisSaoMissUmaVez() {
        MotorRecepcao motor = new MotorRecepcao();
        List<MotorRecepcao.Resultado> perdas = motor.processarPerdidas(80000);
        assertEquals(36, perdas.size());
        for (MotorRecepcao.Resultado r : perdas) {
            assertEquals(Julgamento.MISS, r.julgamento);
        }
        assertTrue(motor.processarPerdidas(80001).isEmpty());
    }

    @Test
    public void segurarNaoRecebeOutrosAlimentosESoltarNaoPontua() {
        MotorRecepcao motor = new MotorRecepcao();
        assertEquals(Julgamento.PERFECT, motor.pressionar(13000).julgamento);
        assertNull(motor.pressionar(17500));
        assertEquals(4, motor.processarPerdidas(18800).size());
        motor.soltar();
        assertTrue(motor.processarPerdidas(18801).isEmpty());
        assertEquals(Julgamento.PERFECT, motor.pressionar(21000).julgamento);
    }

    @Test
    public void tresBipsExigemTresPressoesIndependentes() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.processarPerdidas(16000);
        for (long tempo : new long[] {17500, 18000, 18500}) {
            MotorRecepcao.Resultado r = motor.pressionar(tempo);
            assertEquals(Julgamento.PERFECT, r.julgamento);
            assertEquals(3, r.entrega.quantidade);
            motor.soltar();
        }
        assertTrue(motor.processarPerdidas(18800).isEmpty());
    }

    @Test
    public void tocarNoAvisoRespondeSemConsumirAEntrega() {
        MotorRecepcao motor = new MotorRecepcao();
        MotorRecepcao.Resultado cedo = motor.pressionar(12000);
        assertEquals(Julgamento.MISS, cedo.julgamento);
        assertFalse(cedo.consumido);
        motor.soltar();
        assertEquals(Julgamento.PERFECT, motor.pressionar(13000).julgamento);
    }

    @Test
    public void janelasDePrecisaoPermanecemIguais() {
        assertEquals(Julgamento.PERFECT, new MotorRecepcao().pressionar(13070).julgamento);
        assertEquals(Julgamento.GREAT, new MotorRecepcao().pressionar(13130).julgamento);
        assertEquals(Julgamento.GOOD, new MotorRecepcao().pressionar(13200).julgamento);
        assertEquals(Julgamento.MISS, new MotorRecepcao().pressionar(13201).julgamento);
    }

    @Test
    public void restaurarNaoDuplicaCapturasENaoMantemDedoPreso() {
        MotorRecepcao antes = new MotorRecepcao();
        antes.pressionar(13000);
        MotorRecepcao depois = new MotorRecepcao();
        depois.restaurar(antes.salvar());
        assertEquals(Julgamento.MISS, depois.pressionar(13000).julgamento);
        depois.soltar();
        assertEquals(Julgamento.PERFECT, depois.pressionar(17500).julgamento);
    }

    @Test
    public void fasesRapidasEscolhemOAlimentoMaisProximo() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.processarPerdidas(56000);
        MotorRecepcao.Resultado r = motor.pressionar(57175);
        assertEquals(57250, r.entrega.tempoMs);
        assertEquals(Julgamento.GREAT, r.julgamento);
    }

    @Test
    public void roteiroMantemAvisosVoosETodasAsCapturas() {
        MotorRecepcao motor = new MotorRecepcao();
        int jogaveis = 0, pontuados = 0;
        long anterior = -1;
        for (EntregaCafe e : motor.getEntregas()) {
            assertTrue(e.tempoMs > anterior);
            anterior = e.tempoMs;
            assertEquals(
                    e.quantidade == 3 && e.pedido != 13 && e.pedido != 15 && e.pedido != 16
                            ? 1500
                            : 1000,
                    e.tempoMs - e.avisoMs);
            assertEquals(750, e.tempoMs - e.lancamentoMs);
            assertTrue(e.tempoMs < RoteiroRecepcao.FIM_PEDIDOS_MS);
            if (!e.demonstracao) {
                assertEquals(Julgamento.PERFECT, motor.pressionar(e.tempoMs).julgamento);
                motor.soltar();
                jogaveis++;
                if (e.tempoMs >= RoteiroRecepcao.INICIO_PONTUACAO_MS) {
                    pontuados++;
                }
            }
        }
        assertEquals(36, jogaveis);
        assertEquals(31, pontuados);
        assertTrue(motor.processarPerdidas(80000).isEmpty());
    }

    @Test
    public void ultimoBipEPrimeiraCapturaEstaoSeparadosEmTodosOsTrios() {
        List<EntregaCafe> entregas = RoteiroRecepcao.criarEntregas();
        for (int i = 0; i + 2 < entregas.size(); i++) {
            EntregaCafe primeira = entregas.get(i);
            if (primeira.quantidade != 3
                    || i > 0 && entregas.get(i - 1).pedido == primeira.pedido) {
                continue;
            }
            assertEquals(500, primeira.tempoMs - entregas.get(i + 2).avisoMs);
            assertTrue(primeira.tempoMs - 200 > entregas.get(i + 2).avisoMs + 100);
        }
    }

    @Test
    public void trilhaDeToquesGuardaPressaoFisicaMesmoMissMasNuncaPerdaAutomatica() {
        MotorRecepcao motor = new MotorRecepcao();
        int primeira = idEntrega(17500);
        assertEquals(Julgamento.PERFECT, motor.pressionar(17500).julgamento);
        assertEquals(17500, motor.getToqueTrio(primeira));
        assertNull(motor.pressionar(17750));
        assertEquals(-1, motor.getToqueTrio(primeira + 1));
        motor.soltar();
        assertEquals(Julgamento.MISS, motor.pressionar(17750).julgamento);
        assertEquals(17750, motor.getToqueTrio(primeira + 1));
        motor.soltar();
        motor.processarPerdidas(18800);
        assertEquals(-1, motor.getToqueTrio(primeira + 2));
        MotorRecepcao outro = new MotorRecepcao();
        outro.restaurar(motor.salvar());
        outro.restaurarToquesTrio(motor.salvarToquesTrio());
        assertArrayEquals(motor.salvarToquesTrio(), outro.salvarToquesTrio());
        outro.reiniciar();
        for (long t : outro.salvarToquesTrio()) {
            assertEquals(-1, t);
        }
    }

    @Test
    public void spamDepoisDeMissNaoCapturaAlimentoMesmoNaJanelaPerfect() {
        MotorRecepcao motor = new MotorRecepcao();
        assertEquals(Julgamento.MISS, motor.pressionar(12790).julgamento);
        motor.soltar();
        for (long t = 12800; t <= 13200; t += 10) {
            assertNull("Spam aceito em " + t, motor.pressionar(t));
            motor.soltar();
            assertNull(motor.getJulgamento(idEntrega(13000)));
        }
        assertEquals(13340, motor.getBloqueadoAteMs());
        assertEquals(Julgamento.MISS, motor.processarPerdidas(13201).get(0).julgamento);
        assertEquals(13340, motor.getBloqueadoAteMs());
    }

    @Test
    public void pequenasRegressoesNoHorarioDoEventoNaoReabremABloqueio() {
        MotorRecepcao motor = new MotorRecepcao();
        assertEquals(Julgamento.MISS, motor.pressionar(12450).julgamento);
        motor.soltar();
        for (long tempo : new long[] {12449, 12450, 12448, 12451, 12999}) {
            assertNull("Entrada aceita durante a penalidade em " + tempo, motor.pressionar(tempo));
            motor.soltar();
            assertEquals(13000, motor.getBloqueadoAteMs());
        }
        assertEquals(Julgamento.PERFECT, motor.pressionar(13000).julgamento);
    }

    @Test
    public void prazoDaPenalidadeTemLimiteExatoESpamNaoOProlonga() {
        MotorRecepcao motor = new MotorRecepcao();
        assertEquals(Julgamento.MISS, motor.pressionar(12450).julgamento);
        motor.soltar();
        assertTrue(motor.estaEmRecuperacao(12999));
        assertNull(motor.pressionar(12999));
        motor.soltar();
        assertFalse(motor.estaEmRecuperacao(13000));
        assertEquals(Julgamento.PERFECT, motor.pressionar(13000).julgamento);
    }

    @Test
    public void segurarDuranteBloqueioNaoCriaCapturaAoExpirar() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.pressionar(12450);
        motor.soltar();
        assertNull(motor.pressionar(12900));
        assertNull(motor.pressionar(13000));
        assertNull(motor.getJulgamento(idEntrega(13000)));
        motor.soltar();
        assertEquals(Julgamento.PERFECT, motor.pressionar(13001).julgamento);
    }

    @Test
    public void triosRapidosSemErroContinuamAceitandoTodosOsAcertos() {
        MotorRecepcao motor = new MotorRecepcao();
        for (long t : new long[] {57000, 57250, 57500}) {
            assertEquals(Julgamento.PERFECT, motor.pressionar(t).julgamento);
            motor.soltar();
            assertFalse(motor.estaEmRecuperacao(t));
        }
    }

    @Test
    public void cliquesBloqueadosNaoAdiantamDegrausDeZoom() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.pressionar(17500);
        motor.soltar();
        motor.pressionar(17750);
        motor.soltar();
        assertNull(motor.pressionar(18000));
        motor.soltar();
        assertEquals(-1, motor.getToqueTrio(idEntrega(18500)));
        assertEquals(Julgamento.PERFECT, motor.pressionar(18500).julgamento);
        assertEquals(18500, motor.getToqueTrio(idEntrega(18500)));
    }

    @Test
    public void restaurarPreservaRecuperacaoEReiniciarLimpaPenalidade() {
        MotorRecepcao antes = new MotorRecepcao();
        antes.pressionar(12790);
        antes.soltar();
        MotorRecepcao depois = new MotorRecepcao();
        depois.restaurar(antes.salvar());
        depois.restaurarRecuperacao(antes.getBloqueadoAteMs());
        assertTrue(depois.estaEmRecuperacao(13000));
        assertNull(depois.pressionar(13000));
        depois.soltar();
        depois.reiniciar();
        assertFalse(depois.estaEmRecuperacao(13000));
        assertEquals(Julgamento.PERFECT, depois.pressionar(13000).julgamento);
    }

    @Test
    public void restaurarInstantesPreservaToqueRealEPerdaTardiaSemMudarJulgamentos() {
        MotorRecepcao antes = new MotorRecepcao();
        antes.pressionar(25050);
        antes.soltar();
        antes.processarPerdidas(25800);
        MotorRecepcao depois = new MotorRecepcao();
        depois.restaurar(antes.salvar());
        depois.restaurarInstantes(antes.salvarInstantes());
        assertEquals(25050, depois.getInstante(idEntrega(25000)));
        assertEquals(25800, depois.getInstante(idEntrega(25500)));
        assertArrayEquals(antes.salvar(), depois.salvar());
    }

    @Test
    public void instantesAntigosOuInvalidosMantemFallbackSemCriarAcertos() {
        MotorRecepcao antes = new MotorRecepcao();
        antes.pressionar(25050);
        antes.soltar();
        MotorRecepcao depois = new MotorRecepcao();
        depois.restaurar(antes.salvar());
        depois.restaurarInstantes(null);
        depois.restaurarInstantes(new long[1]);
        assertEquals(25000, depois.getInstante(idEntrega(25000)));
        long[] invalido = antes.salvarInstantes();
        invalido[idEntrega(25000)] = 80001;
        invalido[idEntrega(25500)] = 25500;
        depois.restaurarInstantes(invalido);
        assertEquals(25000, depois.getInstante(idEntrega(25000)));
        assertNull(depois.getJulgamento(idEntrega(25500)));
        invalido[idEntrega(25000)] = 24000;
        depois.restaurarInstantes(invalido);
        assertEquals(25000, depois.getInstante(idEntrega(25000)));
    }

    @Test
    public void salvarInstantesNaoExpoeEstadoInternoEReiniciarLimpaTudo() {
        MotorRecepcao motor = new MotorRecepcao();
        motor.pressionar(25050);
        motor.soltar();
        long[] salvo = motor.salvarInstantes();
        salvo[idEntrega(25000)] = 0;
        assertEquals(25050, motor.getInstante(idEntrega(25000)));
        motor.reiniciar();
        for (long instante : motor.salvarInstantes()) {
            assertEquals(-1, instante);
        }
    }

    private int idEntrega(long alvo) {
        for (com.example.beatdays.model.EntregaCafe e :
                com.example.beatdays.game.RoteiroRecepcao.criarEntregas()) {
            if (e.tempoMs == alvo) {
                return e.id;
            }
        }
        throw new AssertionError("Captura não encontrada em " + alvo);
    }
}
