# Structural Typing in Scala

Your company is working on a web app that collaborates betewen the various social media platforms its customers use.  Your task is to work on a new feature: given the location data for a group of friends, find a reasonable place for the group to meetup at.  Since the location data comes from a mixture of platforms, each data point will be in one of the following forms:

```scala
// Note, unless explicitly stated, all code samples are in the Scala programming language
case class KmlPoint (latitude: Double, longitude: Double, altitude: Double)

case class GeoPoint (latitude: Double, longitude: Double)

case class GeoJsonPoint (coordinates: (Double, Double), properties: Map [String, String])
```

You decide to write a function which consumes the point data and calculates the center of the group, which you can then use as a starting point to search for coffeshops, bars, and other attractions in the area, ensuring that the meetup location isn't too far from any one member.  There's only one problem: the point classes are defined in the external SDK for each service that provides the data, and so we cannot modify the classes to extend a common trait.  So, you employ the [adapter pattern][1],

```scala
trait LatLonPoint{
  val latitude: Double
  val longitude: Double
}

case class KmlPointAdapter(point: KmlPoint) extends LatLonPoint {
  val latitude = point.latitude
  val longitude = point.longitude
}

case class GeoPointAdapter(point: GeoPoint) extends LatLonPoint {
  val latitude = point.latitude
  val longitude = point.longitude
}

case class GeoJsonPointAdapter(point: GeoJsonPoint) extends LatLonPoint{
  val (latitude, longitude) = point.coordinates
  val properties: Map[String, String] = point.properties
}
```

which allows your write the central location finding method without discriminating on what type of point is being processed

```scala
val kmlPoints = LinkedList[KmlPoint](new KmlPoint(1.0, 2.0, 4))
val geoPoints = LinkedList[GeoPoint](new GeoPoint(3.0, 2.0))
val geoJsonPoints = LinkedList[GeoJsonPoint](new GeoJsonPoint((2.4, 5.0), null))

override def main(args:Array[String]) = {
  // Combine the points by wrapping then in their appropriate adapter
  val wrappedKmlPoints = kmlPoints.map( (a) => new KmlPointAdapter(a) ) 
  val wrappedGeoPoints = geoPoints.map( (a) => new GeoPointAdapter(a) ) 
  val wrappedGeoJsonPoints = geoJsonPoints.map( (a) => new GeoJsonPointAdapter(a) )
  
  val mixedPoints: LinkedList[LatLonPoint] = wrappedKmlPoints.union(wrappedGeoPoints.union(wrappedGeoJsonPoints))
  
  println("Median Point: " + MedianPoint(mixedPoints.toList))
}

def MedianPoint(points: List[LatLonPoint]): LatLonPoint = {
  // Add up all of the latitudes and longitudes in the list
  def sumPoint = points.foldLeft( (0.0, 0.0) )( (aggregate,latLonPt) =>
    (aggregate._1 + latLonPt.latitude, aggregate._2 + latLonPt.longitude) )
  
  // Divide the summed latitude and longitude to find the average
  new LatLonPoint{
    val latitude = sumPoint._1 / (1.0 * points.length)
    val longitude = sumPoint._2 / (1.0 * points.length)
  }
}
```

### What's in a name?

Consider the cost of adding this single behavior; a trait, three classes, and the boilerplate code of wrapping the point types.  Even worse, with social media platforms rising and falling in popularity each day it's likely that additional point data types will have to be introduced and, at some point, removed from the code base.  For every new point type, we must write a new adapter class and manually wrap the associated collection, bloating the codebase and duplicating data in memory (even if only temporarily).

But consider the `GeoPoint` point type and the `LatLonPoint` trait.  These two types define the exact same set of fields, but cannot be used interchangebly because `GeoPoint` does not formally satisfy the `LatLonPoint` trait, that is, we have not explicitly stated that `GeoPoint extends LatLonPoint`, and so, to the type checker, there is no subtype relationship between the two types.  This kind of type system, where all subtype relationships must be declared explicitly by name, is known as a Nominal Type System.  Even though the _structure_ of the data in the `GeoPoint` and `LatLonPoint` types match, we've had to create an entirely useless wrapper class, `GeoPointAdapter`, which does nothing but state the nominal subtype relationship and forward on the members of the `GeoPoint` class.  The same can be said of the `KmlPoint` type - it posseses the exact same internal structure as the `LatLonPoint` type, but simply augments it with one additional property.  The `KmlPointAdapter` exists _only_ to satisfy the nominal type checker.

### Structurally sound

Scala features an alternative method of determining type compatibility known as _Structural Typing_.  In structural type systems, the compiler inspects the structure of statically defined types and automatically _inferrs_ subtype relationships based on structural similarities.

```scala
type LatLon = {def latitude: Double; def longitude: Double}

def StructuralMedianPoint[LatLon](points: List[LatLon]): LatLon = {
  // Add up all of the latitudes and longitudes in the list
  def sumPoint = points.foldLeft( (0.0, 0.0) )( (aggregate,latLonPt) =>
    (aggregate._1 + latLonPt.latitude, aggregate._2 + latLonPt.longitude) )
  
  // Divide the summed latitude and longitude to find the average
  LatLonPt(
      sumPoint._1 / points.length,
      sumPoint._2 / points.length
  )
}
```

Notice that the named type parameter of the `points` list has been replaced by an anonymously defined structural definition which matches the `LatLonPoint` interface.  Now, any type which possesses the same members as `LatLonPoint`, namely `KmlPoint` and `GeoPoint`, will be type compatible with the `points` parameter's type.  This change eliminates the need for the wrapper classes for all but the `GeoJsonPoint` type, which does not possess the same internal structure as the `LatLonPoint` class.  But Scala also supports another feature to make this process even cleaner; implicit type conversions.

```scala
def StructuralMedianPoint[T <% LatLon](points: List[T]): LatLon = {
  // Exactly the same as definition above
}
```

Implicit conversions allow the compiler to convert between two incompatible types automatically.  Here, we define an implicit conversion from the `GeoJsonPoint` type to the `LatLonPoint` type, allowing `GeoJsonPoint` objects to be treated as if they satisfied the `LatLon` structural type.  In addition to adding the implicit conversion itself, we've modified the `findCenterOfPoints` signature with a _view bound_, specifying that the type parameter `A` must either be compatible with the anonymous structural definition or possess an implicit conversion which is compatible.

Using structural typing and implicit conversions, we eliminate the need for simple wrapper classes and boilerplate conversion code.  We maintain a clean class hierarchy and minimize the impact of introducing new types on the system.  At most, a single implicit conversion will have to be added when we introduce a new representation of a point, and even then only if the structure of the data doesn't match an existing type.  This produces an extremely flexible, minimal code base.  Here's the complete example:

```scala
type LatLon = {def latitude: Double; def longitude: Double}
case class LatLonPt(latitude: Double, longitude: Double)

implicit def GeoJsonPointToAdapter(in: GeoJsonPoint): LatLon = {
  LatLonPt(in.coordinates._1, in.coordinates._2)
}

def StructuralMedianPoint[T <% LatLon](points: List[T]): LatLon = {
  def sumPoint = points.foldLeft( (0.0, 0.0) )( (aggregate,latLonPt) =>
    (aggregate._1 + latLonPt.latitude, aggregate._2 + latLonPt.longitude) )
  
  LatLonPt(
      sumPoint._1 / points.length,
      sumPoint._2 / points.length
  )
}

override def main(args:Array[String]) = {
  val convertedGeoJsonPoints = LinkedList[LatLon](new GeoJsonPoint((2.4, 5.0), null))
  
  val baseList = (new LinkedList[LatLon])++ kmlPoints ++ geoPoints ++ convertedGeoJsonPoints
  
  println("Structural Median Point: " + StructuralMedianPoint(baseList.toList))
}
```

One structural type definition and a minimal implementing class, one implicit conversion per different structure, and our actual method; the minimum amount of information necessary to add our desired behavior.  A total of 24 liberally spaced lines.  Compare that to the full Adapter Pattern solution:

```scala
case class KmlPointAdapter(point: KmlPoint) extends LatLonPoint {
  val latitude = point.latitude
  val longitude = point.longitude
}

case class GeoPointAdapter(point: GeoPoint) extends LatLonPoint {
  val latitude = point.latitude
  val longitude = point.longitude
}

case class GeoJsonPointAdapter(point: GeoJsonPoint) extends LatLonPoint{
  val (latitude, longitude) = point.coordinates
  
  val properties: Map[String, String] = point.properties
}

trait LatLonPoint{
  val latitude: Double
  val longitude: Double
}

def MedianPoint(points: List[LatLonPoint]): LatLonPoint = {
  def sumPoint = points.foldLeft( (0.0, 0.0) )( (aggregate,latLonPt) =>
    (aggregate._1 + latLonPt.latitude, aggregate._2 + latLonPt.longitude) )
  
  new LatLonPoint{
    val latitude = sumPoint._1 / (1.0 * points.length)
    val longitude = sumPoint._2 / (1.0 * points.length)
  }
}

override def main(args:Array[String]) = {
  val wrappedKmlPoints = kmlPoints.map( (a) => new KmlPointAdapter(a) ) 
  val wrappedGeoPoints = geoPoints.map( (a) => new GeoPointAdapter(a) ) 
  val wrappedGeoJsonPoints = geoJsonPoints.map( (a) => new GeoJsonPointAdapter(a) )
  
  val mixedPoints: LinkedList[LatLonPoint] = wrappedKmlPoints.union(wrappedGeoPoints.union(wrappedGeoJsonPoints))
  
  println("Median Point: " + MedianPoint(mixedPoints.toList))
}
```

A new trait, a new case class for _every_ new point type, boilerplate code to wrap each type in the main method, all for almost the exact same behavior - the body of the `MedianPoint` method is exactly the same, all that changes is the type signature!  In 40 lines that will only continue to increase as new types are added.

### Was it worth it?

Structural typing can be somewhat difficult to get right for relatively new users of the Scala programming language (including me).  Most non-trivial cases will involve some use of implicit type conversions, which require users to be comfortable with the concept of view bounds, understand how implicit conversions are triggered, and how to give the Scala compiler the right hints when working with collections of structurally typed objects.  This can create complicated type signatures that appear overwhelming for new users.

Another potential drawback from using structural typing in Scala is performance.  Structural types in Scala are implemented through the Java reflection API which, depending on how it is used, can lead to significant performance penalties.  In general, [unless used extensively or in very tight loops][3], the performance impact of using Scala's structural typing capabilities should be negligable, but should be considered nonetheless.

Finally, structural typing introduces the concern for accidental interface implementation.  It's completely possible that a class could unintentionally satisfy a type which defines vague or commonly named members.  For example, a `JsonSerializable` type might require only a `toString` and `parse` method, which could potentially be implicitly satisfied by an `XmlSerializable` class that defined the same two methods, even though the structure of the text data expected as input and produced as output are entirely different.  In practice, this is usually mitigated by using more specific names for methods, like `parseJson` and `serializeToJson`, reserving very general names like `parse` for interfaces, like `Parsable`.

On the other hand, structural types make it very easy to work with classes defined in external libraries, especially when the classes follow a naming convention.  As it's written above, we could add any number of classes with `latitude: Double` and `longitude: Double` properties, or a `latLon: (Double, Double` property, without having to alter the code.  New implicit conversions are only required when a data type with a different structure is introduced, preventing code duplication.  This is extremely helpful if, for example, a library exposes several client classes without exposing a common parent interface, such as entities automatically produced by examining a database or objects converted from JSON web service responses.

### Too Long; Didn't Read;

Scala implements an optional, alternative type system based on inferring subtype relationships based on structural similarties of objects and type parameters.  By splitting the traditional role of inheritence as both a method of code reuse and to declare subtype relationships, structural typing allows users to interchangably use types without creating wrapper classes simply to create a subtype relationship.  While this does add some complication to the type signatures in the system and has performance considerations, using structural typing can create very flexible, clean type hierarchies for data types which follow some naming convention, either intentionally due to some underlying type hierarchy that has not been conveyed to the user, or through coincidence due to the nature of the domain space.

As a little extra, I've included a second solution to the problem in another language featuring structural typing: Google's [Go programming language][4].  All of the code above and a discussion of the differences of structural typing in Go can be found on my [public GitHub][5].




[1]: http://en.wikipedia.org/wiki/Adapter_pattern
[2]: http://en.wikipedia.org/wiki/Duck_typing
[3]: http://infoscience.epfl.ch/record/138931/files/2009_structural.pdf
[4]: http://www.golang.org
[5]: https://github.com/axiomabsolute