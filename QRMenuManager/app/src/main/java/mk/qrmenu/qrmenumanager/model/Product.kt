package mk.qrmenu.qrmenumanager.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Product(
    @DocumentId var id: String = "",
    var title: String = "",
    var description: String = "",
    var price: Double = 0.0,
    var imageUrl: String = "",
    var category: String = "",
    @ServerTimestamp var createdAt: Date? = null,
) : Parcelable
