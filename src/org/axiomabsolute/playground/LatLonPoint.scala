package org.axiomabsolute.playground

trait LatLonPoint{
	val latitude: Double
	val longitude: Double
	
	override def toString():String = {
	  "(" + latitude + ", " + longitude + ")"
	}
}