package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import com.example.beatdays.game.TreinoCafe;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;
import com.example.beatdays.ui.game.support.GestosTeste;

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
public class TreinoInstrumentedTest {
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
    public void botaoFisicoRepeteLicaoPreservaPausaEEntraNaFaseSemPontosDoTreino()
            throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar(R.id.btn_treinar);
            esperarRetorno(11600);
            etapa(TreinoCafe.Etapa.DUPLA, 0);
            pontos(0, 0);
            buscar(13250);
            avancar(15600);
            esperarRetorno(11600);
            etapa(TreinoCafe.Etapa.DUPLA, 1);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                assertNull(motor().getJulgamento(id(13000)));
                                assertNull(motor().getJulgamento(id(13500)));
                                assertEquals(
                                        atividade.get().getString(R.string.treino_repetir_dupla),
                                        ((TextView) atividade.get().findViewById(R.id.tv_instrucao))
                                                .getText()
                                                .toString());
                            });
            salvarImagem("beatdays-treino-repetir.png");
            tocar(R.id.btn_pausar);
            long pausa = posicao();
            scenario.recreate();
            scenario.onActivity(atividade::set);
            esperarPosicao((int) pausa);
            etapa(TreinoCafe.Etapa.DUPLA, 1);
            pontos(0, 0);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                atividade.get().findViewById(R.id.btn_pausar).performClick();
                                audio().pausar();
                            });
            buscar(13030);
            tocar();
            buscar(13530);
            tocar();
            pontos(0, 0);
            buscar(15600);
            etapa(TreinoCafe.Etapa.TRIO, 0);
            buscar(17530);
            tocar();
            buscar(18250);
            avancar(19600);
            esperarRetorno(15600);
            etapa(TreinoCafe.Etapa.TRIO, 1);
            pontos(0, 0);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                assertEquals(Julgamento.PERFECT, motor().getJulgamento(id(13000)));
                                assertEquals(Julgamento.PERFECT, motor().getJulgamento(id(13500)));
                                for (long t : new long[] {17500, 18000, 18500}) {
                                    assertNull(motor().getJulgamento(id(t)));
                                    assertEquals(-1, motor().getToqueTrio(id(t)));
                                }
                            });
            for (int t : new int[] {17530, 18030, 18530}) {
                buscar(t);
                tocar();
            }
            buscar(19600);
            etapa(TreinoCafe.Etapa.CONCLUIDO, 0);
            pontos(0, 0);
            buscar(21030);
            tocar();
            pontos(100, 1);
        }
    }

    @Test
    public void toqueNormalJogaDiretoSemRepeticaoObrigatoria() {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            tocar();
            buscar(19600);
            etapa(TreinoCafe.Etapa.INATIVO, 0);
            pontos(0, 0);
            buscar(21030);
            tocar();
            pontos(100, 1);
        }
    }

    @Test
    public void pausarDuranteBuscaRespeitaAjusteERecriacaoSemRetomarAudio() throws Exception {
        InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getSharedPreferences("beatdays_ajustes", Context.MODE_PRIVATE)
                .edit()
                .putInt("atraso_cena_ms", 100)
                .commit();
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(atividade::set);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                atividade.get().findViewById(R.id.btn_treinar).performClick();
                                atividade.get().findViewById(R.id.btn_pausar).performClick();
                                try {
                                    Field f = MainActivity.class.getDeclaredField("buscandoTreino");
                                    f.setAccessible(true);
                                    assertTrue(f.getBoolean(atividade.get()));
                                } catch (ReflectiveOperationException e) {
                                    throw new AssertionError(e);
                                }
                            });
            scenario.recreate();
            scenario.onActivity(atividade::set);
            esperarPosicao(11700);
            etapa(TreinoCafe.Etapa.DUPLA, 0);
            pontos(0, 0);
            SystemClock.sleep(200);
            esperarPosicao(11700);
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                assertFalse(audio().estaTocando());
                                assertEquals(
                                        atividade.get().getString(R.string.continuar),
                                        atividade
                                                .get()
                                                .findViewById(R.id.btn_pausar)
                                                .getContentDescription());
                                assertEquals(
                                        View.GONE,
                                        atividade
                                                .get()
                                                .findViewById(R.id.tv_instrucao)
                                                .getVisibility());
                            });
        }
    }

    private TreinoCafe treino() {
        try {
            Field f = MainActivity.class.getDeclaredField("treino");
            f.setAccessible(true);
            return (TreinoCafe) f.get(atividade.get());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private void etapa(TreinoCafe.Etapa etapa, int repeticoes) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            assertEquals(etapa, treino().getEtapa());
                            assertEquals(repeticoes, treino().getRepeticoes());
                        });
    }

    private void avancar(int tempo) {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            audio().pausar();
                            audio().definirPosicaoMs(tempo);
                        });
    }

    private void esperarRetorno(int alvo) {
        long limite = SystemClock.uptimeMillis() + 6000;
        AtomicLong pronto = new AtomicLong();
        while (SystemClock.uptimeMillis() < limite) {
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                try {
                                    Field f = MainActivity.class.getDeclaredField("buscandoTreino");
                                    f.setAccessible(true);
                                    long tempo = audio().getPosicaoAtualMs();
                                    if (!f.getBoolean(atividade.get())
                                            && tempo >= alvo - 30
                                            && tempo <= alvo + 800) {
                                        audio().pausar();
                                        pronto.set(1);
                                    }
                                } catch (ReflectiveOperationException e) {
                                    throw new AssertionError(e);
                                }
                            });
            if (pronto.get() == 1) {
                return;
            }
            SystemClock.sleep(20);
        }
        fail("A lição não retornou a " + alvo + ", posição=" + posicao());
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
        esperarPosicao(tempo);
    }

    private void esperarPosicao(int tempo) {
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
        AtomicReference<Bitmap> captura = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            View tela = atividade.get().getWindow().getDecorView();
                            Bitmap bitmap =
                                    Bitmap.createBitmap(
                                            tela.getWidth(),
                                            tela.getHeight(),
                                            Bitmap.Config.ARGB_8888);
                            tela.draw(new Canvas(bitmap));
                            captura.set(bitmap);
                        });
        Bitmap bitmap = captura.get();
        assertNotNull(bitmap);
        File destino = new File(atividade.get().getExternalFilesDir(null), nome);
        try (FileOutputStream stream = new FileOutputStream(destino)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        bitmap.recycle();
    }
}
