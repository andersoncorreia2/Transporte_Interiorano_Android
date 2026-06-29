package com.example.transporte_interiorano.telas

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
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
import org.osmdroid.views.overlay.Polyline
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaEmergencialScreen(
    isMotorista: Boolean,
    latitudeAtual: Double,      // 🟢 Adicionado para receber o GPS real do sensor de hardware
    longitudeAtual: Double,     // 🟢 Adicionado para receber o GPS real do sensor de hardware
    aoClicarVoltar: () -> Unit,
    aoChamarMotorista: (String, (Int) -> Unit) -> Unit,
    aoFicarDisponivelMotorista: () -> Unit
) {
    val contexto = LocalContext.current
    val escopoCorrotina = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = contexto.packageName
    }

    // 🗺️ Referência do Mapa para desenhar as linhas de rotas dinamicamente
    var mapaRef by remember { mutableStateOf<MapView?>(null) }

    var enderecoDestino by remember { mutableStateOf("") }

    val sugestoes = remember { mutableStateListOf<String>() }
    var expandido by remember { mutableStateOf(false) }

    var localidadeIdentificadaReal by remember { mutableStateOf("sua região") }

    // 📡 RASTREADOR REGIONAL: Converte as coordenadas do hardware em nome real de cidade/bairro
    LaunchedEffect(latitudeAtual, longitudeAtual) {
        escopoCorrotina.launch(Dispatchers.IO) {
            try {
                val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$latitudeAtual&lon=$longitudeAtual&format=json&addressdetails=1&zoom=10")
                val conexao = url.openConnection() as java.net.HttpURLConnection
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
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var motoristaOnline by remember { mutableStateOf(false) }
    val chamadosDisponiveis = BancoDeDados.corridasEmergentesDisponiveis

    // 🟢 ESTADO ADICIONADO: Mantém a corrida travada na tela do motorista após o aceite
    var corridaAceitaPeloMotoristaReal by remember { mutableStateOf<JSONObject?>(null) }

    // ⏳ Estados do Passageiro (Controle de Cancelamento e Tempos)
    var corridaCriadaId by remember { mutableStateOf<Int?>(null) }
    var tempoToleranciaCancelamento by remember { mutableStateOf(300) } // 5 minutos em segundos
    var tempoEstimadoTexto by remember { mutableStateOf("Calculando rota...") }

    // 🟢 DEBOUNCE: Auto-sugestão de Endereços
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

    // 🟢 POOLING: Radar Ativo do Motorista
    // 🟢 POOLING PROTEGIDO: Só busca novos chamados se não estiver em viagem
    LaunchedEffect(isMotorista, motoristaOnline, corridaAceitaPeloMotoristaReal) {
        if (isMotorista && motoristaOnline) {
            while (true) {
                // 🔒 TRAVA DE HARDWARE: Se já aceitou um passageiro, pausa o radar de buscas
                if (corridaAceitaPeloMotoristaReal == null) {
                    BancoDeDados.buscarCorridasEmergentesDoServidor { _ -> }
                }
                delay(4000)
            }
        }
    }

    // 🟢 CRONÔMETRO: Contagem Regressiva de Cancelamento do Passageiro
    LaunchedEffect(corridaCriadaId) {
        if (corridaCriadaId != null) {
            while (tempoToleranciaCancelamento > 0) {
                delay(1000)
                tempoToleranciaCancelamento--
            }
        }
    }

    // 📡 ENCAIXE O CÓDIGO EXATAMENTE AQUI:
    // 📡 RASTREADOR DE HARDWARE: Faz a câmera do mapa seguir o GPS real do aparelho onde quer que ele esteja
    LaunchedEffect(mapaRef, latitudeAtual, longitudeAtual) {
        if (mapaRef != null) {
            // Se o hardware do GPS já tiver capturado uma posição válida, move a câmera para lá
            val pontoDispositivo = GeoPoint(latitudeAtual, longitudeAtual)
            mapaRef?.controller?.setCenter(pontoDispositivo)
        }
    }

    // 🗺️ FUNÇÃO AUXILIAR PRIMEIRO: Declarada antes para os laços conseguirem enxergar
    fun tracarRotaNoMapa(latOri: Double, lngOri: Double, latDes: Double, lngDes: Double) {
        escopoCorrotina.launch(Dispatchers.IO) {
            try {
                val url = URL("https://router.project-osrm.org/route/v1/driving/$lngOri,$latOri;$lngDes,$latDes?overview=full&geometries=geojson")
                val conexao = url.openConnection() as HttpURLConnection
                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resposta)
                    val rotasArray = json.getJSONArray("routes")
                    if (rotasArray.length() > 0) {
                        val rotaPrincipal = rotasArray.getJSONObject(0)

                        // Captura o tempo base em segundos e converte para minutos aproximados
                        val duracaoSegundos = rotaPrincipal.getDouble("duration")
                        val minutos = (duracaoSegundos / 60).toInt()

                        val geometry = rotaPrincipal.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")

                        val pontosDaRota = mutableListOf<GeoPoint>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            pontosDaRota.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                        }

                        withContext(Dispatchers.Main) {
                            tempoEstimadoTexto = if (minutos <= 2) "1 a 2 min (Vias Livres 🟢)" else "$minutos min (Trânsito Regular 🟡)"

                            mapaRef?.let { mapa ->
                                mapa.overlays.removeAll { it is Polyline }
                                val linhaVisual = Polyline(mapa).apply {
                                    setPoints(pontosDaRota)
                                    outlinePaint.color = android.graphics.Color.BLUE
                                    outlinePaint.strokeWidth = 8f
                                }
                                mapa.overlays.add(linhaVisual)

                                // 🟢 SATÉLITE EM AÇÃO: Move o foco visual do mapa diretamente para a rua de início da corrida real
                                mapa.controller.animateTo(GeoPoint(latOri, lngOri))

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

    // 🟢 NOVO LAÇO DE ESCUTA DO PASSAGEIRO (Agora ele consegue achar a função acima sem erros!)
    var statusCorridaPassageiro by remember { mutableStateOf("Procurando") }
    var motoristaVinculadoTexto by remember { mutableStateOf("") }

    LaunchedEffect(corridaCriadaId) {
        if (corridaCriadaId != null) {
            while (true) {
                BancoDeDados.buscarStatusCorridaNuvem(corridaCriadaId!!) { dadosCorrida ->
                    if (dadosCorrida != null) {
                        val statusMestre = dadosCorrida.optString("status", "Procurando")
                        statusCorridaPassageiro = statusMestre

                        if (statusMestre == "Aceita") {
                            val nomeMot = dadosCorrida.optString("motorista_nome", "Parceiro")
                            val veiculoMot = dadosCorrida.optString("veiculo", "Carro")
                            val placaMot = dadosCorrida.optString("placa", "---")
                            motoristaVinculadoTexto = "Motorista $nomeMot vindo em um $veiculoMot ($placaMot)"

                            // Traça a rota dinamicamente na tela do passageiro usando os dados reais do satélite
                            val latO = dadosCorrida.optDouble("origem_latitude", latitudeAtual)
                            val lngO = dadosCorrida.optDouble("origem_longitude", longitudeAtual)
                            val latD = dadosCorrida.optDouble("destino_latitude", latitudeAtual)
                            val lngD = dadosCorrida.optDouble("destino_longitude", longitudeAtual)
                            tracarRotaNoMapa(latO, lngO, latD, lngD)
                        } else if (statusMestre == "Procurando") {
                            // Caso o motorista demore e a corrida seja reaberta, limpa o vínculo local
                            motoristaVinculadoTexto = ""
                            mapaRef?.overlays?.removeAll { it is Polyline }
                            mapaRef?.invalidate()
                        }
                    }
                }
                delay(3000) // Verifica a cada 3 segundos
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isMotorista) "Radar de Emergências" else "Chamar Corrida",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = aoClicarVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrincipal)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(16.0) // Zoom confortável para nível de rua
                        mapaRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 🎛️ Painel Flutuante Unificado
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isMotorista) {
                        // 🙋‍♂️ INTERFACE DO PASSAGEIRO
                        if (corridaCriadaId == null) {
                            Text(
                                text = "Para onde deseja ir?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AzulPrincipal
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    OutlinedTextField(
                                        value = enderecoDestino,
                                        onValueChange = {
                                            enderecoDestino = it
                                            expandido = true
                                        },
                                        label = { Text("Digite o endereço de destino") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (enderecoDestino.trim().isNotEmpty()) {
                                                // Envia o destino e passa o callback para capturar o ID dinâmico vindo da MainActivity
                                                aoChamarMotorista(enderecoDestino) { idGeradoPeloServidor ->
                                                    corridaCriadaId = idGeradoPeloServidor // 🟢 Grava o ID real, eliminando o 12345 fixo!
                                                }
                                            }
                                        },
                                        enabled = enderecoDestino.trim().isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Solicitar Motorista $localidadeIdentificadaReal ⚡", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (expandido && sugestoes.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 56.dp)
                                            .heightIn(max = 200.dp),
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
                        } else {
                            // 🟢 MUDANÇA CIRÚRGICA: Texto reage ao status real vindo do satélite
                            if (statusCorridaPassageiro == "Procurando") {
                                Text(
                                    text = "⚡ Procurando parceiros próximos...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AzulPrincipal
                                )
                            } else {
                                Text(
                                    text = "✅ Motorista a Caminho!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = motoristaVinculadoTexto,
                                    fontSize = 13.sp,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }
                            Text(
                                text = "Tempo estimado de chegada: $tempoEstimadoTexto",
                                fontSize = 13.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )

                            val minutosRegressivos = tempoToleranciaCancelamento / 60
                            val segundosRegressivos = tempoToleranciaCancelamento % 60
                            val formatoCronometro = String.format("%02d:%02d", minutosRegressivos, segundosRegressivos)

                            Text(
                                text = "Tempo limite para cancelamento gratuito: $formatoCronometro",
                                fontSize = 12.sp,
                                color = if (tempoToleranciaCancelamento > 60) Color.Gray else Color.Red,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    BancoDeDados.cancelarCorridaEmergentePassageiro(corridaCriadaId!!) { sucesso ->
                                        if (sucesso) {
                                            Toast.makeText(contexto, "Corrida cancelada pelo passageiro.", Toast.LENGTH_SHORT).show()
                                        }
                                        corridaCriadaId = null
                                        tempoToleranciaCancelamento = 300
                                        mapaRef?.overlays?.removeAll { it is Polyline }
                                        mapaRef?.invalidate()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancelar Corrida ❌", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // 🚗 INTERFACE DO MOTORISTA
                        if (!motoristaOnline) {
                            Text(
                                text = "Central de Operações",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AzulPrincipal
                            )
                            Text(
                                text = "Fique online para receber solicitações de corridas emergenciais na sua proximidade em \$localidadeIdentificadaReal.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            Button(
                                onClick = {
                                    motoristaOnline = true
                                    aoFicarDisponivelMotorista()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Ficar Disponível (Ficar Online) 🟢", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // 🚗 PAINEL DE RADAR DO MOTORISTA ATIVO
                            Text(
                                text = "🟢 Modo Radar Ativo",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 🟢 SE O MOTORISTA JÁ ACEITOU UMA CORRIDA, TRAVA ELA NA TELA
                            if (corridaAceitaPeloMotoristaReal != null) {
                                val corridaFixa = corridaAceitaPeloMotoristaReal!!
                                val endDestino = corridaFixa.optString("endereco_destino", "Destino Não Informado")
                                val endOrigem = corridaFixa.optString("endereco_origem", "Origem Não Informada")

                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("✅ VIAGEM EM ANDAMENTO", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Text("Dirija-se para: $endOrigem\nDestino Final: $endDestino", fontSize = 13.sp, color = Color.Black)

                                    Button(
                                        onClick = {
                                            // Reseta o estado para voltar ao radar quando chegar
                                            corridaAceitaPeloMotoristaReal = null
                                            BancoDeDados.corridasEmergentesDisponiveis.clear()
                                            mapaRef?.overlays?.removeAll { it is Polyline }
                                            mapaRef?.invalidate()
                                            Toast.makeText(contexto, "Corrida concluída com sucesso!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Cheguei ao Destino (Concluir) 🏁", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            // Se não aceitou nenhuma ainda, exibe o radar normal da nuvem
                            else if (chamadosDisponiveis.isEmpty()) {
                                Text(
                                    text = "Aguardando solicitações de passageiros em \$localidadeIdentificadaReal...",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            } else {
                                val primeiroChamado = chamadosDisponiveis.first()
                                val idCorrida = primeiroChamado.optInt("id", 0)
                                val endDestino = primeiroChamado.optString("endereco_destino", "Destino Não Informado")
                                val endOrigem = primeiroChamado.optString("endereco_origem", "Origem Não Informada")

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "🚨 CORRIDA EMERGENTE DETECTADA!",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AzulPrincipal
                                    )
                                    Text(
                                        text = "Saindo de: $endOrigem\nPara: $endDestino",
                                        fontSize = 13.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                    )

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                BancoDeDados.aceitarCorridaEmergenteNuvem(idCorrida) { sucesso, msg ->
                                                    if (sucesso) {
                                                        Toast.makeText(contexto, "🟢 $msg", Toast.LENGTH_LONG).show()

                                                        // 🟢 PERSISTÊNCIA LOCAL: Salva a corrida para não sumir no próximo pooling de 4 segundos
                                                        corridaAceitaPeloMotoristaReal = primeiroChamado

                                                        val latO = primeiroChamado.optDouble("origem_latitude", latitudeAtual)
                                                        val lngO = primeiroChamado.optDouble("origem_longitude", longitudeAtual)
                                                        val latD = primeiroChamado.optDouble("destino_latitude", latitudeAtual)
                                                        val lngD = primeiroChamado.optDouble("destino_longitude", longitudeAtual)
                                                        tracarRotaNoMapa(latO, lngO, latD, lngD)
                                                    } else {
                                                        Toast.makeText(contexto, "❌ $msg", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Aceitar Chamado Agora 🗺️", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                BancoDeDados.corridasEmergentesDisponiveis.clear()
                                                Toast.makeText(contexto, "Chamado recusado.", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575)),
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Recusar Chamado", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
}