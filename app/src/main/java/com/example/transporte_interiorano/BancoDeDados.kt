package com.example.transporte_interiorano

import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

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

    fun buscarCaronasDoServidor() {
        thread {
            try {
                val resposta = URL("https://transporte-interiorano-backend.onrender.com/caronas").readText()
                val jsonArray = JSONArray(resposta)
                val novaLista = mutableListOf<Carona>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    novaLista.add(
                        Carona(
                            id = item.getInt("id"),
                            evento_nome = item.optString("evento_nome", ""),
                            cidade_origem = item.optString("cidade_origem", ""),
                            endereco_origem = item.optString("origem", ""),
                            cidade_destino = item.optString("cidade_destino", ""),
                            endereco_destino = item.optString("destino", ""),
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
                caronas.clear()
                caronas.addAll(novaLista)
            } catch (erro: Exception) { erro.printStackTrace() }
        }
    }

    fun enviarCaronaParaServidor(nomeEvento: String, cidadeOrigem: String, enderecoOrigem: String, cidadeDestino: String, enderecoDestino: String, horario: String, vagas: String, motorista: String, motoristaCpf: String) {
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/caronas").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true
                val json = """{"evento_nome": "$nomeEvento", "cidade_origem": "$cidadeOrigem", "endereco_origem": "$enderecoOrigem", "cidade_destino": "$cidadeDestino", "endereco_destino": "$enderecoDestino", "horario": "$horario", "vagas": "$vagas", "motorista": "$motorista", "motorista_cpf": "$motoristaCpf"}"""
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()
                if (conexao.responseCode == 201) buscarCaronasDoServidor()
            } catch (erro: Exception) { erro.printStackTrace() }
        }
    }

    fun fazerSolicitacao(carona: Carona, nomePassageiro: String, cpfPassageiro: String) {
        todosOsPedidos.add(Pedido(0, carona.id, nomePassageiro, cpfPassageiro, "Pendente"))
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/solicitacoes").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true
                val json = """{"carona_id": ${carona.id}, "passageiro": "$nomePassageiro", "passageiro_cpf": "$cpfPassageiro"}"""
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()
                buscarSolicitacoesDoServidor()
            } catch (erro: Exception) {}
        }
    }

    fun cancelarPedidoPassageiro(pedidoIdReal: Int) {
        todosOsPedidos.removeAll { it.idReal == pedidoIdReal }
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/solicitacoes/$pedidoIdReal").openConnection() as HttpURLConnection
                conexao.requestMethod = "DELETE"
                if (conexao.responseCode == 200) {
                    buscarSolicitacoesDoServidor()
                    buscarCaronasDoServidor()
                }
            } catch (erro: Exception) {}
        }
    }

    fun responderPedidoMotorista(pedidoIdReal: Int, statusDecidido: String) {
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/solicitacoes/$pedidoIdReal").openConnection() as HttpURLConnection
                conexao.requestMethod = "PUT"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true
                val json = """{"status": "$statusDecidido"}"""
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()
                buscarSolicitacoesDoServidor()
            } catch (erro: Exception) {}
        }
    }

    fun excluirCaronaDoServidor(caronaId: Int) {
        caronas.removeAll { it.id == caronaId }
        todosOsPedidos.removeAll { it.caronaId == caronaId }
        temEventoAtivo = false
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/caronas/$caronaId").openConnection() as HttpURLConnection
                conexao.requestMethod = "DELETE"
                buscarCaronasDoServidor()
            } catch (erro: Exception) {}
        }
    }

    fun finalizarSolicitacaoNuvem(solicitacaoId: Int, motorista: String, passageiroCpf: String, caronaId: Int) {
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/finalizar_solicitacao").openConnection() as HttpURLConnection
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
                if (conexao.responseCode == 200) buscarSolicitacoesDoServidor()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun buscarSolicitacoesDoServidor() {
        try {
            val resposta = URL("https://transporte-interiorano-backend.onrender.com/solicitacoes").readText()
            val jsonArray = JSONArray(resposta)
            val novaLista = mutableListOf<Pedido>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val status = item.getString("status")
                if (!status.equals("Finalizado", ignoreCase = true)) {
                    novaLista.add(
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
            todosOsPedidos.clear()
            todosOsPedidos.addAll(novaLista)
        } catch (erro: Exception) { erro.printStackTrace() }
    }

    fun ligarRadar() {
        thread {
            while (true) {
                buscarCaronasDoServidor()
                buscarSolicitacoesDoServidor()
                Thread.sleep(5000)
            }
        }
    }

    fun fazerLoginNuvem(usuarioRecebido: String, senhaRecebida: String, aoTerminar: (Usuario?, String) -> Unit) {
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/login").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
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
                    aoTerminar(usuarioLogado, "")
                } else {
                    aoTerminar(null, "Negado")
                }
            } catch (erro: Exception) { aoTerminar(null, "Erro") }
        }
    }

    fun cadastrarUsuarioNuvem(nome: String, cpf: String, telefone: String, email: String, senha: String, veiculo: String, placa: String, vagas: String, rua: String, numero: String, complemento: String, bairro: String, cidade: String, estado: String, cep: String, username: String, aoTerminar: (Boolean, String) -> Unit) {
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/usuarios").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
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
            } catch (erro: Exception) { aoTerminar(false, "Falha") }
        }
    }

    fun verificarDisponibilidadeUsuario(username: String, aoResultar: (Boolean, List<String>) -> Unit) {
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/verificar_usuario/${username.trim()}").openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                if (conexao.responseCode == 200) {
                    val res = JSONObject(conexao.inputStream.bufferedReader().readText())
                    val disp = res.getBoolean("disponivel")
                    val arr = res.getJSONArray("sugestoes")
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) { list.add(arr.getString(i)) }
                    aoResultar(disp, list)
                }
            } catch (e: Exception) { aoResultar(false, emptyList()) }
        }
    }

    fun verificarCpfExistente(cpf: String, aoDescobrir: (Boolean) -> Unit) {
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/verificar_cpf/$cpf").openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                if (conexao.responseCode == 200) {
                    val res = JSONObject(conexao.inputStream.bufferedReader().readText())
                    aoDescobrir(res.getBoolean("existe"))
                }
            } catch (erro: Exception) {}
        }
    }

    fun excluirUsuario(email: String) {
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/usuarios/${java.net.URLEncoder.encode(email, "UTF-8")}").openConnection() as HttpURLConnection
                conexao.requestMethod = "DELETE"
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.responseCode
            } catch (erro: Exception) {}
        }
    }

    fun buscarMétricasPorCpf(cpf: String, aoTerminar: (Int, Int) -> Unit) {
        thread {
            try {
                val resposta = URL("https://transporte-interiorano-backend.onrender.com/usuarios_por_cpf/$cpf").readText()
                val json = JSONObject(resposta)
                aoTerminar(json.getInt("corridas_realizadas"), json.getInt("passageiros_conduzidos"))
            } catch (e: Exception) { aoTerminar(0, 0) }
        }
    }

    // 🟢 REINSERIDA: Função de Histórico do Passageiro mapeada perfeitamente
    fun buscarHistoricoPassageiroPorCpf(cpf: String, aoReceber: (List<Pedido>) -> Unit) {
        thread {
            try {
                val resposta = URL("https://transporte-interiorano-backend.onrender.com/historico_cpf/${java.net.URLEncoder.encode(cpf, "UTF-8")}").readText()
                val jsonArray = JSONArray(resposta)
                val listaHistorico = mutableListOf<Pedido>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    listaHistorico.add(
                        Pedido(
                            idReal = item.getInt("id"), caronaId = item.getInt("carona_id"), passageiro = item.getString("passageiro"),
                            passageiroCpf = item.optString("passageiro_cpf", ""), status = item.getString("status"),
                            evento_nome = item.optString("evento_nome", ""), cidade_origem = item.optString("cidade_origem", ""),
                            cidade_destino = item.optString("cidade_destino", ""), horario = item.optString("horario", "")
                        )
                    )
                }
                aoReceber(listaHistorico)
            } catch (e: Exception) { aoReceber(emptyList()) }
        }
    }

    // 🟢 REINSERIDA E CORRIGIDA: Função de Histórico do Motorista que estava faltando!
    fun buscarHistoricoMotoristaPorCpf(cpf: String, aoReceber: (List<Pedido>) -> Unit) {
        thread {
            try {
                val resposta = URL("https://transporte-interiorano-backend.onrender.com/historico_motorista_cpf/${java.net.URLEncoder.encode(cpf, "UTF-8")}").readText()
                val jsonArray = JSONArray(resposta)
                val listaHistorico = mutableListOf<Pedido>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    listaHistorico.add(
                        Pedido(
                            idReal = item.getInt("id"), caronaId = item.getInt("carona_id"), passageiro = item.getString("passageiro"),
                            passageiroCpf = item.optString("passageiro_cpf", ""), status = item.getString("status"),
                            evento_nome = item.optString("evento_nome", ""), cidade_origem = item.optString("cidade_origem", ""),
                            cidade_destino = item.optString("cidade_destino", ""), horario = item.optString("horario", "")
                        )
                    )
                }
                aoReceber(listaHistorico)
            } catch (e: Exception) { aoReceber(emptyList()) }
        }
    }

    fun atualizarUsuarioNuvem(usuario: Usuario, aoTerminar: (Boolean) -> Unit) {
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/usuarios/${usuario.cpf}").openConnection() as HttpURLConnection
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
            } catch (erro: Exception) { aoTerminar(false) }
        }
    }

    fun solicitarCodigoRecuperacao(email: String, cpf: String, aoTerminar: (Boolean, String, String?) -> Unit) {
        val cpfLimpo = cpf.filter { it.isDigit() }
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/solicitar_codigo").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true
                val json = """{"email": "$email", "cpf": "$cpfLimpo"}"""
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()
                if (conexao.responseCode == 200) {
                    val res = JSONObject(conexao.inputStream.bufferedReader().readText())
                    aoTerminar(true, "Código enviado para o e-mail cadastrado!", res.optString("codigo_debug", null))
                } else { aoTerminar(false, "Dados incorretos ou não cadastrados.", null) }
            } catch (erro: Exception) { aoTerminar(false, "Falha na conexão.", null) }
        }
    }

    fun redefinirSenhaComCodigo(email: String, codigo: String, novaSenha: String, aoTerminar: (Boolean, String) -> Unit) {
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/validar_e_redefinir_senha").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true
                val json = """{"email": "$email", "codigo": "$codigo", "senha": "$novaSenha"}"""
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()
                aoTerminar(conexao.responseCode == 200, "")
            } catch (erro: Exception) { aoTerminar(false, "") }
        }
    }

    fun listarNomesNoBanco() {}
}