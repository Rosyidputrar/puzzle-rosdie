package id.rosdie.puzzle.data.repository

import java.util.TimeZone

/**
 * Tanggal lokal sebagai kunci yyyyMMdd dan hari-epoch, tanpa java.time (minSdk 24 tanpa
 * desugaring). Konversi sipil memakai algoritma Howard Hinnant (days_from_civil).
 */
object DateKeys {
    fun epochDayOf(millis: Long, zone: TimeZone = TimeZone.getDefault()): Long =
        Math.floorDiv(millis + zone.getOffset(millis), 86_400_000L)

    fun keyOf(millis: Long, zone: TimeZone = TimeZone.getDefault()): Int = keyFromEpochDay(epochDayOf(millis, zone))

    fun keyFromEpochDay(epochDay: Long): Int {
        val (y, m, d) = civilFromDays(epochDay)
        return y * 10_000 + m * 100 + d
    }

    fun epochDayFromKey(key: Int): Long = daysFromCivil(key / 10_000, key / 100 % 100, key % 100)

    fun year(key: Int) = key / 10_000
    fun month(key: Int) = key / 100 % 100
    fun day(key: Int) = key % 100

    /** 0 = Senin ... 6 = Minggu. */
    fun dayOfWeekMondayFirst(epochDay: Long): Int = Math.floorMod(epochDay + 3, 7L).toInt()

    fun daysInMonth(year: Int, month: Int): Int =
        (daysFromCivil(if (month == 12) year + 1 else year, if (month == 12) 1 else month + 1, 1) -
            daysFromCivil(year, month, 1)).toInt()

    fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = (if (month <= 2) year - 1 else year).toLong()
        val era = Math.floorDiv(y, 400L)
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    fun civilFromDays(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719_468
        val era = Math.floorDiv(z, 146_097L)
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
        return Triple((if (m <= 2) y + 1 else y).toInt(), m, d)
    }
}

/** Sumber waktu yang bisa diganti di test. */
interface TimeSource {
    /** Waktu monotonik untuk timer permainan. */
    fun elapsedMs(): Long

    /** Waktu dinding (epoch ms). */
    fun nowMs(): Long

    fun todayKey(): Int = DateKeys.keyOf(nowMs())

    fun todayEpochDay(): Long = DateKeys.epochDayOf(nowMs())
}

object SystemTimeSource : TimeSource {
    override fun elapsedMs(): Long = System.nanoTime() / 1_000_000
    override fun nowMs(): Long = System.currentTimeMillis()
}
