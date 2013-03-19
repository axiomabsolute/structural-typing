package org.axiomabsolute.playground

case class KmlPoint (latitude: Double, longitude: Double, altitude: Double)

//case class KmlPointAdapter(latitude: Double, longitude: Double, alt: Double) extends LatLonPoint

case class KmlPointAdapter(point: KmlPoint) extends LatLonPoint {
  val latitude = point.latitude
  val longitude = point.longitude
}