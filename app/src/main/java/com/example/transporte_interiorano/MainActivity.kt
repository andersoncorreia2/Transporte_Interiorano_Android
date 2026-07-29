package com.example.transporte_interiorano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transporte_interiorano.telas.*
import com.example.transporte_interiorano.ui.theme.transporte_interioranoTheme
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.concurrent.thread
import java.net.HttpURLConnection
import java.net.URL
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        android.util.Log.e("DEBUG_TESTE", "O MainActivity iniciou com sucesso!")

        BancoDeDados.ligarRadar()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM_TOKEN", "Token capturado na MainActivity: ${task.result}")
            } else {
                Log.e("FCM_TOKEN", "Falha ao capturar token", task.exception)
            }
        }

        val veioDaNotificacao = intent.getStringExtra("AÇÃO_NOTIFICACAO") == "ABRIR_MAPA"

        setContent {
            transporte_interioranoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 1. Variáveis de controle de bloqueio
                    var usuarioBloqueadoGlobal by remember { mutableStateOf(false) }
                    var detalhesDebito by remember { mutableStateOf<JSONObject?>(null) }
                    var verificandoBloqueio by remember { mutableStateOf(true) }
                    var bloqueadoPorCalote by remember { mutableStateOf(false) }

                    // 2. Variáveis de Navegação e Estado do Usuário
                    var telaAtual by rememberSaveable { mutableStateOf(if (veioDaNotificacao) "mapaEmergencial" else "splash") }
                    var latitudeAtual by rememberSaveable { mutableStateOf(-7.9407) }
                    var longitudeAtual by rememberSaveable { mutableStateOf(-34.8728) }
                    var motoristaOnlineGlobal by rememberSaveable { mutableStateOf(false) }
                    var corridaCriadaIdGlobal by rememberSaveable { mutableStateOf<Int?>(null) }
                    var corridaAceitaMotoristaGlobalStr by rememberSaveable { mutableStateOf<String?>(null) }
                    var tempoCancelamentoGlobal by rememberSaveable { mutableStateOf(180) }

                    var nomeLogado by rememberSaveable { mutableStateOf("") }
                    var cpfLogado by rememberSaveable { mutableStateOf("") }
                    var emailLogado by rememberSaveable { mutableStateOf("") }
                    var telefoneLogado by rememberSaveable { mutableStateOf("") }
                    var veiculoLogado by rememberSaveable { mutableStateOf("") }
                    var placaLogada by rememberSaveable { mutableStateOf("") }
                    var vagasLogada by rememberSaveable { mutableStateOf("") }
                    var ruaLogada by rememberSaveable { mutableStateOf("") }
                    var numeroLogado by rememberSaveable { mutableStateOf("") }
                    var complementoLogado by rememberSaveable { mutableStateOf("") }
                    var bairroLogada by rememberSaveable { mutableStateOf("") }
                    var cidadeLogada by rememberSaveable { mutableStateOf("") }
                    var estadoLogado by rememberSaveable { mutableStateOf("") }
                    var cepLogado by rememberSaveable { mutableStateOf("") }
                    var usuarioLogado by rememberSaveable { mutableStateOf("") }
                    var corridasRealizadas by rememberSaveable { mutableStateOf(0) }
                    var passageirosConduzidos by rememberSaveable { mutableStateOf(0) }

                    LaunchedEffect(BancoDeDados.tokenSessao) {
                        if (BancoDeDados.tokenSessao.isNotEmpty()) {
                            verificandoBloqueio = true
                            try {
                                PagamentoEmergenteService.verificarDebitoPassageiro(BancoDeDados.tokenSessao) { bloqueado, msg, detalhes ->
                                    usuarioBloqueadoGlobal = bloqueado
                                    detalhesDebito = detalhes
                                    verificandoBloqueio = false
                                }
                            } catch (e: Exception) {
                                verificandoBloqueio = false
                            }
                        } else {
                            verificandoBloqueio = false
                        }
                    }

                    // 🟢 CORREÇÃO: Removido o IF/ELSE bloqueador.
                    // O 'Box' abaixo garante que tudo seja renderizado.
                    Box(modifier = Modifier.fillMaxSize()) {

                        val contextoAndroid = this@MainActivity

                        LaunchedEffect(telaAtual) {
                            if (telaAtual == "mapaEmergencial") {
                                if (ContextCompat.checkSelfPermission(contextoAndroid, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    val locationManager = contextoAndroid.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                                    try {
                                        val ultimaLocalizacao = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                            ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                                        ultimaLocalizacao?.let {
                                            latitudeAtual = it.latitude
                                            longitudeAtual = it.longitude
                                        }
                                        locationManager.requestLocationUpdates(
                                            android.location.LocationManager.GPS_PROVIDER, 5000L, 5f,
                                            object : android.location.LocationListener {
                                                override fun onLocationChanged(location: android.location.Location) {
                                                    latitudeAtual = location.latitude
                                                    longitudeAtual = location.longitude
                                                }
                                                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                                                override fun onProviderEnabled(provider: String) {}
                                                override fun onProviderDisabled(provider: String) {}
                                            }
                                        )
                                    } catch (e: SecurityException) { }
                                } else {
                                    ActivityCompat.requestPermissions(contextoAndroid, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200)
                                }
                            }
                        }

                        var erroDeCadastro by rememberSaveable { mutableStateOf("") }
                        var messageLogin by rememberSaveable { mutableStateOf("") }

                        LaunchedEffect(telaAtual) {
                            if (telaAtual == "perfil" && cpfLogado.isNotEmpty()) {
                                BancoDeDados.buscarMétricasPorCpf(cpfLogado) { c, p ->
                                    corridasRealizadas = c
                                    passageirosConduzidos = p
                                }
                            }
                        }

                        var caronaSelecionada by remember { mutableStateOf<Carona?>(null) }

                        // 🟢 O sistema de navegação roda SEMPRE
                        when (telaAtual) {
                            "splash" -> SplashScreen(onTimeout = { telaAtual = if (cpfLogado.isNotEmpty()) "escolhaModalidade" else "login" })

                            "login" -> LoginScreen(
                                aoFazerLogin = { usernameInput, senha ->
                                    messageLogin = "Conectando ao servidor..."
                                    BancoDeDados.fazerLoginNuvem(usernameInput, senha) { usuarioEncontrado, erro ->
                                        if (usuarioEncontrado != null) {
                                            BancoDeDados.cpfUsuarioLogado = usuarioEncontrado.cpf
                                            nomeLogado = usuarioEncontrado.nome
                                            cpfLogado = usuarioEncontrado.cpf
                                            emailLogado = usuarioEncontrado.email
                                            telefoneLogado = usuarioEncontrado.telefone
                                            veiculoLogado = usuarioEncontrado.veiculo
                                            placaLogada = usuarioEncontrado.placa
                                            vagasLogada = usuarioEncontrado.vagas
                                            ruaLogada = usuarioEncontrado.rua
                                            numeroLogado = usuarioEncontrado.numero
                                            complementoLogado = usuarioEncontrado.complemento
                                            bairroLogada = usuarioEncontrado.bairro
                                            cidadeLogada = usuarioEncontrado.cidade
                                            estadoLogado = usuarioEncontrado.estado
                                            cepLogado = usuarioEncontrado.cep
                                            usuarioLogado = usuarioEncontrado.usuario
                                            messageLogin = ""
                                            BancoDeDados.buscarMétricasPorCpf(usuarioEncontrado.cpf) { corridas, pass ->
                                                corridasRealizadas = corridas
                                                passageirosConduzidos = pass
                                            }
                                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                                if (task.isSuccessful) enviarTokenParaServidor(usuarioEncontrado.email, task.result)
                                            }
                                            telaAtual = "escolhaModalidade"
                                        } else { messageLogin = erro }
                                    }
                                },
                                aoClicarCriarConta = { erroDeCadastro = ""; messageLogin = ""; telaAtual = "cadastro" },
                                mensagemErro = messageLogin
                            )

                            "escolhaModalidade" -> EscolhaModalidadeScreen(
                                estaBloqueado = usuarioBloqueadoGlobal,
                                verificandoBloqueio = verificandoBloqueio,
                                onQuitarDebito = {
                                    val idDebito = detalhesDebito?.optInt("corrida_id", 0) ?: 0
                                    if (idDebito > 0) {
                                        PagamentoEmergenteService.quitarDebitoPassageiro(idDebito, BancoDeDados.tokenSessao) { sucesso, msg ->
                                            if (sucesso) {
                                                usuarioBloqueadoGlobal = false
                                                detalhesDebito = null
                                                Toast.makeText(this@MainActivity, "Débito regularizado!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(this@MainActivity, "Erro ao quitar: $msg", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                onModalidadeSelecionada = { modalidade ->
                                    if (modalidade == "Programada") {
                                        telaAtual = if (veiculoLogado.isNotEmpty()) "status" else "listaCaronas"
                                    } else {
                                        kotlin.concurrent.thread {
                                            try {
                                                val url = URL("${BancoDeDados.BASE_URL}/usuarios/alterar_modalidade")
                                                val conexao = url.openConnection() as java.net.HttpURLConnection
                                                conexao.requestMethod = "POST"
                                                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                                                conexao.setRequestProperty("Authorization", "Bearer ${BancoDeDados.tokenSessao}")
                                                conexao.doOutput = true
                                                val json = JSONObject().apply { put("modalidade", "Emergencial") }
                                                val escritor = java.io.OutputStreamWriter(conexao.outputStream)
                                                escritor.write(json.toString())
                                                escritor.flush()
                                                escritor.close()
                                            } catch (e: Exception) { e.printStackTrace() }
                                        }
                                        telaAtual = "mapaEmergencial"
                                    }
                                },
                                onClicarFecharGeral = {
                                    veiculoLogado = ""; nomeLogado = ""; emailLogado = ""; usuarioLogado = ""; cpfLogado = ""
                                    BancoDeDados.tokenSessao = ""; BancoDeDados.cpfUsuarioLogado = ""
                                    telaAtual = "login"
                                },
                                onClicarPerfil = { telaAtual = "perfil" },
                                onClicarHistorico = { telaAtual = "historico" }
                            )

                            "cadastro" -> CadastroScreen(
                                aoConcluirCadastro = { nome, cpf, telefone, email, senha, veiculo, placa, vagas, rua, numero, complemento, bairro, cidade, estado, cep, username ->
                                    if (nome.isBlank() || cpf.isBlank() || telefone.isBlank() || email.isBlank() || senha.isBlank() || rua.isBlank() || numero.isBlank() || bairro.isBlank() || cidade.isBlank() || estado.isBlank() || cep.isBlank() || username.isBlank()) {
                                        erroDeCadastro = "Preencha todos os campos obrigatórios, incluindo o endereço e usuário!"
                                    } else {
                                        erroDeCadastro = "Conectando ao servidor..."
                                        BancoDeDados.cadastrarUsuarioNuvem(
                                            nome,
                                            cpf,
                                            telefone,
                                            email,
                                            senha,
                                            veiculo,
                                            placa,
                                            vagas,
                                            rua,
                                            numero,
                                            complemento,
                                            bairro,
                                            cidade,
                                            estado,
                                            cep,
                                            username
                                        ) { sucesso, mensagem ->
                                            if (sucesso) {
                                                erroDeCadastro = ""
                                                telaAtual = "login"
                                            } else {
                                                erroDeCadastro = mensagem
                                            }
                                        }
                                    }
                                },
                                aoClicarFechar = {
                                    erroDeCadastro = ""
                                    telaAtual = "login"
                                },
                                mensagemErro = erroDeCadastro
                            )

                            "criarEvento" -> CriarEventoScreen(
                                aoPublicarEvento = { nome, cidOri, endOri, cidDes, endDes, hor, vag, valorTotal, nomeMotorista, cpfMotorista ->
                                    BancoDeDados.enviarCaronaParaServidor(
                                        nomeEvento = nome,
                                        cidadeOrigem = cidOri,
                                        enderecoOrigem = endOri,
                                        cidadeDestino = cidDes,
                                        enderecoDestino = endDes,
                                        horario = hor,
                                        vagas = vag,
                                        valorCorrida = valorTotal, // 🟢 Passando o valor da corrida
                                        motorista = nomeMotorista,
                                        motoristaCpf = cpfMotorista
                                    )
                                    BancoDeDados.temEventoAtivo = true
                                    telaAtual = "status"
                                },
                                aoClicarSair = { telaAtual = "status" },
                                cpfLogado = cpfLogado,
                                nomeLogado = nomeLogado // 🟢 Passando o parâmetro exigido pela tela
                            )

                            "listaCaronas" -> ListaCaronasScreen(
                                nomeLogado = nomeLogado,
                                aoClicarEmSolicitar = { carona ->
                                    caronaSelecionada = carona
                                    telaAtual = "detalhes"
                                },
                                aoClicarVoltar = {
                                    telaAtual = "escolhaModalidade"
                                }
                            )

                            "historico" -> HistoricoScreen(
                                cpfUsuario = cpfLogado,
                                isMotorista = veiculoLogado.isNotEmpty(),
                                aoClicarVoltar = {
                                    telaAtual = "escolhaModalidade"
                                }
                            )

                            "detalhes" -> {
                                val escopoCorrotina = rememberCoroutineScope()
                                DetalhesScreen(
                                    caronaInfo = caronaSelecionada,
                                    nomePassageiroLogado = nomeLogado,
                                    corridasIniciais = corridasRealizadas,
                                    passageirosIniciais = passageirosConduzidos,
                                    aoConfirmarCarona = {
                                        if (caronaSelecionada != null) {
                                            BancoDeDados.todosOsPedidos.add(
                                                Pedido(
                                                    idReal = 9999,
                                                    caronaId = caronaSelecionada!!.id,
                                                    passageiro = nomeLogado,
                                                    passageiroCpf = cpfLogado,
                                                    status = "Pendente"
                                                )
                                            )

                                            BancoDeDados.fazerSolicitacao(
                                                carona = caronaSelecionada!!,
                                                nomePassageiro = nomeLogado,
                                                cpfPassageiro = cpfLogado
                                            )

                                            escopoCorrotina.launch {
                                                try {
                                                    delay(1000)
                                                    BancoDeDados.buscarCaronasDoServidor()
                                                    BancoDeDados.buscarSolicitacoesDoServidor()
                                                } catch (e: Exception) {}
                                            }
                                        }
                                        telaAtual = "listaCaronas"
                                    },
                                    aoClicarVoltar = { telaAtual = "listaCaronas" }
                                )
                            }

                            "status" -> MinhasSolicitacoesScreen(
                                isMotorista = veiculoLogado.isNotEmpty(),
                                nomeMotoristaLogado = nomeLogado,
                                aoClicarPerfil = { telaAtual = "perfil" },
                                aoClicarVoltar = { telaAtual = "escolhaModalidade" },
                                aoClicarNovoEvento = { telaAtual = "criarEvento" },
                                aoClicarHistorico = { telaAtual = "historico" },
                                aoClicarEditarViagem = { caronaClicada ->
                                    caronaSelecionada = caronaClicada
                                    telaAtual = "editarEvento"
                                }
                            )

                            "editarEvento" -> {
                                EditarEventoScreen(
                                    caronaInfo = caronaSelecionada,
                                    aoSalvarAlteracao = { ev, cidO, endO, cidD, endD, hor, vag, valorTotal ->
                                        android.util.Log.d("EDITAR_EVENTO", "MainActivity recebeu os dados. caronaSelecionada é nula? ${caronaSelecionada == null}")

                                        caronaSelecionada?.let { carona ->
                                            android.util.Log.d("EDITAR_EVENTO", "Disparando atualizarCaronaNoServidor para ID: ${carona.id}")

                                            BancoDeDados.atualizarCaronaNoServidor(
                                                id = carona.id,
                                                nomeEvento = ev,
                                                cidadeOrigem = cidO,
                                                enderecoOrigem = endO,
                                                cidadeDestino = cidD,
                                                enderecoDestino = endD,
                                                horario = hor,
                                                vagas = vag,
                                                valorCorrida = valorTotal,
                                                aoConcluir = { sucesso ->
                                                    android.util.Log.d("EDITAR_EVENTO", "Resposta do servidor recebida. Sucesso = $sucesso")

                                                    if (sucesso) {
                                                        BancoDeDados.buscarCaronasDoServidor()
                                                        telaAtual = "status"
                                                    } else {
                                                        android.util.Log.e("EDITAR_EVENTO", "O servidor rejeitou a atualização (Código diferente de 200)")
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    aoClicarVoltar = {
                                        telaAtual = "status"
                                    }
                                )
                            }

                            "perfil" -> {
                                val formatador = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                                val dataFormatada = formatador.format(Date())

                                PerfilScreen(
                                    nome = nomeLogado,
                                    email = emailLogado,
                                    dataCadastro = dataFormatada,
                                    veiculo = veiculoLogado,
                                    placa = placaLogada,
                                    corridas = corridasRealizadas,
                                    passageiros = passageirosConduzidos,
                                    aoClicarSair = {
                                        veiculoLogado = ""
                                        nomeLogado = ""
                                        emailLogado = ""
                                        usuarioLogado = ""
                                        cpfLogado = ""
                                        telaAtual = "login"
                                    },
                                    aoClicarVoltar = {
                                        // 🟢 CIRÚRGICO: Seta agora retorna direto para a tela "Como deseja viajar hoje?"
                                        telaAtual = "escolhaModalidade"
                                    },
                                    aoClicarExcluirConta = {
                                        BancoDeDados.excluirUsuario(emailLogado)
                                        veiculoLogado = ""
                                        nomeLogado = ""
                                        emailLogado = ""
                                        usuarioLogado = ""
                                        cpfLogado = ""
                                        telaAtual = "login"
                                    },
                                    aoClicarEditar = { telaAtual = "editarPerfil" }
                                )
                            }

                            "editarPerfil" -> {
                                val usuarioAtual = Usuario(
                                    nome = nomeLogado, cpf = cpfLogado, email = emailLogado, telefone = telefoneLogado,
                                    veiculo = veiculoLogado, placa = placaLogada, vagas = vagasLogada,
                                    rua = ruaLogada, numero = numeroLogado, complemento = complementoLogado,
                                    bairro = bairroLogada, cidade = cidadeLogada, estado = estadoLogado, cep = cepLogado,
                                    usuario = usuarioLogado
                                )
                                EditarPerfilScreen(
                                    usuarioAtual = usuarioAtual,
                                    aoSalvar = { usuarioAtualizado ->
                                        // 🟢 CIRÚRGICO: Traz todo o bloco de mutação de estado do Compose para rodar de forma segura na Main Thread
                                        this@MainActivity.runOnUiThread {
                                            nomeLogado = usuarioAtualizado.nome
                                            emailLogado = usuarioAtualizado.email
                                            telefoneLogado = usuarioAtualizado.telefone
                                            veiculoLogado = usuarioAtualizado.veiculo
                                            placaLogada = usuarioAtualizado.placa
                                            vagasLogada = usuarioAtualizado.vagas
                                            ruaLogada = usuarioAtualizado.rua
                                            numeroLogado = usuarioAtualizado.numero
                                            complementoLogado = usuarioAtualizado.complemento
                                            bairroLogada = usuarioAtualizado.bairro
                                            cidadeLogada = usuarioAtualizado.cidade
                                            estadoLogado = usuarioAtualizado.estado
                                            cepLogado = usuarioAtualizado.cep
                                            usuarioLogado = usuarioAtualizado.usuario

                                            Toast.makeText(this@MainActivity, "Alteração Realizada com Sucesso!", Toast.LENGTH_SHORT).show()
                                            telaAtual = "perfil"
                                        }
                                    },
                                    aoCancelar = {
                                        telaAtual = "perfil"
                                    }
                                )
                            }



                            "mapaEmergencial" -> MapaEmergencialScreen(
                                isMotorista = veiculoLogado.isNotEmpty(),
                                latitudeAtual = latitudeAtual,
                                longitudeAtual = longitudeAtual,
                                motoristaOnlineGlobal = motoristaOnlineGlobal,
                                corridaCriadaIdGlobal = corridaCriadaIdGlobal,
                                corridaAceitaMotoristaGlobalStr = corridaAceitaMotoristaGlobalStr, // 🟢 PASSA A STRING DA CORRIDA DO MOTORISTA
                                tempoCancelamentoGlobal = tempoCancelamentoGlobal, // 🟢 ADICIONADO: Envia o tempo atual para a tela
                                aoClicarVoltar = { telaAtual = "escolhaModalidade" },
                                aoChamarMotorista = { origemDigitada, destinoDigitado, tipoVeiculoSelecionado, aoConfirmarIdNaTela ->
                                    kotlin.concurrent.thread {
                                        try {
                                            // 1. ORIGEM: Usa exatamente o texto que o passageiro digitou/editou na tela!
                                            val enderecoPartidaReal = origemDigitada

                                            // 2. DESTINO: Usa o token do Mapbox para achar a coordenada (mesmo sistema da sugestão)
                                            val queryCodificada = java.net.URLEncoder.encode(destinoDigitado, "UTF-8")
                                            val tokenMapbox = com.example.transporte_interiorano.dev.BuildConfig.MAPBOX_TOKEN
                                            // 🟢 O Mapbox agora vai priorizar endereços que estejam perto do GPS atual do celular!
                                            val urlDestino = URL("https://api.mapbox.com/geocoding/v5/mapbox.places/$queryCodificada.json?access_token=$tokenMapbox&proximity=$longitudeAtual,$latitudeAtual&country=br&limit=1")

                                            val conexaoDestino = urlDestino.openConnection() as java.net.HttpURLConnection
                                            conexaoDestino.setRequestProperty("User-Agent", contextoAndroid.packageName)

                                            if (conexaoDestino.responseCode == 200) {
                                                val resposta = conexaoDestino.inputStream.bufferedReader().use { it.readText() }
                                                val json = JSONObject(resposta)
                                                val features = json.optJSONArray("features")

                                                if (features != null && features.length() > 0) {
                                                    val local = features.getJSONObject(0)
                                                    val center = local.getJSONArray("center")
                                                    // O Mapbox inverte: retorna [longitude, latitude]
                                                    val lngDestinoReal = center.getDouble(0)
                                                    val latDestinoReal = center.getDouble(1)

                                                    BancoDeDados.criarCorridaEmergenteNuvem(
                                                        enderecoOrigem = enderecoPartidaReal, // 🟢 Vai com o seu número manual para o motorista!
                                                        enderecoDestino = destinoDigitado,
                                                        latOrigem = latitudeAtual,
                                                        lngOrigem = longitudeAtual,
                                                        latDestino = latDestinoReal,
                                                        lngDestino = lngDestinoReal,
                                                        veiculoTipo = tipoVeiculoSelecionado,
                                                        formaPagamento = "Dinheiro",
                                                        aoConcluir = { sucesso, mensagemServidor, idCorridaReal ->
                                                            contextoAndroid.runOnUiThread {
                                                                if (sucesso) {
                                                                    Toast.makeText(contextoAndroid, "⚡ $mensagemServidor", Toast.LENGTH_LONG).show()
                                                                    corridaCriadaIdGlobal = idCorridaReal
                                                                    idCorridaReal?.let { aoConfirmarIdNaTela(it) }
                                                                } else {
                                                                    Toast.makeText(contextoAndroid, "❌ $mensagemServidor", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
                                                    )
                                                } else {
                                                    contextoAndroid.runOnUiThread {
                                                        Toast.makeText(contextoAndroid, "❌ Endereço de destino não localizado no mapa.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                },
                                aoLimparCorridaGlobal = {
                                    corridaCriadaIdGlobal = null
                                    tempoCancelamentoGlobal = 180 // 🟢 ADICIONADO: Reseta o cronômetro persistente quando a corrida fecha
                                },
                                // 🟢 ADICIONADO: Sincroniza cada segundo regredido com o estado da MainActivity
                                aoAtualizarTempoCancelamentoGlobal = { tempoRestante ->
                                    tempoCancelamentoGlobal = tempoRestante
                                },
                                aoAtualizarCorridaAceitaMotoristaGlobal = { jsonStr ->
                                    corridaAceitaMotoristaGlobalStr = jsonStr // 🟢 CONTROLA O SALVAMENTO REATIVO DA CORRIDA DO MOTORISTA
                                },
                                aoAlternarDisponibilidadeMotorista = { ficarOnline ->
                                    motoristaOnlineGlobal = ficarOnline
                                    val modalidadeAlvo = if (ficarOnline) "Emergencial" else "Programada"

                                    kotlin.concurrent.thread {
                                        try {
                                            val url = URL("${BancoDeDados.BASE_URL}/usuarios/alterar_modalidade")
                                            val conexao = url.openConnection() as java.net.HttpURLConnection
                                            conexao.requestMethod = "POST"
                                            conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                                            conexao.setRequestProperty("Authorization", "Bearer ${BancoDeDados.tokenSessao}")
                                            conexao.doOutput = true

                                            val json = JSONObject().apply { put("modalidade", modalidadeAlvo) }
                                            val escritor = java.io.OutputStreamWriter(conexao.outputStream)
                                            escritor.write(json.toString())
                                            escritor.flush()
                                            escritor.close()

                                            val codigoHttp = conexao.responseCode
                                            contextoAndroid.runOnUiThread {
                                                if (codigoHttp == 200) {
                                                    val msg = if (ficarOnline) "Radar ativado! Modo Emergencial ativo 🟢" else "Radar desativado! Modo Programado ativo 🔴"
                                                    Toast.makeText(contextoAndroid, msg, Toast.LENGTH_SHORT).show()
                                                } else {
                                                    motoristaOnlineGlobal = !ficarOnline
                                                    Toast.makeText(contextoAndroid, "Erro de sincronização com o servidor.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            )
                        }
                    } // 🟢 CORREÇÃO: FECHAMENTO DO BLOCO ELSE
                }
            }
        }
    }

    fun enviarTokenParaServidor(email: String, token: String) {
        thread {
            try {
                val url = URL("${BancoDeDados.BASE_URL}/registrar_token")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true

                val json = """{"email": "$email", "token": "$token"}"""
                val escritor = java.io.OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()

                Log.d("FCM_TOKEN", "Resposta do servidor: ${conexao.responseCode}")
            } catch (e: Exception) {
                Log.e("FCM_TOKEN", "Erro: ${e.message}")
            }
        }
    }
}