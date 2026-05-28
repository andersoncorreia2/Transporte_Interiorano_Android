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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BancoDeDados.ligarRadar()

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
                    // ------------------------------------------

                    var caronaSelecionada by remember { mutableStateOf<Carona?>(null) }

                    when (telaAtual) {
                        "login" -> LoginScreen(
                            aoFazerLogin = { email, senha ->
                                mensagemLogin = "Conectando ao servidor..."
                                BancoDeDados.fazerLoginNuvem(email, senha) { usuarioEncontrado, erro ->
                                    if (usuarioEncontrado != null) {
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
                                    erroDeCadastro = "Preencha todos os campos obrigatórios, incluindo o endereço!"
                                } else {
                                    erroDeCadastro = "Conectando ao servidor..."
                                    BancoDeDados.cadastrarUsuarioNuvem(nome, cpf, telefone, email, senha, veiculo, placa, vagas, rua, numero, complemento, bairro, cidade, estado, cep) { sucesso, mensagem ->
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
                            aoPublicarEvento = { nomeEvento, cidadeOrigem, origem, cidadeDestino, destino, horario, vagas ->
                                val origemComEvento = "$nomeEvento - $origem"
                                BancoDeDados.enviarCaronaParaServidor(cidadeOrigem, origemComEvento, cidadeDestino, destino, horario, vagas, nomeLogado)
                                BancoDeDados.temEventoAtivo = true
                                telaAtual = "status"
                            },
                            aoClicarSair = {
                                telaAtual = "status"
                            }
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
                            }
                        )
                        "detalhes" -> DetalhesScreen(
                            caronaInfo = caronaSelecionada,
                            aoConfirmarCarona = {
                                if (caronaSelecionada != null) {
                                    BancoDeDados.fazerSolicitacao(caronaSelecionada!!, nomeLogado)
                                }
                                telaAtual = "listaCaronas"
                            },
                            aoClicarVoltar = { telaAtual = "listaCaronas" }
                        )
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
                            aoClicarSair = {
                                veiculoLogado = ""
                                nomeLogado = ""
                                emailLogado = ""
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
                                    // ... (seu código de salvamento)
                                    nomeLogado = usuarioAtualizado.nome
                                    // ... atualize as outras variáveis aqui também (rua, bairro, etc)
                                    telaAtual = "perfil" // Isso já garante que ele volte para a página do perfil
                                },
                                aoCancelar = { telaAtual = "perfil" }
                            )
                        }
                    }
                }
            }
        }
    }
}