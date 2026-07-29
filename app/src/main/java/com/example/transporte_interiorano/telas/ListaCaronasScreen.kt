package com.example.transporte_interiorano.telas

import com.example.transporte_interiorano.BancoDeDados
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.Carona
import com.example.transporte_interiorano.ui.theme.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaCaronasScreen(
    nomeLogado: String,
    aoClicarEmSolicitar: (Carona) -> Unit,
    aoClicarVoltar: () -> Unit
) {
    // 🟢 ALTERAÇÃO CRÍTICA: Quando o passageiro entra na listagem programada, sincroniza o status...
    LaunchedEffect(Unit) {
        kotlin.concurrent.thread {
            try {
                // ... código do http connection original mantido aqui...
                val url = java.net.URL("${BancoDeDados.BASE_URL}/usuarios/alterar_modalidade")
                val conexao = url.openConnection() as java.net.HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.setRequestProperty("Authorization", "Bearer ${BancoDeDados.tokenSessao}")
                conexao.doOutput = true
                val json = org.json.JSONObject().apply { put("modalidade", "Programada") }
                val escritor = java.io.OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()
                conexao.responseCode
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 🟢 MOTOR DE POLLING (TEMPO REAL): Se o passageiro tem algum pedido rodando, checa a nuvem a cada 4 segundos
    val temPedidoAtivo = BancoDeDados.todosOsPedidos.any {
        it.passageiro.trim().equals(nomeLogado.trim(), ignoreCase = true) &&
                (it.status.lowercase().contains("pendente") || it.status.lowercase().contains("aceito"))
    }
    LaunchedEffect(temPedidoAtivo) {
        if (temPedidoAtivo) {
            while (true) {
                kotlinx.coroutines.delay(4000) // Aguarda 4 segundos
                BancoDeDados.buscarSolicitacoesDoServidor() // Puxa atualização silenciosa
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transporte Interiorano", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = aoClicarVoltar) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrincipal)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F7F7)).padding(paddingValues).padding(16.dp)) {

            Text(
                "Eventos Disponíveis",
                color = Color.DarkGray,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
            )

            // 🟢 CORREÇÃO CIRÚRGICA 1: Filtra para ESCONDER caronas onde o motorista recusou este passageiro
            val caronasOrdenadas = remember(BancoDeDados.caronas, BancoDeDados.todosOsPedidos) {
            BancoDeDados.caronas.filter { carona ->
                val meuPedidoDestaCarona = BancoDeDados.todosOsPedidos
                    .filter { it.caronaId == carona.id && it.passageiro.trim().equals(nomeLogado.trim(), ignoreCase = true) }
                    .maxByOrNull { it.idReal }

                val statusEhRecusado = meuPedidoDestaCarona?.status?.lowercase()?.contains("recusado") ?: false

                // Se foi recusado pelo motorista, remove completamente o cartão da tela!
                !statusEhRecusado
            }.sortedByDescending { carona ->
                val temPedidoAtivo = BancoDeDados.todosOsPedidos.any {
                    it.caronaId == carona.id &&
                            it.passageiro.trim().equals(nomeLogado.trim(), ignoreCase = true) &&
                            !it.status.lowercase().contains("recusado")
                }
                if (temPedidoAtivo) 1 else 0
            }
        }

            if (caronasOrdenadas.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nenhum motorista disponível no momento.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(caronasOrdenadas) { carona ->
                        CartaoCaronaDisponivel(carona, nomeLogado, aoClicarEmSolicitar)
                    }
                }
            }

            // 🟢 REMOVIDO: O bloco inferior que continha os botões "Ver Meu Perfil" e "Ver Histórico" foi limpo!
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CartaoCaronaDisponivel(carona: Carona, nomeLogado: String, aoClicarEmSolicitar: (Carona) -> Unit) {
    val pedidosDaCarona = BancoDeDados.todosOsPedidos.filter { it.caronaId == carona.id }
    val meuPedido = pedidosDaCarona
        .filter { it.passageiro.trim().equals(nomeLogado.trim(), ignoreCase = true) }
        .maxByOrNull { it.idReal }

    val totalVagas = carona.vagas.toIntOrNull() ?: 4

    // Conta quem realmente ocupa vaga (os que não foram recusados ou expirados)
    val qtdOcupadas = pedidosDaCarona.count {
        val st = it.status.lowercase()
        st.contains("aceito") || st.contains("pendente")
    }
    val vagasRestantes = totalVagas - qtdOcupadas

    val status = meuPedido?.status?.trim()?.lowercase() ?: ""

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.LightGray), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(carona.motorista, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("⭐ Novo Parceiro", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🎯 Evento: ${carona.evento_nome}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AzulPrincipal)

                Text("📍 Origem: ${carona.cidade_origem}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (carona.endereco_origem.isNotEmpty()) {
                    Text("   ${carona.endereco_origem}", fontSize = 12.sp, color = Color.Gray)
                }

                Text("🏁 Destino: ${carona.cidade_destino}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (carona.endereco_destino.isNotEmpty()) {
                    Text("   ${carona.endereco_destino}", fontSize = 12.sp, color = Color.Gray)
                }

                Text("⏰ Partida: ${carona.horario}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("👥 Vagas Livres: $vagasRestantes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (vagasRestantes <= 0) VermelhoErro else VerdeBotao)

                // 🟢 ADICIONADO: Exibe o valor total da corrida no cartão do passageiro
                Text("💰 Valor Total: R$ ${carona.valor_corrida}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AzulPrincipal)

                if (meuPedido != null && status.contains("pendente")) {
                    var segundosRestantes by remember { mutableStateOf(0) }

                    LaunchedEffect(key1 = meuPedido.idReal) {
                        try {
                            val formatoData = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                            //formatoData.timeZone = java.util.TimeZone.getTimeZone("UTC")

                            val dataCriacaoBanco = formatoData.parse(meuPedido.dataCriacao)

                            if (dataCriacaoBanco != null) {
                                // Pega a hora do banco e soma 15 minutos (em milissegundos)
                                val limiteParaPagar = dataCriacaoBanco.time + (15 * 60 * 1000)

                                while (true) {
                                    val agora = System.currentTimeMillis()
                                    val diff = ((limiteParaPagar - agora) / 1000).toInt()

                                    if (diff > 0) {
                                        segundosRestantes = diff
                                        kotlinx.coroutines.delay(1000)
                                    } else {
                                        segundosRestantes = 0
                                        break
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            segundosRestantes = 0
                        }
                    }

                    val minutosFormato = segundosRestantes / 60
                    val segundosFormato = segundosRestantes % 60
                    val tempoTexto = String.format("%02d:%02d", minutosFormato, segundosFormato)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (segundosRestantes > 0) "⏱️ Tempo restante para pagar: $tempoTexto" else "⚠️ O tempo de pagamento expirou!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (segundosRestantes > 120) AzulPrincipal else VermelhoErro
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🟢 CORREÇÃO CIRÚRGICA 2: Validação clara e segura do Status do passageiro
            if (meuPedido != null && !status.contains("expirado") && !status.contains("recusado")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status:", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = if (status.contains("aceito")) "Aceito ✅" else "Pendente ⏳",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (status.contains("aceito")) VerdeBotao else AmareloAviso
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (status.contains("pendente")) {
                        OutlinedButton(
                            onClick = {
                                BancoDeDados.cancelarPedidoPassageiro(meuPedido.idReal)
                                BancoDeDados.buscarSolicitacoesDoServidor()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VermelhoErro)
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            } else if (vagasRestantes > 0) {
                val foiExpiradoAnteriormente = BancoDeDados.todosOsPedidos.any {
                    it.caronaId == carona.id && it.passageiroCpf == BancoDeDados.cpfUsuarioLogado && it.status.equals("Expirado", ignoreCase = true)
                }

                Button(
                    onClick = {
                        aoClicarEmSolicitar(carona)
                        // 🟢 FORÇA O RECARREGAMENTO: Avisa o banco para baixar a lista nova e redesenhar a tela
                        BancoDeDados.buscarSolicitacoesDoServidor()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (foiExpiradoAnteriormente) "Solicitar Vaga Novamente 🔄" else "Solicitar Vaga 🚗",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(modifier = Modifier.fillMaxWidth(), color = Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp)) {
                    Text("Esgotado", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}