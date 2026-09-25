package com.threedreport.app.data

import kotlin.time.ExperimentalTime

/**
 * Relógio injetável. Os repositórios carimbam cada registro com o momento da mudança (decisão 106), e
 * os testes precisam controlar esse momento.
 */
fun interface Clock {
    fun nowMillis(): Long

    companion object {
        @OptIn(ExperimentalTime::class)
        val System: Clock = Clock { kotlin.time.Clock.System.now().toEpochMilliseconds() }
    }
}
