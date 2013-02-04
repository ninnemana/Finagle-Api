package com.curt.vehicle

import java.sql.{Connection, DriverManager, ResultSet}
import scala.collection.mutable.ListBuffer
import org.squeryl.SessionFactory
import org.squeryl.Schema
import org.squeryl.annotations.Column
import org.squeryl.PrimitiveTypeMode._
import com.curt.database.Database


class Years(val YearID: Int)
class Makes(val ID: Int, val AAIAMakeID: Int, val MakeName: String)
class Models(val ID: Int, val AAIAModelID: Int, val ModelName: String, val VehicleTypeID: Int)
class Submodels(val ID: Int, val AAIASubmodelID: Int, val SubmodelName: String)
class BaseVehicles(val ID: Int, val AAIABaseVehicleID: String, val YearID: Int, val MakeID: Int, val ModelID: Int)
class Vehicles(val ID: Int, val BaseVehicleID: Int, val SubModelID: Int, val ConfigID: Int, val AppID: Int)
class VehicleParts(val ID: Int, val VehicleID: Int, val PartNumber: Int)





class Vehicle(year:Double, make:String, model:String, submodel:String, Config:List[String]) extends Database with Schema{

	def  this() = { this(0,"","","",List[String]()) }
	def this(year:Double) = { this(year,"","","",List[String]()) }
	def this(year:Double,make:String) = { this(year,make,"","",List[String]()) }
	def this(year:Double,make:String,model:String) = { this(year,make,model,"",List[String]()) }
	def this(year:Double,make:String,model:String,submodel:String) = { this(year,make,model,submodel,List[String]()) }

	def getYears = {
		val years = ListBuffer[Double]()
		val dbYears = table[Years]("vcdb_Year")

		val all_years = from(dbYears)(y =>
			select(y.YearID)
			orderBy(y.YearID desc))
		for(year <- all_years){
			years += year.toDouble
		}
		years.toList
	}

	def getMakes = {

		val dbMakes = table[Makes]("vcdb_Make")
		val dbBase = table[BaseVehicles]("BaseVehicle")
		val dbVehicle = table[Vehicles]("vcdb_Vehicle")
		val dbVehicleParts = table[VehicleParts]("vcdb_VehiclePart")

		val makes = from(dbMakes,dbBase,dbVehicle,dbVehicleParts)((makes,base,vehicle,parts) =>
			where(base.MakeID === makes.ID and vehicle.BaseVehicleID === base.ID))

		// Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
		// val conn = DriverManager.getConnection("jdbc:sqlserver://srjbmn26rz.database.windows.net;database=CurtDev;user=discounthitch;password=eC0mm3rc3")

		// val statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
		// val rs = statement.executeQuery("select distinct ma.MakeName as make from vcdb_Year y "+
		// 				"join BaseVehicle bv on y.YearID = bv.YearID "+
		// 				"join vcdb_Make ma on bv.MakeID = ma.ID " +
		// 				"join vcdb_Vehicle v on bv.ID = v.BaseVehicleID " +
		// 				"join vcdb_VehiclePart vp on v.ID = vp.VehicleID " +
		// 				"where bv.YearID = " + this.year +
		// 				" order by ma.MakeName")

		val makes = ListBuffer[String]()
		// while(rs.next()){
		// 	makes += rs.getString("make")
			
		// }
		makes.toList
	}

	def getParts = {
		val parts = ListBuffer[Int]()
		parts.toList
	}
	def getGroups = {
		val parts = ListBuffer[Int]()
		parts.toList
	}
	override def toString:String= year+" "+make+" "+model+" "+submodel
}