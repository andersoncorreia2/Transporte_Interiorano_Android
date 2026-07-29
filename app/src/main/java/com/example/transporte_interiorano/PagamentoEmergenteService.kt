package com.example.transporte_interiorano

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object PagamentoEmergenteService {
    private const val BASE_URL = BancoDeDados.BASE_URL

    // 🟢 PASSAGEIRO: Verifica se tem corridas anteriores não pagas (Com trava anti-401)
    fun verificarDebitoPassageiro(tokenSessao: String, aoConcluir: (Boolean, String, JSONObject?) -> Unit) {
        // 🛡️ TRAVA DE SEGURANÇA: Se o token estiver vazio, nem gasta rede gerando erro 401 no servidor
        if (tokenSessao.isEmpty()) {
            aoConcluir(false, "Token ausente", null)
            return
        }

        thread {
            try {
                val url = URL("$BASE_URL/pagamentos/emergente/verificar_debito")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.connectTimeout = 5000

                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resposta)
                    val bloqueado = json.optBoolean("bloqueado", false)
                    val msg = json.optString("mensagem", "")
                    val detalhes = json.optJSONObject("detalhes")

                    Handler(Looper.getMainLooper()).post { aoConcluir(bloqueado, msg, detalhes) }
                } else {
                    val textoErro = conexao.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    val msg = try { JSONObject(textoErro).optString("erro", "Erro ao consultar financeiro.") } catch (e: Exception) { "Erro ao consultar financeiro." }
                    Handler(Looper.getMainLooper()).post { aoConcluir(false, msg, null) }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { aoConcluir(false, "Falha de rede.", null) }
            }
        }
    }

    // 🟢 PASSAGEIRO: Gera a cobrança Pix real de R$ 0,01 via Mercado Pago
    fun gerarPixDebitoPassageiro(
        corridaId: Int,
        tokenSessao: String,
        aoConcluir: (Boolean, String, String?, String?, Double?) -> Unit
    ) {
        thread {
            try {
                val url = URL("$BASE_URL/pagamentos/emergente/gerar_pix_debito")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.connectTimeout = 8000
                conexao.readTimeout = 8000
                conexao.doOutput = true

                val jsonPayload = JSONObject().apply {
                    if (corridaId > 0) {
                        put("corrida_id", corridaId)
                    }
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(jsonPayload.toString())
                escritor.flush()
                escritor.close()

                val codigoResposta = conexao.responseCode

                if (codigoResposta == 200 || codigoResposta == 201) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val res = JSONObject(resposta)
                    val pixCopiaCola = res.optString("pix_copia_cola", "")
                    val qrCodeBase64 = res.optString("qr_code_base64", "")
                    val valorCobrado = res.optDouble("valor_cobrado", 0.01)

                    Handler(Looper.getMainLooper()).post {
                        aoConcluir(true, "Pix gerado com sucesso!", pixCopiaCola, qrCodeBase64, valorCobrado)
                    }
                } else {
                    // 🟢 Extrai a mensagem real enviada pelo servidor Python
                    val textoErro = conexao.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    var msgErro = "Falha ao gerar Pix (Código $codigoResposta)"
                    try {
                        if (textoErro.isNotEmpty()) {
                            val jsonErro = JSONObject(textoErro)
                            msgErro = jsonErro.optString("detalhe_tecnico").ifEmpty { jsonErro.optString("erro", msgErro) }
                        }
                    } catch (e: Exception) {}

                    Handler(Looper.getMainLooper()).post {
                        aoConcluir(false, msgErro, null, null, null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    aoConcluir(false, "Erro de conexão ao gerar Pix: ${e.message}", null, null, null)
                }
            }
        }
    }

    // 🟢 PASSAGEIRO: Paga a dívida antiga para desbloquear o app (Quitação manual)
    fun quitarDebitoPassageiro(corridaId: Int, tokenSessao: String, aoConcluir: (Boolean, String) -> Unit) {
        thread {
            try {
                val url = URL("$BASE_URL/pagamentos/emergente/quitar_debito/$corridaId")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")

                if (conexao.responseCode == 200) {
                    val msg = JSONObject(conexao.inputStream.bufferedReader().use { it.readText() }).optString("mensagem", "Pago com sucesso!")
                    Handler(Looper.getMainLooper()).post { aoConcluir(true, msg) }
                } else {
                    Handler(Looper.getMainLooper()).post { aoConcluir(false, "Falha ao quitar.") }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { aoConcluir(false, "Erro de conexão.") }
            }
        }
    }

    // 🟢 MOTORISTA: Verifica se a mensalidade do app está em dia para ligar o radar
    fun verificarAssinaturaMotorista(tokenSessao: String, aoConcluir: (Boolean, String) -> Unit) {
        thread {
            try {
                val url = URL("$BASE_URL/pagamentos/emergente/verificar_assinatura")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")

                if (conexao.responseCode == 200) {
                    val json = JSONObject(conexao.inputStream.bufferedReader().use { it.readText() })
                    val ativo = json.optBoolean("assinatura_ativa", false)
                    val msg = json.optString("mensagem", "")
                    Handler(Looper.getMainLooper()).post { aoConcluir(ativo, msg) }
                } else {
                    Handler(Looper.getMainLooper()).post { aoConcluir(false, "Erro ao verificar assinatura.") }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { aoConcluir(false, "Erro de rede.") }
            }
        }
    }

    // 🟢 PASSAGEIRO/MOTORISTA: Atualiza a modalidade ativa no backend (Programada / Emergencial)
    fun alterarModalidadeUsuario(modalidade: String, tokenSessao: String, aoConcluir: (Boolean) -> Unit) {
        thread {
            try {
                val url = URL("$BASE_URL/usuarios/alterar_modalidade")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.connectTimeout = 5000
                conexao.readTimeout = 5000
                conexao.doOutput = true

                val jsonPayload = JSONObject().apply {
                    put("modalidade", modalidade)
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(jsonPayload.toString())
                escritor.flush()
                escritor.close()

                val codigoResposta = conexao.responseCode
                val sucesso = (codigoResposta == 200 || codigoResposta == 201)

                Handler(Looper.getMainLooper()).post {
                    aoConcluir(sucesso)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    aoConcluir(false)
                }
            }
        }
    }
}