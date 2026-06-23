package com.example.transporte_interiorano

import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.os.Handler
import android.os.Looper

data class Carona(
    val id: Int = 0,
    val evento_nome: String = "",
    val city_origem: String = "",
    val cidade_origem: String = "",
    val endereco_origem: String = "",
    val cidade_destino: String = "",
    val endereco_destino: String = "",
    val horario: String = "",
    val vagas: String = "",
    val motorista: String = "",
    val motorista_cpf: String = "",
    val corridas_realizadas: Int = 0,
    val passageiros_conduzidos: Int = 0,
    val status: String = "Aberta"
)

data class Usuario(
    val nome: String, val cpf: String, val email: String, val telefone: String,
    val veiculo: String = "", val placa: String = "", val senha: String = "", val vagas: String = "0",
    val rua: String = "", val numero: String = "", val complemento: String = "",
    val bairro: String = "", val cidade: String = "", val estado: String = "", val cep: String = "",
    val usuario: String = ""
)

data class Pedido(
    val idReal: Int,
    val caronaId: Int,
    val passageiro: String,
    val passageiroCpf: String,
    val status: String,
    val evento_nome: String = "",
    val cidade_origem: String = "",
    val cidade_destino: String = "",
    val horario: String = ""
)

object BancoDeDados {
    var caronas = mutableStateListOf<Carona>()
    var todosOsPedidos = mutableStateListOf<Pedido>()
    var temEventoAtivo: Boolean = false
    var tokenSessao: String = ""

    // 🟢 Guardamos o CPF do usuário logado localmente para o Radar usar em background
    var cpfUsuarioLogado: String = ""

    // 🟢 MODIFICADO: Agora recebe o cpfUsuario para injetar o filtro na URL do backend
    private fun carregarDadosSincrono(cpfUsuario: String) {
        if (cpfUsuario.isEmpty()) return
        try {
            // 🟢 Injeta o parâmetro do CPF na rota de caronas conforme o novo app.py
            val respostaCaronas = URL(
                "https://transporte-interiorano-backend.onrender.com/caronas/${
                    java.net.URLEncoder.encode(
                        cpfUsuario,
                        "UTF-8"
                    )
                }"
            ).readText()
            val jsonArrayCaronas = JSONArray(respostaCaronas)
            val novaListaCaronas = mutableListOf<Carona>()
            for (i in 0 until jsonArrayCaronas.length()) {
                val item = jsonArrayCaronas.getJSONObject(i)
                novaListaCaronas.add(
                    Carona(
                        id = item.getInt("id"),
                        evento_nome = item.optString("evento_nome", ""),
                        cidade_origem = item.optString("cidade_origem", ""),
                        endereco_origem = item.optString("endereco_origem", ""),
                        cidade_destino = item.optString("cidade_destino", ""),
                        endereco_destino = item.optString("endereco_destino", ""),
                        horario = item.getString("horario"),
                        vagas = item.getString("vagas"),
                        motorista = item.getString("motorista"),
                        motorista_cpf = item.optString("motorista_cpf", ""),
                        corridas_realizadas = item.optInt("corridas_realizadas", 0),
                        passageiros_conduzidos = item.optInt("passageiros_conduzidos", 0),
                        status = item.optString("status", "Aberta")
                    )
                )
            }

            val respostaSolicitacoes =
                URL("https://transporte-interiorano-backend.onrender.com/solicitacoes").readText()
            val jsonArraySolicitacoes = JSONArray(respostaSolicitacoes)
            val novaListaPedidos = mutableListOf<Pedido>()
            for (i in 0 until jsonArraySolicitacoes.length()) {
                val item = jsonArraySolicitacoes.getJSONObject(i)
                val status = item.getString("status")
                if (!status.equals("Finalizado", ignoreCase = true)) {
                    novaListaPedidos.add(
                        Pedido(
                            idReal = item.getInt("id"),
                            caronaId = item.getInt("carona_id"),
                            passageiro = item.getString("passageiro"),
                            passageiroCpf = item.optString("passageiro_cpf", ""),
                            status = status,
                            evento_nome = item.optString("evento_nome", ""),
                            cidade_origem = item.optString("cidade_origem", ""),
                            cidade_destino = item.optString("cidade_destino", ""),
                            horario = item.optString("horario", "")
                        )
                    )
                }
            }

            caronas.clear()
            caronas.addAll(novaListaCaronas)
            todosOsPedidos.clear()
            todosOsPedidos.addAll(novaListaPedidos)
        } catch (erro: Exception) {
            erro.printStackTrace()
        }
    }

    // 🟢 MODIFICADO: Encaminha o CPF logado
    fun buscarCaronasDoServidor() {
        thread {
            carregarDadosSincrono(cpfUsuarioLogado)
        }
    }

    // 🟢 MODIFICADO: Encaminha o CPF logado
    fun buscarSolicitacoesDoServidor() {
        thread {
            carregarDadosSincrono(cpfUsuarioLogado)
        }
    }

    fun fazerSolicitacao(carona: Carona, nomePassageiro: String, cpfPassageiro: String) {
        thread {
            try {
                val url = URL("https://transporte-interiorano-backend.onrender.com/solicitacoes")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true

                val json = JSONObject().apply {
                    put("carona_id", carona.id)
                    put("passageiro", nomePassageiro)
                    put("passageiro_cpf", cpfPassageiro)
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()

                if (conexao.responseCode == 200 || conexao.responseCode == 201) {
                    android.util.Log.d("SOLICITACAO_OK", "Pedido gravado com sucesso no Postgres!")
                    carregarDadosSincrono(cpfUsuarioLogado)
                }
            } catch (erro: Exception) {
                erro.printStackTrace()
            }
        }
    }

    fun enviarCaronaParaServidor(
        nomeEvento: String,
        cidadeOrigem: String,
        enderecoOrigem: String,
        cidadeDestino: String,
        enderecoDestino: String,
        horario: String,
        vagas: String,
        motorista: String,
        motoristaCpf: String
    ) {
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/caronas").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true

                val json = JSONObject().apply {
                    put("evento_nome", nomeEvento)
                    put("cidade_origem", cidadeOrigem)
                    put("endereco_origem", enderecoOrigem)
                    put("cidade_destino", cidadeDestino)
                    put("endereco_destino", enderecoDestino)
                    put("horario", horario)
                    put("vagas", vagas)
                    put("motorista", motorista)
                    put("motorista_cpf", motoristaCpf)
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()

                if (conexao.responseCode == 201) {
                    carregarDadosSincrono(cpfUsuarioLogado)
                }
            } catch (erro: Exception) {
                erro.printStackTrace()
            }
        }
    }

    fun cancelarPedidoPassageiro(pedidoIdReal: Int) {
        todosOsPedidos.removeAll { it.idReal == pedidoIdReal }
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/solicitacoes/$pedidoIdReal").openConnection() as HttpURLConnection
                conexao.requestMethod = "DELETE"
                if (conexao.responseCode == 200) {
                    carregarDadosSincrono(cpfUsuarioLogado)
                }
            } catch (erro: Exception) {
            }
        }
    }

    fun responderPedidoMotorista(pedidoIdReal: Int, statusDecidido: String) {
        // 🟢 PASSO 1: Atualiza imediatamente a interface local na Main Thread para dar velocidade
        Handler(Looper.getMainLooper()).post {
            val index = todosOsPedidos.indexOfFirst { it.idReal == pedidoIdReal }
            if (index != -1) {
                val pedidoAntigo = todosOsPedidos[index]
                todosOsPedidos[index] = pedidoAntigo.copy(status = statusDecidido)
            }
        }

        thread {
            try {
                val url =
                    URL("https://transporte-interiorano-backend.onrender.com/solicitacoes/$pedidoIdReal")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "PUT"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true

                val json = """{"status": "$statusDecidido"}"""
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()

                if (conexao.responseCode == 200 || conexao.responseCode == 201) {
                    android.util.Log.d("STATUS_OK", "Pedido respondido com sucesso!")
                    // 🟢 PASSO 2: Força o recarregamento síncrono também dentro da Thread de interface de forma segura
                    Handler(Looper.getMainLooper()).post {
                        thread {
                            carregarDadosSincrono(cpfUsuarioLogado)
                        }
                    }
                }
            } catch (erro: Exception) {
                erro.printStackTrace()
            }
        }
    }

    fun excluirCaronaDoServidor(caronaId: Int) {
        caronas.removeAll { it.id == caronaId }
        todosOsPedidos.removeAll { it.caronaId == caronaId }
        temEventoAtivo = false
        thread {
            try {
                val url =
                    URL("https://transporte-interiorano-backend.onrender.com/caronas/$caronaId")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "DELETE"
                conexao.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )

                if (conexao.responseCode == 200) {
                    android.util.Log.d("DELETE_OK", "Evento excluído com sucesso!")
                    carregarDadosSincrono(cpfUsuarioLogado)
                }
            } catch (erro: Exception) {
                erro.printStackTrace()
            }
        }
    }

    fun finalizarSolicitacaoNuvem(
        solicitacaoId: Int,
        motorista: String,
        passageiroCpf: String,
        caronaId: Int
    ) {
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/finalizar_solicitacao").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
                conexao.doOutput = true
                val json = JSONObject().apply {
                    put("solicitacao_id", solicitacaoId)
                    put("motorista", motorista)
                    put("passageiro_cpf", passageiroCpf)
                    put("carona_id", caronaId)
                }
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                if (conexao.responseCode == 200) {
                    carregarDadosSincrono(cpfUsuarioLogado)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 🟢 MODIFICADO: O radar de segundo plano repassa o CPF salvo em background continuamente
    fun ligarRadar() {
        thread {
            while (true) {
                if (cpfUsuarioLogado.isNotEmpty()) {
                    carregarDadosSincrono(cpfUsuarioLogado)
                }
                Thread.sleep(5000)
            }
        }
    }

    fun fazerLoginNuvem(
        usuarioRecebido: String,
        senhaRecebida: String,
        aoTerminar: (Usuario?, String) -> Unit
    ) {
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/login").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
                conexao.doOutput = true
                val json = """{"usuario": "$usuarioRecebido", "senha": "$senhaRecebida"}"""
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()

                if (conexao.responseCode == 200) {
                    val res = JSONObject(conexao.inputStream.bufferedReader().readText())
                    tokenSessao = res.getString("token")
                    val resUsuario = res.getJSONObject("usuario")

                    val usuarioLogado = Usuario(
                        nome = resUsuario.getString("nome"),
                        cpf = resUsuario.getString("cpf"),
                        email = resUsuario.getString("email"),
                        telefone = resUsuario.getString("telefone"),
                        veiculo = resUsuario.optString("veiculo", ""),
                        placa = resUsuario.optString("placa", ""),
                        vagas = resUsuario.optString("vagas", ""),
                        senha = senhaRecebida,
                        rua = resUsuario.optString("rua", ""),
                        numero = resUsuario.optString("numero", ""),
                        complemento = resUsuario.optString("complemento", ""),
                        bairro = resUsuario.optString("bairro", ""),
                        cidade = resUsuario.optString("cidade", ""),
                        estado = resUsuario.optString("estado", ""),
                        cep = resUsuario.optString("cep", ""),
                        usuario = resUsuario.optString("usuario", "")
                    )

                    // 🟢 REGISTRO CRÍTICO: No login bem-sucedido, memoriza o CPF para uso automático das chamadas subsequentes
                    cpfUsuarioLogado = usuarioLogado.cpf

                    aoTerminar(usuarioLogado, "")
                } else {
                    aoTerminar(null, "Negado")
                }
            } catch (erro: Exception) {
                aoTerminar(null, "Erro")
            }
        }
    }

    fun cadastrarUsuarioNuvem(
        nome: String,
        cpf: String,
        telefone: String,
        email: String,
        senha: String,
        veiculo: String,
        placa: String,
        vagas: String,
        rua: String,
        numero: String,
        complemento: String,
        bairro: String,
        cidade: String,
        estado: String,
        cep: String,
        username: String,
        aoTerminar: (Boolean, String) -> Unit
    ) {
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/usuarios").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
                conexao.doOutput = true

                val json = """{
                    "nome": "$nome", "cpf": "$cpf", "telefone": "$telefone", "email": "$email", 
                    "senha": "$senha", "veiculo": "$veiculo", "placa": "$placa", "vagas": "$vagas", 
                    "rua": "$rua", "numero": "$numero", "complemento": "$complemento", 
                    "bairro": "$bairro", "cidade": "$cidade", "estado": "$estado", "cep": "$cep", "usuario": "$username"
                }"""

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()
                when (conexao.responseCode) {
                    201 -> aoTerminar(true, "")
                    400 -> aoTerminar(false, "Usuário ou e-mail já existe")
                    else -> aoTerminar(false, "Erro")
                }
            } catch (erro: Exception) {
                aoTerminar(false, "Falha")
            }
        }
    }

    fun verificarDisponibilidadeUsuario(
        username: String,
        aoResultar: (Boolean, List<String>) -> Unit
    ) {
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/verificar_usuario/${username.trim()}").openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                if (conexao.responseCode == 200) {
                    val res = JSONObject(conexao.inputStream.bufferedReader().readText())
                    val disp = res.getBoolean("disponivel")
                    val arr = res.getJSONArray("sugestoes")
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        list.add(arr.getString(i))
                    }
                    aoResultar(disp, list)
                }
            } catch (e: Exception) {
                aoResultar(false, emptyList())
            }
        }
    }

    fun verificarCpfExistente(cpf: String, aoDescobrir: (Boolean) -> Unit) {
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/verificar_cpf/$cpf").openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                if (conexao.responseCode == 200) {
                    val res = JSONObject(conexao.inputStream.bufferedReader().readText())
                    aoDescobrir(res.getBoolean("existe"))
                }
            } catch (erro: Exception) {
            }
        }
    }

    fun excluirUsuario(email: String) {
        thread {
            try {
                val conexao = URL(
                    "https://transporte-interiorano-backend.onrender.com/usuarios/${
                        java.net.URLEncoder.encode(
                            email,
                            "UTF-8"
                        )
                    }"
                ).openConnection() as HttpURLConnection
                conexao.requestMethod = "DELETE"
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.responseCode
            } catch (erro: Exception) {
            }
        }
    }

    fun buscarMétricasPorCpf(cpf: String, aoTerminar: (Int, Int) -> Unit) {
        thread {
            try {
                val resposta =
                    URL("https://transporte-interiorano-backend.onrender.com/usuarios_por_cpf/$cpf").readText()
                val json = JSONObject(resposta)
                aoTerminar(
                    json.getInt("corridas_realizadas"),
                    json.getInt("passageiros_conduzidos")
                )
            } catch (e: Exception) {
                aoTerminar(0, 0)
            }
        }
    }

    fun buscarHistoricoPassageiroPorCpf(cpf: String, aoReceber: (List<Pedido>) -> Unit) {
        thread {
            try {
                val resposta = URL(
                    "https://transporte-interiorano-backend.onrender.com/historico_cpf/${
                        java.net.URLEncoder.encode(
                            cpf,
                            "UTF-8"
                        )
                    }"
                ).readText()
                val jsonArray = JSONArray(resposta)
                val listaHistorico = mutableListOf<Pedido>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    listaHistorico.add(
                        Pedido(
                            idReal = item.getInt("id"),
                            caronaId = item.getInt("carona_id"),
                            passageiro = item.getString("passageiro"),
                            passageiroCpf = item.optString("passageiro_cpf", ""),
                            status = item.getString("status"),
                            evento_nome = item.optString("evento_nome", ""),
                            cidade_origem = item.optString("cidade_origem", ""),
                            cidade_destino = item.optString("cidade_destino", ""),
                            horario = item.optString("horario", "")
                        )
                    )
                }
                aoReceber(listaHistorico)
            } catch (e: Exception) {
                aoReceber(emptyList())
            }
        }
    }

    fun buscarHistoricoMotoristaPorCpf(cpf: String, aoReceber: (List<Pedido>) -> Unit) {
        thread {
            try {
                val resposta = URL(
                    "https://transporte-interiorano-backend.onrender.com/historico_motorista_cpf/${
                        java.net.URLEncoder.encode(
                            cpf,
                            "UTF-8"
                        )
                    }"
                ).readText()
                val jsonArray = JSONArray(resposta)
                val listaHistorico = mutableListOf<Pedido>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    listaHistorico.add(
                        Pedido(
                            idReal = item.getInt("id"),
                            caronaId = item.getInt("carona_id"),
                            passageiro = item.getString("passageiro"),
                            passageiroCpf = item.optString("passageiro_cpf", ""),
                            status = item.getString("status"),
                            evento_nome = item.optString("evento_nome", ""),
                            cidade_origem = item.optString("cidade_origem", ""),
                            cidade_destino = item.optString("cidade_destino", ""),
                            horario = item.optString("horario", "")
                        )
                    )
                }
                aoReceber(listaHistorico)
            } catch (e: Exception) {
                aoReceber(emptyList())
            }
        }
    }

    fun atualizarUsuarioNuvem(usuario: Usuario, aoTerminar: (Boolean) -> Unit) {
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/usuarios/${usuario.cpf}").openConnection() as HttpURLConnection
                conexao.requestMethod = "PUT"
                conexao.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.doOutput = true

                val json = """{
                    "nome": "${usuario.nome}", "telefone": "${usuario.telefone}", "email": "${usuario.email}", 
                    "veiculo": "${usuario.veiculo}", "placa": "${usuario.placa}", "vagas": "${usuario.vagas}",
                    "rua": "${usuario.rua}", "numero": "${usuario.numero}", "complemento": "${usuario.complemento}",
                    "bairro": "${usuario.bairro}", "cidade": "${usuario.cidade}", "estado": "${usuario.estado}", "cep": "${usuario.cep}"
                }"""

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()
                aoTerminar(conexao.responseCode == 200)
            } catch (erro: Exception) {
                aoTerminar(false)
            }
        }
    }

    fun solicitarCodigoRecuperacao(
        email: String,
        cpf: String,
        aoTerminar: (Boolean, String, String?) -> Unit
    ) {
        val cpfLimpo = cpf.filter { it.isDigit() }.trim()
        val emailTratado = email.trim().lowercase()

        thread {
            try {
                val url =
                    URL("https://transporte-interiorano-backend.onrender.com/solicitar_codigo")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
                conexao.doOutput = true
                conexao.connectTimeout = 60000
                conexao.readTimeout = 60000

                val jsonPayload = JSONObject().apply {
                    put("email", emailTratado)
                    put("cpf", cpfLimpo)
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(jsonPayload.toString())
                escritor.flush()
                escritor.close()

                val codigoResposta = conexao.responseCode

                if (codigoResposta == 200) {
                    val textoResposta = conexao.inputStream.bufferedReader().readText()
                    val res = JSONObject(textoResposta)
                    val mensagemServidor = res.optString(
                        "mensagem",
                        "Código enviado para o e-mail cadastrado!"
                    )
                    aoTerminar(true, mensagemServidor, "")
                } else {
                    val erroStream = conexao.errorStream
                    val textoErro = erroStream?.bufferedReader()?.readText()
                        ?: "Erro desconhecido no servidor"
                    var mensagemErro = "Dados incorretos ou não cadastrados."
                    try {
                        val resErro = JSONObject(textoErro)
                        mensagemErro = resErro.optString("erro", mensagemErro)
                    } catch (e: Exception) {
                    }
                    aoTerminar(false, mensagemErro, null)
                }
            } catch (erro: Exception) {
                erro.printStackTrace()
                aoTerminar(false, "Falha na conexão. Verifique sua internet.", null)
            }
        }
    }

    fun redefinirSenhaComCodigo(
        email: String,
        codigo: String,
        novaSenha: String,
        aoTerminar: (Boolean, String) -> Unit
    ) {
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/validar_e_redefinir_senha").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
                conexao.doOutput = true
                conexao.connectTimeout = 60000
                conexao.readTimeout = 60000

                val json = JSONObject().apply {
                    put("email", email.trim().lowercase())
                    put("codigo", codigo.trim())
                    put("senha", novaSenha)
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()

                val codigoResposta = conexao.responseCode

                if (codigoResposta == 200) {
                    Handler(Looper.getMainLooper()).post {
                        aoTerminar(true, "Senha alterada com sucesso!")
                    }
                } else {
                    val erroStream = conexao.errorStream
                    val textoErro = erroStream?.bufferedReader()?.readText()
                        ?: "Erro desconhecido (Código $codigoResposta)"
                    var mensagemErro = "Falha ao alterar a senha."
                    try {
                        val resErro = JSONObject(textoErro)
                        mensagemErro = resErro.optString("erro", mensagemErro)
                    } catch (e: Exception) {
                        mensagemErro = "Erro do servidor: $textoErro"
                    }
                    Handler(Looper.getMainLooper()).post {
                        aoTerminar(false, mensagemErro)
                    }
                }
            } catch (erro: Exception) {
                erro.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    aoTerminar(false, "Erro de conexão: ${erro.message}")
                }
            }
        }
    }

    fun listarNomesNoBanco() {}

    fun cancelarViagemGeralMotorista(
        caronaId: Int,
        motivoTexto: String,
        aoConcluir: (Boolean) -> Unit
    ) {
        thread {
            try {
                val url =
                    URL("https://transporte-interiorano-backend.onrender.com/cancelar_carona_geral")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
                conexao.doOutput = true

                val json = JSONObject().apply {
                    put("carona_id", caronaId)
                    put("motivo", motivoTexto)
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()

                if (conexao.responseCode == 200) {
                    // Recarrega os dados locais para o app atualizar a tela do motorista imediatamente
                    carregarDadosSincrono(cpfUsuarioLogado)

                    // Retorna o sucesso para a Thread Principal (UI) atualizar os componentes visuais
                    Handler(Looper.getMainLooper()).post { aoConcluir(true) }
                } else {
                    Handler(Looper.getMainLooper()).post { aoConcluir(false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { aoConcluir(false) }
            }
        }
    }
}