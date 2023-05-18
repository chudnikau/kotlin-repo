package com.sedex.connect.company.table

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.models.CompanySubsidiaryRelation
import com.sedex.connect.lang.mapToSet
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.time.Instant
import java.util.UUID

object AdvanceCompanySubsidiaryTable : Table("advance_company_subsidiary") {
    val id = uuid("id")
    val parentOrganisationCode = text("parent_organisation_code")
    val childOrganisationCode = text("child_organisation_code")
    val createdTime = timestamp("created_time")
}

fun AdvanceCompanySubsidiaryTable.exists(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode): Boolean =
    select(
        (parentOrganisationCode eq parentOrgCode.value) and
            (childOrganisationCode eq childOrgCode.value)
    )
        .singleOrNull() != null

fun AdvanceCompanySubsidiaryTable.getByParentOrgCode(parentOrgCode: OrganisationCode): Set<OrganisationCode> =
    select(parentOrganisationCode eq parentOrgCode.value)
        .mapToSet { row -> OrganisationCode(row[this.childOrganisationCode]) }

fun AdvanceCompanySubsidiaryTable.insert(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode) {
    insert {
        it[id] = UUID.randomUUID()
        it[parentOrganisationCode] = parentOrgCode.value
        it[childOrganisationCode] = childOrgCode.value
        it[createdTime] = Instant.now()
    }
}

fun AdvanceCompanySubsidiaryTable.getByParentOrgCodeRecursive(parentOrgCode: OrganisationCode): Set<CompanySubsidiaryRelation> {
    return TransactionManager.current().exec(
        stmt = """WITH RECURSIVE tree AS (
  SELECT s.${parentOrganisationCode.name}, s.${childOrganisationCode.name}
    FROM $tableName s
    WHERE s.${parentOrganisationCode.name} = ?
    UNION all
    SELECT t.${parentOrganisationCode.name}, t.${childOrganisationCode.name}
    FROM $tableName t
      JOIN tree rt ON rt.${childOrganisationCode.name} = t.${parentOrganisationCode.name}
)
SELECT *
FROM tree""",
        args = listOf(VarCharColumnType() to parentOrgCode.value),
        explicitStatementType = StatementType.SELECT,
    ) { rs ->
        val set = mutableSetOf<CompanySubsidiaryRelation>()
        while (rs.next()) {
            set.add(
                CompanySubsidiaryRelation(
                    parentOrgCode = OrganisationCode(rs.getString(1)),
                    childOrgCode = OrganisationCode(rs.getString(2)),
                )
            )
        }
        set.toSet()
    } ?: emptySet()
}

fun AdvanceCompanySubsidiaryTable.delete(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode) {
    deleteWhere {
        (parentOrganisationCode eq parentOrgCode.value) and
            (childOrganisationCode eq childOrgCode.value)
    }
}
