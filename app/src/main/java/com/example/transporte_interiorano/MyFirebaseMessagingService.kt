package com.example.transporte_interiorano

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
//import java.net.HttpURLConnection
//import java.net.URL
//import kotlin.concurrent.thread
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlin.random.Random

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
        super.onMessageReceived(remoteMessage)

        Log.d("FCM_TESTE", "Mensagem recebida do Firebase!")  //: ${remoteMessage.notification?.body}")

        // Captura o título e o corpo enviados pelo servidor Python, ou usa um texto padrão
        val titulo = remoteMessage.notification?.title ?: "Nova Solicitação de Carona!"
        val mensagem = remoteMessage.notification?.body ?: "Alguém quer uma vaga na sua corrida."

        mostrarNotificacao(titulo, mensagem)
    }

    // 🟢 ADICIONADO: Função que constrói e exibe o alerta visual no celular
    private fun mostrarNotificacao(titulo: String, mensagem: String) {
        val channelId = "canal_caronas_id"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // No Android 8.0+, o SOM e a VIBRAÇÃO devem ser configurados obrigatoriamente no CANAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Avisos de Carona",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de pedidos de vagas"
                enableLights(true)
                enableVibration(true)
                // O IMPORTANCE_HIGH já força o som padrão do sistema no canal automaticamente
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Monta a notificação visual de forma limpa (sem o setDefaults descontinuado)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Mantido para retrocompatibilidade
            .setAutoCancel(true)

        // Usa um ID aleatório para acumular notificações sem sobrepor
        notificationManager.notify(Random.nextInt(), builder.build())
    }

    // Método chamado quando um novo token é gerado pelo servidor
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Novo token gerado: $token")
    }
}