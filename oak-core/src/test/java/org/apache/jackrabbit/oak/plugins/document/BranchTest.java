/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.document;

import com.google.common.collect.Sets;

import org.apache.jackrabbit.oak.plugins.document.Branch.BranchCommit;
import org.apache.jackrabbit.oak.plugins.document.memory.MemoryDocumentStore;
import org.apache.jackrabbit.oak.plugins.document.util.Utils;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class BranchTest {

    @Rule
    public DocumentMKBuilderProvider builderProvider = new DocumentMKBuilderProvider();

    @Test
    public void getModifiedPathsUntil() {
        UnmergedBranches branches = new UnmergedBranches();

        RevisionVector base = new RevisionVector(Revision.newRevision(1));
        Revision c1 = Revision.newRevision(1).asBranchRevision();
        Branch b = branches.create(base, c1, null);

        BranchCommit bc1 = b.getCommit(c1);
        bc1.track("/foo");

        Revision c2 = Revision.newRevision(1).asBranchRevision();
        b.addCommit(c2);
        BranchCommit bc2 = b.getCommit(c2);
        bc2.track("/bar");

        Revision c3 = Revision.newRevision(1).asBranchRevision();
        b.rebase(c3, new RevisionVector(Revision.newRevision(1)));

        Revision c4 = Revision.newRevision(1).asBranchRevision();
        b.addCommit(c4);
        BranchCommit bc4 = b.getCommit(c4);
        bc4.track("/baz");

        Revision c5 = Revision.newRevision(1).asBranchRevision();

        try {
            b.getModifiedPathsUntil(Revision.newRevision(1));
            fail("Must fail with IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        assertModifiedPaths(b.getModifiedPathsUntil(c1), "/foo");
        assertModifiedPaths(b.getModifiedPathsUntil(c2), "/foo", "/bar");
        assertModifiedPaths(b.getModifiedPathsUntil(c3), "/foo", "/bar");
        assertModifiedPaths(b.getModifiedPathsUntil(c4), "/foo", "/bar", "/baz");
        assertModifiedPaths(b.getModifiedPathsUntil(c5));
    }

    @Test
    public void orphanedBranchTest() {
        String rootId = Utils.getIdFromPath("/");
        MemoryDocumentStore store = new MemoryDocumentStore();
        DocumentNodeStore ns = builderProvider.newBuilder()
                .setDocumentStore(store).getNodeStore();
        NodeBuilder builder = ns.getRoot().builder();
        builder.setProperty("p", "v");
        for (int i = 0; ;i++) {
            builder.child("n-" + i);
            NodeDocument root = store.find(Collection.NODES, rootId);
            assertNotNull(root);
            if (!root.getLocalMap("p").isEmpty()) {
                // branch has been created
                break;
            }
        }
        ns.dispose();

        // start again
        ns = builderProvider.newBuilder()
                .setDocumentStore(store).getNodeStore();
        assertFalse(ns.getRoot().hasProperty("p"));
    }

    private void assertModifiedPaths(Iterable<String> actual, String... expected) {
        assertEquals(Sets.newHashSet(expected), Sets.newHashSet(actual));
    }
}
