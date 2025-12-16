package controller;

import java.io.IOException;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import dao.UsuarioDao;
import model.Usuario;

@WebServlet("/authServlet")
public class AuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        String action = request.getParameter("action");
        UsuarioDao dao = new UsuarioDao();
        HttpSession session = request.getSession();

   
        if ("login".equals(action)) {
            String email = request.getParameter("email");
            String senha = request.getParameter("senha");

            boolean isValido = dao.autenticar(email, senha);

            if (isValido) {
//              CARALHA DO TOKENA LEATÓRIO  
            	String tokenGerado = String.format("%06d", new Random().nextInt(999999));
 
                // CRIA A SESSÃO TEMPORÁRIA
                session.setAttribute("temp_user_email", email);
                session.setAttribute("2fa_token", tokenGerado);
                
                System.out.println(">>> TOKEN P/ " + email + ": " + tokenGerado);
                
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Credenciais inválidas");
            }
        
        //VALIDAR TOKEN (2FA) ---
        } else if ("validate_2fa".equals(action)) {
            
            String tokenDigitado = request.getParameter("token");
            
            String tokenReal = (String) session.getAttribute("2fa_token");
            String emailTemp = (String) session.getAttribute("temp_user_email");

            if (tokenReal != null && tokenReal.equals(tokenDigitado) && emailTemp != null) {
                
                // LIMPEZA
                session.removeAttribute("2fa_token");
                session.removeAttribute("temp_user_email");
                
                session.setAttribute("usuarioLogado", emailTemp); 
                
                response.setStatus(HttpServletResponse.SC_OK); 
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido ou expirado");
            }
            
        } else if ("logout".equals(action)) {
            session.invalidate(); // Destrói a sessão inteira
            response.sendRedirect("login.jsp");
        }
    }
}