package com.cdpjenkins.genetic.ui

import com.cdpjenkins.genetic.model.Evolver
import com.cdpjenkins.genetic.model.EvolverListener
import com.cdpjenkins.genetic.model.Individual
import javax.swing.SwingWorker

class EvolverWorker(val evolver: Evolver) : SwingWorker<Void?, Individual>() {
    private var listeners: MutableList<EvolverListener> = mutableListOf()

    fun addListener(listener: EvolverListener) {
        listeners.add(listener)
    }

    override fun doInBackground(): Void? {
        evolver.addListener { publish(it) }
        while (!isCancelled) {
            evolver.mutate()
        }
        return null
    }

    override fun process(chunks: MutableList<Individual>) {
        chunks.forEach { chunk ->
            listeners.forEach { listener ->
                listener.notify(chunk)
            }
        }
    }
}