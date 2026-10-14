package com.example.beatdays.ui.editor;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beatdays.R;
import com.example.beatdays.audio.ControladorAudio;
import com.example.beatdays.ui.common.TelaImersiva;

import java.util.ArrayList;
import java.util.List;

public class EditorBatidasActivity extends AppCompatActivity {
    private ControladorAudio controladorAudio;
    private final List<Long> batidasMarcadas = new ArrayList<>();
    private TextView tvQuantidade;
    private TextView tvUltimaBatida;
    private TextView tvChart;
    private Button btnIniciarMusica;
    private Button btnMarcar;
    private Button btnDesfazer;
    private boolean musicaIniciada = false;
    private boolean musicaPausada = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor_batidas);
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        TelaImersiva.configurar(this, findViewById(R.id.tela_editor));
        configurarComponentes();
        configurarAudio();
        configurarEventos();
        if (savedInstanceState != null) {
            long[] tempos = savedInstanceState.getLongArray("batidas");
            if (tempos != null) {
                for (long tempo : tempos) {
                    batidasMarcadas.add(tempo);
                }
            }
            musicaPausada = savedInstanceState.getBoolean("pausada");
            controladorAudio.definirPosicaoMs(savedInstanceState.getInt("posicao"));
            tvChart.setText(savedInstanceState.getString("chart", ""));
            if (musicaPausada) {
                btnIniciarMusica.setText(R.string.continuar);
            }
            atualizarInterface();
        }
    }

    private void configurarComponentes() {
        tvQuantidade = findViewById(R.id.tv_quantidade);
        tvUltimaBatida = findViewById(R.id.tv_ultima_batida);
        tvChart = findViewById(R.id.tv_chart);
        btnIniciarMusica = findViewById(R.id.btn_iniciar_musica);
        btnMarcar = findViewById(R.id.btn_marcar);
        btnDesfazer = findViewById(R.id.btn_desfazer);
    }

    private void configurarAudio() {
        controladorAudio = new ControladorAudio();
        controladorAudio.preparar(this, R.raw.cafe_rush_final);
        controladorAudio.definirAoFinalizar(
                () -> {
                    musicaIniciada = false;
                    btnMarcar.setEnabled(false);
                    btnIniciarMusica.setEnabled(true);
                    btnIniciarMusica.setText(R.string.iniciar_musica);
                });
    }

    private void configurarEventos() {
        btnIniciarMusica.setOnClickListener(v -> iniciarMusica());
        btnMarcar.setOnClickListener(v -> marcarBatida());
        btnDesfazer.setOnClickListener(v -> desfazerUltimaBatida());
        findViewById(R.id.btn_reiniciar).setOnClickListener(v -> reiniciarEditor());
        findViewById(R.id.btn_gerar_chart).setOnClickListener(v -> gerarChart());
    }

    private void iniciarMusica() {
        if (musicaPausada) {
            musicaPausada = false;
            musicaIniciada = true;
            controladorAudio.continuar();
            btnMarcar.setEnabled(true);
            btnIniciarMusica.setEnabled(false);
            return;
        }
        batidasMarcadas.clear();
        atualizarInterface();
        controladorAudio.iniciar();
        musicaIniciada = true;
        btnMarcar.setEnabled(true);
        btnIniciarMusica.setEnabled(false);
        tvChart.setText("");
    }

    private void marcarBatida() {
        if (!musicaIniciada) {
            return;
        }
        long tempoAtual = controladorAudio.getPosicaoAtualMs();
        batidasMarcadas.add(tempoAtual);
        atualizarInterface();
    }

    private void desfazerUltimaBatida() {
        if (batidasMarcadas.isEmpty()) {
            return;
        }
        batidasMarcadas.remove(batidasMarcadas.size() - 1);
        atualizarInterface();
    }

    private void reiniciarEditor() {
        controladorAudio.parar();
        batidasMarcadas.clear();
        musicaIniciada = false;
        musicaPausada = false;
        btnIniciarMusica.setText(R.string.iniciar_musica);
        btnMarcar.setEnabled(false);
        btnIniciarMusica.setEnabled(true);
        tvChart.setText("");
        atualizarInterface();
    }

    private void atualizarInterface() {
        tvQuantidade.setText(getString(R.string.batidas_marcadas, batidasMarcadas.size()));
        if (batidasMarcadas.isEmpty()) {
            tvUltimaBatida.setText(getString(R.string.ultima_vazia));
            btnDesfazer.setEnabled(false);
        } else {
            long ultima = batidasMarcadas.get(batidasMarcadas.size() - 1);
            tvUltimaBatida.setText(getString(R.string.ultima_batida, ultima));
            btnDesfazer.setEnabled(true);
        }
    }

    private void gerarChart() {
        if (batidasMarcadas.isEmpty()) {
            tvChart.setText(getString(R.string.sem_batidas));
            return;
        }
        StringBuilder resultado = new StringBuilder();
        resultado.append("Arrays.asList(\n");
        for (int i = 0; i < batidasMarcadas.size(); i++) {
            resultado.append("    ").append(batidasMarcadas.get(i)).append("L");
            if (i < batidasMarcadas.size() - 1) {
                resultado.append(",");
            }
            resultado.append("\n");
        }
        resultado.append(")");
        tvChart.setText(resultado.toString());
    }

    @Override
    protected void onPause() {
        if (musicaIniciada) {
            controladorAudio.pausar();
            musicaIniciada = false;
            musicaPausada = true;
            btnMarcar.setEnabled(false);
            btnIniciarMusica.setEnabled(true);
            btnIniciarMusica.setText(R.string.continuar);
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle estado) {
        long[] tempos = new long[batidasMarcadas.size()];
        for (int i = 0; i < tempos.length; i++) {
            tempos[i] = batidasMarcadas.get(i);
        }
        estado.putLongArray("batidas", tempos);
        estado.putBoolean("pausada", musicaIniciada || musicaPausada);
        estado.putInt("posicao", controladorAudio.getPosicaoAtualMs());
        estado.putString("chart", tvChart.getText().toString());
        super.onSaveInstanceState(estado);
    }

    @Override
    public void onWindowFocusChanged(boolean temFoco) {
        super.onWindowFocusChanged(temFoco);
        if (temFoco) {
            TelaImersiva.ocultarBarras(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        controladorAudio.liberar();
    }
}
