package com.example.beatdays.ui.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.beatdays.MainActivity;
import com.example.beatdays.R;
import com.example.beatdays.audio.ControladorAudio;
import com.example.beatdays.game.RoteiroRecepcao;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.ResultadoCafe;
import com.example.beatdays.ui.game.animation.ReacaoMika;
import com.example.beatdays.ui.game.sprite.ConjuntoSprites;
import com.example.beatdays.ui.game.sprite.QuadroSprite;
import com.example.beatdays.ui.game.view.CenaCafe;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public final class QualidadeCafeInstrumentedTest {
    private final AtomicReference<MainActivity> activity = new AtomicReference<>();

    @Before
    public void configurar() {
        InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getSharedPreferences("beatdays_ajustes", 0)
                .edit()
                .putInt("atraso_cena_ms", 0)
                .commit();
    }

    private void main(Runnable r) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            r.run();
        } else {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(r);
        }
    }

    private Object field(String name) {
        try {
            Field f = MainActivity.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(activity.get());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private ControladorAudio audio() {
        return (ControladorAudio) field("audio");
    }

    private long position() {
        AtomicLong v = new AtomicLong();
        main(() -> v.set(audio().getPosicaoAtualMs()));
        return v.get();
    }

    private void touch() {
        main(
                () -> {
                    View v = activity.get().findViewById(R.id.tela_jogo);
                    long now = SystemClock.uptimeMillis();
                    for (int action : new int[] {0, 1}) {
                        MotionEvent e =
                                MotionEvent.obtain(
                                        now,
                                        now,
                                        action,
                                        v.getWidth() * .85f,
                                        v.getHeight() * .82f,
                                        0);
                        activity.get().dispatchTouchEvent(e);
                        e.recycle();
                    }
                });
    }

    private void seek(int time) {
        main(
                () -> {
                    audio().pausar();
                    audio().definirPosicaoMs(time);
                });
        long end = SystemClock.uptimeMillis() + 5000;
        int stable = 0;
        while (SystemClock.uptimeMillis() < end) {
            stable = Math.abs(position() - time) < 25 ? stable + 1 : 0;
            if (stable >= 6) {
                return;
            }
            SystemClock.sleep(30);
        }
        fail("Busca falhou em " + time);
    }

    private Bitmap frame(View v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(b));
        return b;
    }

    private void save(String name) {
        AtomicReference<Bitmap> picture = new AtomicReference<>();
        main(() -> picture.set(frame(activity.get().getWindow().getDecorView())));
        try (FileOutputStream f =
                new FileOutputStream(new File(activity.get().getExternalFilesDir(null), name))) {
            picture.get().compress(Bitmap.CompressFormat.PNG, 100, f);
        } catch (Exception e) {
            throw new AssertionError(e);
        } finally {
            picture.get().recycle();
        }
    }

    @Test
    public void faseUnicaCarregaTrilhaFinalENaoMostraSelecaoDeFases() {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent launch = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        assertNotNull(launch);
        assertEquals(MainActivity.class.getName(), launch.getComponent().getClassName());
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity::set);
            SystemClock.sleep(500);
            main(
                    () -> {
                        assertEquals(R.raw.cafe_rush_final, audio().getRecursoPreparado());
                        assertEquals(80000, audio().getDuracaoMs(), 20);
                        assertEquals("PRONTO", field("estado").toString());
                    });
            save("beatdays-qualidade-inicio.png");
        }
    }

    @Test
    public void posesTemEscalaComumERecortesInteiros() {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ConjuntoSprites banco = new ConjuntoSprites(c.getResources());
        for (String name : new String[] {"cliente", "theo", "lia"}) {
            QuadroSprite[] poses = banco.cliente(name);
            assertEquals(8, poses.length);
            long memoria = 0;
            for (QuadroSprite pose : poses) {
                memoria += pose.getMemoriaBytes();
            }
            assertTrue("Família não deve explodir memória", memoria < 30L * 1024 * 1024);
        }
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity::set);
            SystemClock.sleep(500);
            touch();
            seek(6000);
            seek(8100);
            save("beatdays-qualidade-theo.png");
            AtomicLong width = new AtomicLong(), height = new AtomicLong();
            main(
                    () -> {
                        View v = activity.get().findViewById(R.id.img_cliente);
                        width.set(v.getWidth());
                        height.set(v.getHeight());
                        assertEquals(
                                1,
                                ((CenaCafe) activity.get().findViewById(R.id.cena_cafe))
                                        .getClienteAtual());
                    });
            seek(14000);
            seek(15000);
            seek(24050);
            main(
                    () -> {
                        View v = activity.get().findViewById(R.id.img_cliente);
                        assertEquals(width.get(), v.getWidth());
                        assertEquals(height.get(), v.getHeight());
                        assertEquals(
                                2,
                                ((CenaCafe) activity.get().findViewById(R.id.cena_cafe))
                                        .getClienteAtual());
                    });
            save("beatdays-qualidade-lia.png");
            seek(25010);
            touch();
            seek(25110);
            save("beatdays-qualidade-pegar.png");
            seek(25170);
            save("beatdays-qualidade-guardar.png");
        }
    }

    @Test
    public void sustoSempreApareceSemAlterarCabecaECorpo() {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity::set);
            SystemClock.sleep(500);
            touch();
            seek(66000);
            seek(67100);
            AtomicReference<Bitmap> normal = new AtomicReference<>();
            main(
                    () -> {
                        CenaCafe cena = activity.get().findViewById(R.id.cena_cafe);
                        cena.atualizar(67100, false);
                        normal.set(frame(activity.get().findViewById(R.id.img_barista)));
                    });
            seek(67210);
            touch();
            seek(67260);
            save("beatdays-qualidade-miss-lancamento.png");
            main(
                    () -> {
                        CenaCafe cena = activity.get().findViewById(R.id.cena_cafe);
                        assertEquals(ReacaoMika.Tipo.SUSTO, cena.getReacaoMika().getTipo());
                        assertEquals(2, cena.getQuadroMika());
                        assertEquals(
                                0f,
                                activity.get().findViewById(R.id.img_barista_lancar).getAlpha(),
                                0);
                        assertEquals(
                                1f, activity.get().findViewById(R.id.img_barista).getAlpha(), 0);
                        Bitmap erro = frame(activity.get().findViewById(R.id.img_barista));
                        Bitmap antes = normal.get();
                        assertEquals(antes.getWidth(), erro.getWidth());
                        assertEquals(antes.getHeight(), erro.getHeight());
                        int width = erro.getWidth(), height = erro.getHeight();
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                if (x >= width * .415f
                                        && x <= width * .600f
                                        && y >= height * .120f
                                        && y <= height * .275f) {
                                    continue;
                                }
                                assertEquals(
                                        "Pixel fora do rosto mudou em " + x + "," + y,
                                        antes.getPixel(x, y),
                                        erro.getPixel(x, y));
                            }
                        }
                        antes.recycle();
                        erro.recycle();
                    });
            main(() -> activity.get().findViewById(R.id.btn_pausar).performClick());
            scenario.recreate();
            scenario.onActivity(activity::set);
            SystemClock.sleep(500);
            main(
                    () ->
                            assertEquals(
                                    2,
                                    ((CenaCafe) activity.get().findViewById(R.id.cena_cafe))
                                            .getQuadroMika()));
        }
    }

    @Test
    public void partidaCompletaRecebeSomenteToquesReais() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity::set);
            SystemClock.sleep(500);
            touch();
            List<EntregaCafe> alvos = new ArrayList<>();
            for (EntregaCafe e : RoteiroRecepcao.criarEntregas()) {
                if (!e.demonstracao) {
                    alvos.add(e);
                }
            }
            int index = 0;
            long end = SystemClock.uptimeMillis() + 90000;
            while (SystemClock.uptimeMillis() < end) {
                long now = position();
                if (index < alvos.size() && now >= alvos.get(index).tempoMs + 8) {
                    touch();
                    index++;
                }
                AtomicBoolean done = new AtomicBoolean();
                main(() -> done.set(field("estado").toString().equals("RESULTADO")));
                if (done.get()) {
                    break;
                }
                SystemClock.sleep(4);
            }
            AtomicReference<String> stats = new AtomicReference<>();
            main(
                    () -> {
                        assertEquals("RESULTADO", field("estado").toString());
                        int p = (Integer) field("perfects"),
                                g = (Integer) field("greats"),
                                gd = (Integer) field("goods"),
                                m = (Integer) field("misses");
                        ResultadoCafe r = new ResultadoCafe(p, g, gd, m, 31);
                        assertTrue("Sincronia abaixo de 95%", r.getPrecisaoPercentual() >= 95);
                        stats.set(
                                "Café Rush: PERFECT "
                                        + p
                                        + ", GREAT "
                                        + g
                                        + ", GOOD "
                                        + gd
                                        + ", MISS "
                                        + m
                                        + ", precisão "
                                        + r.getPrecisaoPercentual()
                                        + "%\n");
                    });
            save("beatdays-qualidade-resultado.png");
            try (FileOutputStream f =
                    new FileOutputStream(
                            new File(
                                    activity.get().getExternalFilesDir(null),
                                    "beatdays-qualidade-partida.txt"))) {
                f.write(stats.get().getBytes("UTF-8"));
            }
        }
    }

    @Test
    public void transicoesChegamAPoseFinalSemSumirOuDuplicarCliente() throws Exception {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity::set);
            SystemClock.sleep(500);
            touch();
            seek(19000);
            seek(19820);
            seek(19910);
            seek(20100);
            main(() -> com.example.beatdays.ui.game.support.PosesTeste.cliente(activity.get()));
            seek(20132);
            main(
                    () -> {
                        float alpha = activity.get().findViewById(R.id.img_cliente).getAlpha();
                        assertTrue(alpha > 0 && alpha < 1);
                        com.example.beatdays.ui.game.support.PosesTeste.cliente(activity.get());
                    });
            seek(20180);
            main(
                    () ->
                            assertEquals(
                                    1,
                                    activity.get().findViewById(R.id.img_cliente).getAlpha(),
                                    0));
            seek(20700);
            seek(20800);
            seek(20832);
            main(
                    () -> {
                        float alpha =
                                activity.get().findViewById(R.id.img_cliente_receber).getAlpha();
                        assertTrue(alpha > 0 && alpha < 1);
                        com.example.beatdays.ui.game.support.PosesTeste.cliente(activity.get());
                    });
            seek(20880);
            main(
                    () ->
                            assertEquals(
                                    1,
                                    activity.get()
                                            .findViewById(R.id.img_cliente_receber)
                                            .getAlpha(),
                                    0));
        }
    }
}
