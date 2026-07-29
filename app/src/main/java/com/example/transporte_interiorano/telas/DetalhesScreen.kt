package com.example.transporte_interiorano.telas

import com.example.transporte_interiorano.BancoDeDados
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.transporte_interiorano.PagamentoProgramadoService

@Composable
fun DetalhesScreen(
    caronaInfo: Carona?,
    nomePassageiroLogado: String,
    corridasIniciais: Int,
    passageirosIniciais: Int,
    aoConfirmarCarona: () -> Unit,
    aoClicarVoltar: () -> Unit
) {
    val contextoLocal = LocalContext.current

    var corridas by remember { mutableStateOf(corridasIniciais) }
    var passageiros by remember { mutableStateOf(passageirosIniciais) }

    // 🟢 NOVOS ESTADOS PARA A REGRA DE 72H E TAXA DE R$ 5,00
    var mostrarDialogoEscolhaReserva by remember { mutableStateOf(false) }
    var permiteTaxaReserva by remember { mutableStateOf(false) }
    var valorTotalViagem by remember { mutableStateOf(0.0) }
    var carregandoRegra by remember { mutableStateOf(false) }
    // 🟢 ESTADOS PARA EXIBIR O PIX NA TELA
    var mostrarDialogoPix by remember { mutableStateOf(false) }
    var codigoPixCopiaCola by remember { mutableStateOf("") }

    LaunchedEffect(caronaInfo?.motorista_cpf) {
        if (caronaInfo != null && caronaInfo.motorista_cpf.isNotEmpty()) {
            BancoDeDados.buscarMétricasPorCpf(caronaInfo.motorista_cpf) { c, p ->
                corridas = c
                passageiros = p
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = aoClicarVoltar, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = AzulPrincipal)
            }
            Text(
                "Detalhes da Corrida",
                color = AzulPrincipal,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (caronaInfo != null) {
            val pedidosDaCarona =
                BancoDeDados.todosOsPedidos.filter { it.caronaId == caronaInfo.id }

            // 🆕 LÓGICA DE STATUS: Busca o pedido específico deste passageiro
            //val meuPedido = pedidosDaCarona.find {
                //it.passageiro.trim().lowercase() == nomePassageiroLogado.trim().lowercase()
            //}

            val totalVagas = caronaInfo.vagas.toIntOrNull() ?: 0
            val qtdOcupadas = pedidosDaCarona.count {
                val status = it.status.lowercase()
                // Só conta vaga ocupada se for "aceito" ou "pendente" (ainda no prazo)
                status.contains("aceito") || status.contains("pendente")
            }
            //val meuPedido = pedidosDaCarona.find {
                //it.passageiro.trim().lowercase() == nomePassageiroLogado.trim().lowercase() &&
                        //(it.status.lowercase() == "pendente" || it.status.lowercase() == "aceito")
            //}
            val vagasRestantes = totalVagas - qtdOcupadas

            // Campos Origem/Destino/Horário
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AzulPrincipal)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Origem", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "${caronaInfo.cidade_origem} - ${caronaInfo.endereco_origem}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = VerdeBotao)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Destino", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "${caronaInfo.cidade_destino} - ${caronaInfo.endereco_destino}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Horário", fontSize = 12.sp, color = Color.Gray)
                    Text(caronaInfo.horario, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Vagas disponíveis", fontSize = 12.sp, color = Color.Gray)
                    Text("$vagasRestantes vagas", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Motorista", fontSize = 12.sp, color = Color.Gray)
                    Text(caronaInfo.motorista, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    // Aqui os valores são exibidos exatamente como solicitado
                    Text("Corridas realizadas: $corridas", fontSize = 12.sp, color = Color.DarkGray)
                    Text(
                        "Passageiros conduzidos: $passageiros",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seção Valor
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$",
                fontSize = 24.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Valor", fontSize = 12.sp, color = Color.Gray)
                Text("Gratuito", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VerdeBotao)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 1. Defina o pedido do passageiro
        val pedidosDaCarona = BancoDeDados.todosOsPedidos.filter { it.caronaId == caronaInfo?.id }
        val meuPedido = pedidosDaCarona
            .filter { it.passageiro.trim().lowercase() == nomePassageiroLogado.trim().lowercase() }
            .sortedByDescending { it.idReal }
            .firstOrNull { it.status.lowercase() != "finalizado" }

        // 🕵️ RASTREADOR DE HISTÓRICO: Verifica se o último registro dela para essa viagem foi marcado como Expirado
        val foiExpiradoAnteriormente = pedidosDaCarona.any {
            it.passageiro.trim().lowercase() == nomePassageiroLogado.trim().lowercase() &&
                    it.status.equals("Expirado", ignoreCase = true)
        }

        // 2. Lógica para exibir o status (se existir pedido ativo)
        if (meuPedido != null) {
            Text(
                "Status atual: ${meuPedido.status}",
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
        }

        // 3. Botão ÚNICO de Confirmar Vaga com Escolha de Taxa ou Pagamento Total
        val contextoAtual = LocalContext.current
        Button(
            onClick = {
                if (caronaInfo != null) {
                    carregandoRegra = true
                    // 🟢 FAZ A REQUISIÇÃO PARA VER SE ESTÁ A MAIS DE 72H DA VIAGEM
                    kotlin.concurrent.thread {
                        try {
                            val url = java.net.URL("${BancoDeDados.BASE_URL}/caronas/verificar_prazo_reserva/${caronaInfo.id}")
                            val conexao = url.openConnection() as java.net.HttpURLConnection
                            conexao.requestMethod = "GET"
                            conexao.setRequestProperty("Authorization", "Bearer ${BancoDeDados.tokenSessao}")

                            if (conexao.responseCode == 200) {
                                val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                                val json = org.json.JSONObject(resposta)
                                val permite = json.optBoolean("permite_taxa_reserva", false)
                                val valor = json.optDouble("valor_total", 0.0)

                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    carregandoRegra = false
                                    permiteTaxaReserva = permite
                                    valorTotalViagem = valor

                                    if (permite) {
                                        // Se faltam mais de 72h, abre a escolha (Taxa de R$ 5,00 vs Valor Total)
                                        mostrarDialogoEscolhaReserva = true
                                    } else {
                                        // 🟢 CORRIGIDO: Menos de 72h gera o Pix do Valor Total de imediato e exibe para o passageiro
                                        Toast.makeText(contextoAtual, "⚠️ Menos de 72h. Gerando Pix do Valor Total (Prazo de 15 min)...", Toast.LENGTH_LONG).show()

                                        if (caronaInfo != null) {
                                            PagamentoProgramadoService.gerarCheckoutValorTotal(
                                                caronaId = caronaInfo.id,
                                                valorTotal = valorTotalViagem,
                                                tokenSessao = BancoDeDados.tokenSessao
                                            ) { sucesso, mensagem, urlCheckout ->
                                                if (sucesso && !urlCheckout.isNullOrEmpty()) {
                                                    // Se preferir abrir link de checkout ou se for Pix Copia e Cola, ajustamos para o diálogo:
                                                    codigoPixCopiaCola = urlCheckout // ou o payload do Pix retornado pelo backend
                                                    mostrarDialogoPix = true
                                                    BancoDeDados.buscarSolicitacoesDoServidor()
                                                } else {
                                                    Toast.makeText(contextoLocal, mensagem, Toast.LENGTH_LONG).show()
                                                    aoConfirmarCarona()
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    carregandoRegra = false
                                    aoConfirmarCarona() // Fallback seguro
                                }
                            }
                        } catch (e: Exception) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                carregandoRegra = false
                                aoConfirmarCarona() // Fallback seguro
                            }
                        }
                    }
                }
            },
            enabled = !carregandoRegra,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao)
        ) {
            Text(
                text = if (carregandoRegra) "Verificando prazos..." else if (foiExpiradoAnteriormente) "Confirmar Vaga Novamente 🔄" else "Confirmar Vaga",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } // Fecha a Column

    // ==========================================
    // 🟢 DIÁLOGO DE ESCOLHA: TAXA DE RESERVA (R$ 5,00) VS PAGAMENTO INTEGRAL
    // ==========================================
    if (mostrarDialogoEscolhaReserva) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEscolhaReserva = false },
            title = { Text("Garantir Vaga 🚗", fontWeight = FontWeight.Bold, color = AzulPrincipal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Como faltam mais de 3 dias para a viagem, você pode escolher:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("⚡ Opção 1: Pagar Taxa de Reserva de R$ 5,00 agora e garantir sua vaga por 24 horas (o restante será cobrado depois).", fontSize = 12.sp, color = Color.DarkGray)
                    Text("💳 Opção 2: Pagar o Valor Total (R$ ${String.format("%.2f", valorTotalViagem)}) em até 15 minutos para garantir de vez.", fontSize = 12.sp, color = Color.DarkGray)
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. Botão da Taxa de Reserva de R$ 5,00
                    Button(
                        onClick = {
                            mostrarDialogoEscolhaReserva = false
                            if (caronaInfo != null) {
                                Toast.makeText(contextoLocal, "Gerando Pix da Taxa de Reserva (R$ 5,00)...", Toast.LENGTH_SHORT).show()

                                PagamentoProgramadoService.gerarPixTaxaReserva(
                                    caronaId = caronaInfo.id,
                                    tokenSessao = BancoDeDados.tokenSessao
                                ) { sucesso, mensagem, pixCopiaCola, qrCodeBase64, solicitacaoId ->
                                    if (sucesso && !pixCopiaCola.isNullOrEmpty()) {
                                        // 🟢 SALVA O CÓDIGO E ABRE O DIÁLOGO DO PIX PARA O USUÁRIO COPIAR
                                        codigoPixCopiaCola = pixCopiaCola
                                        mostrarDialogoPix = true
                                        BancoDeDados.buscarSolicitacoesDoServidor()
                                    } else {
                                        Toast.makeText(contextoLocal, mensagem, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Pagar Taxa de R$ 5,00 (Garante 24h) ⚡") }

                    // 2. Botão do Valor Total (Checkout / Pix ou Cartão)
                    Button(
                        onClick = {
                            mostrarDialogoEscolhaReserva = false
                            if (caronaInfo != null) {
                                Toast.makeText(contextoLocal, "Gerando link de pagamento total...", Toast.LENGTH_SHORT).show()

                                PagamentoProgramadoService.gerarCheckoutValorTotal(
                                    caronaId = caronaInfo.id,
                                    valorTotal = valorTotalViagem,
                                    tokenSessao = BancoDeDados.tokenSessao
                                ) { sucesso, mensagem, urlCheckout ->
                                    if (sucesso && !urlCheckout.isNullOrEmpty()) {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(urlCheckout))
                                        contextoLocal.startActivity(intent)

                                        BancoDeDados.buscarSolicitacoesDoServidor()
                                        aoConfirmarCarona()
                                    } else {
                                        Toast.makeText(contextoLocal, mensagem, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Pagar Valor Total (15 min) 💳") }
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEscolhaReserva = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // ==========================================
    // 🟢 DIÁLOGO PARA EXIBIR O PIX COPIA E COLA
    // ==========================================
    if (mostrarDialogoPix) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoPix = false
                aoConfirmarCarona()
            },
            title = { Text("Pague o Pix de R$ 5,00 📱", fontWeight = FontWeight.Bold, color = AzulPrincipal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Copie o código abaixo e pague no aplicativo do seu banco para garantir sua vaga por 24 horas:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Caixa de texto com o código Copia e Cola
                    OutlinedTextField(
                        value = codigoPixCopiaCola,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        label = { Text("Pix Copia e Cola") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 🟢 CORREÇÃO: Conversão explícita correta para android.content.ClipboardManager
                        val clipboard = contextoLocal.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Pix Copia e Cola", codigoPixCopiaCola)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(contextoLocal, "Código Pix copiado!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Copiar Código Pix 📋") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoPix = false
                    aoConfirmarCarona()
                }) {
                    Text("Já paguei / Fechar", color = AzulPrincipal, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
} // Fecha a função DetalhesScreen