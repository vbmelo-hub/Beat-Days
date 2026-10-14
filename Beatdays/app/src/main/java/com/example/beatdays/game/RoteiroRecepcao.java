package com.example.beatdays.game;

import com.example.beatdays.model.EntregaCafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Partitura de Café Rush: apresentação, conversa, respiro e clímax. 120 BPM, 80 segundos. */
public final class RoteiroRecepcao {
    public static final long BATIDA_MS = 500, DURACAO_MS = 80000;
    public static final long INICIO_TREINO_MS = 12000,
            INICIO_PONTUACAO_MS = 20000,
            FIM_PEDIDOS_MS = 75000;
    public static final int VERSAO = 5;
    private static final long[] INICIOS_MS = {
        4000, 8000, 12000, 16000, 20000, 24000, 28000, 32000, 35000, 39000, 42000, 46000, 52000,
        56000, 60000, 64000, 67000, 71500
    };
    private static final long[] INTERVALOS_MS = {
        500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 500, 250, 500, 250, 250, 500
    };
    private static final int[] QUANTIDADES = {1, 3, 2, 3, 1, 2, 3, 1, 3, 2, 3, 3, 1, 3, 2, 3, 3, 1};
    private static final EntregaCafe.Alimento[] CARDAPIO = EntregaCafe.Alimento.values();
    private static final int[] PEDIDOS_RELOGIO = {2, 5, 9, 14};

    private RoteiroRecepcao() {}

    private static EntregaCafe.Aviso tipoAviso(int pedido) {
        if (QUANTIDADES[pedido] == 3) {
            return EntregaCafe.Aviso.TIMER;
        }
        for (int p : PEDIDOS_RELOGIO) {
            if (pedido == p) {
                return EntregaCafe.Aviso.RELOGIO;
            }
        }
        return EntregaCafe.Aviso.MIKA;
    }

    public static List<EntregaCafe> criarEntregas() {
        List<EntregaCafe> entregas = new ArrayList<>();
        for (int p = 0; p < QUANTIDADES.length; p++) {
            long inicio = INICIOS_MS[p];
            long intervalo = INTERVALOS_MS[p];
            long espera = QUANTIDADES[p] > 1 ? intervalo * (QUANTIDADES[p] - 1) + BATIDA_MS : 1000;
            for (int n = 0; n < QUANTIDADES[p]; n++) {
                EntregaCafe.Alimento alimento = CARDAPIO[(p + n) % CARDAPIO.length];
                long aviso = inicio + n * intervalo;
                entregas.add(
                        new EntregaCafe(
                                entregas.size(),
                                p,
                                QUANTIDADES[p],
                                alimento,
                                aviso,
                                aviso + espera,
                                tipoAviso(p)));
            }
        }
        return Collections.unmodifiableList(entregas);
    }
}
