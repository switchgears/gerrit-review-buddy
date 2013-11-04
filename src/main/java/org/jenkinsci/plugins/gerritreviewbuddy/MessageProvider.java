/*
The MIT License (MIT)

Copyright (c) 2013 Switch-Gears ApS

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

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
