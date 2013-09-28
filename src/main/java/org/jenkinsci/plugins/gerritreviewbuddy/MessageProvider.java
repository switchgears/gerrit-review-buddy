// Copyright 2013 Switch-Gears ApS

package org.jenkinsci.plugins.gerritreviewbuddy;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multiset;
import com.sonyericsson.hudson.plugins.gerrit.trigger.gerritnotifier.GerritMessageProvider;
import hudson.Extension;
import hudson.model.AbstractBuild;

@Extension
public class MessageProvider extends GerritMessageProvider {

    @Override
    public String getBuildCompletedMessage(AbstractBuild build) {
        Preconditions.checkNotNull(build);

        ReviewBuddyAction action = build.getAction(ReviewBuddyAction.class);
        if (action == null) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" -----------------------------------------------------------\n\n");
        stringBuilder.append(" REVIEW BUDDY\n\n");
        stringBuilder.append(" This commit changed ").append(action.getChangedLinesCount()).append(" lines.\n\n");

        // Comment on the size of the change. Cohen 2006
        if(action.getChangedLinesCount() <= 200) {
            stringBuilder.append(" This is a reasonable size of a commit.\n\n");
        } else {
            stringBuilder.append(" This may be too large for an effective review.\n");
            stringBuilder.append(" A complete read-through of the change before the actual review will help to maximize");
            stringBuilder.append("\n the review effort.\n\n");
        }

        // Length of the review. Dunsmore 2000
        stringBuilder.append(" It is suggested to perform the review in ");
        if (action.getReviewSessionsCount() > 1) {
            stringBuilder.append(action.getReviewSessionsCount()).append(" sessions of ");
        }
        stringBuilder.append("about ").append(action.getReviewSessionsLength()).append(" minutes.\n\n");

        // Number of reviewers.
        stringBuilder.append(" A minimum of ").append(action.getReviewersCount()).append(" reviewers should be invited.\n\n");

        // List of reviewers.
        stringBuilder.append(" Here is a list of developers sorted by experience in the area:\n\n");
        for (Multiset.Entry<String> reviewer : action.getPotentialReviewers().entrySet()) {
            stringBuilder.append(" ").append(reviewer.getCount()).append(" ").append(reviewer.getElement()).append("\n");
        }

        return stringBuilder.toString();
    }
}
