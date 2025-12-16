package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.Usuario;

public class UsuarioDao {

    public void inserir(Usuario usuario) {
        String sql = "INSERT INTO tb_usuarios (nome, email, senha, tipo, status) VALUES (?, ?, ?, ?, ?)";
        
        try (Conn conn = new Conn()) {
            Connection cn = conn.getConnection();
            try (PreparedStatement stmt = cn.prepareStatement(sql)) {
                stmt.setString(1, usuario.getNome());
                stmt.setString(2, usuario.getEmail());
                stmt.setString(3, usuario.getSenha());
                stmt.setString(4, usuario.getTipo());
                stmt.setString(5, "ATIVO"); 

                stmt.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void atualizar(Usuario usuario) {
        String sql = "UPDATE tb_usuarios SET nome=?, email=?, tipo=? WHERE id=?";
        
        if (usuario.getSenha() != null && !usuario.getSenha().isEmpty()) {
            sql = "UPDATE tb_usuarios SET nome=?, email=?, tipo=?, senha=? WHERE id=?";
        }

        try (Conn conn = new Conn()) {
            Connection cn = conn.getConnection();
            try (PreparedStatement stmt = cn.prepareStatement(sql)) {
                
                stmt.setString(1, usuario.getNome());
                stmt.setString(2, usuario.getEmail());
                stmt.setString(3, usuario.getTipo());

                if (usuario.getSenha() != null && !usuario.getSenha().isEmpty()) {
                    stmt.setString(4, usuario.getSenha());
                    stmt.setInt(5, usuario.getId());
                } else {
                    stmt.setInt(4, usuario.getId());
                }

                stmt.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void excluir(int id) {
        String sql = "DELETE FROM tb_usuarios WHERE id=?";
        
        try (Conn conn = new Conn()) {
            Connection cn = conn.getConnection();
            try (PreparedStatement stmt = cn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                stmt.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Usuario> listar() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM tb_usuarios";

        try (Conn conn = new Conn()) {
            Connection cn = conn.getConnection();
            try (PreparedStatement stmt = cn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setNome(rs.getString("nome"));
                    u.setEmail(rs.getString("email"));
                    u.setTipo(rs.getString("tipo"));
                    u.setStatus(rs.getString("status"));
                    lista.add(u);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean autenticar(String email, String senha) {
        String sql = "SELECT * FROM tb_usuarios WHERE email = ? AND senha = ?";
        
        try (Conn conn = new Conn()) {
            Connection cn = conn.getConnection();
            try (PreparedStatement stmt = cn.prepareStatement(sql)) {
                stmt.setString(1, email);
                stmt.setString(2, senha);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}