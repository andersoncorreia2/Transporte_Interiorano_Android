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

    var mostrarDialogoEscolhaReserva by remember { mutableStateOf(false) }
    var permiteTaxaReserva by remember { mutableStateOf(false) }
    var valorTotalViagem by remember { mutableStateOf(0.0) }
    var carregandoRegra by remember { mutableStateOf(false) }

    var mostrarDialogoPix by remember { mutableStateOf(false) }
    var codigoPixCopiaCola by remember { mutableStateOf("") }

    // 🟢 CORREÇÃO CRÍTICA: Variáveis "lembradas" globalmente usando o 'rememberSaveable' para não sumirem da memória na re-renderização
    var solicitacaoIdParaVerificar by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(-1) }
    var tipoPagamentoPix by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var carregandoVerificacaoPix by remember { mutableStateOf(false) }

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
            val pedidosDaCarona = BancoDeDados.todosOsPedidos.filter { it.caronaId == caronaInfo.id }

            val totalVagas = caronaInfo.vagas.toIntOrNull() ?: 0
            val qtdOcupadas = pedidosDaCarona.count {
                val status = it.status.lowercase()
                status.contains("aceito") || status.contains("pendente")
            }
            val vagasRestantes = totalVagas - qtdOcupadas

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AzulPrincipal)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Origem", fontSize = 12.sp, color = Color.Gray)
                    Text("${caronaInfo.cidade_origem} - ${caronaInfo.endereco_origem}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = VerdeBotao)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Destino", fontSize = 12.sp, color = Color.Gray)
                    Text("${caronaInfo.cidade_destino} - ${caronaInfo.endereco_destino}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    Text("Corridas realizadas: $corridas", fontSize = 12.sp, color = Color.DarkGray)
                    Text("Passageiros conduzidos: $passageiros", fontSize = 12.sp, color = Color.DarkGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🟢 IDENTIFICA SE A CORRIDA É GRATUITA OU PAGA
        val valorDB = caronaInfo?.valor_corrida ?: "0.00"
        val isGratuito = valorDB == "0.00" || valorDB == "0" || valorDB == "0.0" || valorDB.isBlank()

        val textoValor = if (isGratuito) "Gratuito" else "R$ $valorDB"
        val corDoValor = if (isGratuito) VerdeBotao else AzulPrincipal

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$", fontSize = 24.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Valor", fontSize = 12.sp, color = Color.Gray)
                Text(textoValor, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = corDoValor)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val pedidosDaCarona = BancoDeDados.todosOsPedidos.filter { it.caronaId == caronaInfo?.id }
        val meuPedido = pedidosDaCarona
            .filter { it.passageiro.trim().lowercase() == nomePassageiroLogado.trim().lowercase() }
            .sortedByDescending { it.idReal }
            .firstOrNull { it.status.lowercase() != "finalizado" }

        val foiExpiradoAnteriormente = pedidosDaCarona.any {
            it.passageiro.trim().lowercase() == nomePassageiroLogado.trim().lowercase() &&
                    it.status.equals("Expirado", ignoreCase = true)
        }

        if (meuPedido != null) {
            Text("Status atual: ${meuPedido.status}", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold, color = Color.Gray)
        }

        val contextoAtual = LocalContext.current

        // 🟢 ADICIONADO: Lógica para calcular o Saldo Restante
        val ehTaxaPaga = meuPedido?.status?.lowercase()?.contains("taxa paga") == true
        val valorDaCaronaDb = caronaInfo?.valor_corrida?.toDoubleOrNull() ?: 0.0
        val saldoAPagar = if (ehTaxaPaga) (valorDaCaronaDb - 5.0) else valorDaCaronaDb

        Button(
            onClick = {
                if (caronaInfo != null) {
                    if (ehTaxaPaga && meuPedido != null) {
                        // 🟢 GERA O PIX DO SALDO DIRETAMENTE
                        Toast.makeText(contextoAtual, "Gerando Pix do Saldo Restante...", Toast.LENGTH_SHORT).show()
                        carregandoRegra = true
                        PagamentoProgramadoService.gerarPixSaldoRestante(
                            caronaId = caronaInfo.id,
                            solicitacaoId = meuPedido.idReal,
                            valorSaldo = saldoAPagar,
                            tokenSessao = BancoDeDados.tokenSessao
                        ) { sucesso, mensagem, codigoPix ->
                            carregandoRegra = false
                            if (sucesso && !codigoPix.isNullOrEmpty()) {
                                codigoPixCopiaCola = codigoPix
                                solicitacaoIdParaVerificar = meuPedido.idReal
                                tipoPagamentoPix = "SALDO"
                                mostrarDialogoPix = true
                            } else {
                                Toast.makeText(contextoLocal, mensagem, Toast.LENGTH_LONG).show()
                            }
                        }
                    } else if (isGratuito) {
                        BancoDeDados.buscarSolicitacoesDoServidor()
                        aoConfirmarCarona()
                    } else {
                        carregandoRegra = true
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
                                            mostrarDialogoEscolhaReserva = true
                                        } else {
                                            Toast.makeText(contextoAtual, "⚠️ Menos de 72h. Gerando Pix...", Toast.LENGTH_LONG).show()
                                            PagamentoProgramadoService.gerarCheckoutValorTotal(
                                                caronaId = caronaInfo.id, valorTotal = valorTotalViagem, tokenSessao = BancoDeDados.tokenSessao
                                            ) { sucesso, mensagem, codigoPix, solicitacaoId -> // 🟢 Recebe o ID
                                                if (sucesso && !codigoPix.isNullOrEmpty()) {
                                                    codigoPixCopiaCola = codigoPix
                                                    solicitacaoIdParaVerificar = solicitacaoId // 🟢 Salva o ID
                                                    tipoPagamentoPix = "INTEGRAL" // 🟢 Salva o tipo
                                                    mostrarDialogoPix = true
                                                    BancoDeDados.buscarSolicitacoesDoServidor()
                                                } else {
                                                    Toast.makeText(contextoLocal, mensagem, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        carregandoRegra = false
                                        Toast.makeText(contextoAtual, "Erro ao verificar. Tente novamente.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    carregandoRegra = false
                                    Toast.makeText(contextoAtual, "Falha de rede.", Toast.LENGTH_SHORT).show()
                                }
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
                text = if (carregandoRegra) "Verificando prazos..."
                else if (ehTaxaPaga) "Pagar Saldo (R$ ${String.format("%.2f", saldoAPagar)}) 💳"
                else if (foiExpiradoAnteriormente) "Confirmar Vaga Novamente 🔄"
                else "Confirmar Vaga",
                fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
        }
    }

    if (mostrarDialogoEscolhaReserva) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEscolhaReserva = false },
            title = { Text("Garantir Vaga 🚗", fontWeight = FontWeight.Bold, color = AzulPrincipal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Como faltam mais de 3 dias para a viagem, você pode escolher:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("⚡ Opção 1: Pagar Taxa de Reserva de R$ 5,00 agora e garantir sua vaga por 24 horas (o restante deverá ser pago até antes do término das 24 horas).", fontSize = 12.sp, color = Color.DarkGray)
                    Text("💳 Opção 2: Pagar o Valor Total (R$ ${String.format("%.2f", valorTotalViagem)}) em até 15 minutos para garantir de vez.", fontSize = 12.sp, color = Color.DarkGray)
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            mostrarDialogoEscolhaReserva = false
                            if (caronaInfo != null) {
                                Toast.makeText(contextoLocal, "Gerando Pix da Taxa de Reserva (R$ 5,00)...", Toast.LENGTH_SHORT).show()
                                PagamentoProgramadoService.gerarPixTaxaReserva(
                                    caronaId = caronaInfo.id, tokenSessao = BancoDeDados.tokenSessao
                                ) { sucesso, mensagem, pixCopiaCola, _, solicitacaoId -> // 🟢 Pega o último parâmetro
                                    if (sucesso && !pixCopiaCola.isNullOrEmpty()) {
                                        codigoPixCopiaCola = pixCopiaCola
                                        solicitacaoIdParaVerificar = solicitacaoId // 🟢 Salva o ID
                                        tipoPagamentoPix = "TAXA" // 🟢 Salva o tipo
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

                    Button(
                        onClick = {
                            mostrarDialogoEscolhaReserva = false
                            if (caronaInfo != null) {
                                Toast.makeText(contextoLocal, "Gerando Pix do valor total...", Toast.LENGTH_SHORT).show()
                                PagamentoProgramadoService.gerarCheckoutValorTotal(
                                    caronaId = caronaInfo.id, valorTotal = valorTotalViagem, tokenSessao = BancoDeDados.tokenSessao
                                ) { sucesso, mensagem, codigoPix, solicitacaoId -> // 🟢 Recebe o ID
                                    if (sucesso && !codigoPix.isNullOrEmpty()) {
                                        codigoPixCopiaCola = codigoPix
                                        solicitacaoIdParaVerificar = solicitacaoId // 🟢 Salva o ID
                                        tipoPagamentoPix = "INTEGRAL" // 🟢 Salva o tipo
                                        mostrarDialogoPix = true
                                        BancoDeDados.buscarSolicitacoesDoServidor()
                                    } else {
                                        Toast.makeText(contextoLocal, mensagem, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Pagar Valor Total (15 min) 💳") }

                    // 🟢 MOVIDO PARA CÁ: O botão Cancelar agora faz parte da mesma coluna e fica alinhado no centro!
                    TextButton(
                        onClick = { mostrarDialogoEscolhaReserva = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {} // 🟢 DEIXADO VAZIO: Isso impede que o Android tente espremer botões do lado de fora da coluna
        )
    }

    if (mostrarDialogoPix) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoPix = false
                aoClicarVoltar() // 🟢 CORREÇÃO CRÍTICA: Volta pra tela principal e NÃO duplica o pedido!
            },
            title = { Text("Pague o Pix 📱", fontWeight = FontWeight.Bold, color = AzulPrincipal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Copie o código abaixo e pague no aplicativo do seu banco para garantir sua vaga:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = codigoPixCopiaCola, onValueChange = {}, readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(120.dp), label = { Text("Pix Copia e Cola") }
                    )
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val clipboard = contextoLocal.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Pix Copia e Cola", codigoPixCopiaCola)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(contextoLocal, "Código Pix copiado!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Copiar Código Pix 📋") }

                    // 🟢 O BOTÃO QUE FALTAVA: Aciona o servidor para verificar a chave PIX e avisar o motorista!
                    Button(
                        onClick = {
                            val solId = solicitacaoIdParaVerificar
                            if (solId != -1) {
                                carregandoVerificacaoPix = true

                                if (tipoPagamentoPix == "TAXA") {
                                    PagamentoProgramadoService.verificarPagamentoTaxa(solId, BancoDeDados.tokenSessao) { pago ->
                                        carregandoVerificacaoPix = false
                                        if (pago) {
                                            Toast.makeText(contextoLocal, "✅ Pagamento confirmado! Vaga garantida por 24h.", Toast.LENGTH_LONG).show()
                                            mostrarDialogoPix = false
                                            aoClicarVoltar()
                                        } else {
                                            Toast.makeText(contextoLocal, "⏳ Pagamento ainda não identificado.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else if (tipoPagamentoPix == "SALDO") {
                                    // 🟢 LÊ A ROTA DO SALDO
                                    PagamentoProgramadoService.verificarPagamentoSaldo(solId, BancoDeDados.tokenSessao) { pago ->
                                        carregandoVerificacaoPix = false
                                        if (pago) {
                                            Toast.makeText(contextoLocal, "✅ Saldo quitado! Vaga confirmada 100%.", Toast.LENGTH_LONG).show()
                                            mostrarDialogoPix = false
                                            aoClicarVoltar()
                                        } else {
                                            Toast.makeText(contextoLocal, "⏳ Pagamento ainda não identificado.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    PagamentoProgramadoService.verificarPagamentoIntegral(solId, BancoDeDados.tokenSessao) { pago ->
                                        carregandoVerificacaoPix = false
                                        if (pago) {
                                            Toast.makeText(contextoLocal, "✅ Pagamento Integral confirmado! Vaga garantida 100%.", Toast.LENGTH_LONG).show()
                                            mostrarDialogoPix = false
                                            aoClicarVoltar()
                                        } else {
                                            Toast.makeText(contextoLocal, "⏳ Pagamento ainda não identificado. Tente novamente em instantes.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                        enabled = !carregandoVerificacaoPix,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (carregandoVerificacaoPix) "Verificando..." else "Já Paguei / Verificar ⚡") }

                    Button(
                        onClick = {
                            mostrarDialogoPix = false
                            aoClicarVoltar()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Fechar / Pagar Depois ❌", color = Color.White) }
                }
            },
            dismissButton = {} // Vazio, pois agora usamos botões grandes em bloco
        )
    }
}