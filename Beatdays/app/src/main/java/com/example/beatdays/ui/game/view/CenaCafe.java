package com.example.beatdays.ui.game.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.beatdays.R;
import com.example.beatdays.game.MotorRecepcao;
import com.example.beatdays.game.RoteiroRecepcao;
import com.example.beatdays.model.EntregaCafe;
import com.example.beatdays.model.Julgamento;
import com.example.beatdays.ui.game.animation.AnimacaoCaptura;
import com.example.beatdays.ui.game.animation.AtendimentoCafe;
import com.example.beatdays.ui.game.animation.CelebracaoCafe;
import com.example.beatdays.ui.game.animation.CoreografiaCafe;
import com.example.beatdays.ui.game.animation.ReacaoMika;
import com.example.beatdays.ui.game.animation.TransicaoPose;
import com.example.beatdays.ui.game.sprite.ConjuntoSprites;
import com.example.beatdays.ui.game.sprite.QuadroSprite;
import com.example.beatdays.ui.game.sprite.SpriteImageView;

/** Poses, reações e direção de câmera derivadas da mesma linha de tempo da trilha. */
public class CenaCafe extends ConstraintLayout {
    private final Rect recorteBalcao = new Rect();
    private final Paint camada = new Paint();
    private final PaletasCafe paletas = new PaletasCafe();
    private final CoreografiaCafe coreografia = new CoreografiaCafe();
    private AtendimentoCafe atendimento = new AtendimentoCafe();
    private final ReacaoMika reacaoMika = new ReacaoMika();
    private final Matrix inversaCamera = new Matrix();
    private final float[] pontoToque = new float[2];
    private SpriteImageView cliente,
            receber,
            pegar,
            guardar,
            erro,
            desapontado,
            barista,
            preparar,
            lancar,
            despedida;
    private SpriteImageView caminharA, caminharB;
    private SpriteImageView[] clientes, baristas;
    private EfeitosCafeView efeitos;
    private AmbienteCafeView ambiente;
    private MotorRecepcao motor;
    private float alturaCena;
    private QuadroSprite[][] personagens;
    private android.graphics.Bitmap rostoAprova, rostoSusto;
    private int clienteAtual = 0, quadroMika = -1;
    private final TransicaoPose transicaoCliente = new TransicaoPose(8),
            transicaoMika = new TransicaoPose(4);

    public int getClienteAtual() {
        return clienteAtual;
    }

    SpriteImageView getClienteVisivel() {
        return poseVisivel(clientes);
    }

    SpriteImageView getMikaVisivel() {
        return poseVisivel(baristas);
    }

    private static SpriteImageView poseVisivel(SpriteImageView[] poses) {
        SpriteImageView atual = poses[0];
        for (SpriteImageView pose : poses) {
            if (pose.getAlpha() > atual.getAlpha()) {
                atual = pose;
            }
        }
        return atual;
    }

    public int getQuadroMika() {
        return quadroMika;
    }

    private float progressoCelebracao = -1;
    private long instanteFeedback = -10000;
    private Julgamento julgamento;

    public CenaCafe(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        cliente = findViewById(R.id.img_cliente);
        receber = findViewById(R.id.img_cliente_receber);
        erro = findViewById(R.id.img_cliente_erro);
        pegar = findViewById(R.id.img_cliente_pegar);
        desapontado = findViewById(R.id.img_cliente_desapontado);
        guardar = findViewById(R.id.img_cliente_guardar);
        caminharA = findViewById(R.id.img_cliente_caminhar_a);
        caminharB = findViewById(R.id.img_cliente_caminhar_b);
        clientes =
                new SpriteImageView[] {
                    cliente, receber, pegar, guardar, erro, desapontado, caminharA, caminharB
                };
        ConjuntoSprites conjuntos = new ConjuntoSprites(getResources());
        personagens =
                new QuadroSprite[][] {
                    conjuntos.cliente("cliente"),
                    conjuntos.cliente("theo"),
                    conjuntos.cliente("lia")
                };
        for (int i = 0; i < clientes.length; i++) {
            clientes[i].definirQuadro(personagens[0][i]);
        }
        barista = findViewById(R.id.img_barista);
        lancar = findViewById(R.id.img_barista_lancar);
        preparar = findViewById(R.id.img_barista_preparar);
        despedida = findViewById(R.id.img_barista_despedida);
        baristas = new SpriteImageView[] {barista, preparar, lancar, despedida};
        QuadroSprite[] mika =
                conjuntos.carregar(
                        "mika",
                        new String[] {
                            "barista_idle",
                            "barista_preparar",
                            "barista_lancar",
                            "barista_despedida"
                        });
        for (int i = 0; i < baristas.length; i++) {
            baristas[i].definirQuadro(mika[i]);
        }
        rostoAprova =
                SpriteImageView.carregarRosto(getContext(), R.drawable.barista_aprovar_qualidade);
        rostoSusto =
                SpriteImageView.carregarRosto(getContext(), R.drawable.barista_susto_qualidade);
        ambiente = findViewById(R.id.ambiente_cafe);
        efeitos = findViewById(R.id.efeitos_cafe);
    }

    public void definirMotor(MotorRecepcao motor) {
        this.motor = motor;
        atendimento = new AtendimentoCafe(motor.getEntregas());
    }

    public void definirPartidaIniciada(boolean iniciada) {
        efeitos.definirPartidaIniciada(iniciada);
    }

    public float getProgressoCelebracao() {
        return progressoCelebracao;
    }

    public AtendimentoCafe getAtendimento() {
        return atendimento;
    }

    public ReacaoMika getReacaoMika() {
        return reacaoMika;
    }

    public CoreografiaCafe.Cliente getEstadoCliente() {
        return coreografia.getCliente();
    }

    public CoreografiaCafe.Mika getEstadoMika() {
        return coreografia.getMika();
    }

    public CoreografiaCafe.Paleta getPaleta() {
        return coreografia.getPaleta();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        ImageView fundo = findViewById(R.id.img_fundo_cafe);
        if (fundo == null || fundo.getDrawable() == null) {
            return;
        }
        Drawable d = fundo.getDrawable();
        float proporcao = (float) d.getIntrinsicWidth() / d.getIntrinsicHeight();
        float largura = Math.max(getWidth(), getHeight() * proporcao);
        alturaCena = largura / proporcao;
        float x = (getWidth() - largura) / 2, y = (getHeight() - alturaCena) / 2;
        posicionar(fundo, x, y, largura, alturaCena);
        posicionar(ambiente, x, y, largura, alturaCena);
        for (SpriteImageView sprite : baristas) {
            float h = alturaCena * .43f, w = h * sprite.getProporcaoVisivel();
            posicionar(
                    sprite,
                    x + largura * .68f - w * sprite.getAncoraPes(),
                    y + alturaCena * .23f,
                    w,
                    h);
        }
        View balcao = findViewById(R.id.img_balcao_frente);
        posicionar(balcao, x, y, largura, alturaCena);
        recorteBalcao.set(
                0,
                Math.round(alturaCena * .414f),
                Math.round(largura),
                Math.round(alturaCena * .69f));
        balcao.setClipBounds(recorteBalcao);
        for (SpriteImageView sprite : clientes) {
            float h = alturaCena * .46f, w = h * sprite.getProporcaoVisivel();
            posicionar(
                    sprite,
                    x + largura * .34f - w * sprite.getAncoraPes(),
                    y + alturaCena * .27f,
                    w,
                    h);
        }
        posicionar(efeitos, 0, 0, getWidth(), getHeight());
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (camada.getColorFilter() == null) {
            super.dispatchDraw(canvas);
            return;
        }
        int salvo = canvas.saveLayer(0, 0, getWidth(), getHeight(), camada);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(salvo);
    }

    public void reiniciar() {
        limparFeedback();
        atualizar(0, false);
    }

    public void limparFeedback() {
        instanteFeedback = -10000;
        julgamento = null;
        efeitos.encerrarToque();
    }

    public void reagir(EntregaCafe entrega, Julgamento resultado, long tempo) {
        julgamento = resultado;
        instanteFeedback = tempo;
    }

    private void converterToque(float x, float y) {
        pontoToque[0] = x - getLeft();
        pontoToque[1] = y - getTop();
        if (getMatrix().invert(inversaCamera)) {
            inversaCamera.mapPoints(pontoToque);
        }
    }

    public void indicarToque(float x, float y) {
        converterToque(x, y);
        efeitos.indicarToque(pontoToque[0], pontoToque[1]);
    }

    public void moverToque(float x, float y) {
        converterToque(x, y);
        efeitos.moverToque(pontoToque[0], pontoToque[1]);
    }

    public void encerrarToque() {
        efeitos.encerrarToque();
    }

    public void atualizar(long tempo, boolean ativo) {
        if (motor == null || alturaCena == 0) {
            return;
        }
        ambiente.atualizar(tempo);
        coreografia.atualizar(tempo, motor, instanteFeedback, julgamento);
        float pulso = coreografia.getPulso();
        atendimento.atualizar(tempo, motor);
        float deslocamento = getWidth() * .65f * atendimento.getDeslocamento();
        boolean presente = atendimento.getPedidoAtual() != null;
        int novo = presente ? atendimento.getPedidoAtual().id % 3 : clienteAtual;
        if (novo != clienteAtual) {
            clienteAtual = novo;
            for (int i = 0; i < clientes.length; i++) {
                clientes[i].definirQuadro(personagens[novo][i]);
            }
            requestLayout();
        }
        CoreografiaCafe.Cliente pose = coreografia.getCliente();
        progressoCelebracao = CelebracaoCafe.progresso(tempo, motor, atendimento, pose);
        float agradecimento = CelebracaoCafe.impulso(progressoCelebracao);
        AtendimentoCafe.Etapa etapa = atendimento.getEtapa();
        boolean saindo = etapa == AtendimentoCafe.Etapa.SAINDO;
        long passoMs = RoteiroRecepcao.BATIDA_MS / 4;
        long desdePasso =
                presente
                        ? tempo
                                - (saindo
                                        ? atendimento.getPedidoAtual().saidaMs
                                        : atendimento.getPedidoAtual().entradaMs)
                        : 0;
        boolean despedidaTriste =
                saindo
                        && desdePasso < passoMs
                        && (atendimento.getDesfecho() == AtendimentoCafe.Desfecho.PARCIAL
                                || atendimento.getDesfecho() == AtendimentoCafe.Desfecho.PERDIDO);
        boolean caminhando =
                presente
                        && pose != CoreografiaCafe.Cliente.ERRA
                        && !despedidaTriste
                        && (etapa == AtendimentoCafe.Etapa.CHEGANDO || saindo);
        int quadro = (int) (desdePasso / passoMs % 2);
        caminharA.setAlpha(caminhando && quadro == 0 ? 1f : 0f);
        caminharB.setAlpha(caminhando && quadro == 1 ? 1f : 0f);
        boolean emAtendimento = presente && !caminhando;
        float p = coreografia.getProgressoCliente();
        long desdeCaptura = Math.round(p * 520f);
        boolean gestoCaptura =
                pose == CoreografiaCafe.Cliente.PEGA && AnimacaoCaptura.estaPegando(desdeCaptura);
        boolean gestoGuardar =
                pose == CoreografiaCafe.Cliente.PEGA && AnimacaoCaptura.estaGuardando(desdeCaptura);
        boolean satisfeito =
                pose == CoreografiaCafe.Cliente.ESPERA
                        && atendimento.getDesfecho() == AtendimentoCafe.Desfecho.COMPLETO;
        boolean decepcionado =
                pose == CoreografiaCafe.Cliente.ESPERA
                        && (atendimento.getDesfecho() == AtendimentoCafe.Desfecho.PARCIAL
                                || atendimento.getDesfecho() == AtendimentoCafe.Desfecho.PERDIDO);
        int alvo =
                !presente
                        ? -1
                        : caminhando
                                ? 6 + quadro
                                : pose == CoreografiaCafe.Cliente.ERRA
                                        ? 4
                                        : decepcionado
                                                ? 5
                                                : gestoGuardar
                                                        ? 3
                                                        : gestoCaptura || satisfeito
                                                                ? 2
                                                                : pose
                                                                                        == CoreografiaCafe
                                                                                                .Cliente
                                                                                                .PREPARA
                                                                                || pose
                                                                                        == CoreografiaCafe
                                                                                                .Cliente
                                                                                                .PEGA
                                                                        ? 1
                                                                        : 0;
        float[] pesos =
                transicaoCliente.atualizar(
                        alvo, tempo, alvo < 0 || alvo == 2 || alvo == 3 || alvo == 4 || alvo >= 6);
        for (int i = 0; i < clientes.length; i++) {
            clientes[i].setAlpha(pesos[i]);
        }
        float inclinacao = .6f * pulso, acaoX = 0, acaoY = -alturaCena * .003f * pulso;
        if (pose == CoreografiaCafe.Cliente.PREPARA) {
            inclinacao = -1.8f;
        }
        if (caminhando) {
            inclinacao = 0;
            acaoY =
                    -alturaCena
                            * .003f
                            * Math.abs((float) Math.sin(desdePasso * Math.PI / passoMs));
        } else if (pose == CoreografiaCafe.Cliente.PEGA) {
            float impulso =
                    (float)
                            Math.sin(
                                    Math.min(
                                                    1,
                                                    desdeCaptura
                                                            / (float)
                                                                    AnimacaoCaptura.FIM_GUARDAR_MS)
                                            * Math.PI);
            acaoX = -alturaCena * .013f * impulso;
            acaoY += alturaCena * .008f * impulso;
            inclinacao = -3 * impulso;
        } else if (pose == CoreografiaCafe.Cliente.ERRA) {
            float recupera = 1 - p;
            acaoX = -alturaCena * .012f * (float) Math.sin(p * Math.PI);
            inclinacao =
                    3 * (float) Math.sin(p * Math.PI)
                            + (float) Math.sin(p * Math.PI * 4) * recupera;
        }
        inclinacao += 2.3f * agradecimento;
        acaoY += alturaCena * .006f * agradecimento;
        for (SpriteImageView sprite : clientes) {
            sprite.setTranslationX(deslocamento + acaoX);
            sprite.setTranslationY(acaoY);
            sprite.setPivotX(sprite.getWidth() * sprite.getAncoraPes());
            sprite.setPivotY(sprite.getHeight());
            sprite.setRotation(inclinacao);
            sprite.setScaleX(1f);
        }
        caminharA.setScaleX(saindo ? -1f : 1f);
        caminharB.setScaleX(saindo ? -1f : 1f);
        CoreografiaCafe.Mika mika = coreografia.getMika();
        reacaoMika.atualizar(
                tempo, motor, atendimento, mika, pose, coreografia.getProgressoCliente());
        float[] pesosMika =
                transicaoMika.atualizar(mika.ordinal(), tempo, mika == CoreografiaCafe.Mika.LANCAR);
        for (int i = 0; i < baristas.length; i++) {
            baristas[i].setAlpha(pesosMika[i]);
        }
        quadroMika = -1;
        barista.definirRosto(null);
        if (reacaoMika.getTipo() != ReacaoMika.Tipo.NENHUMA) {
            quadroMika = reacaoMika.getTipo() == ReacaoMika.Tipo.SUSTO ? 2 : 0;
            for (SpriteImageView sprite : baristas) {
                sprite.setAlpha(0f);
            }
            barista.setAlpha(1f);
            barista.definirRosto(quadroMika == 2 ? rostoSusto : rostoAprova);
        }
        float giro = pulso * .6f, movimento = 0;
        if (mika == CoreografiaCafe.Mika.PREPARA) {
            giro = 2.5f;
            movimento = alturaCena * .005f;
        } else if (mika == CoreografiaCafe.Mika.LANCAR) {
            float impulso = 1 - coreografia.getProgressoMika();
            giro = -5 * impulso;
            movimento = -alturaCena * .012f * impulso;
        } else if (mika == CoreografiaCafe.Mika.DESPEDIDA) {
            giro =
                    .8f
                            * (float)
                                    Math.sin(
                                            (tempo - RoteiroRecepcao.FIM_PEDIDOS_MS)
                                                    * Math.PI
                                                    / 500f);
        }
        if (mika == CoreografiaCafe.Mika.ESPERA) {
            giro -= 1.6f * agradecimento;
            giro += reacaoMika.getInclinacao();
        }
        if (reacaoMika.getTipo() == ReacaoMika.Tipo.SUSTO) {
            giro = reacaoMika.getInclinacao();
            movimento = 0;
        }
        for (SpriteImageView sprite : baristas) {
            sprite.setPivotX(sprite.getWidth() * sprite.getAncoraPes());
            sprite.setPivotY(sprite.getHeight() * .65f);
            sprite.setRotation(giro);
            sprite.setTranslationX(movimento);
            sprite.setTranslationY(-alturaCena * .002f * pulso);
        }
        float zoom = coreografia.getZoom();
        setPivotX(getWidth() * .48f);
        setPivotY(getHeight() * .4f);
        setScaleX(zoom);
        setScaleY(zoom);
        setTranslationX(0f);
        setTranslationY(0f);
        camada.setColorFilter(
                paletas.obter(
                        coreografia.getPaletaAnterior(),
                        coreografia.getPaleta(),
                        coreografia.getMistura()));
        efeitos.atualizar(tempo, ativo, motor);
        invalidate();
    }

    private void posicionar(View v, float x, float y, float w, float h) {
        int iw = Math.max(1, Math.round(w)), ih = Math.max(1, Math.round(h));
        v.measure(
                MeasureSpec.makeMeasureSpec(iw, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(ih, MeasureSpec.EXACTLY));
        int ix = Math.round(x), iy = Math.round(y);
        v.layout(ix, iy, ix + iw, iy + ih);
    }
}
