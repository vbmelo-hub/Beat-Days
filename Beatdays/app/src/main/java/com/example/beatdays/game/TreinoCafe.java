package com.example.beatdays.game;

import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;

/** Lições opcionais no relógio da trilha; só capturas reais aprovam a sequência. */
public final class TreinoCafe {
    public enum Etapa {
        INATIVO,
        DUPLA,
        TRIO,
        CONCLUIDO
    }

    private Etapa etapa = Etapa.INATIVO;
    private boolean falhou;
    private int repeticoes;

    public void reiniciar() {
        etapa = Etapa.INATIVO;
        falhou = false;
        repeticoes = 0;
    }

    public void iniciar() {
        reiniciar();
        etapa = Etapa.DUPLA;
    }

    public Etapa getEtapa() {
        return etapa;
    }

    public boolean estaAtivo() {
        return etapa == Etapa.DUPLA || etapa == Etapa.TRIO;
    }

    public boolean falhou() {
        return falhou;
    }

    public int getRepeticoes() {
        return repeticoes;
    }

    public long getFimMs() {
        return etapa == Etapa.DUPLA ? 15600 : 19600;
    }

    public long getInicioMs() {
        return etapa == Etapa.DUPLA ? 11600 : 15600;
    }

    private int pedido() {
        return etapa == Etapa.DUPLA ? 2 : 3;
    }

    public void registrar(MotorRecepcao.Resultado resultado) {
        if (estaAtivo()
                && resultado != null
                && resultado.julgamento == Julgamento.MISS
                && (resultado.entrega == null || resultado.entrega.pedido == pedido())) {
            falhou = true;
        }
    }

    /** Retorna o ponto de repetição, ou -1 quando a trilha pode continuar. */
    public long atualizar(long tempo, MotorRecepcao motor) {
        if (!estaAtivo() || tempo < getFimMs()) {
            return -1;
        }
        boolean completo = !falhou;
        int itens = 0;
        for (EntregaCafe e : motor.getEntregas()) {
            if (e.pedido == pedido()) {
                itens++;
                Julgamento j = motor.getJulgamento(e.id);
                completo &= j != null && j != Julgamento.MISS;
            }
        }
        completo &= itens == (etapa == Etapa.DUPLA ? 2 : 3);
        if (completo) {
            etapa = etapa == Etapa.DUPLA ? Etapa.TRIO : Etapa.CONCLUIDO;
            falhou = false;
            repeticoes = 0;
            return -1;
        }
        long inicio = getInicioMs();
        motor.reiniciarPedidoTreino(pedido());
        falhou = false;
        repeticoes = Math.min(999, repeticoes + 1);
        return inicio;
    }

    public void restaurar(int valor, boolean erro, int repeticoes) {
        etapa = valor >= 0 && valor < Etapa.values().length ? Etapa.values()[valor] : Etapa.INATIVO;
        falhou = estaAtivo() && erro;
        this.repeticoes = estaAtivo() ? Math.max(0, Math.min(999, repeticoes)) : 0;
    }
}
