package com.example.transporte_interiorano.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.delay

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
    aoConcluirCadastro: (String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String) -> Unit,
    aoClicarFechar: () -> Unit,
    mensagemErro: String = ""
) {
    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var confirmarSenhaVisivel by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }

    var tipoVeiculo by remember { mutableStateOf("Carro") }
    var veiculo by remember { mutableStateOf("") }
    var placa by remember { mutableStateOf("") }
    // 🟢 ALTERAÇÃO INICIAL: Vagas agora começam com o valor pré-carregado como "4" correspondente ao tipo Carro
    var vagas by remember { mutableStateOf("4") }

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

    // 🟢 ADIÇÃO: Estados dos Termos
    var termosAceitos by remember { mutableStateOf(false) }
    var showPlaceholderTermos by remember { mutableStateOf(false) }

    var usuarioDisponivel by remember { mutableStateOf(true) }
    val sugestoesNomes = remember { mutableStateListOf<String>() }
    var ufExpandido by remember { mutableStateOf(false) }
    var tipoVeiculoExpandido by remember { mutableStateOf(false) }

    val estadosBrasil = listOf("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO")

    LaunchedEffect(nome) {
        // 🟢 DEBOUNCE: Aguarda 500ms após o usuário parar de digitar
        delay(500)

        val nomeTratado = nome.trim()

        // 🟢 VALIDAÇÃO: Só chama o banco se não estiver vazio e tiver um espaço
        if (nomeTratado.isNotBlank() && nomeTratado.contains(" ")) {
            val partes = nomeTratado.split("\\s+".toRegex())
            if (partes.size >= 2) {
                val combinacaoBase = "${partes.first().lowercase()}.${partes.last().lowercase()}"

                // Evita chamar o banco se o username for exatamente o que já está na tela
                if (username != combinacaoBase) {
                    username = combinacaoBase
                    BancoDeDados.verificarDisponibilidadeUsuario(combinacaoBase) { livre, lista ->
                        usuarioDisponivel = livre
                        sugestoesNomes.clear()
                        if (!livre) {
                            sugestoesNomes.addAll(lista)
                        }
                    }
                }
            }
        }
    }

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
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("CRIAR NOVA CONTA", fontSize = 24.sp, color = AzulPrincipal, fontWeight = FontWeight.Bold)

            if (mensagemErro.isNotEmpty()) {
                Text(mensagemErro, color = VermelhoErro, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                supportingText = { if (cpfJaExiste) Text("⚠️ Este CPF já está cadastrado!", color = VermelhoErro, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                visualTransformation = CpfVisualTransformation()
            )

            OutlinedTextField(
                value = telefone, onValueChange = { telefone = it.filter { char -> char.isDigit() }.take(11) }, label = { Text("Telefone / WhatsApp") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                visualTransformation = TelefoneVisualTransformation()
            )

            Text("ENDEREÇO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = rua, onValueChange = { rua = it }, label = { Text("Rua/Avenida") }, modifier = Modifier.weight(2f), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                OutlinedTextField(value = numero, onValueChange = { numero = it }, label = { Text("Nº") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = complemento, onValueChange = { complemento = it }, label = { Text("Compl. (Opcional)") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                OutlinedTextField(value = bairro, onValueChange = { bairro = it }, label = { Text("Bairro") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = cidade, onValueChange = { cidade = it }, label = { Text("Cidade") }, modifier = Modifier.weight(1.3f), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))

                ExposedDropdownMenuBox(
                    expanded = ufExpandido,
                    onExpandedChange = { ufExpandido = !ufExpandido },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = estado, onValueChange = {}, readOnly = true, singleLine = true, label = { Text("UF") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ufExpandido) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = ufExpandido, onDismissRequest = { ufExpandido = false }) {
                        estadosBrasil.forEach { uf ->
                            DropdownMenuItem(text = { Text(uf) }, onClick = { estado = uf; ufExpandido = false })
                        }
                    }
                }
            }

            OutlinedTextField(
                value = cep, onValueChange = { cep = it.filter { char -> char.isDigit() }.take(8) }, label = { Text("CEP") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), visualTransformation = CepVisualTransformation()
            )

            Text("DADOS DA CONTA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { novoUser ->
                    username = novoUser.filter { !it.isWhitespace() }.lowercase()
                    BancoDeDados.verificarDisponibilidadeUsuario(username) { livre, lista ->
                        usuarioDisponivel = livre
                        sugestoesNomes.clear()
                        if (!livre) {
                            sugestoesNomes.addAll(lista)
                        }
                    }
                },
                label = { Text("Nome de Usuário (Para Login)") },
                modifier = Modifier.fillMaxWidth(),
                isError = !usuarioDisponivel,
                supportingText = { if (!usuarioDisponivel) Text("❌ Nome ocupado! Escolha uma sugestão livre:", color = VermelhoErro, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            if (!usuarioDisponivel && sugestoesNomes.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Nome ocupado! Sugestões livres (deslize para o lado):", color = VermelhoErro, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(sugestoesNomes.size) { index ->
                            val sugestao = sugestoesNomes[index]
                            SuggestionChip(
                                onClick = {
                                    username = sugestao
                                    usuarioDisponivel = true
                                    sugestoesNomes.clear()
                                },
                                label = { Text(text = sugestao,   fontWeight = FontWeight.Bold, color = AzulPrincipal, maxLines = 1) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))

            OutlinedTextField(
                value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next), // 🟢 Alterado para Next
                trailingIcon = {
                    val image = if (senhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { senhaVisivel = !senhaVisivel }) { Icon(imageVector = image, contentDescription = null) }
                }
            )

            // 🟢 ADICIONADO CIRURGICAMENTE: Validação de critérios robustos em tempo real
            val senhaValida = senha.length >= 8 &&
                    senha.any { it.isUpperCase() } &&
                    senha.any { it.isDigit() } &&
                    senha.any { !it.isLetterOrDigit() }

            if (senha.isNotEmpty() && !senhaValida) {
                Text(
                    text = "⚠️ A senha deve conter ao menos 8 caracteres, 1 letra maiúscula, 1 número e 1 caractere especial.",
                    color = VermelhoErro, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // 🟢 ADICIONADO CIRURGICAMENTE: Segundo campo obrigatório de Confirmação de Senha
            OutlinedTextField(
                value = confirmarSenha, onValueChange = { confirmarSenha = it }, label = { Text("Confirmar Senha") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (confirmarSenhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                isError = confirmarSenha.isNotEmpty() && senha != confirmarSenha,
                supportingText = {
                    if (confirmarSenha.isNotEmpty() && senha != confirmarSenha) {
                        Text("❌ As senhas digitadas não coincidem!", color = VermelhoErro, fontWeight = FontWeight.Bold)
                    }
                },
                trailingIcon = {
                    val image = if (confirmarSenhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { confirmarSenhaVisivel = !confirmarSenhaVisivel }) { Icon(imageVector = image, contentDescription = null) }
                }
            )

            Surface(color = Color.White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (ofertarCarona) "Ofertar Corridas (Ativado)" else "Ofertar Corridas (Desativado)", color = if (ofertarCarona) AzulPrincipal else Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(checked = ofertarCarona, onCheckedChange = { ofertarCarona = it })
                    }

                    if (ofertarCarona) {
                        Spacer(modifier = Modifier.height(6.dp))

                        ExposedDropdownMenuBox(
                            expanded = tipoVeiculoExpandido,
                            onExpandedChange = { tipoVeiculoExpandido = !tipoVeiculoExpandido },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = tipoVeiculo, onValueChange = {}, readOnly = true, singleLine = true,
                                label = { Text("Tipo de Veículo") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoVeiculoExpandido) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            // 🟢 ALTERAÇÃO 1: Injeta dinamicamente as vagas predefinidas no clique da modalidade (4 para carro, 1 para moto)
                            ExposedDropdownMenu(expanded = tipoVeiculoExpandido, onDismissRequest = { tipoVeiculoExpandido = false }) {
                                DropdownMenuItem(text = { Text("Carro") }, onClick = { tipoVeiculo = "Carro"; vagas = "4"; tipoVeiculoExpandido = false })
                                DropdownMenuItem(text = { Text("Moto") }, onClick = { tipoVeiculo = "Moto"; vagas = "1"; tipoVeiculoExpandido = false })
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = veiculo, onValueChange = { veiculo = it }, label = { Text("Modelo do Veículo") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = placa, onValueChange = { placa = it.uppercase().take(7) }, label = { Text("Placa") }, modifier = Modifier.weight(1.5f), visualTransformation = PlacaVisualTransformation(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))

                            // 🟢 ALTERAÇÃO 2: Campo numérico aberto e 100% editável para suportar carros de 7 lugares ou vans de 11 vagas
                            OutlinedTextField(
                                value = vagas,
                                onValueChange = { vagas = it.filter { char -> char.isDigit() } },
                                label = { Text("Vagas") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }

            // 🟢 ADIÇÃO: Row do Checkbox (Termos)
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = termosAceitos,
                    onCheckedChange = { termosAceitos = it }
                )
                Text(
                    text = "Li e concordo com os Termos de Uso e Política de Privacidade",
                    modifier = Modifier.clickable { showPlaceholderTermos = true },
                    color = AzulPrincipal,
                    fontWeight = FontWeight.Bold
                )
            }

            // 🟢 AVISO TEMPORÁRIO (Dialog)
            if (showPlaceholderTermos) {
                AlertDialog(
                    onDismissRequest = { showPlaceholderTermos = false },
                    title = { Text("Termos em Desenvolvimento") },
                    text = { Text("Estamos redigindo nossos Termos de Uso e Política de Privacidade. Em breve estarão disponíveis para consulta.") },
                    confirmButton = {
                        Button(onClick = { showPlaceholderTermos = false }) { Text("OK") }
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                // DEPOIS

                // 1. Defina a regra de validação antes do botão
                val isFormValid = termosAceitos && !cpfJaExiste && usuarioDisponivel && username.isNotBlank() && senhaValida && senha == confirmarSenha

                // 2. O botão utiliza o estado de habilitação
                Button(
                    onClick = {
                        // O código aqui fica limpo, pois só executa se o botão estiver enabled
                        val veiculoFinal = if (ofertarCarona) "$tipoVeiculo - $veiculo" else ""
                        val placaFinal = if (ofertarCarona) placa.uppercase() else ""
                        val vagasFinal = if (ofertarCarona) vagas else "0"

                        aoConcluirCadastro(nome, cpf, telefone, email, senha, veiculoFinal, placaFinal, vagasFinal, rua, numero, complemento, bairro, cidade, estado, cep, username)
                    },
                    enabled = isFormValid, // 🟢 O botão bloqueia cliques automaticamente se for false
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VerdeBotao,          // Cor quando ativo
                        disabledContainerColor = Color.Gray   // 🟢 Cor automática quando desativado
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Concluir Cadastro", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        nome = ""; cpf = ""; telefone = ""; email = ""; senha = ""; username = ""; veiculo = ""; placa = ""; vagas = "4";
                        rua = ""; numero = ""; complemento = ""; bairro = ""; cidade = ""; estado = ""; cep = "";
                        confirmarSenha = ""; senhaVisivel = false; confirmarSenhaVisivel = false;
                        ofertarCarona = false; cpfJaExiste = false; senhaVisivel = false; usuarioDisponivel = true;
                        sugestoesNomes.clear()
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) { Text("Limpar Todos os Campos", color = Color.DarkGray) }

                TextButton(onClick = aoClicarFechar, modifier = Modifier.fillMaxWidth().height(44.dp)) { Text("Fechar Cadastro", color = VermelhoErro) }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}