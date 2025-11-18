package com.apptolast.greenhousefronts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representa el payload completo recibido del sensor, mapeando las claves del JSON
 * a propiedades de la clase utilizando @SerialName.
 */
@Serializable
data class GreenhouseMessage(
    @SerialName("timestamp") val timestamp: String = "",

    // Temperaturas
    @SerialName("TEMPERATURA INVERNADERO 01") val temperaturaInvernadero01: Double = 0.0,
    @SerialName("TEMPERATURA INVERNADERO 02") val temperaturaInvernadero02: Double = 0.0,
    @SerialName("TEMPERATURA INVERNADERO 03") val temperaturaInvernadero03: Double = 0.0,

    // Humedades
    @SerialName("HUMEDAD INVERNADERO 01") val humedadInvernadero01: Double = 0.0,
    @SerialName("HUMEDAD INVERNADERO 02") val humedadInvernadero02: Double = 0.0,
    @SerialName("HUMEDAD INVERNADERO 03") val humedadInvernadero03: Double = 0.0,

    // Sectores Invernadero 01
    @SerialName("INVERNADERO_01_SECTOR_01") val invernadero01Sector01: Double = 0.0,
    @SerialName("INVERNADERO_01_SECTOR_02") val invernadero01Sector02: Double = 0.0,
    @SerialName("INVERNADERO_01_SECTOR_03") val invernadero01Sector03: Double = 0.0,
    @SerialName("INVERNADERO_01_SECTOR_04") val invernadero01Sector04: Double = 0.0,

    // Sectores Invernadero 02
    @SerialName("INVERNADERO_02_SECTOR_01") val invernadero02Sector01: Double = 0.0,
    @SerialName("INVERNADERO_02_SECTOR_02") val invernadero02Sector02: Double = 0.0,
    @SerialName("INVERNADERO_02_SECTOR_03") val invernadero02Sector03: Double = 0.0,
    @SerialName("INVERNADERO_02_SECTOR_04") val invernadero02Sector04: Double = 0.0,

    // Sectores Invernadero 03
    @SerialName("INVERNADERO_03_SECTOR_01") val invernadero03Sector01: Double = 0.0,
    @SerialName("INVERNADERO_03_SECTOR_02") val invernadero03Sector02: Double = 0.0,
    @SerialName("INVERNADERO_03_SECTOR_03") val invernadero03Sector03: Double = 0.0,
    @SerialName("INVERNADERO_03_SECTOR_04") val invernadero03Sector04: Double = 0.0,

    // Extractores
    @SerialName("INVERNADERO_01_EXTRACTOR") val invernadero01Extractor: Double = 0.0,
    @SerialName("INVERNADERO_02_EXTRACTOR") val invernadero02Extractor: Double = 0.0,
    @SerialName("INVERNADERO_03_EXTRACTOR") val invernadero03Extractor: Double = 0.0,

    // Reserva
    @SerialName("RESERVA") val reserva: Double = 0.0,

    @SerialName("greenhouseId") val greenhouseId: String = "",
    @SerialName("tenantId") val tenantId: String = "",
)

/**
 * Clase de utilidad para agrupar los datos de un único invernadero de una forma
 * más limpia y manejable para usar en la UI u otras lógicas.
 */
data class GreenhouseData(
    val id: Int,
    val temperatura: Double?,
    val humedad: Double?,
    val sectores: List<Double?>,
    val extractor: Double?
)

/**
 * Función de extensión para transformar el mensaje plano (GreenhouseMessage)
 * en una lista de objetos GreenhouseData, uno por cada invernadero.
 *
 * Esto hace que trabajar con los datos en el resto de la app sea mucho más fácil.
 */
fun GreenhouseMessage.toGroupedData(): List<GreenhouseData> {
    return listOf(
        GreenhouseData(
            id = 1,
            temperatura = this.temperaturaInvernadero01,
            humedad = this.humedadInvernadero01,
            sectores = listOf(
                this.invernadero01Sector01,
                this.invernadero01Sector02,
                this.invernadero01Sector03,
                this.invernadero01Sector04
            ),
            extractor = this.invernadero01Extractor
        ),
        GreenhouseData(
            id = 2,
            temperatura = this.temperaturaInvernadero02,
            humedad = this.humedadInvernadero02,
            sectores = listOf(
                this.invernadero02Sector01,
                this.invernadero02Sector02,
                this.invernadero02Sector03,
                this.invernadero02Sector04
            ),
            extractor = this.invernadero02Extractor
        ),
        GreenhouseData(
            id = 3,
            temperatura = this.temperaturaInvernadero03,
            humedad = this.humedadInvernadero03,
            sectores = listOf(
                this.invernadero03Sector01,
                this.invernadero03Sector02,
                this.invernadero03Sector03,
                this.invernadero03Sector04
            ),
            extractor = this.invernadero03Extractor
        )
    )
}
