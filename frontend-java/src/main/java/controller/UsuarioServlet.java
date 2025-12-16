package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.UsuarioDao;
import model.Usuario;

@WebServlet("/usuarioServlet")
public class UsuarioServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private UsuarioDao dao = new UsuarioDao();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("list".equals(action)) {
            List<Usuario> lista = dao.listar();
            
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            PrintWriter out = response.getWriter();
            StringBuilder json = new StringBuilder("[");
            
            for (int i = 0; i < lista.size(); i++) {
                Usuario u = lista.get(i);
                json.append("{")
                    .append("\"id\":").append(u.getId()).append(",")
                    .append("\"nome\":\"").append(u.getNome()).append("\",")
                    .append("\"email\":\"").append(u.getEmail()).append("\",")
                    .append("\"tipo\":\"").append(u.getTipo()).append("\",")
                    .append("\"status\":\"").append(u.getStatus()).append("\"")
                    .append("}");
                
                if (i < lista.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");
            out.print(json.toString());
            out.flush();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        if ("save".equals(action)) {
            String idStr = request.getParameter("id");
            String nome = request.getParameter("nome");
            String email = request.getParameter("email");
            String senha = request.getParameter("senha");
            String perfil = request.getParameter("perfil");

            Usuario u = new Usuario();
            u.setNome(nome);
            u.setEmail(email);
            u.setSenha(senha);
            u.setTipo(perfil);

            if (idStr == null || idStr.isEmpty()) {
                dao.inserir(u);
            } else {
                u.setId(Integer.parseInt(idStr));
                dao.atualizar(u);
            }
            response.setStatus(HttpServletResponse.SC_OK);

        } else if ("delete".equals(action)) {
            String idStr = request.getParameter("id");
            if (idStr != null) {
                dao.excluir(Integer.parseInt(idStr));
            }
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }
}