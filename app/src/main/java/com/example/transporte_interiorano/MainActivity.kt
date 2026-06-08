package com.example.transporte_interiorano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.safeDrawingPadding
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
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    var telaAtual by remember { mutableStateOf("login") }
                    var erroDeCadastro by remember { mutableStateOf("") }
                    var mensagemLogin by remember { mutableStateOf("") }

                    // --- VARIÁVEIS DE ESTADO QUE FALTAVAM ---
                    var nomeLogado by remember { mutableStateOf("") }
                    var cpfLogado by remember { mutableStateOf("") }
                    var emailLogado by remember { mutableStateOf("") }
                    var telefoneLogado by remember { mutableStateOf("") }
                    var veiculoLogado by remember { mutableStateOf("") }
                    var placaLogada by remember { mutableStateOf("") }
                    var vagasLogada by remember { mutableStateOf("") }
                    var senhaLogada by remember { mutableStateOf("") }
                    var ruaLogada by remember { mutableStateOf("") }
                    var numeroLogado by remember { mutableStateOf("") }
                    var complementoLogado by remember { mutableStateOf("") }
                    var bairroLogado by remember { mutableStateOf("") }
                    var cidadeLogada by remember { mutableStateOf("") }
                    var estadoLogado by remember { mutableStateOf("") }
                    var cepLogado by remember { mutableStateOf("") }
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
                        "login" -> LoginScreen(
                            aoFazerLogin = { email, senha ->
                                mensagemLogin = "Conectando ao servidor..."
                                BancoDeDados.fazerLoginNuvem(email, senha) { usuarioEncontrado, erro ->
                                    if (usuarioEncontrado != null) {
                                        // 1. Preenche os dados do usuário
                                        nomeLogado = usuarioEncontrado.nome
                                        cpfLogado = usuarioEncontrado.cpf
                                        emailLogado = usuarioEncontrado.email
                                        telefoneLogado = usuarioEncontrado.telefone
                                        veiculoLogado = usuarioEncontrado.veiculo
                                        placaLogada = usuarioEncontrado.placa
                                        vagasLogada = usuarioEncontrado.vagas
                                        senhaLogada = usuarioEncontrado.senha
                                        ruaLogada = usuarioEncontrado.rua
                                        numeroLogado = usuarioEncontrado.numero
                                        complementoLogado = usuarioEncontrado.complemento
                                        bairroLogado = usuarioEncontrado.bairro
                                        cidadeLogada = usuarioEncontrado.cidade
                                        estadoLogado = usuarioEncontrado.estado
                                        cepLogado = usuarioEncontrado.cep
                                        mensagemLogin = ""

                                        // 2. AQUI ESTÁ O SEGREDO: BUSCAR MÉTRICAS PELO CPF LOGADO
                                        BancoDeDados.buscarMétricasPorCpf(usuarioEncontrado.cpf) { corridas, pass ->
                                            corridasRealizadas = corridas
                                            passageirosConduzidos = pass
                                        }

                                        // 3. Notificação e navegação
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
                            mensagemErro = mensagemLogin
                        )

                        "cadastro" -> CadastroScreen(
                            aoConcluirCadastro = { nome, cpf, telefone, email, senha, veiculo, placa, vagas, rua, numero, complemento, bairro, cidade, estado, cep ->
                                if (nome.isBlank() || cpf.isBlank() || telefone.isBlank() || email.isBlank() || senha.isBlank() || rua.isBlank() || numero.isBlank() || bairro.isBlank() || cidade.isBlank() || estado.isBlank() || cep.isBlank()) {
                                    erroDeCadastro =
                                        "Preencha todos os campos obrigatórios, incluindo o endereço!"
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
                                        cep
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
                            cpfLogado = cpfLogado // 🆕 PASSE O CPF DA MAINACTIVITY PARA A TELA
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
                            aoClicarPerfil = {
                                telaAtual = "perfil"
                            },
                            aoClicarHistorico = { // 👈 Adicione isso
                                telaAtual = "historico"
                            }
                        )

                        "historico" -> HistoricoScreen(
                            cpfPassageiro = cpfLogado, // O parâmetro que o HistoricoScreen espera
                            aoClicarVoltar = { telaAtual = "listaCaronas" }
                        )

                        "detalhes" -> {
                            DetalhesScreen(
                                caronaInfo = caronaSelecionada,
                                nomePassageiroLogado = nomeLogado, // 👈 PASSE O NOME AQUI
                                corridasIniciais = corridasRealizadas,
                                passageirosIniciais = passageirosConduzidos,
                                aoConfirmarCarona = {
                                    if (caronaSelecionada != null) {
                                        BancoDeDados.fazerSolicitacao(
                                            caronaSelecionada!!,
                                            nomeLogado,
                                            cpfLogado
                                        )
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
                            aoClicarNovoEvento = {
                                telaAtual = "criarEvento"
                            }
                        )

                        "perfil" -> PerfilScreen(
                            nome = nomeLogado,
                            email = emailLogado,
                            veiculo = veiculoLogado,
                            placa = placaLogada,
                            corridas = corridasRealizadas,       // ⬅️ ADICIONEI ESTA LINHA
                            passageiros = passageirosConduzidos, // ⬅️ ADICIONEI ESTA LINHA
                            aoClicarSair = {
                                veiculoLogado = ""
                                nomeLogado = ""
                                emailLogado = ""
                                telaAtual = "login"
                            },
                            aoClicarVoltar = {
                                telaAtual =
                                    if (veiculoLogado.isNotEmpty()) "status" else "listaCaronas"
                            },
                            aoClicarExcluirConta = {
                                BancoDeDados.excluirUsuario(emailLogado)
                                veiculoLogado = ""
                                nomeLogado = ""
                                emailLogado = ""
                                telaAtual = "login"
                            },
                            // 🆕 O botão agora envia o utilizador para a tela de edição!
                            aoClicarEditar = {
                                telaAtual = "editarPerfil"
                            }
                        )

                        "editarPerfil" -> {
                            // Criamos um "Usuário" temporário só com o que temos guardado para a tela abrir
                            val usuarioParaEditar = Usuario(
                                nome = nomeLogado,
                                cpf = "",
                                email = emailLogado,
                                telefone = telefoneLogado,
                                veiculo = veiculoLogado,
                                placa = placaLogada,
                                vagas = vagasLogada,
                                senha = "",
                                rua = ruaLogada,
                                numero = numeroLogado,
                                complemento = complementoLogado,
                                bairro = bairroLogado,
                                cidade = cidadeLogada,
                                estado = estadoLogado,
                                cep = cepLogado,
                            )

                            EditarPerfilScreen(
                                usuarioAtual = Usuario(
                                    nome = nomeLogado,
                                    cpf = cpfLogado,       // 👈 Certifique-se de que cpfLogado está preenchido!
                                    email = emailLogado,
                                    telefone = telefoneLogado,
                                    veiculo = veiculoLogado,
                                    placa = placaLogada,
                                    vagas = vagasLogada,
                                    senha = "",            // senha não deve ser editada aqui
                                    rua = ruaLogada,       // 👈 Verifique se esta variável tem o valor do banco
                                    numero = numeroLogado,
                                    complemento = complementoLogado,
                                    bairro = bairroLogado,
                                    cidade = cidadeLogada,
                                    estado = estadoLogado,
                                    cep = cepLogado
                                ),
                                aoSalvar = { usuarioAtualizado ->
                                    nomeLogado = usuarioAtualizado.nome
                                    telefoneLogado = usuarioAtualizado.telefone
                                    veiculoLogado = usuarioAtualizado.veiculo
                                    placaLogada = usuarioAtualizado.placa
                                    vagasLogada = usuarioAtualizado.vagas
                                    ruaLogada = usuarioAtualizado.rua
                                    numeroLogado = usuarioAtualizado.numero
                                    complementoLogado = usuarioAtualizado.complemento
                                    bairroLogado = usuarioAtualizado.bairro
                                    cidadeLogada = usuarioAtualizado.cidade
                                    estadoLogado = usuarioAtualizado.estado
                                    cepLogado = usuarioAtualizado.cep

                                    telaAtual =
                                        "perfil" // Isso força o App a voltar para a tela de perfil
                                },
                                aoCancelar = { telaAtual = "perfil" }
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
                val url = URL("http://192.168.1.67:5000/registrar_token")
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