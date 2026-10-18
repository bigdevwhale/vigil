"""Captures the README / Stillcraft screenshots from the test phone.

python tools/capture.py <out_dir> en|ru

Needs the `-Pdemo -Pshots` build installed (app.vigil.shots), freshly cleared: it seeds the demo
vows in the app's language on first start. Every tap first checks that no call is active and that
the app is in front, and stops otherwise.
"""
import os
import re
import subprocess
import sys
import time

ADB = os.path.join(os.environ["LOCALAPPDATA"], "Android", "Sdk", "platform-tools", "adb.exe")
PKG = "app.vigil.shots"

TEXT = {
    "en": {"sugar": "No sugar", "alcohol": "No alcohol", "tempt": "Temptation", "break": "I want to break it", "broke": "I already broke it",
           "catalog": "Catalog", "take": "Take this vow", "chronicle": "Chronicle"},
    "ru": {"sugar": "Без сахара", "alcohol": "Без алкоголя", "tempt": "Искушение", "break": "Хочу нарушить", "broke": "Я уже нарушил",
           "catalog": "Каталог", "take": "Дать этот обет", "chronicle": "Летопись"},
}


def adb(*args, binary=False):
    r = subprocess.run([ADB, *args], capture_output=True)
    return r.stdout if binary else r.stdout.decode("utf-8", "replace")


def guard():
    call = re.search(r"mCallState=(\d)", adb("shell", "dumpsys", "telephony.registry"))
    if not call or call.group(1) != "0":
        sys.exit("ABORT: a call is active")
    focus = re.search(r"mCurrentFocus=.*", adb("shell", "dumpsys", "window"))
    if not focus or PKG not in focus.group(0):
        sys.exit("ABORT: focus is " + (focus.group(0) if focus else "?"))


def find(text):
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    x = adb("exec-out", "cat", "/sdcard/ui.xml")
    for n in re.findall(r"<node [^>]*>", x):
        if 'text="' + text in n or 'content-desc="' + text in n:
            a = list(map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()))
            return (a[0] + a[2]) // 2, (a[1] + a[3]) // 2
    return None


def tap(text, wait=2.0, last=False):
    guard()
    hits = None
    if last:
        # The lowest match on screen (a list row rather than a heading).
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        x = adb("exec-out", "cat", "/sdcard/ui.xml")
        for n in re.findall(r"<node [^>]*>", x):
            if 'text="' + text in n:
                a = list(map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()))
                hits = ((a[0] + a[2]) // 2, (a[1] + a[3]) // 2)
    else:
        hits = find(text)
    if not hits:
        sys.exit("ABORT: no '%s' on screen" % text)
    adb("shell", "input", "tap", str(hits[0]), str(hits[1]))
    time.sleep(wait)


def back(wait=1.5):
    guard()
    adb("shell", "input", "keyevent", "4")
    time.sleep(wait)


def swipe(y1, y2, wait=1.5):
    guard()
    adb("shell", "input", "swipe", "540", str(y1), "540", str(y2), "400")
    time.sleep(wait)


def main():
    out, lang = sys.argv[1], sys.argv[2]
    t = TEXT[lang]
    suffix = ".ru" if lang == "ru" else ""
    os.makedirs(out, exist_ok=True)

    def shot(name):
        guard()
        with open(os.path.join(out, name + suffix + ".png"), "wb") as f:
            f.write(adb("exec-out", "screencap", "-p", binary=True))

    adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(6)
    shot("vows")
    tap(t["sugar"])
    shot("vow")
    back()
    tap(t["tempt"], last=True)
    tap(t["sugar"], wait=5)
    shot("temptation")
    back()
    tap(t["sugar"])
    tap(t["break"])
    shot("ladder")
    back()
    tap(t["broke"])
    shot("outcome")
    back()
    back()
    tap(t["catalog"], last=True)
    shot("catalog")
    tap(t["alcohol"], last=True)
    shot("preset")
    tap(t["take"])
    shot("make")
    back()
    back()
    tap(t["chronicle"], last=True)
    shot("chronicle")
    swipe(2000, 500)
    swipe(2000, 700)
    tap(t["alcohol"], last=True, wait=3)
    shot("page")
    back()


if __name__ == "__main__":
    main()
