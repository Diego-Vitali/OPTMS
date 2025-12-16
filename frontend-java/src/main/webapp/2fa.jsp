<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // Se não tem token na sessão, chuta de volta pro login
    if (session.getAttribute("2fa_token") == null) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
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

                        <form id="form2FA">
                            <div class="text-center mb-4">
                                <p class="text-muted small">
                                    Enviamos um código de verificação para o seu e-mail.
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
    $("#btnVerificar").click(function() {
        var token = $("#tokenInput").val();
        $("#alertBox").hide();

        $("#btnVerificar").prop("disabled", true).text("Validando...");

        $.ajax({
            url: "${pageContext.request.contextPath}/authServlet",
            type: "POST",
            data: {
                action: "validate_2fa",
                token: token
            },
            success: function(response) {
                window.location.href = "menu.jsp"; 
            },
            error: function() {
                $("#alertBox").removeClass("alert-danger").addClass("alert-warning");
                $("#alertBox").text("Código inválido ou expirado.").show();
                $("#btnVerificar").prop("disabled", false).text("Validar Acesso");
            }
        });
    });

    $("#btnVoltar").click(function() {
    	window.location.href = "index.jsp"; 

    });
});
</script>

</body>
</html>