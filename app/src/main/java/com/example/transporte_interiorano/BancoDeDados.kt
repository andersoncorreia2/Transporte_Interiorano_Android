package com.example.transporte_interiorano

import android.R
import androidx.compose.runtime.mutableStateListOf
import java.net.URL
import kotlin.concurrent.thread
import java.net.HttpURLConnection
import java.io.OutputStreamWriter
import org.json.JSONArray
import org.json.JSONObject

// Estruturas de Dados
data class Carona(val id: Int = 0, val cidadeOrigem: String = "", val origem: String, val cidadeDestino: String = "", val destino: String, val horario: String, val vagas: String, val motorista: String)

data class Usuario(
    val nome: String, val cpf: String, val email: String, val telefone: String,
    val veiculo: String = "", val placa: String = "", val senha: String = "", val vagas: String = "0",
    val rua: String = "", val numero: String = "", val complemento: String = "",
    val bairro: String = "", val cidade: String = "", val estado: String = "", val cep: String = ""
)

// APENAS UMA DECLARAÇÃO DE PEDIDO
data class Pedido(val idReal: Int, val caronaId: Int, val passageiro: String, val status: String)

object BancoDeDados {
    var caronas = mutableStateListOf<Carona>()
    var todosOsPedidos = mutableStateListOf<Pedido>()
    var temEventoAtivo: Boolean = false

    fun buscarCaronasDoServidor() {
        thread {
            try {
                val enderecoMagico = "https://transporte-interiorano-backend.onrender.com/caronas"
                val resposta = URL(enderecoMagico).readText()
                val jsonArray = JSONArray(resposta)

                val novaLista = mutableListOf<Carona>()

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    novaLista.add(
                        Carona(
                            id = item.getInt("id"),
                            cidadeOrigem = item.optString("cidade_origem", ""),
                            origem = item.getString("origem"),
                            cidadeDestino = item.optString("cidade_destino", ""),
                            destino = item.getString("destino"),
                            horario = item.getString("horario"),
                            vagas = item.getString("vagas"),
                            motorista = item.getString("motorista")
                        )
                    )
                }

                caronas.clear()
                caronas.addAll(novaLista)

            } catch (erro: Exception) {
                println("❌ ERRO CARONAS NUVEM: ${erro.message}")
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
        motorista: String
    ) {
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/caronas").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true

                // Este JSON agora bate exatamente com as colunas que você criou no app.py
                val json = """{
                    "evento_nome": "$nomeEvento",
                    "cidade_origem": "$cidadeOrigem",
                    "endereco_origem": "$enderecoOrigem",
                    "cidade_destino": "$cidadeDestino",
                    "endereco_destino": "$enderecoDestino",
                    "horario": "$horario",
                    "vagas": "$vagas",
                    "motorista": "$motorista"
                }"""

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()

                // O servidor responde 201 (criado), então forçamos a atualização da lista
                if (conexao.responseCode == 201) {
                    buscarCaronasDoServidor()
                }
            } catch (erro: Exception) {
                println("❌ Erro ao publicar carona: ${erro.message}")
            }
        }
    }

    fun fazerSolicitacao(carona: Carona, nomePassageiro: String) {
        todosOsPedidos.add(
            Pedido(
                idReal = 0,
                caronaId = carona.id,
                passageiro = nomePassageiro,
                status = "Pendente"
            )
        )
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/solicitacoes").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true
                val json = """{"carona_id": ${carona.id}, "passageiro": "$nomePassageiro"}"""
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()
                val code = conexao.responseCode
                buscarSolicitacoesDoServidor()
            } catch (erro: Exception) {
            }
        }
    }

    fun cancelarPedidoPassageiro(pedidoIdReal: Int) {
        val index = todosOsPedidos.indexOfFirst { it.idReal == pedidoIdReal }
        if (index != -1) {
            todosOsPedidos.removeAt(index)
        }

        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/solicitacoes/$pedidoIdReal").openConnection() as HttpURLConnection
                conexao.requestMethod = "DELETE"
                val code = conexao.responseCode
                println("🗑️ Pedido cancelado pelo passageiro! Servidor respondeu: $code")

                buscarSolicitacoesDoServidor()
                buscarCaronasDoServidor()
            } catch (erro: Exception) {
                println("❌ Erro ao cancelar pedido: ${erro.message}")
            }
        }
    }

    fun responderPedidoMotorista(pedidoIdReal: Int, statusDecidido: String) {
        val index = todosOsPedidos.indexOfFirst { it.idReal == pedidoIdReal }
        if (index != -1) {
            val antigo = todosOsPedidos[index]
            todosOsPedidos[index] = antigo.copy(status = statusDecidido)
        }

        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/solicitacoes/$pedidoIdReal").openConnection() as HttpURLConnection
                conexao.requestMethod = "PUT"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true
                val json = """{"status": "$statusDecidido"}"""
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()
                val code = conexao.responseCode
                buscarSolicitacoesDoServidor()
            } catch (erro: Exception) {
            }
        }
    }

    fun excluirCaronaDoServidor(caronaId: Int) {
        caronas.removeAll { it.id == caronaId }
        todosOsPedidos.removeAll { it.caronaId == caronaId }
        temEventoAtivo = false
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/caronas/$caronaId").openConnection() as HttpURLConnection
                conexao.requestMethod = "DELETE"
                val code = conexao.responseCode
                buscarCaronasDoServidor()
            } catch (erro: Exception) {
            }
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

                // Lógica de tratamento do status
                if (status == "Expirado") {
                    // Aqui você pode adicionar um log para debug ou
                    // tomar alguma ação específica no app
                    println("⚠️ Aviso: Pedido ${item.getInt("id")} expirou!")
                }

                novaLista.add(
                    Pedido(
                        idReal = item.getInt("id"),
                        caronaId = item.getInt("carona_id"),
                        passageiro = item.getString("passageiro"),
                        status = status // O status processado entra aqui
                    )
                )
            }
            todosOsPedidos.clear()
            todosOsPedidos.addAll(novaLista)
        } catch (erro: Exception) {
            println("❌ Radar falhou: ${erro.message}")
        }
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

    fun fazerLoginNuvem(
        emailRecebido: String,
        senhaRecebida: String,
        aoTerminar: (Usuario?, String) -> Unit
    ) {
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/login").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val json = """{"email": "$emailRecebido", "senha": "$senhaRecebida"}"""
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()

                // 🆕 ADICIONE ESTA LINHA PARA VER O CÓDIGO NO LOGCAT
                //android.util.Log.d("DEBUG_LOGIN", "Código de resposta: ${conexao.responseCode}")

                if (conexao.responseCode == 200) {
                    val res = JSONObject(conexao.inputStream.bufferedReader().readText())

                    // ALTERAÇÃO: O aplicativo agora lê e guarda os novos endereços do servidor quando o usuário faz login!
                    val usuarioLogado = Usuario(
                        nome = res.getString("nome"),
                        cpf = res.getString("cpf"),
                        email = res.getString("email"),
                        telefone = res.getString("telefone"),
                        veiculo = res.optString("veiculo", ""),
                        placa = res.optString("placa", ""),
                        vagas = res.optString("vagas", ""), // <--- TEM DE SER "vagas" (plural)
                        senha = senhaRecebida,
                        rua = res.optString("rua", ""),
                        numero = res.optString("numero", ""),
                        complemento = res.optString("complemento", ""),
                        bairro = res.optString("bairro", ""),
                        cidade = res.optString("cidade", ""),
                        estado = res.optString("estado", ""),
                        cep = res.optString("cep", "")
                    )

                    aoTerminar(usuarioLogado, "")
                } else {
                    aoTerminar(null, "Negado")
                }
            } catch (erro: Exception) {
                aoTerminar(null, "Erro")
            }
        }
    }

    // ALTERAÇÃO: A função de cadastro agora recebe os 7 campos novos!
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
        aoTerminar: (Boolean, String) -> Unit
    ) {
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/usuarios").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")

                // INCLUSÃO: Empacotando o super dossiê de endereço no formato JSON!
                val json = """{
                    "nome": "$nome", "cpf": "$cpf", "telefone": "$telefone", "email": "$email", 
                    "senha": "$senha", "veiculo": "$veiculo", "placa": "$placa", "vagas": "$vagas", 
                    "rua": "$rua", "numero": "$numero", "complemento": "$complemento", 
                    "bairro": "$bairro", "cidade": "$cidade", "estado": "$estado", "cep": "$cep"
                }"""

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()
                when (conexao.responseCode) {
                    201 -> aoTerminar(true, "")
                    400 -> aoTerminar(false, "Já existe")
                    else -> aoTerminar(false, "Erro")
                }
            } catch (erro: Exception) {
                aoTerminar(false, "Falha")
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
                val emailSeguroParaInternet = java.net.URLEncoder.encode(email, "UTF-8")
                val enderecoMagico =
                    URL("https://transporte-interiorano-backend.onrender.com/usuarios/$emailSeguroParaInternet")
                val conexao = enderecoMagico.openConnection() as HttpURLConnection
                conexao.requestMethod = "DELETE"
                val codigoResposta = conexao.responseCode
                println("🗑️ ORDEM DE EXCLUSÃO ENVIADA! Servidor respondeu: $codigoResposta")
            } catch (erro: Exception) {
                println("❌ ERRO AO EXCLUIR CONTA NUVEM: ${erro.message}")
            }
        }
    }

    // INCLUSÃO: Função para atualizar os dados do utilizador na nuvem
    fun atualizarUsuarioNuvem(usuario: Usuario, aoTerminar: (Boolean) -> Unit) {
        thread {
            try {
                // A atualização usa o e-mail como chave (que não pode ser alterado)
                val emailSeguro = java.net.URLEncoder.encode(usuario.email, "UTF-8")
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/usuarios/$emailSeguro").openConnection() as HttpURLConnection
                conexao.requestMethod = "PUT"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true

                // Dentro da função atualizarUsuarioNuvem no BancoDeDados.kt:
                val json = """{
                    "nome": "${usuario.nome}", "telefone": "${usuario.telefone}", 
                    "veiculo": "${usuario.veiculo}", "placa": "${usuario.placa}", "vagas": "${usuario.vagas}",
                    "rua": "${usuario.rua}", "numero": "${usuario.numero}", "complemento": "${usuario.complemento}",
                    "bairro": "${usuario.bairro}", "cidade": "${usuario.cidade}", "estado": "${usuario.estado}", "cep": "${usuario.cep}"
                }"""

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()

                if (conexao.responseCode == 200) {
                    aoTerminar(true)
                } else {
                    aoTerminar(false)
                }
            } catch (erro: Exception) {
                aoTerminar(false)
            }
        }
    }
    fun recuperarSenhaNuvem(email: String, cpf: String, novaSenha: String, aoTerminar: (Boolean, String) -> Unit) {
        thread {
            try {
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/recuperar_senha").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true

                val json = """{"email": "$email", "cpf": "$cpf", "senha": "$novaSenha"}"""
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()

                if (conexao.responseCode == 200) {
                    aoTerminar(true, "Senha alterada com sucesso!")
                } else {
                    aoTerminar(false, "Erro: E-mail ou CPF incorretos.")
                }
            } catch (erro: Exception) {
                aoTerminar(false, "Falha na conexão.")
            }
        }
    }
}