package com.example.beatdays;

import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.beatdays.audio.ControladorAudio;
import com.example.beatdays.audio.SonsCafe;
import com.example.beatdays.data.PreferenciasJogo;
import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.game.RoteiroRecepcao;
import com.example.beatdays.game.TreinoCafe;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;
import com.example.beatdays.model.ResultadoCafe;
import com.example.beatdays.model.ResumoAtendimento;
import com.example.beatdays.model.ResumoTimingCafe;
import com.example.beatdays.ui.about.SobreActivity;
import com.example.beatdays.ui.common.TelaImersiva;
import com.example.beatdays.ui.game.view.CamadaToqueCafe;
import com.example.beatdays.ui.game.view.CenaCafe;

public class MainActivity extends AppCompatActivity {
    private enum Estado {
        PRONTO,
        JOGANDO,
        PAUSADO,
        RESULTADO
    }

    private Estado estado = Estado.PRONTO;
    private TextView tvScore, tvCombo, tvInstrucao, tvTitulo, tvTexto, tvDica;
    private View overlay, hud, painelSincronia;
    private boolean sincroniaExpandida;
    private PreferenciasJogo preferencias;
    private TextView tvSincronia;
    private Button btnAdiantar, btnAtrasar, btnZerar;
    private int ajusteConfiguradoMs, ajustePartidaMs;
    private ImageButton btnPausar;
    private CenaCafe cena;
    private MotorRecepcao motor;
    private final TreinoCafe treino = new TreinoCafe();
    private boolean buscandoTreino;
    private long alvoTreino;
    private ControladorAudio audio;
    private SonsCafe sons;
    private int score, combo, misses, melhorCombo;
    private int perfects, greats, goods;
    private int recorde;
    private boolean novoRecorde;
    private TextView tvAvaliacao, tvRecorde, tvPedidosResultado, tvTimingResultado;
    private int ponteiroAtivo = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable loop =
            new Runnable() {
                @Override
                public void run() {
                    if (estado != Estado.JOGANDO) {
                        return;
                    }
                    atualizarJogo();
                    handler.postDelayed(this, 16);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_main);
        preferencias = new PreferenciasJogo(this);
        TelaImersiva.configurarCena(this, findViewById(R.id.interface_jogo));
        cena = findViewById(R.id.cena_cafe);
        overlay = findViewById(R.id.overlay_jogo);
        hud = findViewById(R.id.hud);
        tvScore = findViewById(R.id.tv_score);
        tvCombo = findViewById(R.id.tv_combo);
        tvInstrucao = findViewById(R.id.tv_instrucao);
        tvTitulo = findViewById(R.id.tv_overlay_titulo);
        tvTexto = findViewById(R.id.tv_overlay_texto);
        tvDica = findViewById(R.id.tv_overlay_dica);
        btnPausar = findViewById(R.id.btn_pausar);
        motor = new MotorRecepcao();
        cena.definirMotor(motor);
        audio = new ControladorAudio();
        sons = new SonsCafe(this);
        audio.preparar(this, R.raw.cafe_rush_final);
        ((TextView) findViewById(R.id.tv_nome_fase)).setText(R.string.nome_fase);
        findViewById(R.id.btn_inicio).setOnClickListener(v -> voltarAoInicio());
        findViewById(R.id.btn_sobre)
                .setOnClickListener(
                        v -> startActivity(new android.content.Intent(this, SobreActivity.class)));
        audio.definirAoFinalizar(this::finalizarJogo);
        audio.definirAoConcluirBusca(
                () -> {
                    if (buscandoTreino) {
                        if (Math.abs(tempoAtual() - alvoTreino) > 100) {
                            return;
                        }
                        buscandoTreino = false;
                        if (estado == Estado.JOGANDO) {
                            atualizarCena(tempoAtual());
                            audio.continuar();
                        } else {
                            cena.atualizar(tempoAtual(), false);
                        }
                    } else if (estado != Estado.JOGANDO) {
                        cena.atualizar(tempoAtual(), false);
                    }
                });
        ((CamadaToqueCafe) findViewById(R.id.tela_jogo)).definirAoTocar(this::aoTocarCena);
        btnPausar.setOnClickListener(
                v -> {
                    if (estado == Estado.PAUSADO) {
                        continuarJogo();
                    } else {
                        pausarJogo();
                    }
                });
        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                if (estado == Estado.JOGANDO) {
                                    pausarJogo();
                                } else {
                                    finish();
                                }
                            }
                        });
        findViewById(R.id.btn_treinar)
                .setOnClickListener(
                        v -> {
                            if (estado == Estado.PRONTO) {
                                iniciarJogo(true);
                            }
                        });
        findViewById(R.id.btn_acao_menu)
                .setOnClickListener(
                        v -> {
                            if (estado == Estado.PAUSADO) {
                                continuarJogo();
                            } else if (estado == Estado.PRONTO || estado == Estado.RESULTADO) {
                                iniciarJogo();
                            }
                        });
        findViewById(R.id.btn_sincronia)
                .setOnClickListener(
                        v -> {
                            if (estado != Estado.PAUSADO) {
                                return;
                            }
                            sincroniaExpandida = !sincroniaExpandida;
                            painelSincronia.setVisibility(
                                    sincroniaExpandida ? View.VISIBLE : View.GONE);
                            findViewById(R.id.btn_sincronia).setSelected(sincroniaExpandida);
                            ScrollView conteudo = findViewById(R.id.menu_scroll_area);
                            if (sincroniaExpandida) {
                                painelSincronia.post(
                                        () ->
                                                painelSincronia.requestRectangleOnScreen(
                                                        new Rect(
                                                                0,
                                                                0,
                                                                painelSincronia.getWidth(),
                                                                Math.min(
                                                                        painelSincronia.getHeight(),
                                                                        conteudo.getHeight())),
                                                        true));
                            } else {
                                conteudo.post(() -> conteudo.scrollTo(0, 0));
                            }
                        });
        configurarSincronia();
        tvAvaliacao = findViewById(R.id.tv_avaliacao);
        tvRecorde = findViewById(R.id.tv_recorde);
        tvPedidosResultado = findViewById(R.id.tv_pedidos_resultado);
        tvTimingResultado = findViewById(R.id.tv_timing_resultado);
        recorde = preferencias.getRecorde();
        restaurar(savedInstanceState);
        atualizarInterface();
        cena.post(() -> cena.atualizar(tempoAtual(), false));
    }

    private void configurarSincronia() {
        painelSincronia = findViewById(R.id.painel_sincronia);
        tvSincronia = findViewById(R.id.tv_sincronia);
        btnAdiantar = findViewById(R.id.btn_adiantar_cena);
        btnAtrasar = findViewById(R.id.btn_atrasar_cena);
        btnZerar = findViewById(R.id.btn_zerar_sincronia);
        ajusteConfiguradoMs = limitarAjuste(preferencias.getAjusteMs());
        ajustePartidaMs = ajusteConfiguradoMs;
        btnAdiantar.setOnClickListener(v -> ajustarSincronia(ajusteConfiguradoMs - 10));
        btnAtrasar.setOnClickListener(v -> ajustarSincronia(ajusteConfiguradoMs + 10));
        btnZerar.setOnClickListener(v -> ajustarSincronia(0));
        findViewById(R.id.btn_recomecar_ajuste)
                .setOnClickListener(
                        v -> {
                            if (estado == Estado.PAUSADO) {
                                iniciarJogo();
                            }
                        });
        atualizarSincronia();
    }

    private static int limitarAjuste(int valor) {
        return Math.max(-250, Math.min(250, valor));
    }

    private long tempoRitmo(long posicaoAudio) {
        return Math.max(0, posicaoAudio - ajustePartidaMs);
    }

    private long tempoAtual() {
        return tempoRitmo(audio.getPosicaoAtualMs());
    }

    private void ajustarSincronia(int valor) {
        if (estado != Estado.PAUSADO) {
            return;
        }
        ajusteConfiguradoMs = limitarAjuste(valor);
        preferencias.salvarAjusteMs(ajusteConfiguradoMs);
        atualizarSincronia();
    }

    private void atualizarSincronia() {
        tvSincronia.setText(getString(R.string.sincronia_valor, ajusteConfiguradoMs));
        btnAdiantar.setEnabled(ajusteConfiguradoMs > -250);
        btnAtrasar.setEnabled(ajusteConfiguradoMs < 250);
        btnZerar.setEnabled(ajusteConfiguradoMs != 0);
    }

    private boolean aoTocarCena(View view, MotionEvent event) {
        int acao = event.getActionMasked();
        if (acao == MotionEvent.ACTION_DOWN) {
            ponteiroAtivo = event.getPointerId(0);
            if (estado == Estado.PRONTO || estado == Estado.RESULTADO) {
                iniciarJogo();
                ponteiroAtivo = -1;
                return true;
            }
            if (estado == Estado.PAUSADO) {
                continuarJogo();
                ponteiroAtivo = -1;
                return true;
            }
            if (estado == Estado.JOGANDO) {
                if (buscandoTreino) {
                    ponteiroAtivo = -1;
                    return true;
                }
                long tempo = tempoRitmo(audio.getPosicaoNoEventoMs(event.getEventTime()));
                if (treino.estaAtivo() && tempo >= treino.getFimMs()) {
                    atualizarJogo();
                    ponteiroAtivo = -1;
                    return true;
                }
                processarPerdidas(tempo);
                cena.indicarToque(event.getX(), event.getY());
                if (tempo >= RoteiroRecepcao.INICIO_TREINO_MS
                        && tempo < RoteiroRecepcao.FIM_PEDIDOS_MS) {
                    aplicarResultado(motor.pressionar(tempo), tempo);
                    atualizarCena(tempoAtual());
                } else {
                    sons.toque();
                }
            }
            return true;
        }
        if (acao == MotionEvent.ACTION_CANCEL) {
            cena.encerrarToque();
            ponteiroAtivo = -1;
            motor.soltar();
            return true;
        }
        if (acao == MotionEvent.ACTION_MOVE && ponteiroAtivo >= 0) {
            int indice = event.findPointerIndex(ponteiroAtivo);
            if (indice >= 0) {
                cena.moverToque(event.getX(indice), event.getY(indice));
            }
        }
        boolean soltaAtivo =
                acao == MotionEvent.ACTION_UP
                        || (acao == MotionEvent.ACTION_POINTER_UP
                                && event.getPointerId(event.getActionIndex()) == ponteiroAtivo);
        if (soltaAtivo && ponteiroAtivo >= 0) {
            motor.soltar();
            cena.encerrarToque();
            ponteiroAtivo = -1;
        }
        return true;
    }

    private void voltarAoInicio() {
        handler.removeCallbacks(loop);
        audio.pausar();
        motor.soltar();
        ponteiroAtivo = -1;
        treino.reiniciar();
        buscandoTreino = false;
        alvoTreino = 0;
        sincroniaExpandida = false;
        estado = Estado.PRONTO;
        score = combo = misses = melhorCombo = perfects = greats = goods = 0;
        motor.reiniciar();
        cena.reiniciar();
        audio.definirPosicaoMs(0);
        atualizarInterface();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void iniciarJogo() {
        iniciarJogo(false);
    }

    private void iniciarJogo(boolean praticar) {
        handler.removeCallbacks(loop);
        treino.reiniciar();
        buscandoTreino = false;
        alvoTreino = 0;
        motor.reiniciar();
        cena.reiniciar();
        ajustePartidaMs = ajusteConfiguradoMs;
        score = combo = misses = melhorCombo = 0;
        perfects = greats = goods = 0;
        novoRecorde = false;
        sincroniaExpandida = false;
        estado = Estado.JOGANDO;
        atualizarInterface();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (praticar) {
            treino.iniciar();
            buscarTreino(treino.getInicioMs());
        } else {
            audio.iniciar();
        }
        handler.post(loop);
    }

    private void atualizarJogo() {
        if (buscandoTreino) {
            return;
        }
        long tempo = tempoAtual();
        processarPerdidas(tempo);
        long retorno = treino.atualizar(tempo, motor);
        if (retorno >= 0) {
            buscarTreino(retorno);
            return;
        }
        atualizarCena(tempo);
    }

    private void buscarTreino(long inicio) {
        audio.pausar();
        buscandoTreino = true;
        alvoTreino = inicio;
        motor.soltar();
        ponteiroAtivo = -1;
        cena.limparFeedback();
        atualizarCena(inicio);
        audio.definirPosicaoMs((int) (inicio + ajustePartidaMs));
    }

    private void processarPerdidas(long tempo) {
        long limite = treino.estaAtivo() ? Math.min(tempo, treino.getFimMs()) : tempo;
        for (MotorRecepcao.Resultado resultado : motor.processarPerdidas(limite)) {
            aplicarResultado(resultado, limite);
        }
    }

    private void aplicarResultado(MotorRecepcao.Resultado resultado, long tempo) {
        if (resultado == null) {
            return;
        }
        treino.registrar(resultado);
        boolean pontua =
                !treino.estaAtivo()
                        && tempo >= RoteiroRecepcao.INICIO_PONTUACAO_MS
                        && (resultado.entrega == null
                                || resultado.entrega.tempoMs
                                        >= RoteiroRecepcao.INICIO_PONTUACAO_MS);
        if (pontua) {
            switch (resultado.julgamento) {
                case PERFECT:
                    perfects++;
                    score += 100;
                    combo++;
                    break;
                case GREAT:
                    greats++;
                    score += 70;
                    combo++;
                    break;
                case GOOD:
                    goods++;
                    score += 40;
                    combo++;
                    break;
                case MISS:
                    misses++;
                    combo = 0;
                    break;
            }
            melhorCombo = Math.max(melhorCombo, combo);
            atualizarPontos();
        }
        cena.reagir(resultado.entrega, resultado.julgamento, tempo);
        if (resultado.julgamento == Julgamento.MISS) {
            sons.erro();
        } else {
            sons.acerto(resultado.julgamento, resultado.entrega.alimento);
        }
    }

    private void atualizarCena(long tempo) {
        cena.atualizar(tempo, true);
        int texto = 0;
        if (tempo < 4000) {
            texto = R.string.ouca_ritmo;
        } else if (tempo < RoteiroRecepcao.INICIO_TREINO_MS) {
            texto = R.string.observe_mika;
        } else if (tempo < 16000) {
            texto = R.string.treino_receber;
        } else if (tempo < RoteiroRecepcao.INICIO_PONTUACAO_MS) {
            texto = R.string.treino_triplo;
        } else if (tempo < 22000) {
            texto = R.string.valendo;
        }
        if (treino.estaAtivo()) {
            texto =
                    treino.getEtapa() == TreinoCafe.Etapa.DUPLA
                            ? (treino.getRepeticoes() > 0
                                    ? R.string.treino_repetir_dupla
                                    : R.string.treino_receber)
                            : (treino.getRepeticoes() > 0
                                    ? R.string.treino_repetir_trio
                                    : R.string.treino_triplo);
        } else if (treino.getEtapa() == TreinoCafe.Etapa.CONCLUIDO && tempo < 22000) {
            texto = R.string.treino_aprovado;
        }
        tvInstrucao.setVisibility(texto == 0 ? View.GONE : View.VISIBLE);
        if (texto != 0) {
            tvInstrucao.setText(texto);
        }
    }

    private void pausarJogo() {
        if (estado != Estado.JOGANDO) {
            return;
        }
        audio.pausar();
        estado = Estado.PAUSADO;
        handler.removeCallbacks(loop);
        motor.soltar();
        cena.encerrarToque();
        ponteiroAtivo = -1;
        atualizarInterface();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void continuarJogo() {
        if (estado != Estado.PAUSADO) {
            return;
        }
        sincroniaExpandida = false;
        estado = Estado.JOGANDO;
        atualizarInterface();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!buscandoTreino) {
            audio.continuar();
        }
        handler.removeCallbacks(loop);
        handler.post(loop);
    }

    private void finalizarJogo() {
        if (estado == Estado.RESULTADO) {
            return;
        }
        if (buscandoTreino || treino.estaAtivo()) {
            atualizarJogo();
            return;
        }
        processarPerdidas(tempoRitmo(audio.getDuracaoMs()));
        estado = Estado.RESULTADO;
        handler.removeCallbacks(loop);
        cena.atualizar(tempoRitmo(audio.getDuracaoMs()), false);
        novoRecorde = score > recorde;
        if (novoRecorde) {
            recorde = score;
            preferencias.salvarRecorde(recorde);
        }
        motor.soltar();
        cena.encerrarToque();
        ponteiroAtivo = -1;
        atualizarInterface();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private ResultadoCafe avaliarPartida() {
        int total = 0;
        for (EntregaCafe e : motor.getEntregas()) {
            if (e.tempoMs >= RoteiroRecepcao.INICIO_PONTUACAO_MS) {
                total++;
            }
        }
        return new ResultadoCafe(perfects, greats, goods, misses, total);
    }

    private int textoClassificacao(ResultadoCafe.Classificacao classificacao) {
        switch (classificacao) {
            case PERFEITO:
                return R.string.avaliacao_perfeito;
            case EXCELENTE:
                return R.string.avaliacao_excelente;
            case BOM:
                return R.string.avaliacao_bom;
            default:
                return R.string.avaliacao_tente;
        }
    }

    private void atualizarPontos() {
        tvScore.setText(getString(R.string.score, score));
        tvCombo.setText(getString(R.string.combo, combo));
    }

    private void atualizarInterface() {
        atualizarPontos();
        ScrollView conteudo = findViewById(R.id.menu_scroll_area);
        conteudo.post(() -> conteudo.scrollTo(0, 0));
        findViewById(R.id.btn_inicio)
                .setVisibility(
                        estado == Estado.PAUSADO || estado == Estado.RESULTADO
                                ? View.VISIBLE
                                : View.GONE);
        findViewById(R.id.btn_sobre)
                .setVisibility(
                        estado == Estado.PRONTO || estado == Estado.RESULTADO
                                ? View.VISIBLE
                                : View.GONE);
        boolean jogando = estado == Estado.JOGANDO, pausado = estado == Estado.PAUSADO;
        cena.definirPartidaIniciada(jogando || pausado);
        overlay.setVisibility(jogando ? View.GONE : View.VISIBLE);
        findViewById(R.id.btn_treinar)
                .setVisibility(estado == Estado.PRONTO ? View.VISIBLE : View.GONE);
        painelSincronia.setVisibility(pausado && sincroniaExpandida ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_sincronia).setVisibility(pausado ? View.VISIBLE : View.GONE);
        findViewById(R.id.menu_guia)
                .setVisibility(estado == Estado.PRONTO ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.tv_menu_etiqueta))
                .setText(
                        pausado
                                ? R.string.menu_etiqueta_pausa
                                : estado == Estado.RESULTADO
                                        ? R.string.menu_etiqueta_resultado
                                        : R.string.menu_etiqueta_inicio);
        TextView descricao = findViewById(R.id.tv_menu_descricao);
        descricao.setVisibility(pausado || estado == Estado.RESULTADO ? View.VISIBLE : View.GONE);
        if (pausado || estado == Estado.RESULTADO) {
            descricao.setText(
                    pausado ? R.string.menu_descricao_pausa : R.string.menu_descricao_resultado);
        }
        ((Button) findViewById(R.id.btn_acao_menu))
                .setText(
                        pausado
                                ? R.string.menu_continuar
                                : estado == Estado.RESULTADO
                                        ? R.string.menu_repetir
                                        : R.string.menu_comecar);
        tvTitulo.setTextSize(25);
        boolean resultado = estado == Estado.RESULTADO;
        findViewById(R.id.menu_resultados).setVisibility(resultado ? View.VISIBLE : View.GONE);
        tvTexto.setVisibility(resultado ? View.GONE : View.VISIBLE);
        if (resultado) {
            ((TextView) findViewById(R.id.tv_menu_pontos))
                    .setText(getString(R.string.menu_valor, score));
            ((TextView) findViewById(R.id.tv_menu_combo))
                    .setText(getString(R.string.menu_valor, melhorCombo));
            int[] ids = {
                R.id.tv_menu_perfect, R.id.tv_menu_great, R.id.tv_menu_good, R.id.tv_menu_miss
            };
            String[] nomes = {"PERFECT", "GREAT", "GOOD", "MISS"};
            int[] valores = {perfects, greats, goods, misses};
            for (int i = 0; i < ids.length; i++) {
                ((TextView) findViewById(ids[i]))
                        .setText(getString(R.string.menu_julgamento, nomes[i], valores[i]));
            }
        }
        tvAvaliacao.setVisibility(resultado ? View.VISIBLE : View.GONE);
        tvPedidosResultado.setVisibility(resultado ? View.VISIBLE : View.GONE);
        if (resultado) {
            ResumoAtendimento atendimento = new ResumoAtendimento(motor);
            tvPedidosResultado.setText(
                    getString(
                            R.string.fechamento_pedidos,
                            atendimento.getCompletos(),
                            atendimento.getTotal()));
        }
        if (resultado) {
            ResultadoCafe desempenho = avaliarPartida();
            boolean tentar =
                    desempenho.getClassificacao() == ResultadoCafe.Classificacao.TENTE_NOVAMENTE;
            boolean bom = desempenho.getClassificacao() == ResultadoCafe.Classificacao.BOM;
            tvAvaliacao.setBackgroundResource(
                    tentar
                            ? R.drawable.menu_badge_tente
                            : bom ? R.drawable.menu_badge_neutro : R.drawable.menu_badge_sucesso);
            tvAvaliacao.setTextColor(
                    getColor(
                            tentar
                                    ? R.color.menu_coral
                                    : bom ? R.color.menu_honey : R.color.menu_sage));
            tvAvaliacao.setText(
                    getString(
                            R.string.avaliacao_resumo,
                            getString(textoClassificacao(desempenho.getClassificacao())),
                            desempenho.getPrecisaoPercentual()));
        }
        tvTimingResultado.setVisibility(View.GONE);
        if (resultado) {
            ResumoTimingCafe timing = new ResumoTimingCafe(motor);
            if (timing.temDesvios()) {
                int dica =
                        timing.getTendencia() == ResumoTimingCafe.Tendencia.CEDO
                                ? R.string.timing_dica_cedo
                                : timing.getTendencia() == ResumoTimingCafe.Tendencia.TARDE
                                        ? R.string.timing_dica_tarde
                                        : R.string.timing_dica_equilibrado;
                tvTimingResultado.setText(
                        getString(
                                R.string.timing_resultado,
                                timing.getCedo(),
                                timing.getTarde(),
                                getString(dica)));
                tvTimingResultado.setVisibility(View.VISIBLE);
            }
        }
        boolean mostrarRecorde = recorde > 0 && (resultado || estado == Estado.PRONTO);
        tvRecorde.setVisibility(mostrarRecorde ? View.VISIBLE : View.GONE);
        if (mostrarRecorde) {
            tvRecorde.setText(
                    getString(
                            resultado && novoRecorde
                                    ? R.string.novo_recorde_cafe
                                    : R.string.recorde_cafe,
                            recorde));
        }
        hud.setVisibility(jogando ? View.VISIBLE : View.GONE);
        btnPausar.setVisibility(jogando || pausado ? View.VISIBLE : View.GONE);
        btnPausar.setImageResource(pausado ? R.drawable.ic_play_cafe : R.drawable.ic_pause_cafe);
        btnPausar.setContentDescription(getString(pausado ? R.string.continuar : R.string.pausar));
        if (!jogando) {
            tvInstrucao.setVisibility(View.GONE);
        }
        if (estado == Estado.PRONTO) {
            tvTitulo.setText(R.string.nome_fase);
            tvTexto.setText(R.string.menu_intro);
            tvDica.setText(R.string.menu_hint_start);
        } else if (pausado) {
            tvTitulo.setText(R.string.pausado);
            tvTexto.setText(R.string.pausa_texto);
            tvDica.setText(R.string.menu_hint_pause);
        } else if (estado == Estado.RESULTADO) {
            tvTitulo.setText(R.string.turno_completo);
            tvDica.setText(R.string.menu_hint_result);
        }
        btnPausar.bringToFront();
    }

    private void restaurar(Bundle salvo) {
        if (salvo == null
                || salvo.getInt("faseId", 0) != 0
                || salvo.getInt("versaoRoteiro") != RoteiroRecepcao.VERSAO) {
            return;
        }
        sincroniaExpandida = salvo.getBoolean("sincroniaExpandida", false);
        score = salvo.getInt("score");
        combo = salvo.getInt("combo");
        misses = salvo.getInt("misses");
        melhorCombo = salvo.getInt("melhorCombo");
        novoRecorde = salvo.getBoolean("novoRecorde", false);
        motor.restaurar(salvo.getIntArray("entregasRecepcao"));
        motor.restaurarInstantes(salvo.getLongArray("instantesRecepcao"));
        perfects = salvo.getInt("perfects");
        greats = salvo.getInt("greats");
        goods = salvo.getInt("goods");
        if (!salvo.containsKey("perfects")) {
            for (EntregaCafe entrega : motor.getEntregas()) {
                if (entrega.tempoMs < RoteiroRecepcao.INICIO_PONTUACAO_MS) {
                    continue;
                }
                Julgamento julgamento = motor.getJulgamento(entrega.id);
                if (julgamento == Julgamento.PERFECT) {
                    perfects++;
                } else if (julgamento == Julgamento.GREAT) {
                    greats++;
                } else if (julgamento == Julgamento.GOOD) {
                    goods++;
                }
            }
        }
        motor.restaurarToquesTrio(salvo.getLongArray("toquesTrio"));
        motor.restaurarRecuperacao(salvo.getLong("bloqueadoAteMs", -1));
        ajustePartidaMs = limitarAjuste(salvo.getInt("ajustePartidaMs", 0));
        treino.restaurar(
                salvo.getInt("treinoEtapa", 0),
                salvo.getBoolean("treinoFalhou", false),
                salvo.getInt("treinoRepeticoes", 0));
        buscandoTreino = salvo.getBoolean("buscandoTreino", false);
        alvoTreino = salvo.getLong("alvoTreino", 0);
        audio.definirPosicaoMs(
                buscandoTreino ? (int) (alvoTreino + ajustePartidaMs) : salvo.getInt("posicao"));
        String anterior = salvo.getString("estadoCafe", Estado.PRONTO.name());
        estado = Estado.valueOf(anterior);
        if (estado == Estado.JOGANDO) {
            estado = Estado.PAUSADO;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle salvo) {
        salvo.putInt("faseId", 0);
        salvo.putString("estadoCafe", estado.name());
        salvo.putInt("score", score);
        salvo.putInt("combo", combo);
        salvo.putInt("misses", misses);
        salvo.putInt("melhorCombo", melhorCombo);
        salvo.putInt("posicao", audio.getPosicaoAtualMs());
        salvo.putInt("treinoEtapa", treino.getEtapa().ordinal());
        salvo.putBoolean("treinoFalhou", treino.falhou());
        salvo.putInt("treinoRepeticoes", treino.getRepeticoes());
        salvo.putBoolean("buscandoTreino", buscandoTreino);
        salvo.putLong("alvoTreino", alvoTreino);
        salvo.putInt("perfects", perfects);
        salvo.putInt("greats", greats);
        salvo.putInt("goods", goods);
        salvo.putBoolean("novoRecorde", novoRecorde);
        salvo.putInt("ajustePartidaMs", ajustePartidaMs);
        salvo.putBoolean("sincroniaExpandida", sincroniaExpandida);
        salvo.putInt("versaoRoteiro", RoteiroRecepcao.VERSAO);
        salvo.putIntArray("entregasRecepcao", motor.salvar());
        salvo.putLongArray("instantesRecepcao", motor.salvarInstantes());
        salvo.putLongArray("toquesTrio", motor.salvarToquesTrio());
        salvo.putLong("bloqueadoAteMs", motor.getBloqueadoAteMs());
        super.onSaveInstanceState(salvo);
    }

    @Override
    protected void onPause() {
        pausarJogo();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean foco) {
        super.onWindowFocusChanged(foco);
        if (foco) {
            TelaImersiva.ocultarBarras(this);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (audio != null) {
            audio.liberar();
        }
        if (sons != null) {
            sons.liberar();
        }
        super.onDestroy();
    }
}
