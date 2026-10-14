package com.example.beatdays.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;

import com.example.beatdays.R;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Os avisos estão na trilha; apenas ações do jogador produzem este foley. */
public final class SonsCafe {
    private final SoundPool pool;
    private final Set<Integer> prontos = ConcurrentHashMap.newKeySet();
    private final int erro, toque, brilho;
    private final int[] capturas = new int[EntregaCafe.Alimento.values().length];
    private int somPendente;
    private float volumePendente, velocidadePendente;
    private long momentoPendente;

    public SonsCafe(Context context) {
        pool =
                new SoundPool.Builder()
                        .setMaxStreams(6)
                        .setAudioAttributes(
                                new AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_GAME)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .build())
                        .build();
        pool.setOnLoadCompleteListener(
                (soundPool, id, status) -> {
                    if (status != 0) {
                        return;
                    }
                    prontos.add(id);
                    if (somPendente == id) {
                        if (SystemClock.uptimeMillis() - momentoPendente < 180) {
                            tocar(id, volumePendente, velocidadePendente, false);
                        }
                        somPendente = 0;
                    }
                });
        erro = pool.load(context, R.raw.miss_recepcao, 1);
        capturas[EntregaCafe.Alimento.CAFE.ordinal()] = pool.load(context, R.raw.captura_cafe, 1);
        capturas[EntregaCafe.Alimento.CROISSANT.ordinal()] =
                pool.load(context, R.raw.captura_croissant, 1);
        capturas[EntregaCafe.Alimento.PANQUECA.ordinal()] =
                pool.load(context, R.raw.captura_panqueca, 1);
        capturas[EntregaCafe.Alimento.CUPCAKE.ordinal()] =
                pool.load(context, R.raw.captura_cupcake, 1);
        toque = pool.load(context, R.raw.toque_recepcao, 1);
        brilho = pool.load(context, R.raw.brilho_perfect, 1);
    }

    /** Retorna o stream reproduzido, ou zero se não houve captura válida ou o som ainda carrega. */
    public int acerto(Julgamento julgamento, EntregaCafe.Alimento alimento) {
        if (julgamento == null || julgamento == Julgamento.MISS || alimento == null) {
            return 0;
        }
        float volume =
                julgamento == Julgamento.PERFECT
                        ? .44f
                        : julgamento == Julgamento.GREAT ? .39f : .35f;
        int stream = tocar(capturas[alimento.ordinal()], volume, 1, true);
        if (stream != 0 && julgamento == Julgamento.PERFECT) {
            tocar(brilho, .16f, 1, false);
        }
        return stream;
    }

    public void toque() {
        tocar(toque, .16f, 1, true);
    }

    public void erro() {
        tocar(erro, .30f, 1, true);
    }

    private int tocar(int id, float volume, float velocidade, boolean resultado) {
        if (prontos.contains(id)) {
            return pool.play(id, volume, volume, resultado ? 2 : 1, 0, velocidade);
        } else if (resultado) {
            somPendente = id;
            volumePendente = volume;
            velocidadePendente = velocidade;
            momentoPendente = SystemClock.uptimeMillis();
        }
        return 0;
    }

    public void liberar() {
        somPendente = 0;
        pool.setOnLoadCompleteListener(null);
        pool.release();
        prontos.clear();
    }
}
