package dao;



import java.lang.reflect.InvocationTargetException;

import java.sql.Connection;

import java.sql.DriverManager;

import java.sql.SQLException;



public class Conn implements AutoCloseable {



private String host;

private String port;

private String schema;

private String user;

private String password;


private Connection connection = null;


public Conn(String host, String port, String schema, String user, String password) {

this.setHost(host);

this.setPort(port);

this.setSchema(schema);

this.setUser(user);

this.setPassword(password);

this.doConnection();

}


public Conn() {

this.setHost ("localhost");

this.setPort ("3306");

this.setSchema ("tma");

this.setUser ("root");

this.setPassword("");

this.doConnection();

}


private void doConnection() {

String timezone = "&useTimezone=true&serverTimezone=UTC";

String url = "jdbc:mysql://"+this.host+":"+port+"/"+this.schema+"?user="+this.user+"&password="+this.password+timezone;

try {

Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();

DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());

this.connection = DriverManager.getConnection(url);

} catch (InstantiationException e){

e.printStackTrace();

} catch (IllegalAccessException e){

e.printStackTrace();

} catch (ClassNotFoundException e) {

e.printStackTrace();

} catch (SQLException e) {

e.printStackTrace();

}catch (IllegalArgumentException e) {

e.printStackTrace();

} catch (InvocationTargetException e) {

e.printStackTrace();

} catch (NoSuchMethodException e) {

e.printStackTrace();

} catch (SecurityException e) {

e.printStackTrace();

}

}


@Override

public void close() {

try {

if (this.connection != null && !this.connection.isClosed()) {

this.connection.close();

}

} catch (SQLException e) {

System.err.println("Erro ao fechar a conexão: " + e.getMessage());

e.printStackTrace();

}

}



public String getHost() {

return host;

}


public void setHost(String host) {

this.host = ( host.isEmpty() ? "localhost" : host ) ;

}


public String getPort() {

return port;

}


public void setPort(String port) {

this.port = ( port.isEmpty() ? "3306" : port ) ;

}


public String getSchema() {

return schema;

}


public void setSchema(String schema) {

this.schema = schema;

}


public String getUser() {

return user;

}


public void setUser(String user) {

this.user = user;

}


public void setPassword(String password) {
	this.password = password;
}

public Connection getConnection() {
	return (this.connection);
}
}