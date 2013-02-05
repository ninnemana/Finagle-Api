package com.curt.vehicle

//import java.sql.{Connection, DriverManager, ResultSet}
import scala.collection.mutable.ListBuffer
import org.squeryl.SessionFactory
import org.squeryl.{Schema, KeyedEntity, Session, Query,Queryable}
import org.squeryl.annotations.Column
import org.squeryl.PrimitiveTypeMode._
import com.curt.database.Database


// The root object of the schema. Inheriting KeyedEntity[T] is not
// mandatory it just makes primary key methods availabile(delete and lookup) on tables.
abstract class VehicleObject extends KeyedEntity[Int]{
	val ID: Int = 0
}

class Years(val YearID: Double) {

	// IMPORTANT : currently classes with Option[] members *must* provide a zero arg
	// constructor where every Option[T] member gets initialized with Some(t:T).
	// or else Squeryl will not be able to reflect the type of the field, and an exception will
	// be thrown at table instantiation time.
	def this() = this(0)

	def all = {
		//Thread.sleep(1000)
		println("inside all")
		from(CurtDev.years)(y => select (y.YearID)orderBy(y.YearID desc)).toList
	}

	def get_makes = {
		join(CurtDev.makes, CurtDev.baseVehicles, CurtDev.vehicles,CurtDev.vehicleParts)((m,bv,v,vp) =>
			where(bv.YearID === YearID)
			select((m.MakeName))
			on((m.ID === bv.MakeID),
			(bv.ID === v.BaseVehicleID),
			(v.ID === vp.VehicleID))).distinct
	}

}
class Makes(val ID: Int, val AAIAMakeID: Int, val MakeName: String){

	def this() = this(0,0,"")

	def all = {
		from(CurtDev.makes)(m => select(m.MakeName)orderBy(m.MakeName asc)).toList
	}

	def get_models(y:Double,m:String) = {
		join(CurtDev.makes, CurtDev.models, CurtDev.baseVehicles, CurtDev.vehicles, CurtDev.vehicleParts)((ma, mo, bv, v, vp) =>
			where(bv.YearID === y and ma.MakeName === m)
			select((mo.ModelName))
			on((mo.ID === bv.ModelID),
				(ma.ID === bv.MakeID),
				(bv.ID === v.BaseVehicleID),
				(v.ID === vp.VehicleID))).distinct
	}

}
class Models(val ID: Int, val AAIAModelID: Int, val ModelName: String, val VehicleTypeID: Int)
class Submodels(val ID: Int, val AAIASubmodelID: Int, val SubmodelName: String)
class BaseVehicles(val ID: Int, val AAIABaseVehicleID: Int, val YearID: Int, val MakeID: Int, val ModelID: Int)
class Vehicles(val ID: Int, val BaseVehicleID: Int, val SubModelID: Int, val ConfigID: Int, val AppID: Int)
class VehicleParts(val ID: Int, val VehicleID: Int, val PartNumber: Int)

class Vehicle(year:Double, make:String, model:String, submodel:String, config: List[String]){

	def this() = this(0,"","","",List[String]())
	def this(y:Double) = this(y,"","","",List[String]())
	def this(year:Double,make:String) = this(year,make,"","",List[String]())
	def this(year:Double,make:String,model:String) = this(year,make,model,"",List[String]())
	def this(year:Double,make:String,model:String,submodel:String) = this(year,make,model,submodel,List[String]())

	def years = new Years().all

	def makes = {
		val years = new Years(year)
		years.get_makes
	}

	def models = {
		val makes = new Makes
		makes.get_models(year,make)
	}

}

object CurtDev extends Schema {
	val years = table[Years]("vcdb_Year")
	val makes = table[Makes]("vcdb_Make")
	val models = table[Models]("vcdb_Model")
	val baseVehicles = table[BaseVehicles]("BaseVehicle")
	val vehicles = table[Vehicles]("vcdb_Vehicle")
	val vehicleParts = table[VehicleParts]("vcdb_VehiclePart")
}

// class Vehicle(year:Double, make:String, model:String, submodel:String, Config:List[String]) extends Database with Schema{

// 	def  this() = { this(0,"","","",List[String]()) }
// 	def this(year:Double) = { this(year,"","","",List[String]()) }
// 	def this(year:Double,make:String) = { this(year,make,"","",List[String]()) }
// 	def this(year:Double,make:String,model:String) = { this(year,make,model,"",List[String]()) }
// 	def this(year:Double,make:String,model:String,submodel:String) = { this(year,make,model,submodel,List[String]()) }

// 	def getYears = {
// 		val years = ListBuffer[Double]()
// 		val dbYears = table[Years]("vcdb_Year")

// 		val all_years = from(dbYears)(y =>
// 			select(y.YearID)
// 			orderBy(y.YearID desc))
// 		for(year <- all_years){
// 			years += year.toDouble
// 		}
// 		years.toList
// 	}

// 	def getMakes = {

// 		val dbMakes = table[Makes]("vcdb_Make")
// 		val dbBase = table[BaseVehicles]("BaseVehicle")
// 		val dbVehicle = table[Vehicles]("vcdb_Vehicle")
// 		val dbVehicleParts = table[VehicleParts]("vcdb_VehiclePart")

// 		// val makes = from(dbMakes,dbBase,dbVehicle,dbVehicleParts)((makes,base,vehicle,parts) =>
// 		// 	where(base.MakeID === makes.ID and vehicle.BaseVehicleID === base.ID))

// 		// Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
// 		// val conn = DriverManager.getConnection("jdbc:sqlserver://srjbmn26rz.database.windows.net;database=CurtDev;user=discounthitch;password=eC0mm3rc3")

// 		// val statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
// 		// val rs = statement.executeQuery("select distinct ma.MakeName as make from vcdb_Year y "+
// 		// 				"join BaseVehicle bv on y.YearID = bv.YearID "+
// 		// 				"join vcdb_Make ma on bv.MakeID = ma.ID " +
// 		// 				"join vcdb_Vehicle v on bv.ID = v.BaseVehicleID " +
// 		// 				"join vcdb_VehiclePart vp on v.ID = vp.VehicleID " +
// 		// 				"where bv.YearID = " + this.year +
// 		// 				" order by ma.MakeName")

// 		val makes = ListBuffer[String]()
// 		// while(rs.next()){
// 		// 	makes += rs.getString("make")
			
// 		// }
// 		makes.toList
// 	}

// 	def getParts = {
// 		val parts = ListBuffer[Int]()
// 		parts.toList
// 	}
// 	def getGroups = {
// 		val parts = ListBuffer[Int]()
// 		parts.toList
// 	}
// 	override def toString:String= year+" "+make+" "+model+" "+submodel
// }