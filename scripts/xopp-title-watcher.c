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
static Atom net_wm_state = None;
static Atom net_wm_state_modal = None;

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
    net_wm_state = XInternAtom(dpy, "_NET_WM_STATE", False);
    net_wm_state_modal = XInternAtom(dpy, "_NET_WM_STATE_MODAL", False);
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
        if (ch.res_name && (strcasecmp(ch.res_name, "xournalpp") == 0 ||
                            strcasecmp(ch.res_name, "xopp") == 0 ||
                            case_str_search(ch.res_name, "xournal") != NULL)) {
            match = 1;
        } else if (ch.res_class && (strcasecmp(ch.res_class, "xournalpp") == 0 ||
                                   strcasecmp(ch.res_class, "xopp") == 0 ||
                                   case_str_search(ch.res_class, "xournal") != NULL)) {
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

static int is_modal_state(Display *dpy, Window w) {
    if (net_wm_state == None || net_wm_state_modal == None) return 0;
    Atom type;
    int format;
    unsigned long nitems = 0, bytes_after = 0;
    unsigned char *prop = NULL;
    int modal = 0;
    if (XGetWindowProperty(dpy, w, net_wm_state, 0, 32, False, XA_ATOM,
                           &type, &format, &nitems, &bytes_after, &prop) == Success && prop) {
        if (type == XA_ATOM && format == 32) {
            Atom *atoms = (Atom *)prop;
            for (unsigned long i = 0; i < nitems; i++) {
                if (atoms[i] == net_wm_state_modal) {
                    modal = 1;
                    break;
                }
            }
        }
        XFree(prop);
    }
    return modal;
}

static int is_tooltip_window(Display *dpy, Window w) {
    XWindowAttributes attr;
    if (XGetWindowAttributes(dpy, w, &attr)) {
        // Tooltips and menus in X11/GTK are override-redirect windows
        if (attr.override_redirect) return 1;
    }
    if (net_wm_window_type != None) {
        Atom type;
        int format;
        unsigned long nitems = 0, bytes_after = 0;
        unsigned char *prop = NULL;
        if (XGetWindowProperty(dpy, w, net_wm_window_type, 0, 32, False, XA_ATOM,
                               &type, &format, &nitems, &bytes_after, &prop) == Success && prop) {
            if (type == XA_ATOM && format == 32) {
                Atom *atoms = (Atom *)prop;
                for (unsigned long i = 0; i < nitems; i++) {
                    if (atoms[i] == net_wm_window_type_tooltip ||
                        atoms[i] == net_wm_window_type_notification ||
                        atoms[i] == net_wm_window_type_popup_menu ||
                        atoms[i] == net_wm_window_type_dropdown_menu ||
                        atoms[i] == net_wm_window_type_dnd) {
                        XFree(prop);
                        return 1;
                    }
                }
            }
            XFree(prop);
        }
    }
    return 0;
}

static int is_viewable_managed_xournal_window(Display *dpy, Window w) {
    if (!dpy || !w || w == DefaultRootWindow(dpy)) return 0;
    XWindowAttributes attr;
    if (!XGetWindowAttributes(dpy, w, &attr)) return 0;
    if (attr.map_state != IsViewable) return 0;
    if (attr.override_redirect) return 0; // Filter out tooltips, menus, popups
    if (!is_xournalpp_window(dpy, w)) return 0;
    if (is_tooltip_window(dpy, w)) return 0;
    return 1;
}

static int is_dialog_window(Display *dpy, Window w) {
    if (is_transient_window(dpy, w)) return 1;
    if (is_modal_state(dpy, w)) return 1;

    // Check _NET_WM_WINDOW_TYPE
    if (net_wm_window_type != None) {
        Atom type;
        int format;
        unsigned long nitems = 0, bytes_after = 0;
        unsigned char *prop = NULL;
        if (XGetWindowProperty(dpy, w, net_wm_window_type, 0, 32, False, XA_ATOM,
                               &type, &format, &nitems, &bytes_after, &prop) == Success && prop) {
            if (type == XA_ATOM && format == 32) {
                Atom *atoms = (Atom *)prop;
                for (unsigned long i = 0; i < nitems; i++) {
                    if (atoms[i] == net_wm_window_type_dialog ||
                        atoms[i] == net_wm_window_type_utility ||
                        atoms[i] == net_wm_window_type_splash) {
                        XFree(prop);
                        return 1;
                    }
                }
            }
            XFree(prop);
        }
    }

    char *title = get_window_title(dpy, w);
    if (!title || title[0] == '\0') {
        if (title) free(title);
        // Window lacks a title.
        return 1;
    }

    int d = is_dialog_or_ignored_title(title);
    free(title);
    return d;
}

static void query_managed_xournal_windows(Display *dpy, Window root,
                                         Window *out_main, int *out_main_count,
                                         Window *out_dialogs, int *out_dialog_count,
                                         int max_windows) {
    *out_main_count = 0;
    *out_dialog_count = 0;

    Window candidate_windows[64];
    int candidate_count = 0;

    Atom type;
    int format;
    unsigned long nitems = 0, bytes_after = 0;
    unsigned char *prop = NULL;

    if (net_client_list != None &&
        XGetWindowProperty(dpy, root, net_client_list, 0, 1024, False, XA_WINDOW,
                           &type, &format, &nitems, &bytes_after, &prop) == Success && prop) {
        if (type == XA_WINDOW && format == 32 && nitems > 0) {
            Window *clients = (Window *)prop;
            for (unsigned long i = 0; i < nitems && candidate_count < 64; i++) {
                Window w = clients[i];
                if (is_viewable_managed_xournal_window(dpy, w)) {
                    candidate_windows[candidate_count++] = w;
                }
            }
        }
        XFree(prop);
    }

    if (candidate_count == 0) {
        Window root_ret, parent_ret, *children = NULL;
        unsigned int nchildren = 0;
        if (XQueryTree(dpy, root, &root_ret, &parent_ret, &children, &nchildren) && children) {
            for (unsigned int i = 0; i < nchildren && candidate_count < 64; i++) {
                Window w = children[i];
                if (is_viewable_managed_xournal_window(dpy, w)) {
                    candidate_windows[candidate_count++] = w;
                }
            }
            XFree(children);
        }
    }

    if (candidate_count == 0) return;

    // Identify primary document window among candidates
    int main_idx = -1;
    int best_score = -1;

    for (int i = 0; i < candidate_count; i++) {
        Window w = candidate_windows[i];
        if (is_transient_window(dpy, w) || is_modal_state(dpy, w)) {
            continue;
        }
        char *title = NULL;
        int score = score_candidate_window(dpy, w, &title);
        if (title) free(title);
        if (score > best_score) {
            best_score = score;
            main_idx = i;
        }
    }

    if (main_idx >= 0) {
        if (*out_main_count < max_windows) {
            out_main[(*out_main_count)++] = candidate_windows[main_idx];
        }
        for (int i = 0; i < candidate_count; i++) {
            if (i == main_idx) continue;
            // Any other managed window is a dialog, prompt, or subwindow (including untitled prompts)
            if (*out_dialog_count < max_windows) {
                out_dialogs[(*out_dialog_count)++] = candidate_windows[i];
            }
        }
    } else {
        // No obvious document window found. Classify windows explicitly
        for (int i = 0; i < candidate_count; i++) {
            Window w = candidate_windows[i];
            if (is_dialog_window(dpy, w)) {
                if (*out_dialog_count < max_windows) {
                    out_dialogs[(*out_dialog_count)++] = w;
                }
            } else {
                if (*out_main_count < max_windows) {
                    out_main[(*out_main_count)++] = w;
                }
            }
        }
        if (*out_dialog_count == 0 && *out_main_count > 1) {
            for (int i = 1; i < *out_main_count; i++) {
                if (*out_dialog_count < max_windows) {
                    out_dialogs[(*out_dialog_count)++] = out_main[i];
                }
            }
            *out_main_count = 1;
        }
    }
}

static int last_emitted_dialog_count = -1;

static void evaluate_and_emit_status(Display *dpy, Window root) {
    evaluate_and_emit_document_title(dpy, root);

    Window main_wins[64];
    Window dialog_wins[64];
    int main_count = 0, dialog_count = 0;
    query_managed_xournal_windows(dpy, root, main_wins, &main_count, dialog_wins, &dialog_count, 64);

    if (dialog_count != last_emitted_dialog_count) {
        last_emitted_dialog_count = dialog_count;
        printf("DIALOGS:%d\n", dialog_count);
        fflush(stdout);
    }
}

int main(int argc, char **argv) {
    // Install custom error handler so closing dialogs never abort this process
    XSetErrorHandler(ignore_x_errors);

    Display *dpy = NULL;

    // Retry connection while X server boots up (faster 50ms interval)
    int max_retries = (argc > 1) ? 10 : 50;
    for (int i = 0; i < max_retries; i++) {
        dpy = XOpenDisplay(NULL);
        if (dpy) break;
        usleep(50000); // 50ms
    }

    if (!dpy) {
        fprintf(stderr, "xopp-title-watcher: Failed to connect to X display\n");
        return 1;
    }

    init_atoms(dpy);
    Window root = DefaultRootWindow(dpy);

    if (argc > 1) {
        if (strcmp(argv[1], "--list-dialogs") == 0 || strcmp(argv[1], "--check-dialogs") == 0) {
            Window main_wins[64];
            Window dialog_wins[64];
            int main_count = 0, dialog_count = 0;
            query_managed_xournal_windows(dpy, root, main_wins, &main_count, dialog_wins, &dialog_count, 64);
            for (int i = 0; i < dialog_count; i++) {
                printf("%lu\n", (unsigned long)dialog_wins[i]);
            }
            fflush(stdout);
            XCloseDisplay(dpy);
            return (dialog_count > 0) ? 0 : 1;
        } else if (strcmp(argv[1], "--list-windows") == 0) {
            Window main_wins[64];
            Window dialog_wins[64];
            int main_count = 0, dialog_count = 0;
            query_managed_xournal_windows(dpy, root, main_wins, &main_count, dialog_wins, &dialog_count, 64);
            for (int i = 0; i < main_count; i++) {
                printf("%lu\n", (unsigned long)main_wins[i]);
            }
            for (int i = 0; i < dialog_count; i++) {
                printf("%lu\n", (unsigned long)dialog_wins[i]);
            }
            fflush(stdout);
            XCloseDisplay(dpy);
            return 0;
        }
    }

    XSelectInput(dpy, root, PropertyChangeMask | SubstructureNotifyMask);
    evaluate_and_emit_status(dpy, root);

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
                    ev.xproperty.atom == net_wm_window_type ||
                    ev.xproperty.atom == net_wm_state) {
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
            evaluate_and_emit_status(dpy, root);
        }

        struct pollfd pfd;
        pfd.fd = x11_fd;
        pfd.events = POLLIN;
        pfd.revents = 0;
        int ret = poll(&pfd, 1, 400); // 400ms periodic refresh & poll
        if (ret == 0) {
            // Periodic sync check to guarantee title and dialog state are always current
            evaluate_and_emit_status(dpy, root);
        }
    }

    XCloseDisplay(dpy);
    return 0;
}
