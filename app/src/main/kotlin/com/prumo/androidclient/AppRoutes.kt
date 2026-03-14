package com.prumo.androidclient

object AppRoutes {
    const val Splash = "splash"
    const val Login = "login"
    const val Index = "index"
    const val SemAcesso = "sem-acesso"
    const val Obras = "obras"
    const val UsuariosAcessos = "usuarios-acessos"
    const val Fornecedores = "cadastros/fornecedores"
    const val Materiais = "cadastros/materiais"
    const val MaterialFornecedor = "cadastros/material-fornecedor"
    const val DashboardPattern = "dashboard/{obraId}"
    const val PedidosPattern = "dashboard/{obraId}/pedidos"
    const val RecebimentoPattern = "dashboard/{obraId}/recebimento"
    const val EstoquePattern = "dashboard/{obraId}/estoque"
    const val AccessReviewPattern = "acesso/avaliar/{token}"

    fun dashboard(obraId: String) = "dashboard/$obraId"
    fun pedidos(obraId: String) = "dashboard/$obraId/pedidos"
    fun recebimento(obraId: String) = "dashboard/$obraId/recebimento"
    fun estoque(obraId: String) = "dashboard/$obraId/estoque"
    fun accessReview(token: String) = "acesso/avaliar/$token"
}
