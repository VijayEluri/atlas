package org.atlasapi.remotesite.wikipedia.football;

import de.fau.cs.osr.ptk.common.AstVisitor;
import de.fau.cs.osr.ptk.common.ast.AstNode;
import org.atlasapi.remotesite.wikipedia.wikiparsers.SwebleHelper;
import org.sweble.wikitext.lazy.parser.*;
import xtc.parser.ParseException;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

public class EuropeanTeamListScraper extends AstVisitor {

    public static Collection<String> extractNames(String indexText) throws IOException, ParseException {
        AstNode indexAST = SwebleHelper.parse(indexText);

        Visitor v = new Visitor();
        v.go(indexAST);
        return v.list;
    }

    protected static final class Visitor extends AstVisitor {
        LinkedList<String> list = new LinkedList<String>();

        public void visit(LazyParsedPage p) {
            iterate(p.getContent());
        }

        public void visit(Section s) {
            if ("Teams".equalsIgnoreCase(SwebleHelper.flattenTextNodeList(s.getTitle()))) {
                iterate(s.getBody());
            }
        }

        public void visit(Table t) {
            iterate(t.getBody());
        }

        public void visit(TableRow t) {
            iterate(t.getBody());
        }

        public void visit(TableCell c) {
            iterate(c.getBody());
        }

        public void visit(InternalLink l) {
            String target = l.getTarget();
            if (target.contains("UEFA")
                || target.contains("#")
                || target.contains("European")
                || target.startsWith("UTC")) {
                return;
            }
            list.add(target);
        }

        @Override
        protected Object visitNotFound(AstNode node) { return null; }
    }
}
