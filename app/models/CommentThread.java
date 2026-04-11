/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2013 NAVER Corp.
 * http://yobi.io
 *
 * @author Keesun Baik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package models;

import models.enumeration.ResourceType;
import models.resource.Resource;
import models.resource.ResourceConvertible;
import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Keesun Baik
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class CommentThread extends Model implements ResourceConvertible {

    private static final long serialVersionUID = 1L;
    public static final Finder<Long, CommentThread> find = new Finder<>(Long.class, CommentThread.class);

    @Id
    public Long id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "author_id")),
            @AttributeOverride(name = "loginId", column = @Column(name = "author_login_id")),
            @AttributeOverride(name = "name", column = @Column(name = "author_name")),
    })
    public UserIdent author;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.REMOVE)
    public List<ReviewComment> reviewComments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    public ThreadState state;

    @Constraints.Required
    @Formats.DateTime(pattern = "YYYY/MM/DD/hh/mm/ss")
    public Date createdDate;

    @ManyToOne
    public PullRequest pullRequest;

    public boolean isOnPullRequest() {
        return pullRequest != null;
    }

    @Override
    public String toString() {
        return "CommentThread{" +
                "id=" + id +
                ", author=" + author +
                ", reviewComments=" + reviewComments +
                ", state=" + state +
                ", createdDate=" + createdDate +
                ", project=" + project +
                '}';
    }

    @ManyToOne
    public Project project;

    public Resource asResource() {
        return new Resource() {
            @Override
            public String getId() {
                return String.valueOf(id);
            }

            @Override
            public Project getProject() {
                return project;
            }

            @Override
            public ResourceType getType() {
                return ResourceType.COMMENT_THREAD;
            }

            @Override
            public Long getAuthorId() {
                return author.id;
            }
        };
    }

    public void removeComment(ReviewComment reviewComment) {
        reviewComments.remove(reviewComment);
        reviewComment.thread = null;
    }

    public enum ThreadState {
        OPEN, CLOSED;
    }

    public void addComment(ReviewComment reviewComment) {
        reviewComments.add(reviewComment);
        reviewComment.thread = this;
    }

    public ReviewComment getFirstReviewComment() {
        List<ReviewComment> list = ReviewComment.findByThread(this.id);
        if(!list.isEmpty()) {
            return list.get(0);
        }

        throw new IllegalStateException("This thread has no ReviewComment.");
    }

    public static void deleteByPullRequest(PullRequest pullRequest) {
        for(CommentThread commentThread : find.where().eq("pullRequest", pullRequest).findList()) {
            commentThread.delete();
        }
    }
}
