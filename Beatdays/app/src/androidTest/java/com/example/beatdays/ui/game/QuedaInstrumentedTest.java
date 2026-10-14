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
import android.view.View;
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
public class QuedaInstrumentedTest {
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
    public void cafePerdidoCaiRespingaCongelaNaPausaESomeDoChao() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(20000);
            buscar(21180);
            queda(1, 0, 0);
            pontos(0, 0);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(() -> assertNull(motor().getJulgamento(id(21000))));
            buscar(21250);
            perdido(21000);
            queda(1, 0, 0);
            pontos(0, 0);
            buscar(21420);
            queda(1, 0, 0);
            salvarImagem("beatdays-queda-cafe-ar.png");
            buscar(21660);
            queda(0, 1, 1);
            salvarImagem("beatdays-queda-cafe-impacto.png");
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            Bitmap antes = piso();
            SystemClock.sleep(200);
            Bitmap depois = piso();
            assertEquals(pausa, posicao(), 30);
            assertTrue("Queda deve congelar na pausa", antes.sameAs(depois));
            depois.recycle();
            scenario.recreate();
            scenario.onActivity(atividade::set);
            SystemClock.sleep(220);
            queda(0, 1, 1);
            Bitmap restaurado = piso();
            assertTrue("Impacto deve reconstruir o mesmo quadro", antes.sameAs(restaurado));
            antes.recycle();
            restaurado.recycle();
            tocar(R.id.btn_pausar);
            buscar(21890);
            queda(0, 1, 1);
            salvarImagem("beatdays-queda-cafe-saida.png");
            buscar(21950);
            queda(0, 1, 0);
            buscar(22190);
            queda(0, 0, 0);
            pontos(0, 0);
        }
    }

    @Test
    public void trioMistoSoDerrubaAlimentoPerdidoEAcertoTardioNaoCai() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(20000);
            buscar(21190);
            tocar();
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> assertEquals(Julgamento.GOOD, motor().getJulgamento(id(21000))));
            buscar(21660);
            queda(0, 0, 0);
            pontos(40, 1);
            buscar(55000);
            buscar(57050);
            tocar();
            pegou(57000);
            buscar(57490);
            perdido(57250);
            buscar(57550);
            tocar();
            pegou(57500);
            buscar(57890);
            queda(0, 1, 1);
            pontos(240, 1);
            salvarImagem("beatdays-queda-trio-misto.png");
            buscar(58450);
            queda(0, 0, 0);
            tocar();
            buscar(58520);
            queda(0, 0, 0);
            pontos(240, 0);
        }
    }

    private void perdido(long alvo) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> assertEquals(Julgamento.MISS, motor().getJulgamento(id(alvo))));
    }

    private void queda(int voo, int chao, int impactos) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            EfeitosCafeView efeitos =
                                    atividade.get().findViewById(R.id.efeitos_cafe);
                            Bitmap b =
                                    Bitmap.createBitmap(
                                            efeitos.getWidth(),
                                            efeitos.getHeight(),
                                            Bitmap.Config.ARGB_8888);
                            efeitos.draw(new Canvas(b));
                            b.recycle();
                            assertEquals(voo, efeitos.getAlimentosEmVoo());
                            assertEquals(chao, efeitos.getAlimentosNoChao());
                            assertEquals(impactos, efeitos.getImpactosAtivos());
                        });
    }

    private Bitmap piso() {
        AtomicReference<Bitmap> imagem = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            View v = atividade.get().findViewById(R.id.efeitos_cafe);
                            Bitmap inteiro =
                                    Bitmap.createBitmap(
                                            v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
                            v.draw(new Canvas(inteiro));
                            int top = Math.round(v.getHeight() * .72f);
                            imagem.set(
                                    Bitmap.createBitmap(
                                            inteiro,
                                            0,
                                            top,
                                            inteiro.getWidth(),
                                            inteiro.getHeight() - top));
                            inteiro.recycle();
                        });
        return imagem.get();
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
