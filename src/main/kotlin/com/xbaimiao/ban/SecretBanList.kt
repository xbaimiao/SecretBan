package com.xbaimiao.ban

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.xbaimiao.easylib.database.dsl.dao
import com.xbaimiao.easylib.database.dsl.wrapper.select

@DatabaseTable(tableName = "secret_ban")
data class SecretBanDao(
    @DatabaseField(generatedId = true)
    var id: Int = 0,
    @DatabaseField(columnName = "player", canBeNull = false)
    var player: String = "",
    @DatabaseField(columnName = "expire", canBeNull = false)
    var expire: Long = 0
) {

    constructor() : this(0, "", 0)

}

object SecretBanList {

    private val dao = SecretBanDao::class.dao<SecretBanDao, Int>()

    fun addSecretBan(player: String, time: Long) {
        val old = dao.select { SecretBanDao::player eq player }
        val expire = if (time <= 0) Long.MAX_VALUE else System.currentTimeMillis() + time
        if (old != null) {
            old.expire = expire
            dao.update(old)
            return
        }
        dao.create(SecretBanDao(player = player, expire = expire))
    }

    fun removeSecretBan(player: String): Boolean {
        val old = dao.select { SecretBanDao::player eq player }
        if (old != null) {
            old.expire = 0
            dao.update(old)
            return true
        }
        return false
    }

    fun isSecretBan(player: String): Boolean {
        val expire = dao.select { SecretBanDao::player eq player }?.expire ?: 0
        return expire > System.currentTimeMillis()
    }

}