package org.axiomabsolute.playground

import scala.collection.mutable.LinkedList

object StructuralTypingDemo extends Application {

  def MedianPoint(points: List[LatLonPoint]): LatLonPoint = {
    // Add up all of the latitudes and longitudes in the list
    def sumPoint = points.foldLeft( (0.0, 0.0) )( (aggregate,latLonPt) => (aggregate._1 + latLonPt.latitude, aggregate._2 + latLonPt.longitude) )
    
    // Divide the summed latitude and longitude to find the average
    new LatLonPoint{
      val latitude = sumPoint._1 / (1.0 * points.length)
      val longitude = sumPoint._2 / (1.0 * points.length)
    }
  }
  
  // Type alias for the desired structural type
  type LatLon = {def latitude: Double; def longitude: Double}
  
  case class LatLonPt(latitude: Double, longitude: Double)
  implicit def GeoJsonPointToAdapter(in: GeoJsonPoint): LatLon = {
    LatLonPt(in.coordinates._1, in.coordinates._2)
  }
  
  def StructuralMedianPoint[T <% LatLon](points: List[T]): LatLon = {
    def sumPoint = points.foldLeft( (0.0, 0.0) )( (aggregate,latLonPt) => (aggregate._1 + latLonPt.latitude, aggregate._2 + latLonPt.longitude) )
    
    LatLonPt(
        sumPoint._1 / points.length,
        sumPoint._2 / points.length
    )
  }
  
  override def main(args:Array[String]) = {
    // Shared data set
    val kmlPoints = LinkedList[KmlPoint](new KmlPoint(1.0, 2.0, 4))
    val geoPoints = LinkedList[GeoPoint](new GeoPoint(3.0, 2.0))
    val geoJsonPoints = LinkedList[GeoJsonPoint](new GeoJsonPoint((2.4, 5.0), null))
    
    // First, with Adapter pattern
    // Combine the points by wrapping then in their appropriate adapter
    val wrappedKmlPoints = kmlPoints.map( (a) => new KmlPointAdapter(a) ) 
    val wrappedGeoPoints = geoPoints.map( (a) => new GeoPointAdapter(a) ) 
    val wrappedGeoJsonPoints = geoJsonPoints.map( (a) => new GeoJsonPointAdapter(a) )
    
    val mixedPoints: LinkedList[LatLonPoint] = wrappedKmlPoints.union(wrappedGeoPoints.union(wrappedGeoJsonPoints))
    
    println("Median Point: " + MedianPoint(mixedPoints.toList))
    
    // Now let's try with structural typing
    val convertedGeoJsonPoints = LinkedList[LatLon](new GeoJsonPoint((2.4, 5.0), null))
    
    val baseList = (new LinkedList[LatLon])++ kmlPoints ++ geoPoints ++ convertedGeoJsonPoints
    
    println("Structural Median Point: " + StructuralMedianPoint(baseList.toList))
    
  }
  
}