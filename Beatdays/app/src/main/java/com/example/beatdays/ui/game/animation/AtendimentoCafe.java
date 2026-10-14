package com.example.beatdays.ui.game.animation;

import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.game.RoteiroRecepcao;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Um cliente por pedido, reconstruído pelo relógio e pelos resultados existentes. Não julga
 * entradas.
 */
public final class AtendimentoCafe {
    public enum Etapa {
        AUSENTE,
        CHEGANDO,
        RECEBENDO,
        DESPEDIDA,
        SAINDO
    }

    public enum Desfecho {
        PENDENTE,
        COMPLETO,
        PARCIAL,
        PERDIDO
    }

    public enum Situacao {
        PENDENTE,
        RECEBIDO,
        PERDIDO
    }

    public static final class Pedido {
        public final int id;
        public final List<EntregaCafe> itens;
        public final long entradaMs, chegadaMs, saidaMs, fimMs;

        private Pedido(List<EntregaCafe> itens) {
            this.itens = Collections.unmodifiableList(new ArrayList<>(itens));
            EntregaCafe primeira = itens.get(0), ultima = itens.get(itens.size() - 1);
            id = primeira.pedido;
            entradaMs = id == 0 ? 0 : primeira.avisoMs - 400;
            chegadaMs = id == 0 ? 1500 : primeira.avisoMs;
            saidaMs = ultima.tempoMs + 750;
            fimMs = saidaMs + 300;
        }
    }

    private final List<Pedido> pedidos;
    private Pedido atual;
    private MotorRecepcao motor;
    private long tempo;
    private int recebidos, perdidos;
    private Etapa etapa = Etapa.AUSENTE;
    private Desfecho desfecho = Desfecho.PENDENTE;

    public AtendimentoCafe() {
        this(RoteiroRecepcao.criarEntregas());
    }

    public AtendimentoCafe(List<EntregaCafe> entregas) {
        List<Pedido> grupos = new ArrayList<>();
        List<EntregaCafe> itens = new ArrayList<>();
        for (EntregaCafe e : entregas) {
            if (!itens.isEmpty() && itens.get(0).pedido != e.pedido) {
                grupos.add(new Pedido(itens));
                itens.clear();
            }
            itens.add(e);
        }
        if (!itens.isEmpty()) {
            grupos.add(new Pedido(itens));
        }
        pedidos = Collections.unmodifiableList(grupos);
    }

    public void atualizar(long tempo, MotorRecepcao motor) {
        this.tempo = tempo;
        this.motor = motor;
        atual = null;
        recebidos = perdidos = 0;
        etapa = Etapa.AUSENTE;
        desfecho = Desfecho.PENDENTE;
        for (Pedido p : pedidos) {
            if (tempo >= p.entradaMs && tempo < p.fimMs) {
                atual = p;
                break;
            }
        }
        if (atual == null) {
            return;
        }
        for (int i = 0; i < atual.itens.size(); i++) {
            Situacao s = getSituacao(i);
            if (s == Situacao.RECEBIDO) {
                recebidos++;
            } else if (s == Situacao.PERDIDO) {
                perdidos++;
            }
        }
        if (recebidos + perdidos == atual.itens.size()) {
            desfecho =
                    perdidos == 0
                            ? Desfecho.COMPLETO
                            : recebidos == 0 ? Desfecho.PERDIDO : Desfecho.PARCIAL;
        }
        if (tempo < atual.chegadaMs) {
            etapa = Etapa.CHEGANDO;
        } else if (tempo >= atual.saidaMs) {
            etapa = Etapa.SAINDO;
        } else {
            etapa = desfecho == Desfecho.PENDENTE ? Etapa.RECEBENDO : Etapa.DESPEDIDA;
        }
    }

    public Situacao getSituacao(int indice) {
        EntregaCafe e = atual.itens.get(indice);
        if (e.demonstracao) {
            return tempo >= e.tempoMs ? Situacao.RECEBIDO : Situacao.PENDENTE;
        }
        Julgamento j = motor.getJulgamento(e.id);
        if (j == null || tempo < motor.getInstante(e.id)) {
            return Situacao.PENDENTE;
        }
        return j == Julgamento.MISS ? Situacao.PERDIDO : Situacao.RECEBIDO;
    }

    private static float suave(float p) {
        p = Math.max(0, Math.min(1, p));
        return p * p * (3 - 2 * p);
    }

    public float getProgressoSaida() {
        return atual == null ? 0 : suave((tempo - atual.saidaMs) / 300f);
    }

    public float getDeslocamento() {
        if (atual == null) {
            return -1;
        }
        float entrada =
                1 - suave((tempo - atual.entradaMs) / (float) (atual.chegadaMs - atual.entradaMs));
        return -Math.max(entrada, getProgressoSaida());
    }

    public List<Pedido> getPedidos() {
        return pedidos;
    }

    public Pedido getPedidoAtual() {
        return atual;
    }

    public Etapa getEtapa() {
        return etapa;
    }

    public Desfecho getDesfecho() {
        return desfecho;
    }

    public int getRecebidos() {
        return recebidos;
    }

    public int getPerdidos() {
        return perdidos;
    }
}
