/*
 * Copyright 2004-2008 H2 Group. Multiple-Licensed under the H2 License, 
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.bnf;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents a sequence of BNF rules, or a list of alternative rules.
 */
public class RuleList implements Rule {

    private boolean or;
    private ArrayList list;
    private boolean mapSet;
    
    public String toString() {
        StringBuffer buff = new StringBuffer();
        if (or) {
            buff.append("{");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    buff.append("|");
                }
                buff.append(list.get(i).toString());
            }
            buff.append("}");
        } else {
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    buff.append(" ");
                }
                buff.append(list.get(i).toString());
            }
        }
        return buff.toString();
    }

    RuleList(Rule first, Rule next, boolean or) {
        list = new ArrayList();
        if (first instanceof RuleList && ((RuleList) first).or == or) {
            list.addAll(((RuleList) first).list);
        } else {
            list.add(first);
        }
        if (next instanceof RuleList && ((RuleList) next).or == or) {
            list.addAll(((RuleList) next).list);
        } else {
            list.add(next);
        }
        if (!or && Bnf.COMBINE_KEYWORDS) {
            for (int i = 0; i < list.size() - 1; i++) {
                Rule r1 = (Rule) list.get(i);
                Rule r2 = (Rule) list.get(i + 1);
                if (!(r1 instanceof RuleElement) || !(r2 instanceof RuleElement)) {
                    continue;
                }
                RuleElement re1 = (RuleElement) r1;
                RuleElement re2 = (RuleElement) r2;
                if (!re1.isKeyword() || !re2.isKeyword()) {
                    continue;
                }
                re1 = re1.merge(re2);
                list.set(i, re1);
                list.remove(i + 1);
                i--;
            }
        }
        this.or = or;
    }

    public String random(Bnf config, int level) {
        if (or) {
            if (level > 10) {
                if (level > 1000) {
                    // better than stack overflow
                    throw new Error();
                }
                return get(0).random(config, level);
            }
            int idx = config.getRandom().nextInt(list.size());
            return get(idx).random(config, level + 1);
        } else {
            StringBuffer buff = new StringBuffer();
            for (int i = 0; i < list.size(); i++) {
                buff.append(get(i).random(config, level+1));
            }
            return buff.toString();
        }
    }

    private Rule get(int idx) {
        return ((Rule) list.get(idx));
    }

    public String name() {
        return null;
    }
    
    public Rule last() {
        return get(list.size() - 1);
    }

    public void setLinks(HashMap ruleMap) {
        if (!mapSet) {
            for (int i = 0; i < list.size(); i++) {
                get(i).setLinks(ruleMap);
            }
            mapSet = true;
        }
    }

    public boolean matchRemove(Sentence sentence) {
        String query = sentence.query;
        if (query.length() == 0) {
            return false;
        }
        if (or) {
            for (int i = 0; i < list.size(); i++) {
                if (get(i).matchRemove(sentence)) {
                    return true;
                }
            }
            return false;
        } else {
            for (int i = 0; i < list.size(); i++) {
                Rule r = get(i);
                if (!r.matchRemove(sentence)) {
                    return false;
                }
            }
            return true;
        }
    }

    public void addNextTokenList(Sentence sentence) {
        String old = sentence.query;
        if (or) {
            for (int i = 0; i < list.size(); i++) {
                sentence.setQuery(old);
                Rule r = get(i);
                r.addNextTokenList(sentence);
            }
        } else {
            for (int i = 0; i < list.size(); i++) {
                Rule r = get(i);
                r.addNextTokenList(sentence);
                if (!r.matchRemove(sentence)) {
                    break;
                }
            }
        }
        sentence.setQuery(old);
    }

}