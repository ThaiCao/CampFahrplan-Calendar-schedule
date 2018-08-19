package nerd.tuxmobil.fahrplan.congress.net

data class FetchScheduleResult(

        val httpStatus: HttpStatus,
        val scheduleXml: String = "",
        val eTag: String = "",
        val hostName: String,
        val exceptionMessage: String = ""

)
