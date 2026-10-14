package com.example.beatdays.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.SystemClock;

public class ControladorAudio {
    private MediaPlayer mediaPlayer;
    private int recursoPreparado;

    public int getRecursoPreparado() {
        return recursoPreparado;
    }

    private boolean iniciarAposBusca;
    private Runnable aoConcluirBusca;

    public void preparar(Context context, int recursoAudio) {
        liberar();
        AudioAttributes atributos =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
        mediaPlayer = MediaPlayer.create(context, recursoAudio, atributos, 0);
        if (mediaPlayer == null) {
            throw new IllegalStateException("Não foi possível abrir o áudio da fase.");
        }
        recursoPreparado = recursoAudio;
        mediaPlayer.setVolume(.84f, .84f);
        mediaPlayer.setOnSeekCompleteListener(
                mp -> {
                    if (iniciarAposBusca) {
                        iniciarAposBusca = false;
                        mp.start();
                    }
                    if (aoConcluirBusca != null) {
                        aoConcluirBusca.run();
                    }
                });
    }

    public void iniciar() {
        if (mediaPlayer == null) {
            return;
        }
        pausar();
        iniciarAposBusca = true;
        mediaPlayer.seekTo(0);
    }

    public void pausar() {
        iniciarAposBusca = false;
        if (estaTocando()) {
            mediaPlayer.pause();
        }
    }

    public void continuar() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    public void definirPosicaoMs(int posicao) {
        iniciarAposBusca = false;
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(Math.max(0, Math.min(posicao, getDuracaoMs())));
        }
    }

    public int getPosicaoAtualMs() {
        return mediaPlayer == null ? 0 : mediaPlayer.getCurrentPosition();
    }

    /** Julga o instante físico do toque, descontando o atraso na fila da interface. */
    public long getPosicaoNoEventoMs(long instanteEventoUptimeMs) {
        long posicao = getPosicaoAtualMs();
        long atraso = Math.max(0, SystemClock.uptimeMillis() - instanteEventoUptimeMs);
        return Math.max(0, posicao - atraso);
    }

    public int getDuracaoMs() {
        return mediaPlayer == null ? 0 : mediaPlayer.getDuration();
    }

    public boolean estaTocando() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void parar() {
        if (mediaPlayer == null) {
            return;
        }
        pausar();
        mediaPlayer.seekTo(0);
    }

    public void definirAoFinalizar(Runnable acao) {
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> acao.run());
        }
    }

    /** Permite reconstruir a cena pausada depois que a busca assíncrona termina. */
    public void definirAoConcluirBusca(Runnable acao) {
        aoConcluirBusca = acao;
    }

    public void liberar() {
        iniciarAposBusca = false;
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.setOnSeekCompleteListener(null);
            mediaPlayer.release();
            mediaPlayer = null;
        }
        aoConcluirBusca = null;
    }
}
