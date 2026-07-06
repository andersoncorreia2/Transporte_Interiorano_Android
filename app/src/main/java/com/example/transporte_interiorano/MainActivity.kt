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

        // 🟢 INCLUSÃO: Captura o extra enviado pela notificação push do Firebase
        val veioDaNotificacao = intent.getStringExtra("AÇÃO_NOTIFICACAO") == "ABRIR_MAPA"

        setContent {
            transporte_interioranoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    // 🟢 ALTERAÇÃO: Se veio do clique do balão, joga direto no mapa, senão vai para o fluxo padrão (splash)
                    var telaAtual by rememberSaveable { mutableStateOf(if (veioDaNotificacao) "mapaEmergencial" else "splash") }

                    var latitudeAtual by rememberSaveable { mutableStateOf(-7.9407) }
                    var longitudeAtual by rememberSaveable { mutableStateOf(-34.8728) }
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
                                        android.location.LocationManager.GPS_PROVIDER,
                                        5000L,
                                        5f,
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
                                } catch (e: SecurityException) {
                                    Log.e("GPS_ERRO", "Erro de permissão ao ler sensores: ${e.message}")
                                }
                            } else {
                                ActivityCompat.requestPermissions(contextoAndroid, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200)
                            }
                        }
                    }

                    var erroDeCadastro by rememberSaveable { mutableStateOf("") }
                    var mensagemLogin by rememberSaveable { mutableStateOf("") }

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

                    LaunchedEffect(telaAtual) {
                        if (telaAtual == "perfil" && cpfLogado.isNotEmpty()) {
                            BancoDeDados.buscarMétricasPorCpf(cpfLogado) { c, p ->
                                corridasRealizadas = c
                                passageirosConduzidos = p
                            }
                        }
                    }

                    var caronaSelecionada by remember { mutableStateOf<Carona?>(null) }

                    when (telaAtual) {
                        "splash" -> SplashScreen(
                            onTimeout = {
                                telaAtual = if (cpfLogado.isNotEmpty()) {
                                    "escolhaModalidade"
                                } else {
                                    "login"
                                }
                            }
                        )

                        "login" -> LoginScreen(
                            aoFazerLogin = { usernameInput, senha ->
                                mensagemLogin = "Conectando ao servidor..."
                                BancoDeDados.fazerLoginNuvem(
                                    usernameInput,
                                    senha
                                ) { usuarioEncontrado, erro ->
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
                                        mensagemLogin = ""

                                        BancoDeDados.buscarMétricasPorCpf(usuarioEncontrado.cpf) { corridas, pass ->
                                            corridasRealizadas = corridas
                                            passageirosConduzidos = pass
                                        }

                                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                            if (task.isSuccessful) enviarTokenParaServidor(
                                                usuarioEncontrado.email,
                                                task.result
                                            )
                                        }

                                        telaAtual = "escolhaModalidade"
                                    } else {
                                        mensagemLogin = erro
                                    }
                                }
                            },
                            aoClicarCriarConta = {
                                erroDeCadastro = ""
                                mensagemLogin = ""
                                telaAtual = "cadastro"
                            },
                            mensagemErro = mensagemLogin
                        )

                        "cadastro" -> CadastroScreen(
                            aoConcluirCadastro = { nome, cpf, telefone, email, senha, veiculo, placa, vagas, rua, numero, complemento, bairro, cidade, estado, cep, username ->
                                if (nome.isBlank() || cpf.isBlank() || telefone.isBlank() || email.isBlank() || senha.isBlank() || rua.isBlank() || numero.isBlank() || bairro.isBlank() || cidade.isBlank() || estado.isBlank() || cep.isBlank() || username.isBlank()) {
                                    erroDeCadastro =
                                        "Preencha todos os campos obrigatórios, incluindo o endereço e usuário!"
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
                            aoPublicarEvento = { nome, cidOri, endOri, cidDes, endDes, hor, vag, cpfMotorista ->
                                BancoDeDados.enviarCaronaParaServidor(
                                    nome,
                                    cidOri,
                                    endOri,
                                    cidDes,
                                    endDes,
                                    hor,
                                    vag,
                                    nomeLogado,
                                    cpfMotorista
                                )
                                BancoDeDados.temEventoAtivo = true
                                telaAtual = "status"
                            },
                            aoClicarSair = { telaAtual = "status" },
                            cpfLogado = cpfLogado
                        )

                        "listaCaronas" -> ListaCaronasScreen(
                            nomeLogado = nomeLogado,
                            aoClicarEmSolicitar = { carona ->
                                caronaSelecionada = carona
                                telaAtual = "detalhes"
                            },
                            aoClicarVoltar = {
                                telaAtual = "escolhaModalidade"
                            },
                            aoClicarPerfil = { telaAtual = "perfil" },
                            aoClicarHistorico = { telaAtual = "historico" }
                        )

                        "historico" -> HistoricoScreen(
                            cpfUsuario = cpfLogado,
                            isMotorista = veiculoLogado.isNotEmpty(),
                            aoClicarVoltar = {
                                telaAtual =
                                    if (veiculoLogado.isNotEmpty()) "status" else "listaCaronas"
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
                                aoSalvarAlteracao = { ev, cidO, endO, cidD, endD, hor, vag ->
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
                                    telaAtual = if (veiculoLogado.isNotEmpty()) "status" else "listaCaronas"
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

                                    if (usuarioAtualizado.usuario.isNotEmpty()) {
                                        usuarioLogado = usuarioAtualizado.usuario
                                    }

                                    this@MainActivity.runOnUiThread {
                                        Toast.makeText(this@MainActivity, "Alteração Realizada com Sucesso!", Toast.LENGTH_SHORT).show()
                                        telaAtual = "perfil"
                                    }
                                },
                                aoCancelar = {
                                    telaAtual = "perfil"
                                }
                            )
                        }

                        "escolhaModalidade" -> EscolhaModalidadeScreen(
                            onModalidadeSelecionada = { modalidade ->
                                if (modalidade == "Programada") {
                                    telaAtual = if (veiculoLogado.isNotEmpty()) "status" else "listaCaronas"
                                } else {
                                    telaAtual = "mapaEmergencial"
                                }
                            },
                            onClicarFecharGeral = {
                                veiculoLogado = ""
                                nomeLogado = ""
                                emailLogado = ""
                                usuarioLogado = ""
                                cpfLogado = ""
                                BancoDeDados.tokenSessao = ""
                                BancoDeDados.cpfUsuarioLogado = ""
                                telaAtual = "login"
                            }
                        )

                        "mapaEmergencial" -> MapaEmergencialScreen(
                            isMotorista = veiculoLogado.isNotEmpty(),
                            latitudeAtual = latitudeAtual,
                            longitudeAtual = longitudeAtual,
                            aoClicarVoltar = { telaAtual = "escolhaModalidade" },
                            // 🟢 ALTERAÇÃO: Lambda adaptada para receber 3 parâmetros (destino, tipo de veículo, callback de ID)
                            aoChamarMotorista = { destinoDigitado, tipoVeiculo, aoConfirmarIdNaTela ->
                                kotlin.concurrent.thread {
                                    try {
                                        val urlOrigem = URL("https://nominatim.openstreetmap.org/reverse?lat=$latitudeAtual&lon=$longitudeAtual&format=json")
                                        val conexaoOrigem = urlOrigem.openConnection() as java.net.HttpURLConnection
                                        conexaoOrigem.setRequestProperty("User-Agent", contextoAndroid.packageName)

                                        var enderecoPartidaReal = "$ruaLogada, $numeroLogado, $bairroLogada, $cidadeLogada"
                                        if (conexaoOrigem.responseCode == 200) {
                                            val resp = conexaoOrigem.inputStream.bufferedReader().use { it.readText() }
                                            enderecoPartidaReal = JSONObject(resp).optString("display_name", enderecoPartidaReal)
                                        }

                                        val queryCodificada = java.net.URLEncoder.encode(destinoDigitado, "UTF-8")
                                        val urlDestino = URL("https://nominatim.openstreetmap.org/search?q=$queryCodificada&format=json&limit=1")
                                        val conexaoDestino = urlDestino.openConnection() as java.net.HttpURLConnection
                                        conexaoDestino.setRequestProperty("User-Agent", contextoAndroid.packageName)

                                        if (conexaoDestino.responseCode == 200) {
                                            val resposta = conexaoDestino.inputStream.bufferedReader().use { it.readText() }
                                            val jsonArray = org.json.JSONArray(resposta)

                                            if (jsonArray.length() > 0) {
                                                val local = jsonArray.getJSONObject(0)
                                                val latDestinoReal = local.getDouble("lat")
                                                val lngDestinoReal = local.getDouble("lon")

                                                // Envia o tipoVeiculo coletado diretamente da tela
                                                BancoDeDados.criarCorridaEmergenteNuvem(
                                                    enderecoOrigem = enderecoPartidaReal,
                                                    enderecoDestino = destinoDigitado,
                                                    latOrigem = latitudeAtual,
                                                    lngOrigem = longitudeAtual,
                                                    latDestino = latDestinoReal,
                                                    lngDestino = lngDestinoReal,
                                                    veiculoTipo = tipoVeiculo, // 🟢 Passa o tipo aqui!
                                                    aoConcluir = { sucesso, mensagemServidor, idCorridaReal ->
                                                        contextoAndroid.runOnUiThread {
                                                            if (sucesso) {
                                                                Toast.makeText(contextoAndroid, "⚡ $mensagemServidor", Toast.LENGTH_LONG).show()
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
                            aoFicarDisponivelMotorista = {
                                BancoDeDados.ficarOnlineRadarMotorista { sucesso, mensagemServidor ->
                                    if (sucesso) {
                                        Toast.makeText(contextoAndroid, "🟢 $mensagemServidor", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(contextoAndroid, "❌ $mensagemServidor", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    fun enviarTokenParaServidor(email: String, token: String) {
        thread {
            try {
                //val url = URL("https://transporte-interiorano-backend.onrender.com/registrar_token")
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