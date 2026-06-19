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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permissão de notificação para Android 13 ou superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // 🔍 TESTE DE PROVA REAL
        android.util.Log.e("DEBUG_TESTE", "O MainActivity iniciou com sucesso!")

        // Inicializa seu radar existente
        BancoDeDados.ligarRadar()

        // Adicione aqui a captura do token:
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM_TOKEN", "Token capturado na MainActivity: ${task.result}")
            } else {
                Log.e("FCM_TOKEN", "Falha ao capturar token", task.exception)
            }
        }

        setContent {
            transporte_interioranoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    var telaAtual by remember { mutableStateOf("splash") }
                    var erroDeCadastro by remember { mutableStateOf("") }
                    var mensagemLogin by remember { mutableStateOf("") }

                    // --- VARIÁVEIS DE ESTADO DO USUÁRIO ---
                    var nomeLogado by remember { mutableStateOf("") }
                    var cpfLogado by remember { mutableStateOf("") }
                    var emailLogado by remember { mutableStateOf("") }
                    var telefoneLogado by remember { mutableStateOf("") }
                    var veiculoLogado by remember { mutableStateOf("") }
                    var placaLogada by remember { mutableStateOf("") }
                    var vagasLogada by remember { mutableStateOf("") }
                    var ruaLogada by remember { mutableStateOf("") }
                    var numeroLogado by remember { mutableStateOf("") }
                    var complementoLogado by remember { mutableStateOf("") }
                    var bairroLogada by remember { mutableStateOf("") }
                    var cidadeLogada by remember { mutableStateOf("") }
                    var estadoLogado by remember { mutableStateOf("") }
                    var cepLogado by remember { mutableStateOf("") }
                    var usuarioLogado by remember { mutableStateOf("") }

                    var corridasRealizadas by remember { mutableStateOf(0) }
                    var passageirosConduzidos by remember { mutableStateOf(0) }
                    // ------------------------------------------

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
                            onTimeout = { telaAtual = "login" }
                        )

                        "login" -> LoginScreen(
                            aoFazerLogin = { usernameInput, senha ->
                                mensagemLogin = "Conectando ao servidor..."
                                BancoDeDados.fazerLoginNuvem(usernameInput, senha) { usuarioEncontrado, erro ->
                                    if (usuarioEncontrado != null) {
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
                                            if (task.isSuccessful) enviarTokenParaServidor(usuarioEncontrado.email, task.result)
                                        }

                                        if (usuarioEncontrado.veiculo.isNotEmpty()) {
                                            telaAtual = "status"
                                        } else {
                                            BancoDeDados.buscarCaronasDoServidor()
                                            telaAtual = "listaCaronas"
                                        }
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
                            mensagemErro = mensagemLogin // 🟢 CORRIGIDO AQUI!
                        )

                        "cadastro" -> CadastroScreen(
                            aoConcluirCadastro = { nome, cpf, telefone, email, senha, veiculo, placa, vagas, rua, numero, complemento, bairro, cidade, estado, cep, username ->
                                if (nome.isBlank() || cpf.isBlank() || telefone.isBlank() || email.isBlank() || senha.isBlank() || rua.isBlank() || numero.isBlank() || bairro.isBlank() || cidade.isBlank() || estado.isBlank() || cep.isBlank() || username.isBlank()) {
                                    erroDeCadastro = "Preencha todos os campos obrigatórios, incluindo o endereço e usuário!"
                                } else {
                                    erroDeCadastro = "Conectando ao servidor..."
                                    BancoDeDados.cadastrarUsuarioNuvem(
                                        nome, cpf, telefone, email, senha, veiculo, placa, vagas, rua, numero, complemento, bairro, cidade, estado, cep, username
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
                            mensagemErro = erroDeCadastro // 🟢 CORRIGIDO AQUI TAMBÉM!
                        )

                        "criarEvento" -> CriarEventoScreen(
                            aoPublicarEvento = { nome, cidOri, endOri, cidDes, endDes, hor, vag, cpfMotorista ->
                                BancoDeDados.enviarCaronaParaServidor(nome, cidOri, endOri, cidDes, endDes, hor, vag, nomeLogado, cpfMotorista)
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
                                veiculoLogado = ""
                                nomeLogado = ""
                                emailLogado = ""
                                telaAtual = "login"
                            },
                            aoClicarPerfil = { telaAtual = "perfil" },
                            aoClicarHistorico = { telaAtual = "historico" }
                        )

                        "historico" -> HistoricoScreen(
                            cpfUsuario = cpfLogado,
                            isMotorista = veiculoLogado.isNotEmpty(),
                            aoClicarVoltar = {
                                telaAtual = if (veiculoLogado.isNotEmpty()) "status" else "listaCaronas"
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
                                        // 1. Decrementa o contador visual na hora
                                        //val idAlvo = caronaSelecionada!!.id
                                        //val index = BancoDeDados.caronas.indexOfFirst { it.id == idAlvo }
                                        //if (index != -1) {
                                            //val caronaAtual = BancoDeDados.caronas[index]
                                            //val vagasNum = caronaAtual.vagas.toIntOrNull() ?: 0
                                            //if (vagasNum > 0) {
                                                //BancoDeDados.caronas[index] = caronaAtual.copy(
                                                    //vagas = (vagasNum - 1).toString()
                                                //)
                                            //}
                                        //}

                                        // 2. Insere o pedido pendente local para ativar o botão Cancelar na ListaCaronasScreen
                                        //BancoDeDados.todosOsPedidos.add(
                                            //Pedido(
                                                //idReal = 999, // provisório até o radar atualizar
                                                //caronaId = idAlvo,
                                                //passageiro = nomeLogado,
                                                //passageiroCpf = cpfLogado,
                                                //status = "Pendente",
                                                //evento_nome = caronaSelecionada!!.evento_nome,
                                                //cidade_origem = caronaSelecionada!!.cidade_origem,
                                                //cidade_destino = caronaSelecionada!!.cidade_destino,
                                                //horario = caronaSelecionada!!.horario
                                            //)
                                        //)

                                        // 🟢 APENAS ADICIONA O PEDIDO PENDENTE LOCAL:
                                        // A ListaCaronasScreen fará a matemática automática (4 - 1 = 3)
                                        BancoDeDados.todosOsPedidos.add(
                                            Pedido(
                                                idReal = 9999,
                                                caronaId = caronaSelecionada!!.id,
                                                passageiro = nomeLogado,
                                                passageiroCpf = cpfLogado,
                                                status = "Pendente"
                                            )
                                        )

                                        // 3. Dispara o envio físico para o DBeaver
                                        BancoDeDados.fazerSolicitacao(
                                            carona = caronaSelecionada!!,
                                            nomePassageiro = nomeLogado,
                                            cpfPassageiro = cpfLogado
                                        )

                                        escopoCorrotina.launch {
                                            try {
                                                delay(1000) // 1 segundo de folga para o Render persistir no Postgres
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
                            aoClicarVoltar = {
                                veiculoLogado = ""
                                nomeLogado = ""
                                emailLogado = ""
                                telaAtual = "login"
                            },
                            aoClicarNovoEvento = { telaAtual = "criarEvento" },
                            aoClicarHistorico = { telaAtual = "historico" }
                        )

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
                                    telaAtual = "login"
                                },
                                aoClicarEditar = { telaAtual = "editarPerfil" }
                            )
                        }

                        "editarPerfil" -> {
                            // 🟢 ADICIONADO: Captura o escopo de corrotina para garantir a navegação segura de UI
                            //val escopoMainActivity = rememberCoroutineScope()

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
                                    // 1. Atualiza as variáveis de estado local da sessão
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

                                    // 🟢 FIX CRÍTICO: Mantém o nome do usuário/username intacto para não quebrar a sessão
                                    if (usuarioAtualizado.usuario.isNotEmpty()) {
                                        usuarioLogado = usuarioAtualizado.usuario
                                    }

                                    // 2. 🟢 REDIRECIONAMENTO SÍNCRONO: Executa a troca de tela na Main UI Thread de forma limpa
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
                    }
                }
            }
        }
    }

    fun enviarTokenParaServidor(email: String, token: String) {
        thread {
            try {
                val url = URL("https://transporte-interiorano-backend.onrender.com/registrar_token")
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