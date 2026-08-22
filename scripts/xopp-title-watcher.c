#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <X11/Xlib.h>
#include <X11/Xatom.h>

static void check_and_print_title(Display *dpy, Window w, Atom net_wm_name) {
    if (!dpy || !w) return;

    Atom type;
    int format;
    unsigned long nitems = 0, bytes_after = 0;
    unsigned char *prop = NULL;

    // Try _NET_WM_NAME (UTF-8)
    if (net_wm_name != None &&
        XGetWindowProperty(dpy, w, net_wm_name, 0, 1024, False, AnyPropertyType,
                           &type, &format, &nitems, &bytes_after, &prop) == Success && prop) {
        if (nitems > 0 && prop[0] != '\0') {
            printf("TITLE:%s\n", (char *)prop);
            fflush(stdout);
        }
        XFree(prop);
        return;
    }

    // Fallback to standard WM_NAME
    char *name = NULL;
    if (XFetchName(dpy, w, &name) > 0 && name) {
        if (name[0] != '\0') {
            printf("TITLE:%s\n", name);
            fflush(stdout);
        }
        XFree(name);
    }
}

static void select_all_windows(Display *dpy, Window root, Atom net_wm_name) {
    Window root_ret, parent_ret, *children = NULL;
    unsigned int nchildren = 0;

    if (XQueryTree(dpy, root, &root_ret, &parent_ret, &children, &nchildren) && children) {
        for (unsigned int i = 0; i < nchildren; i++) {
            XSelectInput(dpy, children[i], PropertyChangeMask | StructureNotifyMask);
            check_and_print_title(dpy, children[i], net_wm_name);
        }
        XFree(children);
    }
}

int main(int argc, char **argv) {
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
    select_all_windows(dpy, root, net_wm_name);

    XEvent ev;
    while (1) {
        XNextEvent(dpy, &ev);

        if (ev.type == PropertyNotify) {
            if (ev.xproperty.atom == net_wm_name || ev.xproperty.atom == XA_WM_NAME) {
                check_and_print_title(dpy, ev.xproperty.window, net_wm_name);
            } else if (ev.xproperty.atom == net_active || ev.xproperty.atom == net_client_list) {
                select_all_windows(dpy, root, net_wm_name);
            }
        } else if (ev.type == CreateNotify) {
            XSelectInput(dpy, ev.xcreatewindow.window, PropertyChangeMask | StructureNotifyMask);
        } else if (ev.type == MapNotify) {
            XSelectInput(dpy, ev.xmap.window, PropertyChangeMask | StructureNotifyMask);
            check_and_print_title(dpy, ev.xmap.window, net_wm_name);
        }
    }

    XCloseDisplay(dpy);
    return 0;
}
