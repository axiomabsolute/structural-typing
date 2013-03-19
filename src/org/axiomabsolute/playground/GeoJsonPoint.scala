package org.axiomabsolute.playground

case class GeoJsonPoint (coordinates: (Double, Double), properties: Map [String, String])

case class GeoJsonPointAdapter(point: GeoJsonPoint) extends LatLonPoint{
  val (latitude, longitude) = point.coordinates
  
  val properties: Map[String, String] = point.properties
}