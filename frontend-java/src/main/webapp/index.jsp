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
        .otp-input {
            letter-spacing: 5px;
            font-size: 1.5rem;   
            text-align: center;
        }
        #form2FA {
        	display: none;
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
    
    $("#formLogin").submit(function(e) {
        e.preventDefault();

        var email = $("#emailInput").val();
        var senha = $("#senhaInput").val();

        $("#btnAvancar").prop("disabled", true).text("Verificando...");

        $.ajax({
            url: "${pageContext.request.contextPath}/authServlet", 
            type: "POST",
            data: {
                action: "login",
                email: email,
                senha: senha
            },
            success: function(response) {
            	window.location.href = "2fa.jsp"; 
            },
            error: function(xhr) {
                alert("Usuário ou senha inválidos.");
                
                $("#btnAvancar").prop("disabled", false).text("Entrar");
            }
        });
    });
});
</script>

</body>
</html>