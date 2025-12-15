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

        //LOGIN Email + Senha
        if ("login".equals(action)) {
            String email = request.getParameter("email");
            String senha = request.getParameter("senha");

            // Valida no banco DAO
            boolean isValido = dao.autenticar(email, senha);

            // Aqui serve para criar um token aleatório para usar na session
            if (isValido) {
                String tokenGerado = String.format("%06d", new Random().nextInt(999999));
 
         
                session.setAttribute("user_email", email);
                session.setAttribute("2fa_token", tokenGerado);

                
                System.out.println(">>> TOKEN GERADO PARA " + email + ": " + tokenGerado);// TESTE
                
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        
        //VALIDAR TOKEN (2FA) ---
        } else if ("validate_2fa".equals(action)) {
            
            String tokenDigitado = request.getParameter("token");
            String tokenReal = (String) session.getAttribute("2fa_token");

            if (tokenReal != null && tokenReal.equals(tokenDigitado)) {
                session.removeAttribute("2fa_token"); 
                
                session.setAttribute("usuario_logado", true); 
                
                response.setStatus(HttpServletResponse.SC_OK); 
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }
    }
}