package com.example.transporte_interiorano

import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

// Estruturas de Dados
data class Carona(
    val id: Int = 0,
    val evento_nome: String = "",
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
    val bairro: String = "", val cidade: String = "", val estado: String = "", val cep: String = ""
)

// APENAS UMA DECLARAÇÃO DE PEDIDO
data class Pedido(
    val idReal: Int,
    val caronaId: Int,
    val passageiro: String,
    val passageiroCpf: String, // 🟢 ADICIONEI ESTA LINHA
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

    fun buscarCaronasDoServidor() {
        thread {
            try {
                val enderecoMagico = "https://transporte-interiorano-backend.onrender.com/caronas"
                val resposta = URL(enderecoMagico).readText()

                // 🆕 ADICIONANDO O RASTREADOR AQUI:
                android.util.Log.d("DEBUG_JSON", "Resposta do servidor: $resposta")

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
        motorista: String,
        motoristaCpf: String // 🆕 PARÂMETRO DO CPF AQUI
    ) {
        thread {
            try {
                // ⚠️ ESTAS LINHAS ESTAVAM FALTANDO NO SEU CÓDIGO!
                val conexao = URL("https://transporte-interiorano-backend.onrender.com/caronas").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true

                // O JSON completo
                val json = """{
                    "evento_nome": "$nomeEvento",
                    "cidade_origem": "$cidadeOrigem",
                    "endereco_origem": "$enderecoOrigem",
                    "cidade_destino": "$cidadeDestino",
                    "endereco_destino": "$enderecoDestino",
                    "horario": "$horario",
                    "vagas": "$vagas",
                    "motorista": "$motorista",
                    "motorista_cpf": "$motoristaCpf" 
                }"""

                // Agora o "conexao" existe e o erro vai sumir!
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

    fun fazerSolicitacao(carona: Carona, nomePassageiro: String, cpfPassageiro: String) { // 🟢 Adicione o CPF aqui
        todosOsPedidos.add(
            Pedido(
                idReal = 0,
                caronaId = carona.id,
                passageiro = nomePassageiro,
                passageiroCpf = cpfPassageiro, // 🟢 Adicionei esta linha
                status = "Pendente",
                evento_nome = "",
                cidade_origem = "",
                cidade_destino = "",
                horario = ""
            )
        )
        thread {
            try {
                val conexao =
                    URL("https://transporte-interiorano-backend.onrender.com/solicitacoes").openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true
                val json = """{"carona_id": ${carona.id}, "passageiro": "$nomePassageiro", "passageiro_cpf": "$cpfPassageiro"}"""
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

    fun finalizarSolicitacaoNuvem(solicitacaoId: Int, motorista: String, passageiroCpf: String, caronaId: Int) {
        thread {
            try {
                val url = URL("https://transporte-interiorano-backend.onrender.com/finalizar_solicitacao")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true

                // O servidor espera um JSON com esses campos
                val json = JSONObject()
                json.put("solicitacao_id", solicitacaoId)
                json.put("motorista", motorista)
                json.put("passageiro_cpf", passageiroCpf)
                json.put("carona_id", caronaId)

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()

                // O servidor responde 200 para esta rota específica
                if (conexao.responseCode == 200) {
                    android.util.Log.d("DEBUG_FINALIZAR", "Solicitação finalizada com sucesso! Atualizando...")
                    buscarSolicitacoesDoServidor()
                }

            } catch (e: Exception) {
                e.printStackTrace()
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

                // 🟢 FILTRO DE OURO: Só adicionamos na lista do App o que NÃO estiver finalizado
                if (!status.equals("Finalizado", ignoreCase = true)) {
                    novaLista.add(
                        Pedido(
                            idReal = item.getInt("id"),
                            caronaId = item.getInt("carona_id"),
                            passageiro = item.getString("passageiro"),
                            passageiroCpf = item.optString("passageiro_cpf", ""), // 🟢 ADICIONEi ESTA LINHA AQUI
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

    fun buscarMétricasDoUsuario(email: String, aoTerminar: (Int, Int) -> Unit) {
        thread {
            try {
                // Usamos o e-mail que é único e não tem problemas com espaços
                val emailSeguro = java.net.URLEncoder.encode(email, "UTF-8")
                android.util.Log.d("DEBUG_METRICAS", "Buscando métricas para o e-mail: $email")

                // A URL agora aponta para a nova rota que criamos no app.py
                val url = URL("https://transporte-interiorano-backend.onrender.com/usuarios_por_email/$emailSeguro")
                val resposta = url.readText()
                android.util.Log.d("DEBUG_METRICAS", "Resposta do servidor: $resposta")

                val json = JSONObject(resposta)
                val corridas = json.getInt("corridas_realizadas")
                val passageiros = json.getInt("passageiros_conduzidos")

                aoTerminar(corridas, passageiros)
            } catch (e: Exception) {
                android.util.Log.e("DEBUG_METRICAS", "Erro ao buscar métricas: ${e.message}")
                aoTerminar(0, 0)
            }
        }
    }

    fun buscarMétricasDoUsuarioPorNome(nomeMotorista: String, aoTerminar: (Int, Int) -> Unit) {
        thread {
            try {
                val nomeSeguro = java.net.URLEncoder.encode(nomeMotorista, "UTF-8")
                // Você precisará adicionar esta rota no seu app.py (veja abaixo)
                val url = URL("https://transporte-interiorano-backend.onrender.com/usuarios_por_nome/$nomeSeguro")
                val resposta = url.readText()
                val json = JSONObject(resposta)
                aoTerminar(json.getInt("corridas_realizadas"), json.getInt("passageiros_conduzidos"))
            } catch (e: Exception) {
                aoTerminar(0, 0)
            }
        }
    }

    fun buscarMétricasPorCpf(cpf: String, aoTerminar: (Int, Int) -> Unit) {
        thread {
            try {
                val url = URL("https://transporte-interiorano-backend.onrender.com/usuarios_por_cpf/$cpf")
                val resposta = url.readText()
                val json = JSONObject(resposta)
                aoTerminar(json.getInt("corridas_realizadas"), json.getInt("passageiros_conduzidos"))
            } catch (e: Exception) {
                aoTerminar(0, 0)
            }
        }
    }

    fun buscarHistoricoPassageiroPorCpf(cpf: String, aoReceber: (List<Pedido>) -> Unit) {
        thread {
            try {
                // Nota: O CPF é seguro para passar na URL.
                // O uso de URLEncoder é boa prática mesmo para CPF.
                val encodedCpf = java.net.URLEncoder.encode(cpf, "UTF-8")
                val resposta = URL("https://transporte-interiorano-backend.onrender.com/historico_cpf/$encodedCpf").readText()

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
                // Retorna a lista para a sua tela de Histórico
                aoReceber(listaHistorico)
            } catch (e: Exception) {
                e.printStackTrace()
                aoReceber(emptyList()) // Retorna lista vazia em caso de erro
            }
        }
    }

    fun buscarHistoricoMotoristaPorCpf(cpf: String, aoReceber: (List<Pedido>) -> Unit) {
        thread {
            try {
                val encodedCpf = java.net.URLEncoder.encode(cpf, "UTF-8")
                val resposta = URL("https://transporte-interiorano-backend.onrender.com/historico_motorista_cpf/$encodedCpf").readText()

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
                            horario = item.optString("horario", "")
                        )
                    )
                }
                aoReceber(listaHistorico)
            } catch (e: Exception) {
                e.printStackTrace()
                aoReceber(emptyList())
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

    fun listarNomesNoBanco() {
        thread {
            try {
                // Rota que busca todos os usuários para você investigar
                val url = URL("https://transporte-interiorano-backend.onrender.com/caronas")
                val resposta = url.readText()
                android.util.Log.d("DEBUG_BANCO", "Lista de dados brutos: $resposta")
            } catch (e: Exception) {
                android.util.Log.e("DEBUG_BANCO", "Erro ao listar: ${e.message}")
            }
        }
    }

    fun atualizarLocalizacaoMotorista(cpf: String, nome: String, lat: Double, lon: Double, status: String) {
        thread {
            try {
                val url = URL("https://transporte-interiorano-backend.onrender.com/atualizar_localizacao")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.doOutput = true

                // O JSON que o servidor espera
                val json = """{"cpf": "$cpf", "nome": "$nome", "latitude": $lat, "longitude": $lon, "status": "$status"}"""

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json)
                escritor.flush()

                android.util.Log.d("GPS_DEBUG", "Localização enviada: ${conexao.responseCode}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}