#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <fcntl.h>
#include <errno.h>

typedef void* gpointer;
typedef int gint;
typedef unsigned int guint;
typedef char gchar;
typedef unsigned long GType;
typedef struct _GtkWidget GtkWidget;
typedef struct _GtkWindow GtkWindow;
typedef struct _GList GList;
struct _GList {
    gpointer data;
    GList *next;
    GList *prev;
};

typedef void (*GCallback)(void);
typedef int (*GSourceFunc)(gpointer);

extern GType gtk_entry_get_type(void);
extern GType gtk_text_view_get_type(void);
extern GType gtk_editable_get_type(void);
extern int g_type_check_instance_is_a(gpointer instance, GType type);
extern unsigned long g_signal_connect_data(gpointer instance, const gchar *detailed_signal,
                                           GCallback c_handler, gpointer data,
                                           gpointer destroy_data, int connect_flags);
extern GList* gtk_window_list_toplevels(void);
extern void g_list_free(GList *list);
extern guint g_timeout_add(guint interval, GSourceFunc function, gpointer data);
extern GtkWidget* gtk_window_get_focus(GtkWindow *window);
extern int gtk_window_is_active(GtkWindow *window);
extern int gtk_widget_is_visible(GtkWidget *widget);

static int sock_fd = -1;

static void ensure_socket(void) {
    if (sock_fd >= 0) return;

    sock_fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (sock_fd < 0) return;

    // Use abstract Linux namespace socket "@aournal_ime_bridge"
    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    addr.sun_path[0] = '\0';
    const char *abstract_name = "aournal_ime_bridge";
    strncpy(&addr.sun_path[1], abstract_name, sizeof(addr.sun_path) - 2);

    socklen_t len = sizeof(sa_family_t) + 1 + strlen(abstract_name);

    // Blocking connect to local abstract socket (completes in microseconds if server is up)
    if (connect(sock_fd, (struct sockaddr *)&addr, len) < 0) {
        close(sock_fd);
        sock_fd = -1;
        return;
    }

    // Set non-blocking after connection is established so future operations don't stall the UI
    int flags = fcntl(sock_fd, F_GETFL, 0);
    if (flags >= 0) {
        fcntl(sock_fd, F_SETFL, flags | O_NONBLOCK);
    }
}

static void send_ime_event(const char *event) {
    ensure_socket();
    if (sock_fd >= 0) {
        size_t len = strlen(event);
        ssize_t written = send(sock_fd, event, len, MSG_NOSIGNAL | MSG_DONTWAIT);
        if (written < 0 && errno != EAGAIN && errno != EWOULDBLOCK) {
            close(sock_fd);
            sock_fd = -1;
        }
    }
}

static int is_editable_text_widget(GtkWidget *widget) {
    if (!widget) return 0;

    // Check if widget implements GtkEditable (GtkEntry, GtkSearchEntry, GtkSpinButton, etc.)
    // or is a GtkTextView (multi-line text views, canvas text editors)
    if (g_type_check_instance_is_a((gpointer)widget, gtk_editable_get_type())) {
        return 1;
    }
    if (g_type_check_instance_is_a((gpointer)widget, gtk_text_view_get_type())) {
        return 1;
    }

    return 0;
}

static GtkWidget *last_active_widget = NULL;

static void update_focus_state(GtkWidget *widget) {
    if (widget == last_active_widget) return;
    last_active_widget = widget;

    if (widget && is_editable_text_widget(widget)) {
        send_ime_event("FOCUS_IN\n");
    } else {
        send_ime_event("FOCUS_OUT\n");
    }
}

static void on_window_set_focus(GtkWindow *window, GtkWidget *widget, gpointer user_data) {
    update_focus_state(widget);
}

static int on_window_map(GtkWidget *window_widget, gpointer event, gpointer user_data) {
    GtkWidget *focused = gtk_window_get_focus((GtkWindow *)window_widget);
    if (focused) {
        update_focus_state(focused);
    }
    return 0;
}

static int on_window_focus_in(GtkWidget *window_widget, gpointer event, gpointer user_data) {
    GtkWidget *focused = gtk_window_get_focus((GtkWindow *)window_widget);
    update_focus_state(focused);
    return 0;
}

static int on_window_focus_out(GtkWidget *window_widget, gpointer event, gpointer user_data) {
    update_focus_state(NULL);
    return 0;
}

#define MAX_HOOKED 128
static GtkWindow* hooked_windows[MAX_HOOKED];
static int num_hooked = 0;

static void hook_window(GtkWindow *window) {
    if (!window) return;

    for (int i = 0; i < num_hooked; i++) {
        if (hooked_windows[i] == window) return; // Already hooked
    }

    if (num_hooked < MAX_HOOKED) {
        hooked_windows[num_hooked++] = window;
        g_signal_connect_data((gpointer)window, "set-focus", (GCallback)on_window_set_focus, NULL, NULL, 0);
        g_signal_connect_data((gpointer)window, "map-event", (GCallback)on_window_map, NULL, NULL, 0);
        g_signal_connect_data((gpointer)window, "focus-in-event", (GCallback)on_window_focus_in, NULL, NULL, 0);
        g_signal_connect_data((gpointer)window, "focus-out-event", (GCallback)on_window_focus_out, NULL, NULL, 0);

        if (gtk_window_is_active(window)) {
            GtkWidget *focused = gtk_window_get_focus(window);
            if (focused) {
                update_focus_state(focused);
            }
        }
    }
}

static int poll_toplevels(gpointer data) {
    GList *list = gtk_window_list_toplevels();
    for (GList *l = list; l != NULL; l = l->next) {
        GtkWindow *win = (GtkWindow *)l->data;
        hook_window(win);
        if (gtk_window_is_active(win)) {
            GtkWidget *focused = gtk_window_get_focus(win);
            update_focus_state(focused);
        }
    }
    if (list) {
        g_list_free(list);
    }
    return 1; // Keep repeating
}

__attribute__((visibility("default")))
void gtk_module_init(gint *argc, gchar ***argv) {
    g_timeout_add(150, (GSourceFunc)poll_toplevels, NULL);
}

__attribute__((visibility("default")))
void gtk_module_display_init(gpointer display) {
    poll_toplevels(NULL);
}
