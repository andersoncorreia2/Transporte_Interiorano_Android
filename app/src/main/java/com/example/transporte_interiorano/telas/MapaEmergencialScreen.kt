package com.example.transporte_interiorano.telas

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.BancoDeDados
import com.example.transporte_interiorano.ui.theme.AzulPrincipal
import com.example.transporte_interiorano.ui.theme.VerdeBotao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaEmergencialScreen(
    isMotorista: Boolean,
    latitudeAtual: Double,
    longitudeAtual: Double,
    motoristaOnlineGlobal: Boolean,
    corridaCriadaIdGlobal: Int?,
    corridaAceitaMotoristaGlobalStr: String?,
    tempoCancelamentoGlobal: Int,
    aoClicarVoltar: () -> Unit,
    aoChamarMotorista: (String, String, (Int) -> Unit) -> Unit,
    aoLimparCorridaGlobal: () -> Unit,
    aoAtualizarTempoCancelamentoGlobal: (Int) -> Unit,
    aoAtualizarCorridaAceitaMotoristaGlobal: (String?) -> Unit,
    aoAlternarDisponibilidadeMotorista: (Boolean) -> Unit
) {
    val contexto = LocalContext.current
    val escopoCorrotina = rememberCoroutineScope()

    val dispararNotificacaoComSom = { titulo: String, mensagem: String ->
        val notificationManager = contexto.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val canalId = "notificacoes_emergentes"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val canal = android.app.NotificationChannel(canalId, "Alertas Emergenciais", android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Canal para notifications sonoras de corridas emergenciais"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION), null)
            }
            notificationManager.createNotificationChannel(canal)
        }

        val intentCliqueLocal = android.content.Intent(contexto, com.example.transporte_interiorano.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("AÇÃO_NOTIFICACAO", "ABRIR_MAPA")
        }

        val pendingIntentLocal = android.app.PendingIntent.getActivity(
            contexto,
            kotlin.random.Random.nextInt(),
            intentCliqueLocal,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val construtor = androidx.core.app.NotificationCompat.Builder(contexto, canalId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .setContentIntent(pendingIntentLocal)

        if (androidx.core.content.ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(System.currentTimeMillis().toInt(), construtor.build())
        }
    }

    var mapaRef by remember { mutableStateOf<MapView?>(null) }
    var enderecoOrigem by remember { mutableStateOf("") }
    var enderecoDestino by remember { mutableStateOf("") }
    val paradasExtras = remember { mutableStateListOf<String>() }
    var tipoVeiculoSelecionado by remember { mutableStateOf("Carro") }

    val sugestoes = remember { mutableStateListOf<String>() }
    var expandido by remember { mutableStateOf(false) }
    var localidadeIdentificadaReal by remember { mutableStateOf("sua região") }
    var painelMinimizado by remember { mutableStateOf(false) }

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var primeiraCentralizacaoRealizada by remember { mutableStateOf(false) }
    var deixarCameraLivrePassageiro by remember { mutableStateOf(false) }

    val chamadosRecusadosIds = remember { mutableStateListOf<Int>() }

    // Auxiliar para gerar ícones customizados escalados de Carro ou Moto de forma nativa e limpa
    fun criarMarcadorVeiculoIcon(textoEmoji: String): Drawable {
        val tamanhoPx = (40 * contexto.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(tamanhoPx, tamanhoPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            textSize = tamanhoPx * 0.8f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.drawText(textoEmoji, tamanhoPx / 2f, tamanhoPx * 0.75f, paint)
        return BitmapDrawable(contexto.resources, bitmap)
    }

    LaunchedEffect(latitudeAtual, longitudeAtual) {
        escopoCorrotina.launch(Dispatchers.IO) {
            try {
                val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$latitudeAtual&lon=$longitudeAtual&format=json&addressdetails=1&zoom=18")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.setRequestProperty("User-Agent", contexto.packageName)

                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resposta)
                    val address = json.optJSONObject("address")
                    val cidadeReal = address?.optString("city")
                        ?: address?.optString("town")
                        ?: address?.optString("suburb")
                        ?: "sua região"

                    withContext(Dispatchers.Main) {
                        localidadeIdentificadaReal = cidadeReal
                        if (enderecoOrigem.isEmpty()) {
                            val rua = address?.optString("road")
                            if (rua != null) {
                                enderecoOrigem = "$rua, $cidadeReal"
                            } else {
                                enderecoOrigem = json.optString("display_name", "Minha Localização").split(",").take(3).joinToString(",")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val chamadosDisponiveis = BancoDeDados.corridasEmergentesDisponiveis

    var corridaAceitaPeloMotoristaReal by remember {
        mutableStateOf(corridaAceitaMotoristaGlobalStr?.let { JSONObject(it) })
    }

    var corridaCriadaId by remember { mutableStateOf(corridaCriadaIdGlobal) }
    LaunchedEffect(corridaCriadaIdGlobal) {
        corridaCriadaId = corridaCriadaIdGlobal
    }

    var tempoToleranciaCancelamento by remember { mutableStateOf(tempoCancelamentoGlobal) }
    LaunchedEffect(tempoCancelamentoGlobal) {
        tempoToleranciaCancelamento = tempoCancelamentoGlobal
    }

    var tempoEstimadoTexto by remember { mutableStateOf("Calculando rota...") }
    var statusCorridaPassageiro by remember { mutableStateOf("Procurando") }
    var iconeVeiculoConfirmado by remember { mutableStateOf("🚗") }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = contexto.packageName

        // 🟢 TRAVA DE RECUPERAÇÃO: Dispara a verificação silenciosa na nuvem
        BancoDeDados.recuperarEstadoCorridaEmergenteNuvem { corridaRecuperada ->
            if (corridaRecuperada != null) {
                val statusRecuperado = corridaRecuperada.optString("status")
                val idRecuperado = corridaRecuperada.optInt("id")
                val souOMotoristaDesta = corridaRecuperada.optBoolean("is_motorista_desta_corrida", false)

                if (isMotorista && souOMotoristaDesta) {
                    // Força o radar a ligar e devolve os dados da corrida para a tela do motorista
                    aoAlternarDisponibilidadeMotorista(true)
                    aoAtualizarCorridaAceitaMotoristaGlobal(corridaRecuperada.toString())
                    corridaAceitaPeloMotoristaReal = corridaRecuperada
                } else if (!isMotorista && !souOMotoristaDesta) {
                    // Devolve o status e o mapa travado para a tela do passageiro
                    corridaCriadaId = idRecuperado
                    statusCorridaPassageiro = statusRecuperado
                }
            }
        }
    }

    LaunchedEffect(enderecoDestino) {
        if (enderecoDestino.trim().length >= 3 && expandido) {
            delay(500)
            BancoDeDados.buscarSugestoesDeEndereco(enderecoDestino) { resultado ->
                sugestoes.clear()
                sugestoes.addAll(resultado)
            }
        } else {
            sugestoes.clear()
        }
    }

    LaunchedEffect(isMotorista, motoristaOnlineGlobal, corridaAceitaPeloMotoristaReal) {
        if (isMotorista && motoristaOnlineGlobal) {
            var totalChamadosAnterior = 0
            while (true) {
                if (corridaAceitaPeloMotoristaReal == null) {
                    BancoDeDados.buscarCorridasEmergentesDoServidor { sucesso ->
                        if (sucesso) {
                            val chamadosAtuais = BancoDeDados.corridasEmergentesDisponiveis.size
                            if (chamadosAtuais > totalChamadosAnterior) {
                                dispararNotificacaoComSom("🚨 NOVO CHAMADO DETECTADO!", "Há uma nova solicitação de corrida emergente no seu radar!")
                            }
                            totalChamadosAnterior = chamadosAtuais
                        }
                    }
                }
                delay(4000)
            }
        }
    }

    // 🟢 DINÂMICO E SEGURO: Sincronização de localização ativa do motorista na nuvem a cada 4 segundos
    LaunchedEffect(isMotorista, motoristaOnlineGlobal, corridaAceitaPeloMotoristaReal) {
        if (isMotorista && motoristaOnlineGlobal && corridaAceitaPeloMotoristaReal != null) {
            val idCorrida = corridaAceitaPeloMotoristaReal!!.optInt("id", 0)
            while (corridaAceitaPeloMotoristaReal != null) {
                BancoDeDados.atualizarLocalizacaoMotoristaNuvem(idCorrida, latitudeAtual, longitudeAtual)
                delay(4000)
            }
        }
    }

    LaunchedEffect(corridaCriadaId, statusCorridaPassageiro, tempoToleranciaCancelamento) {
        if (corridaCriadaId != null && statusCorridaPassageiro == "Aceita") {
            if (tempoToleranciaCancelamento > 0) {
                delay(1000)
                val proximoSegundo = tempoToleranciaCancelamento - 1
                tempoToleranciaCancelamento = proximoSegundo
                aoAtualizarTempoCancelamentoGlobal(proximoSegundo)
            }
        }
    }

    LaunchedEffect(mapaRef, latitudeAtual, longitudeAtual) {
        if (mapaRef != null && !primeiraCentralizacaoRealizada) {
            val pontoDispositivo = GeoPoint(latitudeAtual, longitudeAtual)
            mapaRef?.controller?.setCenter(pontoDispositivo)
            primeiraCentralizacaoRealizada = true
        }
    }

    fun tracarRotaNoMapa(
        latOri: Double, lngOri: Double,
        latDes: Double, lngDes: Double,
        corDaLinhaHex: String = "#0000FF",
        tituloOrigem: String = "Origem",
        tituloDestino: String = "Destino",
        forcarMovimentacaoCamera: Boolean = false,
        emojiMarcadorCustom: String? = null // 🟢 Injeção opcional do ícone do veículo dinâmico
    ) {
        escopoCorrotina.launch(Dispatchers.IO) {
            try {
                val url = URL("https://router.project-osrm.org/route/v1/driving/$lngOri,$latOri;$lngDes,$latDes?overview=full&geometries=geojson")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.setRequestProperty("User-Agent", contexto.packageName)
                conexao.connectTimeout = 5000
                conexao.readTimeout = 5000

                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resposta)
                    val rotasArray = json.getJSONArray("routes")

                    if (rotasArray.length() > 0) {
                        val rotaPrincipal = rotasArray.getJSONObject(0)
                        val duracaoSegundos = rotaPrincipal.getDouble("duration")
                        val minutes = (duracaoSegundos / 60).toInt()

                        val geometry = rotaPrincipal.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")

                        val pontosDaRota = mutableListOf<GeoPoint>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            pontosDaRota.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                        }

                        withContext(Dispatchers.Main) {
                            tempoEstimadoTexto = when {
                                minutes <= 1 -> "Chegada Imediata 🟢"
                                minutes <= 5 -> "$minutes min (Vias Livres 🟢)"
                                minutes <= 15 -> "$minutes min (Trânsito Regular 🟡)"
                                else -> "$minutes min (Fluxo Intenso 🔴)"
                            }

                            mapaRef?.let { mapa ->
                                mapa.overlays.removeAll { it is Polyline || it is Marker }

                                val linhaVisual = Polyline(mapa).apply {
                                    setPoints(pontosDaRota)
                                    outlinePaint.color = android.graphics.Color.parseColor(corDaLinhaHex)
                                    outlinePaint.strokeWidth = 10f
                                }
                                mapa.overlays.add(linhaVisual)

                                val marcadorOrigem = Marker(mapa).apply {
                                    position = GeoPoint(latOri, latOri) // Mantém sua lógica de coordenadas
                                    position = GeoPoint(latOri, lngOri)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = tituloOrigem
                                    // 🟢 CORREÇÃO: Se for a posição do motorista (Passageiro olhando), aplica o ícone customizado
                                    if (emojiMarcadorCustom != null && !isMotorista) {
                                        icon = criarMarcadorVeiculoIcon(emojiMarcadorCustom)
                                    }
                                }
                                mapa.overlays.add(marcadorOrigem)

                                val marcadorDestino = Marker(mapa).apply {
                                    position = GeoPoint(latDes, lngDes)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = tituloDestino
                                    // 🟢 CORREÇÃO: Se for a posição do motorista se deslocando (Motorista olhando), aplica o ícone customizado
                                    if (emojiMarcadorCustom != null && isMotorista) {
                                        icon = criarMarcadorVeiculoIcon(emojiMarcadorCustom)
                                    }
                                }
                                mapa.overlays.add(marcadorDestino)

                                if (forcarMovimentacaoCamera) {
                                    mapa.controller.animateTo(GeoPoint(latOri, lngOri))
                                }
                                mapa.invalidate()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var motoristaVinculadoTexto by remember { mutableStateOf("") }

    // 🟢 PASSAGEIRO TOTALMENTE DINÂMICO PELO GPS: Rastreia a aproximação do veículo real vindo do banco
    LaunchedEffect(corridaCriadaId) {
        val idFixo = corridaCriadaId
        if (idFixo != null) {
            while (true) {
                BancoDeDados.buscarStatusCorridaNuvem(idFixo) { dadosCorrida ->
                    if (dadosCorrida != null) {
                        val statusMestre = dadosCorrida.optString("status", "Procurando")

                        if (statusMestre != statusCorridaPassageiro) {
                            when (statusMestre) {
                                "Aceita" -> dispararNotificacaoComSom("✅ Corrida Aceita!", "O motorista já está a caminho do seu local!")
                                "Em Viagem" -> dispararNotificacaoComSom("🚗 Boa Viagem!", "O motorista iniciou o trajeto até o destino final.")
                                "Finalizada" -> dispararNotificacaoComSom("🏁 Corrida Concluída!", "Você chegou ao seu destino com segurança.")
                                "Cancelada" -> dispararNotificacaoComSom("🛑 Corrida Cancelada", "O chamado emergencial foi encerrado.")
                            }
                        }

                        statusCorridaPassageiro = statusMestre

                        if (statusMestre == "Cancelada" || statusMestre == "Expirada") {
                            corridaCriadaId = null
                            aoLimparCorridaGlobal()
                            statusCorridaPassageiro = "Procurando"
                            motoristaVinculadoTexto = ""
                            mapaRef?.overlays?.removeAll { it is Polyline || it is Marker }; mapaRef?.invalidate()
                        } else if (statusMestre == "Aceita") {
                            val nomeMot = dadosCorrida.optString("motorista_nome", "Parceiro")
                            val veiculoMot = dadosCorrida.optString("veiculo", "Carro")
                            val placaMot = dadosCorrida.optString("placa", "---")
                            motoristaVinculadoTexto = "Motorista $nomeMot vindo em um $veiculoMot ($placaMot)"

                            // 🟢 BUSCA AS COORDENADAS REAIS DO MOTORISTA ATUALIZADAS NO SERVIDOR
                            val latMotoristaNuvem = dadosCorrida.optDouble("motorista_latitude", latitudeAtual)
                            val lngMotoristaNuvem = dadosCorrida.optDouble("motorista_longitude", longitudeAtual)

                            val tipoVeiculoDaCorrida = dadosCorrida.optString("veiculo", "Carro")
                            val veiculoTipoBackup = dadosCorrida.optString("veiculo_tipo", "Carro")

                            val emojiIcone = if (tipoVeiculoDaCorrida.contains("moto", ignoreCase = true) || veiculoTipoBackup.contains("moto", ignoreCase = true)) "🏍️" else "🚗"
                            iconeVeiculoConfirmado = emojiIcone // SALVA O EMOJI CORRETO AQUI

                            tracarRotaNoMapa(
                                latOri = latMotoristaNuvem, lngOri = lngMotoristaNuvem,
                                latDes = latitudeAtual, lngDes = longitudeAtual,
                                corDaLinhaHex = "#0000FF",
                                tituloOrigem = "Motorista vindo",
                                tituloDestino = "Você está aqui 🙋‍♂️",
                                forcarMovimentacaoCamera = !deixarCameraLivrePassageiro,
                                emojiMarcadorCustom = emojiIcone // Passa o emoji detectado
                            )
                            deixarCameraLivrePassageiro = true
                        } else if (statusMestre == "Em Viagem") {
                            val latO = dadosCorrida.optDouble("origem_latitude", latitudeAtual)
                            val lngO = dadosCorrida.optDouble("origem_longitude", longitudeAtual)
                            val latD = dadosCorrida.optDouble("destino_latitude", latitudeAtual)
                            val lngD = dadosCorrida.optDouble("destino_longitude", longitudeAtual)

                            tracarRotaNoMapa(
                                latOri = latO, lngOri = lngO,
                                latDes = latD, lngDes = lngD,
                                corDaLinhaHex = "#2ECC71",
                                tituloOrigem = "Local de Embarque 📍",
                                tituloDestino = "Seu Destino Final 🏁",
                                forcarMovimentacaoCamera = false
                            )
                        } else if (statusMestre == "Finalizada") {
                            Toast.makeText(contexto, " Sua corrida foi finalizada com sucesso!", Toast.LENGTH_LONG).show()

                            corridaCriadaId = null
                            aoLimparCorridaGlobal()
                            statusCorridaPassageiro = "Procurando"
                            motoristaVinculadoTexto = ""
                            deixarCameraLivrePassageiro = false
                            tempoToleranciaCancelamento = 180
                            enderecoOrigem = ""
                            enderecoDestino = ""
                            paradasExtras.clear()

                            mapaRef?.overlays?.removeAll { it is Polyline || it is Marker }
                            mapaRef?.invalidate()
                        }
                    }
                }
                delay(3500)
            }
        }
    }

    LaunchedEffect(corridaAceitaPeloMotoristaReal) {
        if (corridaAceitaPeloMotoristaReal != null) {
            val idCorridaMonitorada = corridaAceitaPeloMotoristaReal!!.optInt("id", 0)
            while (corridaAceitaPeloMotoristaReal != null) {
                delay(3000)
                BancoDeDados.buscarStatusCorridaNuvem(idCorridaMonitorada) { dados ->
                    try {
                        if (dados == null || dados.optString("status") == "Cancelada" || dados.optString("status") == "Procurando") {
                            Toast.makeText(contexto, "⚠️ Esta corrida foi cancelada pelo passageiro.", Toast.LENGTH_LONG).show()
                            corridaAceitaPeloMotoristaReal = null
                            aoAtualizarCorridaAceitaMotoristaGlobal(null)
                            mapaRef?.overlays?.removeAll { it is Polyline || it is Marker }; mapaRef?.invalidate()
                        } else {
                            val novoStatus = dados.optString("status", "Aceita")
                            val jsonAtualizado = JSONObject(corridaAceitaPeloMotoristaReal.toString())
                            jsonAtualizado.put("status", novoStatus)

                            corridaAceitaPeloMotoristaReal = jsonAtualizado
                            aoAtualizarCorridaAceitaMotoristaGlobal(jsonAtualizado.toString())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        corridaAceitaPeloMotoristaReal = null
                        aoAtualizarCorridaAceitaMotoristaGlobal(null)
                    }
                }
            }
        }
    }

    // 🟢 MOTORISTA TOTALMENTE DINÂMICO PELO GPS: Renderiza o trajeto e o veículo dele se deslocando
    LaunchedEffect(corridaAceitaPeloMotoristaReal, latitudeAtual, longitudeAtual) {
        if (corridaAceitaPeloMotoristaReal != null) {
            val corridaAtiva = corridaAceitaPeloMotoristaReal!!
            val statusInternal = corridaAtiva.optString("status", "Aceita")
            val latPassageiro = corridaAtiva.optDouble("origem_latitude")
            val lngPassageiro = corridaAtiva.optDouble("origem_longitude")

            val tipoVeiculoDaCorrida = corridaAtiva.optString("veiculo_tipo", "Carro")
            // 🟢 CORRIGIDO: Validação imune a problemas de maiúsculas/minúsculas
            val emojiIcone = if (tipoVeiculoDaCorrida.contains("moto", ignoreCase = true)) "🏍️" else "🚗"

            if (statusInternal != "Em Viagem") {
                tracarRotaNoMapa(
                    latOri = latitudeAtual, lngOri = longitudeAtual,
                    latDes = latPassageiro, lngDes = lngPassageiro,
                    corDaLinhaHex = "#0055FF",
                    tituloOrigem = "Meu Veículo $emojiIcone",
                    tituloDestino = "Buscar Passageiro 🙋‍♂️",
                    forcarMovimentacaoCamera = false,
                    emojiMarcadorCustom = emojiIcone
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (isMotorista) "Radar de Emergências" else "Chamar Corrida", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = aoClicarVoltar) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrincipal)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AndroidView(
                factory = { ctx -> MapView(ctx).apply { setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true); controller.setZoom(16.5); mapaRef = this } },
                modifier = Modifier.fillMaxSize()
            )

            Button(
                onClick = {
                    val centralPonto = GeoPoint(latitudeAtual, longitudeAtual)
                    mapaRef?.controller?.animateTo(centralPonto)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(50.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("🎯", fontSize = 20.sp)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                if (!isMotorista) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (corridaCriadaId == null) {
                            Text(text = "Para onde deseja ir?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AzulPrincipal, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                            Spacer(modifier = Modifier.height(14.dp))

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = enderecoOrigem,
                                            onValueChange = { enderecoOrigem = it },
                                            label = { Text("Digite o endereço de origem 📍") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        paradasExtras.forEachIndexed { indice, enderecoParada ->
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = enderecoParada,
                                                    onValueChange = { valor -> paradasExtras[indice] = valor },
                                                    label = { Text("Parada intermediária ${indice + 1} 📍") },
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )
                                                IconButton(onClick = { paradasExtras.removeAt(indice) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Deletar Parada", tint = Color.Red)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = enderecoDestino,
                                            onValueChange = { enderecoDestino = it; expandido = true },
                                            label = { Text("Digite o endereço de destino 🏁") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    IconButton(
                                        onClick = { paradasExtras.add("") },
                                        modifier = Modifier
                                            .background(AzulPrincipal, CircleShape)
                                            .size(44.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar parada", tint = Color.White)
                                    }
                                }

                                if (expandido && sugestoes.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(top = 114.dp).heightIn(max = 200.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        elevation = CardDefaults.cardElevation(4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        LazyColumn {
                                            items(sugestoes) { endereco ->
                                                Text(text = endereco, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clickable { enderecoDestino = endereco; expandido = false }.padding(12.dp))
                                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(text = "Para você", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)

                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Card(
                                    modifier = Modifier.weight(1f).clickable { tipoVeiculoSelecionado = "Carro" },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (tipoVeiculoSelecionado == "Carro") Color(0xFFE3EDF7) else Color(0xFFF5F5F5))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.Start) {
                                        Text("🚗", fontSize = 24.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Carro", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(1f).clickable { tipoVeiculoSelecionado = "Moto" },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (tipoVeiculoSelecionado == "Moto") Color(0xFFE3EDF7) else Color(0xFFF5F5F5))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.Start) {
                                        Text("🏍️", fontSize = 24.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Moto", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    if (enderecoDestino.trim().isNotEmpty()) {
                                        aoChamarMotorista(enderecoDestino, tipoVeiculoSelecionado) { idGerado ->
                                            corridaCriadaId = idGerado
                                        }
                                    }
                                },
                                enabled = enderecoDestino.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "Solicitar $tipoVeiculoSelecionado Agora ⚡", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    // SUBSTITUÍDO O 🚗 POR $iconeVeiculoConfirmado ABAIXO:
                                    text = if (statusCorridaPassageiro == "Em Viagem") "$iconeVeiculoConfirmado Viagem em Andamento!" else if (statusCorridaPassageiro == "Aceita") "✅ Motorista a Caminho!" else "⚡ Procurando parceiros próximos...",
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (statusCorridaPassageiro == "Procurando") AzulPrincipal else Color(0xFF2E7D32)
                                )

                                IconButton(onClick = {
                                    val linkDoMapaVivo = "https://www.openstreetmap.org/?mlat=$latitudeAtual&mlon=$longitudeAtual#map=17"
                                    val textoCompartilhar = "Acompanhe minha viagem em tempo real pelo Transporte Interiorano ⚡\n\n📍 Veja minha localização em movimento aqui: $linkDoMapaVivo"
                                    val intentCompartilhar = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, textoCompartilhar) }
                                    contexto.startActivity(Intent.createChooser(intentCompartilhar, "Compartilhar Mapa em Tempo Real:"))
                                }) { Icon(Icons.Default.Share, contentDescription = "Compartilhar Viagem", tint = AzulPrincipal) }
                            }

                            if (motoristaVinculadoTexto.isNotEmpty()) {
                                Text(text = motoristaVinculadoTexto, fontSize = 13.sp, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                            }

                            Text(text = "Tempo estimado de chegada: $tempoEstimadoTexto", fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                            if (statusCorridaPassageiro != "Em Viagem") {
                                if (statusCorridaPassageiro == "Aceita") {
                                    val minutosRegressivos = tempoToleranciaCancelamento / 60
                                    val segundosRegressivos = tempoToleranciaCancelamento % 60
                                    val formatoCronometro = String.format("%02d:%02d", minutosRegressivos, segundosRegressivos)

                                    if (tempoToleranciaCancelamento > 0) {
                                        Text(text = "Tempo limite para cancelamento gratuito: $formatoCronometro", fontSize = 12.sp, color = if (tempoToleranciaCancelamento > 30) Color.Gray else Color.Red, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text(text = "⚠️ O cancelamento agora poderá gerar taxas de deslocamento.", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(text = "Aguardando aceitação do chamado no radar...", fontSize = 12.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val idAtual = corridaCriadaId
                                        if (idAtual != null) {
                                            BancoDeDados.cancelarCorridaEmergentePassageiro(idAtual) { sucesso ->
                                                if (sucesso) {
                                                    Toast.makeText(contexto, "Corrida cancelada pelo passageiro.", Toast.LENGTH_SHORT).show()
                                                    corridaCriadaId = null
                                                    aoLimparCorridaGlobal()
                                                    tempoToleranciaCancelamento = 180
                                                    enderecoOrigem = ""
                                                    enderecoDestino = ""
                                                    paradasExtras.clear()
                                                    mapaRef?.overlays?.removeAll { it is Polyline || it is Marker }; mapaRef?.invalidate()
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(size = 8.dp)
                                ) { Text("Cancelar Corrida ❌", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                } else {
                    if (!motoristaOnlineGlobal) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Central de Operações", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AzulPrincipal)
                            Text(text = "Fique online para receber solicitações de corridas emergenciais na sua proximidade em $localidadeIdentificadaReal.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (motoristaOnlineGlobal) "Radar (Ativado)" else "Radar (Desativado)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (motoristaOnlineGlobal) AzulPrincipal else Color.Gray
                                )
                                Switch(
                                    checked = motoristaOnlineGlobal,
                                    onCheckedChange = { aoAlternarDisponibilidadeMotorista(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AzulPrincipal,
                                        uncheckedThumbColor = Color(0xFF757575),
                                        uncheckedTrackColor = Color(0xFFE0E0E0)
                                    )
                                )
                            }
                        }
                    } else {
                        if (corridaAceitaPeloMotoristaReal != null) {
                            val corridaFixa = corridaAceitaPeloMotoristaReal!!
                            val endDestino = corridaFixa.optString("endereco_destino", "Destino Não Informado")
                            val endOrigem = corridaFixa.optString("endereco_origem", "Origem Não Informada")
                            val statusInternoMotorista = corridaFixa.optString("status", "Aceita")

                            if (painelMinimizado) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "🏁 Chamado Ativo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AzulPrincipal)
                                        Text(text = if (statusInternoMotorista == "Em Viagem") "🚗 Viagem em Andamento..." else "🎯 Indo ao encontro", fontSize = 13.sp, color = Color.Gray)
                                    }
                                    Text(text = "🔼 Maximizar", color = AzulPrincipal, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { painelMinimizado = false }.padding(8.dp))
                                }
                            } else {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(14.dp).background(Color(0xFF7CB342), CircleShape))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = "Radar Ativo", fontSize = 16.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(28.dp).background(Color(0xFF1E88E5), RoundedCornerShape(4.dp)).clickable { painelMinimizado = true }, contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                                            Box(modifier = Modifier.size(28.dp).background(Color(0xFF1E88E5), RoundedCornerShape(4.dp)).clickable { painelMinimizado = false }, contentAlignment = Alignment.Center) { Text("⬜", color = Color.White, fontSize = 10.sp) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))

                                        val statusTextoMotorista = if (statusInternoMotorista == "Em Viagem") "VIAGEM EM ANDAMENTO" else "EM BUSCA DO PASSAGEIRO"
                                        Text(text = statusTextoMotorista, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(text = "Dirija-se para: $endOrigem\n\nDestino Final: $endDestino", fontSize = 14.sp, color = Color.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                                    Spacer(modifier = Modifier.height(18.dp))

                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (statusInternoMotorista != "Em Viagem") {
                                            Button(
                                                onClick = {
                                                    val idCorrida = corridaFixa.optInt("id", 0)
                                                    BancoDeDados.atualizarStatusCorridaEmergenteNuvem(idCorrida, "Em Viagem") { sucesso ->
                                                        if (sucesso) {
                                                            corridaFixa.put("status", "Em Viagem")
                                                            val latO = corridaFixa.optDouble("origem_latitude", latitudeAtual)
                                                            val lngO = corridaFixa.optDouble("origem_longitude", longitudeAtual)
                                                            val latD = corridaFixa.optDouble("destino_latitude", latitudeAtual)
                                                            val lngD = corridaFixa.optDouble("destino_longitude", longitudeAtual)

                                                            val tipoVeic = corridaFixa.optString("veiculo_tipo", "Carro")
                                                            val emj = if (tipoVeic.startsWith("Moto")) "🏍️" else "🚗"

                                                            tracarRotaNoMapa(latO, lngO, latD, lngD, "#2ECC71", "Embarque 📍", "Destino Final 🏁", forcarMovimentacaoCamera = true, emojiMarcadorCustom = emj)
                                                            Toast.makeText(contexto, "Viagem iniciada! Siga rumo ao destino.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) { Text("Passageiro a Bordo", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                                        }

                                        Button(
                                            onClick = {
                                                val idCorrida = corridaFixa.optInt("id", 0)
                                                BancoDeDados.atualizarStatusCorridaEmergenteNuvem(idCorrida, "Finalizada") { sucesso ->
                                                    if (sucesso) {
                                                        corridaAceitaPeloMotoristaReal = null
                                                        aoAtualizarCorridaAceitaMotoristaGlobal(null)
                                                        BancoDeDados.corridasEmergentesDisponiveis.clear()
                                                        mapaRef?.overlays?.removeAll { it is Polyline || it is Marker }; mapaRef?.invalidate()
                                                        Toast.makeText(contexto, "Corrida concluída com sucesso!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) { Text("Finalizar Corrida", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                        } else {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val chamadosFiltrados = chamadosDisponiveis.filter { it.optInt("id", 0) !in chamadosRecusadosIds }
                                if (chamadosFiltrados.isEmpty()) {

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (motoristaOnlineGlobal) "Radar (Ativado)" else "Radar (Desativado)",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (motoristaOnlineGlobal) AzulPrincipal else Color.Gray
                                        )
                                        Switch(
                                            checked = motoristaOnlineGlobal,
                                            onCheckedChange = { aoAlternarDisponibilidadeMotorista(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = AzulPrincipal,
                                                uncheckedThumbColor = Color(0xFF757575),
                                                uncheckedTrackColor = Color(0xFFE0E0E0)
                                            )
                                        )
                                    }

                                    Text(text = "Aguardando solicitações de passageiros em $localidadeIdentificadaReal...", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 16.dp))
                                } else {
                                    val primeiroChamado = chamadosFiltrados.first()
                                    val idCorrida = primeiroChamado.optInt("id", 0)
                                    val endDestino = primeiroChamado.optString("endereco_destino", "Destino Não Informado")
                                    val endOrigem = primeiroChamado.optString("endereco_origem", "Origem Não Informada")

                                    Text(text = "🚨 CORRIDA EMERGENTE DETECTADA!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AzulPrincipal)
                                    Text(text = "Saindo de: $endOrigem\nPara: $endDestino", fontSize = 13.sp, color = Color.Black, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = {
                                                BancoDeDados.aceitarCorridaEmergenteNuvem(idCorrida) { sucesso, msg ->
                                                    if (sucesso) {
                                                        Toast.makeText(contexto, "🟢 $msg", Toast.LENGTH_LONG).show()
                                                        corridaAceitaPeloMotoristaReal = primeiroChamado
                                                        aoAtualizarCorridaAceitaMotoristaGlobal(primeiroChamado.toString())

                                                        val latO = primeiroChamado.optDouble("origem_latitude", latitudeAtual)
                                                        val lngO = primeiroChamado.optDouble("origem_longitude", longitudeAtual)

                                                        val tipoV = primeiroChamado.optString("veiculo_tipo", "Carro")
                                                        val emj = if (tipoV.startsWith("Moto")) "🏍️" else "🚗"

                                                        tracarRotaNoMapa(latitudeAtual, longitudeAtual, latO, lngO, "#0000FF", "Meu Veículo $emj", "Passageiro 🙋", forcarMovimentacaoCamera = true, emojiMarcadorCustom = emj)
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) { Text("Aceitar Chamado Agora 🗺️", fontSize = 14.sp, fontWeight = FontWeight.Bold) }

                                        Button(onClick = { chamadosRecusadosIds.add(idCorrida); BancoDeDados.corridasEmergentesDisponiveis.removeIf { it.optInt("id", 0) == idCorrida } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)), modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp)) { Text("Recusar Chamado", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}