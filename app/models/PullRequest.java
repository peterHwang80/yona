/**
 * Yona, 21st Century Project Hosting SW
 * <p>
 * Copyright Yona & Yobi Authors & NAVER Corp. & NAVER LABS Corp.
 * https://yona.io
 **/

package models;

import com.avaje.ebean.*;
import models.enumeration.EventType;
import models.enumeration.ResourceType;
import models.enumeration.State;
import models.resource.Resource;
import models.resource.ResourceConvertible;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.db.ebean.Transactional;
import playRepository.FileDiff;
import playRepository.GitRepository;
import utils.Constants;

import javax.persistence.*;
import javax.persistence.OrderBy;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.*;

import static com.avaje.ebean.Expr.*;

@Entity
public class PullRequest extends Model implements ResourceConvertible {

    private static final long serialVersionUID = 1L;

    public static final String DELIMETER = ",";
    public static final Finder<Long, PullRequest> finder = new Finder<>(Long.class, PullRequest.class);

    public static final int ITEMS_PER_PAGE = 15;

    @Id
    public Long id;

    @Constraints.Required
    @Size(max=255)
    public String title;

    @Lob
    public String body;

    @Transient
    public Long toProjectId;
    @Transient
    public Long fromProjectId;

    @ManyToOne
    public Project toProject;

    @ManyToOne
    public Project fromProject;

    @Constraints.Required
    @Size(max=255)
    public String toBranch;

    @Constraints.Required
    @Size(max=255)
    public String fromBranch;

    @ManyToOne
    public User contributor;

    @ManyToOne
    public User receiver;

    @Temporal(TemporalType.TIMESTAMP)
    public Date created;

    @Temporal(TemporalType.TIMESTAMP)
    public Date updated;

    @Temporal(TemporalType.TIMESTAMP)
    public Date received;

    public State state = State.OPEN;

    public Boolean isConflict;
    public Boolean isMerging;

    @OneToMany(cascade = CascadeType.ALL)
    public List<PullRequestCommit> pullRequestCommits;

    public String lastCommitId;

    public String mergedCommitIdFrom;

    public String mergedCommitIdTo;

    public Long number;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
        name = "pull_request_reviewers",
        joinColumns = @JoinColumn(name = "pull_request_id", unique = false),
        inverseJoinColumns = @JoinColumn(name = "user_id", unique = false),
        uniqueConstraints = @UniqueConstraint(columnNames = {"pull_request_id", "user_id"})
    )
    public Set<User> reviewers = new HashSet<>();

    @OneToMany(mappedBy = "pullRequest")
    public List<CommentThread> commentThreads = new ArrayList<>();

    @Transient
    private Repository repository;

    @Override
    public String toString() {
        return "PullRequest{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", toProject=" + toProject +
                ", fromProject=" + fromProject +
                ", toBranch='" + toBranch + '\'' +
                ", fromBranch='" + fromBranch + '\'' +
                ", contributor=" + contributor +
                ", receiver=" + receiver +
                ", created=" + created +
                ", updated=" + updated +
                ", received=" + received +
                ", state=" + state +
                '}';
    }

    public boolean isOpen() {
        return this.state == State.OPEN;
    }

    public static PullRequest findById(long id) {
        return finder.byId(id);
    }

    public static List<PullRequest> findSentPullRequests(Project project) {
        return finder.where()
                .eq("fromProject", project)
                .order().desc("created")
                .findList();
    }

    public static List<PullRequest> allReceivedRequests(Project project) {
        return finder.where()
                .eq("toProject", project)
                .order().desc("created")
                .findList();
    }

    @Override
    public Resource asResource() {
        return new Resource() {
            @Override
            public String getId() {
                return id.toString();
            }

            @Override
            public Project getProject() {
                return toProject;
            }

            @Override
            public ResourceType getType() {
                return ResourceType.PULL_REQUEST;
            }

            @Override
            public Long getAuthorId() {
                return contributor.id;
            }
        };
    }

    public String getResourceKey() {
        return ResourceType.PULL_REQUEST.resource() + Constants.RESOURCE_KEY_DELIM + this.id;
    }

    public Set<User> getWatchers() {
        return getWatchers(true);
    }

    public Set<User> getWatchers(boolean allowedWatchersOnly) {
        Set<User> actualWatchers = new HashSet<>();

        actualWatchers.add(this.contributor);
        for (CommentThread thread : commentThreads) {
            for (ReviewComment c : thread.reviewComments) {
                User user = User.find.byId(c.author.id);
                if (user != null) {
                    actualWatchers.add(user);
                }
            }
        }

        return Watch.findActualWatchers(actualWatchers, asResource(), allowedWatchersOnly);
    }

    public static List<PullRequest> findByToProject(Project project) {
        return finder.where().eq("toProject", project).order().asc("created").findList();
    }

    public static List<PullRequest> findByFromProjectAndBranch(Project fromProject, String fromBranch) {
        return finder.where().eq("fromProject", fromProject).eq("fromBranch", fromBranch)
                .or(eq("state", State.OPEN), eq("state", State.REJECTED)).findList();
    }

    @Transactional
    @Override
    public void save() {
        this.number = nextPullRequestNumber(toProject);
        super.save();
    }

    public static long nextPullRequestNumber(Project project) {
        PullRequest maxNumberedPullRequest = PullRequest.finder.where()
                .eq("toProject", project)
                .order().desc("number")
                .setMaxRows(1).findUnique();

        if(maxNumberedPullRequest == null || maxNumberedPullRequest.number == null) {
            return 1;
        } else {
            return ++maxNumberedPullRequest.number;
        }
    }

    public static PullRequest findOne(Project toProject, long number) {
        if(toProject == null || number <= 0) {
            return null;
        }
        return finder.where().eq("toProject", toProject).eq("number", number).findUnique();
    }

    @Transactional
    public static void regulateNumbers() {
        int nullNumberPullRequestCount = finder.where().eq("number", null).findRowCount();

        if(nullNumberPullRequestCount > 0) {
            List<Project> projects = Project.find.all();
            for(Project project : projects) {
                List<PullRequest> pullRequests = PullRequest.findByToProject(project);
                for(PullRequest pullRequest : pullRequests) {
                    if(pullRequest.number == null) {
                        pullRequest.number = nextPullRequestNumber(project);
                        pullRequest.update();
                    }
                }
            }
        }
    }

    public List<FileDiff> getDiff() throws IOException {
        if (mergedCommitIdFrom == null || mergedCommitIdTo == null) {
            throw new IllegalStateException("No mergedCommitIdFrom or mergedCommitIdTo");
        }
        return getDiff(mergedCommitIdFrom, mergedCommitIdTo);
    }

    public Repository getRepository() throws IOException {
        if (repository == null) {
            repository = new GitRepository(toProject).getRepository();
        }

        return repository;
    }

    @Transient
    public List<FileDiff> getDiff(String revA, String revB) throws IOException {
        Repository repository = getRepository();
        return GitRepository.getDiff(repository, revA, repository, revB);
    }

    public void deleteIssueEvents() {
        String newValue = this.id.toString();

        List<IssueEvent> oldEvents = IssueEvent.find.where()
                .eq("newValue", newValue)
                .eq("senderLoginId", this.contributor.loginId)
                .eq("eventType", EventType.ISSUE_REFERRED_FROM_PULL_REQUEST)
                .findList();

        for(IssueEvent event : oldEvents) {
            event.delete();
        }
    }

    @Override
    public void delete() {
        deleteIssueEvents();
        super.delete();
    }

    public void removeCommentThread(CommentThread commentThread) {
        this.commentThreads.remove(commentThread);
        commentThread.pullRequest = null;
    }

    public void addCommentThread(CommentThread thread) {
        this.commentThreads.add(thread);
        thread.pullRequest = this;
    }

    static public boolean noChangesBetween(Repository repoA, String rev1,
                                           Repository repoB, String rev2,
                                           String path) throws IOException {
        ObjectId a = getBlobId(repoA, rev1, path);
        ObjectId b = getBlobId(repoB, rev2, path);
        return ObjectUtils.equals(a, b);
    }

    static private ObjectId getBlobId(Repository repo, String rev, String path) throws IOException {
        if (StringUtils.isEmpty(rev)) {
            throw new IllegalArgumentException("rev must not be empty");
        }
        RevTree tree = new RevWalk(repo).parseTree(repo.resolve(rev));
        TreeWalk tw = TreeWalk.forPath(repo, path, tree);
        if (tw == null) {
            return null;
        }
        return tw.getObjectId(0);
    }
}
