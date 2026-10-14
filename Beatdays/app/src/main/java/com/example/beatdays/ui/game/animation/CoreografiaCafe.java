package com.example.beatdays.ui.game.animation;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.game.RoteiroRecepcao;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;

/** Direção visual no relógio da trilha. Não consome nem julga entradas. */
public final class CoreografiaCafe {
    public enum Cliente {
        ESPERA,
        PREPARA,
        PEGA,
        ERRA
    }

    public enum Mika {
        ESPERA,
        PREPARA,
        LANCAR,
        DESPEDIDA
    }

    public enum Paleta {
        NATURAL,
        MEL,
        CORAL,
        PETROLEO,
        PESSEGO,
        LILAS,
        SALVIA,
        CINZA
    }

    private static final Paleta[] ACENTOS = {
        Paleta.MEL, Paleta.CORAL, Paleta.PETROLEO, Paleta.PESSEGO, Paleta.LILAS, Paleta.SALVIA
    };
    public static final long PREPARO_MIKA_MS = 200;
    private static final long DURACAO_MISS_MS = MotorRecepcao.RECUPERACAO_MISS_MS;
    private final long[] temposCor = new long[6];
    private final Paleta[] cores = new Paleta[6];
    private Cliente cliente = Cliente.ESPERA;
    private Mika mika = Mika.ESPERA;
    private Paleta paleta = Paleta.NATURAL;
    private Paleta paletaAnterior = Paleta.NATURAL;
    private float mistura, progressoCliente, progressoMika, pulso, foco, zoom = 1f;

    public void atualizar(long tempo, MotorRecepcao motor, long feedbackMs, Julgamento feedback) {
        long captura = -10000, erro = -10000, alvoCaptura = -1, alvoErro = -1;
        long lancamento = -10000;
        boolean preparaCliente = false, preparaMika = false;
        for (EntregaCafe e : motor.getEntregas()) {
            long antes = e.tempoMs - tempo;
            if (antes >= -200 && antes <= 260) {
                preparaCliente = true;
            }
            if (tempo >= e.lancamentoMs - PREPARO_MIKA_MS && tempo < e.lancamentoMs) {
                preparaMika = true;
            }
            if (tempo >= e.lancamentoMs && tempo < e.lancamentoMs + 340) {
                lancamento = Math.max(lancamento, e.lancamentoMs);
            }
            Julgamento j = motor.getJulgamento(e.id);
            if (e.demonstracao && tempo >= e.tempoMs) {
                captura = Math.max(captura, e.tempoMs);
                alvoCaptura = Math.max(alvoCaptura, e.tempoMs);
            } else if (j != null && tempo >= motor.getInstante(e.id)) {
                if (j == Julgamento.MISS) {
                    erro = Math.max(erro, motor.getInstante(e.id));
                    alvoErro = Math.max(alvoErro, e.tempoMs);
                } else {
                    captura = Math.max(captura, motor.getInstante(e.id));
                    alvoCaptura = Math.max(alvoCaptura, e.tempoMs);
                }
            }
        }
        cliente = Cliente.ESPERA;
        progressoCliente = 0;
        boolean erroAtual =
                alvoErro > alvoCaptura && tempo >= erro && tempo - erro < DURACAO_MISS_MS;
        if (feedback == Julgamento.MISS
                && feedbackMs > captura
                && tempo >= feedbackMs
                && tempo - feedbackMs < DURACAO_MISS_MS) {
            erro = feedbackMs;
            erroAtual = true;
        }
        if (motor.estaEmRecuperacao(tempo)) {
            erro = motor.getUltimoErroToqueMs();
            erroAtual = true;
        }
        if (erroAtual) {
            cliente = Cliente.ERRA;
            progressoCliente = (tempo - erro) / (float) DURACAO_MISS_MS;
        } else if (tempo >= captura && tempo - captura < 520) {
            cliente = Cliente.PEGA;
            progressoCliente = (tempo - captura) / 520f;
        } else if (preparaCliente) {
            cliente = Cliente.PREPARA;
        }
        mika = preparaMika ? Mika.PREPARA : tempo - lancamento < 340 ? Mika.LANCAR : Mika.ESPERA;
        if (tempo >= RoteiroRecepcao.FIM_PEDIDOS_MS) {
            mika = Mika.DESPEDIDA;
        }
        progressoMika = mika == Mika.LANCAR ? (tempo - lancamento) / 340f : 0;
        pulso = (float) Math.pow(Math.max(0, 1 - (tempo % 500) / 500f), 3);
        foco = 0;
        zoom = 1;
        paleta = paletaAnterior = Paleta.NATURAL;
        mistura = 0;
        java.util.List<EntregaCafe> entregas = motor.getEntregas();
        for (int i = 0; i + 2 < entregas.size(); i++) {
            EntregaCafe primeira = entregas.get(i);
            if (primeira.quantidade != 3
                    || i > 0 && entregas.get(i - 1).pedido == primeira.pedido) {
                continue;
            }
            EntregaCafe ultima = entregas.get(i + 2);
            long intervalo = entregas.get(i + 1).avisoMs - primeira.avisoMs;
            long inicio = primeira.avisoMs,
                    retorno = ultima.tempoMs + 200,
                    fim = retorno + intervalo * 4 / 5;
            if (tempo < inicio || tempo >= fim) {
                continue;
            }
            int toques = 0;
            for (int n = 0; n < 3; n++) {
                EntregaCafe e = entregas.get(i + n);
                foco += suave((tempo - e.avisoMs) / (intervalo * .18f));
                long saida = motor.getToqueTrio(e.id);
                if (saida >= 0 && tempo >= saida) {
                    foco -= suave((tempo - saida) / (intervalo * .18f));
                    toques++;
                }
            }
            foco = Math.max(0, foco);
            if (toques < 3) {
                foco *= 1 - suave((tempo - retorno) / (intervalo * .80f));
            }
            zoom = 1 + foco * .03f;
            int total = 0;
            for (int n = 0; n < 3; n++) {
                temposCor[total] = entregas.get(i + n).avisoMs;
                cores[total++] = ACENTOS[n];
            }
            for (int n = 0; n < 3; n++) {
                EntregaCafe e = entregas.get(i + n);
                Julgamento j = motor.getJulgamento(e.id);
                long instante = e.demonstracao ? e.tempoMs : motor.getInstante(e.id);
                if (instante <= tempo && (e.demonstracao || j != null && j != Julgamento.MISS)) {
                    temposCor[total] = instante;
                    cores[total++] = ACENTOS[3 + n];
                }
            }
            for (int n = 1; n < total; n++) {
                long instante = temposCor[n];
                Paleta cor = cores[n];
                int k = n;
                while (k > 0 && temposCor[k - 1] > instante) {
                    temposCor[k] = temposCor[k - 1];
                    cores[k] = cores[k - 1];
                    k--;
                }
                temposCor[k] = instante;
                cores[k] = cor;
            }
            long anterior = -10000;
            for (int n = 0; n < total; n++) {
                long instante = Math.max(temposCor[n], anterior + intervalo / 5);
                anterior = instante;
                if (tempo < instante) {
                    break;
                }
                paletaAnterior = paleta;
                paleta = cores[n];
                mistura = suave((tempo - instante) / (intervalo * .12f));
            }
            if (tempo >= retorno) {
                paletaAnterior = paleta;
                paleta = Paleta.NATURAL;
                mistura = suave((tempo - retorno) / (intervalo * .80f));
            }
            break;
        }
        if (cliente == Cliente.ERRA) {
            paletaAnterior = paleta;
            paleta = Paleta.CINZA;
            mistura = 1;
        }
    }

    private static float suave(float p) {
        p = Math.max(0, Math.min(1, p));
        return p * p * (3 - 2 * p);
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Mika getMika() {
        return mika;
    }

    public Paleta getPaleta() {
        return paleta;
    }

    public Paleta getPaletaAnterior() {
        return paletaAnterior;
    }

    public float getZoom() {
        return zoom;
    }

    public float getMistura() {
        return mistura;
    }

    public float getProgressoCliente() {
        return progressoCliente;
    }

    public float getProgressoMika() {
        return progressoMika;
    }

    public float getPulso() {
        return pulso;
    }

    public float getFoco() {
        return foco;
    }
}
