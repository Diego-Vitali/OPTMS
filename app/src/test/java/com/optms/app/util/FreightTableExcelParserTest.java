package com.optms.app.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.optms.app.dto.TabelaFreteRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FreightTableExcelParserTest {

    private final FreightTableExcelParser parser = new FreightTableExcelParser();

    @Test
    void importsMultipleOriginsFromNewLayout() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tabela.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes());

        TabelaFreteRequest request = parser.parse(file);

        assertEquals(List.of("SP", "MG"), request.getUfsOrigem());
        assertEquals("Tabela Sudeste", request.getNome());
        assertEquals(3, request.getObjetos().size());
        assertEquals("PARTIDA", request.getObjetos().getFirst().getTipoObjeto());
        assertEquals("SP", request.getObjetos().getFirst().getUfOrigem());
        assertEquals("RJ", request.getObjetos().getFirst().getUfDestino());
        assertEquals("FAIXA", request.getObjetos().getFirst().getConfigCalculo().getFormaCalculo());
        assertEquals(2, request.getObjetos().getFirst().getConfigCalculo().getRegras().size());
    }

    private byte[] workbookBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet config = workbook.createSheet("Config");
            writeHeader(config.createRow(0), List.of("Nome Referência", "Vigência Início", "Vigência Fim"));
            writeRow(config.createRow(1), "Tabela Sudeste", "2026-01-01", "2026-12-31");

            Sheet baseFreight = workbook.createSheet("Frete_Partida");
            writeHeader(baseFreight.createRow(0), List.of(
                    "UF Origem",
                    "UF Destino",
                    "Forma Calculo",
                    "Unidade Faixa",
                    "Limite Inicial",
                    "Limite Final",
                    "Unidade variante",
                    "Tipo Calculo",
                    "Valor do cálculo"
            ));
            writeRow(baseFreight.createRow(1), "SP", "RJ", "FAIXA", "PESO_BRUTO",
                    null, 10.0, "VALOR_NOTA", "PERCENTUAL", 5.0);
            writeRow(baseFreight.createRow(2), "SP", "RJ", "FAIXA", "PESO_BRUTO",
                    10.0, null, "VALOR_NOTA", "PERCENTUAL", 7.0);
            writeRow(baseFreight.createRow(3), "MG", "RJ", "CONSTANTE", null,
                    null, null, "VALOR_NOTA", "VALOR_FIXO", 180.0);

            Sheet components = workbook.createSheet("Componentes");
            writeHeader(components.createRow(0), List.of(
                    "UF Origem",
                    "UF Destino",
                    "Nome Componente",
                    "Forma Calculo",
                    "Unidade Faixa",
                    "Limite Inicial",
                    "Limite Final",
                    "Unidade variante",
                    "Tipo Calculo",
                    "Valor do cálculo"
            ));
            writeRow(components.createRow(1), "SP", "RJ", "Pedagio", "CONSTANTE",
                    null, null, null, "PESO_BRUTO", "MULTIPLICADOR", 1.2);

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void writeHeader(Row header, List<String> headers) {
        for (int index = 0; index < headers.size(); index++) {
            header.createCell(index).setCellValue(headers.get(index));
        }
    }

    private void writeRow(Row row, Object... values) {
        for (int index = 0; index < values.length; index++) {
            Object value = values[index];
            if (value instanceof Number number) {
                row.createCell(index).setCellValue(number.doubleValue());
            } else if (value != null) {
                row.createCell(index).setCellValue(value.toString());
            }
        }
    }
}
