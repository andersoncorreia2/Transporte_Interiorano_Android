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
import com.example.transporte_interiorano.PagamentoEmergenteService
import com.example.transporte_interiorano.PagamentoProgramadoService
import com.example.transporte_interiorano.ui.theme.AzulPrincipal
import com.example.transporte_interiorano.ui.theme.VerdeBotao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope
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
import org.osmdroid.util.MapTileIndex
//import com.example.transporte_interiorano.BuildConfig
import com.example.transporte_interiorano.dev.BuildConfig // Importe a classe gerada pelo namespace que você definiu no Gradle


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
    aoChamarMotorista: (String, String, String, (Int) -> Unit) -> Unit,
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

    // 🟢 NOVOS ESTADOS DE PAGAMENTO (ESTILO UBER)
    var formaPagamentoSelecionada by remember { mutableStateOf("Dinheiro 💵") }
    var mostrarModalPagamento by remember { mutableStateOf(false) }
    var mostrarAlertaDebito by remember { mutableStateOf<JSONObject?>(null) }
    var mostrarConfirmacaoFaturamentoMotorista by remember { mutableStateOf(false) }

    // 🟢 ESTADO DA TELA DE VALIDAÇÃO DE IDENTIDADE
    var mostrarTelaValidacaoIdentidade by remember { mutableStateOf(false) }

    val sugestoes = remember { mutableStateListOf<String>() }
    var expandido by remember { mutableStateOf(false) }
    var localidadeIdentificadaReal by remember { mutableStateOf("sua região") }
    var painelMinimizado by remember { mutableStateOf(false) }

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var primeiraCentralizacaoRealizada by remember { mutableStateOf(false) }
    var deixarCameraLivrePassageiro by remember { mutableStateOf(false) }

    // 🟢 NOVOS ESTADOS PARA O FLUXO 1 (PAGAMENTO ANTECIPADO)
    var mostrarModalPixAntecipado by remember { mutableStateOf(false) }
    var pixCopiaColaAntecipado by remember { mutableStateOf<String?>(null) }
    var corridaIdPendentePagamento by remember { mutableStateOf<Int?>(null) }
    var carregandoVerificacaoPix by remember { mutableStateOf(false) }

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
                            val addressObj = json.optJSONObject("address")

                            if (addressObj != null) {
                                // 1. Tenta capturar o nome da via ou local abrangendo mais categorias do mapa
                                var localPrincipal = addressObj.optString("road", "").trim()
                                if (localPrincipal.isEmpty()) localPrincipal = addressObj.optString("street", "").trim()
                                if (localPrincipal.isEmpty()) localPrincipal = addressObj.optString("highway", "").trim()
                                if (localPrincipal.isEmpty()) localPrincipal = addressObj.optString("pedestrian", "").trim()
                                if (localPrincipal.isEmpty()) localPrincipal = addressObj.optString("amenity", "").trim()
                                if (localPrincipal.isEmpty()) localPrincipal = addressObj.optString("building", "").trim()

                                val numero = addressObj.optString("house_number", "").trim()

                                // 2. Busca Bairro
                                var bairro = addressObj.optString("suburb", "").trim()
                                if (bairro.isEmpty()) bairro = addressObj.optString("neighbourhood", "").trim()
                                if (bairro.isEmpty()) bairro = addressObj.optString("city_district", "").trim()

                                // 3. Pega Estado, CEP e País
                                val estado = addressObj.optString("state", "").trim()
                                val cep = addressObj.optString("postcode", "").trim()
                                val pais = addressObj.optString("country", "").trim()

                                // 4. Monta o endereço de forma inteligente usando uma Lista
                                val partesEndereco = mutableListOf<String>()

                                if (localPrincipal.isNotEmpty()) {
                                    var logradouro = localPrincipal
                                    if (numero.isNotEmpty()) logradouro += ", $numero"
                                    partesEndereco.add(logradouro)
                                }

                                if (bairro.isNotEmpty()) partesEndereco.add(bairro)
                                if (cidadeReal.isNotBlank() && cidadeReal != bairro) partesEndereco.add(cidadeReal)
                                if (estado.isNotEmpty()) partesEndereco.add(estado)
                                //if (cep.isNotEmpty()) partesEndereco.add(cep)
                                if (pais.isNotEmpty()) partesEndereco.add(pais)

                                // 5. Junta todas as partes encontradas separando-as com vírgula e espaço
                                if (partesEndereco.isNotEmpty()) {
                                    enderecoOrigem = partesEndereco.joinToString(", ")
                                } else {
                                // Se o OpenStreetMap bugar completamente, pega a string crua dele
                                    enderecoOrigem = json.optString("display_name", "Localização Desconhecida")
                                }
                            } else {
                                enderecoOrigem = "Buscando localização..."
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
        // 🟢 CONFIGURAÇÃO DO OSM (Corrigida e dentro do escopo)
        Configuration.getInstance().userAgentValue = "TransporteInteriorano/1.0 (contato: seu-email@exemplo.com)"
        Configuration.getInstance().osmdroidBasePath = contexto.cacheDir

        // 🟢 TRAVA DE RECUPERAÇÃO: Agora está DENTRO do LaunchedEffect
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
    } // <--- Agora o LaunchedEffect fecha aqui, com tudo contido corretamente.

    LaunchedEffect(enderecoDestino) {
        val mapboxToken = BuildConfig.MAPBOX_TOKEN
        //val MEU_TOKEN_MAPBOX = com.example.transporte_interiorano.dev.BuildConfig.MAPBOX_TOKEN
        if (enderecoDestino.trim().length >= 3 && expandido) {
            delay(500)
            buscarEnderecoMapbox(enderecoDestino, mapboxToken) { resultado ->
                sugestoes.clear()
                sugestoes.addAll(resultado)
            }
        } else {
            sugestoes.clear()
        }
    }

    LaunchedEffect(isMotorista, motoristaOnlineGlobal) {
        if (isMotorista && motoristaOnlineGlobal) {
            var totalChamadosAnterior = 0
            while (motoristaOnlineGlobal) { // Loop encerra se o radar for desligado
                if (corridaAceitaPeloMotoristaReal == null) {
                    BancoDeDados.buscarCorridasEmergentesDoServidor { sucesso ->
                        if (sucesso) {
                            val chamadosAtuais = BancoDeDados.corridasEmergentesDisponiveis.size
                            if (chamadosAtuais > totalChamadosAnterior) {
                                dispararNotificacaoComSom("🚨 NOVO CHAMADO DETECTADO!", "Há uma nova solicitação de corrida no seu radar!")
                            }
                            totalChamadosAnterior = chamadosAtuais
                        }
                    }
                }
                delay(15000)
            }
        }
    }

// 🟢 DINÂMICO E SEGURO: Sincronização de localização ativa do motorista na nuvem a cada 4 segundos
    LaunchedEffect(isMotorista, motoristaOnlineGlobal, corridaAceitaPeloMotoristaReal) {
        if (isMotorista && motoristaOnlineGlobal && corridaAceitaPeloMotoristaReal != null) {
            val idCorrida = corridaAceitaPeloMotoristaReal!!.optInt("id", 0)
            while (corridaAceitaPeloMotoristaReal != null) {
                BancoDeDados.atualizarLocalizacaoMotoristaNuvem(idCorrida, latitudeAtual, longitudeAtual)
                delay(10000)
            }
        }
    }

    // 🟢 CRONÔMETRO BLINDADO: Baseado em tempo real do sistema (não para ao sair da tela)
    LaunchedEffect(statusCorridaPassageiro) {
        if (statusCorridaPassageiro == "Aceita") {
            // Só define um novo tempo se ainda não houver um epoch ativo gravado
            if (BancoDeDados.deadlineCancelamentoEpoch == 0L) {
                BancoDeDados.deadlineCancelamentoEpoch = System.currentTimeMillis() + 180_000L
            }

            while (statusCorridaPassageiro == "Aceita") {
                val agora = System.currentTimeMillis()
                val restoMs = BancoDeDados.deadlineCancelamentoEpoch - agora
                val segundosRestantes = (restoMs / 1000).coerceAtLeast(0).toInt()

                tempoToleranciaCancelamento = segundosRestantes
                aoAtualizarTempoCancelamentoGlobal(segundosRestantes)

                if (segundosRestantes <= 0) break
                delay(1000)
            }
        }
        // 🟢 REMOVIDO O 'else' que zerava o tempo indevidamente ao transitar estados iniciais!
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
                                    //position = GeoPoint(latOri, latOri) // Mantém sua lógica de coordenadas
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

    // 🟢 GUARDA O DESENHO DA ROTA: Salva a linha azul para desenhar de graça no mapa
    val pontosRotaPassageiro = remember { mutableStateListOf<GeoPoint>() }

    // 🟢 GUARDA A POSIÇÃO DO CARRO: Move o ícone na tela sem recalcular rota
    var coordenadaMotoristaReal by remember { mutableStateOf<GeoPoint?>(null) }

    // 🟢 CONTROLA A API: Evita que o app calcule a rota mais de uma vez por corrida
    var rotaJaCalculadaPassageiro by remember { mutableStateOf(false) }

    // 🟢 FUNÇÃO CUSTO ZERO TOTALMENTE BLINDADA COM CRONÔMETRO
    fun buscarRotaUmaUnicaVez(latOri: Double, lngOri: Double, latDes: Double, lngDes: Double) {
        if (rotaJaCalculadaPassageiro) return // Se já calculou, sai fora para economizar!

        escopoCorrotina.launch(Dispatchers.IO) {
            try {
                val url = URL("https://router.project-osrm.org/route/v1/driving/$lngOri,$latOri;$lngDes,$latDes?overview=full&geometries=geojson")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.setRequestProperty("User-Agent", contexto.packageName)
                conexao.connectTimeout = 5000

                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resposta)
                    val rotasArray = json.getJSONArray("routes")

                    if (rotasArray.length() > 0) {
                        val rotaPrincipal = rotasArray.getJSONObject(0)

                        // 🟢 EXTRAI O TEMPO DO SERVIDOR
                        val duracaoSegundos = rotaPrincipal.getDouble("duration")
                        val minutes = (duracaoSegundos / 60).toInt()

                        val geometry = rotaPrincipal.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")

                        val listaPontos = mutableListOf<GeoPoint>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            listaPontos.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                        }

                        withContext(Dispatchers.Main) {
                            // 🟢 SUCESSO: Atualiza o texto visual do passageiro!
                            tempoEstimadoTexto = when {
                                minutes <= 1 -> "Chegada Imediata 🟢"
                                minutes <= 5 -> "$minutes min (Vias Livres 🟢)"
                                minutes <= 15 -> "$minutes min (Trânsito Regular 🟡)"
                                else -> "$minutes min (Fluxo Intenso 🔴)"
                            }

                            pontosRotaPassageiro.clear()
                            pontosRotaPassageiro.addAll(listaPontos)
                            rotaJaCalculadaPassageiro = true // 🔒 Porta trancada! Não gasta mais API.
                        }
                    }
                } else {
                    // 🔴 PLANO B: Se o servidor rejeitar a coordenada (Ex: Rua sem saída mapeada)
                    withContext(Dispatchers.Main) {
                        tempoEstimadoTexto = "Motorista a caminho (Rota indisponível 🟡)"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 🔴 PLANO C: Se a internet falhar ou der erro no código
                withContext(Dispatchers.Main) {
                    tempoEstimadoTexto = "Motorista a caminho (Aguardando sinal 🔴)"
                }
            }
        }
    }

// 🟢 PASSAGEIRO TOTALMENTE DINÂMICO PELO GPS: Rastreia a aproximação do veículo real vindo do banco
    LaunchedEffect(corridaCriadaId) {
        val idFixo = corridaCriadaId
        if (idFixo != null) {
            while (corridaCriadaId != null) {
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
                            BancoDeDados.deadlineCancelamentoEpoch = 0L // 👈 Reseta o cronômetro global
                            // 🟢 Limpa o rastreamento otimizado:
                            pontosRotaPassageiro.clear()
                            coordenadaMotoristaReal = null
                            rotaJaCalculadaPassageiro = false
                            mapaRef?.overlays?.removeAll { it is Polyline || it is Marker }; mapaRef?.invalidate()
                        } else if (statusMestre == "Aceita") {
                            val nomeMot = dadosCorrida.optString("motorista_nome", "Parceiro")
                            val veiculoMot = dadosCorrida.optString("veiculo", "Carro")
                            val placaMot = dadosCorrida.optString("placa", "---")
                            motoristaVinculadoTexto = "Motorista $nomeMot vindo em um $veiculoMot ($placaMot)"

                            // 🟢 CAPTURA COORDENADAS DO SERVIDOR
                            val latMotoristaNuvem = dadosCorrida.optDouble("motorista_latitude", latitudeAtual)
                            val lngMotoristaNuvem = dadosCorrida.optDouble("motorista_longitude", longitudeAtual)

                            val tipoVeiculoDaCorrida = dadosCorrida.optString("veiculo", "Carro")
                            val veiculoTipoBackup = dadosCorrida.optString("veiculo_tipo", "Carro")

                            val emojiIcone = if (tipoVeiculoDaCorrida.contains("moto", ignoreCase = true) || veiculoTipoBackup.contains("moto", ignoreCase = true)) "🏍️" else "🚗"
                            iconeVeiculoConfirmado = emojiIcone

                            // 🟢 PASSO 1: CALCULA A ROTA APENAS UMA VEZ NA VIAGEM INTEIRA!
                            buscarRotaUmaUnicaVez(
                                latOri = latMotoristaNuvem, lngOri = lngMotoristaNuvem,
                                latDes = latitudeAtual, lngDes = longitudeAtual
                            )

                            // 🟢 PASSO 2: APENAS ATUALIZA O CARRINHO NA TELA (CUSTO ZERO!)
                            coordenadaMotoristaReal = GeoPoint(latMotoristaNuvem, lngMotoristaNuvem)

                            // Centraliza a câmera no motorista no primeiro momento se necessário
                            if (!deixarCameraLivrePassageiro) {
                                mapaRef?.controller?.animateTo(
                                    GeoPoint(
                                        latMotoristaNuvem,
                                        lngMotoristaNuvem
                                    )
                                )
                                deixarCameraLivrePassageiro = true
                            }
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
                            BancoDeDados.deadlineCancelamentoEpoch = 0L // 👈 Reseta o cronômetro global
                            enderecoOrigem = ""
                            enderecoDestino = ""
                            paradasExtras.clear()

                            // 🟢 Limpa o rastreamento otimizado:
                            pontosRotaPassageiro.clear()
                            coordenadaMotoristaReal = null
                            rotaJaCalculadaPassageiro = false

                            mapaRef?.overlays?.removeAll { it is Polyline || it is Marker }
                            mapaRef?.invalidate()
                        }
                    }
                }
                delay(10000)
            }
        }
    }

    LaunchedEffect(corridaAceitaPeloMotoristaReal) {
        if (corridaAceitaPeloMotoristaReal != null) {
            val idCorridaMonitorada = corridaAceitaPeloMotoristaReal!!.optInt("id", 0)
            while (corridaAceitaPeloMotoristaReal != null) {
                delay(10000)
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
                factory = { ctx ->
                    // 1. Identificação obrigatória para o Mapbox
                    org.osmdroid.config.Configuration.getInstance().userAgentValue = "TransporteInterioranoApp"

                    val token = BuildConfig.MAPBOX_TOKEN

                    // Log para ver o token NO MOMENTO DA CRIAÇÃO
                    android.util.Log.e("DEBUG_MAPA", "Token sendo usado: '$token'")

                    val mapboxTileSource = object : org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase(
                        "Mapbox", 1, 20, 256, ".png",
                        arrayOf("https://api.mapbox.com/styles/v1/mapbox/streets-v11/tiles/")
                    ) {
                        override fun getTileURLString(pMapTileIndex: Long): String {
                            val z = MapTileIndex.getZoom(pMapTileIndex)
                            val x = MapTileIndex.getX(pMapTileIndex)
                            val y = MapTileIndex.getY(pMapTileIndex)

                            // Monta a URL
                            val url = "${baseUrl[0]}$z/$x/$y?access_token=$token"
                            return url
                        }
                    }

                    MapView(ctx).apply {
                        setTileSource(mapboxTileSource)
                        setMultiTouchControls(true)
                        controller.setZoom(16.5)
                        // Força a limpeza de cache para garantir que não haja "lixo" antigo
                        tileProvider.clearTileCache()
                        mapaRef = this
                    }
                },
                update = { mapa ->
                    // Limpa tudo o que foi desenhado anteriormente para evitar fantasmas na tela
                    mapa.overlays.removeAll { it is Polyline || it is Marker }

                    // 1. Desenha a linha azul da rota salva
                    if (pontosRotaPassageiro.isNotEmpty()) {
                        val linhaVisual = Polyline(mapa).apply {
                            setPoints(pontosRotaPassageiro)
                            outlinePaint.color = android.graphics.Color.parseColor("#0000FF")
                            outlinePaint.strokeWidth = 10f
                        }
                        mapa.overlays.add(linhaVisual)
                    }

                    // 2. Desenha o Carrinho do Motorista em movimento
                    coordenadaMotoristaReal?.let { pontoCarro ->
                        val marcadorMotorista = Marker(mapa).apply {
                            position = pontoCarro
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Motorista vindo"
                            icon = criarMarcadorVeiculoIcon(iconeVeiculoConfirmado)
                        }
                        mapa.overlays.add(marcadorMotorista)
                    }

                    // 3. Desenha você no mapa
                    val pontoPassageiro = GeoPoint(latitudeAtual, longitudeAtual)
                    val marcadorPassageiro = Marker(mapa).apply {
                        position = pontoPassageiro
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Você está aqui 🙋‍♂️"
                    }
                    mapa.overlays.add(marcadorPassageiro)

                    mapa.invalidate()
                },
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
                                    verticalAlignment = Alignment.Top // 🟢 Ajustado para Top
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

                                        // 🟢 CORREÇÃO: A lista de sugestões agora nasce EXATAMENTE abaixo do destino
                                        if (expandido && sugestoes.isNotEmpty()) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 200.dp)
                                                    .padding(top = 4.dp), // Apenas um respiro de 4.dp
                                                shape = RoundedCornerShape(8.dp),
                                                elevation = CardDefaults.cardElevation(4.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color.White)
                                            ) {
                                                LazyColumn {
                                                    items(sugestoes) { endereco ->
                                                        Text(
                                                            text = endereco,
                                                            fontSize = 13.sp,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    enderecoDestino = endereco
                                                                    expandido = false
                                                                }
                                                                .padding(12.dp)
                                                        )
                                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                                    }
                                                }
                                            }
                                        }
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

                            Spacer(modifier = Modifier.height(8.dp))

                            // 🟢 NOVO: Botão dinâmico Seletor de Pagamento (Estilo Uber)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { mostrarModalPagamento = true }
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF5F5F5)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = if (formaPagamentoSelecionada.contains("Pix")) "⚡" else "💵", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = formaPagamentoSelecionada, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                    }
                                    Text(text = "Alterar >", fontSize = 12.sp, color = AzulPrincipal, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (enderecoDestino.trim().isNotEmpty()) {
                                        PagamentoEmergenteService.verificarDebitoPassageiro(BancoDeDados.tokenSessao) { bloqueado, msg, detalhes ->
                                            if (bloqueado && detalhes != null) {
                                                mostrarAlertaDebito = detalhes
                                            } else {
                                                // 🟢 VERIFICA A FORMA DE PAGAMENTO SELECIONADA PELO PASSAGEIRO
                                                val ehDinheiro = formaPagamentoSelecionada.contains("Dinheiro", ignoreCase = true)

                                                // 1. Cria a corrida no backend passando a forma de pagamento
                                                BancoDeDados.criarCorridaEmergenteNuvem(
                                                    enderecoOrigem = enderecoOrigem,
                                                    enderecoDestino = enderecoDestino,
                                                    latOrigem = latitudeAtual,
                                                    lngOrigem = longitudeAtual,
                                                    latDestino = 0.0, // Ajuste conforme suas variáveis de destino reais
                                                    lngDestino = 0.0,
                                                    veiculoTipo = tipoVeiculoSelecionado,
                                                    formaPagamento = formaPagamentoSelecionada // 🟢 ENVIA A ESCOLHA
                                                ) { sucesso, mensagemServidor, idGerado ->
                                                    if (sucesso && idGerado != null) {
                                                        if (ehDinheiro) {
                                                            // 💵 DINHEIRO: Vai direto para o radar, sem Pix antecipado!
                                                            corridaCriadaId = idGerado
                                                            Toast.makeText(contexto, "⚡ Procurando motoristas...", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            // ⚡ PIX OU CARTÃO: Exige o pagamento antecipado
                                                            corridaIdPendentePagamento = idGerado

                                                            escopoCorrotina.launch(Dispatchers.IO) {
                                                                try {
                                                                    val jsonBody = JSONObject().apply { put("corrida_id", idGerado) }
                                                                    val url = URL("${BancoDeDados.BASE_URL}/pagamentos/emergente/gerar_pix_corrida")
                                                                    val conexao = url.openConnection() as HttpURLConnection
                                                                    conexao.requestMethod = "POST"
                                                                    conexao.setRequestProperty("Content-Type", "application/json")
                                                                    conexao.setRequestProperty("Authorization", "Bearer ${BancoDeDados.tokenSessao}")
                                                                    conexao.doOutput = true

                                                                    conexao.outputStream.write(jsonBody.toString().toByteArray())

                                                                    if (conexao.responseCode == 200) {
                                                                        val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                                                                        val jsonResp = JSONObject(resposta)
                                                                        val copiaCola = jsonResp.optString("pix_copia_cola")

                                                                        withContext(Dispatchers.Main) {
                                                                            pixCopiaColaAntecipado = copiaCola
                                                                            mostrarModalPixAntecipado = true
                                                                        }
                                                                    }
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
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
                                                    BancoDeDados.deadlineCancelamentoEpoch = 0L // 👈 Reseta o cronômetro global
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
                                                    BancoDeDados.atualizarStatusCorridaEmergenteNuvem(
                                                        corridaId = idCorrida,
                                                        statusAlvo = "Em Viagem",
                                                        pago = true, // Valor padrão temporário para iniciar a viagem
                                                        valorCorrida = 0.0, // Valor padrão temporário para iniciar a viagem
                                                        aoConcluir = { sucesso ->
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
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) { Text("Passageiro a Bordo", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                                        }

                                        Button(
                                            onClick = {
                                                // 🟢 Agora abre a confirmação financeira antes de finalizar
                                                mostrarConfirmacaoFaturamentoMotorista = true
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

// ==========================================
// 🟢 DIÁLOGO 1: SELECIONAR FORMA DE PAGAMENTO (PASSAGEIRO)
// ==========================================
    if (mostrarModalPagamento) {
        AlertDialog(
            onDismissRequest = { mostrarModalPagamento = false },
            title = { Text("Selecione o Método", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { formaPagamentoSelecionada = "Dinheiro 💵"; mostrarModalPagamento = false }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💵", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Dinheiro", fontSize = 16.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { formaPagamentoSelecionada = "Pix Inteligente ⚡"; mostrarModalPagamento = false }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Pix Integrado", fontSize = 16.sp)
                    }
                    // 🟢 ADICIONADO: Cartão de Crédito
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { formaPagamentoSelecionada = "Cartão de Crédito 💳"; mostrarModalPagamento = false }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💳", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Cartão de Crédito", fontSize = 16.sp)
                    }
                    // 🟢 ADICIONADO: Cartão de Débito
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { formaPagamentoSelecionada = "Cartão de Débito 💳"; mostrarModalPagamento = false }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💳", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Cartão de Débito", fontSize = 16.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarModalPagamento = false }) { Text("Fechar") }
            }
        )
    }

// ==========================================
// 🟢 DIÁLOGO 2: ALERTA DE DÉBITO PENDENTE (BLOQUEIO COM PIX REAL DE R$ 0,01)
// ==========================================
    if (mostrarAlertaDebito != null) {
        val detalhes = mostrarAlertaDebito!!
        val valorFita = detalhes.optDouble("valor", 0.0)
        val corridaIdDívida = detalhes.optInt("corrida_id", 0)

        var carregandoPixMapa by remember { mutableStateOf(false) }
        var pixCopiaColaMapa by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { },
            title = { Text("Débito Pendente 🚨", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("O sistema identificou que você não realizou o pagamento de uma corrida anterior (R$ ${String.format("%.2f", valorFita)}).", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gere o Pix de teste (R$ 0,01) para desbloquear suas solicitações.", fontSize = 12.sp, color = Color.Gray)

                    if (pixCopiaColaMapa != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Código Copia e Cola Gerado!", fontWeight = FontWeight.Bold, color = VerdeBotao, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                if (pixCopiaColaMapa == null) {
                    Button(
                        onClick = {
                            carregandoPixMapa = true
                            PagamentoEmergenteService.gerarPixDebitoPassageiro(corridaIdDívida, BancoDeDados.tokenSessao) { sucesso, msg, copiaCola, _, _ ->
                                carregandoPixMapa = false
                                if (sucesso && copiaCola != null) {
                                    pixCopiaColaMapa = copiaCola
                                    val clipboard = contexto.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Pix Copia e Cola", copiaCola)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(contexto, "Código Pix de R$ 0,01 copiado para a área de transferência!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(contexto, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                        enabled = !carregandoPixMapa
                    ) { Text(if (carregandoPixMapa) "Gerando..." else "Gerar Pix (R$ 0,01) ⚡") }
                } else {
                    Button(
                        onClick = {
                            val clipboard = contexto.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Pix Copia e Cola", pixCopiaColaMapa)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(contexto, "Código Pix copiado novamente!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
                    ) { Text("Copiar Pix Novamente 📋") }
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAlertaDebito = null }) { Text("Depois", color = Color.Gray) }
            }
        )
    }

// ==========================================
// 🟢 DIÁLOGO 3: FINALIZAÇÃO E COMPROVAÇÃO FINANCEIRA (MOTORISTA)
// ==========================================
    if (mostrarConfirmacaoFaturamentoMotorista) {
        val corridaFixa = corridaAceitaPeloMotoristaReal
        if (corridaFixa != null) {
            val idCorrida = corridaFixa.optInt("id", 0)
            var valorDigitado by remember { mutableStateOf("15.00") } // Valor sugestivo padrão

            AlertDialog(
                onDismissRequest = { mostrarConfirmacaoFaturamentoMotorista = false },
                title = { Text("Finalizar e Cobrar 💳", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Confirme o valor final cobrado ao passageiro:", fontSize = 14.sp)
                        OutlinedTextField(
                            value = valorDigitado,
                            onValueChange = { valorDigitado = it },
                            label = { Text("Valor em R$") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val vDecimal = valorDigitado.toDoubleOrNull() ?: 0.0
                            mostrarConfirmacaoFaturamentoMotorista = false
                            BancoDeDados.atualizarStatusCorridaEmergenteNuvem(
                                corridaId = idCorrida,
                                statusAlvo = "Finalizada",
                                pago = true,
                                valorCorrida = vDecimal,
                                aoConcluir = { sucesso ->
                                    if (sucesso) {
                                        corridaAceitaPeloMotoristaReal = null
                                        aoAtualizarCorridaAceitaMotoristaGlobal(null)
                                        BancoDeDados.corridasEmergentesDisponiveis.clear()
                                        mapaRef?.overlays?.removeAll { it is Polyline || it is Marker }; mapaRef?.invalidate()
                                        Toast.makeText(contexto, "Corrida concluída! Recebimento confirmado.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao)
                    ) { Text("Confirmar Recebimento ✅") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val vDecimal = valorDigitado.toDoubleOrNull() ?: 0.0
                            mostrarConfirmacaoFaturamentoMotorista = false
                            BancoDeDados.atualizarStatusCorridaEmergenteNuvem(
                                corridaId = idCorrida,
                                statusAlvo = "Finalizada",
                                pago = false,
                                valorCorrida = vDecimal,
                                aoConcluir = { sucesso ->
                                    if (sucesso) {
                                        corridaAceitaPeloMotoristaReal = null
                                        aoAtualizarCorridaAceitaMotoristaGlobal(null)
                                        BancoDeDados.corridasEmergentesDisponiveis.clear()
                                        mapaRef?.overlays?.removeAll { it is Polyline || it is Marker }; mapaRef?.invalidate()
                                        Toast.makeText(contexto, "Corrida encerrada. Calote reportado!", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }
                    ) { Text("Não recebi (Reportar Calote ❌)", color = Color.Red) }
                }
            )
        }
    }

    // ==========================================
    // 🟢 DIÁLOGO 4: PAGAMENTO ANTECIPADO DA CORRIDA (FLUXO 1) - DENTRO DA TELA
    // ==========================================
    if (mostrarModalPixAntecipado) {
        AlertDialog(
            onDismissRequest = {
                // Ação ao fechar clicando fora
                val idCorridaAtual = corridaIdPendentePagamento
                if (idCorridaAtual != null) {
                    escopoCorrotina.launch(Dispatchers.IO) {
                        BancoDeDados.cancelarCorridaEmergentePassageiro(idCorridaAtual) { _ -> }
                    }
                }
                mostrarModalPixAntecipado = false
                corridaIdPendentePagamento = null
                aoLimparCorridaGlobal()
            },
            title = { Text("Pagamento Antecipado ⚡", fontWeight = FontWeight.Bold, color = AzulPrincipal) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Para chamar o motorista, realize o pagamento do Pix de teste (R$ 0,01):", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = pixCopiaColaAntecipado ?: "Gerando código...", fontSize = 11.sp, color = Color.Gray, maxLines = 3)
                }
            },
            confirmButton = {
                // 🟢 TODOS OS BOTÕES ORGANIZADOS EM UMA COLUNA VERTICAL ÚNICA
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val clipboard = contexto.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Pix Copia e Cola", pixCopiaColaAntecipado)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(contexto, "Código Pix copiado para a área de transferência!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Pix Copia e Cola 📋") }

                    Button(
                        onClick = {
                            val idCorridaAtual = corridaIdPendentePagamento
                            if (idCorridaAtual != null) {
                                carregandoVerificacaoPix = true
                                escopoCorrotina.launch(Dispatchers.IO) {
                                    try {
                                        val url = URL("${BancoDeDados.BASE_URL}/pagamentos/emergente/verificar_pagamento_corrida/$idCorridaAtual")
                                        val conexao = url.openConnection() as HttpURLConnection
                                        conexao.setRequestProperty("Authorization", "Bearer ${BancoDeDados.tokenSessao}")

                                        if (conexao.responseCode == 200) {
                                            val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                                            val jsonResp = JSONObject(resposta)
                                            val pago = jsonResp.optBoolean("pago", false)

                                            withContext(Dispatchers.Main) {
                                                carregandoVerificacaoPix = false
                                                if (pago) {
                                                    mostrarModalPixAntecipado = false
                                                    corridaCriadaId = idCorridaAtual
                                                    Toast.makeText(contexto, "✅ Pagamento confirmado! Procurando motoristas...", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(contexto, "⏳ Pagamento ainda não identificado. Pague o Pix e tente novamente.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            carregandoVerificacaoPix = false
                                            Toast.makeText(contexto, "Erro ao verificar pagamento.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                        enabled = !carregandoVerificacaoPix,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (carregandoVerificacaoPix) "Verificando..." else "Já Paguei / Verificar ⚡") }

                    // 🟢 BOTÃO DE DESISTIR ABAIXO DOS OUTROS, CANCELANDO O PEDIDO NA NUVEM E RESETANDO A TELA
                    Button(
                        onClick = {
                            val idCorridaAtual = corridaIdPendentePagamento
                            if (idCorridaAtual != null) {
                                escopoCorrotina.launch(Dispatchers.IO) {
                                    BancoDeDados.cancelarCorridaEmergentePassageiro(idCorridaAtual) { _ -> }
                                }
                            }
                            mostrarModalPixAntecipado = false
                            corridaIdPendentePagamento = null
                            aoLimparCorridaGlobal()
                            Toast.makeText(contexto, "Corrida cancelada.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Desistir / Voltar ❌", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        )
    }
} // <--- CHAVE FINAL QUE ENCERRA A FUNÇÃO @Composable MapaEmergencialScreen

// Função para buscar endereços usando o Mapbox
// FUNÇÃO DE BUSCA DO MAPBOX - ALTA PRECISÃO
fun buscarEnderecoMapbox(texto: String, token: String, onResult: (List<String>) -> Unit) {
    val urlString = "https://api.mapbox.com/geocoding/v5/mapbox.places/${texto.replace(" ", "%20")}.json?access_token=$token&country=br&language=pt&autocomplete=true"

    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val conexao = url.openConnection() as HttpURLConnection
            if (conexao.responseCode == 200) {
                val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(resposta)
                val features = json.getJSONArray("features")
                val sugestoes = mutableListOf<String>()

                for (i in 0 until features.length()) {
                    val placeName = features.getJSONObject(i).getString("place_name")
                    sugestoes.add(placeName)
                }
                withContext(Dispatchers.Main) { onResult(sugestoes) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}