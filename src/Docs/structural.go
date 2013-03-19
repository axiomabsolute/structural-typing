package main

import "fmt"

/*
	Our three unmodifiable data types
*/
type KmlPoint struct {
	latitude, longitude float64

	// Optional
	altitude float64
}

type GeoPoint struct {
	latitude, longitude float64
}

type GeoJsonPoint struct {
	latLon [2]float64
	properties map[string]string
}

/*
	Extract common interface
*/
type LatLonPoint interface {
	getLatitude() float64
	getLongitude() float64
}

/*
	Adapt structures by adding methods; Interfaces only define methods, so properties must be wrapped
*/
func (p KmlPoint) getLatitude() float64 {
	return p.latitude
}

func (p KmlPoint) getLongitude() float64 {
	return p.longitude
}

func (p GeoPoint) getLatitude() float64 {
	return p.latitude
}

func (p GeoPoint) getLongitude() float64 {
	return p.longitude
}

func (p GeoJsonPoint) getLatitude() float64 {
	return p.latLon[0]
}

func (p GeoJsonPoint) getLongitude() float64 {
	return p.latLon[1]
}

/*
	Define generic function in terms of interface
*/
func averagePoint(points []LatLonPoint) (float64, float64) {
	var totalLat, totalLon float64
	for _, point := range points {
		totalLat += point.getLatitude()
		totalLon += point.getLongitude()
	}
	return totalLat/float64(len(points)), totalLon/float64(len(points))
}

/*
	Demo
*/
func main() {
	pt1 := KmlPoint{1.0, 2.0, 1.4}
	pt2 := GeoPoint{2.2, 2.3}
	pt3 := GeoJsonPoint{[2]float64{2.4, .2}, map[string]string{}}
	points := []LatLonPoint{pt1, pt2, pt3}
	fmt.Println(averagePoint(points))
}