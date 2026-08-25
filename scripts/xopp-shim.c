#define _GNU_SOURCE
#include <dlfcn.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>

typedef ssize_t (*real_readlink_t)(const char *, char *, size_t);
typedef ssize_t (*real_readlinkat_t)(int, const char *, char *, size_t);

static real_readlink_t sys_readlink = NULL;
static real_readlinkat_t sys_readlinkat = NULL;

static int is_proc_self_exe(const char *path) {
    if (!path) return 0;
    return (strcmp(path, "/proc/self/exe") == 0 ||
            strcmp(path, "/proc/thread-self/exe") == 0);
}

static ssize_t get_fake_exe(char *buf, size_t bufsiz) {
    const char *fake_exe = getenv("XOPP_FAKE_EXE");
    if (fake_exe && *fake_exe) {
        size_t len = strlen(fake_exe);
        if (len > bufsiz) len = bufsiz;
        memcpy(buf, fake_exe, len);
        return (ssize_t)len;
    }
    const char *prefix = getenv("PREFIX");
    if (prefix && *prefix) {
        char fake_path[1024];
        snprintf(fake_path, sizeof(fake_path), "%s/bin/xournalpp", prefix);
        size_t len = strlen(fake_path);
        if (len > bufsiz) len = bufsiz;
        memcpy(buf, fake_path, len);
        return (ssize_t)len;
    }
    return -1;
}

__attribute__((visibility("default")))
ssize_t readlink(const char *pathname, char *buf, size_t bufsiz) {
    if (is_proc_self_exe(pathname)) {
        ssize_t res = get_fake_exe(buf, bufsiz);
        if (res > 0) return res;
    }
    if (!sys_readlink) {
        sys_readlink = (real_readlink_t)dlsym(RTLD_NEXT, "readlink");
    }
    if (sys_readlink) {
        return sys_readlink(pathname, buf, bufsiz);
    }
    return -1;
}

__attribute__((visibility("default")))
ssize_t readlinkat(int dirfd, const char *pathname, char *buf, size_t bufsiz) {
    if (is_proc_self_exe(pathname)) {
        ssize_t res = get_fake_exe(buf, bufsiz);
        if (res > 0) return res;
    }
    if (!sys_readlinkat) {
        sys_readlinkat = (real_readlinkat_t)dlsym(RTLD_NEXT, "readlinkat");
    }
    if (sys_readlinkat) {
        return sys_readlinkat(dirfd, pathname, buf, bufsiz);
    }
    return -1;
}

#include <sys/socket.h>
#include <sys/un.h>
#include <fcntl.h>
#include <errno.h>

static int ime_sock_fd = -1;

static void ensure_ime_socket(void) {
    if (ime_sock_fd >= 0) return;

    ime_sock_fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (ime_sock_fd < 0) return;

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    addr.sun_path[0] = '\0';
    const char *abstract_name = "aournal_ime_bridge";
    strncpy(&addr.sun_path[1], abstract_name, sizeof(addr.sun_path) - 2);

    socklen_t len = sizeof(sa_family_t) + 1 + strlen(abstract_name);

    if (connect(ime_sock_fd, (struct sockaddr *)&addr, len) < 0) {
        close(ime_sock_fd);
        ime_sock_fd = -1;
        return;
    }

    int flags = fcntl(ime_sock_fd, F_GETFL, 0);
    if (flags >= 0) {
        fcntl(ime_sock_fd, F_SETFL, flags | O_NONBLOCK);
    }
}

static void send_ime_event(const char *event) {
    ensure_ime_socket();
    if (ime_sock_fd >= 0) {
        size_t len = strlen(event);
        ssize_t written = send(ime_sock_fd, event, len, MSG_NOSIGNAL | MSG_DONTWAIT);
        if (written < 0 && errno != EAGAIN && errno != EWOULDBLOCK) {
            close(ime_sock_fd);
            ime_sock_fd = -1;
        }
    }
}

typedef void (*real_gtk_im_context_focus_in_t)(void *);
typedef void (*real_gtk_im_context_focus_out_t)(void *);

static real_gtk_im_context_focus_in_t real_gtk_im_context_focus_in = NULL;
static real_gtk_im_context_focus_out_t real_gtk_im_context_focus_out = NULL;

__attribute__((visibility("default")))
void gtk_im_context_focus_in(void *context) {
    send_ime_event("FOCUS_IN\n");
    if (!real_gtk_im_context_focus_in) {
        real_gtk_im_context_focus_in = (real_gtk_im_context_focus_in_t)dlsym(RTLD_NEXT, "gtk_im_context_focus_in");
    }
    if (real_gtk_im_context_focus_in) {
        real_gtk_im_context_focus_in(context);
    }
}

__attribute__((visibility("default")))
void gtk_im_context_focus_out(void *context) {
    send_ime_event("FOCUS_OUT\n");
    if (!real_gtk_im_context_focus_out) {
        real_gtk_im_context_focus_out = (real_gtk_im_context_focus_out_t)dlsym(RTLD_NEXT, "gtk_im_context_focus_out");
    }
    if (real_gtk_im_context_focus_out) {
        real_gtk_im_context_focus_out(context);
    }
}
