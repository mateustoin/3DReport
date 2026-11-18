package com.threedreport.core.pix

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** BR Code estático do Pix (decisão 145, item 2). */
class PixPayloadTest {

    @Test
    fun crcMatchesTheStandardCheckValue() {
        // Valor de conferência do CRC-16/CCITT-FALSE (polinômio 0x1021, início 0xFFFF).
        assertEquals("29B1", PixPayload.crc16("123456789"))
    }

    @Test
    fun matchesTheExampleOfTheBrCodeManual() {
        // Exemplo do Manual de Padrões para Iniciação do Pix (Banco Central), Pix estático sem valor.
        val expected = "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000" +
            "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***63041D3D"

        val payload = PixPayload.build(
            key = "123e4567-e12b-12d1-a456-426655440000",
            merchantName = "Fulano de Tal",
            merchantCity = "BRASILIA",
            amount = null,
            txid = "***",
        )

        assertEquals(expected, payload)
    }

    @Test
    fun theAmountGoesWithADotAndTwoDecimals() {
        val payload = PixPayload.build(key = "a@b.co", merchantName = "LOJA", merchantCity = "SP", amount = 174.34, txid = "3DR0042")

        assertTrue(payload.contains("5406174.34"), payload)
        assertTrue(payload.contains("62110507" + "3DR0042"), payload)
        assertTrue(PixPayload.build("a@b.co", "LOJA", "SP", 5.0, "***").contains("54045.00"))
        assertTrue(PixPayload.build("a@b.co", "LOJA", "SP", 0.1 + 0.2, "***").contains("54040.30"))
    }

    @Test
    fun noAmountFieldWhenZero() {
        val payload = PixPayload.build(key = "a@b.co", merchantName = "LOJA", merchantCity = "SP", amount = 0.0, txid = "***")

        assertTrue(payload.contains("5303986" + "5802BR"), payload)
    }

    @Test
    fun theCrcClosesThePayload() {
        val payload = PixPayload.build(key = "+5511999990000", merchantName = "MARIA", merchantCity = "CAMPINAS", amount = 50.0, txid = "3DR0001")
        val body = payload.dropLast(4)

        assertTrue(body.endsWith("6304"))
        assertEquals(PixPayload.crc16(body), payload.takeLast(4))
    }

    @Test
    fun theTxidIsTheOrderNumberOrStars() {
        assertEquals("3DR0042", PixPayload.txidFor(42))
        assertEquals("3DR12345", PixPayload.txidFor(12345))
        assertEquals("***", PixPayload.txidFor(0))
    }

    @Test
    fun nameAndCityLoseAccentsAreUppercaseAndCut() {
        assertEquals("JOAO DA CONCEICAO", PixPayload.sanitizeName("  João da   Conceição "))
        assertEquals("ESTUDIO DE IMPRESSAO 3D D", PixPayload.sanitizeName("Estúdio de Impressão 3D da Ana"))
        assertEquals(25, PixPayload.sanitizeName("Estúdio de Impressão 3D da Ana").length)
        assertEquals("SAO JOSE DOS CA", PixPayload.sanitizeCity("São José dos Campos"))
        assertEquals("SAO PAULO", PixPayload.sanitizeCity("São Paulo"))
        // Corte que cairia num espaço não deixa espaço no fim.
        assertEquals("RIO DE JANEIRO", PixPayload.sanitizeCity("Rio de Janeiro X"))
    }

    @Test
    fun theFieldLengthCountsUtf8Bytes() {
        // Os campos vão em ASCII; o tamanho é contado em bytes, como o CRC e o QR leem o texto.
        assertFailsWith<IllegalArgumentException> {
            PixPayload.build(key = "joão@b.co", merchantName = "LOJA", merchantCity = "SP", amount = null, txid = "***")
        }
        val payload = PixPayload.build(key = "joao@b.co", merchantName = "LOJA", merchantCity = "SP", amount = null, txid = "***")
        assertEquals(payload.length, payload.encodeToByteArray().size)
        assertTrue(payload.contains("0109joao@b.co"), payload)
    }

    @Test
    fun aKeyTooLongForTheFieldIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            PixPayload.build(key = "a".repeat(78), merchantName = "LOJA", merchantCity = "SP", amount = null, txid = "***")
        }
    }
}
