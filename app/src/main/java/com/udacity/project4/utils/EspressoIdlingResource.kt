package com.udacity.project4.utils

import androidx.test.espresso.idling.CountingIdlingResource

/**
 * Objeto de utilidade para gerenciar um recurso de inatividade do Espresso,
 * permitindo sincronizar operações assíncronas com os testes do Espresso.
 */
object EspressoIdlingResource {

    // Recurso de inatividade para rastrear operações assíncronas.
    val countingIdlingResource = CountingIdlingResource("Global")

    /**
     * Incrementa o contador do recurso de inatividade.
     * Deve ser chamado antes de iniciar uma operação assíncrona.
     */
    fun increment() {
        countingIdlingResource.increment()
    }

    /**
     * Decrementa o contador do recurso de inatividade.
     * Deve ser chamado após a conclusão de uma operação assíncrona.
     */
    fun decrement() {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        }
    }
}

/**
 * Envolva operações assíncronas para sincronizá-las com testes do Espresso.
 * Incrementa antes da operação e decrementa após a conclusão.
 *
 * @param function A função representando a operação assíncrona.
 * @return O resultado da função.
 */
inline fun <T> wrapEspressoIdlingResource(function: () -> T): T {
    EspressoIdlingResource.increment() // Marcar como ocupado antes da operação.
    return try {
        function()
    } finally {
        EspressoIdlingResource.decrement() // Marcar como ocioso após a operação.
    }
}
