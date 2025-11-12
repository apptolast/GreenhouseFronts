package com.apptolast.greenhousefronts.util

/**
 * Proveedor de fecha/hora multiplataforma usando expect/actual pattern
 *
 * Este patrón permite implementaciones específicas de plataforma cuando
 * no hay una biblioteca multiplataforma disponible o cuando se requiere
 * acceso directo a APIs nativas.
 */
expect fun getCurrentTimestamp(): String
