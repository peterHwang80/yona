/**
 * Yona, 21st Century Project Hosting SW
 * <p>
 * Copyright Yona & Yobi Authors & NAVER Corp. & NAVER LABS Corp.
 * https://yona.io
 **/

package utils;

import controllers.routes;
import models.*;
import models.enumeration.ResourceType;
import models.resource.Resource;

public class RouteUtil {
    public static String getUrl(ResourceType resourceType, String resourceId) {
        return getUrl(resourceType, Long.valueOf(resourceId));
    }

    public static String getUrl(ResourceType resourceType, Long longId) {
        try {
            if (Resource.isRemovedFeatureResourceType(resourceType)) {
                return getProjectUrlOfRemovedFeatureResource(resourceType, String.valueOf(longId));
            }
            switch(resourceType) {
                case ISSUE_POST:
                    return getUrl(Issue.finder.byId(longId));
                case ISSUE_COMMENT:
                    return getUrl(IssueComment.find.byId(longId));
                case NONISSUE_COMMENT:
                    return getUrl(PostingComment.find.byId(longId));
                case BOARD_POST:
                    return getUrl(Posting.finder.byId(longId));
                case USER_AVATAR:
                    return getUrl(User.find.byId(longId));
                case PROJECT:
                    return getUrl(Project.find.byId(longId));
                default:
                    throw new IllegalArgumentException(
                            Resource.getInvalidResourceTypeMessage(resourceType));
            }
        } catch (Exception e) {
            play.Logger.error("Failed to get a url to the resource", e);
        }

        return null;
    }

    private static String getProjectUrlOfRemovedFeatureResource(ResourceType resourceType, String resourceId) {
        Resource resource = Resource.get(resourceType, resourceId);
        return resource == null ? null : getUrl(resource.getProject());
    }

    public static String getUrl(User user) {
        if (user == null) return null;
        user.refresh();

        return controllers.routes.UserApp.userInfo(
                user.loginId,
                controllers.routes.UserApp.userInfo$default$2(),
                controllers.routes.UserApp.userInfo$default$3()
        ).url();
    }

    public static String getUrl(Organization org) {
        if (org == null) return null;
        org.refresh();

        return controllers.routes.OrganizationApp.organization(org.name).url();
    }

    public static String getUrl(Project project) {
        if (project == null) return null;
        project.refresh();

        return controllers.routes.ProjectApp.project(project.owner, project.name).url();
    }

    public static String getUrl(Issue issue) {
        if (issue == null) return null;
        issue.refresh();

        return controllers.routes.IssueApp.issue(
                issue.project.owner, issue.project.name, issue.getNumber()).url();
    }

    public static String getUrl(Posting post) {
        if (post == null) return null;
        post.refresh();

        return controllers.routes.BoardApp.post(
                post.project.owner, post.project.name, post.getNumber()).url();
    }

    public static String getUrl(IssueComment comment) {
        if (comment == null) return null;

        return getUrl(comment.issue) + "#comment-" + comment.id;
    }

    public static String getUrl(PostingComment comment) {
        if (comment == null) return null;

        return getUrl(comment.posting) + "#comment-" + comment.id;
    }

    public static String getUrl(Comment comment) {
        if (comment == null) return null;

        if (comment instanceof IssueComment) {
            return getUrl((IssueComment) comment);
        } else if (comment instanceof PostingComment) {
            return getUrl((PostingComment) comment);
        }

        throw new IllegalArgumentException();
    }

    public static String getUrl(playRepository.Commit commit, Project project) {
        if (commit == null) return null;
        return getUrl(project);
    }
}
