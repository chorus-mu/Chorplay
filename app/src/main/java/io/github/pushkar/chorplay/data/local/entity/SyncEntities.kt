package io.github.pushkar.chorplay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_log", primaryKeys = ["peerDeviceId", "collection", "payloadHash"])
data class SyncLogEntity(
    val peerDeviceId: String,
    val collection: String,
    val payloadHash: String,
    val appliedAt: Long,
    val hwmHlc: String,
)

@Entity(tableName = "sync_peers")
data class SyncPeerEntity(
    @PrimaryKey val deviceId: String,
    val deviceName: String,
    val platform: String,
    val lastSyncedAt: Long,
)
