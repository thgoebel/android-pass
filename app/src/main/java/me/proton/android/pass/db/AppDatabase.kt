package me.proton.android.pass.db

import android.content.Context
import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import me.proton.core.account.data.db.AccountConverters
import me.proton.core.account.data.db.AccountDatabase
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.account.data.entity.AccountMetadataEntity
import me.proton.core.account.data.entity.SessionDetailsEntity
import me.proton.core.account.data.entity.SessionEntity
import me.proton.core.challenge.data.db.ChallengeConverters
import me.proton.core.challenge.data.db.ChallengeDatabase
import me.proton.core.challenge.data.entity.ChallengeFrameEntity
import me.proton.core.crypto.android.keystore.CryptoConverters
import me.proton.core.data.room.db.BaseDatabase
import me.proton.core.data.room.db.CommonConverters
import me.proton.core.eventmanager.data.db.EventManagerConverters
import me.proton.core.eventmanager.data.db.EventMetadataDatabase
import me.proton.core.eventmanager.data.entity.EventMetadataEntity
import me.proton.core.featureflag.data.db.FeatureFlagDatabase
import me.proton.core.featureflag.data.entity.FeatureFlagEntity
import me.proton.core.humanverification.data.db.HumanVerificationConverters
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.humanverification.data.entity.HumanVerificationEntity
import me.proton.core.key.data.db.KeySaltDatabase
import me.proton.core.key.data.db.PublicAddressDatabase
import me.proton.core.key.data.entity.KeySaltEntity
import me.proton.core.key.data.entity.PublicAddressEntity
import me.proton.core.key.data.entity.PublicAddressKeyEntity
import me.proton.core.pass.data.db.PassDatabase
import me.proton.core.pass.data.db.entities.ItemEntity
import me.proton.core.pass.data.db.entities.ItemKeyEntity
import me.proton.core.pass.data.db.entities.ShareEntity
import me.proton.core.pass.data.db.entities.VaultKeyEntity
import me.proton.core.payment.data.local.db.PaymentDatabase
import me.proton.core.payment.data.local.entity.GooglePurchaseEntity
import me.proton.core.user.data.db.AddressDatabase
import me.proton.core.user.data.db.UserConverters
import me.proton.core.user.data.db.UserDatabase
import me.proton.core.user.data.entity.AddressEntity
import me.proton.core.user.data.entity.AddressKeyEntity
import me.proton.core.user.data.entity.UserEntity
import me.proton.core.user.data.entity.UserKeyEntity
import me.proton.core.usersettings.data.db.OrganizationDatabase
import me.proton.core.usersettings.data.db.UserSettingsConverters
import me.proton.core.usersettings.data.db.UserSettingsDatabase
import me.proton.core.usersettings.data.entity.OrganizationEntity
import me.proton.core.usersettings.data.entity.OrganizationKeysEntity
import me.proton.core.usersettings.data.entity.UserSettingsEntity

@Database(
    entities = [
        // Core
        AccountEntity::class,
        AccountMetadataEntity::class,
        AddressEntity::class,
        AddressKeyEntity::class,
        ChallengeFrameEntity::class,
        EventMetadataEntity::class,
        FeatureFlagEntity::class,
        GooglePurchaseEntity::class,
        HumanVerificationEntity::class,
        KeySaltEntity::class,
        OrganizationEntity::class,
        OrganizationKeysEntity::class,
        PublicAddressEntity::class,
        PublicAddressKeyEntity::class,
        SessionDetailsEntity::class,
        SessionEntity::class,
        UserEntity::class,
        UserKeyEntity::class,
        UserSettingsEntity::class,
        // Pass
        ItemEntity::class,
        ItemKeyEntity::class,
        ShareEntity::class,
        VaultKeyEntity::class
    ],
    version = AppDatabase.VERSION,
    exportSchema = true
)
@TypeConverters(
    // Core
    CommonConverters::class,
    AccountConverters::class,
    UserConverters::class,
    CryptoConverters::class,
    HumanVerificationConverters::class,
    UserSettingsConverters::class,
    EventManagerConverters::class,
    ChallengeConverters::class
)
abstract class AppDatabase :
    BaseDatabase(),
    AccountDatabase,
    AddressDatabase,
    ChallengeDatabase,
    EventMetadataDatabase,
    FeatureFlagDatabase,
    HumanVerificationDatabase,
    KeySaltDatabase,
    OrganizationDatabase,
    PassDatabase,
    PaymentDatabase,
    PublicAddressDatabase,
    UserDatabase,
    UserSettingsDatabase {

    companion object {
        const val VERSION = 3

        private val migrations: List<Migration> = listOf(
            AppDatabaseMigrations.MIGRATION_1_2,
            AppDatabaseMigrations.MIGRATION_2_3
        )

        fun buildDatabase(context: Context): AppDatabase =
            databaseBuilder<AppDatabase>(context, "db-passkey")
                .apply { migrations.forEach { addMigrations(it) } }
                .fallbackToDestructiveMigration()
                .build()
    }
}
