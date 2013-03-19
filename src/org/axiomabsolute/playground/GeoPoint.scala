package org.axiomabsolute.playground

case class GeoPoint (latitude: Double, longitude: Double)

case class GeoPointAdapter(point: GeoPoint) extends LatLonPoint {
  val latitude = point.latitude
  val longitude = point.longitude
}