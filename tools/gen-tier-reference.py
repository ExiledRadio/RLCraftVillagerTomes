"""Rebuild the bounty band sections of tier-reference.html from ModConfig.java defaults.

Only the three <section class="band ..."> blocks, the bounty count, the version eyebrow
and the footer are rewritten; the rest of the page (styles, catalyst table, notes,
filter script) is left exactly as it is.

    python tools/gen-tier-reference.py
"""
import html
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONFIG = os.path.join(ROOT, "src", "main", "java", "com", "exiledradio",
                      "rlcraftvillagertomes", "ModConfig.java")
PAGE = os.path.join(ROOT, "tier-reference.html")
PROPS = os.path.join(ROOT, "gradle.properties")


def string_array(src, name):
    body = re.search(r"String\[\] " + name + r" = \{(.*?)\n    \};", src, re.S).group(1)
    return re.findall(r'^\s*"([^"]+)",', body, re.M)


def main():
    src = open(CONFIG, encoding="utf-8").read()
    tiers = {}
    for line in string_array(src, "BOUNTY_TIERS"):
        name, rng = line.split("=")
        tiers[name] = rng
    items = []
    for line in string_array(src, "BOUNTY_ITEMS"):
        spec, rest = line.split("=", 1)
        parts = rest.split(",")
        items.append((spec, parts[0], parts[1] if len(parts) > 1 else None))

    sections = []
    for tier, rng in tiers.items():
        members = [i for i in items if i[1] == tier]
        by_ns = {}
        for spec, _, qty in members:
            by_ns.setdefault(spec.split(":")[0], []).append((spec, qty))
        order = sorted(by_ns, key=lambda ns: (ns != "minecraft", ns))
        lo, hi = rng.split("-")
        out = ['<section class="band t-%s"><header class="bandhead"><h3>%s</h3>'
               '<p class="bandmeta"><span class="range">%s&ndash;%s</span> per request '
               '<span class="dot">&middot;</span> %d items</p></header>'
               % (tier, tier, lo, hi, len(members))]
        for i, ns in enumerate(order):
            chips = []
            for spec, qty in by_ns[ns]:
                path = spec.split(":")[1]
                words = path.replace("_", " ")
                if qty:
                    q = '%s <span class="ov" title="quantity overridden for this item">*</span>' % qty
                else:
                    q = rng
                chips.append('<li class="chip" data-search="%s %s"><code>%s</code>'
                             '<span class="qty">%s</span></li>'
                             % (html.escape(spec), html.escape(words), html.escape(spec), q))
            out.append('%s<div class="ns"><h4>%s <span class="nscount">%d</span></h4>'
                       '<ul class="chips">%s</ul></div>'
                       % ("" if i == 0 else "\n", ns, len(chips), "\n".join(chips)))
        out.append("</section>")
        sections.append("".join(out))

    cat_tiers = []
    for line in string_array(src, "CATALYST_TIERS"):
        name, value = line.split("=")
        cat_tiers.append((name, float(value.split(",")[0])))
    cat_items = {}
    for line in string_array(src, "CATALYST_ITEMS"):
        spec, tier = line.split("=")
        cat_items.setdefault(tier, []).append(spec)
    top = max(v for _, v in cat_tiers) or 1.0
    rows = []
    for name, value in cat_tiers:
        members = cat_items.get(name)
        cell = (" ".join("<code>%s</code>" % html.escape(m) for m in members)
                if members else '<span class="none">&mdash;</span>')
        shown = ("%g" % value) + "%"
        rows.append('<tr><th scope="row"><code>%s</code></th><td class="num">%s</td>'
                    '<td class="barcell"><span class="bar" style="width:%.1f%%"></span></td>'
                    '<td class="members">%s</td></tr>' % (name, shown, value * 100.0 / top, cell))
    catalyst_body = "<tbody>" + "\n".join(rows) + "</tbody>"

    page = open(PAGE, encoding="utf-8").read()
    head = page.index("<h2>Catalysts")
    body_start = page.index("<tbody>", head)
    body_end = page.index("</tbody>", body_start) + len("</tbody>")
    page = page[:body_start] + catalyst_body + page[body_end:]
    page = re.sub(r'<p class="note">Catalysts are banked into a villager.*?</p>',
                  '<p class="note">Catalysts only matter with the optional chance system on '
                  '(<code>ENABLE_CHANCE</code>, off by default). They are banked into a villager '
                  'before a book is offered, and are used up whether the attempt succeeds or '
                  'fails. Bands with no items are still defined and available &mdash;\n'
                  'nothing is hard-coded, so anything from any mod can be assigned to one.</p>',
                  page, count=1, flags=re.S)
    page = re.sub(r'<section class="band t-low">.*<section class="band t-high">.*?</section>',
                  lambda m: "\n".join(sections), page, count=1, flags=re.S)
    total = len(items)
    page = re.sub(r'<b>\d+</b><span>bounty items', '<b>%d</b><span>bounty items' % total, page)
    version = re.search(r"^mod_version=(.+)$", open(PROPS).read(), re.M).group(1).strip()
    page = re.sub(r'(RLCraft Villager Tomes &middot; )[\d.]+', r'\g<1>' + version, page)
    page = re.sub(r'<footer>.*?</footer>',
                  '<footer>Generated from <code>ModConfig.java</code> defaults by '
                  '<code>tools/gen-tier-reference.py</code> &middot; %d bounty entries. Entries '
                  'naming a mod that is not installed are skipped at startup.</footer>' % total,
                  page, count=1, flags=re.S)
    open(PAGE, "w", encoding="utf-8", newline="").write(page)
    print("tier-reference.html:", total, "bounty items")


if __name__ == "__main__":
    main()
