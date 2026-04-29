package com.androiddriver.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object RoadRouter {
    private const val OSRM_BASE = "https://router.project-osrm.org"
    private const val TIMEOUT = 10000

    /**
     * Get road distance (km) and route geometry from OSRM
     */
    suspend fun getRoute(lat1: Double, lng1: Double, lat2: Double, lng2: Double): RouteResult? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$OSRM_BASE/route/v1/driving/$lng1,$lat1;$lng2,$lat2?overview=full&geometries=geojson")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = TIMEOUT
                conn.readTimeout = TIMEOUT
                conn.requestMethod = "GET"

                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(response)
                if (json.getString("code") != "Ok") return@withContext null

                val route = json.getJSONArray("routes").getJSONObject(0)
                val distanceMeters = route.getDouble("distance")
                val distanceKm = distanceMeters / 1000.0

                // Parse route geometry (GeoJSON format: [lng, lat] pairs)
                val geometry = route.getJSONObject("geometry")
                val coordsArray = geometry.getJSONArray("coordinates")
                val points = mutableListOf<Pair<Double, Double>>()
                for (i in 0 until coordsArray.length()) {
                    val coord = coordsArray.getJSONArray(i)
                    val lng = coord.getDouble(0)
                    val lat = coord.getDouble(1)
                    points.add(Pair(lat, lng))
                }

                RouteResult(
                    distanceKm = Math.round(distanceKm * 100.0) / 100.0,
                    durationMinutes = Math.round(route.getDouble("duration") / 60.0),
                    routePoints = points
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    data class RouteResult(
        val distanceKm: Double,
        val durationMinutes: Long,
        val routePoints: List<Pair<Double, Double>>
    )
}
