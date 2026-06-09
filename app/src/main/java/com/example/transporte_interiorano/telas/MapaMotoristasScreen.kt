package com.example.transporte_interiorano.telas

import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint
import androidx.compose.ui.platform.LocalContext
import com.example.transporte_interiorano.BancoDeDados
import com.example.transporte_interiorano.MotoristaOnline

@Composable
fun MapaMotoristasScreen(aoClicarVoltar: () -> Unit) {
    // 1. Criamos um estado para guardar a lista que vem do servidor
    var listaMotoristas by remember { mutableStateOf(listOf<MotoristaOnline>()) }

    // 2. Quando a tela abrir, buscamos os motoristas na nuvem
    LaunchedEffect(Unit) {
        BancoDeDados.buscarMotoristasOnline { lista ->
            listaMotoristas = lista
        }
    }

    AndroidView(factory = { ctx ->
        MapView(ctx).apply {
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(-8.054, -34.881))
        }
    }, update = { mapView ->
        // 3. Limpa os pinos antigos antes de desenhar os novos
        mapView.overlays.clear()

        // 4. Desenha um pino para cada motorista que veio do servidor
        listaMotoristas.forEach { motorista ->
            val pino = Marker(mapView)
            pino.position = GeoPoint(motorista.latitude, motorista.longitude)
            pino.title = motorista.nome // Nome do motorista
            mapView.overlays.add(pino)
        }

        // Refresca o mapa para aparecer os novos pinos
        mapView.invalidate()
    })
}