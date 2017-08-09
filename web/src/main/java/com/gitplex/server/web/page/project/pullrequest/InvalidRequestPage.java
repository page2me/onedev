package com.gitplex.server.web.page.project.pullrequest;

import javax.persistence.EntityNotFoundException;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.gitplex.server.GitPlex;
import com.gitplex.server.manager.PullRequestManager;
import com.gitplex.server.model.PullRequest;
import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.web.page.project.NoCommitsPage;
import com.gitplex.server.web.page.project.ProjectPage;
import com.gitplex.server.web.page.project.pullrequest.requestlist.RequestListPage;
import com.gitplex.server.web.util.ConfirmOnClick;
import com.google.common.base.Preconditions;

@SuppressWarnings("serial")
public class InvalidRequestPage extends ProjectPage {

	public static final String PARAM_REQUEST = "request";
	
	private IModel<PullRequest> requestModel;
	
	public InvalidRequestPage(PageParameters params) {
		super(params);
		
		if (getProject().getDefaultBranch() == null) 
			throw new RestartResponseException(NoCommitsPage.class, paramsOf(getProject()));

		requestModel = new LoadableDetachableModel<PullRequest>() {

			@Override
			protected PullRequest load() {
				Long requestNumber = params.get(PARAM_REQUEST).toLong();
				PullRequest request = GitPlex.getInstance(PullRequestManager.class).find(getProject(), requestNumber);
				if (request == null)
					throw new EntityNotFoundException("Unable to find request #" + requestNumber + " in project " + getProject());
				Preconditions.checkState(!request.isValid());
				return request;
			}

		};
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new Link<Void>("delete") {

			@Override
			public void onClick() {
				GitPlex.getInstance(PullRequestManager.class).delete(requestModel.getObject());
				setResponsePage(RequestListPage.class, RequestListPage.paramsOf(getProject()));
			}

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canModify(requestModel.getObject()));
			}
			
		}.add(new ConfirmOnClick("Do you really want to delete pull request #" + requestModel.getObject().getNumber() + "?")));
	}

	public static PageParameters paramsOf(PullRequest request) {
		PageParameters params = ProjectPage.paramsOf(request.getTarget().getProject());
		params.add(PARAM_REQUEST, request.getNumber());
		return params;
	}
	
	@Override
	protected void onDetach() {
		requestModel.detach();
		super.onDetach();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new InvalidRequestResourceReference()));
	}

}