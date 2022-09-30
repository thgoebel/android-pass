package me.proton.core.pass.autofill.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class FieldType : Parcelable {
    FullName,
    Username,
    Email,
    Password,
    Phone,
    Other
}
