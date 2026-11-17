package com.threedreport.core.stl

/*
 * Tabelas com tipos primitivos pra percorrer STL de milhões de triângulos. Um HashMap com Triple/Pair/Integer
 * gasta perto de 100 bytes por entrada; aqui cada entrada custa os poucos ints que ela tem. Uso interno do
 * pacote, sem remoção (não precisa).
 */

/** Espalha os bits de um hash (finalizador do MurmurHash3) pra tabela de endereçamento aberto. */
private fun mix(value: Int): Int {
    var h = value
    h = h xor (h ushr 16)
    h *= -0x7a143595
    h = h xor (h ushr 13)
    h *= -0x3d4d51cb
    return h xor (h ushr 16)
}

private fun capacityFor(expected: Int): Int {
    var capacity = 16
    while (capacity < expected.toLong() * 2 && capacity < (1 shl 30)) capacity = capacity shl 1
    return capacity
}

/**
 * Dá um número sequencial (0, 1, 2…) a cada trio de inteiros, na ordem em que aparece. Serve pra vértice
 * quantizado na análise e pra célula da grade na simplificação.
 */
internal class Int3IdTable(expectedSize: Int = 16) {
    private var capacity = capacityFor(expectedSize)
    private var keys = IntArray(capacity * 3)
    private var ids = IntArray(capacity) { -1 }

    var size: Int = 0
        private set

    /** O número de (x, y, z), criando um novo (igual ao [size] de antes) se o trio ainda não existe. */
    fun idOf(x: Int, y: Int, z: Int): Int {
        if ((size + 1).toLong() * 10 > capacity.toLong() * 7) grow()
        var slot = slotOf(x, y, z)
        while (true) {
            val id = ids[slot]
            if (id < 0) {
                keys[slot * 3] = x
                keys[slot * 3 + 1] = y
                keys[slot * 3 + 2] = z
                ids[slot] = size
                return size++
            }
            if (keys[slot * 3] == x && keys[slot * 3 + 1] == y && keys[slot * 3 + 2] == z) return id
            slot = (slot + 1) and (capacity - 1)
        }
    }

    private fun slotOf(x: Int, y: Int, z: Int): Int = mix(x * 73856093 xor mix(y * 19349663 xor mix(z * 83492791))) and (capacity - 1)

    private fun grow() {
        val oldKeys = keys
        val oldIds = ids
        val oldCapacity = capacity
        capacity = oldCapacity shl 1
        keys = IntArray(capacity * 3)
        ids = IntArray(capacity) { -1 }
        for (old in 0 until oldCapacity) {
            val id = oldIds[old]
            if (id < 0) continue
            val x = oldKeys[old * 3]
            val y = oldKeys[old * 3 + 1]
            val z = oldKeys[old * 3 + 2]
            var slot = slotOf(x, y, z)
            while (ids[slot] >= 0) slot = (slot + 1) and (capacity - 1)
            keys[slot * 3] = x
            keys[slot * 3 + 1] = y
            keys[slot * 3 + 2] = z
            ids[slot] = id
        }
    }
}

/** Conjunto de `Long` (sem o valor `Long.MIN_VALUE`, usado como vazio). */
internal class LongHashSet(expectedSize: Int = 16) {
    private var capacity = capacityFor(expectedSize)
    private var slots = LongArray(capacity) { EMPTY }

    var size: Int = 0
        private set

    /** Põe [value] no conjunto; `false` se já estava. */
    fun add(value: Long): Boolean {
        if ((size + 1).toLong() * 10 > capacity.toLong() * 7) grow()
        var slot = slotOf(value)
        while (true) {
            val current = slots[slot]
            if (current == EMPTY) {
                slots[slot] = value
                size++
                return true
            }
            if (current == value) return false
            slot = (slot + 1) and (capacity - 1)
        }
    }

    private fun slotOf(value: Long): Int = mix(value.toInt() xor mix((value ushr 32).toInt())) and (capacity - 1)

    private fun grow() {
        val old = slots
        capacity = capacity shl 1
        slots = LongArray(capacity) { EMPTY }
        for (value in old) {
            if (value == EMPTY) continue
            var slot = slotOf(value)
            while (slots[slot] != EMPTY) slot = (slot + 1) and (capacity - 1)
            slots[slot] = value
        }
    }

    private companion object {
        const val EMPTY = Long.MIN_VALUE
    }
}

/** Lista de `Int` que cresce, sem caixa por elemento. */
internal class IntList(initialCapacity: Int = 16) {
    var values = IntArray(maxOf(initialCapacity, 4))
        private set
    var size: Int = 0
        private set

    fun add(value: Int) {
        if (size == values.size) values = values.copyOf(grow(values.size))
        values[size++] = value
    }

    operator fun get(index: Int): Int = values[index]

    operator fun set(index: Int, value: Int) {
        values[index] = value
    }
}

/** Lista de `Long` que cresce, sem caixa por elemento. */
internal class LongList(initialCapacity: Int = 16) {
    var values = LongArray(maxOf(initialCapacity, 4))
        private set
    var size: Int = 0
        private set

    fun add(value: Long) {
        if (size == values.size) values = values.copyOf(grow(values.size))
        values[size++] = value
    }
}

/** Lista de `Double` que cresce, sem caixa por elemento. */
internal class DoubleList(initialCapacity: Int = 16) {
    var values = DoubleArray(maxOf(initialCapacity, 4))
        private set
    var size: Int = 0
        private set

    fun add(value: Double) {
        if (size == values.size) values = values.copyOf(grow(values.size))
        values[size++] = value
    }

    operator fun get(index: Int): Double = values[index]

    operator fun set(index: Int, value: Double) {
        values[index] = value
    }
}

/** Cresce em 50%, sem passar do maior array possível. */
private fun grow(current: Int): Int = (current.toLong() + (current shr 1) + 4).coerceAtMost(Int.MAX_VALUE - 8L).toInt()
