package com.sedex.connect.company.endpoint

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.common.http.toResponse
import com.sedex.connect.company.CompanySecurityContext
import com.sedex.connect.company.api.CompanySubsidiaryRelationResponse
import com.sedex.connect.company.api.CompanySubsidiaryResponse
import com.sedex.connect.company.api.CompanySubsidiaryTreeResponse
import com.sedex.connect.company.api.companySubsidiariesResponseExample
import com.sedex.connect.company.api.companySubsidiariesResponseLens
import com.sedex.connect.company.api.companySubsidiariesTreeResponseLens
import com.sedex.connect.company.api.companySubsidiaryRequestExample
import com.sedex.connect.company.api.companySubsidiaryRequestLens
import com.sedex.connect.company.api.companySubsidiaryTreeResponseExample
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion
import com.sedex.connect.company.models.CompanySubsidiary
import com.sedex.connect.company.models.CompanySubsidiaryRelation
import com.sedex.connect.company.services.CompanySubsidiaryService
import com.sedex.connect.lang.mapToSet
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.security.and
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

class CompanySubsidiaryRoute(
    private val companySubsidiaryService: CompanySubsidiaryService,
    private val securityContext: CompanySecurityContext,
) : ContractRoutes {
    private val createCompanySubsidiarySpec = "/subsidiary" meta {
        summary = "Create company subsidiary"
        description = summary
        security = securityContext.requiresServiceToken()
        receiving(companySubsidiaryRequestLens to companySubsidiaryRequestExample)
        returning(Status.CREATED)
    }

    private val deleteCompanySubsidiarySpec = "/subsidiary" meta {
        summary = "Delete company subsidiary"
        description = summary
        security = securityContext.requiresServiceToken()
        receiving(companySubsidiaryRequestLens to companySubsidiaryRequestExample)
        returning(Status.OK)
    }

    private val companySubsidiariesSpec = "/companies" / CreateCompanyRoute.orgCodePath / "subsidiaries" meta {
        summary = "Gets companies subsidiaries"
        description = summary
        security = securityContext.anyLoggedInUser()
            .and(securityContext.canAccessByOrgCode { Companion.orgCodePath(it).value })
        returning(Status.OK, companySubsidiariesResponseLens to setOf(companySubsidiariesResponseExample))
    }
    private val companySubsidiariesTreeSpec = "/companies" / CreateCompanyRoute.orgCodePath / "subsidiaries" / "tree" meta {
        summary = "Gets companies subsidiaries recursive tree"
        description = summary
        security = securityContext.anyLoggedInUser()
            .and(securityContext.canAccessByOrgCode { Companion.orgCodePath(it).value })
        returning(Status.OK, companySubsidiariesTreeResponseLens to companySubsidiaryTreeResponseExample)
    }

    override fun contractRoutes(): List<ContractRoute> = listOf(
        createCompanySubsidiarySpec bindContract POST to ::create,
        deleteCompanySubsidiarySpec bindContract DELETE to ::delete,
        companySubsidiariesSpec bindContract GET to { orgCode, _ -> { getCompanySubsidiaries(orgCode) } },
        companySubsidiariesTreeSpec bindContract GET to { parentOrgCode, _, _ -> { getCompanySubsidiariesTree(parentOrgCode) } },
    )

    private fun create(request: Request): Response {
        val companySubsidiaryRequest = companySubsidiaryRequestLens(request)
        companySubsidiaryService.create(
            companySubsidiaryRequest.parentOrgCode,
            companySubsidiaryRequest.childOrgCode
        )
        return Response(Status.CREATED)
    }

    private fun delete(request: Request): Response {
        val companySubsidiaryRequest = companySubsidiaryRequestLens(request)
        companySubsidiaryService.delete(
            companySubsidiaryRequest.parentOrgCode,
            companySubsidiaryRequest.childOrgCode
        )
        return Response(Status.OK)
    }

    private fun getCompanySubsidiaries(orgCode: OrganisationCode): Response =
        runCatching {
            val companySubsidiaries = companySubsidiaryService.getCompanySubsidiaries(orgCode)
            Response(Status.OK).with(companySubsidiariesResponseLens of companySubsidiaries.mapToSet { it.toResponse() })
        }.getOrElse { it.toResponse() }

    private fun getCompanySubsidiariesTree(parentOrgCode: OrganisationCode): Response {
        val values = companySubsidiaryService.getCompanySubsidiariesTree(parentOrgCode)
            .map { it.toExternal() }
        return if (values.isEmpty()) Response(Status.NOT_FOUND)
        else Response(Status.OK)
            .with(companySubsidiariesTreeResponseLens of CompanySubsidiaryTreeResponse(values))
    }

    private fun CompanySubsidiary.toResponse(): CompanySubsidiaryResponse = CompanySubsidiaryResponse(code, name)

    private fun CompanySubsidiaryRelation.toExternal() = CompanySubsidiaryRelationResponse(
        parentOrgCode = parentOrgCode,
        childOrgCode = childOrgCode,
    )
}
