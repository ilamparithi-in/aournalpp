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
