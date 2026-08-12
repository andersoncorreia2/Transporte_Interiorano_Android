package com.example.transporte_interiorano

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
//import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", "Token capturado: $token")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM_TESTE", "Mensagem recebida do Firebase!")

        val titulo = remoteMessage.notification?.title ?: "Nova Solicitação de Carona!"
        val mensagem = remoteMessage.notification?.body ?: "Alguém quer uma vaga na sua corrida."

        mostrarNotificacao(titulo, mensagem)
    }

    private fun mostrarNotificacao(titulo: String, mensagem: String) {
        // 🟢 CORREÇÃO 1: Mudamos o ID do canal! O Android vai ser forçado a criar um canal novo,
        // esquecendo o antigo que estava "silenciado" e aplicando a força máxima agora.
        val channelId = "canal_caronas_urgente_v2"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Avisos de Carona Urgente",
                NotificationManager.IMPORTANCE_HIGH // Garante o Heads-up (pular na tela)
            ).apply {
                description = "Alertas sonoros de pedidos de vagas"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Configuração da Intenção de clique para abrir o App a partir do Balão
        val intentClique = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("AÇÃO_NOTIFICACAO", "ABRIR_MAPA")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(),
            intentClique,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Monta a notificação visual de forma limpa vinculando o clique
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setPriority(NotificationCompat.PRIORITY_MAX) // 🟢 CORREÇÃO 2: Prioridade MAX!
            .setDefaults(NotificationCompat.DEFAULT_ALL)  // 🟢 CORREÇÃO 3: Força o uso do som e vibração altos do próprio celular
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(Random.nextInt(), builder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Novo token gerado: $token")
    }
}