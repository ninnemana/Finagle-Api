package com.curt.database

import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.{Session, SessionFactory,Schema, KeyedEntity, Query}
import org.squeryl.adapters.MSSQLServer
import com.curt.vehicle._

trait Database {
	val dbUsername = "discounthitch"
	val dbPassword = "eC0mm3rc3"
	var dbConnection = "jdbc:sqlserver://srjbmn26rz.database.windows.net;database=CurtDev"

	var cpds = new ComboPooledDataSource
	cpds.setDriverClass("com.microsoft.sqlserver.jdbc.SQLServerDriver")
	cpds.setJdbcUrl(dbConnection)
	cpds.setUser(dbUsername)
	cpds.setPassword(dbPassword)

	cpds.setMinPoolSize(1)
	cpds.setAcquireIncrement(1)
	cpds.setMaxPoolSize(100)

	SessionFactory.concreteFactory = Some(() => Session.create(cpds.getConnection, new MSSQLServer))

}