package com.example.beatdays.game;

import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;

import java.util.ArrayList;
import java.util.List;

/** Cada pressão recebe um alimento; segurar e soltar nunca capturam automaticamente. */
public final class MotorRecepcao {
    public static final long RECUPERACAO_MISS_MS = 550;

    public static final class Resultado {
        public final EntregaCafe entrega;
        public final Julgamento julgamento;
        public final boolean consumido;

        Resultado(EntregaCafe entrega, Julgamento julgamento, boolean consumido) {
            this.entrega = entrega;
            this.julgamento = julgamento;
            this.consumido = consumido;
        }
    }

    private final List<EntregaCafe> entregas;
    private final MotorRitmo ritmo = new MotorRitmo();
    private final Julgamento[] resultados;
    private final long[] instantes;
    private final long[] toquesTrio;
    private boolean pressionado;
    private long bloqueadoAteMs = -1;

    public MotorRecepcao() {
        this(RoteiroRecepcao.criarEntregas());
    }

    public MotorRecepcao(List<EntregaCafe> chart) {
        entregas = java.util.Collections.unmodifiableList(new ArrayList<>(chart));
        resultados = new Julgamento[entregas.size()];
        instantes = new long[entregas.size()];
        toquesTrio = new long[entregas.size()];
        reiniciar();
    }

    public void reiniciar() {
        List<Long> tempos = new ArrayList<>();
        for (EntregaCafe e : entregas) {
            tempos.add(e.tempoMs);
        }
        ritmo.carregarBatidas(tempos);
        pressionado = false;
        bloqueadoAteMs = -1;
        for (EntregaCafe e : entregas) {
            resultados[e.id] = null;
            instantes[e.id] = -1;
            toquesTrio[e.id] = -1;
            if (e.demonstracao) {
                ritmo.marcarComoPerdida(e.id);
            }
        }
    }

    /** Rearma somente uma lição, preservando os outros resultados e o chart pontuável. */
    public boolean reiniciarPedidoTreino(int pedido) {
        boolean encontrado = false;
        for (EntregaCafe e : entregas) {
            if (e.pedido == pedido) {
                if (e.demonstracao || e.tempoMs >= RoteiroRecepcao.INICIO_PONTUACAO_MS) {
                    return false;
                }
                encontrado = true;
            }
        }
        if (!encontrado) {
            return false;
        }
        boolean[] processadas = ritmo.getEstadoBatidas();
        List<Long> tempos = new ArrayList<>();
        for (EntregaCafe e : entregas) {
            tempos.add(e.tempoMs);
            if (e.pedido == pedido) {
                processadas[e.id] = false;
                resultados[e.id] = null;
                instantes[e.id] = -1;
                toquesTrio[e.id] = -1;
            }
        }
        ritmo.carregarBatidas(tempos);
        ritmo.restaurarEstadoBatidas(processadas);
        pressionado = false;
        bloqueadoAteMs = -1;
        return true;
    }

    public Resultado pressionar(long tempo) {
        if (pressionado) {
            return null;
        }
        pressionado = true;
        if (estaEmRecuperacao(tempo)) {
            return null;
        }
        registrarToqueTrio(tempo);
        boolean[] processadas = ritmo.getEstadoBatidas();
        int id = -1;
        long distancia = MotorRitmo.getJanelaGoodMs() + 1;
        for (EntregaCafe e : entregas) {
            if (processadas[e.id]) {
                continue;
            }
            long d = Math.abs(tempo - e.tempoMs);
            if (d < distancia) {
                distancia = d;
                id = e.id;
            }
        }
        if (id < 0) {
            bloqueadoAteMs = tempo + RECUPERACAO_MISS_MS;
            return new Resultado(null, Julgamento.MISS, false);
        }
        Julgamento julgamento = ritmo.registrarNoIndice(id, tempo);
        resultados[id] = julgamento;
        instantes[id] = tempo;
        if (julgamento == Julgamento.MISS) {
            bloqueadoAteMs = tempo + RECUPERACAO_MISS_MS;
        }
        return new Resultado(entregas.get(id), julgamento, true);
    }

    public void soltar() {
        pressionado = false;
    }

    public boolean estaEmRecuperacao(long tempo) {
        return bloqueadoAteMs >= 0 && tempo < bloqueadoAteMs;
    }

    public long getBloqueadoAteMs() {
        return bloqueadoAteMs;
    }

    public long getUltimoErroToqueMs() {
        return bloqueadoAteMs < 0 ? -10000 : bloqueadoAteMs - RECUPERACAO_MISS_MS;
    }

    public void restaurarRecuperacao(long prazo) {
        bloqueadoAteMs =
                prazo >= RECUPERACAO_MISS_MS
                                && prazo <= RoteiroRecepcao.DURACAO_MS + RECUPERACAO_MISS_MS
                        ? prazo
                        : -1;
    }

    private void registrarToqueTrio(long tempo) {
        for (int i = 0; i + 2 < entregas.size(); i++) {
            EntregaCafe primeira = entregas.get(i), ultima = entregas.get(i + 2);
            if (primeira.quantidade != 3
                    || primeira.demonstracao
                    || i > 0 && entregas.get(i - 1).pedido == primeira.pedido) {
                continue;
            }
            if (tempo < primeira.tempoMs - MotorRitmo.getJanelaGoodMs()
                    || tempo > ultima.tempoMs + MotorRitmo.getJanelaGoodMs()) {
                continue;
            }
            for (int n = 0; n < 3; n++) {
                if (toquesTrio[i + n] < 0) {
                    toquesTrio[i + n] = tempo;
                    return;
                }
            }
            return;
        }
    }

    public long getToqueTrio(int id) {
        return toquesTrio[id];
    }

    public long[] salvarToquesTrio() {
        return toquesTrio.clone();
    }

    public void restaurarToquesTrio(long[] estado) {
        if (estado == null || estado.length != toquesTrio.length) {
            return;
        }
        for (int i = 0; i + 2 < entregas.size(); i++) {
            EntregaCafe primeira = entregas.get(i), ultima = entregas.get(i + 2);
            if (primeira.quantidade != 3
                    || primeira.demonstracao
                    || i > 0 && entregas.get(i - 1).pedido == primeira.pedido) {
                continue;
            }
            long anterior = primeira.tempoMs - MotorRitmo.getJanelaGoodMs();
            for (int n = 0; n < 3; n++) {
                long t = estado[i + n];
                if (t < anterior || t > ultima.tempoMs + MotorRitmo.getJanelaGoodMs()) {
                    break;
                }
                toquesTrio[i + n] = t;
                anterior = t;
            }
        }
    }

    public List<Resultado> processarPerdidas(long tempo) {
        List<Resultado> perdas = new ArrayList<>();
        for (EntregaCafe e : entregas) {
            if (!e.demonstracao
                    && resultados[e.id] == null
                    && tempo > e.tempoMs + MotorRitmo.getJanelaGoodMs()) {
                ritmo.marcarComoPerdida(e.id);
                resultados[e.id] = Julgamento.MISS;
                instantes[e.id] = tempo;
                perdas.add(new Resultado(e, Julgamento.MISS, true));
            }
        }
        return perdas;
    }

    public List<EntregaCafe> getEntregas() {
        return entregas;
    }

    public Julgamento getJulgamento(int id) {
        return resultados[id];
    }

    public long getInstante(int id) {
        return instantes[id];
    }

    public long[] salvarInstantes() {
        return instantes.clone();
    }

    /** Preserva o início real das animações; snapshots antigos mantêm o horário nominal. */
    public void restaurarInstantes(long[] estado) {
        if (estado == null || estado.length != instantes.length) {
            return;
        }
        for (EntregaCafe e : entregas) {
            Julgamento j = resultados[e.id];
            long instante = estado[e.id];
            if (e.demonstracao
                    || j == null
                    || instante < 0
                    || instante > RoteiroRecepcao.DURACAO_MS) {
                continue;
            }
            if (j == Julgamento.MISS
                    ? instante > e.tempoMs + MotorRitmo.getJanelaGoodMs()
                    : Math.abs(instante - e.tempoMs) <= MotorRitmo.getJanelaGoodMs()) {
                instantes[e.id] = instante;
            }
        }
    }

    public int[] salvar() {
        int[] estado = new int[resultados.length];
        for (int i = 0; i < estado.length; i++) {
            estado[i] = resultados[i] == null ? 0 : resultados[i].ordinal() + 1;
        }
        return estado;
    }

    public void restaurar(int[] estado) {
        reiniciar();
        if (estado == null || estado.length != resultados.length) {
            return;
        }
        for (EntregaCafe e : entregas) {
            int valor = estado[e.id];
            if (!e.demonstracao && valor > 0 && valor <= Julgamento.values().length) {
                resultados[e.id] = Julgamento.values()[valor - 1];
                instantes[e.id] = e.tempoMs;
                ritmo.marcarComoPerdida(e.id);
            }
        }
    }
}
