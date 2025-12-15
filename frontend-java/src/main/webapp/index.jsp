<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - Autenticação</title>
    
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    
    <style>
        /* Style sem ser em boostrap só p deixar os números do token grandes */
        .otp-input {
            letter-spacing: 5px;
            font-size: 1.5rem;   
            text-align: center;
        }
    </style>
</head>
<body class="bg-light">
    
    <div class="container mt-5">
        <div class="row justify-content-center">
            <div class="col-md-5 col-lg-4">
                <div class="card shadow">
                    <div class="card-header bg-primary text-white text-center">
                        <h4 class="mb-0" id="headerTitle">Acesso ao Sistema</h4>
                    </div>
                    
                    <div class="card-body p-4">
                        
                        <div id="alertBox" class="alert alert-danger" style="display:none;" role="alert"></div>

                        <form id="formLogin">
                            <div class="mb-3">
                                <label for="emailInput" class="form-label">E-mail</label>
                                <input type="email" class="form-control" id="emailInput" placeholder="seu@email.com" required>
                            </div>
                            <div class="mb-4">
                                <label for="senhaInput" class="form-label">Senha</label>
                                <input type="password" class="form-control" id="senhaInput" placeholder="******" required>
                            </div>
                            <div class="d-grid">
                                <button type="submit" id="btnAvancar" class="btn btn-primary btn-lg">Entrar</button>
                            </div>
                        </form>

                        <form id="form2FA" style="display:none;">
                            <div class="text-center mb-4">
                                <p class="text-muted small">
                                    Enviamos um código de verificação para o seu e-mail/celular.
                                </p>
                            </div>
                            
                            <div class="mb-4">
                                <label for="tokenInput" class="form-label text-center w-100">Código de Verificação</label>
                                <input type="text" class="form-control otp-input" id="tokenInput" maxlength="6" placeholder="000000">
                            </div>

                            <div class="d-grid gap-2">
                                <button type="button" id="btnVerificar" class="btn btn-success btn-lg">Validar Acesso</button>
                                <button type="button" id="btnVoltar" class="btn btn-outline-secondary btn-sm">Voltar</button>
                            </div>
                        </form>

                    </div>
                    <div class="card-footer text-center bg-white border-top-0 pb-4">
                        <small class="text-muted">Sistema de Logística v1.0</small>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

    <script>
    $(document).ready(function() {
        
        //Quando clicar em "Entrar"
        $("#formLogin").submit(function(e) {
            $("#alertBox").hide(); // Esconde erro
            
            var email = $("#emailInput").val();
            var senha = $("#senhaInput").val();

            // Muda o texto do botão pra dar um feedback pro usuário
            var btnOriginalText = $("#btnAvancar").text();
            $("#btnAvancar").prop("disabled", true).text("Verificando...");

            // Chama o Servlet
            $.ajax({
                url: "${pageContext.request.contextPath}/authServlet", 
                type: "POST",
                data: {
                    action: "login", // Manda que é login
                    email: email,
                    senha: senha
                },
                success: function(response) {
                    // SE A SENHA TIVER OK:
                    // 1. Some com o form de login devagarzinho (fadeOut)
                    // 2. Muda o título do card
                    // 3. Aparece o form do Token (fadeIn)
                    $("#formLogin").fadeOut(300, function() {
                        $("#headerTitle").text("Autenticação de 2 Fatores");
                        $("#form2FA").fadeIn(300);
                        $("#tokenInput").focus(); // Já coloca o cursor no campo do token
                    });
                },
                error: function(xhr) {
                    // SE A SENHA TIVER ERRADA:
                    $("#alertBox").text("Usuário ou senha inválidos.").show();
                    // Destrava o botão e volta o texto original
                    $("#btnAvancar").prop("disabled", false).text(btnOriginalText);
                }
            });
        });

        // --- AÇÃO 2: Quando clicar em "Validar Acesso" (Token) ---
        $("#btnVerificar").click(function() {
            var token = $("#tokenInput").val();
            $("#alertBox").hide();

            $("#btnVerificar").prop("disabled", true).text("Validando...");

            $.ajax({
                url: "${pageContext.request.contextPath}/authServlet",
                type: "POST",
                data: {
                    action: "validate_2fa", // Avisa o servlet que agora é validação do token
                    token: token
                },
                success: function(response) {
                    // SE O TOKEN TIVER CERTO: Redireciona pra Home
                    window.location.href = "dashboard.jsp"; 
                },
                error: function() {
                    // SE O TOKEN TIVER ERRADO:
                    $("#alertBox").removeClass("alert-danger").addClass("alert-warning");
                    $("#alertBox").text("Código inválido ou expirado.").show();
                    $("#btnVerificar").prop("disabled", false).text("Validar Acesso");
                }
            });
        });

        // --- Botão Voltar (Reset da tela) ---
        $("#btnVoltar").click(function() {
            // Esconde a parte do Token e mostra o Login de novo
            $("#form2FA").hide();
            $("#headerTitle").text("Acesso ao Sistema");
            $("#formLogin").fadeIn();
            
            // Reseta os botões e limpa o campo do token
            $("#btnAvancar").prop("disabled", false).text("Entrar");
            $("#tokenInput").val("");
            $("#alertBox").hide();
        });
    });
    </script>

</body>
</html>