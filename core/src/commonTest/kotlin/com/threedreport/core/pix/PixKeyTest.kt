package com.threedreport.core.pix

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Normalização da chave Pix antes de entrar no código (decisão 145, item 1), um caso por tipo. */
class PixKeyTest {

    @Test
    fun aPhoneBecomesPlus55AreaCodeNumber() {
        assertEquals(PixKey("+5511999990000", PixKeyType.PHONE), PixKey.parse("(11) 99999-0000"))
        assertEquals(PixKey("+5511999990000", PixKeyType.PHONE), PixKey.parse("+55 11 99999-0000"))
        assertEquals(PixKey("+551133334444", PixKeyType.PHONE), PixKey.parse("11 3333-4444"))
        // Com o 0 e a operadora da ligação interurbana, como no link do WhatsApp.
        assertEquals(PixKey("+5511999990000", PixKeyType.PHONE), PixKey.parse("0 21 11 99999-0000"))
    }

    @Test
    fun aCpfKeepsOnlyTheDigits() {
        assertEquals(PixKey("52998224725", PixKeyType.CPF), PixKey.parse("529.982.247-25"))
        // Onze dígitos sem pontuação: CPF se os dígitos verificadores batem; senão é celular com DDD.
        assertEquals(PixKey("52998224725", PixKeyType.CPF), PixKey.parse("52998224725"))
        assertEquals(PixKey("+5511999990000", PixKeyType.PHONE), PixKey.parse("11999990000"))
    }

    @Test
    fun aCnpjKeepsOnlyTheDigits() {
        assertEquals(PixKey("11222333000181", PixKeyType.CNPJ), PixKey.parse("11.222.333/0001-81"))
        assertEquals(PixKey("11222333000181", PixKeyType.CNPJ), PixKey.parse("11222333000181"))
    }

    @Test
    fun theAlphanumericCnpjKeepsLettersAndDigits() {
        // CNPJ alfanumérico da Receita (a partir de 2026): letras e dígitos, sem a pontuação.
        assertEquals(PixKey("12ABC34501DE35", PixKeyType.CNPJ), PixKey.parse("12.abc.345/01de-35"))
    }

    @Test
    fun anEmailIsLowercase() {
        assertEquals(PixKey("loja@exemplo.com.br", PixKeyType.EMAIL), PixKey.parse("  Loja@Exemplo.com.BR "))
    }

    @Test
    fun aRandomKeyStaysAsItCame() {
        val evp = "123e4567-e12b-12d1-a456-426655440000"
        assertEquals(PixKey(evp, PixKeyType.RANDOM), PixKey.parse("  $evp "))
    }

    @Test
    fun anUnknownKeyIsAcceptedAsTyped() {
        val key = PixKey.parse("chave estranha")

        assertEquals(PixKey("chave estranha", PixKeyType.UNKNOWN), key)
        assertFalse(key!!.isRecognized)
        assertTrue(PixKey.parse("a@b.co")!!.isRecognized)
    }

    @Test
    fun aForeignPhoneIsNotAPixPhone() {
        assertEquals(PixKeyType.UNKNOWN, PixKey.parse("+1 415 555 0100")?.type)
    }

    @Test
    fun blankIsNoKey() {
        assertNull(PixKey.parse("   "))
        assertNull(PixKey.parse(null))
    }
}
