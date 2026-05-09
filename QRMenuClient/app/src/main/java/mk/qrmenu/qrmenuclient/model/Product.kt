package mk.qrmenu.qrmenuclient.model

import com.google.firebase.firestore.DocumentId

data class Product(
    @DocumentId var id: String = "",
    var title: String = "",
    var description: String = "",
    var price: Double = 0.0,
    var imageUrl: String = "",
)
