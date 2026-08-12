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
//import com.example.transporte_interiorano.BuildConfig

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
    val valor_corrida: String = "",
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
    val horario: String = "",
    val dataCriacao: String = "", //Adicionado em 13/07/2026 às 15:03
    val dataFinalizacao: String = "", // 🟢 NOVA INFORMAÇÃO
    val motoristaNome: String = "" // 🟢 ADICIONADO: Recebe o nome do motorista
)

object BancoDeDados {
    // 🟢 CENTRALIZADO: Link do túnel ngrok gerado no seu VS Code para o ambiente de desenvolvimento local
    //const val BASE_URL = "https://obnoxious-audience-finite.ngrok-free.dev"
    // Mude para o IP real da sua ancoragem atual:
    //private const val BASE_URL = "http://10.233.20.194:5000"
    //private const val BASE_URL = "http://10.127.212.194:5000"
    //private const val BASE_URL = "http://192.168.1.66:5000"
    //private const val BASE_URL = "https://transporte-interiorano-backend.onrender.com"
    const val BASE_URL = "https://transporte-interiorano-backend.onrender.com"

    // 🟢 O link agora muda sozinho conforme o botão mágico escolhido!
    //val BASE_URL = BuildConfig.BASE_URL

    var caronas = mutableStateListOf<Carona>()

    var corridasEmergentesDisponiveis = mutableStateListOf<JSONObject>()
    var todosOsPedidos = mutableStateListOf<Pedido>()
    var temEventoAtivo: Boolean = false
    var tokenSessao: String = ""
    var cpfUsuarioLogado: String = ""
    var deadlineCancelamentoEpoch: Long = 0L

    private fun carregarDadosSincrono(cpfUsuario: String) {
        if (cpfUsuario.isEmpty()) return
        try {
            val respostaCaronas = URL(
                "$BASE_URL/caronas/${
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
                        horario = item.optString("horario", ""), // 🟢 Seguro contra crash
                        vagas = item.optString("vagas", ""),       // 🟢 Seguro contra crash
                        valor_corrida = item.optString("valor_corrida", "0.00"), // 🟢 Seguro contra crash
                        motorista = item.optString("motorista", ""), // 🟢 Seguro contra crash
                        motorista_cpf = item.optString("motorista_cpf", ""),
                        corridas_realizadas = item.optInt("corridas_realizadas", 0),
                        passageiros_conduzidos = item.optInt("passageiros_conduzidos", 0),
                        status = item.optString("status", "Aberta")
                    )
                )
            }

            val respostaSolicitacoes = URL("$BASE_URL/solicitacoes").readText()
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
                            horario = item.optString("horario", ""),
                            dataCriacao = item.optString(
                                "data_criacao",
                                ""
                            ) //Adicionado em 13/07/2026 às 15:03
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

    fun buscarCaronasDoServidor() {
        thread {
            carregarDadosSincrono(cpfUsuarioLogado)
        }
    }

    fun buscarSolicitacoesDoServidor() {
        thread {
            carregarDadosSincrono(cpfUsuarioLogado)
        }
    }

    fun fazerSolicitacao(carona: Carona, nomePassageiro: String, cpfPassageiro: String) {
        thread {
            try {
                val url = URL("$BASE_URL/solicitacoes")
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
                    android.util.Log.d("SOLICITACAO_OK", "Pedido gravado com sucesso!")
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
        valorCorrida: String,
        motorista: String,
        motoristaCpf: String
    ) {
        thread {
            try {
                val conexao = URL("$BASE_URL/caronas").openConnection() as HttpURLConnection
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
                    put("valor_corrida", valorCorrida)
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

    fun atualizarCaronaNoServidor(
        id: Int,
        nomeEvento: String,
        cidadeOrigem: String,
        enderecoOrigem: String,
        cidadeDestino: String,
        enderecoDestino: String,
        horario: String,
        vagas: String,
        valorCorrida: String,
        aoConcluir: (Boolean) -> Unit
    ) {
        thread {
            try {
                // Rota PUT mapeada para atualizar a carona pelo ID correspondente
                val conexao = URL("$BASE_URL/caronas/$id").openConnection() as HttpURLConnection
                conexao.requestMethod = "PUT"
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
                    put("valor_corrida", valorCorrida)
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()

                if (conexao.responseCode == 200) {
                    carregarDadosSincrono(cpfUsuarioLogado)
                    Handler(Looper.getMainLooper()).post { aoConcluir(true) }
                } else {
                    Handler(Looper.getMainLooper()).post { aoConcluir(false) }
                }
            } catch (erro: Exception) {
                erro.printStackTrace()
                Handler(Looper.getMainLooper()).post { aoConcluir(false) }
            }
        }
    }

    fun cancelarPedidoPassageiro(pedidoIdReal: Int) {
        todosOsPedidos.removeAll { it.idReal == pedidoIdReal }
        thread {
            try {
                val conexao =
                    URL("$BASE_URL/solicitacoes/$pedidoIdReal").openConnection() as HttpURLConnection
                conexao.requestMethod = "DELETE"
                if (conexao.responseCode == 200) {
                    carregarDadosSincrono(cpfUsuarioLogado)
                }
            } catch (erro: Exception) {
            }
        }
    }

    fun responderPedidoMotorista(pedidoIdReal: Int, statusDecidido: String) {
        Handler(Looper.getMainLooper()).post {
            val index = todosOsPedidos.indexOfFirst { it.idReal == pedidoIdReal }
            if (index != -1) {
                val pedidoAntigo = todosOsPedidos[index]
                todosOsPedidos[index] = pedidoAntigo.copy(status = statusDecidido)
            }
        }

        thread {
            try {
                val url = URL("$BASE_URL/solicitacoes/$pedidoIdReal")
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
                val url = URL("$BASE_URL/caronas/$caronaId")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "DELETE"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")

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
                    URL("$BASE_URL/finalizar_solicitacao").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
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
                val url = URL("$BASE_URL/login")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.connectTimeout = 5000
                conexao.readTimeout = 5000
                conexao.doOutput = true

                val usuarioTratado = usuarioRecebido.trim().lowercase()

                // Envelopamento seguro usando JSONObject nativo
                val json = JSONObject().apply {
                    put("usuario", usuarioTratado)
                    put("senha", senhaRecebida)
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()

                if (conexao.responseCode == 200) {
                    // 💡 CORREÇÃO: Consumo seguro e explícito do buffer de resposta para evitar estouros
                    val textoResposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val res = JSONObject(textoResposta)
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

                    cpfUsuarioLogado = usuarioLogado.cpf

                    // 🟢 CORREÇÃO: Garante que o sucesso do login e a troca de tela rodem de forma segura na Main Thread com o token pronto
                    Handler(Looper.getMainLooper()).post {
                        aoTerminar(usuarioLogado, "")
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        aoTerminar(null, "Usuário ou senha incorretos.")
                    }
                }
            } catch (erro: Exception) {
                erro.printStackTrace()
                aoTerminar(null, "Falha na comunicação com o servidor.")
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
                val url = URL("$BASE_URL/usuarios")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.connectTimeout = 5000
                conexao.readTimeout = 5000
                conexao.doOutput = true

                val json = JSONObject().apply {
                    put("nome", nome)
                    put("cpf", cpf)
                    put("telefone", telefone)
                    put("email", email.trim().lowercase())
                    put("senha", senha)
                    put("veiculo", veiculo)
                    put("placa", placa)
                    put("vagas", vagas)
                    put("rua", rua)
                    put("numero", numero)
                    put("complemento", complemento)
                    put("bairro", bairro)
                    put("cidade", cidade)
                    put("estado", estado)
                    put("cep", cep)
                    put("usuario", username.trim().lowercase())
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()

                when (conexao.responseCode) {
                    201 -> aoTerminar(true, "")
                    400 -> aoTerminar(false, "Usuário ou e-mail já existe")
                    else -> aoTerminar(false, "Erro no servidor")
                }
            } catch (erro: Exception) {
                erro.printStackTrace()
                aoTerminar(false, "Falha na conexão.")
            }
        }
    }

    fun verificarDisponibilidadeUsuario(
        username: String,
        aoResultar: (Boolean, List<String>) -> Unit
    ) {
        thread {
            try {
                // 💡 CORREÇÃO CRÍTICA: Codifica o nome de usuário para aceitar pontos (.), espaços e acentos na URL
                val usuarioCodificado = java.net.URLEncoder.encode(username.trim(), "UTF-8")

                val url = URL("$BASE_URL/verificar_usuario/$usuarioCodificado")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                conexao.connectTimeout = 5000
                conexao.readTimeout = 5000

                if (conexao.responseCode == 200) {
                    val textoResposta = conexao.inputStream.bufferedReader().readText()
                    val res = JSONObject(textoResposta)
                    val disp = res.getBoolean("disponivel")
                    val arr = res.getJSONArray("sugestoes")

                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        list.add(arr.getString(i))
                    }

                    // Retorna os dados com sucesso para a interface
                    aoResultar(disp, list)
                } else {
                    // Se o servidor responder algo diferente de 200, assume que está disponível para não travar o usuário
                    aoResultar(true, emptyList())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Em caso de falha de rede física, assume verdadeiro para o cadastro passar
                aoResultar(true, emptyList())
            }
        }
    }

    fun verificarCpfExistente(cpf: String, aoDescobrir: (Boolean) -> Unit) {
        thread {
            try {
                val conexao =
                    URL("$BASE_URL/verificar_cpf/$cpf").openConnection() as HttpURLConnection
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
                    "$BASE_URL/usuarios/${
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
                val resposta = URL("$BASE_URL/usuarios_por_cpf/$cpf").readText()
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
                    "$BASE_URL/historico_cpf/${
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
                            //horario = item.optString("horario", ""),
                            horario = item.optString("data_criacao", ""),
                            dataCriacao = item.optString("data_criacao", ""),
                            dataFinalizacao = item.optString("data_finalizacao", ""),
                            motoristaNome = item.optString(
                                "motorista_nome",
                                ""
                            ) // 🟢 CAPTURA DO NOME DO MOTORISTA DO PYTHON
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
                    "$BASE_URL/historico_motorista_cpf/${
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
                            //horario = item.optString("horario", ""),
                            horario = item.optString("data_criacao", ""),
                            dataCriacao = item.optString("data_criacao", ""),
                            dataFinalizacao = item.optString("data_finalizacao", "")
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
                    URL("$BASE_URL/usuarios/${usuario.cpf}").openConnection() as HttpURLConnection
                conexao.requestMethod = "PUT"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
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
                val url = URL("$BASE_URL/solicitar_codigo")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
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
                    val mensagemServidor =
                        res.optString("mensagem", "Código enviado para o e-mail cadastrado!")
                    aoTerminar(true, mensagemServidor, "")
                } else {
                    val erroStream = conexao.errorStream
                    val textoErro =
                        erroStream?.bufferedReader()?.readText() ?: "Erro desconhecido no servidor"
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
                    URL("$BASE_URL/validar_e_redefinir_senha").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
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
                val url = URL("$BASE_URL/cancelar_carona_geral")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
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
                    carregarDadosSincrono(cpfUsuarioLogado)
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

    // 🟢 1. DISPARAR SOLICITAÇÃO DE CORRIDA EMERGENTE (PASSAGEIRO)
    fun criarCorridaEmergenteNuvem(
        enderecoOrigem: String,
        enderecoDestino: String,
        latOrigem: Double,
        lngOrigem: Double,
        latDestino: Double,
        lngDestino: Double,
        veiculoTipo: String,
        formaPagamento: String,
        aoConcluir: (Boolean, String, Int?) -> Unit
    ) {
        thread {
            try {
                val url = URL("$BASE_URL/corridas/emergentes")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.setRequestProperty(
                    "Authorization",
                    "Bearer $tokenSessao"
                ) // Envia o token do passageiro logado
                conexao.doOutput = true

                val json = JSONObject().apply {
                    put("endereco_origem", enderecoOrigem)
                    put("endereco_destino", enderecoDestino)
                    put("origem_latitude", latOrigem)
                    put("origem_longitude", lngOrigem)
                    put("destino_latitude", latDestino)
                    put("destino_longitude", lngDestino)
                    put("veiculo_tipo", veiculoTipo)
                    put("forma_pagamento", formaPagamento)
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()

                if (conexao.responseCode == 201) {
                    val textoResposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val res = JSONObject(textoResposta)
                    val msg = res.optString("mensagem", "Procurando motoristas...")

                    // 🟢 CAPTURA DINÂMICA: Pega o ID real retornado pelo Python no Render
                    val idCorridaGerado = res.optInt("corrida_id", 0)

                    Handler(Looper.getMainLooper()).post {
                        // Envia o ID real para a tela do passageiro
                        aoConcluir(true, msg, idCorridaGerado)
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        aoConcluir(
                            false,
                            "Erro ao solicitar corrida.",
                            null
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { aoConcluir(false, "Falha de rede.", null) }
            }
        }
    }

    // 🟢 2. FICAR ONLINE NO RADAR (MOTORISTA)
    fun ficarOnlineRadarMotorista(aoConcluir: (Boolean, String) -> Unit) {
        thread {
            try {
                // Altera a modalidade ativa do motorista para 'Emergencial' na nuvem
                val url = URL("$BASE_URL/usuarios/alterar_modalidade")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.doOutput = true

                val json = JSONObject().apply {
                    put("modalidade", "Emergencial")
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()

                if (conexao.responseCode == 200) {
                    // 🟢 Captura a resposta do servidor Render com segurança
                    val textoResposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val msg =
                        JSONObject(textoResposta).optString("mensagem", "Modo emergencial ativo!")

                    Handler(Looper.getMainLooper()).post { aoConcluir(true, msg) }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        aoConcluir(
                            false,
                            "Não foi possível ativar o modo online."
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    aoConcluir(
                        false,
                        "Erro de comunicação com o servidor."
                    )
                }
            }
        }
    }

    // 🟢 3. BUSCADOR DE ENDEREÇOS AUTOMÁTICO (AUTO-SUGESTÃO GRATUITA)
    fun buscarSugestoesDeEndereco(
        textoDigitado: String,
        aoReceberSugestoes: (List<String>) -> Unit
    ) {
        if (textoDigitado.trim().length < 3) {
            aoReceberSugestoes(emptyList())
            return
        }
        thread {
            try {
                // Consulta o serviço gratuito de Geocodificação Nominatim (OpenStreetMap) filtrando por Pernambuco
                val queryCodificada = java.net.URLEncoder.encode("$textoDigitado, PE", "UTF-8")
                val url =
                    URL("https://nominatim.openstreetmap.org/search?q=$queryCodificada&format=json&addressdetails=1&limit=5")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.setRequestProperty(
                    "User-Agent",
                    "com.example.transporte_interiorano"
                ) // Exigido pelo OpenStreetMap
                conexao.connectTimeout = 4000
                conexao.readTimeout = 4000

                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(resposta)
                    val listaEnderecos = mutableListOf<String>()

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val displayNome = item.optString("display_name", "")
                        if (displayNome.isNotEmpty()) {
                            listaEnderecos.add(displayNome)
                        }
                    }
                    Handler(Looper.getMainLooper()).post { aoReceberSugestoes(listaEnderecos) }
                } else {
                    Handler(Looper.getMainLooper()).post { aoReceberSugestoes(emptyList()) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { aoReceberSugestoes(emptyList()) }
            }
        }
    }

    // 🟢 4. RADAR ATIVO DO MOTORISTA: Puxa chamados da nuvem a cada 4 segundos
    fun buscarCorridasEmergentesDoServidor(aoReceber: (Boolean) -> Unit) {
        thread {
            try {
                val url = URL("$BASE_URL/corridas/emergentes/disponiveis")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                conexao.setRequestProperty(
                    "Authorization",
                    "Bearer $tokenSessao"
                ) // Envia o token do motorista logado
                conexao.connectTimeout = 4000
                conexao.readTimeout = 4000

                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(resposta)

                    val listaTemporaria = mutableListOf<JSONObject>()
                    for (i in 0 until jsonArray.length()) {
                        listaTemporaria.add(jsonArray.getJSONObject(i))
                    }

                    // Atualiza a lista observável na Thread principal do Android Studio
                    Handler(Looper.getMainLooper()).post {
                        corridasEmergentesDisponiveis.clear()
                        corridasEmergentesDisponiveis.addAll(listaTemporaria)
                        aoReceber(true)
                    }
                } else {
                    Handler(Looper.getMainLooper()).post { aoReceber(false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { aoReceber(false) }
            }
        }
    }

    // 🟢 5. MOTORISTA ACEITA A CORRIDA DE EMERGÊNCIA
    fun aceitarCorridaEmergenteNuvem(corridaId: Int, aoConcluir: (Boolean, String) -> Unit) {
        thread {
            try {
                val url = URL("$BASE_URL/corridas/emergentes/aceitar/$corridaId")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "PUT"
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.connectTimeout = 4000

                if (conexao.responseCode == 200) {
                    val texto = conexao.inputStream.bufferedReader().use { it.readText() }
                    val msg = JSONObject(texto).optString("mensagem", "Corrida aceita!")
                    Handler(Looper.getMainLooper()).post { aoConcluir(true, msg) }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        aoConcluir(
                            false,
                            "Esta corrida não está mais disponível."
                        )
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    aoConcluir(
                        false,
                        "Falha de rede com o servidor."
                    )
                }
            }
        }
    }

    // 🟢 6. PASSAGEIRO CANCELA A SOLICITAÇÃO ATIVA
    fun cancelarCorridaEmergentePassageiro(corridaId: Int, aoConcluir: (Boolean) -> Unit) {
        thread {
            try {
                val url = URL("$BASE_URL/corridas/emergentes/cancelar/$corridaId")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "DELETE"
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.connectTimeout = 4000

                val sucesso = conexao.responseCode == 200
                Handler(Looper.getMainLooper()).post { aoConcluir(sucesso) }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { aoConcluir(false) }
            }
        }
    }

    // 🟢 7. FUNÇÃO ADICIONADA: CONSULTA O STATUS ATUAL DA CORRIDA (USADO PELO POOLING DO PASSAGEIRO)
    fun buscarStatusCorridaNuvem(corridaId: Int, aoReceber: (JSONObject?) -> Unit) {
        thread {
            try {
                val url = URL("$BASE_URL/corridas/emergentes/status/$corridaId")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.connectTimeout = 3000

                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resposta)
                    Handler(Looper.getMainLooper()).post { aoReceber(json) }
                } else {
                    Handler(Looper.getMainLooper()).post { aoReceber(null) }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { aoReceber(null) }
            }
        }
    }

    // 🟢 FUNÇÃO ADICIONADA: Altera dinamicamente o status do modo emergencial via PUT seguro com JWT
    // 🟢 VERSÃO FINANCEIRA ATUALIZADA: Altera o status e envia reporte de pagamento e valor
    fun atualizarStatusCorridaEmergenteNuvem(
        corridaId: Int,
        statusAlvo: String,
        pago: Boolean,
        valorCorrida: Double,
        aoConcluir: (Boolean) -> Unit
    ) {
        thread {
            try {
                val url = URL("$BASE_URL/corridas/emergentes/atualizar_status/$corridaId")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "PUT"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.doOutput = true

                val json = JSONObject().apply {
                    put("status", statusAlvo)
                    put("pago", pago)
                    put("valor_corrida", valorCorrida)
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()

                val sucesso = conexao.responseCode == 200
                Handler(Looper.getMainLooper()).post { aoConcluir(sucesso) }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { aoConcluir(false) }
            }
        }
    }

    fun atualizarLocalizacaoMotoristaNuvem(idCorrida: Int, latitude: Double, longitude: Double) {
        kotlin.concurrent.thread {
            try {
                val url = URL("$BASE_URL/corridas_emergentes/atualizar_localizacao")
                val conexao = url.openConnection() as java.net.HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.doOutput = true

                val json = org.json.JSONObject().apply {
                    put("id", idCorrida)
                    put("motorista_latitude", latitude)
                    put("motorista_longitude", longitude)
                }

                val escritor = java.io.OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()

                val codigoHttp = conexao.responseCode
                android.util.Log.d(
                    "GPS_TRACK",
                    "Sincronização de localização na nuvem: Código $codigoHttp"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 🟢 NOVA FUNÇÃO: Tenta recuperar o estado caso o app seja fechado sem querer
    fun recuperarEstadoCorridaEmergenteNuvem(aoReceber: (JSONObject?) -> Unit) {
        thread {
            try {
                val url = URL("$BASE_URL/corridas/emergentes/recuperar_estado")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.connectTimeout = 4000

                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resposta)
                    // Se o JSON trouxer um "id", significa que ele encontrou uma corrida ativa pendente
                    if (json.has("id")) {
                        Handler(Looper.getMainLooper()).post { aoReceber(json) }
                    } else {
                        Handler(Looper.getMainLooper()).post { aoReceber(null) }
                    }
                } else {
                    Handler(Looper.getMainLooper()).post { aoReceber(null) }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { aoReceber(null) }
            }
        }
    }

    // 🟢 ADICIONADO: Puxa o histórico de corridas emergentes concluídas do passageiro e converte em Pedido
    fun buscarHistoricoEmergencialPassageiro(cpf: String, aoReceber: (List<Pedido>) -> Unit) {
        thread {
            try {
                val urlCodificado = java.net.URLEncoder.encode(cpf, "UTF-8")
                val resposta =
                    URL("$BASE_URL/corridas/emergentes/historico_passageiro/$urlCodificado").readText()
                val jsonArray = JSONArray(resposta)
                val listaEmergencial = mutableListOf<Pedido>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    if (item.optString("status", "").equals("Finalizada", ignoreCase = true)) {
                        listaEmergencial.add(
                            Pedido(
                                idReal = item.getInt("id"),
                                caronaId = 0, // Emergencial não possui ID de carona programada
                                passageiro = "",
                                passageiroCpf = cpf,
                                status = "Finalizada",
                                evento_nome = "Corrida Emergencial ⚡",
                                cidade_origem = item.optString("endereco_origem", "Origem")
                                    .split(",").firstOrNull() ?: "Origem",
                                cidade_destino = item.optString("endereco_destino", "Destino")
                                    .split(",").firstOrNull() ?: "Destino",
                                horario = item.optString("data_criacao", ""),
                                dataCriacao = item.optString("data_criacao", ""),
                                dataFinalizacao = item.optString("data_finalizacao", "")
                            )
                        )
                    }
                }
                aoReceber(listaEmergencial)
            } catch (e: Exception) {
                e.printStackTrace()
                aoReceber(emptyList())
            }
        }
    }

    // 🟢 ADICIONADO: Puxa o histórico de corridas emergentes concluídas do motorista e converte em Pedido
    fun buscarHistoricoEmergencialMotorista(cpf: String, aoReceber: (List<Pedido>) -> Unit) {
        thread {
            try {
                val urlCodificado = java.net.URLEncoder.encode(cpf, "UTF-8")
                val resposta =
                    URL("$BASE_URL/corridas/emergentes/historico_motorista/$urlCodificado").readText()
                val jsonArray = JSONArray(resposta)
                val listaEmergencial = mutableListOf<Pedido>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    if (item.optString("status", "").equals("Finalizada", ignoreCase = true)) {
                        listaEmergencial.add(
                            Pedido(
                                idReal = item.getInt("id"),
                                caronaId = 0,
                                passageiro = item.optString("passageiro_nome", "Passageiro"),
                                passageiroCpf = "",
                                status = "Finalizada",
                                evento_nome = "Corrida Emergencial ⚡",
                                cidade_origem = item.optString("endereco_origem", "Origem")
                                    .split(",").firstOrNull() ?: "Origem",
                                cidade_destino = item.optString("endereco_destino", "Destino")
                                    .split(",").firstOrNull() ?: "Destino",
                                horario = item.optString("data_criacao", ""),
                                dataCriacao = item.optString("data_criacao", ""),
                                dataFinalizacao = item.optString("data_finalizacao", "")
                            )
                        )
                    }
                }
                aoReceber(listaEmergencial)
            } catch (e: Exception) {
                e.printStackTrace()
                aoReceber(emptyList())
            }
        }
    }

    // 🟢 8. VERIFICAR SE O PASSAGEIRO JÁ VALIDOU A IDENTIDADE
    fun verificarStatusIdentidadeNuvem(aoResultado: (Boolean) -> Unit) {
        thread {
            try {
                val url = URL("$BASE_URL/usuarios/verificar_identidade")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.connectTimeout = 4000

                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resposta)
                    val validada = json.optBoolean("identidade_validada", false)
                    Handler(Looper.getMainLooper()).post { aoResultado(validada) }
                } else {
                    Handler(Looper.getMainLooper()).post { aoResultado(false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post { aoResultado(false) }
            }
        }
    }

    // 🟢 9. VALIDAR IDENTIDADE INFORMANDO O CPF
    fun validarIdentidadeNuvem(
        cpfInformado: String,
        telefoneInformado: String,
        aoConcluir: (Boolean, String) -> Unit
    ) {
        thread {
            try {
                val url = URL("$BASE_URL/usuarios/validar_identidade")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.doOutput = true

                val json = JSONObject().apply {
                    put("cpf", cpfInformado)
                    put("telefone", telefoneInformado) // 🟢 Enviando o telefone junto
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()

                val respostaTexto = if (conexao.responseCode == 200) {
                    conexao.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conexao.errorStream?.bufferedReader()?.use { it.readText() } ?: "{}"
                }

                val resJson = JSONObject(respostaTexto)
                val sucesso = conexao.responseCode == 200
                val msg = resJson.optString(
                    "mensagem",
                    resJson.optString("erro", "Erro ao validar identidade.")
                )

                Handler(Looper.getMainLooper()).post { aoConcluir(sucesso, msg) }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    aoConcluir(
                        false,
                        "Falha de conexão com o servidor."
                    )
                }
            }
        }
    }
}