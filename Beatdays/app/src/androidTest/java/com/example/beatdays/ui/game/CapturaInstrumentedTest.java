package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
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
import com.example.beatdays.ui.game.animation.CoreografiaCafe;
import com.example.beatdays.ui.game.sprite.SpriteImageView;
import com.example.beatdays.ui.game.support.GestosTeste;
import com.example.beatdays.ui.game.support.PosesTeste;
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
public class CapturaInstrumentedTest {
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
    public void capturaExigeToqueReabreMaoEntreAcertosEPreservaPausaEMiss() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(20000);
            buscar(20880);
            pose(CoreografiaCafe.Cliente.PREPARA, R.id.img_cliente_receber);
            estavel();
            assertNull(motor().getJulgamento(id(21000)));
            pontos(0, 0);
            salvarImagem("beatdays-captura-aguardando.png");
            buscar(21050);
            tocar();
            pegou(21000);
            pontos(100, 1);
            pose(CoreografiaCafe.Cliente.PEGA, R.id.img_cliente_pegar);
            estavel();
            salvarImagem("beatdays-captura-acerto.png");
            buscar(21150);
            pose(CoreografiaCafe.Cliente.PEGA, R.id.img_cliente_pegar);
            buscar(21230);
            pose(CoreografiaCafe.Cliente.PEGA, R.id.img_cliente_guardar);
            buscar(21650);
            pose(CoreografiaCafe.Cliente.ESPERA, R.id.img_cliente_pegar);
            buscar(25050);
            tocar();
            pose(CoreografiaCafe.Cliente.PEGA, R.id.img_cliente_pegar);
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            SystemClock.sleep(180);
            assertEquals(pausa, posicao(), 30);
            scenario.recreate();
            scenario.onActivity(atividade::set);
            SystemClock.sleep(200);
            pose(CoreografiaCafe.Cliente.PEGA, R.id.img_cliente_pegar);
            pontos(200, 2);
            tocar(R.id.btn_pausar);
            buscar(25550);
            tocar();
            pegou(25500);
            pontos(300, 3);
            pose(CoreografiaCafe.Cliente.PEGA, R.id.img_cliente_pegar);
            buscar(26000);
            tocar();
            pose(CoreografiaCafe.Cliente.ERRA, R.id.img_cliente_erro);
            scenario.onActivity(
                    a ->
                            assertEquals(
                                    CoreografiaCafe.Paleta.CINZA,
                                    ((CenaCafe) a.findViewById(R.id.cena_cafe)).getPaleta()));
            salvarImagem("beatdays-captura-miss.png");
            buscar(27700);
            pose(CoreografiaCafe.Cliente.ESPERA, R.id.img_cliente_caminhar_a);
            estavel();
            buscar(32000);
            buscar(32900);
            pose(CoreografiaCafe.Cliente.PREPARA, R.id.img_cliente_receber);
            assertNull(motor().getJulgamento(id(33000)));
            buscar(33250);
            pose(CoreografiaCafe.Cliente.ERRA, R.id.img_cliente_erro);
            buscar(55000);
            for (int n = 0; n < 3; n++) {
                buscar(57050 + n * 250);
                tocar();
                pegou(57000 + n * 250);
                pose(CoreografiaCafe.Cliente.PEGA, R.id.img_cliente_pegar);
                salvarImagem("beatdays-captura-trio-" + (n + 1) + ".png");
                buscar(57290 + n * 250);
                pose(CoreografiaCafe.Cliente.PEGA, R.id.img_cliente_receber);
            }
            pontos(600, 3);
        }
    }

    private void pose(CoreografiaCafe.Cliente estado, int visivel) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            PosesTeste.cliente(atividade.get());
                            CenaCafe cena = atividade.get().findViewById(R.id.cena_cafe);
                            assertEquals(estado, cena.getEstadoCliente());
                            SpriteImageView idle = atividade.get().findViewById(R.id.img_cliente);
                            float ancora = idle.getLeft() + idle.getWidth() * idle.getAncoraPes();
                            for (int id :
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
                                SpriteImageView sprite = atividade.get().findViewById(id);
                                assertTrue(sprite.getAlpha() >= 0 && sprite.getAlpha() <= 1);
                                if (visivel != R.id.img_cliente
                                        && visivel != R.id.img_cliente_receber
                                        && visivel != R.id.img_cliente_desapontado) {
                                    assertEquals(id == visivel ? 1f : 0f, sprite.getAlpha(), 0);
                                }
                                assertEquals(idle.getTop(), sprite.getTop());
                                assertEquals(idle.getBottom(), sprite.getBottom());
                                assertEquals(
                                        ancora,
                                        sprite.getLeft()
                                                + sprite.getWidth() * sprite.getAncoraPes(),
                                        1f);
                                assertTrue(
                                        cena.indexOfChild(sprite)
                                                > cena.indexOfChild(
                                                        atividade
                                                                .get()
                                                                .findViewById(
                                                                        R.id.img_balcao_frente)));
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
