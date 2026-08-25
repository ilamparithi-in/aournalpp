#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <ctype.h>
#include <unistd.h>
#include <poll.h>
#include <X11/Xlib.h>
#include <X11/Xatom.h>
#include <X11/Xutil.h>

static int ignore_x_errors(Display *dpy, XErrorEvent *err) {
    (void)dpy;
    (void)err;
    // Suppress asynchronous BadWindow / BadDrawable errors when dialogs close
    return 0;
}

static char last_emitted_title[1024] = "";

static Atom net_wm_name = None;
static Atom net_wm_visible_name = None;
static Atom net_wm_window_type = None;
static Atom net_wm_window_type_normal = None;
static Atom net_wm_window_type_dialog = None;
static Atom net_wm_window_type_utility = None;
static Atom net_wm_window_type_toolbar = None;
static Atom net_wm_window_type_menu = None;
static Atom net_wm_window_type_dropdown_menu = None;
static Atom net_wm_window_type_popup_menu = None;
static Atom net_wm_window_type_tooltip = None;
static Atom net_wm_window_type_notification = None;
static Atom net_wm_window_type_combo = None;
static Atom net_wm_window_type_dnd = None;
static Atom net_wm_window_type_splash = None;
static Atom net_wm_window_type_dock = None;
static Atom net_wm_window_type_desktop = None;
static Atom net_active = None;
static Atom net_client_list = None;

static void init_atoms(Display *dpy) {
    net_wm_name = XInternAtom(dpy, "_NET_WM_NAME", False);
    net_wm_visible_name = XInternAtom(dpy, "_NET_WM_VISIBLE_NAME", False);
    net_wm_window_type = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE", False);
    net_wm_window_type_normal = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_NORMAL", False);
    net_wm_window_type_dialog = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_DIALOG", False);
    net_wm_window_type_utility = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_UTILITY", False);
    net_wm_window_type_toolbar = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_TOOLBAR", False);
    net_wm_window_type_menu = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_MENU", False);
    net_wm_window_type_dropdown_menu = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_DROPDOWN_MENU", False);
    net_wm_window_type_popup_menu = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_POPUP_MENU", False);
    net_wm_window_type_tooltip = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_TOOLTIP", False);
    net_wm_window_type_notification = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_NOTIFICATION", False);
    net_wm_window_type_combo = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_COMBO", False);
    net_wm_window_type_dnd = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_DND", False);
    net_wm_window_type_splash = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_SPLASH", False);
    net_wm_window_type_dock = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_DOCK", False);
    net_wm_window_type_desktop = XInternAtom(dpy, "_NET_WM_WINDOW_TYPE_DESKTOP", False);
    net_active = XInternAtom(dpy, "_NET_ACTIVE_WINDOW", False);
    net_client_list = XInternAtom(dpy, "_NET_CLIENT_LIST", False);
}

static char *case_str_search(const char *haystack, const char *needle) {
    if (!haystack || !needle) return NULL;
    size_t needle_len = strlen(needle);
    if (needle_len == 0) return (char *)haystack;
    while (*haystack) {
        if (strncasecmp(haystack, needle, needle_len) == 0) {
            return (char *)haystack;
        }
        haystack++;
    }
    return NULL;
}

static char *get_window_title(Display *dpy, Window w) {
    if (!dpy || !w || w == DefaultRootWindow(dpy)) return NULL;

    Atom type;
    int format;
    unsigned long nitems = 0, bytes_after = 0;
    unsigned char *prop = NULL;
    char *title = NULL;

    // 1. Try _NET_WM_NAME (UTF-8)
    if (net_wm_name != None &&
        XGetWindowProperty(dpy, w, net_wm_name, 0, 1024, False, AnyPropertyType,
                           &type, &format, &nitems, &bytes_after, &prop) == Success && prop) {
        if (nitems > 0 && prop[0] != '\0') {
            char *buf = (char *)malloc(nitems + 1);
            if (buf) {
                memcpy(buf, prop, nitems);
                buf[nitems] = '\0';
                title = buf;
            }
        }
        XFree(prop);
        if (title) return title;
    }

    // 2. Try _NET_WM_VISIBLE_NAME
    if (net_wm_visible_name != None &&
        XGetWindowProperty(dpy, w, net_wm_visible_name, 0, 1024, False, AnyPropertyType,
                           &type, &format, &nitems, &bytes_after, &prop) == Success && prop) {
        if (nitems > 0 && prop[0] != '\0') {
            char *buf = (char *)malloc(nitems + 1);
            if (buf) {
                memcpy(buf, prop, nitems);
                buf[nitems] = '\0';
                title = buf;
            }
        }
        XFree(prop);
        if (title) return title;
    }

    // 3. Fallback to standard WM_NAME
    char *name = NULL;
    if (XFetchName(dpy, w, &name) > 0 && name && name[0] != '\0') {
        title = strdup(name);
        XFree(name);
        return title;
    }

    return NULL;
}

static int is_transient_window(Display *dpy, Window w) {
    Window prop_w = None;
    if (XGetTransientForHint(dpy, w, &prop_w) != 0 && prop_w != None && prop_w != DefaultRootWindow(dpy)) {
        return 1;
    }
    return 0;
}

static int is_dialog_or_non_document_window_type(Display *dpy, Window w) {
    if (net_wm_window_type == None) return 0;

    Atom type;
    int format;
    unsigned long nitems = 0, bytes_after = 0;
    unsigned char *prop = NULL;

    if (XGetWindowProperty(dpy, w, net_wm_window_type, 0, 32, False, XA_ATOM,
                           &type, &format, &nitems, &bytes_after, &prop) == Success && prop) {
        if (type == XA_ATOM && format == 32) {
            Atom *atoms = (Atom *)prop;
            for (unsigned long i = 0; i < nitems; i++) {
                Atom a = atoms[i];
                if (a == net_wm_window_type_dialog ||
                    a == net_wm_window_type_utility ||
                    a == net_wm_window_type_toolbar ||
                    a == net_wm_window_type_menu ||
                    a == net_wm_window_type_dropdown_menu ||
                    a == net_wm_window_type_popup_menu ||
                    a == net_wm_window_type_tooltip ||
                    a == net_wm_window_type_notification ||
                    a == net_wm_window_type_combo ||
                    a == net_wm_window_type_dnd ||
                    a == net_wm_window_type_splash ||
                    a == net_wm_window_type_dock ||
                    a == net_wm_window_type_desktop) {
                    XFree(prop);
                    return 1;
                }
            }
        }
        XFree(prop);
    }
    return 0;
}

static int is_dialog_or_ignored_title(const char *title) {
    if (!title || title[0] == '\0') return 1;

    // Skip leading asterisks and whitespace
    const char *p = title;
    while (*p == '*' || isspace((unsigned char)*p)) p++;
    if (*p == '\0') return 1;

    // Check exact matches
    const char *exact_ignored[] = {
        "openbox",
        "desktop",
        "x11",
        "xournal++",
        "com.github.xournalpp.xournalpp",
        "preferences",
        "xournal++ preferences",
        "about xournal++",
        "about",
        "plugin manager",
        "manage plugins",
        "page background",
        "set page background",
        "paper format",
        "select font",
        "font selection",
        "choose font",
        "font",
        "select color",
        "color selection",
        "choose color",
        "custom color",
        "color",
        "export as pdf",
        "export pdf",
        "export as...",
        "export as",
        "export",
        "save file",
        "save as",
        "save document",
        "save",
        "open file",
        "open document",
        "open",
        "select folder",
        "choose folder",
        "select destination folder",
        "question",
        "warning",
        "error",
        "information",
        "confirm",
        "print",
        "page setup",
        "insert text",
        "edit text",
        "latex",
        NULL
    };

    for (int i = 0; exact_ignored[i] != NULL; i++) {
        if (strcasecmp(p, exact_ignored[i]) == 0) return 1;
    }

    // Substring / prefix checks
    if (case_str_search(p, "preferences") != NULL && case_str_search(p, ".xopp") == NULL) return 1;
    if (case_str_search(p, "about xournal++") != NULL) return 1;
    if (case_str_search(p, "plugin manager") != NULL) return 1;
    if (case_str_search(p, "font selection") != NULL) return 1;
    if (case_str_search(p, "color selection") != NULL) return 1;
    if (case_str_search(p, "page background") != NULL) return 1;
    if (case_str_search(p, "save changes") != NULL) return 1;
    if (case_str_search(p, "error saving") != NULL) return 1;
    if (case_str_search(p, "error loading") != NULL) return 1;

    // Dialog titles starting with "Xournal++ " (e.g. "Xournal++ Preferences", "Xournal++ Warning")
    // Note: Document windows in Xournal++ format title as "<filename> - Xournal++" (ends with "- Xournal++")
    if (strncasecmp(p, "xournal++ ", 10) == 0 && case_str_search(p, "- xournal++") == NULL) {
        return 1;
    }

    return 0;
}

static int is_xournalpp_window(Display *dpy, Window w) {
    XClassHint ch;
    memset(&ch, 0, sizeof(ch));
    int match = 0;
    if (XGetClassHint(dpy, w, &ch)) {
        if (ch.res_name && strcasecmp(ch.res_name, "xournalpp") == 0) {
            match = 1;
        } else if (ch.res_class && strcasecmp(ch.res_class, "xournalpp") == 0) {
            match = 1;
        }
        if (ch.res_name) XFree(ch.res_name);
        if (ch.res_class) XFree(ch.res_class);
    }
    return match;
}

static int score_candidate_window(Display *dpy, Window w, char **out_title) {
    *out_title = NULL;
    if (!dpy || !w || w == DefaultRootWindow(dpy)) return -1;

    if (is_transient_window(dpy, w)) return -1;
    if (is_dialog_or_non_document_window_type(dpy, w)) return -1;

    char *title = get_window_title(dpy, w);
    if (!title || title[0] == '\0') {
        if (title) free(title);
        return -1;
    }

    if (is_dialog_or_ignored_title(title)) {
        free(title);
        return -1;
    }

    int score = 10;
    int is_xopp = is_xournalpp_window(dpy, w);
    if (is_xopp) score += 30;

    // Check if title contains standard Xournal++ app suffix: "- Xournal++"
    if (case_str_search(title, "- xournal++") != NULL) {
        score += 100;
    }
    if (case_str_search(title, "[autosaved]") != NULL) {
        score += 20;
    }

    *out_title = title;
    return score;
}

static void scan_window_tree(Display *dpy, Window w, int *best_score, char **best_title) {
    if (!dpy || !w) return;

    XSelectInput(dpy, w, PropertyChangeMask | StructureNotifyMask | SubstructureNotifyMask);

    char *title = NULL;
    int score = score_candidate_window(dpy, w, &title);
    if (score > *best_score && title != NULL) {
        if (*best_title) free(*best_title);
        *best_title = title;
        *best_score = score;
    } else {
        if (title) free(title);
    }

    Window root_ret, parent_ret, *children = NULL;
    unsigned int nchildren = 0;

    if (XQueryTree(dpy, w, &root_ret, &parent_ret, &children, &nchildren) && children) {
        for (unsigned int i = 0; i < nchildren; i++) {
            scan_window_tree(dpy, children[i], best_score, best_title);
        }
        XFree(children);
    }
}

static void evaluate_and_emit_document_title(Display *dpy, Window root) {
    int best_score = -1;
    char *best_title = NULL;

    scan_window_tree(dpy, root, &best_score, &best_title);

    if (best_title != NULL && best_score > 0) {
        if (strcmp(last_emitted_title, best_title) != 0) {
            snprintf(last_emitted_title, sizeof(last_emitted_title), "%s", best_title);
            printf("TITLE:%s\n", best_title);
            fflush(stdout);
        }
        free(best_title);
    }
}

int main(int argc, char **argv) {
    (void)argc;
    (void)argv;
    // Install custom error handler so closing dialogs never abort this process
    XSetErrorHandler(ignore_x_errors);

    Display *dpy = NULL;

    // Retry connection while X server boots up
    for (int i = 0; i < 50; i++) {
        dpy = XOpenDisplay(NULL);
        if (dpy) break;
        usleep(100000); // 100ms
    }

    if (!dpy) {
        fprintf(stderr, "xopp-title-watcher: Failed to connect to X display\n");
        return 1;
    }

    init_atoms(dpy);
    Window root = DefaultRootWindow(dpy);
    XSelectInput(dpy, root, PropertyChangeMask | SubstructureNotifyMask);
    evaluate_and_emit_document_title(dpy, root);

    int x11_fd = ConnectionNumber(dpy);
    XEvent ev;

    while (1) {
        int need_update = 0;
        while (XPending(dpy) > 0) {
            XNextEvent(dpy, &ev);
            if (ev.type == PropertyNotify) {
                if (ev.xproperty.atom == net_wm_name ||
                    ev.xproperty.atom == XA_WM_NAME ||
                    ev.xproperty.atom == net_wm_visible_name ||
                    ev.xproperty.atom == net_active ||
                    ev.xproperty.atom == net_client_list ||
                    ev.xproperty.atom == net_wm_window_type) {
                    need_update = 1;
                }
            } else if (ev.type == CreateNotify ||
                       ev.type == MapNotify ||
                       ev.type == ReparentNotify ||
                       ev.type == DestroyNotify ||
                       ev.type == UnmapNotify) {
                need_update = 1;
            }
        }

        if (need_update) {
            evaluate_and_emit_document_title(dpy, root);
        }

        struct pollfd pfd;
        pfd.fd = x11_fd;
        pfd.events = POLLIN;
        pfd.revents = 0;
        int ret = poll(&pfd, 1, 400); // 400ms periodic refresh & poll
        if (ret == 0) {
            // Periodic sync check to guarantee title is always current
            evaluate_and_emit_document_title(dpy, root);
        }
    }

    XCloseDisplay(dpy);
    return 0;
}
