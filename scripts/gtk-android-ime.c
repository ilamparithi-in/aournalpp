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

extern GType g_type_from_name(const gchar *name);
extern int g_type_check_instance_is_a(gpointer instance, GType type);
extern unsigned long g_signal_connect_data(gpointer instance, const gchar *detailed_signal,
                                           GCallback c_handler, gpointer data,
                                           gpointer destroy_data, int connect_flags);
extern GList* gtk_window_list_toplevels(void);
extern guint g_timeout_add(guint interval, GSourceFunc function, gpointer data);

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

    int flags = fcntl(sock_fd, F_GETFL, 0);
    fcntl(sock_fd, F_SETFL, flags | O_NONBLOCK);

    if (connect(sock_fd, (struct sockaddr *)&addr, len) < 0) {
        if (errno != EINPROGRESS && errno != EALREADY) {
            close(sock_fd);
            sock_fd = -1;
        }
    }
}

static void send_ime_event(const char *event) {
    ensure_socket();
    if (sock_fd >= 0) {
        size_t len = strlen(event);
        if (write(sock_fd, event, len) < 0) {
            close(sock_fd);
            sock_fd = -1;
        }
    }
}

static int is_editable_text_widget(GtkWidget *widget) {
    if (!widget) return 0;

    static GType entry_type = 0;
    static GType text_view_type = 0;
    static GType editable_type = 0;

    if (!entry_type) entry_type = g_type_from_name("GtkEntry");
    if (!text_view_type) text_view_type = g_type_from_name("GtkTextView");
    if (!editable_type) editable_type = g_type_from_name("GtkEditable");

    if (entry_type && g_type_check_instance_is_a(widget, entry_type)) return 1;
    if (text_view_type && g_type_check_instance_is_a(widget, text_view_type)) return 1;
    if (editable_type && g_type_check_instance_is_a(widget, editable_type)) return 1;

    return 0;
}

static void on_window_set_focus(GtkWindow *window, GtkWidget *widget, gpointer user_data) {
    if (widget && is_editable_text_widget(widget)) {
        send_ime_event("FOCUS_IN\n");
    } else {
        send_ime_event("FOCUS_OUT\n");
    }
}

#define MAX_HOOKED 64
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
    }
}

static int poll_toplevels(gpointer data) {
    GList *list = gtk_window_list_toplevels();
    for (GList *l = list; l != NULL; l = l->next) {
        hook_window((GtkWindow *)l->data);
    }
    return 1; // Keep repeating
}

__attribute__((visibility("default")))
void gtk_module_init(gint *argc, gchar ***argv) {
    g_timeout_add(300, (GSourceFunc)poll_toplevels, NULL);
}
