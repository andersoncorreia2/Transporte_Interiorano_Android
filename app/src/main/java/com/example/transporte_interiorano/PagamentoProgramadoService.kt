package com.example.transporte_interiorano

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object PagamentoProgramadoService {
    private const val BASE_URL = BancoDeDados.BASE_URL

    // 🟢 PASSAGEIRO: Solicita o código PIX copia e cola para reservar a vaga
    fun gerarPixTaxaReserva(caronaId: Int, tokenSessao: String, aoConcluir: (Boolean, String, String?) -> Unit) {
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

                if (conexao.responseCode == 200) {
                    val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                    val resJson = JSONObject(resposta)
                    val codigoPix = resJson.optString("codigo_pix_copia_cola")
                    Handler(Looper.getMainLooper()).post { aoConcluir(true, "Pix gerado com sucesso", codigoPix) }
                } else {
                    Handler(Looper.getMainLooper()).post { aoConcluir(false, "Vaga esgotada ou erro.", null) }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { aoConcluir(false, "Falha de rede.", null) }
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
}