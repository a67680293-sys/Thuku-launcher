package com.thuku.launcher

import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal data class WeatherSnapshot(
    val place: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windKph: Double,
    val weatherCode: Int,
    val isDay: Boolean
) {
    val description: String get() = weatherDescription(weatherCode, isDay)
}

/** Small, bounded Open-Meteo client. It uses a city entered by the user, never device location. */
internal object WeatherRepository {
    private data class CacheEntry(val cityKey: String, val unit: String, val fetchedAt: Long, val snapshot: WeatherSnapshot)
    @Volatile private var cached: CacheEntry? = null
    private const val CACHE_TTL_MILLIS = 30 * 60 * 1_000L

    fun fetchCurrent(cityInput: String, unit: String): WeatherSnapshot {
        val city = cityInput.filter { it.code >= 0x20 && it.code != 0x7F }.trim().take(80)
        require(city.isNotBlank()) { "Enter a city name first." }
        require(unit == "C" || unit == "F") { "Unsupported temperature unit." }
        val cityKey = city.lowercase()
        val now = System.currentTimeMillis()
        cached?.takeIf { it.cityKey == cityKey && it.unit == unit && now - it.fetchedAt in 0..CACHE_TTL_MILLIS }?.let { return it.snapshot }
        val result = fetchUncached(city, unit)
        cached = CacheEntry(cityKey, unit, now, result)
        return result
    }

    private fun fetchUncached(city: String, unit: String): WeatherSnapshot {
        val encoded = URLEncoder.encode(city, Charsets.UTF_8.name())
        val geocode = getJson("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json")
        val first = geocode.optJSONArray("results")?.optJSONObject(0)
            ?: throw IllegalStateException("No matching city found. Check the spelling and try again.")
        val latitude = first.optDouble("latitude", Double.NaN)
        val longitude = first.optDouble("longitude", Double.NaN)
        require(latitude.isFinite() && longitude.isFinite()) { "The weather service returned an invalid location." }
        val place = listOf(first.optString("name"), first.optString("admin1"), first.optString("country"))
            .filter { it.isNotBlank() }.distinct().joinToString(", ").take(100)
        val temperatureUnit = if (unit == "F") "fahrenheit" else "celsius"
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,is_day,wind_speed_10m" +
            "&temperature_unit=$temperatureUnit&wind_speed_unit=kmh&timezone=auto"
        val current = getJson(url).optJSONObject("current")
            ?: throw IllegalStateException("Weather data isn't available right now.")
        return WeatherSnapshot(
            place = place.ifBlank { city },
            temperature = current.optDouble("temperature_2m", Double.NaN),
            feelsLike = current.optDouble("apparent_temperature", Double.NaN),
            humidity = current.optInt("relative_humidity_2m", -1).coerceIn(-1, 100),
            windKph = current.optDouble("wind_speed_10m", Double.NaN),
            weatherCode = current.optInt("weather_code", -1),
            isDay = current.optInt("is_day", 1) == 1
        ).also {
            require(it.temperature.isFinite() && it.feelsLike.isFinite()) { "Weather data was incomplete. Please refresh." }
        }
    }

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 6_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ThukuLauncher/0.5.0 (Android launcher; weather requested by user)")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                ?: throw IllegalStateException("Weather service is unavailable (HTTP $code).")
            val bytes = BufferedInputStream(stream).use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= 96 * 1024) { "Weather response was unexpectedly large." }
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            }
            if (code !in 200..299) throw IllegalStateException("Weather service returned HTTP $code.")
            return JSONObject(bytes.toString(Charsets.UTF_8))
        } finally {
            connection.disconnect()
        }
    }
}

internal fun weatherDescription(code: Int, day: Boolean = true): String = when (code) {
    0 -> if (day) "Clear sky" else "Clear night"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Foggy"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61, 63, 65 -> "Rain"
    66, 67 -> "Freezing rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Weather unavailable"
}
