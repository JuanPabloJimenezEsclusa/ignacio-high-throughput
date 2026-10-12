#!/usr/bin/env python3
"""Regenerate the imperative-vs-reactive comparison charts used by the Readme.

Reads the AWS run artifacts and writes one PNG per tool:

    testing/gatling/images/imperative-vs-reactive.png
    testing/k6/images/imperative-vs-reactive.png
    testing/jmeter/images/imperative-vs-reactive.png

Sources:
    Gatling  reports/aws/run-1/req_*.html   (Gatling OSS 3.15.1 emits no JSON report, so the
                                             per-endpoint percentiles are scraped from the request pages)
    k6       result/aws/run-1/summary.json  (metrics["http_req_duration{impl:...}"] and {group:...-smoke})
    JMeter   reports/aws/run-1/html/statistics.json  (medianResTime and pct2ResTime per label)

Usage:
    python3 testing/charts.py
"""

import html as html_mod
import json
import re
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402

REPO = Path(__file__).resolve().parent.parent
IMP = "#3b6fb6"
REA = "#d07a2c"
ENDPOINTS = ["/smokes", "/cpu", "/io", "/aggregate", "/resilience", "/stream"]

plt.rcParams.update({
    "figure.dpi": 140,
    "font.size": 9,
    "axes.edgecolor": "#999999",
    "axes.linewidth": 0.8,
    "axes.grid": True,
    "grid.color": "#dddddd",
    "grid.linewidth": 0.7,
    "axes.axisbelow": True,
    "font.family": "DejaVu Sans",
})


def gatling_run():
    out = {}
    for page in sorted((REPO / "testing/gatling/reports/aws/run-1").glob("req_*.html")):
        text = page.read_text(errors="ignore")
        title = re.search(r"<title>Gatling Stats - ([^<]+)</title>", text)
        if not title:
            continue
        arm, _, endpoint = html_mod.unescape(title.group(1)).strip().partition(" ")
        flat = re.sub(r"\s+", " ", text)

        def percentile(label):
            found = re.search(r'<td class="title">' + label + r'</td>\s*<td class="total">([^<]*)</td>', flat)
            value = found.group(1).strip().replace(",", "") if found else ""
            return int(value) if value.isdigit() else None

        out[(arm, endpoint)] = {"p50": percentile("50th percentile"), "p95": percentile("95th percentile")}
    return out


def k6_run():
    metrics = json.loads((REPO / "testing/k6/result/aws/run-1/summary.json").read_text())["metrics"]

    def read(key):
        values = metrics[key]
        values = values.get("values", values)  # recent k6 exports flat, older nest under "values"
        return {"median": values["med"], "p95": values["p(95)"], "max": values["max"]}

    return {
        "imperative": read("http_req_duration{impl:imperative}"),
        "reactive": read("http_req_duration{impl:reactive}"),
        "imperative_smoke": read("http_req_duration{group:imperative-smoke}"),
        "reactive_smoke": read("http_req_duration{group:reactive-smoke}"),
    }


def jmeter_run():
    stats = json.loads((REPO / "testing/jmeter/reports/aws/run-1/html/statistics.json").read_text())

    def read(needle):
        for label, row in stats.items():
            if needle in label.lower() and row.get("medianResTime") is not None:
                return {"median": row["medianResTime"], "p95": row["pct2ResTime"]}
        return None

    return {"imperative": read("imperative"), "reactive": read("reactive")}


def bars(ax, labels, series, title, ylabel):
    x = np.arange(len(labels), dtype=float)
    width = 0.36
    for index, (values, colour, name) in enumerate(series):
        offset = (index - (len(series) - 1) / 2) * width
        drawn = [(xi + offset, v) for xi, v in zip(x, values) if v is not None]
        handles = ax.bar([xi for xi, _ in drawn], [v for _, v in drawn], width,
                         color=colour, edgecolor="white", linewidth=0.6, label=name)
        for handle, (_, value) in zip(handles, drawn):
            ax.annotate(f"{value:,.0f}", (handle.get_x() + handle.get_width() / 2, value),
                        ha="center", va="bottom", fontsize=7.5, color="#333333",
                        xytext=(0, 2), textcoords="offset points")
    ax.set_xticks(x)
    ax.set_xticklabels(labels)
    ax.set_yscale("log")
    ax.set_title(title, fontsize=10.5, pad=10)
    ax.set_ylabel(ylabel)
    ax.legend(frameon=False)


def main():
    gatling, k6, jmeter = gatling_run(), k6_run(), jmeter_run()

    labels = [e + ("\n(reactive only)" if e == "/stream" else "") for e in ENDPOINTS]
    imp = [(gatling.get(("imperative", e)) or {}).get("p95") for e in ENDPOINTS]
    rea = [(gatling.get(("reactive", e)) or {}).get("p95") for e in ENDPOINTS]
    fig, ax = plt.subplots(figsize=(7.2, 3.4))
    bars(ax, labels, [(imp, IMP, "imperative"), (rea, REA, "reactive")],
         "Gatling on AWS: p95 latency by endpoint (100 users/s, 66 132 requests, 0 errors)",
         "p95 (ms, log scale)")
    fig.tight_layout()
    fig.savefig(REPO / "testing/gatling/images/imperative-vs-reactive.png")
    plt.close(fig)

    fig, ax = plt.subplots(figsize=(6.4, 3.4))
    bars(ax, ["median", "p95", "max"],
         [([k6["imperative"][s] for s in ("median", "p95", "max")], IMP, "imperative"),
          ([k6["reactive"][s] for s in ("median", "p95", "max")], REA, "reactive")],
         "k6 on AWS: latency by implementation (same 13-call workload)",
         "ms (log scale)")
    fig.tight_layout()
    fig.savefig(REPO / "testing/k6/images/imperative-vs-reactive.png")
    plt.close(fig)

    fig, ax = plt.subplots(figsize=(5.8, 3.4))
    bars(ax, ["JMeter\n7 000 threads", "Gatling\n100 users/s", "k6\nramping to 600 it/s"],
         [([jmeter["imperative"]["p95"], gatling[("imperative", "/smokes")]["p95"], k6["imperative_smoke"]["p95"]], IMP, "imperative"),
          ([jmeter["reactive"]["p95"], gatling[("reactive", "/smokes")]["p95"], k6["reactive_smoke"]["p95"]], REA, "reactive")],
         "/smokes p95 by tool (same endpoint on both arms)",
         "p95 (ms, log scale)")
    fig.tight_layout()
    fig.savefig(REPO / "testing/jmeter/images/imperative-vs-reactive.png")
    plt.close(fig)

    print("charts written")


if __name__ == "__main__":
    main()
