<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Previsão de Frete</title>
    
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
</head>
<body class="bg-light">

    <%@ include file="includes/navbar.jsp" %>
    
    <div class="container mt-5">
        <div class="card shadow">
            <div class="card-header bg-primary text-white">
                <h4 class="mb-0">Calcular Previsão de Frete</h4>
            </div>
            
            <div class="card-body">
                <form id="formFrete">
                    
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label for="pesoBrutoInput" class="form-label">Peso Bruto</label>
                            <input type="text" class="form-control" id="pesoBrutoInput" value="150.5">
                        </div>
                        <div class="col-md-6">
                            <label for="metroCubicoInput" class="form-label">Metro Cúbico</label>
                            <input type="text" class="form-control" id="metroCubicoInput" value="10.0">
                        </div>
                    </div>

                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label for="ufOrigemInput" class="form-label">UF Origem</label>
                            <select class="form-select" id="ufOrigemInput">
                                <option value="AC">AC</option>
                                <option value="AL">AL</option>
                                <option value="AP">AP</option>
                                <option value="AM">AM</option>
                                <option value="BA">BA</option>
                                <option value="CE">CE</option>
                                <option value="DF">DF</option>
                                <option value="ES">ES</option>
                                <option value="GO">GO</option>
                                <option value="MA">MA</option>
                                <option value="MT">MT</option>
                                <option value="MS">MS</option>
                                <option value="MG">MG</option>
                                <option value="PA">PA</option>
                                <option value="PB">PB</option>
                                <option value="PR">PR</option>
                                <option value="PE">PE</option>
                                <option value="PI">PI</option>
                                <option value="RJ">RJ</option>
                                <option value="RN">RN</option>
                                <option value="RS">RS</option>
                                <option value="RO">RO</option>
                                <option value="RR">RR</option>
                                <option value="SC">SC</option>
                                <option value="SP" selected>SP</option>
                                <option value="SE">SE</option>
                                <option value="TO">TO</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label for="ufDestinoInput" class="form-label">UF Destino</label>
                            <select class="form-select" id="ufDestinoInput">
                                <option value="AC">AC</option>
                                <option value="AL">AL</option>
                                <option value="AP">AP</option>
                                <option value="AM">AM</option>
                                <option value="BA">BA</option>
                                <option value="CE">CE</option>
                                <option value="DF">DF</option>
                                <option value="ES">ES</option>
                                <option value="GO">GO</option>
                                <option value="MA">MA</option>
                                <option value="MT">MT</option>
                                <option value="MS">MS</option>
                                <option value="MG">MG</option>
                                <option value="PA">PA</option>
                                <option value="PB">PB</option>
                                <option value="PR">PR</option>
                                <option value="PE">PE</option>
                                <option value="PI">PI</option>
                                <option value="RJ">RJ</option> 
                                <option value="RN">RN</option>
                                <option value="RS">RS</option>
                                <option value="RO">RO</option>
                                <option value="RR">RR</option>
                                <option value="SC">SC</option>
                                <option value="SP"  selected>SP</option>
                                <option value="SE">SE</option>
                                <option value="TO">TO</option>
                            </select>
                        </div>
                    </div>

                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label for="valorMercadoriaInput" class="form-label">Valor Declarado (R$)</label>
                            <input type="text" class="form-control" id="valorMercadoriaInput" value="100.00">
                        </div>
                        <div class="col-md-6">
                            <label for="quantidadeVolumesInput" class="form-label">Quantidade de Volumes</label>
                            <input type="text" class="form-control" id="quantidadeVolumesInput" value="1">
                        </div>
                    </div>

                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label for="tipoFreteInput" class="form-label">Tipo de Frete</label>
                            <select class="form-select" id="tipoFreteInput">
                                <option value="FRACIONADO">FRACIONADO</option>
                                <option value="ITINERANTE">ITINERANTE</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label for="viaTransporteInput" class="form-label">Via de Transporte</label>
                            <select id="viaTransporteInput" name="viaTransporteInput" class="form-select">
                                <option value="RODOVIÁRIO">RODOVIÁRIO</option>
                                <option value="AÉREO">AÉREO</option>
                            </select>
                        </div>
                    </div>

                    <div class="d-grid gap-2 mt-4">
                        <button type="button" id="btnPrever" class="btn btn-primary btn-lg">Realizar Previsão</button>
                    </div>

                </form>

                <div class="mt-4 p-3 bg-light border rounded text-center" style="display:none;" id="boxResultado">
                    <h5 class="mb-0 text-secondary">
                        Resultado: <span id="resultadoSpan" class="fw-bold text-dark"></span>
                    </h5>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

    <script>
    $(document).ready(function() {
        
        $("#btnPrever").click(function() {
            // Mostra a caixa de resultado e indica carregamento
            $("#boxResultado").show(); 
            $("#resultadoSpan").text("Calculando...");
            $("#resultadoSpan").removeClass("text-danger text-success").addClass("text-dark");

            // Captura os dados (incluindo os selects)
            var peso = $("#pesoBrutoInput").val();
            var ufOrigem = $("#ufOrigemInput").val();
            var ufDestino = $("#ufDestinoInput").val();
            var metroCubico = $("#metroCubicoInput").val();
            var valorMercadoria = $("#valorMercadoriaInput").val();
            var quantidadeVolumes = $("#quantidadeVolumesInput").val();
            var tipoFrete = $("#tipoFreteInput").val();
            var viaTransporte = $("#viaTransporteInput").val();

            $.ajax({
                url: "${pageContext.request.contextPath}/previsaoServlet", 
                type: "POST",           
                data: {                 
                    pesoBrutoInput: peso,
                    ufOrigemInput: ufOrigem,
                    ufDestinoInput: ufDestino,
                    metroCubicoInput: metroCubico,
                    valorMercadoriaInput: valorMercadoria,
                    quantidadeVolumesInput: quantidadeVolumes,
                    tipoFreteInput: tipoFrete,
                    viaTransporteInput: viaTransporte,
                },
                
                success: function(responseJson) {
                    let dias = Number(responseJson.transitTimeOutput);
                    let diasArredondado = Math.round(dias);
                    
                    $("#resultadoSpan").text(diasArredondado + " dias");
                    $("#resultadoSpan").addClass("text-success");
                },
                
                error: function(xhr, status, error) {
                    $("#resultadoSpan").text("Erro ao calcular.");
                    $("#resultadoSpan").addClass("text-danger");
                }
            });
        });
    });
    </script>

</body>
</html>