package com.example.transporte_interiorano

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object PagamentoProgramadoService {
    private val BASE_URL = BancoDeDados.BASE_URL

    // 🟢 PASSAGEIRO: Solicita o Pix real de R$ 5,00 (Taxa de Reserva) com ID da solicitação
    fun gerarPixTaxaReserva(
        caronaId: Int,
        tokenSessao: String,
        aoConcluir: (Boolean, String, String?, String?, Int) -> Unit
    ) {
        thread {
            try {
                val url = URL("$BASE_URL/pagamentos/programado/gerar_taxa")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.doOutput = true

                val json = JSONObject().apply { put("carona_id", caronaId) }
                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()

                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val resJson = JSONObject(resposta)
                    val pixCopiaCola = resJson.optString("pix_copia_cola", "")
                    val qrCodeBase64 = resJson.optString("qr_code_base64", "")
                    val solicitacaoId = resJson.optInt("solicitacao_id", 0)

                    Handler(Looper.getMainLooper()).post {
                        aoConcluir(true, "Pix de reserva gerado com sucesso!", pixCopiaCola, qrCodeBase64, solicitacaoId)
                    }
                } else {
                    val textoErro = conexao.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    var msgErro = "Falha ao gerar taxa de reserva."
                    try {
                        if (textoErro.isNotEmpty()) {
                            msgErro = JSONObject(textoErro).optString("erro", msgErro)
                        }
                    } catch (e: Exception) {}

                    Handler(Looper.getMainLooper()).post {
                        aoConcluir(false, msgErro, null, null, 0)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    aoConcluir(false, "Falha de rede ao gerar Pix: ${e.message}", null, null, 0)
                }
            }
        }
    }

    // 🟢 Verifica permissão antes de solicitar carona
    fun verificarPermissaoSolicitacao(tokenSessao: String, aoConcluir: (Boolean, String) -> Unit) {
        thread {
            try {
                val url = URL("$BASE_URL/pagamentos/programado/verificar_permissao")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")

                if (conexao.responseCode == 200) {
                    val json = JSONObject(conexao.inputStream.bufferedReader().use { it.readText() })
                    val bloqueado = json.optBoolean("bloqueado", false)
                    val msg = json.optString("mensagem", "Bloqueado.")
                    Handler(Looper.getMainLooper()).post { aoConcluir(bloqueado, msg) }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { aoConcluir(true, "Erro ao verificar acesso.") }
            }
        }
    }

    // 🟢 PASSAGEIRO: Verifica se o Pix da taxa de reserva de R$ 5,00 foi pago
    fun verificarPagamentoTaxa(
        solicitacaoId: Int,
        tokenSessao: String,
        aoConcluir: (Boolean) -> Unit
    ) {
        thread {
            try {
                val url = URL("$BASE_URL/pagamentos/programado/verificar_pagamento_taxa/$solicitacaoId")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.connectTimeout = 4000

                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resposta)
                    val pago = json.optBoolean("pago", false)
                    Handler(Looper.getMainLooper()).post { aoConcluir(pago) }
                } else {
                    Handler(Looper.getMainLooper()).post { aoConcluir(false) }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { aoConcluir(false) }
            }
        }
    }

    // 🟢 PASSAGEIRO: Gera o link de pagamento integral (Pix ou Cartão)
    fun gerarCheckoutValorTotal(
        caronaId: Int,
        valorTotal: Double,
        tokenSessao: String,
        aoConcluir: (Boolean, String, String?) -> Unit
    ) {
        thread {
            try {
                val url = URL("$BASE_URL/pagamentos/programado/gerar_checkout")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.requestMethod = "POST"
                conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexao.setRequestProperty("Authorization", "Bearer $tokenSessao")
                conexao.doOutput = true

                val json = JSONObject().apply {
                    put("carona_id", caronaId)
                    put("valor_total", valorTotal)
                }

                val escritor = OutputStreamWriter(conexao.outputStream)
                escritor.write(json.toString())
                escritor.flush()
                escritor.close()

                if (conexao.responseCode == 200 || conexao.responseCode == 201) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val resJson = JSONObject(resposta)
                    val initPoint = resJson.optString("init_point", "")

                    Handler(Looper.getMainLooper()).post {
                        aoConcluir(true, "Checkout gerado com sucesso!", initPoint)
                    }
                } else {
                    val textoErro = conexao.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    var msgErro = "Falha ao gerar checkout de pagamento."
                    try {
                        if (textoErro.isNotEmpty()) {
                            msgErro = JSONObject(textoErro).optString("erro", msgErro)
                        }
                    } catch (e: Exception) {}

                    Handler(Looper.getMainLooper()).post {
                        aoConcluir(false, msgErro, null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    aoConcluir(false, "Falha de rede ao gerar checkout: ${e.message}", null)
                }
            }
        }
    }
}