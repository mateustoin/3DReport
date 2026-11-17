package com.threedreport.app.ui.text

import _3dreport.composeapp.generated.resources.Res
import _3dreport.composeapp.generated.resources.pick_failed_too_large
import _3dreport.composeapp.generated.resources.pick_failed_unreadable
import com.threedreport.app.platform.PickFailure
import com.threedreport.app.platform.PickResult

/** O texto de um arquivo que não foi lido, citando o nome dele. */
fun PickResult.Failed.text(): UiText = uiText(
    when (reason) {
        PickFailure.UNREADABLE -> Res.string.pick_failed_unreadable
        PickFailure.TOO_LARGE -> Res.string.pick_failed_too_large
    },
    fileName,
)
