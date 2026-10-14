package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.beatdays.MainActivity;
import com.example.beatdays.R;
import com.example.beatdays.audio.ControladorAudio;
import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.game.RoteiroRecepcao;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;
import com.example.beatdays.ui.game.animation.AtendimentoCafe;
import com.example.beatdays.ui.game.animation.CoreografiaCafe;
import com.example.beatdays.ui.game.sprite.SpriteImageView;
import com.example.beatdays.ui.game.support.GestosTeste;
import com.example.beatdays.ui.game.view.CenaCafe;
import com.example.beatdays.ui.game.view.EfeitosCafeView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class AtendimentoInstrumentedTest {
    private final AtomicReference<MainActivity> atividade = new AtomicReference<>();

    @Before
    public void preparar() {
        zerarAjuste();
    }

    @After
    public void encerrar() {
        zerarAjuste();
    }

    private void zerarAjuste() {
        InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getSharedPreferences("beatdays_ajustes", Context.MODE_PRIVATE)
                .edit()
                .putInt("atraso_cena_ms", 0)
                .commit();
    }

    @Test
    public void chegadaPedidoAcertosDespedidaPausaEReacoesPorResultado() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(19000);
            buscar(19700);
            pedido(4, 0, 0, AtendimentoCafe.Desfecho.PENDENTE);
            scenario.onActivity(
                    a -> {
                        AtendimentoCafe atendimento =
                                ((CenaCafe) a.findViewById(R.id.cena_cafe)).getAtendimento();
                        assertEquals(AtendimentoCafe.Etapa.CHEGANDO, atendimento.getEtapa());
                        assertTrue(atendimento.getDeslocamento() < 0);
                    });
            buscar(20400);
            pedido(4, 0, 0, AtendimentoCafe.Desfecho.PENDENTE);
            salvarImagem("beatdays-atendimento-pedido.png");
            buscar(21050);
            tocar();
            pedido(4, 1, 0, AtendimentoCafe.Desfecho.COMPLETO);
            pontos(100, 1);
            assertEquals(atividade.get().getString(R.string.pedido_completo), mensagem());
            buscar(21650);
            scenario.onActivity(
                    a -> assertEquals(1, a.findViewById(R.id.img_cliente_pegar).getAlpha(), 0));
            salvarImagem("beatdays-atendimento-obrigada.png");
            buscar(21900);
            scenario.onActivity(
                    a -> {
                        AtendimentoCafe atendimento =
                                ((CenaCafe) a.findViewById(R.id.cena_cafe)).getAtendimento();
                        assertEquals(AtendimentoCafe.Etapa.SAINDO, atendimento.getEtapa());
                        assertTrue(atendimento.getDeslocamento() < 0);
                    });
            buscar(22100);
            ausente();
            buscar(24100);
            pedido(5, 0, 0, AtendimentoCafe.Desfecho.PENDENTE);
            assertEquals(atividade.get().getString(R.string.pedido_progresso, 0, 2), mensagem());
            buscar(25050);
            tocar();
            pedido(5, 1, 0, AtendimentoCafe.Desfecho.PENDENTE);
            pontos(200, 2);
            salvarImagem("beatdays-atendimento-dupla-primeiro.png");
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            SystemClock.sleep(180);
            assertEquals(pausa, posicao(), 30);
            scenario.recreate();
            scenario.onActivity(atividade::set);
            SystemClock.sleep(200);
            pedido(5, 1, 0, AtendimentoCafe.Desfecho.PENDENTE);
            assertEquals(atividade.get().getString(R.string.pedido_progresso, 1, 2), mensagem());
            tocar(R.id.btn_pausar);
            buscar(25550);
            tocar();
            pedido(5, 2, 0, AtendimentoCafe.Desfecho.COMPLETO);
            pontos(300, 3);
            buscar(26600);
            ausente();
            buscar(28100);
            pedido(6, 0, 0, AtendimentoCafe.Desfecho.PENDENTE);
            buscar(29550);
            tocar();
            buscar(30750);
            pedido(6, 1, 2, AtendimentoCafe.Desfecho.PARCIAL);
            pontos(400, 0);
            assertEquals(
                    atividade
                            .get()
                            .getResources()
                            .getQuantityString(R.plurals.pedido_parcial, 2, 2),
                    mensagem());
            salvarImagem("beatdays-atendimento-parcial.png");
            buscar(31310);
            scenario.onActivity(
                    a -> {
                        assertEquals(1, a.findViewById(R.id.img_cliente_desapontado).getAlpha(), 0);
                        assertNotEquals(
                                CoreografiaCafe.Paleta.CINZA,
                                ((CenaCafe) a.findViewById(R.id.cena_cafe)).getPaleta());
                    });
            salvarImagem("beatdays-atendimento-desapontada.png");
            buscar(31550);
            ausente();
            buscar(33250);
            pedido(7, 0, 1, AtendimentoCafe.Desfecho.PERDIDO);
            assertEquals(atividade.get().getString(R.string.pedido_perdido), mensagem());
            buscar(33850);
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    1, a.findViewById(R.id.img_cliente_desapontado).getAlpha(), 0));
            salvarImagem("beatdays-atendimento-perdido.png");
            buscar(55000);
            for (int n = 0; n < 3; n++) {
                buscar(57050 + n * 250);
                tocar();
                pedido(
                        13,
                        n + 1,
                        0,
                        n == 2
                                ? AtendimentoCafe.Desfecho.COMPLETO
                                : AtendimentoCafe.Desfecho.PENDENTE);
            }
            pontos(700, 3);
            salvarImagem("beatdays-atendimento-trio-completo.png");
            buscar(58400);
            pedido(13, 3, 0, AtendimentoCafe.Desfecho.COMPLETO);
            buscar(58550);
            ausente();
            buscar(60100);
            pedido(14, 0, 0, AtendimentoCafe.Desfecho.PENDENTE);
        }
    }

    private String mensagem() {
        AtomicReference<String> valor = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () ->
                                valor.set(
                                        ((EfeitosCafeView)
                                                        atividade
                                                                .get()
                                                                .findViewById(R.id.efeitos_cafe))
                                                .getMensagemPedido()));
        return valor.get();
    }

    private void pedido(int id, int recebidos, int perdidos, AtendimentoCafe.Desfecho desfecho) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            CenaCafe cena = atividade.get().findViewById(R.id.cena_cafe);
                            AtendimentoCafe atendimento = cena.getAtendimento();
                            assertNotNull(atendimento.getPedidoAtual());
                            assertEquals(id, atendimento.getPedidoAtual().id);
                            assertEquals(recebidos, atendimento.getRecebidos());
                            assertEquals(perdidos, atendimento.getPerdidos());
                            assertEquals(desfecho, atendimento.getDesfecho());
                            SpriteImageView idle = atividade.get().findViewById(R.id.img_cliente);
                            float ancora = idle.getLeft() + idle.getWidth() * idle.getAncoraPes();
                            float alphas = 0;
                            for (int recurso :
                                    new int[] {
                                        R.id.img_cliente,
                                        R.id.img_cliente_receber,
                                        R.id.img_cliente_pegar,
                                        R.id.img_cliente_guardar,
                                        R.id.img_cliente_erro,
                                        R.id.img_cliente_desapontado,
                                        R.id.img_cliente_caminhar_a,
                                        R.id.img_cliente_caminhar_b
                                    }) {
                                SpriteImageView sprite = atividade.get().findViewById(recurso);
                                alphas += sprite.getAlpha();
                                assertEquals(idle.getTop(), sprite.getTop());
                                assertEquals(idle.getBottom(), sprite.getBottom());
                                assertEquals(
                                        ancora,
                                        sprite.getLeft()
                                                + sprite.getWidth() * sprite.getAncoraPes(),
                                        1f);
                            }
                            assertEquals(1f, alphas, 0);
                        });
    }

    private void ausente() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            AtendimentoCafe atendimento =
                                    ((CenaCafe) atividade.get().findViewById(R.id.cena_cafe))
                                            .getAtendimento();
                            assertEquals(AtendimentoCafe.Etapa.AUSENTE, atendimento.getEtapa());
                            assertNull(atendimento.getPedidoAtual());
                            assertEquals(
                                    "",
                                    ((EfeitosCafeView)
                                                    atividade.get().findViewById(R.id.efeitos_cafe))
                                            .getMensagemPedido());
                            for (int recurso :
                                    new int[] {
                                        R.id.img_cliente,
                                        R.id.img_cliente_receber,
                                        R.id.img_cliente_pegar,
                                        R.id.img_cliente_guardar,
                                        R.id.img_cliente_erro,
                                        R.id.img_cliente_desapontado,
                                        R.id.img_cliente_caminhar_a,
                                        R.id.img_cliente_caminhar_b
                                    }) {
                                assertEquals(
                                        0, atividade.get().findViewById(recurso).getAlpha(), 0);
                            }
                        });
    }

    private MotorRecepcao motor() {
        try {
            Field f = MainActivity.class.getDeclaredField("motor");
            f.setAccessible(true);
            return (MotorRecepcao) f.get(atividade.get());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private int id(long tempo) {
        for (EntregaCafe e : RoteiroRecepcao.criarEntregas()) {
            if (e.tempoMs == tempo) {
                return e.id;
            }
        }
        throw new AssertionError("Captura ausente: " + tempo);
    }

    private void pegou(long tempo) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> assertEquals(Julgamento.PERFECT, motor().getJulgamento(id(tempo))));
    }

    private void aviso(int numero) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            EfeitosCafeView efeitos =
                                    atividade.get().findViewById(R.id.efeitos_cafe);
                            assertEquals(EntregaCafe.Aviso.RELOGIO, efeitos.getAvisoAtivo());
                            assertEquals(numero, efeitos.getNumeroAvisoAtivo());
                        });
    }

    private void estavel() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            CenaCafe cena = atividade.get().findViewById(R.id.cena_cafe);
                            assertEquals(1, cena.getScaleX(), 0);
                            assertEquals(1, cena.getScaleY(), 0);
                            assertEquals(0, cena.getTranslationX(), 0);
                            assertEquals(0, cena.getTranslationY(), 0);
                            assertEquals(CoreografiaCafe.Paleta.NATURAL, cena.getPaleta());
                        });
    }

    private ControladorAudio audio() {
        try {
            Field f = MainActivity.class.getDeclaredField("audio");
            f.setAccessible(true);
            return (ControladorAudio) f.get(atividade.get());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private long posicao() {
        AtomicLong tempo = new AtomicLong();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> tempo.set(audio().getPosicaoAtualMs()));
        return tempo.get();
    }

    private void buscar(int tempo) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            audio().pausar();
                            audio().definirPosicaoMs(tempo);
                        });
        long limite = SystemClock.uptimeMillis() + 4000;
        int estavel = 0;
        while (SystemClock.uptimeMillis() < limite) {
            estavel = Math.abs(posicao() - tempo) <= 30 ? estavel + 1 : 0;
            if (estavel >= 6) {
                return;
            }
            SystemClock.sleep(30);
        }
        fail("Busca não alcançou " + tempo);
    }

    private void tocar() {
        tocar(0);
    }

    private void tocar(int id) {
        GestosTeste.tocar(atividade.get(), id);
    }

    private void pontos(int score, int combo) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            MainActivity a = atividade.get();
                            assertEquals(
                                    a.getString(R.string.score, score),
                                    ((TextView) a.findViewById(R.id.tv_score))
                                            .getText()
                                            .toString());
                            assertEquals(
                                    a.getString(R.string.combo, combo),
                                    ((TextView) a.findViewById(R.id.tv_combo))
                                            .getText()
                                            .toString());
                        });
    }

    private void salvarImagem(String nome) throws Exception {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            EfeitosCafeView efeitos =
                                    atividade.get().findViewById(R.id.efeitos_cafe);
                            Bitmap alvo =
                                    Bitmap.createBitmap(
                                            efeitos.getWidth(),
                                            efeitos.getHeight(),
                                            Bitmap.Config.ARGB_8888);
                            efeitos.draw(new Canvas(alvo));
                            if (nome.contains("dois-em-voo")) {
                                assertEquals(2, efeitos.getAlimentosEmVoo());
                            }
                            alvo.recycle();
                        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(150);
        Bitmap bitmap =
                InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File destino = new File(atividade.get().getExternalFilesDir(null), nome);
        try (FileOutputStream stream = new FileOutputStream(destino)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        bitmap.recycle();
    }
}
