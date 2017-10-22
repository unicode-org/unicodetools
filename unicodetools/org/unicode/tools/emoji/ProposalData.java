package org.unicode.tools.emoji;

import java.util.Collection;
import java.util.Map.Entry;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.text.utility.Utility;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UTF16;
import com.google.common.collect.Multimap;

public class ProposalData {
    static ProposalData SINGLETON = new ProposalData();
    
    public static ProposalData getInstance() {
        return SINGLETON;
    }
    
    final Multimap<String, String> proposal = load();
    
    private ProposalData() {}

    public String getProposalHtml(String source) {
        // later add http://www.unicode.org/cgi-bin/GetMatchingDocs.pl?L2/17-023
        StringBuilder result = new StringBuilder();
        source = source.replace(Emoji.EMOJI_VARIANT_STRING, "");
        Collection<String> proposals = getProposal(source);
        if (proposals.isEmpty()) {
            proposals = proposal.get(new StringBuilder().appendCodePoint(source.charAt(0)).toString());
        }
        for (String proposalItem :  proposals) {
            if (result.length() != 0) {
                result.append(", ");
            }
            result.append("<a target='e-prop' href='http://www.unicode.org/cgi-bin/GetMatchingDocs.pl?" + proposalItem.replace('\u2011', '-') + "'>"
                    + proposalItem + "</a>");
        }
        return result.toString();
    }

    private Collection<String> getProposal(String source) {
        Collection<String> result = proposal.get(source);
        if (result.isEmpty()) {
            for (int cp : CharSequences.codePoints(source)) {
                result = proposal.get(UTF16.valueOf(cp));
                if (!result.isEmpty()) {
                    break;
                }
            }
        }
        return result;
    }

    static Multimap<String, String> load() {
        Builder<String, String> builder = ImmutableMultimap.builder();
        for (String line : FileUtilities.in(ProposalData.class, "proposalData.txt")) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue;
            String[] parts = line.split(";");
            String code = Utility.fromHex(parts[0]).replace(Emoji.EMOJI_VARIANT_STRING, "");
            for (String proposal : parts[1].split(",")) {
                builder.put(code, proposal.trim());
            }
        }
        return builder.build();
    }
    
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (Entry<String, Collection<String>> entry : proposal.asMap().entrySet()) {
            if (out.length() != 0) {
               out.append("\n"); 
            }
            out.append(Utility.hex(entry.getKey()) + "\t" + entry.getValue());
        }
        return out.toString();
    }
    
    public static void main(String[] args) {
        System.out.println(getInstance());
    }
}
