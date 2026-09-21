package com.threedreport.core.stl

import kotlin.math.sqrt

/** Vetor/ponto 3D simples, usado pela malha STL e pelo visualizador. */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vec3(x / scalar, y / scalar, z / scalar)

    infix fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    infix fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    val length: Float get() = sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val len = length
        return if (len == 0f) this else this / len
    }

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }
}
