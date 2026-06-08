package com.example.transporte_interiorano.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.BancoDeDados
import com.example.transporte_interiorano.ui.theme.*

// --- MÁSCARAS ---
class CepVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 4) out += "-"
        }
        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 4) offset else if (offset <= 8) offset + 1 else 9
            override fun transformedToOriginal(offset: Int): Int =
                if (offset <= 5) offset else if (offset <= 9) offset - 1 else 8
        }
        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}

class CpfVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 11) text.text.substring(0..10) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 2 || i == 5) out += "."
            if (i == 8) out += "-"
        }
        val cpfOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 2) offset else if (offset <= 5) offset + 1 else if (offset <= 8) offset + 2 else if (offset <= 11) offset + 3 else 14
            override fun transformedToOriginal(offset: Int): Int =
                if (offset <= 3) offset else if (offset <= 7) offset - 1 else if (offset <= 11) offset - 2 else if (offset <= 14) offset - 3 else 11
        }
        return TransformedText(AnnotatedString(out), cpfOffsetTranslator)
    }
}

class TelefoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 11) text.text.substring(0..10) else text.text
        var out = ""
        for (i in trimmed.indices) {
            if (i == 0) out += "("
            out += trimmed[i]
            if (i == 1) out += ") "
            if (i == 6) out += "-"
        }
        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset == 0) 0 else if (offset == 1) 2 else if (offset == 2) 5 else if (offset <= 6) offset + 3 else if (offset <= 11) offset + 4 else 15
            override fun transformedToOriginal(offset: Int): Int =
                if (offset <= 1) 0 else if (offset <= 3) 1 else if (offset <= 5) 2 else if (offset <= 10) offset - 3 else if (offset <= 15) offset - 4 else 11
        }
        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}

class PlacaVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text.take(7)
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 2 && trimmed.length > 3) out += "-"
        }
        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 3) offset else if (offset <= 7) offset + 1 else 8
            override fun transformedToOriginal(offset: Int): Int =
                if (offset <= 3) offset else if (offset <= 8) offset - 1 else 7
        }
        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroScreen(
    aoConcluirCadastro: (String, String, String, String, String, String, String, String, String, String, String, String, String, String, String) -> Unit,
    aoClicarFechar: () -> Unit,
    mensagemErro: String = ""
) {
    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var veiculo by remember { mutableStateOf("") }
    var placa by remember { mutableStateOf("") }
    var vagas by remember { mutableStateOf("") }

    var cep by remember { mutableStateOf("") }
    var rua by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var complemento by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("") }

    var ofertarCarona by remember { mutableStateOf(false) }
    var senhaVisivel by remember { mutableStateOf(false) }
    var cpfJaExiste by remember { mutableStateOf(false) }

    // Lista de estados para o dropdown
    var ufExpandido by remember { mutableStateOf(false) }
    val estadosBrasil = listOf("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO")

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transporte Interiorano", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = aoClicarFechar) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrincipal)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F7))
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text("Criar Nova Conta", fontSize = 24.sp, color = AzulPrincipal, fontWeight = FontWeight.Bold)

            if (mensagemErro.isNotEmpty()) {
                val corAlerta = if (mensagemErro.contains("Conectando")) AzulPrincipal else VermelhoErro
                Text(mensagemErro, color = corAlerta, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Text("DADOS PESSOAIS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            OutlinedTextField(
                value = nome, onValueChange = { nome = it }, label = { Text("Nome completo") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = cpf,
                onValueChange = { novoTexto ->
                    val soNumeros = novoTexto.filter { it.isDigit() }.take(11)
                    cpf = soNumeros
                    if (soNumeros.length < 11) cpfJaExiste = false
                    else if (soNumeros.length == 11) BancoDeDados.verificarCpfExistente(soNumeros) { existe -> cpfJaExiste = existe }
                },
                label = { Text("CPF") }, modifier = Modifier.fillMaxWidth(), isError = cpfJaExiste,
                supportingText = { if (cpfJaExiste) Text("⚠️ Este CPF já está cadastrado no sistema!", color = VermelhoErro, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                visualTransformation = CpfVisualTransformation()
            )

            OutlinedTextField(
                value = telefone, onValueChange = { telefone = it.filter { char -> char.isDigit() }.take(11) }, label = { Text("Telefone / WhatsApp") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                visualTransformation = TelefoneVisualTransformation()
            )

            Text("ENDEREÇO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))

            // 1ª Linha: Rua e Nº
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rua, onValueChange = { rua = it }, label = { Text("Rua/Avenida") }, modifier = Modifier.weight(2f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = numero, onValueChange = { numero = it }, label = { Text("Nº") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )
            }

            // 2ª Linha: Complemento e Bairro
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = complemento, onValueChange = { complemento = it },
                    label = { Text("Compl. (Opcional)") }, // Abreviado!
                    modifier = Modifier.weight(1f),
                    singleLine = true, // Impede que o texto pule linha
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = bairro, onValueChange = { bairro = it },
                    label = { Text("Bairro") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }

            // 3ª Linha: Cidade e UF (Dropdown)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = cidade, onValueChange = { cidade = it },
                    label = { Text("Cidade") },
                    // Diminuímos um pouco o peso da cidade para dar mais espaço para o UF respirar
                    modifier = Modifier.weight(1.3f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                ExposedDropdownMenuBox(
                    expanded = ufExpandido,
                    onExpandedChange = { ufExpandido = !ufExpandido },
                    modifier = Modifier.weight(1f) // Mais espaço para a seta caber do lado do texto
                ) {
                    OutlinedTextField(
                        value = estado,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true, // 🚨 O COMANDO MÁGICO QUE IMPEDE O TEXTO DE FICAR VERTICAL!
                        label = { Text("UF") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ufExpandido) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = ufExpandido,
                        onDismissRequest = { ufExpandido = false }
                    ) {
                        estadosBrasil.forEach { uf ->
                            DropdownMenuItem(
                                text = { Text(uf) },
                                onClick = {
                                    estado = uf
                                    ufExpandido = false
                                }
                            )
                        }
                    }
                }
            }

            // 4ª Linha: CEP com máscara
            OutlinedTextField(
                value = cep, onValueChange = { cep = it.filter { char -> char.isDigit() }.take(8) }, label = { Text("CEP") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                visualTransformation = CepVisualTransformation()
            )

            Text("DADOS DA CONTA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                trailingIcon = {
                    val image = if (senhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { senhaVisivel = !senhaVisivel }) { Icon(imageVector = image, contentDescription = "Mostrar") }
                }
            )

            Surface(color = Color.White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (ofertarCarona) "Ofertar Corridas (Ativado)" else "Quero ofertar Corridas", color = if (ofertarCarona) AzulPrincipal else Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(checked = ofertarCarona, onCheckedChange = { ofertarCarona = it })
                    }

                    if (ofertarCarona) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = veiculo, onValueChange = { veiculo = it }, label = { Text("Modelo do Veículo") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Placa ficou com weight 1.5f para ser mais larga!
                            OutlinedTextField(
                                value = placa, onValueChange = { placa = it.uppercase().take(7) }, label = { Text("Placa") }, modifier = Modifier.weight(1.5f),
                                visualTransformation = PlacaVisualTransformation(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            OutlinedTextField(value = vagas, onValueChange = { vagas = it }, label = { Text("Vagas") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (!cpfJaExiste) {
                            // Lógica de limpeza antes de enviar
                            val veiculoFinal = if (ofertarCarona) veiculo else ""
                            val placaFinal = if (ofertarCarona) placa.uppercase() else ""
                            val vagasFinal = if (ofertarCarona) vagas else "0"

                            aoConcluirCadastro(
                                nome, cpf, telefone, email, senha,
                                veiculoFinal, placaFinal, vagasFinal,
                                rua, numero, complemento, bairro, cidade, estado, cep
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (cpfJaExiste) Color.Gray else VerdeBotao),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Concluir cadastro", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        nome = ""; cpf = ""; telefone = ""; email = ""; senha = ""; veiculo = ""; placa = ""; vagas = "";
                        rua = ""; numero = ""; complemento = ""; bairro = ""; cidade = ""; estado = ""; cep = "";
                        ofertarCarona = false; cpfJaExiste = false; senhaVisivel = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Limpar Todos os Campos", color = Color.DarkGray)
                }

                TextButton(onClick = aoClicarFechar, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Fechar Cadastro", color = VermelhoErro)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}