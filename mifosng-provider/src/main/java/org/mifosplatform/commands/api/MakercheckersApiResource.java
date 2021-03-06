/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.commands.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.mifosplatform.commands.data.AuditData;
import org.mifosplatform.commands.data.AuditSearchData;
import org.mifosplatform.commands.data.CommandSourceData;
import org.mifosplatform.commands.service.AuditReadPlatformService;
import org.mifosplatform.commands.service.PortfolioCommandSourceWritePlatformService;
import org.mifosplatform.infrastructure.core.api.ApiParameterHelper;
import org.mifosplatform.infrastructure.core.api.ApiRequestParameterHelper;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.mifosplatform.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.mifosplatform.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/makercheckers")
@Component
@Scope("singleton")
public class MakercheckersApiResource {

	private final Set<String> RESPONSE_DATA_PARAMETERS = new HashSet<String>(
			Arrays.asList("id", "actionName", "entityName", "resourceId",
					"subresourceId", "maker", "madeOnDate", "checker",
					"checkedOnDate", "processingResult", "commandAsJson",
					"officeName", "groupLevelName", "groupName", "clientName",
					"loanAccountNo", "savingsAccountNo"));
	private final String resourceNameForPermissions = "MAKERCHECKER";

	private final PlatformSecurityContext context;
	private final AuditReadPlatformService readPlatformService;
	private final DefaultToApiJsonSerializer<CommandSourceData> toApiJsonSerializer;
	private final DefaultToApiJsonSerializer<AuditData> toApiJsonSerializerAudit;
	private final DefaultToApiJsonSerializer<AuditSearchData> toApiJsonSerializerSearchTemplate;
	private final ApiRequestParameterHelper apiRequestParameterHelper;
	private final PortfolioCommandSourceWritePlatformService writePlatformService;

	@Autowired
	public MakercheckersApiResource(
			final PlatformSecurityContext context,
			final AuditReadPlatformService readPlatformService,
			final DefaultToApiJsonSerializer<CommandSourceData> toApiJsonSerializer,
			final DefaultToApiJsonSerializer<AuditData> toApiJsonSerializerAudit,
			final DefaultToApiJsonSerializer<AuditSearchData> toApiJsonSerializerSearchTemplate,
			final ApiRequestParameterHelper apiRequestParameterHelper,
			final PortfolioCommandSourceWritePlatformService writePlatformService) {
		this.context = context;
		this.readPlatformService = readPlatformService;
		this.toApiJsonSerializer = toApiJsonSerializer;
		this.apiRequestParameterHelper = apiRequestParameterHelper;
		this.toApiJsonSerializerAudit = toApiJsonSerializerAudit;
		this.toApiJsonSerializerSearchTemplate = toApiJsonSerializerSearchTemplate;
		this.writePlatformService = writePlatformService;
	}

	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveCommands(@Context final UriInfo uriInfo,
			@QueryParam("actionName") final String actionName,
			@QueryParam("entityName") final String entityName,
			@QueryParam("resourceId") final Long resourceId,
			@QueryParam("makerId") final Long makerId,
			@QueryParam("makerDateTimeFrom") final String makerDateTimeFrom,
			@QueryParam("makerDateTimeTo") final String makerDateTimeTo) {

		context.authenticatedUser().validateHasReadPermission(
				resourceNameForPermissions);

		final String extraCriteria = getExtraCriteria(actionName, entityName,
				resourceId, makerId, makerDateTimeFrom, makerDateTimeTo);

		final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper
				.process(uriInfo.getQueryParameters());

		final Collection<AuditData> entries = this.readPlatformService
				.retrieveAllEntriesToBeChecked(extraCriteria,
						settings.isIncludeJson());

		return this.toApiJsonSerializerAudit.serialize(settings, entries,
				RESPONSE_DATA_PARAMETERS);
	}

	@GET
	@Path("/searchtemplate")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveAuditSearchTemplate(@Context final UriInfo uriInfo) {

		context.authenticatedUser().validateHasReadPermission(
				resourceNameForPermissions);

		final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper
				.process(uriInfo.getQueryParameters());

		final AuditSearchData auditSearchData = this.readPlatformService
				.retrieveSearchTemplate("makerchecker");

		final Set<String> RESPONSE_DATA_PARAMETERS_SEARCH_TEMPLATE = new HashSet<String>(
				Arrays.asList("appUsers", "actionNames", "entityNames"));

		return this.toApiJsonSerializerSearchTemplate.serialize(settings,
				auditSearchData, RESPONSE_DATA_PARAMETERS_SEARCH_TEMPLATE);
	}

	@POST
	@Path("{commandId}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String approveMakerCheckerEntry(
			@PathParam("commandId") final Long commandId,
			@QueryParam("command") final String commandParam) {

		CommandProcessingResult result = null;
		if (is(commandParam, "approve")) {
			result = this.writePlatformService.approveEntry(commandId);
		} else {
			throw new UnrecognizedQueryParamException("command", commandParam);
		}

		return this.toApiJsonSerializer.serialize(result);
	}

	private boolean is(final String commandParam, final String commandValue) {
		return StringUtils.isNotBlank(commandParam)
				&& commandParam.trim().equalsIgnoreCase(commandValue);
	}

	@DELETE
	@Path("{commandId}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String deleteMakerCheckerEntry(
			@PathParam("commandId") final Long commandId) {

		final Long id = this.writePlatformService.deleteEntry(commandId);

		return this.toApiJsonSerializer.serialize(CommandProcessingResult
				.commandOnlyResult(id));
	}

	private String getExtraCriteria(final String actionName,
			final String entityName, final Long resourceId, final Long makerId,
			final String makerDateTimeFrom, final String makerDateTimeTo) {

		String extraCriteria = "";

		if (actionName != null) {
			extraCriteria += " and aud.action_name = "
					+ ApiParameterHelper.sqlEncodeString(actionName);
		}
		if (entityName != null) {
			extraCriteria += " and aud.entity_name like "
					+ ApiParameterHelper.sqlEncodeString(entityName + "%");
		}

		if (resourceId != null) {
			extraCriteria += " and aud.resource_id = " + resourceId;
		}
		if (makerId != null) {
			extraCriteria += " and aud.maker_id = " + makerId;
		}
		if (makerDateTimeFrom != null) {
			extraCriteria += " and aud.made_on_date >= "
					+ ApiParameterHelper.sqlEncodeString(makerDateTimeFrom);
		}
		if (makerDateTimeTo != null) {
			extraCriteria += " and aud.made_on_date <= "
					+ ApiParameterHelper.sqlEncodeString(makerDateTimeTo);
		}

		if (StringUtils.isNotBlank(extraCriteria)) {
			extraCriteria = extraCriteria.substring(4);
		}

		return extraCriteria;
	}
}