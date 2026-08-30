#define _GNU_SOURCE
#include <dlfcn.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <pthread.h>

/* ========================================================================= */
/* 1. /proc/self/exe Shim for Android                                        */
/* ========================================================================= */

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

/* ========================================================================= */
/* 2. GTK Android IME Bridge Support                                        */
/* ========================================================================= */

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

/* ========================================================================= */
/* 3. GNU gettext & GLib i18n Translation Engine                             */
/* ========================================================================= */

#define MAX_MO_CATALOGS 32

typedef struct {
    char domain[64];
    char lang[64];
    const uint8_t *data;
    size_t size;
    uint32_t count;
    uint32_t orig_tab;
    uint32_t trans_tab;
} mo_catalog_t;

static mo_catalog_t g_catalogs[MAX_MO_CATALOGS];
static int g_num_catalogs = 0;
static pthread_mutex_t g_mo_mutex = PTHREAD_MUTEX_INITIALIZER;

typedef struct {
    char domain[64];
    char dir[1024];
} textdomain_dir_t;

static textdomain_dir_t g_domain_dirs[16];
static int g_num_domain_dirs = 0;
static char g_current_domain[64] = "xournalpp";
static char g_current_locale[64] = "";

static const char *get_domain_dir(const char *domain) {
    if (!domain || !*domain) domain = g_current_domain;
    for (int i = 0; i < g_num_domain_dirs; i++) {
        if (strcmp(g_domain_dirs[i].domain, domain) == 0) {
            return g_domain_dirs[i].dir;
        }
    }
    const char *textdomaindir = getenv("TEXTDOMAINDIR");
    if (textdomaindir && *textdomaindir) return textdomaindir;

    const char *prefix = getenv("PREFIX");
    if (prefix && *prefix) {
        static char def_path[1024];
        snprintf(def_path, sizeof(def_path), "%s/share/locale", prefix);
        return def_path;
    }
    return "/data/data/dev.ilamparithi.aournalpp/files/usr/share/locale";
}

static void normalize_lang(const char *raw, char *out, size_t out_len) {
    if (!raw || !*raw) {
        out[0] = '\0';
        return;
    }
    size_t i = 0;
    while (raw[i] && raw[i] != '.' && raw[i] != '@' && i < out_len - 1) {
        out[i] = raw[i];
        i++;
    }
    out[i] = '\0';
}

static int try_mmap_mo(const char *filepath, mo_catalog_t *cat) {
    int fd = open(filepath, O_RDONLY);
    if (fd < 0) return 0;
    struct stat st;
    if (fstat(fd, &st) < 0 || st.st_size < 28) {
        close(fd);
        return 0;
    }
    void *map = mmap(NULL, st.st_size, PROT_READ, MAP_SHARED, fd, 0);
    close(fd);
    if (map == MAP_FAILED) return 0;

    const uint32_t *hdr = (const uint32_t *)map;
    // GNU MO magic number: 0x950412de
    if (hdr[0] != 0x950412de) {
        munmap(map, st.st_size);
        return 0;
    }

    cat->data = (const uint8_t *)map;
    cat->size = st.st_size;
    cat->count = hdr[2];
    cat->orig_tab = hdr[3];
    cat->trans_tab = hdr[4];
    return 1;
}

static mo_catalog_t *get_or_load_catalog(const char *domain, const char *lang) {
    if (!domain || !*domain) domain = g_current_domain;
    if (!lang || !*lang) return NULL;

    for (int i = 0; i < g_num_catalogs; i++) {
        if (strcmp(g_catalogs[i].domain, domain) == 0 &&
            strcmp(g_catalogs[i].lang, lang) == 0) {
            return &g_catalogs[i];
        }
    }

    if (g_num_catalogs >= MAX_MO_CATALOGS) return NULL;

    const char *base_dir = get_domain_dir(domain);
    char mo_path[1024];

    // Try lang directly: share/locale/<lang>/LC_MESSAGES/<domain>.mo
    snprintf(mo_path, sizeof(mo_path), "%s/%s/LC_MESSAGES/%s.mo", base_dir, lang, domain);

    mo_catalog_t new_cat;
    memset(&new_cat, 0, sizeof(new_cat));
    strncpy(new_cat.domain, domain, sizeof(new_cat.domain) - 1);
    strncpy(new_cat.lang, lang, sizeof(new_cat.lang) - 1);

    if (try_mmap_mo(mo_path, &new_cat)) {
        g_catalogs[g_num_catalogs] = new_cat;
        return &g_catalogs[g_num_catalogs++];
    }

    // Try short lang (e.g. "de_DE" -> "de")
    char short_lang[64];
    normalize_lang(lang, short_lang, sizeof(short_lang));
    char *underscore = strchr(short_lang, '_');
    if (underscore) {
        *underscore = '\0';
        snprintf(mo_path, sizeof(mo_path), "%s/%s/LC_MESSAGES/%s.mo", base_dir, short_lang, domain);
        if (try_mmap_mo(mo_path, &new_cat)) {
            g_catalogs[g_num_catalogs] = new_cat;
            return &g_catalogs[g_num_catalogs++];
        }
    }

    return NULL;
}

static const char *mo_lookup(mo_catalog_t *cat, const char *key) {
    if (!cat || !cat->data || !key) return NULL;
    int l = 0, r = (int)cat->count - 1;
    while (l <= r) {
        int m = l + (r - l) / 2;
        const uint32_t *o_ent = (const uint32_t *)(cat->data + cat->orig_tab + m * 8);
        const char *orig_str = (const char *)(cat->data + o_ent[1]);
        int cmp = strcmp(key, orig_str);
        if (cmp == 0) {
            const uint32_t *t_ent = (const uint32_t *)(cat->data + cat->trans_tab + m * 8);
            return (const char *)(cat->data + t_ent[1]);
        }
        if (cmp < 0) r = m - 1;
        else l = m + 1;
    }
    return NULL;
}

static const char *do_translate(const char *domain, const char *context, const char *msgid) {
    if (!msgid || !*msgid) return msgid;
    if (!domain || !*domain) domain = g_current_domain;

    pthread_mutex_lock(&g_mo_mutex);

    // Determine target languages to try from env or settings
    const char *langs[6] = {NULL};
    int n_langs = 0;

    const char *env_language = getenv("LANGUAGE");
    char lang_buf[256];
    if (env_language && *env_language) {
        strncpy(lang_buf, env_language, sizeof(lang_buf) - 1);
        lang_buf[sizeof(lang_buf) - 1] = '\0';
        char *tok = strtok(lang_buf, ":");
        while (tok && n_langs < 4) {
            if (strcmp(tok, "C") != 0 && strcmp(tok, "POSIX") != 0) {
                langs[n_langs++] = tok;
            }
            tok = strtok(NULL, ":");
        }
    }

    if (n_langs == 0 || (n_langs == 1 && strcmp(langs[0], "default") == 0)) {
        const char *env_lc_all = getenv("LC_ALL");
        const char *env_lc_msgs = getenv("LC_MESSAGES");
        const char *env_lang = getenv("LANG");
        const char *primary = env_lc_all ? env_lc_all : (env_lc_msgs ? env_lc_msgs : (env_lang ? env_lang : g_current_locale));
        if (primary && *primary && strcmp(primary, "C") != 0 && strcmp(primary, "POSIX") != 0 && strcmp(primary, "default") != 0) {
            static char s_prim[64];
            normalize_lang(primary, s_prim, sizeof(s_prim));
            langs[0] = s_prim;
            n_langs = 1;
        } else {
            // Fallback: Read preferredLocale from settings.xml
            const char *home = getenv("HOME");
            const char *config_home = getenv("XDG_CONFIG_HOME");
            char cfg_path[1024];
            if (config_home && *config_home) {
                snprintf(cfg_path, sizeof(cfg_path), "%s/xournalpp/settings.xml", config_home);
            } else if (home && *home) {
                snprintf(cfg_path, sizeof(cfg_path), "%s/.config/xournalpp/settings.xml", home);
            } else {
                cfg_path[0] = '\0';
            }
            if (cfg_path[0]) {
                FILE *fp = fopen(cfg_path, "r");
                if (fp) {
                    char line[512];
                    while (fgets(line, sizeof(line), fp)) {
                        char *p = strstr(line, "name=\"preferredLocale\"");
                        if (p) {
                            char *v = strstr(p, "value=\"");
                            if (v) {
                                v += 7;
                                char *endq = strchr(v, '"');
                                if (endq && endq > v) {
                                    static char s_pref[64];
                                    size_t len = (size_t)(endq - v);
                                    if (len < sizeof(s_pref)) {
                                        memcpy(s_pref, v, len);
                                        s_pref[len] = '\0';
                                        if (strcmp(s_pref, "default") != 0 && strcmp(s_pref, "system") != 0) {
                                            langs[0] = s_pref;
                                            n_langs = 1;
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                    fclose(fp);
                }
            }
        }
    }

    for (int i = 0; i < n_langs; i++) {
        mo_catalog_t *cat = get_or_load_catalog(domain, langs[i]);
        if (!cat && domain && strcmp(domain, "xournalpp") != 0) {
            cat = get_or_load_catalog("xournalpp", langs[i]);
        }
        if (!cat) continue;

        // 1. Try with context: "context\x04msgid"
        if (context && *context) {
            char ctx_buf[2048];
            size_t c_len = strlen(context);
            size_t m_len = strlen(msgid);
            if (c_len + 1 + m_len < sizeof(ctx_buf)) {
                memcpy(ctx_buf, context, c_len);
                ctx_buf[c_len] = '\x04';
                memcpy(ctx_buf + c_len + 1, msgid, m_len + 1);
                const char *res = mo_lookup(cat, ctx_buf);
                if (res) {
                    pthread_mutex_unlock(&g_mo_mutex);
                    return res;
                }
            }
        }

        // 2. Try plain msgid
        const char *res = mo_lookup(cat, msgid);
        if (res) {
            pthread_mutex_unlock(&g_mo_mutex);
            return res;
        }

        // 3. Fallback: Mnemonic underscore mismatch (e.g. "_Preferences" vs "Preferences")
        if (msgid[0] == '_' && msgid[1] != '\0') {
            res = mo_lookup(cat, msgid + 1);
            if (res) {
                pthread_mutex_unlock(&g_mo_mutex);
                return res;
            }
        } else {
            char mnem_buf[2048];
            mnem_buf[0] = '_';
            size_t m_len = strlen(msgid);
            if (m_len + 2 < sizeof(mnem_buf)) {
                memcpy(mnem_buf + 1, msgid, m_len + 1);
                res = mo_lookup(cat, mnem_buf);
                if (res) {
                    pthread_mutex_unlock(&g_mo_mutex);
                    return res;
                }
            }
        }
    }

    pthread_mutex_unlock(&g_mo_mutex);
    return msgid;
}

__attribute__((visibility("default")))
const char *g_dpgettext2(const char *domain, const char *context, const char *msgid) {
    return do_translate(domain, context, msgid);
}

__attribute__((visibility("default")))
const char *g_dgettext(const char *domain, const char *msgid) {
    return do_translate(domain, NULL, msgid);
}

__attribute__((visibility("default")))
const char *g_dpgettext(const char *domain, const char *msgctxtid, size_t msgidoffset) {
    if (!msgctxtid) return NULL;
    if (msgidoffset > 0) {
        const char *trans = do_translate(domain, NULL, msgctxtid);
        if (trans != msgctxtid) return trans;
        return msgctxtid + msgidoffset;
    }
    return do_translate(domain, NULL, msgctxtid);
}

__attribute__((visibility("default")))
const char *g_dcgettext(const char *domain, const char *msgid, int category) {
    (void)category;
    return do_translate(domain, NULL, msgid);
}

__attribute__((visibility("default")))
const char *dgettext(const char *domain, const char *msgid) {
    return do_translate(domain, NULL, msgid);
}

__attribute__((visibility("default")))
const char *gettext(const char *msgid) {
    return do_translate(NULL, NULL, msgid);
}

__attribute__((visibility("default")))
const char *dcgettext(const char *domain, const char *msgid, int category) {
    (void)category;
    return do_translate(domain, NULL, msgid);
}

__attribute__((visibility("default")))
char *bindtextdomain(const char *domainname, const char *dirname) {
    if (!domainname || !*domainname) return NULL;
    pthread_mutex_lock(&g_mo_mutex);
    for (int i = 0; i < g_num_domain_dirs; i++) {
        if (strcmp(g_domain_dirs[i].domain, domainname) == 0) {
            if (dirname) {
                strncpy(g_domain_dirs[i].dir, dirname, sizeof(g_domain_dirs[i].dir) - 1);
            }
            char *ret = g_domain_dirs[i].dir;
            pthread_mutex_unlock(&g_mo_mutex);
            return ret;
        }
    }
    if (dirname && g_num_domain_dirs < 16) {
        strncpy(g_domain_dirs[g_num_domain_dirs].domain, domainname, sizeof(g_domain_dirs[g_num_domain_dirs].domain) - 1);
        strncpy(g_domain_dirs[g_num_domain_dirs].dir, dirname, sizeof(g_domain_dirs[g_num_domain_dirs].dir) - 1);
        char *ret = g_domain_dirs[g_num_domain_dirs].dir;
        g_num_domain_dirs++;
        pthread_mutex_unlock(&g_mo_mutex);
        return ret;
    }
    pthread_mutex_unlock(&g_mo_mutex);
    return (char *)dirname;
}

__attribute__((visibility("default")))
char *textdomain(const char *domainname) {
    if (domainname && *domainname) {
        pthread_mutex_lock(&g_mo_mutex);
        strncpy(g_current_domain, domainname, sizeof(g_current_domain) - 1);
        pthread_mutex_unlock(&g_mo_mutex);
    }
    return g_current_domain;
}

typedef char *(*real_setlocale_t)(int, const char *);
static real_setlocale_t sys_setlocale = NULL;

__attribute__((visibility("default")))
char *setlocale(int category, const char *locale) {
    if (!sys_setlocale) {
        sys_setlocale = (real_setlocale_t)dlsym(RTLD_NEXT, "setlocale");
    }
    char *res = sys_setlocale ? sys_setlocale(category, locale) : NULL;
    if (locale && *locale) {
        pthread_mutex_lock(&g_mo_mutex);
        strncpy(g_current_locale, locale, sizeof(g_current_locale) - 1);
        pthread_mutex_unlock(&g_mo_mutex);
        if (!res || strcmp(res, "C") == 0 || strcmp(res, "POSIX") == 0) {
            return (char *)locale;
        }
    }
    return res ? res : (char *)"C.UTF-8";
}
