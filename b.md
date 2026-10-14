4. Cole o único trecho de código necessário
Abra [MainActivity.java](E:/Beatdays/app/src/main/java/com/example/beatdays/MainActivity.java).
Procure esta linha dentro de onCreate():
setContentView(R.layout.activity_main);
Logo abaixo dela, cole:
findViewById(R.id.btn_nova_tela).setOnClickListener(v ->
        startActivity(new android.content.Intent(
                MainActivity.this,
                NovaTelaActivity.class
        ))
);
Esse trecho significa:
- Encontre o botão btn_nova_tela.
- Quando ele for clicado, abra NovaTelaActivity.
