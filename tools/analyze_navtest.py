#!/usr/bin/env python3
"""
Navigation Stress Test Results Analyzer

Parses JSON output from NavigationStressTest bot and generates
a human-readable report with insights about navigation reliability.

Usage:
    python analyze_navtest.py <path_to_navtest_json>
    python analyze_navtest.py  # Uses most recent navtest_*.json in AppData
"""

import json
import sys
import os
import glob
from datetime import datetime, timedelta
from collections import defaultdict
from pathlib import Path


def find_latest_navtest():
    """Find the most recent navtest JSON file in AppData."""
    appdata = os.environ.get('APPDATA', '')
    if not appdata:
        # WSL fallback
        appdata = '/mnt/c/Users/imbecil/AppData/Roaming'

    hnh_dir = os.path.join(appdata, 'Haven and Hearth')
    pattern = os.path.join(hnh_dir, 'navtest_*.json')
    files = glob.glob(pattern)

    if not files:
        # Try parent directory
        pattern = os.path.join(hnh_dir, '..', 'navtest_*.json')
        files = glob.glob(pattern)

    if not files:
        return None

    # Return most recently modified
    return max(files, key=os.path.getmtime)


def parse_duration(ms):
    """Convert milliseconds to human-readable duration."""
    if ms < 1000:
        return f"{ms}ms"
    elif ms < 60000:
        return f"{ms/1000:.1f}s"
    elif ms < 3600000:
        mins = ms // 60000
        secs = (ms % 60000) // 1000
        return f"{mins}m {secs}s"
    else:
        hours = ms // 3600000
        mins = (ms % 3600000) // 60000
        return f"{hours}h {mins}m"


def parse_timestamp(ts):
    """Parse ISO timestamp or epoch ms."""
    if isinstance(ts, str):
        # ISO format
        try:
            return datetime.fromisoformat(ts.replace('Z', '+00:00'))
        except:
            return None
    elif isinstance(ts, (int, float)):
        # Epoch milliseconds
        return datetime.fromtimestamp(ts / 1000)
    return None


def analyze_results(data):
    """Analyze the test results and return insights."""
    results = {
        'summary': {},
        'failures': {},
        'performance': {},
        'routes': {},
        'problem_areas': {},
        'timeline': {}
    }

    tests = data.get('tests', [])
    run_info = data.get('runInfo', {})

    if not tests:
        return results

    # === SUMMARY ===
    total = len(tests)
    passed = sum(1 for t in tests if t.get('success'))
    skipped = sum(1 for t in tests if t.get('skipped'))
    failed = total - passed - skipped
    actual_tests = total - skipped

    results['summary'] = {
        'total': total,
        'passed': passed,
        'failed': failed,
        'skipped': skipped,
        'pass_rate': (passed / actual_tests * 100) if actual_tests > 0 else 0,
        'start_time': run_info.get('startTime', 'Unknown'),
        'end_time': run_info.get('endTime', 'Unknown'),
    }

    # Calculate runtime
    start = parse_timestamp(run_info.get('startTime'))
    end = parse_timestamp(run_info.get('endTime'))
    if start and end:
        runtime = end - start
        results['summary']['runtime'] = str(runtime).split('.')[0]  # Remove microseconds

    # === FAILURE ANALYSIS ===
    failure_reasons = defaultdict(list)
    for t in tests:
        if not t.get('success') and not t.get('skipped'):
            reason = t.get('failureReason', 'UNKNOWN')
            failure_reasons[reason].append({
                'id': t.get('id'),
                'from': t.get('fromAreaName', 'Unknown'),
                'to': t.get('toAreaName', 'Unknown'),
                'details': t.get('failureDetails', ''),
                'duration': t.get('durationMs', 0)
            })

    results['failures'] = {
        'by_reason': {k: len(v) for k, v in failure_reasons.items()},
        'details': dict(failure_reasons)
    }

    # === PERFORMANCE ===
    successful_times = [t.get('durationMs', 0) for t in tests if t.get('success')]
    if successful_times:
        results['performance'] = {
            'avg_success_time': sum(successful_times) / len(successful_times),
            'min_time': min(successful_times),
            'max_time': max(successful_times),
            'median_time': sorted(successful_times)[len(successful_times)//2],
        }

        # Find slowest successful navigations
        slow_tests = sorted(
            [t for t in tests if t.get('success')],
            key=lambda x: x.get('durationMs', 0),
            reverse=True
        )[:5]
        results['performance']['slowest'] = [
            {
                'id': t.get('id'),
                'from': t.get('fromAreaName'),
                'to': t.get('toAreaName'),
                'duration': t.get('durationMs')
            }
            for t in slow_tests
        ]

    # === ROUTE ANALYSIS ===
    route_stats = defaultdict(lambda: {'passed': 0, 'failed': 0, 'skipped': 0, 'times': []})
    for t in tests:
        route = f"{t.get('fromAreaName', '?')} -> {t.get('toAreaName', '?')}"
        if t.get('skipped'):
            route_stats[route]['skipped'] += 1
        elif t.get('success'):
            route_stats[route]['passed'] += 1
            route_stats[route]['times'].append(t.get('durationMs', 0))
        else:
            route_stats[route]['failed'] += 1

    results['routes'] = dict(route_stats)

    # === PROBLEM AREAS ===
    # Areas that fail often as destinations
    dest_failures = defaultdict(int)
    dest_attempts = defaultdict(int)
    for t in tests:
        dest = t.get('toAreaName', 'Unknown')
        if not t.get('skipped'):
            dest_attempts[dest] += 1
            if not t.get('success'):
                dest_failures[dest] += 1

    problem_dests = []
    for dest, failures in dest_failures.items():
        attempts = dest_attempts[dest]
        if attempts >= 2:  # At least 2 attempts
            fail_rate = failures / attempts * 100
            if fail_rate > 0:  # Any failures
                problem_dests.append({
                    'area': dest,
                    'failures': failures,
                    'attempts': attempts,
                    'fail_rate': fail_rate
                })

    results['problem_areas'] = sorted(problem_dests, key=lambda x: x['fail_rate'], reverse=True)

    # === TIMELINE ===
    # Group by 10-minute windows to see trends
    time_buckets = defaultdict(lambda: {'passed': 0, 'failed': 0})
    for t in tests:
        ts = t.get('startTime')
        if ts and not t.get('skipped'):
            dt = parse_timestamp(ts)
            if dt:
                # Round to 10-minute bucket
                bucket = dt.replace(minute=(dt.minute // 10) * 10, second=0, microsecond=0)
                bucket_key = bucket.strftime('%H:%M')
                if t.get('success'):
                    time_buckets[bucket_key]['passed'] += 1
                else:
                    time_buckets[bucket_key]['failed'] += 1

    results['timeline'] = dict(sorted(time_buckets.items()))

    return results


def print_report(analysis):
    """Print a human-readable report."""

    print("\n" + "=" * 70)
    print("  NAVIGATION STRESS TEST REPORT")
    print("=" * 70)

    # === SUMMARY ===
    s = analysis['summary']
    print("\n## SUMMARY")
    print("-" * 40)
    print(f"  Total tests:     {s.get('total', 0)}")
    print(f"  Passed:          {s.get('passed', 0)}")
    print(f"  Failed:          {s.get('failed', 0)}")
    print(f"  Skipped:         {s.get('skipped', 0)} (no path exists)")
    print(f"  Pass rate:       {s.get('pass_rate', 0):.1f}%")
    print()
    print(f"  Started:         {s.get('start_time', 'Unknown')}")
    print(f"  Ended:           {s.get('end_time', 'Unknown')}")
    if 'runtime' in s:
        print(f"  Runtime:         {s['runtime']}")

    # === FAILURE BREAKDOWN ===
    failures = analysis.get('failures', {})
    by_reason = failures.get('by_reason', {})
    if by_reason:
        print("\n## FAILURE BREAKDOWN")
        print("-" * 40)
        for reason, count in sorted(by_reason.items(), key=lambda x: -x[1]):
            print(f"  {reason}: {count}")

        # Show details for each failure type
        details = failures.get('details', {})
        for reason, tests in details.items():
            if tests:
                print(f"\n  [{reason}] Examples:")
                for t in tests[:3]:  # Show up to 3 examples
                    print(f"    - Test #{t['id']}: {t['from']} -> {t['to']}")
                    if t['details']:
                        print(f"      {t['details'][:80]}")

    # === PERFORMANCE ===
    perf = analysis.get('performance', {})
    if perf:
        print("\n## PERFORMANCE (successful navigations)")
        print("-" * 40)
        print(f"  Average time:    {parse_duration(perf.get('avg_success_time', 0))}")
        print(f"  Fastest:         {parse_duration(perf.get('min_time', 0))}")
        print(f"  Slowest:         {parse_duration(perf.get('max_time', 0))}")
        print(f"  Median:          {parse_duration(perf.get('median_time', 0))}")

        slowest = perf.get('slowest', [])
        if slowest:
            print("\n  Slowest navigations:")
            for t in slowest:
                print(f"    - {t['from']} -> {t['to']}: {parse_duration(t['duration'])}")

    # === PROBLEM AREAS ===
    problems = analysis.get('problem_areas', [])
    if problems:
        print("\n## PROBLEM DESTINATIONS")
        print("-" * 40)
        print(f"  {len(problems)} areas with navigation failures:\n")

        # Group by failure rate
        total_fail = [p for p in problems if p['fail_rate'] == 100]
        partial_fail = [p for p in problems if p['fail_rate'] < 100]

        if total_fail:
            print(f"  === 100% FAILURE ({len(total_fail)} areas) ===")
            for p in total_fail:
                print(f"    {p['area']} ({p['failures']} attempts)")

        if partial_fail:
            print(f"\n  === PARTIAL FAILURES ({len(partial_fail)} areas) ===")
            for p in partial_fail:
                print(f"    {p['area']}: {p['failures']}/{p['attempts']} failed ({p['fail_rate']:.0f}%)")

    # === ROUTE SUCCESS RATES ===
    routes = analysis.get('routes', {})
    if routes:
        # Find routes with failures
        failed_routes = [
            (route, stats) for route, stats in routes.items()
            if stats['failed'] > 0
        ]
        failed_routes.sort(key=lambda x: x[1]['failed'], reverse=True)

        if failed_routes:
            print("\n## ROUTES WITH FAILURES")
            print("-" * 40)
            for route, stats in failed_routes[:10]:
                total = stats['passed'] + stats['failed']
                rate = stats['passed'] / total * 100 if total > 0 else 0
                print(f"  {route}")
                print(f"    {stats['passed']} passed, {stats['failed']} failed ({rate:.0f}% success)")

    # === TIMELINE ===
    timeline = analysis.get('timeline', {})
    if timeline and len(timeline) > 1:
        print("\n## TIMELINE (10-min buckets)")
        print("-" * 40)

        # Find any periods with high failure rates
        bad_periods = []
        for time, stats in timeline.items():
            total = stats['passed'] + stats['failed']
            if total >= 2 and stats['failed'] > 0:
                fail_rate = stats['failed'] / total * 100
                if fail_rate >= 20:  # 20% or more failures
                    bad_periods.append((time, fail_rate, stats))

        if bad_periods:
            print("  Periods with elevated failures:")
            for time, rate, stats in bad_periods:
                print(f"    {time}: {stats['failed']} failures ({rate:.0f}% fail rate)")
        else:
            print("  No periods with elevated failure rates detected.")

        # Show overall trend
        times = list(timeline.keys())
        if len(times) >= 4:
            first_half = times[:len(times)//2]
            second_half = times[len(times)//2:]

            first_stats = {'p': 0, 'f': 0}
            second_stats = {'p': 0, 'f': 0}

            for t in first_half:
                first_stats['p'] += timeline[t]['passed']
                first_stats['f'] += timeline[t]['failed']
            for t in second_half:
                second_stats['p'] += timeline[t]['passed']
                second_stats['f'] += timeline[t]['failed']

            first_rate = first_stats['p'] / (first_stats['p'] + first_stats['f']) * 100 if (first_stats['p'] + first_stats['f']) > 0 else 0
            second_rate = second_stats['p'] / (second_stats['p'] + second_stats['f']) * 100 if (second_stats['p'] + second_stats['f']) > 0 else 0

            print(f"\n  Trend:")
            print(f"    First half:  {first_rate:.1f}% pass rate")
            print(f"    Second half: {second_rate:.1f}% pass rate")
            if abs(first_rate - second_rate) > 5:
                if second_rate < first_rate:
                    print("    --> Performance may be degrading over time")
                else:
                    print("    --> Performance improving over time")

    # === VERDICT ===
    print("\n" + "=" * 70)
    print("  VERDICT")
    print("=" * 70)

    pass_rate = s.get('pass_rate', 0)
    failed = s.get('failed', 0)

    if failed == 0:
        print("\n  EXCELLENT - No failures detected!")
    elif pass_rate >= 95:
        print(f"\n  GOOD - {pass_rate:.1f}% pass rate, {failed} minor issues")
    elif pass_rate >= 80:
        print(f"\n  ACCEPTABLE - {pass_rate:.1f}% pass rate, review failures")
    else:
        print(f"\n  NEEDS ATTENTION - {pass_rate:.1f}% pass rate")
        print("  Review the failure breakdown above for patterns.")

    print()


def main():
    # Find or use specified file
    if len(sys.argv) > 1:
        filepath = sys.argv[1]
    else:
        filepath = find_latest_navtest()
        if not filepath:
            print("No navtest_*.json file found.")
            print("Usage: python analyze_navtest.py <path_to_json>")
            sys.exit(1)
        print(f"Using: {filepath}")

    # Load and analyze
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"File not found: {filepath}")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Invalid JSON: {e}")
        sys.exit(1)

    analysis = analyze_results(data)
    print_report(analysis)


if __name__ == '__main__':
    main()
