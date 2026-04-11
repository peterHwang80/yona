/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2013 NAVER Corp.
 * http://yobi.io
 *
 * @author Yi EungJun
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
import org.apache.commons.lang3.StringUtils;
import playRepository.Commit;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Entity
public class CommitComment extends CodeComment {
    private static final long serialVersionUID = 1L;
    public static final Finder<Long, CommitComment> find = new Finder<>(Long.class, CommitComment.class);

    @Transient
    public List<CommitComment> replies = new ArrayList<>();

    public String commitId;

    public CommitComment() {
        super();
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
                return project;
            }

            @Override
            public ResourceType getType() {
                return ResourceType.COMMIT_COMMENT;
            }

            @Override
            public Long getAuthorId() {
                return authorId;
            }

            @Override
            public Resource getContainer() {
                return Commit.getAsResource(project, commitId);
            }

            public void delete() {
                CommitComment.this.delete();
            }
        };
    }

    public String groupKey() {
        return new StringBuilder().append(this.commitId)
                .append(this.path).append(this.line).toString();
    }

    public boolean threadEquals(CommitComment other) {
        return commitId.equals(other.commitId) &&
                path.equals(other.path) &&
                line.equals(other.line) &&
                side.equals(other.side);
    }

    public String getCommitId() {
        return commitId;
    }

    public boolean hasLocation() {
        return StringUtils.isNotBlank(this.path) && this.line != null;
    }
}
