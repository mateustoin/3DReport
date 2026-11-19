package com.threedreport.app.data

import com.threedreport.core.model.Purchase
import com.threedreport.core.model.PurchaseLine
import com.threedreport.core.model.StockItemRef
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Uma pasta da 2.2 (sem os arquivos do estoque) abre igual, com estoque vazio e sem gravar nada novo; o `filaments.json`
 * gravado antes continua abrindo; e um backup antigo restaurado volta com o estoque vazio.
 */
class Leva12FormatCompatibilityTest {

    private lateinit var dataDir: File

    @BeforeTest
    fun setDataDir() {
        dataDir = createTempDirectory("3dreport-test").toFile()
        System.setProperty("threedreport.dataDir", dataDir.toString())
    }

    @AfterTest
    fun clearDataDir() {
        System.clearProperty("threedreport.dataDir")
    }

    /** Um filamento como a 2.2 grava: cor "Acabou" e cor sem nome, sem nada de saldo. */
    private val filamentsOf22 = """
        [
          {"id":"pla","createdAtEpochMillis":1000,"updatedAtEpochMillis":2000,"deletedAtEpochMillis":null,
           "data":{"id":"pla","name":"PLA","pricePerKg":100.0,"densityGPerCm3":1.24,"diameterMm":1.75,"brand":"Voolt","materialType":"PLA",
             "colors":[{"id":"red","name":"Vermelho","hex":"#E53935","inStock":false},{"id":"default","name":null,"hex":null,"inStock":true}],
             "archived":false}}
        ]
    """.trimIndent()

    /** Um pedido Pronto como a 2.2 grava, usando a cor vermelha. */
    private val readyOrderOf22 = """
        [
          {"id":"q1","createdAtEpochMillis":1000,"updatedAtEpochMillis":2000,"deletedAtEpochMillis":null,
           "data":{"id":"q1","name":"Vaso","savedAtEpochMillis":1000,"autoNamed":false,"number":42,"status":"ready",
             "statusHistory":[{"status":"quoted","atEpochMillis":1000},{"status":"ready","atEpochMillis":2000}],
             "quote":{"prints":[{"job":{"filaments":[{"filament":{"id":"pla","name":"PLA","pricePerKg":100.0,"densityGPerCm3":1.24},"lengthMeters":12.0,
                        "color":{"id":"red","name":"Vermelho","hex":"#E53935","inStock":true}}],"printTimeMinutes":190.0},
                      "printerId":"p","printerName":"Impressora","cost":{"material":3.58,"energy":1.48,"maintenance":0.54,"finishing":0.36,"investmentReturn":1.78,"fixedCost":0.0}}],
                      "costs":{"material":3.58,"energy":1.48,"maintenance":0.54,"failures":0.77,"finishing":0.36,"investmentReturn":1.78,"administrative":0.0,"labor":0.0,"fixedCost":0.0},
                      "salePrice":23.0}}}
        ]
    """.trimIndent()

    private val stockFiles = listOf("stock-items.json", "stock-movements.json", "purchases.json")

    @Test
    fun aFolderOf22OpensWithAnEmptyStockAndWritesNothingNew() {
        File(dataDir, "filaments.json").writeText(filamentsOf22)
        File(dataDir, "quotes.json").writeText(readyOrderOf22)

        val app = StockApp(dataDir)

        assertTrue(app.stock.items.value.isEmpty())
        assertTrue(app.stock.movements.value.isEmpty())
        assertTrue(app.stock.purchases.value.isEmpty())
        stockFiles.forEach { assertFalse(File(dataDir, it).exists(), "$it não deveria ser criado só por abrir o app") }
        // Os arquivos da 2.2 não são regravados só por abrir.
        assertEquals(filamentsOf22, File(dataDir, "filaments.json").readText())
        assertEquals(readyOrderOf22, File(dataDir, "quotes.json").readText())
    }

    @Test
    fun theFilamentsOf22OpenAsTheyWere() {
        File(dataDir, "filaments.json").writeText(filamentsOf22)

        val filament = StockApp(dataDir).filaments.filaments.value.single()

        assertEquals("PLA", filament.name)
        assertEquals(listOf(false, true), filament.colors.map { it.inStock })
        assertEquals(listOf("red", "default"), filament.colors.map { it.id })
    }

    @Test
    fun anOrderAlreadyReadyIn22IsNotDiscountedWhenTrackingStarts() {
        File(dataDir, "filaments.json").writeText(filamentsOf22)
        File(dataDir, "quotes.json").writeText(readyOrderOf22)
        val app = StockApp(dataDir, TestClock(now = 10_000L))
        val red = StockItemRef.ColorRef("pla", "red")

        app.stock.startTracking(red, 1000.0)
        app.clock.now += 1_000
        app.history.recordFollowUp("q1")
        app.history.updateStatus("q1", com.threedreport.core.model.OrderStatus.ENTREGUE)

        assertEquals(1, app.stock.movements.value.size, "só o início")
    }

    @Test
    fun anOldBackupWithoutTheStockFilesRestoresWithAnEmptyStock() {
        File(dataDir, "filaments.json").writeText(filamentsOf22)
        val oldBackup = BackupRepository().createBackupZip()

        val app = StockApp(dataDir)
        val red = StockItemRef.ColorRef("pla", "red")
        app.stock.startTracking(red, 1000.0)
        app.stock.savePurchase(Purchase(id = "p", dateEpochDay = 1, lines = listOf(PurchaseLine(red, 1000.0, 90.0))))
        stockFiles.forEach { assertTrue(File(dataDir, it).exists()) }

        assertIs<RestoreResult.Success>(BackupRepository().restoreFromZip(oldBackup))

        val restored = StockApp(dataDir)
        assertTrue(restored.stock.items.value.isEmpty())
        assertTrue(restored.stock.movements.value.isEmpty())
        assertTrue(restored.stock.purchases.value.isEmpty())
        assertEquals("PLA", restored.filaments.filaments.value.single().name)
    }

    @Test
    fun aNewBackupCarriesTheStock() {
        val app = StockApp(dataDir)
        val red = StockItemRef.ColorRef("pla", "red")
        app.stock.startTracking(red, 1000.0)
        val backup = BackupRepository().createBackupZip()
        app.stock.stopTracking(red)

        assertIs<RestoreResult.Success>(BackupRepository().restoreFromZip(backup))

        assertEquals(red, StockApp(dataDir).stock.items.value.single().ref)
    }
}
