package com.example.transporte_interiorano

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Método chamado quando o serviço é criado, força a exibição do token
    override fun onCreate() {
        super.onCreate()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", "Token capturado: $token")

                // Exemplo: se você tiver o e-mail salvo em algum lugar, chame aqui:
                // enviarTokenParaServidor("seu-email@aqui.com", token)
            }
        }
    }

    // Método chamado quando uma mensagem chega enquanto o app está aberto
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM_TESTE", "Mensagem recebida: ${remoteMessage.notification?.body}")
    }

    // Método chamado quando um novo token é gerado pelo servidor
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Novo token gerado: $token")
    }
}