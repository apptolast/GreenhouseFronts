package com.apptolast.greenhousefronts.di

import org.koin.core.module.Module

/**
 * Expect declaration for platform-specific dependencies
 * Each platform (Android, iOS, Desktop, Web) provides its own implementation
 */
expect fun platformModule(): Module
