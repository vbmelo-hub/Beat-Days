package com.example.beatdays.ui.game.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.example.beatdays.R;
import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.game.RoteiroRecepcao;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;
import com.example.beatdays.model.ResumoAtendimento;
import com.example.beatdays.ui.game.animation.AberturaCafe;
import com.example.beatdays.ui.game.animation.AnimacaoCaptura;
import com.example.beatdays.ui.game.animation.AnimacaoQueda;
import com.example.beatdays.ui.game.animation.AtendimentoCafe;
import com.example.beatdays.ui.game.animation.CelebracaoCafe;
import com.example.beatdays.ui.game.animation.CoreografiaCafe;
import com.example.beatdays.ui.game.animation.FeedbackCafe;
import com.example.beatdays.ui.game.animation.ImpactoCaptura;
import com.example.beatdays.ui.game.animation.IndicadorPedido;
import com.example.beatdays.ui.game.animation.LegendaAvisos;
import com.example.beatdays.ui.game.animation.ReacaoMika;
import com.example.beatdays.ui.game.sprite.SpriteImageView;

/** Alimentos independentes, avisos no cenário e reação física da captura. */
public class EfeitosCafeView extends View {
    private final Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF destino = new RectF();
    private final float[] ancoras = new float[4];
    private final float[] ancoraPreparo = new float[2];
    private final float[] ancoraBolsa = new float[2];
    private final float[] ancoraCaptura = new float[2];
    private final float[] ancoraReacaoMika = new float[2];
    private final float[] pontoBalao = new float[2];
    private final Bitmap[] alimentos = new Bitmap[EntregaCafe.Alimento.values().length];
    private final Rect[] recortes = new Rect[alimentos.length];
    private final float[] apoiosNoChao = new float[alimentos.length];
    private final int creme = Color.rgb(255, 241, 216),
            marrom = Color.rgb(64, 40, 30),
            verde = Color.rgb(150, 232, 186);
    private long tempo, toqueInicio = -10000;
    private float toqueX, toqueY;
    private boolean ativo, dedo, partidaIniciada;
    private MotorRecepcao motor;
    private final FeedbackCafe feedback = new FeedbackCafe();
    private final LegendaAvisos legenda = new LegendaAvisos();
    private EntregaCafe.Aviso tipoLegenda;
    private int numeroLegenda, quantidadeLegenda;
    private String textoLegenda = "";
    private Julgamento julgamento;
    private String rotulo, dica = "";
    private int alimentosEmVoo,
            alimentosEmGuarda,
            alimentosNoChao,
            impactosAtivos,
            alimentosEmPreparo;

    public EfeitosCafeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        tinta.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        carregarAlimento(EntregaCafe.Alimento.CAFE, R.drawable.cafe);
        carregarAlimento(EntregaCafe.Alimento.CROISSANT, R.drawable.croissant);
        carregarAlimento(EntregaCafe.Alimento.PANQUECA, R.drawable.panqueca);
        carregarAlimento(EntregaCafe.Alimento.CUPCAKE, R.drawable.cupcake);
    }

    private void carregarAlimento(EntregaCafe.Alimento alimento, int recurso) {
        int indice = alimento.ordinal();
        alimentos[indice] = BitmapFactory.decodeResource(getResources(), recurso);
        recortes[indice] = recorte(alimentos[indice]);
        apoiosNoChao[indice] = apoio(alimentos[indice], recortes[indice], anguloNoChao(alimento));
    }

    private float apoio(Bitmap bitmap, Rect recorte, float angulo) {
        float seno = (float) Math.sin(Math.toRadians(angulo)),
                cosseno = (float) Math.cos(Math.toRadians(angulo));
        float centroX = recorte.exactCenterX(), centroY = recorte.exactCenterY(), base = 0;
        float bordaPixel = (Math.abs(seno) + Math.abs(cosseno)) * .5f;
        int[] linha = new int[recorte.width()];
        for (int y = recorte.top; y < recorte.bottom; y++) {
            bitmap.getPixels(linha, 0, linha.length, recorte.left, y, linha.length, 1);
            for (int x = 0; x < linha.length; x++) {
                if ((linha[x] >>> 24) > 64) {
                    float suporte =
                            (recorte.left + x + .5f - centroX) * seno
                                    + (y + .5f - centroY) * cosseno
                                    + bordaPixel;
                    base = Math.max(base, suporte);
                }
            }
        }
        return base / recorte.height();
    }

    private float alturaAlimento(EntregaCafe.Alimento alimento, float base) {
        switch (alimento) {
            case CROISSANT:
                return base * .75f;
            case PANQUECA:
                return base * .80f;
            case CUPCAKE:
                return base * .90f;
            default:
                return base;
        }
    }

    private Rect recorte(Bitmap b) {
        int l = b.getWidth(), t = b.getHeight(), r = 0, base = 0;
        int[] linha = new int[b.getWidth()];
        for (int y = 0; y < b.getHeight(); y++) {
            b.getPixels(linha, 0, linha.length, 0, y, linha.length, 1);
            for (int x = 0; x < linha.length; x++) {
                if ((linha[x] >>> 24) > 16) {
                    l = Math.min(l, x);
                    t = Math.min(t, y);
                    r = Math.max(r, x + 1);
                    base = Math.max(base, y + 1);
                }
            }
        }
        return r > l ? new Rect(l, t, r, base) : new Rect(0, 0, b.getWidth(), b.getHeight());
    }

    public void atualizar(long tempo, boolean ativo, MotorRecepcao motor) {
        this.tempo = tempo;
        this.ativo = ativo;
        this.motor = motor;
        atualizarLegenda();
        feedback.atualizar(tempo, motor);
        julgamento = feedback.getJulgamento();
        rotulo =
                julgamento == null
                        ? ""
                        : feedback.isForaDoRitmo()
                                ? getResources().getString(R.string.fora_do_ritmo)
                                : julgamento.name();
        dica =
                feedback.getDirecao() == FeedbackCafe.Direcao.CEDO
                        ? getResources().getString(R.string.toque_cedo)
                        : feedback.getDirecao() == FeedbackCafe.Direcao.TARDE
                                ? getResources().getString(R.string.toque_tarde)
                                : "";
        String descricao = dica.isEmpty() ? rotulo : rotulo + " · " + dica;
        if (!descricao.contentEquals(
                getContentDescription() == null ? "" : getContentDescription())) {
            setContentDescription(descricao);
        }
        invalidate();
    }

    public void definirPartidaIniciada(boolean iniciada) {
        if (partidaIniciada == iniciada) {
            return;
        }
        partidaIniciada = iniciada;
        atualizarLegenda();
        invalidate();
    }

    public float getAlphaAbertura() {
        return AberturaCafe.alpha(tempo, partidaIniciada);
    }

    public String getDicaTiming() {
        return dica;
    }

    public void indicarToque(float x, float y) {
        toqueX = x;
        toqueY = y;
        toqueInicio = SystemClock.uptimeMillis();
        dedo = true;
        invalidate();
    }

    public void moverToque(float x, float y) {
        toqueX = x;
        toqueY = y;
        invalidate();
    }

    public void encerrarToque() {
        dedo = false;
        invalidate();
    }

    public int getAlimentosEmPreparo() {
        return alimentosEmPreparo;
    }

    public int getAlimentosEmVoo() {
        return alimentosEmVoo;
    }

    public int getAlimentosEmGuarda() {
        return alimentosEmGuarda;
    }

    public int getAlimentosNoChao() {
        return alimentosNoChao;
    }

    public int getImpactosAtivos() {
        return impactosAtivos;
    }

    private EntregaCafe avisoAtual() {
        if (!partidaIniciada || motor == null) {
            return null;
        }
        for (EntregaCafe e : motor.getEntregas()) {
            long duracao = e.tipoAviso == EntregaCafe.Aviso.TIMER ? 160 : 300;
            if (tempo >= e.avisoMs && tempo < e.avisoMs + duracao) {
                return e;
            }
        }
        return null;
    }

    public EntregaCafe.Aviso getAvisoAtivo() {
        EntregaCafe e = avisoAtual();
        return e == null ? null : e.tipoAviso;
    }

    public int getNumeroAvisoAtivo() {
        EntregaCafe e = avisoAtual();
        if (e == null) {
            return 0;
        }
        int numero = 1;
        for (EntregaCafe anterior : motor.getEntregas()) {
            if (anterior.pedido == e.pedido && anterior.avisoMs < e.avisoMs) {
                numero++;
            }
        }
        return numero;
    }

    public String getMensagemPedido() {
        if (!(getParent() instanceof CenaCafe)) {
            return "";
        }
        AtendimentoCafe a = ((CenaCafe) getParent()).getAtendimento();
        AtendimentoCafe.Pedido pedido = a.getPedidoAtual();
        if (pedido == null) {
            return "";
        }
        switch (a.getDesfecho()) {
            case COMPLETO:
                return getResources().getString(R.string.pedido_completo);
            case PARCIAL:
                return getResources()
                        .getQuantityString(
                                R.plurals.pedido_parcial, a.getPerdidos(), a.getPerdidos());
            case PERDIDO:
                return getResources().getString(R.string.pedido_perdido);
            default:
                return getResources()
                        .getString(
                                R.string.pedido_progresso, a.getRecebidos(), pedido.itens.size());
        }
    }

    private long instanteRecebimento(EntregaCafe entrega) {
        return entrega.demonstracao ? entrega.tempoMs : motor.getInstante(entrega.id);
    }

    public int getIndiceAvisoPedido() {
        if (!(getParent() instanceof CenaCafe)) {
            return -1;
        }
        AtendimentoCafe a = ((CenaCafe) getParent()).getAtendimento();
        AtendimentoCafe.Pedido pedido = a.getPedidoAtual();
        if (pedido == null) {
            return -1;
        }
        for (int i = 0; i < pedido.itens.size(); i++) {
            if (IndicadorPedido.aviso(tempo, pedido.itens.get(i), a.getSituacao(i)) > 0) {
                return i;
            }
        }
        return -1;
    }

    public int getConfirmacoesPedido() {
        if (!(getParent() instanceof CenaCafe)) {
            return 0;
        }
        AtendimentoCafe a = ((CenaCafe) getParent()).getAtendimento();
        AtendimentoCafe.Pedido pedido = a.getPedidoAtual();
        if (pedido == null) {
            return 0;
        }
        int numero = 0;
        for (int i = 0; i < pedido.itens.size(); i++) {
            if (IndicadorPedido.confirmacao(
                            tempo, instanteRecebimento(pedido.itens.get(i)), a.getSituacao(i))
                    > 0) {
                numero++;
            }
        }
        return numero;
    }

    private void desenharPedido(Canvas c, float u, View cliente) {
        AtendimentoCafe a = ((CenaCafe) getParent()).getAtendimento();
        AtendimentoCafe.Pedido pedido = a.getPedidoAtual();
        if (pedido == null || a.getEtapa() == AtendimentoCafe.Etapa.CHEGANDO) {
            return;
        }
        ((SpriteImageView) cliente).mapearPonto(.35f, .31f, pontoBalao);
        float x = pontoBalao[0] - 138 * u, y = pontoBalao[1] - 16 * u;
        int salvo =
                c.saveLayerAlpha(
                        x - 3 * u,
                        y - 3 * u,
                        x + 139 * u,
                        y + 66 * u,
                        Math.round((1 - a.getProgressoSaida()) * 255));
        cor(marrom);
        destino.set(x, y, x + 130 * u, y + 63 * u);
        c.drawRoundRect(destino, 7 * u, 7 * u, tinta);
        c.drawCircle(x + 130 * u, y + 16 * u, 5 * u, tinta);
        cor(creme);
        tinta.setTextAlign(Paint.Align.CENTER);
        tinta.setTextSize(9 * u);
        c.drawText(getResources().getString(R.string.pedido_titulo), x + 65 * u, y + 12 * u, tinta);
        for (int i = 0; i < pedido.itens.size(); i++) {
            EntregaCafe e = pedido.itens.get(i);
            AtendimentoCafe.Situacao estado = a.getSituacao(i);
            float cx = x + 65 * u + (i - (pedido.itens.size() - 1) / 2f) * 28 * u, cy = y + 31 * u;
            float aviso = IndicadorPedido.aviso(tempo, e, estado);
            float confirmacao = IndicadorPedido.confirmacao(tempo, instanteRecebimento(e), estado);
            cor(Color.rgb(91, 61, 43));
            destino.set(cx - 12 * u, cy - 12 * u, cx + 12 * u, cy + 12 * u);
            c.drawRoundRect(destino, 6 * u, 6 * u, tinta);
            int contorno =
                    estado == AtendimentoCafe.Situacao.RECEBIDO
                            ? verde
                            : estado == AtendimentoCafe.Situacao.PERDIDO
                                    ? Color.rgb(238, 164, 148)
                                    : creme;
            cor(contorno);
            tinta.setStyle(Paint.Style.STROKE);
            tinta.setStrokeWidth(u);
            tinta.setAlpha(estado == AtendimentoCafe.Situacao.PENDENTE ? 45 : 130);
            c.drawRoundRect(destino, 6 * u, 6 * u, tinta);
            if (aviso > 0 || confirmacao > 0) {
                float intensidade = Math.max(aviso, confirmacao);
                cor(confirmacao > 0 ? verde : Color.rgb(245, 193, 111));
                tinta.setAlpha(Math.round(intensidade * 100));
                c.drawRoundRect(destino, 6 * u, 6 * u, tinta);
                tinta.setAlpha(Math.round(intensidade * 230));
                tinta.setStyle(Paint.Style.STROKE);
                tinta.setStrokeWidth(1.5f * u);
                c.drawRoundRect(destino, 6 * u, 6 * u, tinta);
            }
            Rect origem = recortes[e.alimento.ordinal()];
            float h = alturaAlimento(e.alimento, 19 * u), w = h * origem.width() / origem.height();
            if (w > 23 * u) {
                h *= 23 * u / w;
                w = 23 * u;
            }
            float escala = IndicadorPedido.escala(aviso, confirmacao);
            h *= escala;
            w *= escala;
            destino.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
            cor(creme);
            tinta.setAlpha(
                    estado == AtendimentoCafe.Situacao.PERDIDO
                            ? 90
                            : estado == AtendimentoCafe.Situacao.PENDENTE && aviso == 0
                                    ? 180
                                    : 255);
            tinta.setFilterBitmap(false);
            c.drawBitmap(alimentos[e.alimento.ordinal()], origem, destino, tinta);
            float sx = cx + 8 * u, sy = cy + 8 * u;
            if (estado == AtendimentoCafe.Situacao.RECEBIDO) {
                cor(verde);
                tinta.setStrokeWidth(2 * u);
                c.drawLine(sx - 4 * u, sy, sx - 1 * u, sy + 3 * u, tinta);
                c.drawLine(sx - 1 * u, sy + 3 * u, sx + 5 * u, sy - 4 * u, tinta);
            } else if (estado == AtendimentoCafe.Situacao.PERDIDO) {
                cor(Color.rgb(238, 164, 148));
                tinta.setStrokeWidth(2 * u);
                c.drawLine(sx - 3 * u, sy - 3 * u, sx + 3 * u, sy + 3 * u, tinta);
                c.drawLine(sx + 3 * u, sy - 3 * u, sx - 3 * u, sy + 3 * u, tinta);
            }
        }
        cor(a.getDesfecho() == AtendimentoCafe.Desfecho.COMPLETO ? verde : creme);
        tinta.setTextAlign(Paint.Align.CENTER);
        tinta.setTextSize(10 * u);
        c.drawText(getMensagemPedido(), x + 65 * u, y + 55 * u, tinta);
        c.restoreToCount(salvo);
    }

    public String getMensagemFechamento() {
        if (!ativo || motor == null || tempo < RoteiroRecepcao.FIM_PEDIDOS_MS) {
            return "";
        }
        ResumoAtendimento resumo = new ResumoAtendimento(motor);
        return getResources()
                .getString(R.string.fechamento_pedidos, resumo.getCompletos(), resumo.getTotal());
    }

    private void desenharFechamento(Canvas c, float u, View mika) {
        String mensagem = getMensagemFechamento();
        if (mensagem.isEmpty()) {
            return;
        }
        float p = Math.min(1, (tempo - RoteiroRecepcao.FIM_PEDIDOS_MS) / 400f);
        p = p * p * (3 - 2 * p);
        ((SpriteImageView) mika).mapearPonto(.36f, .24f, pontoBalao);
        float x = pontoBalao[0] - 209 * u, y = pontoBalao[1] - 23 * u + (1 - p) * 7 * u;
        int salvo =
                c.saveLayerAlpha(
                        x - 2 * u, y - 2 * u, x + 210 * u, y + 86 * u, Math.round(p * 255));
        cor(marrom);
        destino.set(x, y, x + 200 * u, y + 84 * u);
        c.drawRoundRect(destino, 9 * u, 9 * u, tinta);
        c.drawCircle(x + 200 * u, y + 23 * u, 6 * u, tinta);
        cor(creme);
        tinta.setTextAlign(Paint.Align.CENTER);
        tinta.setTextSize(13 * u);
        c.drawText(
                getResources().getString(R.string.fechamento_titulo),
                x + 100 * u,
                y + 25 * u,
                tinta);
        cor(verde);
        tinta.setTextSize(10 * u);
        c.drawText(mensagem, x + 100 * u, y + 47 * u, tinta);
        ResumoAtendimento resumo = new ResumoAtendimento(motor);
        cor(creme);
        tinta.setTextSize(10 * u);
        c.drawText(
                getResources()
                        .getString(
                                resumo.getCompletos() > 0
                                        ? R.string.fechamento_obrigada
                                        : R.string.fechamento_tente),
                x + 100 * u,
                y + 68 * u,
                tinta);
        c.restoreToCount(salvo);
    }

    private void desenharCelebracao(Canvas c, float u, View cliente) {
        float p = ((CenaCafe) getParent()).getProgressoCelebracao();
        if (p < 0) {
            return;
        }
        float brilho = CelebracaoCafe.impulso(p);
        for (int n = 0; n < 3; n++) {
            ((SpriteImageView) cliente)
                    .mapearPonto(
                            n == 0 ? .34f : n == 1 ? .65f : .70f,
                            n == 0 ? .23f : n == 1 ? .13f : .28f,
                            pontoBalao);
            float x = pontoBalao[0], y = pontoBalao[1] - 8 * u * p;
            float r = (n == 1 ? 7 : 5) * u * (.5f + .5f * brilho), espessura = r * .30f;
            cor(Color.rgb(245, 193, 111));
            tinta.setAlpha(Math.round(220 * brilho));
            c.drawRect(x - r, y - espessura, x + r, y + espessura, tinta);
            c.drawRect(x - espessura, y - r, x + espessura, y + r, tinta);
            cor(creme);
            tinta.setAlpha(Math.round(235 * brilho));
            c.drawRect(x - espessura, y - espessura, x + espessura, y + espessura, tinta);
        }
    }

    private void atualizarLegenda() {
        if (motor == null) {
            return;
        }
        legenda.atualizar(tempo, motor.getEntregas(), partidaIniciada);
        EntregaCafe.Aviso tipo = legenda.getTipo();
        int numero = legenda.getNumero(), quantidade = legenda.getQuantidade();
        if (tipo == tipoLegenda && numero == numeroLegenda && quantidade == quantidadeLegenda) {
            return;
        }
        tipoLegenda = tipo;
        numeroLegenda = numero;
        quantidadeLegenda = quantidade;
        if (tipo == null) {
            textoLegenda = "";
        } else {
            int recurso =
                    tipo == EntregaCafe.Aviso.MIKA
                            ? R.string.guia_mika
                            : tipo == EntregaCafe.Aviso.RELOGIO
                                    ? R.string.guia_relogio
                                    : R.string.guia_cronometro;
            textoLegenda = getResources().getString(recurso, numero, quantidade);
        }
    }

    public String getLegendaAviso() {
        return textoLegenda;
    }

    private void desenharLegenda(Canvas c, float u, View mika) {
        float alpha = legenda.getAlpha();
        if (textoLegenda.isEmpty() || alpha <= 0) {
            return;
        }
        tinta.setTextAlign(Paint.Align.CENTER);
        tinta.setTextSize(8.5f * u);
        float largura = tinta.measureText(textoLegenda) + 12 * u;
        ((SpriteImageView) mika).mapearPonto(.36f, .20f, pontoBalao);
        float x = pontoBalao[0] - largura / 2 - 8 * u, y = pontoBalao[1];
        int salvo =
                c.saveLayerAlpha(
                        x - largura / 2 - 2 * u,
                        y - 10 * u,
                        x + largura / 2 + 7 * u,
                        y + 9 * u,
                        Math.round(alpha * 255));
        cor(marrom);
        destino.set(x - largura / 2, y - 9 * u, x + largura / 2, y + 7 * u);
        c.drawRoundRect(destino, 3 * u, 3 * u, tinta);
        c.drawCircle(x + largura / 2, y - u, 4 * u, tinta);
        cor(creme);
        c.drawText(textoLegenda, x, y + 2 * u, tinta);
        c.restoreToCount(salvo);
    }

    private void desenharAbertura(Canvas c, float u) {
        float alpha = getAlphaAbertura();
        if (alpha <= 0) {
            return;
        }
        float x = (getWidth() - 230 * u) / 2f, y = getHeight() * .27f + (1 - alpha) * 5 * u;
        int salvo =
                c.saveLayerAlpha(
                        x - 2 * u, y - 2 * u, x + 232 * u, y + 80 * u, Math.round(alpha * 255));
        cor(marrom);
        destino.set(x, y, x + 230 * u, y + 78 * u);
        c.drawRoundRect(destino, 8 * u, 8 * u, tinta);
        cor(Color.rgb(245, 193, 111));
        tinta.setAlpha(110);
        tinta.setStyle(Paint.Style.STROKE);
        tinta.setStrokeWidth(u);
        c.drawRoundRect(destino, 8 * u, 8 * u, tinta);
        cor(Color.rgb(91, 61, 43));
        c.drawCircle(x + 37 * u, y + 39 * u, 24 * u, tinta);
        int indice = EntregaCafe.Alimento.CAFE.ordinal();
        Rect origem = recortes[indice];
        float h = 34 * u, w = h * origem.width() / origem.height();
        destino.set(x + 37 * u - w / 2, y + 39 * u - h / 2, x + 37 * u + w / 2, y + 39 * u + h / 2);
        cor(creme);
        tinta.setFilterBitmap(false);
        c.drawBitmap(alimentos[indice], origem, destino, tinta);
        cor(Color.rgb(245, 193, 111));
        tinta.setTextAlign(Paint.Align.CENTER);
        tinta.setTextSize(20 * u);
        c.drawText(getResources().getString(R.string.nome_fase), x + 145 * u, y + 35 * u, tinta);
        cor(creme);
        tinta.setTextSize(9 * u);
        c.drawText(
                getResources().getString(R.string.abertura_turno), x + 145 * u, y + 56 * u, tinta);
        c.restoreToCount(salvo);
    }

    private void desenharReacaoMika(Canvas c, float u, View mika) {
        ReacaoMika reacao = ((CenaCafe) getParent()).getReacaoMika();
        if (reacao.getTipo() == ReacaoMika.Tipo.NENHUMA) {
            return;
        }
        ((SpriteImageView) mika).mapearPonto(.64f, .14f, ancoraReacaoMika);
        float x = ancoraReacaoMika[0] + 8 * u, y = ancoraReacaoMika[1];
        float alpha = reacao.getAlpha(), p = reacao.getProgresso();
        if (reacao.getTipo() == ReacaoMika.Tipo.SUSTO) {
            cor(creme);
            tinta.setAlpha(Math.round(255 * alpha));
            c.drawCircle(x, y, 10 * u, tinta);
            c.drawCircle(x - 7 * u, y + 9 * u, 3 * u, tinta);
            cor(marrom);
            tinta.setAlpha(Math.round(255 * alpha));
            tinta.setTextAlign(Paint.Align.CENTER);
            tinta.setTextSize(17 * u);
            c.drawText("!", x, y + 6 * u, tinta);
        } else {
            for (int n = 0; n < 2; n++) {
                float sx = x + n * 10 * u,
                        sy = y - n * 10 * u - 4 * u * p,
                        r = (n == 0 ? 6 : 4) * u;
                cor(marrom);
                tinta.setAlpha(Math.round(255 * alpha));
                c.drawRect(sx - r - u, sy - 2 * u, sx + r + u, sy + 2 * u, tinta);
                c.drawRect(sx - 2 * u, sy - r - u, sx + 2 * u, sy + r + u, tinta);
                cor(Color.rgb(245, 193, 111));
                tinta.setAlpha(Math.round(255 * alpha));
                c.drawRect(sx - r, sy - u, sx + r, sy + u, tinta);
                c.drawRect(sx - u, sy - r, sx + u, sy + r, tinta);
                cor(creme);
                tinta.setAlpha(Math.round(255 * alpha));
                c.drawRect(sx - u, sy - u, sx + u, sy + u, tinta);
            }
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        alimentosEmVoo =
                alimentosEmGuarda = alimentosNoChao = impactosAtivos = alimentosEmPreparo = 0;
        if (motor == null || getWidth() == 0) {
            return;
        }
        View cena = (View) getParent();
        View mika = cena.findViewById(R.id.img_barista_lancar),
                cliente = cena.findViewById(R.id.img_cliente_receber);
        SpriteImageView mikaEmEspera = ((CenaCafe) cena).getMikaVisivel();
        SpriteImageView clienteVisivel = ((CenaCafe) cena).getClienteVisivel();
        float u = Math.min(getWidth() / 900f, getHeight() / 400f);
        ancoras[0] = mika.getWidth() * .165f;
        ancoras[1] = mika.getHeight() * .337f;
        ancoras[2] = cliente.getWidth() * .780f;
        ancoras[3] = cliente.getHeight() * .350f;
        cliente.getMatrix().mapPoints(ancoras, 2, ancoras, 2, 1);
        float xInicio = mika.getLeft() + ancoras[0], yInicio = mika.getTop() + ancoras[1];
        float xMao = cliente.getLeft() + ancoras[2], yMao = cliente.getTop() + ancoras[3];
        View preparar = cena.findViewById(R.id.img_barista_preparar);
        ancoraPreparo[0] = preparar.getWidth() * .260f;
        ancoraPreparo[1] = preparar.getHeight() * .337f;
        preparar.getMatrix().mapPoints(ancoraPreparo);
        float preparoX = preparar.getLeft() + ancoraPreparo[0],
                preparoY = preparar.getTop() + ancoraPreparo[1];
        View pegando = cena.findViewById(R.id.img_cliente_pegar);
        ancoraCaptura[0] = pegando.getWidth() * .750f;
        ancoraCaptura[1] = pegando.getHeight() * .352f;
        pegando.getMatrix().mapPoints(ancoraCaptura);
        float xCaptura = pegando.getLeft() + ancoraCaptura[0];
        float yCaptura = pegando.getTop() + ancoraCaptura[1];
        View bolsa = cena.findViewById(R.id.img_cliente_guardar);
        ancoraBolsa[0] =
                bolsa.getWidth() * (((CenaCafe) cena).getClienteAtual() == 1 ? .605f : .450f);
        ancoraBolsa[1] = bolsa.getHeight() * .442f;
        bolsa.getMatrix().mapPoints(ancoraBolsa);
        float xBolsa = bolsa.getLeft() + ancoraBolsa[0], yBolsa = bolsa.getTop() + ancoraBolsa[1];
        float tamanho = getHeight() * .072f;
        desenharPedido(c, u, clienteVisivel);
        desenharCelebracao(c, u, clienteVisivel);
        desenharReacaoMika(c, u, mikaEmEspera);
        for (EntregaCafe e : motor.getEntregas()) {
            if (e.tipoAviso == EntregaCafe.Aviso.MIKA
                    && partidaIniciada
                    && tempo >= e.avisoMs
                    && tempo < e.avisoMs + 300
                    && textoLegenda.isEmpty()) {
                mikaEmEspera.mapearPonto(.36f, .20f, pontoBalao);
                float sx = pontoBalao[0] - 20 * u, sy = pontoBalao[1];
                cor(creme);
                destino.set(sx - 26 * u, sy - 18 * u, sx + 26 * u, sy + 18 * u);
                c.drawRoundRect(destino, 9 * u, 9 * u, tinta);
                cor(marrom);
                tinta.setTextSize(16 * u);
                c.drawText("♪", sx, sy + 6 * u, tinta);
            }
            if (tempo < e.lancamentoMs - CoreografiaCafe.PREPARO_MIKA_MS) {
                continue;
            }
            Julgamento j = motor.getJulgamento(e.id);
            boolean pegou = e.demonstracao ? tempo >= e.tempoMs : j != null && j != Julgamento.MISS;
            long captura = e.demonstracao ? e.tempoMs : motor.getInstante(e.id);
            float x, y, rotacao, alpha = 1, escala = 1;
            if (tempo < e.lancamentoMs) {
                long antes = e.lancamentoMs - tempo;
                float p = Math.max(0, Math.min(1, 1 - antes / 80f));
                p = p * p * (3 - 2 * p);
                x = preparoX + (xInicio - preparoX) * p;
                y = preparoY - tamanho * .30f + (yInicio - preparoY + tamanho * .30f) * p;
                alpha = Math.min(1, (CoreografiaCafe.PREPARO_MIKA_MS - antes) / 60f);
                rotacao = -4 * (1 - p);
                alimentosEmPreparo++;
            } else if (pegou && tempo >= captura) {
                long depois = tempo - captura;
                if (depois >= AnimacaoCaptura.FIM_ALIMENTO_MS) {
                    continue;
                }
                float guardar = AnimacaoCaptura.progressoGuardar(depois),
                        sumir = AnimacaoCaptura.progressoSumir(depois);
                x = xCaptura + (xBolsa - xCaptura) * guardar;
                y = yCaptura - tamanho * .3f + (yBolsa - yCaptura + tamanho * .3f) * guardar;
                escala = (1 - .80f * sumir) * ImpactoCaptura.escala(depois);
                alpha = 1 - sumir;
                if (depois >= AnimacaoCaptura.INICIO_GUARDAR_MS) {
                    alimentosEmGuarda++;
                }
                float encaixe = Math.min(1, depois / 70f);
                encaixe = 1 - (1 - encaixe) * (1 - encaixe);
                float voo = (captura - e.lancamentoMs) / 750f;
                float antesX = xInicio + (xMao - xInicio) * voo;
                float antesY =
                        yInicio
                                + (yMao - tamanho * .3f - yInicio) * voo
                                - getHeight()
                                        * .055f
                                        * (float) Math.sin(Math.min(1, voo) * Math.PI);
                x = antesX + (x - antesX) * encaixe;
                y = antesY + (y - antesY) * encaixe;
                rotacao = -8 * (float) Math.sin(guardar * Math.PI);
                float brilho = ImpactoCaptura.alpha(depois);
                if (brilho > 0) {
                    float p = depois / (float) ImpactoCaptura.DURACAO_MS;
                    cor(verde);
                    tinta.setStrokeWidth(1.5f * u);
                    tinta.setStyle(Paint.Style.STROKE);
                    tinta.setAlpha(Math.round(brilho * 180));
                    c.drawCircle(xMao, yMao - tamanho * .3f, (9 + 10 * p) * u, tinta);
                    for (int raio = 0; raio < 4; raio++) {
                        double angulo = raio * Math.PI / 2;
                        float r = (14 + 8 * p) * u;
                        c.drawLine(
                                xMao + (float) Math.cos(angulo) * r,
                                yMao - tamanho * .3f + (float) Math.sin(angulo) * r,
                                xMao + (float) Math.cos(angulo) * (r + 4 * u),
                                yMao - tamanho * .3f + (float) Math.sin(angulo) * (r + 4 * u),
                                tinta);
                    }
                    tinta.setStyle(Paint.Style.FILL);
                }
            } else if (j == Julgamento.MISS && tempo - e.tempoMs >= AnimacaoQueda.INICIO_MS) {
                long depois = tempo - e.tempoMs;
                if (depois >= AnimacaoQueda.FIM_MS) {
                    continue;
                }
                float baseInicioX = mika.getLeft() + mika.getWidth() * .165f;
                float baseInicioY = mika.getTop() + mika.getHeight() * .337f;
                float baseMaoX = cliente.getLeft() + cliente.getWidth() * .780f;
                float baseMaoY = cliente.getTop() + cliente.getHeight() * .350f - tamanho * .3f;
                float voo = 1 + AnimacaoQueda.INICIO_MS / 750f;
                float quedaX = baseInicioX + (baseMaoX - baseInicioX) * voo;
                float quedaY =
                        baseInicioY
                                + (baseMaoY - baseInicioY) * voo
                                + getHeight() * .12f * (voo - 1) * (voo - 1);
                float fimX =
                        Math.max(
                                tamanho,
                                Math.min(
                                        quedaX - getWidth() * .022f,
                                        cliente.getLeft() - tamanho * .6f));
                float chao = Math.min(getHeight() * .82f, cliente.getBottom());
                float h = alturaAlimento(e.alimento, tamanho);
                float angulo = anguloNoChao(e.alimento);
                float fimY = chao - h * apoiosNoChao[e.alimento.ordinal()];
                float p = AnimacaoQueda.progresso(depois);
                x = quedaX + (fimX - quedaX) * p;
                y = quedaY + (fimY - quedaY) * p * p;
                rotacao = -(voo - 1) * 95 + (angulo + (voo - 1) * 95) * p;
                alpha =
                        (1 - AnimacaoQueda.INICIO_MS / 900f * (1 - p))
                                * AnimacaoQueda.alpha(depois);
                if (depois < AnimacaoQueda.IMPACTO_MS) {
                    alimentosEmVoo++;
                } else {
                    alimentosNoChao++;
                    if (depois < AnimacaoQueda.FIM_PARTICULAS_MS) {
                        impactosAtivos++;
                    }
                    desenharImpacto(c, e.alimento, fimX, chao, u, depois, alpha);
                }
            } else {
                long depois = tempo - e.tempoMs;
                if (depois > 900) {
                    continue;
                }
                float p = (tempo - e.lancamentoMs) / 750f;
                x = xInicio + (xMao - xInicio) * p;
                y =
                        yInicio
                                + (yMao - tamanho * .3f - yInicio) * p
                                - getHeight() * .055f * (float) Math.sin(Math.min(1, p) * Math.PI);
                if (p > 1) {
                    y += getHeight() * .12f * (p - 1) * (p - 1);
                    alpha = Math.max(0, 1 - depois / 900f);
                }
                rotacao = p > 1 ? -(p - 1) * 95 : (float) Math.sin(p * Math.PI) * 6;
                alimentosEmVoo++;
                if (p > 0 && p <= 1) {
                    float anterior = Math.max(0, p - .05f);
                    float anteriorX = xInicio + (xMao - xInicio) * anterior;
                    float anteriorY =
                            yInicio
                                    + (yMao - tamanho * .3f - yInicio) * anterior
                                    - getHeight() * .055f * (float) Math.sin(anterior * Math.PI);
                    float dx = anteriorX - x,
                            dy = anteriorY - y,
                            distancia = (float) Math.hypot(dx, dy);
                    if (distancia > 0) {
                        dx /= distancia;
                        dy /= distancia;
                        cor(creme);
                        tinta.setAlpha(100);
                        tinta.setStrokeWidth(1.5f * u);
                        for (int linha = 0; linha < 2; linha++) {
                            float afastamento = (linha == 0 ? -1 : 1) * 3 * u;
                            float inicio = tamanho * .75f, fim = inicio + (linha == 0 ? 12 : 8) * u;
                            c.drawLine(
                                    x + dx * inicio - dy * afastamento,
                                    y + dy * inicio + dx * afastamento,
                                    x + dx * fim - dy * afastamento,
                                    y + dy * fim + dx * afastamento,
                                    tinta);
                        }
                    }
                }
            }
            int a = e.alimento.ordinal();
            Rect origem = recortes[a];
            float h = alturaAlimento(e.alimento, tamanho) * escala;
            float w = h * origem.width() / origem.height();
            c.save();
            c.rotate(rotacao, x, y);
            destino.set(x - w / 2, y - h / 2, x + w / 2, y + h / 2);
            tinta.setAlpha(Math.round(alpha * 255));
            tinta.setFilterBitmap(false);
            c.drawBitmap(alimentos[a], origem, destino, tinta);
            c.restore();
            if (j == Julgamento.MISS && tempo - e.tempoMs >= AnimacaoQueda.IMPACTO_MS) {
                desenharParticulas(
                        c,
                        e.alimento,
                        x,
                        Math.min(getHeight() * .82f, cliente.getBottom()),
                        u,
                        tempo - e.tempoMs,
                        alpha);
            }
        }
        desenharLegenda(c, u, mikaEmEspera);
        desenharAbertura(c, u);
        desenharFechamento(c, u, mikaEmEspera);
        long desde = SystemClock.uptimeMillis() - toqueInicio;
        if (dedo || desde < 180) {
            cor(creme);
            tinta.setStyle(Paint.Style.STROKE);
            tinta.setStrokeWidth(2 * u);
            tinta.setAlpha(dedo ? 150 : Math.max(0, 150 - (int) desde));
            c.drawCircle(toqueX, toqueY, (12 + Math.min(1, desde / 150f) * 8) * u, tinta);
            tinta.setStyle(Paint.Style.FILL);
            if (!dedo) {
                postInvalidateOnAnimation();
            }
        }
        if (julgamento != null) {
            cor(
                    julgamento == Julgamento.MISS
                            ? Color.rgb(235, 142, 128)
                            : julgamento == Julgamento.GOOD
                                    ? Color.rgb(245, 193, 111)
                                    : julgamento == Julgamento.GREAT ? creme : verde);
            tinta.setAlpha(Math.round(255 * feedback.getAlpha()));
            tinta.setTextSize(12 * u);
            tinta.setTextAlign(Paint.Align.CENTER);
            tinta.setShadowLayer(2 * u, 0, 1, marrom);
            clienteVisivel.mapearPonto(.70f, .245f, pontoBalao);
            float textoX = pontoBalao[0], textoY = pontoBalao[1] - (dica.isEmpty() ? 0 : 7 * u);
            c.drawText(rotulo, textoX, textoY, tinta);
            if (!dica.isEmpty()) {
                tinta.setTextSize(9 * u);
                c.drawText(dica, textoX, textoY + 13 * u, tinta);
            }
            tinta.clearShadowLayer();
        }
    }

    private float anguloNoChao(EntregaCafe.Alimento alimento) {
        switch (alimento) {
            case CAFE:
                return -78;
            case CUPCAKE:
                return -70;
            case CROISSANT:
                return -18;
            default:
                return -12;
        }
    }

    private void desenharImpacto(
            Canvas c,
            EntregaCafe.Alimento alimento,
            float x,
            float y,
            float u,
            long depois,
            float alpha) {
        float p = AnimacaoQueda.impacto(depois);
        cor(marrom);
        tinta.setAlpha(Math.round(65 * alpha));
        destino.set(x - 15 * u, y - 2 * u, x + 15 * u, y + 3 * u);
        c.drawOval(destino, tinta);
        boolean cafe = alimento == EntregaCafe.Alimento.CAFE;
        if (cafe) {
            cor(Color.rgb(111, 67, 39));
            tinta.setAlpha(Math.round(155 * alpha));
            float r = (10 + 8 * Math.min(1, p * 2)) * u;
            destino.set(x - r, y - 2 * u, x + r, y + 3 * u);
            c.drawOval(destino, tinta);
        }
    }

    private void desenharParticulas(
            Canvas c,
            EntregaCafe.Alimento alimento,
            float x,
            float y,
            float u,
            long depois,
            float alpha) {
        float p = AnimacaoQueda.impacto(depois);
        boolean cafe = alimento == EntregaCafe.Alimento.CAFE;
        if (depois >= AnimacaoQueda.FIM_PARTICULAS_MS) {
            return;
        }
        for (int n = 0; n < 5; n++) {
            float lado = (n - 2) / 2f;
            float px = x + lado * (5 + 24 * p) * u;
            float py = y - (7 + ((n % 2) + 1) * 12 * p) * (1 - p) * u;
            cor(
                    cafe
                            ? Color.rgb(139, 83, 47)
                            : alimento == EntregaCafe.Alimento.CUPCAKE && n % 2 == 0
                                    ? Color.rgb(236, 166, 155)
                                    : Color.rgb(218, 157, 91));
            tinta.setAlpha(Math.round((1 - p) * 190 * alpha));
            if (cafe) {
                c.drawCircle(px, py, 1.6f * u, tinta);
            } else {
                c.drawRect(px, py, px + 2 * u, py + 2 * u, tinta);
            }
        }
    }

    private void cor(int cor) {
        tinta.setColor(cor);
        tinta.setAlpha(255);
        tinta.setStyle(Paint.Style.FILL);
    }
}
