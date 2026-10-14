package com.example.beatdays.ui.game.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.model.Julgamento;

import org.junit.Test;

public class CoreografiaCafeTest {
    private final CoreografiaCafe cena = new CoreografiaCafe();
    private final MotorRecepcao motor = new MotorRecepcao();

    private void atualizar(long t) {
        cena.atualizar(t, motor, -10000, null);
    }

    @Test
    public void mikaPreparaLancaEVoltaSemMudarOChart() {
        atualizar(4120);
        assertEquals(CoreografiaCafe.Mika.PREPARA, cena.getMika());
        atualizar(4280);
        assertEquals(CoreografiaCafe.Mika.LANCAR, cena.getMika());
        atualizar(4600);
        assertEquals(CoreografiaCafe.Mika.ESPERA, cena.getMika());
        assertEquals(40, motor.getEntregas().size());
    }

    @Test
    public void esperaNaJanelaMasNuncaPegaSemEntrada() {
        atualizar(12800);
        assertEquals(CoreografiaCafe.Cliente.PREPARA, cena.getCliente());
        atualizar(13000);
        assertEquals(CoreografiaCafe.Cliente.PREPARA, cena.getCliente());
        atualizar(13190);
        assertEquals(CoreografiaCafe.Cliente.PREPARA, cena.getCliente());
        atualizar(13201);
        assertEquals(CoreografiaCafe.Cliente.ESPERA, cena.getCliente());
        assertNull(motor.getJulgamento(idEntrega(13000)));
    }

    @Test
    public void capturaAcompanhaEntradaFisicaENovoToqueReiniciaAnimacao() {
        motor.pressionar(17560);
        motor.soltar();
        atualizar(17700);
        assertEquals(CoreografiaCafe.Cliente.PEGA, cena.getCliente());
        assertEquals(140 / 520f, cena.getProgressoCliente(), .001f);
        motor.pressionar(18010);
        motor.soltar();
        atualizar(18020);
        assertEquals(10 / 520f, cena.getProgressoCliente(), .001f);
    }

    @Test
    public void demonstracaoAnimaSemInventarJulgamento() {
        atualizar(5050);
        assertEquals(CoreografiaCafe.Cliente.PEGA, cena.getCliente());
        assertNull(motor.getJulgamento(idEntrega(5000)));
        assertTrue(motor.processarPerdidas(6000).isEmpty());
    }

    @Test
    public void perdaAutomaticaEToqueForaDoRitmoProduzemReacao() {
        motor.processarPerdidas(13210);
        atualizar(13240);
        assertEquals(CoreografiaCafe.Cliente.ERRA, cena.getCliente());
        cena.atualizar(14200, motor, 14100, Julgamento.MISS);
        assertEquals(CoreografiaCafe.Cliente.ERRA, cena.getCliente());
        atualizar(14900);
        assertEquals(CoreografiaCafe.Cliente.ESPERA, cena.getCliente());
    }

    @Test
    public void efeitoVisualNaoJulgaNemConsomeAlimentos() {
        for (long t = 0; t < 80000; t += 16) {
            atualizar(t);
        }
        for (int estado : motor.salvar()) {
            assertEquals(0, estado);
        }
        assertEquals(36, motor.processarPerdidas(80000).size());
    }

    @Test
    public void perdasAntigasNaoEncobremCapturaAtual() {
        motor.processarPerdidas(37550);
        motor.pressionar(37549);
        motor.soltar();
        cena.atualizar(37550, motor, 37549, Julgamento.PERFECT);
        assertEquals(CoreografiaCafe.Cliente.PEGA, cena.getCliente());
    }

    @Test
    public void coresMudamNosTresBipsEVoltamSemFlash() {
        atualizar(56120);
        assertEquals(CoreografiaCafe.Paleta.MEL, cena.getPaleta());
        atualizar(56370);
        assertEquals(CoreografiaCafe.Paleta.CORAL, cena.getPaleta());
        assertEquals(CoreografiaCafe.Paleta.MEL, cena.getPaletaAnterior());
        atualizar(56620);
        assertEquals(CoreografiaCafe.Paleta.PETROLEO, cena.getPaleta());
        assertEquals(CoreografiaCafe.Paleta.CORAL, cena.getPaletaAnterior());
        atualizar(57800);
        assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
        assertEquals(.5f, cena.getMistura(), .001f);
        atualizar(58200);
        assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaletaAnterior());
    }

    @Test
    public void cameraFicaParadaForaDosTriosInclusiveNoPerfect() {
        motor.pressionar(25000);
        motor.soltar();
        for (long t :
                new long[] {0, 1000, 4100, 5000, 13050, 21000, 25050, 33000, 61000, 74000, 79000}) {
            atualizar(t);
            assertEquals("Zoom fora de trio em " + t, 1, cena.getZoom(), 0);
            assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
        }
    }

    @Test
    public void trioTemTresDegrausAcumuladosSemRecuarEntreBips() {
        float anterior = 1;
        for (long t = 55880; t < 56700; t += 10) {
            atualizar(t);
            assertTrue(cena.getZoom() >= anterior);
            anterior = cena.getZoom();
        }
        atualizar(56120);
        assertEquals(1.03f, cena.getZoom(), .00001f);
        atualizar(56250);
        assertEquals(1.03f, cena.getZoom(), .00001f);
        atualizar(56370);
        assertEquals(1.06f, cena.getZoom(), .00001f);
        atualizar(56500);
        assertEquals(1.06f, cena.getZoom(), .00001f);
        for (long t = 56620; t <= 57700; t += 17) {
            atualizar(t);
            assertEquals(1.09f, cena.getZoom(), .00001f);
        }
        anterior = 1.09f;
        for (long t = 57700; t <= 57900; t += 10) {
            atualizar(t);
            assertTrue(cena.getZoom() <= anterior);
            anterior = cena.getZoom();
        }
        atualizar(57900);
        assertEquals(1, cena.getZoom(), 0);
        atualizar(9700);
        assertEquals(1.09f, cena.getZoom(), .00001f);
    }

    @Test
    public void seisCoresExigemTresAcertosReaisNoTrioRapido() {
        long[] bips = {56120, 56370, 56620};
        CoreografiaCafe.Paleta[] cores = {
            CoreografiaCafe.Paleta.MEL,
            CoreografiaCafe.Paleta.CORAL,
            CoreografiaCafe.Paleta.PETROLEO,
            CoreografiaCafe.Paleta.PESSEGO,
            CoreografiaCafe.Paleta.LILAS,
            CoreografiaCafe.Paleta.SALVIA
        };
        for (int n = 0; n < 3; n++) {
            atualizar(bips[n]);
            assertEquals(cores[n], cena.getPaleta());
        }
        atualizar(57000);
        assertEquals(cores[2], cena.getPaleta());
        for (int n = 0; n < 3; n++) {
            assertNotEquals(Julgamento.MISS, motor.pressionar(57000 + n * 250).julgamento);
            motor.soltar();
            atualizar(57050 + n * 250);
            assertEquals(cores[3 + n], cena.getPaleta());
            assertEquals(1, cena.getMistura(), 0);
            assertEquals(1f + (2 - n) * .03f, cena.getZoom(), .00001f);
        }
    }

    @Test
    public void trioLentoTerminaZoomInAntesDeAbrirPrimeiraCaptura() {
        atualizar(17000);
        assertEquals(CoreografiaCafe.Paleta.PETROLEO, cena.getPaleta());
        atualizar(17080);
        assertEquals(CoreografiaCafe.Paleta.PETROLEO, cena.getPaleta());
        atualizar(17120);
        assertEquals(CoreografiaCafe.Paleta.PETROLEO, cena.getPaleta());
        assertEquals(1.09f, cena.getZoom(), .00001f);
        for (int n = 0; n < 3; n++) {
            motor.pressionar(17500 + n * 500);
            motor.soltar();
            atualizar(17600 + n * 500);
            assertEquals(
                    n == 0
                            ? CoreografiaCafe.Paleta.PESSEGO
                            : n == 1 ? CoreografiaCafe.Paleta.LILAS : CoreografiaCafe.Paleta.SALVIA,
                    cena.getPaleta());
            assertEquals(1f + (2 - n) * .03f, cena.getZoom(), .00001f);
        }
    }

    @Test
    public void velocidadeDasTransicoesAcompanhaOIntervaloDosBips() {
        atualizar(56015);
        float rapido = cena.getMistura();
        atualizar(16030);
        assertEquals(rapido, cena.getMistura(), .0001f);
        atualizar(56020);
        float zoomRapido = cena.getZoom();
        atualizar(16040);
        assertEquals(zoomRapido, cena.getZoom(), .0001f);
    }

    @Test
    public void pretoEBrancoDuraExatamenteEnquantoSpriteEstaEmMiss() {
        motor.processarPerdidas(25210);
        atualizar(25230);
        assertEquals(CoreografiaCafe.Paleta.CINZA, cena.getPaleta());
        assertEquals(1, cena.getMistura(), 0);
        for (long t = 25210; t < 25760; t += 13) {
            atualizar(t);
            assertEquals(CoreografiaCafe.Cliente.ERRA, cena.getCliente());
            assertEquals(CoreografiaCafe.Paleta.CINZA, cena.getPaleta());
        }
        atualizar(25760);
        assertNotEquals(CoreografiaCafe.Cliente.ERRA, cena.getCliente());
        assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
        motor.processarPerdidas(57210);
        atualizar(57220);
        assertEquals(CoreografiaCafe.Paleta.CINZA, cena.getPaleta());
        motor.pressionar(57250);
        motor.soltar();
        atualizar(57280);
        assertEquals(CoreografiaCafe.Paleta.LILAS, cena.getPaleta());
        cena.atualizar(57290, motor, 57285, Julgamento.MISS);
        assertEquals(CoreografiaCafe.Paleta.CINZA, cena.getPaleta());
        cena.atualizar(57835, motor, 57285, Julgamento.MISS);
        assertNotEquals(CoreografiaCafe.Cliente.ERRA, cena.getCliente());
        assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
        cena.atualizar(57450, motor, 57285, Julgamento.MISS);
        assertEquals(CoreografiaCafe.Paleta.CINZA, cena.getPaleta());
        motor.pressionar(57500);
        motor.soltar();
        cena.atualizar(57501, motor, 57500, Julgamento.PERFECT);
        assertEquals(CoreografiaCafe.Cliente.PEGA, cena.getCliente());
        assertEquals(CoreografiaCafe.Paleta.SALVIA, cena.getPaleta());
    }

    @Test
    public void perderNaoGeraCorDeAcertoEAcertarForaDeTrioNaoMudaCor() {
        motor.pressionar(25000);
        motor.soltar();
        atualizar(25030);
        assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
        motor.processarPerdidas(57210);
        atualizar(57760);
        assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
    }

    @Test
    public void semToquesCameraSeguraUltimoZoomAteTerminarJanelaDoTrio() {
        for (long inicio : new long[] {16000, 56000}) {
            long intervalo = inicio == 16000 ? 500 : 250;
            long terceiroBip = inicio + 2 * intervalo;
            atualizar(terceiroBip + intervalo / 5 - 1);
            assertEquals(1.09f, cena.getZoom(), .00001f);
            for (int n = 0; n < 3; n++) {
                long resposta = terceiroBip + 500 + n * intervalo;
                motor.processarPerdidas(resposta + 199);
                atualizar(resposta + 199);
                assertEquals(1.09f, cena.getZoom(), .00001f);
            }
            long fim = terceiroBip + 500 + 2 * intervalo + 200;
            atualizar(fim);
            assertEquals(1.09f, cena.getZoom(), .00001f);
            atualizar(fim + intervalo * 4 / 5);
            assertEquals(1, cena.getZoom(), 0);
        }
    }

    @Test
    public void toqueTardioIniciaZoomOutNoInstanteFisicoENaoNoAlvo() {
        atualizar(57100);
        assertEquals(1.09f, cena.getZoom(), .00001f);
        motor.pressionar(57130);
        motor.soltar();
        atualizar(57130);
        assertEquals(1.09f, cena.getZoom(), .00001f);
        atualizar(57180);
        assertEquals(1.06f, cena.getZoom(), .00001f);
        atualizar(57700);
        assertEquals(1.06f, cena.getZoom(), .00001f);
        atualizar(57900);
        assertEquals(1, cena.getZoom(), 0);
    }

    @Test
    public void toqueMissNaRespostaTambemAfastaMasNaoGeraCorDeAcerto() {
        motor.pressionar(17500);
        motor.soltar();
        atualizar(17600);
        assertEquals(1.06f, cena.getZoom(), .00001f);
        assertEquals(Julgamento.MISS, motor.pressionar(17750).julgamento);
        motor.soltar();
        cena.atualizar(17850, motor, 17750, Julgamento.MISS);
        assertEquals(1.03f, cena.getZoom(), .00001f);
        assertEquals(CoreografiaCafe.Paleta.CINZA, cena.getPaleta());
        atualizar(18690);
        assertEquals(1.03f, cena.getZoom(), .00001f);
    }

    @Test
    public void recriarMantemDegrauParcialEInstanteRealDoToque() {
        motor.pressionar(57130);
        motor.soltar();
        atualizar(57150);
        MotorRecepcao outroMotor = new MotorRecepcao();
        outroMotor.restaurar(motor.salvar());
        outroMotor.restaurarToquesTrio(motor.salvarToquesTrio());
        CoreografiaCafe outra = new CoreografiaCafe();
        outra.atualizar(57150, outroMotor, -10000, null);
        assertEquals(cena.getZoom(), outra.getZoom(), 0);
    }

    @Test
    public void recuperarMantemSpriteMissEPretoEBrancoMesmoAposRecriar() {
        motor.pressionar(17750);
        motor.soltar();
        MotorRecepcao restaurado = new MotorRecepcao();
        restaurado.restaurar(motor.salvar());
        restaurado.restaurarRecuperacao(motor.getBloqueadoAteMs());
        CoreografiaCafe outra = new CoreografiaCafe();
        outra.atualizar(18000, restaurado, -10000, null);
        assertEquals(CoreografiaCafe.Cliente.ERRA, outra.getCliente());
        assertEquals(CoreografiaCafe.Paleta.CINZA, outra.getPaleta());
        outra.atualizar(18300, restaurado, -10000, null);
        assertNotEquals(CoreografiaCafe.Cliente.ERRA, outra.getCliente());
        assertNotEquals(CoreografiaCafe.Paleta.CINZA, outra.getPaleta());
    }

    @Test
    public void mesmoTempoMantemCameraPaletaEPoseAoPausarOuRecriar() {
        atualizar(37300);
        float pulso = cena.getPulso(), foco = cena.getFoco(), mistura = cena.getMistura();
        CoreografiaCafe outra = new CoreografiaCafe();
        outra.atualizar(37300, motor, -10000, null);
        assertEquals(pulso, outra.getPulso(), 0);
        assertEquals(foco, outra.getFoco(), 0);
        assertEquals(mistura, outra.getMistura(), 0);
        assertEquals(cena.getMika(), outra.getMika());
        assertEquals(cena.getPaleta(), outra.getPaleta());
    }

    @Test
    public void despedidaSoComecaAposPedidosESemZoomOuNovaPaleta() {
        atualizar(74999);
        assertEquals(CoreografiaCafe.Mika.ESPERA, cena.getMika());
        atualizar(75000);
        assertEquals(CoreografiaCafe.Mika.DESPEDIDA, cena.getMika());
        assertEquals(1, cena.getZoom(), 0);
        assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
        motor.processarPerdidas(76000);
        atualizar(76600);
        assertEquals(CoreografiaCafe.Mika.DESPEDIDA, cena.getMika());
        assertEquals(1, cena.getZoom(), 0);
        assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
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
