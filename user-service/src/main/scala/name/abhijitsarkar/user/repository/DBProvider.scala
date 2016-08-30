package name.abhijitsarkar.user.repository

//import com.typesafe.config.ConfigFactory
//import org.apache.commons.dbcp2.{DriverManagerConnectionFactory, PoolableConnection, PoolableConnectionFactory, PoolingDataSource}
//import org.apache.commons.pool2.impl.GenericObjectPool
//import slick.jdbc.JdbcBackend.Database

//object DBProvider {
//  private val config = ConfigFactory.load()
//  private val url = config.getString("db.url")
//  private val username = config.getString("db.username")
//  private val password = config.getString("db.password")
//
//  private val connectionFactory = new DriverManagerConnectionFactory(url, username, password)
//  private val poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null)
//  private val connectionPool = new GenericObjectPool[PoolableConnection](poolableConnectionFactory)
//  poolableConnectionFactory.setPool(connectionPool)
//
//  private val dataSource = new PoolingDataSource[PoolableConnection](connectionPool)
//
//  val db = Database.forDataSource(dataSource)
//}