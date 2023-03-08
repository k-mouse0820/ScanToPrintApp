package jp.daisen_solution.scantoprintapp

import jp.co.toshibatec.bcp.library.BCPControl

object Consts {

    // BCP Print 用の定数
    const val AsynchronousMode = 1 // 送信完了復帰（非同期）


    const val bcpFolderPath = "/TOSHIBATEC/BCP_Print_for_Android"
    const val bcpSectionName = "bcpCommonData"
    var bcpControl: BCPControl? = null



}