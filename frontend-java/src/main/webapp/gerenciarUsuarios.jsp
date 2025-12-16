<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gerenciar Usuários</title>
    
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
</head>
<body class="bg-light">

    <div class="container mt-5">
        <div class="card shadow">
            <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
                <h4 class="mb-0">Gerenciar Usuários</h4>
                <button class="btn btn-light btn-sm fw-bold" id="btnNovoUsuario">+ Novo Usuário</button>
            </div>
            
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover table-bordered align-middle" id="tabela-usuarios">
                        <thead class="table-light">
                            <tr>
                                <th width="5%">ID</th>
                                <th>Nome</th>
                                <th>E-mail</th>
                                <th width="15%">Perfil</th>
                                <th width="15%" class="text-center">Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                        </tbody>
                    </table>
                </div>
                <div id="alertFeedback" class="alert alert-success mt-3" style="display:none;"></div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="userModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title" id="modalTitle">Novo Usuário</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form id="formUsuario">
                        <input type="hidden" id="userIdInput">
                        <div class="mb-3">
                            <label for="nomeInput" class="form-label">Nome Completo</label>
                            <input type="text" class="form-control" id="nomeInput" required>
                        </div>
                        <div class="mb-3">
                            <label for="emailInput" class="form-label">E-mail</label>
                            <input type="email" class="form-control" id="emailInput" required>
                        </div>
                        <div class="mb-3">
                            <label for="senhaInput" class="form-label">Senha</label>
                            <input type="password" class="form-control" id="senhaInput">
                        </div>
                        <div class="mb-3">
                            <label for="perfilInput" class="form-label">Perfil</label>
                            <select class="form-select" id="perfilInput">
                                <option value="comum">Usuário Padrão</option>
                                <option value="admin">Administrador</option>
                            </select>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                    <button type="button" class="btn btn-primary" id="btnSalvarUsuario">Salvar</button>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

    <script>
    $(document).ready(function() {
        
        function carregarUsuarios() {
            $.ajax({
                url: 'usuarioServlet?action=list',
                type: 'GET',
                dataType: 'json',
                success: function(listaUsuarios) {
                    var tbody = $('#tabela-usuarios tbody');
                    tbody.empty();

                    $.each(listaUsuarios, function(index, usuario) {
                    	var tr = 
                    	    '<tr>' +
                    	        '<td>' + usuario.id + '</td>' +
                    	        '<td>' + usuario.nome + '</td>' +
                    	        '<td>' + usuario.email + '</td>' +
                    	        '<td>' + usuario.tipo + '</td>' +
                    	        '<td class="text-center">' +
                    	            '<button class="btn btn-sm btn-primary btn-editar" data-id="' + usuario.id + '">Editar</button> ' +
                    	            '<button class="btn btn-sm btn-danger btn-excluir" data-id="' + usuario.id + '">Excluir</button>' +
                    	        '</td>' +
                    	    '</tr>';
                        tbody.append(tr);
                    });
                },
                error: function(xhr, status, error) {
                    console.error("Erro ao carregar usuários");
                }
            });
        }

        $("#btnNovoUsuario").click(function() {
            $("#modalTitle").text("Novo Usuário");
            $("#userIdInput").val("");
            $("#formUsuario")[0].reset();
            $("#userModal").modal("show");
        });

        $(document).on("click", ".btn-editar", function() {
            var id = $(this).data("id");
            var linha = $(this).closest("tr");
            var nome = linha.find("td:eq(1)").text();
            var email = linha.find("td:eq(2)").text();
            var tipo = linha.find("td:eq(3)").text();
            
            $("#modalTitle").text("Editar Usuário");
            $("#userIdInput").val(id);
            $("#nomeInput").val(nome);
            $("#emailInput").val(email);
            $("#perfilInput").val(tipo);
            $("#senhaInput").val(""); 
            $("#userModal").modal("show");
        });

        $("#btnSalvarUsuario").click(function() {
            var id = $("#userIdInput").val();
            var nome = $("#nomeInput").val();
            var email = $("#emailInput").val();
            var senha = $("#senhaInput").val();
            var perfil = $("#perfilInput").val();

            $.ajax({
                url: "usuarioServlet",
                type: "POST",
                data: {
                    action: "save",
                    id: id,
                    nome: nome,
                    email: email,
                    senha: senha,
                    perfil: perfil
                },
                success: function() {
                    $("#userModal").modal("hide");
                    carregarUsuarios(); 
                    $("#alertFeedback").text("Salvo com sucesso!").fadeIn().delay(2000).fadeOut();
                },
                error: function() {
                    alert("Erro ao salvar.");
                }
            });
        });

        $(document).on("click", ".btn-excluir", function() {
            var id = $(this).data("id");
            if(confirm("Deseja excluir?")) {
                $.ajax({
                    url: "usuarioServlet",
                    type: "POST",
                    data: { action: "delete", id: id },
                    success: function() {
                        carregarUsuarios();
                    }
                });
            }
        });

        carregarUsuarios();
    });
    </script>
</body>
</html>