package com.threedreport.app.platform

import kotlinx.coroutines.runBlocking

/** Os textos dos documentos pro cliente, lidos dos recursos de verdade, pros testes que geram PDF e imagem. */
val testClientTexts: ClientDocumentTexts by lazy { runBlocking { loadClientDocumentTexts(pieceCounts = 1..100) } }
