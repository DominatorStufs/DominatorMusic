-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

#noinspection ShrinkerUnresolvedReference
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    #noinspection ShrinkerUnresolvedReference
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    #noinspection ShrinkerUnresolvedReference
    kotlinx.serialization.KSerializer serializer(...);
}

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder

-dontwarn app.vitune.compose.persist.PersistKt
-dontwarn app.vitune.compose.persist.PersistMap
-dontwarn app.vitune.compose.persist.PersistMapCleanupKt
-dontwarn app.vitune.compose.persist.PersistMapKt
-dontwarn app.vitune.compose.preferences.PreferencesHolder
-dontwarn app.vitune.compose.preferences.SharedPreferencesProperty
-dontwarn app.vitune.compose.reordering.AnimateItemPlacementKt
-dontwarn app.vitune.compose.reordering.DraggedItemKt
-dontwarn app.vitune.compose.reordering.ReorderKt
-dontwarn app.vitune.compose.reordering.ReorderingState
-dontwarn app.vitune.compose.reordering.ReorderingStateKt
-dontwarn app.vitune.compose.routing.GlobalRouteKt
-dontwarn app.vitune.compose.routing.Route0
-dontwarn app.vitune.compose.routing.Route1
-dontwarn app.vitune.compose.routing.Route3
-dontwarn app.vitune.compose.routing.Route4
-dontwarn app.vitune.compose.routing.Route
-dontwarn app.vitune.compose.routing.RouteHandlerKt
-dontwarn app.vitune.compose.routing.RouteHandlerScope
-dontwarn app.vitune.compose.routing.RouteRequest
-dontwarn app.vitune.compose.routing.TransitionsKt