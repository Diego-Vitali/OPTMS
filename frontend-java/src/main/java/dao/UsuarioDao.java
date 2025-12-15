package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import model.Usuario;

public class UsuarioDao {

	public void inserir(Usuario usuario) {
        String sql = "INSERT INTO tb_usuarios (nome, email, senha, tipo, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Conn.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getSenha());
            stmt.setString(4, usuario.getTipo());
            stmt.setString(5, usuario.getStatus());

            stmt.execute();
            System.out.println("Usuário inserido com sucesso!");

        } catch (SQLException e) {
            System.err.println("Erro ao inserir usuário: " + e.getMessage());
        }
	}

	public boolean autenticar(String email, String senha) {
	    //busca por email E senha
	    String sql = "SELECT * FROM tb_usuarios WHERE email = ? AND senha = ?";
	    
	    try (Connection conn = Conn.getConnection();
	         PreparedStatement stmt = conn.prepareStatement(sql)) {
	        
	        stmt.setString(1, email);
	        stmt.setString(2, senha);
	        
	        ResultSet rs = stmt.executeQuery();
	        
	        //rs.next retorna TRUE se encontrou e FALSE se não encontrou
	        return rs.next();
	        
	    } catch (SQLException e) {
	        System.err.println("Erro na autenticação: " + e.getMessage());
	        return false;
	    }
	}
}