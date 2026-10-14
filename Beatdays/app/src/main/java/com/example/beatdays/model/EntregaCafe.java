package com.example.beatdays.model;

/** Aviso e captura usam o mesmo relógio da trilha. */
public final class EntregaCafe {
    public enum Alimento {
        CAFE,
        CROISSANT,
        PANQUECA,
        CUPCAKE
    }

    public enum Aviso {
        MIKA,
        RELOGIO,
        TIMER
    }

    public final Aviso tipoAviso;
    public final int id, pedido, quantidade;
    public final Alimento alimento;
    public final long avisoMs, lancamentoMs, tempoMs;
    public final boolean demonstracao;

    public EntregaCafe(int id, int pedido, int quantidade, Alimento alimento, long avisoMs) {
        this(id, pedido, quantidade, alimento, avisoMs, avisoMs + 1000);
    }

    public EntregaCafe(
            int id, int pedido, int quantidade, Alimento alimento, long avisoMs, long capturaMs) {
        this(
                id,
                pedido,
                quantidade,
                alimento,
                avisoMs,
                capturaMs,
                quantidade == 3 ? Aviso.TIMER : quantidade == 2 ? Aviso.RELOGIO : Aviso.MIKA);
    }

    public EntregaCafe(
            int id,
            int pedido,
            int quantidade,
            Alimento alimento,
            long avisoMs,
            long capturaMs,
            Aviso tipoAviso) {
        this.tipoAviso = tipoAviso;
        this.id = id;
        this.pedido = pedido;
        this.quantidade = quantidade;
        this.alimento = alimento;
        this.avisoMs = avisoMs;
        this.tempoMs = capturaMs;
        this.lancamentoMs = capturaMs - 750;
        this.demonstracao = avisoMs < 12000;
    }
}
