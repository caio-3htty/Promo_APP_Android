package com.prumo.core.model

object AccessPolicy {
    fun hasObraAccess(
        role: AppRole?,
        obraIds: List<String>,
        obraId: String?
    ): Boolean {
        if (obraId.isNullOrBlank()) return false
        if (role == AppRole.MASTER || role == AppRole.GESTOR) return true
        return obraIds.contains(obraId)
    }

    fun hasPermissionGrant(
        permissions: List<EffectivePermission>,
        permissionKey: String,
        obraId: String? = null
    ): Boolean {
        return permissions.any { permission ->
            if (permission.permissionKey != permissionKey) return@any false
            when (permission.scopeType) {
                PermissionScopeType.TENANT,
                PermissionScopeType.ALL_OBRAS -> true
                PermissionScopeType.SELECTED_OBRAS -> !obraId.isNullOrBlank() && permission.obraIds.contains(obraId)
            }
        }
    }

    fun can(
        user: SessionUser?,
        permissionKey: String,
        obraId: String? = null
    ): Boolean {
        if (user == null || !user.isActive) return false
        if (user.role == AppRole.MASTER || user.role == AppRole.GESTOR) return true
        return hasPermissionGrant(user.permissions, permissionKey, obraId)
    }

    fun hasOperationalAccess(user: SessionUser?): Boolean {
        if (user == null) return false
        if (!user.isActive) return false
        val role = user.role
        if (role == AppRole.MASTER || role == AppRole.GESTOR) return true
        return user.obraScope.isNotEmpty() || can(user, permissionKey = "obras.view")
    }

    fun canManageCadastros(role: AppRole?): Boolean {
        return role == AppRole.MASTER || role == AppRole.GESTOR || role == AppRole.OPERACIONAL
    }

    fun canApprovePedidos(role: AppRole?): Boolean {
        return role == AppRole.MASTER || role == AppRole.GESTOR || role == AppRole.ENGENHEIRO
    }

    fun canEditPedidosBase(role: AppRole?): Boolean {
        return role == AppRole.MASTER || role == AppRole.GESTOR || role == AppRole.OPERACIONAL
    }

    fun canReceivePedidos(role: AppRole?): Boolean {
        return role == AppRole.MASTER || role == AppRole.GESTOR || role == AppRole.ALMOXARIFE
    }

    fun canAccessRecebimentoRoute(role: AppRole?): Boolean {
        return role == AppRole.MASTER ||
            role == AppRole.GESTOR ||
            role == AppRole.ALMOXARIFE ||
            role == AppRole.ENGENHEIRO
    }
}
