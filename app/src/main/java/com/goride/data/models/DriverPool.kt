package com.goride.data.models

object DriverPool {

    fun getDrivers(vehicleType: String, baseFare: Float): List<DriverModel> {
        return when (vehicleType.uppercase()) {
            "ECONOMY" -> economyDrivers(baseFare)
            "COMFORT" -> comfortDrivers(baseFare)
            else      -> normalDrivers(baseFare)   // "NORMAL" + any unknown type
        }
    }

    // ── NORMAL ────────────────────────────────────────────────────────────────

    private fun normalDrivers(base: Float) = listOf(
        DriverModel(
            name        = "Ahmed Khan",
            rating      = 4.8f,
            vehicle     = "Toyota Corolla",
            plateNumber = "LHR-2341",
            etaMinutes  = 3,
            fare        = (base - 10f).coerceAtLeast(50f)
        ),
        DriverModel(
            name        = "Bilal Raza",
            rating      = 4.6f,
            vehicle     = "Honda City",
            plateNumber = "LHR-5567",
            etaMinutes  = 5,
            fare        = base
        ),
        DriverModel(
            name        = "Usman Ali",
            rating      = 4.7f,
            vehicle     = "Toyota Yaris",
            plateNumber = "LHR-3390",
            etaMinutes  = 4,
            fare        = (base + 15f)
        ),
        DriverModel(
            name        = "Faisal Sheikh",
            rating      = 4.5f,
            vehicle     = "Honda Civic",
            plateNumber = "LHR-7723",
            etaMinutes  = 6,
            fare        = (base - 5f).coerceAtLeast(50f)
        ),
        DriverModel(
            name        = "Tariq Mehmood",
            rating      = 4.9f,
            vehicle     = "Suzuki Cultus",
            plateNumber = "LHR-9912",
            etaMinutes  = 2,
            fare        = (base + 20f)
        )
    )

    // ── ECONOMY ───────────────────────────────────────────────────────────────

    private fun economyDrivers(base: Float) = listOf(
        DriverModel(
            name        = "Nadeem Iqbal",
            rating      = 4.3f,
            vehicle     = "Suzuki Alto",
            plateNumber = "LHR-4421",
            etaMinutes  = 4,
            fare        = (base - 15f).coerceAtLeast(50f)
        ),
        DriverModel(
            name        = "Rashid Hussain",
            rating      = 4.1f,
            vehicle     = "Suzuki Wagon R",
            plateNumber = "LHR-6634",
            etaMinutes  = 6,
            fare        = base
        ),
        DriverModel(
            name        = "Imran Butt",
            rating      = 4.4f,
            vehicle     = "Suzuki Swift",
            plateNumber = "LHR-1187",
            etaMinutes  = 3,
            fare        = (base + 10f)
        ),
        DriverModel(
            name        = "Kamran Zafar",
            rating      = 4.2f,
            vehicle     = "Daihatsu Cuore",
            plateNumber = "LHR-8856",
            etaMinutes  = 7,
            fare        = (base - 10f).coerceAtLeast(50f)
        ),
        DriverModel(
            name        = "Sohail Akhtar",
            rating      = 4.5f,
            vehicle     = "Suzuki Mehran",
            plateNumber = "LHR-3302",
            etaMinutes  = 5,
            fare        = (base + 5f)
        )
    )

    // ── COMFORT ───────────────────────────────────────────────────────────────

    private fun comfortDrivers(base: Float) = listOf(
        DriverModel(
            name        = "Zubair Malik",
            rating      = 4.9f,
            vehicle     = "Toyota Fortuner",
            plateNumber = "LHR-1122",
            etaMinutes  = 5,
            fare        = (base - 20f).coerceAtLeast(100f)
        ),
        DriverModel(
            name        = "Hassan Mirza",
            rating      = 4.8f,
            vehicle     = "Hyundai Tucson",
            plateNumber = "LHR-7745",
            etaMinutes  = 7,
            fare        = base
        ),
        DriverModel(
            name        = "Asad Qureshi",
            rating      = 4.7f,
            vehicle     = "KIA Sportage",
            plateNumber = "LHR-5531",
            etaMinutes  = 4,
            fare        = (base + 25f)
        ),
        DriverModel(
            name        = "Omar Farooq",
            rating      = 4.9f,
            vehicle     = "Toyota Prado",
            plateNumber = "LHR-9901",
            etaMinutes  = 8,
            fare        = (base + 50f)
        ),
        DriverModel(
            name        = "Salman Chaudhry",
            rating      = 4.6f,
            vehicle     = "Honda CR-V",
            plateNumber = "LHR-4478",
            etaMinutes  = 6,
            fare        = (base - 10f).coerceAtLeast(100f)
        )
    )
}
