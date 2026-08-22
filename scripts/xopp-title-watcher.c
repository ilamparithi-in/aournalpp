#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <X11/Xlib.h>
#include <X11/Xatom.h>

static int ignore_x_errors(Display *dpy, XErrorEvent *err) {
    (void)dpy;
    (void)err;
    // Suppress asynchronous BadWindow / BadDrawable errors when dialogs close
    return 0;
}

static void check_and_print_title(Display *dpy, Window w, Atom net_wm_name) {
    if (!dpy || !w || w == DefaultRootWindow(dpy)) return;

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
            title = strdup((char *)prop);
        }
        XFree(prop);
    }

    // 2. Fallback to standard WM_NAME
    if (!title) {
        char *name = NULL;
        if (XFetchName(dpy, w, &name) > 0 && name && name[0] != '\0') {
            title = strdup(name);
            XFree(name);
        }
    }

    if (title) {
        // Only accept main Xournal++ document window titles:
        // Examples: "MyNotes.xopp - Xournal++", "Lecture.xopp * - Xournal++", "Unsaved Document - Xournal++", "Unsaved Document"
        // Dialogs like "Save File", "Open Document", "Preferences", "Openbox" will NOT match.
        if (strstr(title, " - Xournal++") != NULL ||
            strstr(title, ".xopp") != NULL ||
            strcmp(title, "Unsaved Document") == 0 ||
            strcmp(title, "Untitled") == 0) {
            printf("TITLE:%s\n", title);
            fflush(stdout);
        }
        free(title);
    }
}

static void select_all_windows_recursive(Display *dpy, Window w, Atom net_wm_name) {
    if (!dpy || !w) return;

    XSelectInput(dpy, w, PropertyChangeMask | StructureNotifyMask | SubstructureNotifyMask);
    check_and_print_title(dpy, w, net_wm_name);

    Window root_ret, parent_ret, *children = NULL;
    unsigned int nchildren = 0;

    if (XQueryTree(dpy, w, &root_ret, &parent_ret, &children, &nchildren) && children) {
        for (unsigned int i = 0; i < nchildren; i++) {
            select_all_windows_recursive(dpy, children[i], net_wm_name);
        }
        XFree(children);
    }
}

int main(int argc, char **argv) {
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

    Window root = DefaultRootWindow(dpy);
    Atom net_wm_name = XInternAtom(dpy, "_NET_WM_NAME", False);
    Atom net_active = XInternAtom(dpy, "_NET_ACTIVE_WINDOW", False);
    Atom net_client_list = XInternAtom(dpy, "_NET_CLIENT_LIST", False);

    XSelectInput(dpy, root, PropertyChangeMask | SubstructureNotifyMask);
    select_all_windows_recursive(dpy, root, net_wm_name);

    XEvent ev;
    while (1) {
        XNextEvent(dpy, &ev);

        if (ev.type == PropertyNotify) {
            if (ev.xproperty.atom == net_wm_name || ev.xproperty.atom == XA_WM_NAME) {
                check_and_print_title(dpy, ev.xproperty.window, net_wm_name);
            } else if (ev.xproperty.atom == net_active || ev.xproperty.atom == net_client_list) {
                select_all_windows_recursive(dpy, root, net_wm_name);
            }
        } else if (ev.type == CreateNotify) {
            XSelectInput(dpy, ev.xcreatewindow.window, PropertyChangeMask | StructureNotifyMask | SubstructureNotifyMask);
            check_and_print_title(dpy, ev.xcreatewindow.window, net_wm_name);
        } else if (ev.type == MapNotify) {
            XSelectInput(dpy, ev.xmap.window, PropertyChangeMask | StructureNotifyMask | SubstructureNotifyMask);
            check_and_print_title(dpy, ev.xmap.window, net_wm_name);
        } else if (ev.type == ReparentNotify) {
            XSelectInput(dpy, ev.xreparent.window, PropertyChangeMask | StructureNotifyMask | SubstructureNotifyMask);
            check_and_print_title(dpy, ev.xreparent.window, net_wm_name);
        } else if (ev.type == DestroyNotify || ev.type == UnmapNotify) {
            select_all_windows_recursive(dpy, root, net_wm_name);
        }
    }

    XCloseDisplay(dpy);
    return 0;
}
