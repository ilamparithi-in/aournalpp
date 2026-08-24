#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <poll.h>
#include <X11/Xlib.h>
#include <X11/Xatom.h>

static int ignore_x_errors(Display *dpy, XErrorEvent *err) {
    (void)dpy;
    (void)err;
    // Suppress asynchronous BadWindow / BadDrawable errors when dialogs close
    return 0;
}

static char last_emitted_title[1024] = "";

static void check_and_print_title(Display *dpy, Window w, Atom net_wm_name, Atom net_wm_visible_name) {
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
            char *buf = (char *)malloc(nitems + 1);
            if (buf) {
                memcpy(buf, prop, nitems);
                buf[nitems] = '\0';
                title = buf;
            }
        }
        XFree(prop);
    }

    // 2. Try _NET_WM_VISIBLE_NAME
    if (!title && net_wm_visible_name != None &&
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
    }

    // 3. Fallback to standard WM_NAME
    if (!title) {
        char *name = NULL;
        if (XFetchName(dpy, w, &name) > 0 && name && name[0] != '\0') {
            title = strdup(name);
            XFree(name);
        }
    }

    if (title) {
        // Filter out dialogs / internal window titles
        int is_dialog = (strcmp(title, "Openbox") == 0 ||
                         strcmp(title, "Preferences") == 0 ||
                         strcmp(title, "Save File") == 0 ||
                         strcmp(title, "Save As") == 0 ||
                         strcmp(title, "Save") == 0 ||
                         strcmp(title, "Open File") == 0 ||
                         strcmp(title, "Open Document") == 0 ||
                         strcmp(title, "Open") == 0 ||
                         strcmp(title, "Export as PDF") == 0 ||
                         strcmp(title, "Export") == 0 ||
                         strcmp(title, "Desktop") == 0 ||
                         strcmp(title, "X11") == 0 ||
                         strcmp(title, "Select Folder") == 0 ||
                         strcmp(title, "Choose Folder") == 0 ||
                         strcmp(title, "Question") == 0 ||
                         strcmp(title, "Warning") == 0 ||
                         strcmp(title, "Error") == 0 ||
                         strcmp(title, "Information") == 0);

        if (!is_dialog && strlen(title) > 0) {
            if (strcmp(last_emitted_title, title) != 0) {
                snprintf(last_emitted_title, sizeof(last_emitted_title), "%s", title);
                printf("TITLE:%s\n", title);
                fflush(stdout);
            }
        }
        free(title);
    }
}

static void select_all_windows_recursive(Display *dpy, Window w, Atom net_wm_name, Atom net_wm_visible_name) {
    if (!dpy || !w) return;

    XSelectInput(dpy, w, PropertyChangeMask | StructureNotifyMask | SubstructureNotifyMask);
    check_and_print_title(dpy, w, net_wm_name, net_wm_visible_name);

    Window root_ret, parent_ret, *children = NULL;
    unsigned int nchildren = 0;

    if (XQueryTree(dpy, w, &root_ret, &parent_ret, &children, &nchildren) && children) {
        for (unsigned int i = 0; i < nchildren; i++) {
            select_all_windows_recursive(dpy, children[i], net_wm_name, net_wm_visible_name);
        }
        XFree(children);
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

    Window root = DefaultRootWindow(dpy);
    Atom net_wm_name = XInternAtom(dpy, "_NET_WM_NAME", False);
    Atom net_wm_visible_name = XInternAtom(dpy, "_NET_WM_VISIBLE_NAME", False);
    Atom net_active = XInternAtom(dpy, "_NET_ACTIVE_WINDOW", False);
    Atom net_client_list = XInternAtom(dpy, "_NET_CLIENT_LIST", False);

    XSelectInput(dpy, root, PropertyChangeMask | SubstructureNotifyMask);
    select_all_windows_recursive(dpy, root, net_wm_name, net_wm_visible_name);

    int x11_fd = ConnectionNumber(dpy);
    XEvent ev;

    while (1) {
        while (XPending(dpy) > 0) {
            XNextEvent(dpy, &ev);

            if (ev.type == PropertyNotify) {
                if (ev.xproperty.atom == net_wm_name ||
                    ev.xproperty.atom == XA_WM_NAME ||
                    ev.xproperty.atom == net_wm_visible_name) {
                    check_and_print_title(dpy, ev.xproperty.window, net_wm_name, net_wm_visible_name);
                } else if (ev.xproperty.atom == net_active || ev.xproperty.atom == net_client_list) {
                    select_all_windows_recursive(dpy, root, net_wm_name, net_wm_visible_name);
                }
            } else if (ev.type == CreateNotify) {
                XSelectInput(dpy, ev.xcreatewindow.window, PropertyChangeMask | StructureNotifyMask | SubstructureNotifyMask);
                check_and_print_title(dpy, ev.xcreatewindow.window, net_wm_name, net_wm_visible_name);
            } else if (ev.type == MapNotify) {
                XSelectInput(dpy, ev.xmap.window, PropertyChangeMask | StructureNotifyMask | SubstructureNotifyMask);
                check_and_print_title(dpy, ev.xmap.window, net_wm_name, net_wm_visible_name);
            } else if (ev.type == ReparentNotify) {
                XSelectInput(dpy, ev.xreparent.window, PropertyChangeMask | StructureNotifyMask | SubstructureNotifyMask);
                check_and_print_title(dpy, ev.xreparent.window, net_wm_name, net_wm_visible_name);
            } else if (ev.type == DestroyNotify || ev.type == UnmapNotify) {
                select_all_windows_recursive(dpy, root, net_wm_name, net_wm_visible_name);
            }
        }

        struct pollfd pfd;
        pfd.fd = x11_fd;
        pfd.events = POLLIN;
        pfd.revents = 0;
        int ret = poll(&pfd, 1, 400); // 400ms periodic refresh & poll
        if (ret == 0) {
            // Periodic sync check to guarantee title is always current
            select_all_windows_recursive(dpy, root, net_wm_name, net_wm_visible_name);
        }
    }

    XCloseDisplay(dpy);
    return 0;
}
